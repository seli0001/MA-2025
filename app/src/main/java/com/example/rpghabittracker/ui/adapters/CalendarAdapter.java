package com.example.rpghabittracker.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for calendar grid showing days of a month
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<CalendarDay> days = new ArrayList<>();
    private final OnDayClickListener listener;
    private int selectedPosition = -1;
    private final Map<Integer, List<Integer>> taskColors = new HashMap<>(); // day -> list of colors

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> newDays, int todayPosition) {
        days.clear();
        days.addAll(newDays);
        selectedPosition = todayPosition;
        notifyDataSetChanged();
    }

    public void setTaskIndicators(Map<Integer, List<Integer>> indicators) {
        taskColors.clear();
        taskColors.putAll(indicators);
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0) notifyItemChanged(oldPosition);
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day, position == selectedPosition, taskColors.get(day.dayOfMonth));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout dayContainer;
        private final TextView textDay;
        private final View indicator1, indicator2, indicator3;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayContainer = itemView.findViewById(R.id.dayContainer);
            textDay = itemView.findViewById(R.id.textDay);
            indicator1 = itemView.findViewById(R.id.indicator1);
            indicator2 = itemView.findViewById(R.id.indicator2);
            indicator3 = itemView.findViewById(R.id.indicator3);
        }

        void bind(CalendarDay day, boolean isSelected, List<Integer> colors) {
            if (day.dayOfMonth == 0) {
                // Empty cell
                textDay.setText("");
                dayContainer.setBackground(null);
                indicator1.setVisibility(View.GONE);
                indicator2.setVisibility(View.GONE);
                indicator3.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                return;
            }

            textDay.setText(String.valueOf(day.dayOfMonth));

            // Set text color based on state
            if (day.isToday) {
                textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                textDay.setBackground(bg);
            } else if (isSelected) {
                textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setStroke(2, ContextCompat.getColor(itemView.getContext(), R.color.primary));
                bg.setColor(Color.TRANSPARENT);
                textDay.setBackground(bg);
            } else if (day.isCurrentMonth) {
                textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
                textDay.setBackground(null);
            } else {
                textDay.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_tertiary));
                textDay.setBackground(null);
            }

            // Task indicators
            if (colors != null && !colors.isEmpty()) {
                setIndicator(indicator1, colors.size() > 0 ? colors.get(0) : null);
                setIndicator(indicator2, colors.size() > 1 ? colors.get(1) : null);
                setIndicator(indicator3, colors.size() > 2 ? colors.get(2) : null);
            } else {
                indicator1.setVisibility(View.GONE);
                indicator2.setVisibility(View.GONE);
                indicator3.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && day.isCurrentMonth) {
                    setSelectedPosition(getAdapterPosition());
                    listener.onDayClick(day);
                }
            });
        }

        private void setIndicator(View indicator, Integer color) {
            if (color != null) {
                indicator.setVisibility(View.VISIBLE);
                GradientDrawable bg = (GradientDrawable) indicator.getBackground();
                bg.setColor(color);
            } else {
                indicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Data class representing a calendar day
     */
    public static class CalendarDay {
        public final int dayOfMonth;
        public final boolean isCurrentMonth;
        public final boolean isToday;
        public final long timestamp;

        public CalendarDay(int dayOfMonth, boolean isCurrentMonth, boolean isToday, long timestamp) {
            this.dayOfMonth = dayOfMonth;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.timestamp = timestamp;
        }

        public static CalendarDay empty() {
            return new CalendarDay(0, false, false, 0);
        }
    }
}
