-- Missing @CollectionTable tables referenced by Visit and Escalation entities.
-- These are created by Hibernate as child tables but not by any prior migration.

CREATE TABLE visit.visit_procedures (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    procedure VARCHAR(255) NOT NULL,
    PRIMARY KEY (visit_id, procedure)
);

CREATE TABLE visit.visit_photos (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    photo_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (visit_id, photo_url)
);

CREATE TABLE visit.visit_recommendations (
    visit_id BIGINT NOT NULL REFERENCES visit.visits(id) ON DELETE CASCADE,
    recommendation VARCHAR(500) NOT NULL,
    PRIMARY KEY (visit_id, recommendation)
);

CREATE TABLE visit.escalation_notified_users (
    escalation_id BIGINT NOT NULL REFERENCES visit.escalations(id) ON DELETE CASCADE,
    notified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (escalation_id, notified_user)
);
