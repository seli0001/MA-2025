#!/usr/bin/env python3
"""Seed Firestore with realistic RPGHabitTracker test data.

This script creates (or updates) users, friendships, alliance data,
alliance chat messages, tasks and equipment so core app flows are easy to test.

Usage example:
  python3 scripts/seed_firestore.py \
    --service-account /path/to/service-account.json \
    --project-id rpg-habit-tracker-a9ce8 \
    --current-user-id YOUR_FIREBASE_UID \
    --current-username YourName
"""

from __future__ import annotations

import argparse
import os
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Dict, List


try:
    import firebase_admin
    from firebase_admin import auth, credentials, firestore
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Missing dependency 'firebase-admin'. Install with: pip install firebase-admin"
    ) from exc


SEED_ALLIANCE_ID = "seed_alliance_alpha"


@dataclass(frozen=True)
class SeedUser:
    uid: str
    username: str
    email: str
    avatar: str
    level: int
    title: str
    xp: int
    coins: int
    pp: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed Firestore data for testing")
    parser.add_argument(
        "--service-account",
        default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"),
        help="Path to Firebase service account JSON (or use GOOGLE_APPLICATION_CREDENTIALS)",
    )
    parser.add_argument(
        "--project-id",
        default=os.environ.get("FIREBASE_PROJECT_ID", "rpg-habit-tracker-a9ce8"),
        help="Firebase project id (default: rpg-habit-tracker-a9ce8)",
    )
    parser.add_argument(
        "--current-user-id",
        default=os.environ.get("RPG_CURRENT_USER_ID"),
        help="UID of the real user you want wired into friendships/alliance",
    )
    parser.add_argument(
        "--current-username",
        default=os.environ.get("RPG_CURRENT_USERNAME", "LocalTester"),
        help="Display username for current user if script creates/updates that user",
    )
    parser.add_argument(
        "--messages",
        type=int,
        default=12,
        help="How many alliance chat messages to seed (default: 12)",
    )
    parser.add_argument(
        "--with-current-user",
        action="store_true",
        help="Include current user in alliance/friendships/tasks. Requires --current-user-id.",
    )
    parser.add_argument(
        "--auth-password",
        default=os.environ.get("RPG_SEED_AUTH_PASSWORD", "SeedPass123!"),
        help="Password used for seed Firebase Auth users (default: SeedPass123!)",
    )
    parser.add_argument(
        "--skip-auth-users",
        action="store_true",
        help="Skip creating/updating Firebase Auth users for seed accounts.",
    )
    parser.add_argument(
        "--include-current-user-auth",
        action="store_true",
        help="Also create/update Firebase Auth for --current-user-id (off by default for safety).",
    )
    return parser.parse_args()


def init_firestore(service_account: str, project_id: str):
    if not service_account:
        raise SystemExit(
            "No service account path provided. Use --service-account or set GOOGLE_APPLICATION_CREDENTIALS."
        )
    if not os.path.exists(service_account):
        raise SystemExit(f"Service account file not found: {service_account}")

    cred = credentials.Certificate(service_account)
    firebase_admin.initialize_app(cred, {"projectId": project_id})
    return firestore.client()


def seed_user_docs(db, users: List[SeedUser]) -> None:
    now = datetime.now(timezone.utc)
    for user in users:
        payload = {
            "userId": user.uid,
            "username": user.username,
            "usernameLower": user.username.lower(),
            "email": user.email,
            "avatar": user.avatar,
            "level": user.level,
            "title": user.title,
            "xp": user.xp,
            "coins": user.coins,
            "powerPoints": user.pp,
            "basePowerPoints": user.pp,
            "bossLevel": 1,
            "totalTasksCompleted": 18,
            "totalTasksCreated": 28,
            "totalTasksFailed": 5,
            "currentStreak": 4,
            "longestStreak": 9,
            "badges": ["first_steps", "consistency_7"],
            "allianceId": None,
            "createdAt": now,
            "lastUpdated": now,
        }
        db.collection("users").document(user.uid).set(payload, merge=True)


def seed_auth_users(users: List[SeedUser], password: str) -> Dict[str, List[str]]:
    if len(password) < 6:
        raise SystemExit("--auth-password must be at least 6 characters long.")

    result: Dict[str, List[str]] = {
        "created": [],
        "updated": [],
        "failed": [],
    }

    for user in users:
        try:
            auth.get_user(user.uid)
            auth.update_user(
                user.uid,
                email=user.email,
                display_name=user.username,
                email_verified=True,
                disabled=False,
                password=password,
            )
            result["updated"].append(user.uid)
        except auth.UserNotFoundError:
            try:
                auth.create_user(
                    uid=user.uid,
                    email=user.email,
                    password=password,
                    display_name=user.username,
                    email_verified=True,
                    disabled=False,
                )
                result["created"].append(user.uid)
            except Exception as exc:  # pragma: no cover
                result["failed"].append(f"{user.uid}: {exc}")
        except Exception as exc:  # pragma: no cover
            result["failed"].append(f"{user.uid}: {exc}")

    return result


