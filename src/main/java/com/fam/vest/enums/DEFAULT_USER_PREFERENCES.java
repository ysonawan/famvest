package com.fam.vest.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum DEFAULT_USER_PREFERENCES {

    DAILY_WATCHLIST_REFRESH("Automatically refresh your watchlist every day to keep expiry strikes and holdings up to date",
            "Daily Watchlist Refresh",
            "YES",
            "YES", "NO"),
    IPO_NOTIFICATIONS("Receive notifications about upcoming IPOs. Choose between SME, Mainboard, both, or none",
            "IPO Notifications",
            "BOTH",
            "SME", "MAINBOARD", "BOTH", "NONE"),
    WEEKLY_PORTFOLIO_REPORT("Get a detailed portfolio performance report delivered weekly to your email",
            "Weekly Portfolio Report",
            "YES",
            "YES", "NO"),
    MONTHLY_PORTFOLIO_REPORT("Get a detailed portfolio performance report delivered monthly to your email",
            "Monthly Portfolio Report",
            "YES",
            "YES", "NO"),
    QUARTERLY_PORTFOLIO_REPORT("Get a detailed portfolio performance report delivered quarterly to your email",
            "Quarterly Portfolio Report",
            "YES",
            "YES", "NO"),
    YEARLY_PORTFOLIO_REPORT("Get a detailed portfolio performance report delivered yearly to your email",
            "Yearly Portfolio Report",
            "YES",
            "YES", "NO"),
    MONTHLY_SIP_REPORT("Receive a summary report of your active SIP investments every month",
            "Monthly SIP Report",
            "YES",
            "YES", "NO"),
    THEME_COLOR("Set the application's background color according to the selected theme preference",
            "Background Theme",
            "GRAY",
            "BLUE", "PINK", "GREEN", "ORANGE", "YELLOW", "GRAY");

    private final String description;
    private final String displayName;
    private final String defaultValue;
    private final String[] allowedValues;

    DEFAULT_USER_PREFERENCES(String description, String displayName, String defaultValue, String... allowedValues) {
        this.description = description;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.allowedValues = allowedValues;
    }

    public static List<DEFAULT_USER_PREFERENCES> getDefaultPreferences() {
        return new ArrayList<>(List.of(DEFAULT_USER_PREFERENCES.values()));
    }
}
