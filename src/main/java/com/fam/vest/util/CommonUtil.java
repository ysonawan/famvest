package com.fam.vest.util;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.opencsv.CSVWriter;
import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.StringWriter;
import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
public class CommonUtil {

   public static String getExceptionMessage(Throwable exception) {
       String errorMessage = (exception instanceof KiteException) ? ((KiteException) exception).message : exception.getMessage();
       if (errorMessage == null || errorMessage.isEmpty()) {
           errorMessage = "An unknown internal error occurred while performing the operation";
       }
       return errorMessage;
   }

    public static Date getStartOfLastMonthDate() {
        LocalDate firstDayOfLastMonth = YearMonth.now().minusMonths(1).atDay(1);
        ZonedDateTime zonedDateTime = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getEndOfLastMonthDate() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate lastDayOfLastMonth = lastMonth.atEndOfMonth();
        ZonedDateTime zonedDateTime = lastDayOfLastMonth.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getLastSundayDate() {
        LocalDate today = LocalDate.now();
        // Subtract 1 day at a time until you find the previous Sunday
        LocalDate date = today.minusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        ZonedDateTime zonedDateTime = date.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1); // subtract 1 day
        cal.set(Calendar.HOUR_OF_DAY, 0); // set time to start of day
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static ResponseEntity<Object> success(Object data) {
        return new ResponseEntity<>(new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, HttpStatus.OK.getReasonPhrase(),
                String.valueOf(HttpStatus.OK.value()), data), HttpStatus.OK);
    }

    public static ResponseEntity<Object> success(Object data, String message) {
        return new ResponseEntity<>(new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, message,
                String.valueOf(HttpStatus.OK.value()), data), HttpStatus.OK);
    }

    public static ResponseEntity<Object> error(String message) {
        return new ResponseEntity<>(new RestResponse<>(REST_RESPONSE_STATUS.ERROR, message,
                String.valueOf(HttpStatus.BAD_REQUEST.value()), null), HttpStatus.BAD_REQUEST);
    }

    public static String formatDate(LocalDate date) {
       return date.format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
    }

    public static String formatDateWithSuffix(LocalDate date) {
        int day = date.getDayOfMonth();
        String suffix = switch (day) {
            case 11, 12, 13 -> "th";  // Special case for 11th, 12th, 13th
            default -> switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        };
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        return day + suffix + " " + date.format(formatter);
    }

    public static String formatDateTime(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static Time convertToUtcTime(LocalTime localTime, ZoneId sourceZone) {
        LocalDate today = LocalDate.now();
        ZonedDateTime sourceZonedDateTime = localTime.atDate(today).atZone(sourceZone);
        ZonedDateTime utcZonedDateTime = sourceZonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
        LocalTime utcTime = utcZonedDateTime.toLocalTime();
        return Time.valueOf(utcTime);
    }

    public static String generateCsv(HistoricalData historicalData) {
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter writer = new CSVWriter(stringWriter)) {
            // Header
            writer.writeNext(new String[]{"TimeStamp", "Open", "High", "Low", "Close", "Volume", "OI"});
            // Rows
            historicalData.dataArrayList.forEach(data ->
                writer.writeNext(new String[]{
                    data.timeStamp,
                    String.valueOf(data.open),
                    String.valueOf(data.high),
                    String.valueOf(data.low),
                    String.valueOf(data.close),
                    String.valueOf(data.volume),
                    String.valueOf(data.oi)
                })
            );
        } catch (Exception e) {
            log.error("Exception occurred while generating CSV: {}", e.getMessage(), e);
        }
        return stringWriter.toString();
    }

    public static boolean isNumeric(String input) {
        if (input == null) return false;
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static String describeCron(String cronExpression) {
       try {
           CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING);
           CronParser parser = new CronParser(cronDefinition);
           Cron cron = parser.parse(cronExpression);
           cron.validate();
           return CronDescriptor.instance(Locale.ENGLISH).describe(cron);
       } catch (Exception e) {
              log.error("Error describing cron expression '{}': {}", cronExpression, e.getMessage());
              return "Invalid cron expression";
       }

    }

    public static Date getStartOfLastMonth() {
        LocalDate firstDayOfLastMonth = YearMonth.now().minusMonths(1).atDay(1);
        ZonedDateTime zonedDateTime = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getStartOfLastQuarter() {
        LocalDate today = LocalDate.now();
        int currentQuarter = (today.getMonthValue() - 1) / 3 + 1;

        // Calculate the first month of the previous quarter
        int lastQuarterFirstMonth;
        int year = today.getYear();

        if (currentQuarter == 1) {
            // Current quarter is Q1, so last quarter is Q4 of previous year
            lastQuarterFirstMonth = 10; // October
            year = year - 1;
        } else {
            // Previous quarter in the same year
            lastQuarterFirstMonth = (currentQuarter - 2) * 3 + 1;
        }

        LocalDate firstDayOfLastQuarter = LocalDate.of(year, lastQuarterFirstMonth, 1);
        ZonedDateTime zonedDateTime = firstDayOfLastQuarter.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static Date getStartOfLastYear() {
        LocalDate firstDayOfLastYear = LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
        ZonedDateTime zonedDateTime = firstDayOfLastYear.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    public static String getReportPeriodDescription(String reportType) {
        LocalDate today = LocalDate.now();

        return switch (reportType.toLowerCase()) {
            case "weekly" -> {
                LocalDate lastSunday = today.minusDays(1);
                while (lastSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    lastSunday = lastSunday.minusDays(1);
                }
                yield "From " + formatDateWithSuffix(lastSunday) + " to " + formatDateWithSuffix(today);
            }
            case "monthly" -> {
                LocalDate startOfLastMonth = YearMonth.now().minusMonths(1).atDay(1);
                yield "From " + formatDateWithSuffix(startOfLastMonth) + " to " + formatDateWithSuffix(today);
            }
            case "quarterly" -> {
                int currentQuarter = (today.getMonthValue() - 1) / 3 + 1;
                int lastQuarterFirstMonth;
                int year = today.getYear();

                if (currentQuarter == 1) {
                    lastQuarterFirstMonth = 10; // October
                    year = year - 1;
                } else {
                    lastQuarterFirstMonth = (currentQuarter - 2) * 3 + 1;
                }

                LocalDate startOfLastQuarter = LocalDate.of(year, lastQuarterFirstMonth, 1);
                yield "From " + formatDateWithSuffix(startOfLastQuarter) + " to " + formatDateWithSuffix(today);
            }
            case "yearly" -> {
                LocalDate startOfLastYear = LocalDate.of(today.getYear() - 1, 1, 1);
                yield "From " + formatDateWithSuffix(startOfLastYear) + " to " + formatDateWithSuffix(today);
            }
            default -> "Period not specified";
        };
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
