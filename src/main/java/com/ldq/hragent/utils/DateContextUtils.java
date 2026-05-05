package com.ldq.hragent.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 当前日期上下文工具
 * 用于让 Prompt 和 Tool 动态感知当前系统时间
 */
public class DateContextUtils {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateContextUtils() {
    }

    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE_ID);
    }

    public static String currentDate() {
        return today().format(DATE_FORMATTER);
    }

    public static String currentMonth() {
        return YearMonth.from(today()).format(MONTH_FORMATTER);
    }

    public static String currentYear() {
        return String.valueOf(today().getYear());
    }

    /**
     * 把用户说的月份归一化成 yyyy-MM
     */
    public static String normalizeMonth(String monthText) {
        if (monthText == null || monthText.isBlank()) {
            return currentMonth();
        }

        String text = monthText.trim();

        if (text.contains("本月")
                || text.contains("这个月")
                || text.contains("当前月")
                || text.contains("最近")) {
            return currentMonth();
        }

        if (text.matches("\\d{4}-\\d{2}")) {
            return text;
        }

        // 例如：2026年4月、2026年04月、2026年4月的
        Matcher fullMonthMatcher = Pattern.compile("(\\d{4})年(\\d{1,2})月").matcher(text);
        if (fullMonthMatcher.find()) {
            String year = fullMonthMatcher.group(1);
            String month = fullMonthMatcher.group(2);
            return year + "-" + padMonth(month);
        }

        // 例如：今年4月、今年04月
        Matcher currentYearMonthMatcher = Pattern.compile("今年(\\d{1,2})月").matcher(text);
        if (currentYearMonthMatcher.find()) {
            String month = currentYearMonthMatcher.group(1);
            return currentYear() + "-" + padMonth(month);
        }

        // 例如：4月、04月，默认按今年处理
        Matcher onlyMonthMatcher = Pattern.compile("(?<!\\d)(\\d{1,2})月").matcher(text);
        if (onlyMonthMatcher.find()) {
            String month = onlyMonthMatcher.group(1);
            return currentYear() + "-" + padMonth(month);
        }

        return text;
    }

    private static String padMonth(String month) {
        int value = Integer.parseInt(month);
        if (value < 1 || value > 12) {
            return month;
        }
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}