CREATE TABLE common_keys
(
   internal_case_id uuid NOT NULL PRIMARY KEY,
   encrypted_common_key VARCHAR NOT NULL,
   FOREIGN KEY(internal_case_id) REFERENCES cases(internal_case_id) ON DELETE CASCADE
);