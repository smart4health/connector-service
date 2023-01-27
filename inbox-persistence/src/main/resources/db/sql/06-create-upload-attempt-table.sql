CREATE TABLE upload_attempts
(
    attempt_id uuid NOT NULL PRIMARY KEY,
    internal_document_id uuid NOT NULL,
    attempted_at TIMESTAMP NOT NULL,
    FOREIGN KEY(internal_document_id) REFERENCES document_references(internal_document_id) ON DELETE CASCADE
)