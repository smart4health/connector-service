CREATE TABLE cases
(
   internal_case_id uuid NOT NULL PRIMARY KEY,
   external_case_id VARCHAR NOT NULL UNIQUE,
   private_key VARCHAR NOT NULL
);