-- V4: booking schema - bookings and status_timeline
CREATE TABLE booking.bookings (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    service_id BIGINT,
    package_id BIGINT,
    address_id BIGINT,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(255) DEFAULT 'REQUESTED' NOT NULL,
    payment_amount NUMERIC(38,2),
    payment_status VARCHAR(255) DEFAULT 'PENDING',
    cancellation_reason VARCHAR(255),
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_bookings_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_bookings_patient FOREIGN KEY (patient_id) REFERENCES usr.patients(id),
    CONSTRAINT fk_bookings_provider FOREIGN KEY (provider_id) REFERENCES usr.providers(id),
    CONSTRAINT fk_bookings_address FOREIGN KEY (address_id) REFERENCES usr.addresses(id)
);

CREATE TABLE booking.status_timeline (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    CONSTRAINT fk_status_timeline_booking FOREIGN KEY (booking_id) REFERENCES booking.bookings(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_bookings_patient_id ON booking.bookings(patient_id);
CREATE INDEX idx_bookings_provider_id ON booking.bookings(provider_id);
CREATE INDEX idx_bookings_date ON booking.bookings(booking_date);
CREATE INDEX idx_bookings_patient_date ON booking.bookings(patient_id, booking_date);
CREATE INDEX idx_bookings_provider_date ON booking.bookings(provider_id, booking_date);
CREATE INDEX idx_bookings_status ON booking.bookings(status);
CREATE INDEX idx_bookings_payment_status ON booking.bookings(payment_status);
CREATE INDEX idx_bookings_idempotency_key ON booking.bookings(idempotency_key);
CREATE INDEX idx_bookings_created_at ON booking.bookings(created_at);
CREATE INDEX idx_status_timeline_booking_id ON booking.status_timeline(booking_id);
