package com.example.rpghabittracker.data.local;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type converters for Room database
 * Converts complex types to/from database-friendly types
 */
public class Converters {
    
    private static final Gson gson = new Gson();
    
    // Date converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    // List<String> converters
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    // Map<String, Integer> converters (for equipment counts, stats, etc.)
    @TypeConverter
    public static Map<String, Integer> fromMapString(String value) {
        if (value == null) {
            return new HashMap<>();
        }
        Type mapType = new TypeToken<Map<String, Integer>>() {}.getType();
        return gson.fromJson(value, mapType);
    }
    
    @TypeConverter
    public static String fromMap(Map<String, Integer> map) {
        if (map == null) {
            return null;
        }
        return gson.toJson(map);
    }
    
    // List<Integer> converters (for recurring days, etc.)
    @TypeConverter
    public static List<Integer> fromIntListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String fromIntList(List<Integer> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    // Map<String, Double> converters (for weapon upgrades, etc.)
    @TypeConverter
    public static Map<String, Double> fromDoubleMapString(String value) {
        if (value == null) {
            return new HashMap<>();
        }
        Type mapType = new TypeToken<Map<String, Double>>() {}.getType();
        return gson.fromJson(value, mapType);
    }
    
    @TypeConverter
    public static String fromDoubleMap(Map<String, Double> map) {
        if (map == null) {
            return null;
        }
        return gson.toJson(map);
    }
}
