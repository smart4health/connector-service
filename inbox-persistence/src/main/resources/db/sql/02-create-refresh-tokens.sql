CREATE TABLE refresh_tokens
(
   internal_case_id uuid NOT NULL PRIMARY KEY,
   refresh_token VARCHAR NOT NULL,
   FOREIGN KEY(internal_case_id) REFERENCES cases(internal_case_id) ON DELETE CASCADE
);