def seed_friendships(db, owner_uid: str, bots: List[SeedUser]) -> None:
    now = datetime.now(timezone.utc)

    accepted_pairs = [bots[0].uid, bots[1].uid]
    for bot_uid in accepted_pairs:
        doc_id = f"{owner_uid}_{bot_uid}"
        db.collection("friendships").document(doc_id).set(
            {
                "senderId": owner_uid,
                "receiverId": bot_uid,
                "status": "ACCEPTED",
                "createdAt": now,
            },
            merge=True,
        )

    # Incoming pending request
    incoming_uid = bots[2].uid
    db.collection("friendships").document(f"{incoming_uid}_{owner_uid}").set(
        {
            "senderId": incoming_uid,
            "receiverId": owner_uid,
            "status": "PENDING",
            "createdAt": now - timedelta(hours=3),
        },
        merge=True,
    )

    # Outgoing pending request
    outgoing_uid = bots[3].uid
    db.collection("friendships").document(f"{owner_uid}_{outgoing_uid}").set(
        {
            "senderId": owner_uid,
            "receiverId": outgoing_uid,
            "status": "PENDING",
            "createdAt": now - timedelta(hours=2),
        },
        merge=True,
    )


def seed_alliance(db, owner_uid: str, owner_name: str, bots: List[SeedUser], message_count: int) -> None:
    now = datetime.now(timezone.utc)
    member_ids = [owner_uid, bots[0].uid, bots[1].uid]

    db.collection("alliances").document(SEED_ALLIANCE_ID).set(
        {
            "id": SEED_ALLIANCE_ID,
            "name": "QA Savez",
            "leaderId": owner_uid,
            "memberIds": member_ids,
            "missionActive": True,
            "missionBossHp": 300,
            "missionCurrentDamage": 87,
            "missionStartTime": now - timedelta(hours=1),
            "createdAt": now - timedelta(days=5),
        },
        merge=True,
    )

    for uid in member_ids:
        db.collection("users").document(uid).set({"allianceId": SEED_ALLIANCE_ID}, merge=True)

    # Deterministic chat history
    senders = [
        (owner_uid, owner_name),
        (bots[0].uid, bots[0].username),
        (bots[1].uid, bots[1].username),
    ]
    lines = [
        "Ajmo tim, zavrsavamo mission veceras.",
        "Uradila sam daily zadatke, spremna za boss.",
        "Idemo, ja sam aktivirao opremu.",
        "Ko je za koordinisan attack za 10 min?",
        "Ja sam online i spreman.",
        "Pazite na PP, nemojte da ga potrosite prerano.",
    ]

    messages_ref = (
        db.collection("alliances")
        .document(SEED_ALLIANCE_ID)
        .collection("messages")
    )

    for i in range(max(1, message_count)):
        sender_uid, sender_name = senders[i % len(senders)]
        text = lines[i % len(lines)]
        timestamp = now - timedelta(minutes=(message_count - i) * 3)
        doc_id = f"seed_msg_{i + 1:03d}"
        messages_ref.document(doc_id).set(
            {
                "id": doc_id,
                "allianceId": SEED_ALLIANCE_ID,
                "senderId": sender_uid,
                "senderName": sender_name,
                "text": text,
                "timestamp": timestamp,
                "timestampClient": int(timestamp.timestamp() * 1000),
            },
            merge=True,
        )


def seed_tasks_for_owner(db, owner_uid: str, owner_level: int = 4) -> None:
    now = datetime.now(timezone.utc)
    tasks_ref = db.collection("tasks")

    task_templates = [
        ("Jutarnje istezanje", "VERY_EASY", "NORMAL", "COMPLETED", True, 1),
        ("30 min ucenje", "EASY", "IMPORTANT", "ACTIVE", False, 0),
        ("Trening snage", "HARD", "VERY_IMPORTANT", "COMPLETED", True, 2),
        ("Planiranje dana", "VERY_EASY", "NORMAL", "FAILED", False, 0),
        ("Specijalni projekat", "EXTREME", "SPECIAL", "ACTIVE", False, 0),
        ("Citanje knjige", "EASY", "IMPORTANT", "COMPLETED", True, 1),
    ]

    for idx, (name, difficulty, importance, status, completed_bool, days_offset) in enumerate(task_templates, start=1):
        due_date = now + timedelta(days=1 - days_offset)
        created_at = now - timedelta(days=3 + idx)
        completed_at = (now - timedelta(hours=idx * 4)) if completed_bool else None
        doc_id = f"seed_task_{idx:03d}_{owner_uid[:8]}"

        payload = {
            "id": doc_id,
            "userId": owner_uid,
            "name": name,
            "description": f"Seed zadatak #{idx} za testiranje.",
            "categoryId": "health",
            "difficulty": difficulty,
            "importance": importance,
            "difficultyXp": 3 + idx,
            "importanceXp": 2 + idx,
            "totalXp": 5 + idx * 2,
            "status": status,
            "completed": completed_bool,
            "isRecurring": False,
            "repeatInterval": 0,
            "repeatUnit": None,
            "dueDate": int(due_date.timestamp() * 1000),
            "endDate": int((due_date + timedelta(days=2)).timestamp() * 1000),
            "createdAt": int(created_at.timestamp() * 1000),
            "completedDate": int(completed_at.timestamp() * 1000) if completed_at else None,
            "userLevelAtCreation": owner_level,
            "countsTowardQuota": True,
        }

        tasks_ref.document(doc_id).set(payload, merge=True)


