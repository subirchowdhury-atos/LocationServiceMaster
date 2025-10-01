-- Sample data initialization script for LocationServiceMaster

-- Create tables if they don't exist (backup in case JPA doesn't create them)
CREATE TABLE IF NOT EXISTS addresses (
    id BIGSERIAL PRIMARY KEY,
    street_address VARCHAR(255) NOT NULL,
    street_address_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_eligible BOOLEAN,
    eligibility_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS eligibility_zones (
    id BIGSERIAL PRIMARY KEY,
    zone_name VARCHAR(255) NOT NULL UNIQUE,
    zone_type VARCHAR(50) NOT NULL,
    min_latitude DOUBLE PRECISION,
    max_latitude DOUBLE PRECISION,
    min_longitude DOUBLE PRECISION,
    max_longitude DOUBLE PRECISION,
    is_active BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zone_zip_codes (
    zone_id BIGINT NOT NULL,
    zip_code VARCHAR(10),
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id)
);

CREATE TABLE IF NOT EXISTS zone_cities (
    zone_id BIGINT NOT NULL,
    city VARCHAR(100),
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id)
);

CREATE TABLE IF NOT EXISTS zone_states (
    zone_id BIGINT NOT NULL,
    state VARCHAR(50),
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_zip_code ON addresses(zip_code);
CREATE INDEX IF NOT EXISTS idx_city_state ON addresses(city, state);
CREATE INDEX IF NOT EXISTS idx_coordinates ON addresses(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_zone_name ON eligibility_zones(zone_name);
CREATE INDEX IF NOT EXISTS idx_zone_type ON eligibility_zones(zone_type);
CREATE INDEX IF NOT EXISTS idx_active ON eligibility_zones(is_active);

-- Insert sample eligibility zones
INSERT INTO eligibility_zones (zone_name, zone_type, priority, is_active) VALUES
    ('Chicago Metro Area', 'CITY', 10, true),
    ('Illinois State', 'STATE', 5, true),
    ('Premium ZIP Codes', 'ZIP_CODE', 15, true),
    ('Extended Service Area', 'COORDINATES', 8, true),
    ('Custom Zone 1', 'CUSTOM', 3, true)
ON CONFLICT (zone_name) DO NOTHING;

-- Get zone IDs for reference
DO $$
DECLARE
    chicago_zone_id BIGINT;
    illinois_zone_id BIGINT;
    premium_zip_id BIGINT;
    coord_zone_id BIGINT;
BEGIN
    SELECT id INTO chicago_zone_id FROM eligibility_zones WHERE zone_name = 'Chicago Metro Area';
    SELECT id INTO illinois_zone_id FROM eligibility_zones WHERE zone_name = 'Illinois State';
    SELECT id INTO premium_zip_id FROM eligibility_zones WHERE zone_name = 'Premium ZIP Codes';
    SELECT id INTO coord_zone_id FROM eligibility_zones WHERE zone_name = 'Extended Service Area';

    -- Insert cities for Chicago Metro Area
    INSERT INTO zone_cities (zone_id, city) VALUES
        (chicago_zone_id, 'Chicago'),
        (chicago_zone_id, 'Evanston'),
        (chicago_zone_id, 'Oak Park'),
        (chicago_zone_id, 'Naperville'),
        (chicago_zone_id, 'Aurora'),
        (chicago_zone_id, 'Joliet')
    ON CONFLICT DO NOTHING;

    -- Insert states for Illinois zone
    INSERT INTO zone_states (zone_id, state) VALUES
        (illinois_zone_id, 'IL'),
        (illinois_zone_id, 'Illinois')
    ON CONFLICT DO NOTHING;

    -- Insert premium ZIP codes
    INSERT INTO zone_zip_codes (zone_id, zip_code) VALUES
        (premium_zip_id, '60601'),
        (premium_zip_id, '60602'),
        (premium_zip_id, '60603'),
        (premium_zip_id, '60604'),
        (premium_zip_id, '60605'),
        (premium_zip_id, '60606'),
        (premium_zip_id, '60607'),
        (premium_zip_id, '60610'),
        (premium_zip_id, '60611'),
        (premium_zip_id, '60614'),
        (premium_zip_id, '60615'),
        (premium_zip_id, '60616')
    ON CONFLICT DO NOTHING;

    -- Update coordinate bounds for Extended Service Area (Chicago area)
    UPDATE eligibility_zones 
    SET min_latitude = 41.5, 
        max_latitude = 42.5,
        min_longitude = -88.5,
        max_longitude = -87.0
    WHERE id = coord_zone_id;
END $$;

-- Insert sample addresses with eligibility status
INSERT INTO addresses (street_address, city, state, zip_code, country, latitude, longitude, is_eligible, eligibility_reason) VALUES
    ('123 N Michigan Ave', 'Chicago', 'IL', '60601', 'USA', 41.8781, -87.6298, true, 'Address is eligible for service (Zone: Premium ZIP Codes, Confidence: 100%)'),
    ('456 State St', 'Chicago', 'IL', '60605', 'USA', 41.8781, -87.6298, true, 'Address is eligible for service (Zone: Premium ZIP Codes, Confidence: 100%)'),
    ('789 Oak St', 'Evanston', 'IL', '60201', 'USA', 42.0451, -87.6877, true, 'Address is eligible for service (Zone: Chicago Metro Area, Confidence: 80%)'),
    ('321 Main St', 'Springfield', 'IL', '62701', 'USA', 39.7817, -89.6501, true, 'Address is eligible for service (Zone: Illinois State, Confidence: 60%)'),
    ('999 Rural Rd', 'Remote Town', 'WY', '82001', 'USA', 41.1400, -104.8202, false, 'Address is not in any eligible service area')
ON CONFLICT DO NOTHING;