CREATE TABLE oauth_states
(
   internal_case_id uuid NOT NULL PRIMARY KEY,
   state VARCHAR UNIQUE NOT NULL,
   FOREIGN KEY(internal_case_id) REFERENCES cases(internal_case_id) ON DELETE CASCADE
);