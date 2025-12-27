INSERT INTO app_schema.scheduled_task (
    scheduler_name, description, cron_expression, time_zone, is_active,
    last_execution_date, execution_start_time, execution_end_time, status, error_message
) VALUES
-- Reload Instruments
('RELOAD_INSTRUMENTS', 'Reload instrument data to internal database from Kite API', '0 45 2 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Reload Watchlist
('RELOAD_WATCHLIST', 'Reload user watchlist instruments from internal database', '0 0 3 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Clear Mutual Fund NAV Cache
('CLEAR_MF_NAV_CACHE', 'Clear in-memory mutual fund NAV cache', '0 30 1 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Update Mutual Fund NAV Cache
('UPDATE_MF_NAV_CACHE', 'Update mutual fund NAV cache with fresh data from third party API', '0 0/30 * * * * ', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Capture Account Snapshot
('CAPTURE_ACCOUNT_SNAPSHOT', 'Take daily snapshot of user trading accounts', '0 0 11,15,19,23 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Weekly Portfolio Report
('WEEKLY_PF_REPORT', 'Generate and send weekly portfolio report', '0 30 1 ? * SUN', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Monthly Portfolio Report - Run on 1st of every month at 8:00 AM
('MONTHLY_PF_REPORT', 'Generate and send monthly portfolio report', '0 30 1 1 * ?', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Quarterly Portfolio Report - Run on 1st of every quarter (Jan, Apr, Jul, Oct) at 8:00 AM
('QUARTERLY_PF_REPORT', 'Generate and send quarterly portfolio report', '0 30 1 1 1,4,7,10 ?', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Yearly Portfolio Report - Run on 1st of January every year at 8:00 AM
('YEARLY_PF_REPORT', 'Generate and send yearly portfolio report', '0 30 1 1 1 ?', 'UTC', true,  CURRENT_DATE,NOW(), NOW(), NULL, NULL),

-- Monthly SIP Report
('MONTHLY_SIP_REPORT', 'Generate and send monthly SIP report', '0 15 1 1 * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Restart Application
('SCHEDULER_ERROR_NOTIFICATION', 'Send scheduler error notification email to admins', '0 30 3,15 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Retrieve IPO Details
('RETRIEVE_IPO_DETAILS', 'Retrieve IPO details periodically from Zerodha website', '0 15 0/3 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL),

-- Open IPO Notification
('OPEN_IPO_NOTIFICATION', 'Notify application users with live IPOs closing in the next 2 days', '0 30 2 * * *', 'UTC', true, CURRENT_DATE, NOW(), NOW(), NULL, NULL);