def seed_equipment_for_owner(db, owner_uid: str) -> None:
    now = datetime.now(timezone.utc)
    equipment_ref = db.collection("users").document(owner_uid).collection("equipment")

    equipment_items: Dict[str, Dict] = {
        "seed_eq_pp_boost": {
            "name": "Potion PP +10",
            "type": "potion",
            "description": "Dodaje 10 PP za battle test.",
            "icon": "potion_blue",
            "quantity": 2,
            "active": True,
            "battlesRemaining": 0,
            "bonus": 10,
            "effect": "BOOST_PP",
            "updatedAt": now,
        },
        "seed_eq_attack": {
            "name": "Sword of QA",
            "type": "weapon",
            "description": "Povecava attack power za test.",
            "icon": "sword",
            "quantity": 1,
            "active": True,
            "battlesRemaining": 0,
            "bonus": 15,
            "effect": "ATTACK_POWER",
            "updatedAt": now,
        },
    }

    for doc_id, payload in equipment_items.items():
        equipment_ref.document(doc_id).set(payload, merge=True)


def main() -> None:
    args = parse_args()
    db = init_firestore(args.service_account, args.project_id)

    seed_bots = [
        SeedUser("seed_user_mila", "MilaQuest", "mila.quest@example.com", "avatar_2", 6, "Ratnik", 980, 760, 120),
        SeedUser("seed_user_luka", "LukaDaily", "luka.daily@example.com", "avatar_3", 5, "Borac", 720, 530, 95),
        SeedUser("seed_user_ana", "AnaFocus", "ana.focus@example.com", "avatar_4", 4, "Avanturista", 510, 390, 80),
        SeedUser("seed_user_vuk", "VukStrong", "vuk.strong@example.com", "avatar_5", 7, "Vitez", 1300, 1120, 155),
    ]

    all_users = list(seed_bots)
    owner_seed_user = None

    owner_uid = None
    owner_username = args.current_username

    if args.with_current_user:
        if not args.current_user_id:
            raise SystemExit("--with-current-user requires --current-user-id")
        owner_uid = args.current_user_id
        owner_seed_user = SeedUser(
            uid=owner_uid,
            username=owner_username,
            email=f"{owner_uid[:8]}@seed.local",
            avatar="avatar_1",
            level=4,
            title="Avanturista",
            xp=560,
            coins=480,
            pp=85,
        )
        all_users.append(owner_seed_user)

    seed_user_docs(db, all_users)

    auth_seed_users = list(seed_bots)
    if args.include_current_user_auth and owner_seed_user is not None:
        auth_seed_users.append(owner_seed_user)

    auth_result = None
    if not args.skip_auth_users:
        auth_result = seed_auth_users(auth_seed_users, args.auth_password)

    if owner_uid:
        seed_friendships(db, owner_uid, seed_bots)
        seed_alliance(db, owner_uid, owner_username, seed_bots, args.messages)
        seed_tasks_for_owner(db, owner_uid)
        seed_equipment_for_owner(db, owner_uid)

    print("Seed completed.")
    print(f"Project: {args.project_id}")
    print("Users seeded:")
    for user in all_users:
        print(f"  - {user.uid} ({user.username})")

    if auth_result is not None:
        print("Auth users seeded (email verified = true):")
        print(f"  - created: {len(auth_result['created'])}")
        print(f"  - updated: {len(auth_result['updated'])}")
        print(f"  - failed: {len(auth_result['failed'])}")
        if auth_result["failed"]:
            print("Failed auth entries:")
            for item in auth_result["failed"]:
                print(f"  - {item}")
        print("Login credentials for seed auth users:")
        for user in auth_seed_users:
            print(f"  - {user.email} / {args.auth_password}")
    else:
        print("Auth user creation skipped (--skip-auth-users).")

    if owner_uid:
        print(f"Alliance seeded: {SEED_ALLIANCE_ID} (owner: {owner_uid})")
        print("Friendships/tasks/equipment/chat seeded for current user.")
    else:
        print("Only standalone seed users were created.")
        print("Tip: use --with-current-user --current-user-id <UID> for full in-app test data.")


if __name__ == "__main__":
    main()
