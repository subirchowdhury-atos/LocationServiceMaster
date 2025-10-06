-- Sample data initialization script for LocationServiceMaster
-- Includes data from addresses.yml and application-eligible-regions.yml

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
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS zone_cities (
    zone_id BIGINT NOT NULL,
    city VARCHAR(100),
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS zone_states (
    zone_id BIGINT NOT NULL,
    state VARCHAR(50),
    FOREIGN KEY (zone_id) REFERENCES eligibility_zones(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_zip_code ON addresses(zip_code);
CREATE INDEX IF NOT EXISTS idx_city_state ON addresses(city, state);
CREATE INDEX IF NOT EXISTS idx_coordinates ON addresses(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_zone_name ON eligibility_zones(zone_name);
CREATE INDEX IF NOT EXISTS idx_zone_type ON eligibility_zones(zone_type);
CREATE INDEX IF NOT EXISTS idx_active ON eligibility_zones(is_active);

-- Clear existing data
TRUNCATE TABLE zone_cities, zone_states, zone_zip_codes, eligibility_zones, addresses CASCADE;

-- Insert eligibility zones from application-eligible-regions.yml
-- California - Alameda County
INSERT INTO eligibility_zones (zone_name, zone_type, priority, is_active) VALUES
    ('California-Alameda County', 'COUNTY', 10, true);

DO $$
DECLARE
    alameda_zone_id BIGINT;
BEGIN
    SELECT id INTO alameda_zone_id FROM eligibility_zones WHERE zone_name = 'California-Alameda County';
    
    INSERT INTO zone_states (zone_id, state) VALUES (alameda_zone_id, 'California');
    
    INSERT INTO zone_cities (zone_id, city) VALUES
        (alameda_zone_id, 'Alameda'),
        (alameda_zone_id, 'Albany'),
        (alameda_zone_id, 'Berkeley'),
        (alameda_zone_id, 'Dublin'),
        (alameda_zone_id, 'Emeryville'),
        (alameda_zone_id, 'Fremont'),
        (alameda_zone_id, 'Hayward'),
        (alameda_zone_id, 'Livermore'),
        (alameda_zone_id, 'Newark'),
        (alameda_zone_id, 'Oakland'),
        (alameda_zone_id, 'Piedmont'),
        (alameda_zone_id, 'Pleasanton'),
        (alameda_zone_id, 'San Leandro'),
        (alameda_zone_id, 'Union City');
END $$;

-- California - Contra Costa County
INSERT INTO eligibility_zones (zone_name, zone_type, priority, is_active) VALUES
    ('California-Contra Costa County', 'COUNTY', 10, true);

DO $$
DECLARE
    contra_costa_zone_id BIGINT;
BEGIN
    SELECT id INTO contra_costa_zone_id FROM eligibility_zones WHERE zone_name = 'California-Contra Costa County';
    
    INSERT INTO zone_states (zone_id, state) VALUES (contra_costa_zone_id, 'California');
    
    INSERT INTO zone_cities (zone_id, city) VALUES
        (contra_costa_zone_id, 'Antioch'),
        (contra_costa_zone_id, 'Brentwood'),
        (contra_costa_zone_id, 'Clayton'),
        (contra_costa_zone_id, 'Concord'),
        (contra_costa_zone_id, 'Danville'),
        (contra_costa_zone_id, 'El Cerrito'),
        (contra_costa_zone_id, 'Lafayette'),
        (contra_costa_zone_id, 'Martinez'),
        (contra_costa_zone_id, 'Oakley'),
        (contra_costa_zone_id, 'Pittsburg'),
        (contra_costa_zone_id, 'Pleasant Hill'),
        (contra_costa_zone_id, 'Richmond'),
        (contra_costa_zone_id, 'San Pablo'),
        (contra_costa_zone_id, 'San Ramon'),
        (contra_costa_zone_id, 'Walnut Creek');
END $$;

-- Florida - Palm Beach County
INSERT INTO eligibility_zones (zone_name, zone_type, priority, is_active) VALUES
    ('Florida-Palm Beach County', 'COUNTY', 10, true);

DO $$
DECLARE
    palm_beach_zone_id BIGINT;
BEGIN
    SELECT id INTO palm_beach_zone_id FROM eligibility_zones WHERE zone_name = 'Florida-Palm Beach County';
    
    INSERT INTO zone_states (zone_id, state) VALUES (palm_beach_zone_id, 'Florida');
    
    INSERT INTO zone_cities (zone_id, city) VALUES
        (palm_beach_zone_id, 'Boynton Beach'),
        (palm_beach_zone_id, 'Delray Beach'),
        (palm_beach_zone_id, 'Lake Worth'),
        (palm_beach_zone_id, 'Lantana'),
        (palm_beach_zone_id, 'Mangonia Park'),
        (palm_beach_zone_id, 'North Palm Beach'),
        (palm_beach_zone_id, 'Tequesta'),
        (palm_beach_zone_id, 'West Palm Beach');
END $$;

-- Insert sample addresses from addresses.yml
-- California - Eligible
INSERT INTO addresses (street_address, city, state, zip_code, country, is_eligible, eligibility_reason) VALUES
    ('212 encounter bay', 'Alameda', 'California', '90255', 'United States', true, 'Address is in eligible region: Alameda County, California'),
    ('978 stannage avenu', 'Albany', 'California', '91106', 'United States', true, 'Address is in eligible region: Alameda County, California'),
    ('433 Camden', 'San Ramon', 'California', '90210', 'United States', true, 'Address is in eligible region: Contra Costa County, California'),
    ('1920 Hinckley', 'Albany', 'California', '94706', 'United States', true, 'Address is in eligible region: Alameda County, California'),
    
-- Florida - Eligible
    ('123 Test', 'Delray Beach', 'Florida', '90255', 'United States', true, 'Address is in eligible region: Palm Beach County, Florida'),
    
-- New York - Not Eligible
    ('400 E', 'Newburgh', 'New York', '12550', 'United States', false, 'Address is not in an eligible region: Orange County, New York'),
    ('123 Test', 'Newburgh', 'New York', '12550', 'United States', false, 'Address is not in an eligible region: Orange County, New York'),
    ('834 67th', 'Brooklyn', 'New York', '11220', 'United States', false, 'Address is not in an eligible region: Kings County, New York'),
    ('11 Brooks Hill', 'Lansing', 'New York', '14882', 'United States', false, 'Address is not in an eligible region: Tompkins County, New York'),
    ('2477 Norte Vista', 'WallKill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('115 Noteockaway Beach Blv', 'Far Rockaway', 'New York', '11694', 'United States', false, 'Address is not in an eligible region: Queens County, New York'),
    ('1574 Coast', 'Newburgh', 'New York', '12550', 'United States', false, 'Address is not in an eligible region: Orange County, New York'),
    ('1725 Manzanita', 'Newburgh', 'New York', '12550', 'United States', false, 'Address is not in an eligible region: Orange County, New York'),
    ('471 9th', 'Brooklyn', 'New York', '11215', 'United States', false, 'Address is not in an eligible region: Kings County, New York'),
    ('2416 156', 'Newburgh', 'New York', '12550', 'United States', false, 'Address is not in an eligible region: Orange County, New York'),
    ('6514 Parsons', 'Fresh Meadows', 'New York', '11365', 'United States', false, 'Address is not in an eligible region: Queens County, New York'),
    ('1220 Box 1220 PSC 220', 'Wallkill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('9311 La Grange', 'Wallkill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('1140 Salisbury', 'Wallkill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('524 Royal Oaks', 'Wallkill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('2101 Raymond', 'Wallkill', 'New York', '12589', 'United States', false, 'Address is not in an eligible region: Ulster County, New York'),
    ('16004 17th', 'Whitestone', 'New York', '11357', 'United States', false, 'Address is not in an eligible region: Queens County, New York'),
    ('1356 Cleveland', 'Niagara Falls', 'New York', '14305', 'United States', false, 'Address is not in an eligible region: Niagara County, New York'),
    
-- Pennsylvania - Not Eligible
    ('23 Green', 'Clairton', 'Pennsylvania', '15025', 'United States', false, 'Address is not in an eligible region: Allegheny County, Pennsylvania'),
    ('760 Sproul', 'Springfield', 'Pennsylvania', '19064', 'United States', false, 'Address is not in an eligible region: Delaware County, Pennsylvania'),
    ('320 Harrison', 'Lewisburg', 'Pennsylvania', '17837', 'United States', false, 'Address is not in an eligible region: Union County, Pennsylvania');

-- Display summary
DO $$
DECLARE
    zone_count INTEGER;
    address_count INTEGER;
    eligible_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO zone_count FROM eligibility_zones;
    SELECT COUNT(*) INTO address_count FROM addresses;
    SELECT COUNT(*) INTO eligible_count FROM addresses WHERE is_eligible = true;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Database initialized successfully!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Loaded % eligibility zones', zone_count;
    RAISE NOTICE 'Loaded % total addresses', address_count;
    RAISE NOTICE '  - % eligible addresses', eligible_count;
    RAISE NOTICE '  - % ineligible addresses', address_count - eligible_count;
    RAISE NOTICE '========================================';
END $$;