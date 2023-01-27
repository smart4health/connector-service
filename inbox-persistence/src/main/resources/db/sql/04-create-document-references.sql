CREATE TABLE document_references
(
   internal_document_id uuid NOT NULL PRIMARY KEY,
   external_case_id VARCHAR NOT NULL,
   inserted_at TIMESTAMP NOT NULL,
   json VARCHAR NOT NULL,
   FOREIGN KEY(external_case_id) REFERENCES cases(external_case_id) ON DELETE CASCADE
);