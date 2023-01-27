CREATE TABLE case_nonces
(
    internal_case_id uuid NOT NULL PRIMARY KEY,
    nonce INT NOT NULL,
    FOREIGN KEY(internal_case_id) REFERENCES cases(internal_case_id) ON DELETE CASCADE
)