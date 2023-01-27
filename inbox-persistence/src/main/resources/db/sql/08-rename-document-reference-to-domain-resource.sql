ALTER TABLE document_references RENAME TO domain_resources;
ALTER TABLE domain_resources RENAME COLUMN internal_document_id TO internal_resource_id;
ALTER TABLE upload_attempts RENAME COLUMN internal_document_id TO internal_resource_id;