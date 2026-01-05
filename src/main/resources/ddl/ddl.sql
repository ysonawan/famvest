---- Create Database ----
CREATE DATABASE fam_vest_app;

---- Connect to the Database ----
\c fam_vest_app;

---- Create User ----
--#https://vault.zoho.in#/unlock/extension?routeName=%23%2Fpasscard%2F63500000000007049
CREATE USER app_user WITH ENCRYPTED PASSWORD '123456';

---- Grant privileges to the user ----
GRANT CONNECT ON DATABASE fam_vest_app TO app_user;

---- Create Schema and Set copilot_user as Owner ----
CREATE SCHEMA IF NOT EXISTS app_schema AUTHORIZATION app_user;

---- Grant usage on schema ----
GRANT USAGE, CREATE ON SCHEMA app_schema TO app_user;

---- All internal tables ----

CREATE TABLE app_schema.trading_account (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    totp_key VARCHAR(255),
    api_key VARCHAR(255),
    api_secret VARCHAR(255),
    request_token VARCHAR(255),
    enc_token VARCHAR(255),
    created_by varchar(255) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by varchar(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.application_user (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.user_preferences (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES app_schema.application_user(id) ON DELETE CASCADE,
    preference VARCHAR(255) NOT NULL,
    value TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_preference UNIQUE (user_id, preference)
);

CREATE TABLE app_schema.application_user_trading_account_mapping (
    application_user_id INT NOT NULL,
    trading_account_id INT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_application_user_trading_account PRIMARY KEY (application_user_id, trading_account_id),
    CONSTRAINT fk_application_user FOREIGN KEY (application_user_id)
    REFERENCES app_schema.application_user (id),
    CONSTRAINT fk_trading_account FOREIGN KEY (trading_account_id)
    REFERENCES app_schema.trading_account (id)
);

CREATE TABLE app_schema.instrument (
    id SERIAL PRIMARY KEY,
    instrument_token BIGINT,
    exchange_token BIGINT,
    display_name VARCHAR(255) NOT NULL,
    trading_symbol VARCHAR(255) NOT NULL,
    exchange VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    last_price DOUBLE PRECISION,
    expiry DATE,
    strike VARCHAR(255),
    tick_size DOUBLE PRECISION,
    lot_size INTEGER,
    instrument_type VARCHAR(255),
    segment VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL,
    CONSTRAINT unique_exchange_trading_symbol UNIQUE (exchange, trading_symbol)
);

-- Index for fast lookup by display name
CREATE INDEX idx_display_name ON app_schema.instrument (display_name);

-- Index for fast lookup by instrument token
CREATE INDEX idx_instrument_token ON app_schema.instrument (instrument_token);

-- Index for name search
CREATE INDEX idx_instrument_name ON app_schema.instrument (name);

-- Index for expiry filtering
CREATE INDEX idx_instrument_expiry ON app_schema.instrument (expiry);

-- Index for instrument type filtering
CREATE INDEX idx_instrument_type ON app_schema.instrument (instrument_type);

-- Index for segment based filtering
CREATE INDEX idx_instrument_segment ON app_schema.instrument (segment);

-- Index for exchange based filtering
CREATE INDEX idx_instrument_exchange ON app_schema.instrument (exchange);


CREATE TABLE app_schema.watchlist (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES app_schema.application_user(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL
);

CREATE TABLE app_schema.watchlist_instrument (
    id SERIAL PRIMARY KEY,
    watchlist_id INTEGER NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    trading_symbol VARCHAR(255) NOT NULL,
    instrument_token BIGINT,
    exchange VARCHAR(255),
    segment VARCHAR(255) NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE app_schema.account_snapshot (
    id SERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    holdings JSONB,
    orders JSONB,
    positions JSONB,
    funds JSONB,
    mf_orders JSONB,
    mf_sips JSONB,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (snapshot_date)
);

-- Index for fast lookup by snapshot_date
CREATE INDEX idx_snapshot_date ON app_schema.account_snapshot (snapshot_date);

CREATE TABLE app_schema.historical_timeline_values (
    id SERIAL PRIMARY KEY,
    date DATE NOT NULL,
    holdings JSONB,
    positions JSONB,
    funds JSONB,
    mf_sips JSONB,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (date)
);

-- Index for fast lookup by date
CREATE INDEX idx_date ON app_schema.historical_timeline_values (date);

CREATE TABLE app_schema.straddle_strategy (
    id SERIAL PRIMARY KEY,
    trading_account_user_id VARCHAR(255) NOT NULL,
    side VARCHAR(5) NOT NULL,
    instrument VARCHAR(255) NOT NULL,
    exchange VARCHAR(255) NOT NULL,
    trading_segment VARCHAR(255) NOT NULL,
    index VARCHAR(255) NOT NULL,
    strike_step INTEGER DEFAULT 100,
    underlying_strike_selector VARCHAR(255) NOT NULL,
    underlying_segment VARCHAR(255) NOT NULL DEFAULT 'NSE',
    lots INTEGER NOT NULL,
    market_order BOOLEAN DEFAULT TRUE,
    entry_time TIME NOT NULL,
    exit_time TIME NOT NULL,
    stop_loss DOUBLE PRECISION,
    target DOUBLE PRECISION,
    paper_trade BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    expiry_scope VARCHAR(10) NOT NULL DEFAULT 'CURRENT',
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE app_schema.straddle_strategy_execution (
    id SERIAL PRIMARY KEY,
    execution_date DATE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    strategy_id INT NOT NULL,
    unique_run_id VARCHAR(100) NOT NULL UNIQUE,
    instrument VARCHAR(255) NOT NULL,
    strike_selector VARCHAR(255) NOT NULL,
    call_strike VARCHAR(255) NOT NULL,
    call_quantity INTEGER NOT NULL,
    call_entry_price DECIMAL(10, 4),
    call_exit_price DECIMAL(10, 4),
    put_strike VARCHAR(255) NOT NULL,
    put_quantity INTEGER NOT NULL,
    paper_trade BOOLEAN NOT NULL,
    put_entry_price DECIMAL(10, 4),
    put_exit_price DECIMAL(10, 4),
    exit_pnl DECIMAL(15, 4),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    exited_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_strategy_date ON app_schema.straddle_strategy_execution (execution_date);

CREATE TABLE app_schema.scheduled_task (
    id SERIAL PRIMARY KEY,
    scheduler_name VARCHAR(100) NOT NULL,
    description TEXT,
    cron_expression VARCHAR(100) NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_execution_date DATE NOT NULL,
    execution_start_time TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_end_time TIMESTAMP,
    status VARCHAR(20),
    error_message TEXT
);

CREATE TABLE app_schema.ipo (
    id SERIAL PRIMARY KEY,
    symbol VARCHAR(255),
    type VARCHAR(20),
    name VARCHAR(255),
    details_url VARCHAR(255),
    logo_url VARCHAR(255),
    start_date DATE,
    end_date DATE,
    listing_date DATE,
    price_range VARCHAR(50),
    status VARCHAR(20),
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP NOT NULL DEFAULT NOW()
);

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app_schema TO app_user;

GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA app_schema TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT SELECT ON SEQUENCES TO app_user;
