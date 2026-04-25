-- V7: billing schema - invoices
CREATE TABLE billing.invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    amount NUMERIC(38,2) NOT NULL,
    tax_amount NUMERIC(38,2),
    status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
    due_date DATE,
    paid_at TIMESTAMP,
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id) REFERENCES usr.patients(id),
    CONSTRAINT fk_invoices_provider FOREIGN KEY (provider_id) REFERENCES usr.providers(id),
    CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES booking.bookings(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_invoices_booking_id ON billing.invoices(booking_id);
CREATE INDEX idx_invoices_patient_id ON billing.invoices(patient_id);
CREATE INDEX idx_invoices_provider_id ON billing.invoices(provider_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);
CREATE INDEX idx_invoices_due_date ON billing.invoices(due_date);
CREATE INDEX idx_invoices_stripe_payment_intent ON billing.invoices(stripe_payment_intent_id);
