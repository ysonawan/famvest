package com.fam.vest.scraper;

import com.fam.vest.exception.InternalException;
import com.fam.vest.pojo.IpoDetails;
import com.fam.vest.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ZerodhaIpoScraper {

    private static final String IPO_URL = "https://zerodha.com/ipo/";
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{2}\\w{2})\\s*–\\s*(\\d{2}\\w{2})\\s+(\\w+\\s+\\d{4})");
    private static final Pattern LISTING_DATE_PATTERN = Pattern.compile("(\\d{2}\\w{2})\\s+(\\w+)\\s+(\\d{4})");

    public List<IpoDetails> loadAllIpos() {
        log.info("Connecting to Zerodha IPO page: {}", IPO_URL);
        List<IpoDetails> allIpoDetails = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(IPO_URL)
                    .userAgent("Mozilla/5.0")
                    .get();
            allIpoDetails.addAll(extractIpos(doc, "#live-ipo-table", "LIVE"));
            allIpoDetails.addAll(extractIpos(doc, "#upcoming-ipo", "UPCOMING"));
            allIpoDetails.addAll(extractIpos(doc, "#closed-ipo", "CLOSED"));

            log.info("Total IPOs scraped: {}", allIpoDetails.size());
        } catch (Exception exception) {
            log.error("Error while scraping IPOs from Zerodha: {}", exception.getMessage(), exception);
            throw new InternalException("Error while scraping IPOs from Zerodha: " + CommonUtil.getExceptionMessage(exception));
        }
        return allIpoDetails;
    }

    private List<IpoDetails> extractIpos(Document doc, String tableId, String status) {
        log.debug("Extracting IPOs with status: {}", status);

        List<IpoDetails> ipoDetails = new ArrayList<>();
        Elements rows = doc.select(tableId + " tbody > tr");

        log.debug("Found {} rows in table: {}", rows.size(), tableId);

        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 5) {
                log.warn("Skipping row with insufficient columns.");
                continue;
            }

            IpoDetails ipo = new IpoDetails();
            ipo.setStatus(status);

            try {
                // Logo & details
                Element logoTd = cols.get(0);
                String relativeUrl = logoTd.selectFirst("a") != null ? logoTd.selectFirst("a").attr("href") : "";
                ipo.setDetailsUrl(IPO_URL + relativeUrl.replaceFirst("^/ipo/", ""));
                ipo.setLogoUrl(logoTd.selectFirst("img") != null ? logoTd.selectFirst("img").attr("src") : "");

                // Symbol, Type, Name
                Element symbolTd = cols.get(1);
                ipo.setType(symbolTd.selectFirst("span.ipo-type") != null ? symbolTd.selectFirst("span.ipo-type").text().trim() : "");
                if(StringUtils.isBlank(ipo.getType())) {
                    ipo.setType("MAINBOARD");
                }
                Element symbolEl = symbolTd.selectFirst("span.ipo-symbol");
                if (symbolEl != null) symbolEl.select("span.ipo-type").remove();
                ipo.setSymbol(symbolEl != null ? symbolEl.text().trim() : "");
                ipo.setName(symbolTd.selectFirst("span.ipo-name") != null ? symbolTd.selectFirst("span.ipo-name").text().trim() : "");

                // IPO Dates
                String dateText = cols.get(2).ownText().trim();
                Matcher matcher = DATE_RANGE_PATTERN.matcher(dateText);
                if (matcher.find()) {
                    String startDay = stripSuffix(matcher.group(1)); // 04th -> 04
                    String endDay = stripSuffix(matcher.group(2));   // 06th -> 06
                    String monthYear = matcher.group(3);             // Aug 2025

                    ipo.setStartDate(parseDate(startDay + " " + monthYear));
                    ipo.setEndDate(parseDate(endDay + " " + monthYear));
                }

                // Listing Date
                String listingText = cols.get(3).ownText().trim();
                ipo.setListingDate(parseDate(listingText));

                // Price Range
                ipo.setPriceRange(cols.get(4).ownText().trim());

                ipoDetails.add(ipo);
                log.debug("Parsed IPO: {}", ipo.getSymbol());

            } catch (Exception e) {
                log.error("Error parsing IPO row", e);
            }
        }

        log.info("Extracted {} IPOs with status: {}", ipoDetails.size(), status);
        return ipoDetails;
    }

    private String stripSuffix(String dayStr) {
        return dayStr.replaceAll("(st|nd|rd|th)", "");
    }

    private Date parseDate(String raw) {
        if(StringUtils.isBlank(raw) || raw.trim().equals("-")) {
            return null;
        }
        // Remove ordinal suffix (st, nd, rd, th)
        String cleaned = raw.replaceAll("(\\d{1,2})(st|nd|rd|th)", "$1");
        try {
            return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).parse(cleaned);
        } catch (ParseException e) {
            return null;
        }
    }
}
