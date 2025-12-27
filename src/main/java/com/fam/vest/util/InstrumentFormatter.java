package com.fam.vest.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstrumentFormatter {

    private static final Map<Character, Integer> monthMap = new HashMap<>();
    static {
        monthMap.put('1', 1);
        monthMap.put('2', 2);
        monthMap.put('3', 3);
        monthMap.put('4', 4);
        monthMap.put('5', 5);
        monthMap.put('6', 6);
        monthMap.put('7', 7);
        monthMap.put('8', 8);
        monthMap.put('9', 9);
        monthMap.put('O', 10);
        monthMap.put('N', 11);
        monthMap.put('D', 12);
    }

    public static String formatInstrument(String symbol, String instrumentType) {
        if("FUT".equalsIgnoreCase(instrumentType)) {
            return formatFuture(symbol);
        } else if("CE".equalsIgnoreCase(instrumentType) || "PE".equalsIgnoreCase(instrumentType)) {
            // Check for Oct Nov Dec weekly options (e.g., SENSEX25O0172500PE)
            if (symbol.matches("^[A-Z]+\\d{2}[1-9OND]\\d{2}\\d+(CE|PE)$")) {
                return formatWeeklyOption(symbol);
            }
            // Check for regular monthly options
            if (symbol.matches("^[A-Z]+\\d{2}[A-Z]{3}\\d+(CE|PE)$")) {
                return formatMonthlyOption(symbol);
            }
            // Check for other weekly option formats
            if (symbol.matches("^[A-Z]+\\d{5}\\d+(CE|PE)$")) {
                return formatWeeklyOption(symbol);
            }
        }
        return symbol;
    }

    private static String formatFuture(String symbol) {
        String index = symbol.replaceAll("\\d{2}[A-Z]{3}FUT$", "");
        String month = symbol.replaceAll("^[A-Z]+\\d{2}", "").replace("FUT", "");
        return index + " " + month + " FUT";
    }

    private static String formatMonthlyOption(String symbol) {
        Pattern pattern = Pattern.compile("^([A-Z]+)(\\d{2})([A-Z]{3})(\\d+)(CE|PE)$");
        Matcher matcher = pattern.matcher(symbol);
        if (matcher.find()) {
            String index = matcher.group(1);
            String month = matcher.group(3);
            String strike = matcher.group(4);
            String type = matcher.group(5);
            return index + " " + month + " " + strike + " " + type;
        }
        return "Invalid Monthly Option: " + symbol;
    }

    private static String formatWeeklyOption(String symbol) {
        Pattern pattern = Pattern.compile("^([A-Z]+)(\\d{2})([1-9OND])(\\d{2})(\\d+)(CE|PE)$");
        Matcher matcher = pattern.matcher(symbol);
        if (matcher.find()) {
            String index = matcher.group(1);
            String year = "20" + matcher.group(2);
            char monthCode = matcher.group(3).charAt(0);
            String day = matcher.group(4);
            String strike = matcher.group(5);
            String type = matcher.group(6);

            Integer month = monthMap.get(monthCode);
            if (month == null) return "Invalid Month Code";

            LocalDate date = LocalDate.of(Integer.parseInt(year), month, Integer.parseInt(day));
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)).toUpperCase();

            return index + " " + formattedDate + " " + strike + " " + type;
        }
        return "Invalid Weekly Option: " + symbol;
    }
}
