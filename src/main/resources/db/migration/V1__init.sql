-- 프로젝트
CREATE TABLE projects (
    id          TEXT      NOT NULL PRIMARY KEY,
    name        TEXT      NOT NULL,
    url         TEXT,
    enabled     INTEGER   NOT NULL DEFAULT 1,
    sort_order  INTEGER   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP
);

-- 의견
CREATE TABLE feedbacks (
    id            TEXT      NOT NULL PRIMARY KEY,
    receipt_no    TEXT      NOT NULL,
    project_id    TEXT      NOT NULL,
    title         TEXT      NOT NULL,
    email         TEXT,
    -- 접수자가 처리 결과를 이메일로 받기를 원하는지. 완료 처리 시 이 값으로 발송 여부를 정한다.
    email_reply   INTEGER   NOT NULL DEFAULT 0,
    content       TEXT      NOT NULL,
    password_hash TEXT,
    status        TEXT      NOT NULL DEFAULT 'RECEIVED',
    reply_content TEXT,
    -- 접수 당시 사용자의 언어. 처리 완료 메일을 이 언어로 보낸다.
    locale        TEXT      NOT NULL DEFAULT 'ko',
    processed_at  TIMESTAMP,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_feedbacks_receipt_no UNIQUE (receipt_no),
    CONSTRAINT fk_feedbacks_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE INDEX idx_feedbacks_created_at ON feedbacks (created_at);
CREATE INDEX idx_feedbacks_project_id ON feedbacks (project_id);

-- 첨부 파일
CREATE TABLE feedback_files (
    id            TEXT      NOT NULL PRIMARY KEY,
    feedback_id   TEXT      NOT NULL,
    original_name TEXT      NOT NULL,
    stored_name   TEXT      NOT NULL,
    content_type  TEXT,
    size          INTEGER   NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_feedback_files_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id)
);

CREATE INDEX idx_feedback_files_feedback_id ON feedback_files (feedback_id);

-- 관리자 계정
CREATE TABLE admin_users (
    id            TEXT      NOT NULL PRIMARY KEY,
    username      TEXT      NOT NULL,
    password_hash TEXT      NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_admin_users_username UNIQUE (username)
);
