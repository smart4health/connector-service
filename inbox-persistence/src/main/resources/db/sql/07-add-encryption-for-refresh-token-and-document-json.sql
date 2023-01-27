ALTER TABLE refresh_tokens RENAME COLUMN refresh_token TO encrypted_refresh_token;
ALTER TABLE document_references RENAME COLUMN json TO encrypted_json;