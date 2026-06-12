CREATE TABLE documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by_user_id VARCHAR(255) NOT NULL,
    uploaded_by_email VARCHAR(320) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_documents_project_id ON documents (project_id);
