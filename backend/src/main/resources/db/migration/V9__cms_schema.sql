-- V9: cms schema - services and service_packages
CREATE TABLE cms.services (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER DEFAULT 60 NOT NULL,
    price NUMERIC(38,2) NOT NULL,
    rating DOUBLE PRECISION DEFAULT 0,
    image_url VARCHAR(500),
    included_items TEXT[],
    required_documents TEXT[],
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE cms.service_packages (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size VARCHAR(255) NOT NULL,
    description TEXT,
    service_ids BIGINT[],
    price NUMERIC(38,2) NOT NULL,
    discount_percent NUMERIC(5,2) DEFAULT 0,
    validity_days INTEGER DEFAULT 365,
    benefits TEXT[],
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_services_uuid ON cms.services(uuid);
CREATE INDEX idx_services_category ON cms.services(category);
CREATE INDEX idx_services_active ON cms.services(active);
CREATE INDEX idx_services_active_category ON cms.services(active, category);
CREATE INDEX idx_services_name ON cms.services(name);
CREATE INDEX idx_services_price ON cms.services(price);
CREATE INDEX idx_service_packages_size ON cms.service_packages(size);
CREATE INDEX idx_service_packages_active ON cms.service_packages(active);
