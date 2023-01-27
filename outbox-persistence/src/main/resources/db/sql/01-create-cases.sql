CREATE TABLE cases
(
   internal_case_id uuid NOT NULL PRIMARY KEY,
   status INT NOT NULL,
   public_key VARCHAR NOT NULL
);