-- ============================================================
-- TeamUp Database Schema  (PostgreSQL)
-- ============================================================
-- Run this script to initialize the database.
-- Compatible with PostgreSQL 14+ / MySQL 8+.
-- ============================================================

-- --------------------  1. USERS  --------------------
CREATE TABLE IF NOT EXISTS users (
    user_id          BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)  NOT NULL,
    email            VARCHAR(150)  NOT NULL UNIQUE,
    role             VARCHAR(20)   NOT NULL CHECK (role IN ('STUDENT', 'TEACHER')),
    password_hash    VARCHAR(255)  NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP
);

-- --------------------  2. GROUPS  --------------------
CREATE TABLE IF NOT EXISTS groups_table (
    group_id         BIGSERIAL PRIMARY KEY,
    group_name       VARCHAR(100)  NOT NULL,
    class_id         VARCHAR(50)   NOT NULL,
    leader_id        BIGINT        NOT NULL
                          CONSTRAINT fk_group_leader
                          REFERENCES users(user_id)
                          ON DELETE RESTRICT,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP
);

-- Index for faster lookup by class
CREATE INDEX IF NOT EXISTS idx_groups_class_id ON groups_table(class_id);

-- --------------------  3. TASKS  --------------------
CREATE TABLE IF NOT EXISTS tasks (
    task_id                BIGSERIAL PRIMARY KEY,
    task_name             VARCHAR(200)  NOT NULL,
    description           TEXT,
    deadline              TIMESTAMP,
    progress              INTEGER       NOT NULL DEFAULT 0
                                   CHECK (progress >= 0 AND progress <= 100),
    status                VARCHAR(20)   NOT NULL DEFAULT 'TODO'
                                   CHECK (status IN ('TODO','IN_PROGRESS','PENDING_REVIEW','DONE')),
    last_progress_update  TIMESTAMP,
    group_id              BIGINT        NOT NULL
                            CONSTRAINT fk_task_group
                            REFERENCES groups_table(group_id)
                            ON DELETE CASCADE,
    assigned_to           BIGINT
                            CONSTRAINT fk_task_assignee
                            REFERENCES users(user_id)
                            ON DELETE SET NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_tasks_group_id       ON tasks(group_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to    ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tasks_status         ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline        ON tasks(deadline);

-- --------------------  4. SUBMISSIONS  --------------------
CREATE TABLE IF NOT EXISTS submissions (
    submission_id   BIGSERIAL PRIMARY KEY,
    file_url        VARCHAR(500)  NOT NULL,
    submitted_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    task_id         BIGINT        NOT NULL
                        CONSTRAINT fk_submission_task
                        REFERENCES tasks(task_id)
                        ON DELETE CASCADE
);

-- Index for listing submissions per task
CREATE INDEX IF NOT EXISTS idx_submissions_task_id  ON submissions(task_id);

-- --------------------  5. PEER REVIEWS  --------------------
CREATE TABLE IF NOT EXISTS peer_reviews (
    review_id     BIGSERIAL PRIMARY KEY,
    score         INTEGER       NOT NULL CHECK (score >= 1 AND score <= 5),
    comment       TEXT,
    group_id      BIGINT        NOT NULL
                        CONSTRAINT fk_review_group
                        REFERENCES groups_table(group_id)
                        ON DELETE CASCADE,
    reviewer_id   BIGINT        NOT NULL
                        CONSTRAINT fk_review_reviewer
                        REFERENCES users(user_id)
                        ON DELETE CASCADE,
    reviewee_id   BIGINT        NOT NULL
                        CONSTRAINT fk_review_reviewee
                        REFERENCES users(user_id)
                        ON DELETE CASCADE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- A reviewer can only submit one review per reviewee per group
    CONSTRAINT uk_peer_review_pair
        UNIQUE (group_id, reviewer_id, reviewee_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_peer_reviews_group_id     ON peer_reviews(group_id);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewer_id ON peer_reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewee_id ON peer_reviews(reviewee_id);

-- --------------------  6. GROUP MEMBERS  --------------------
CREATE TABLE IF NOT EXISTS group_members (
    group_id  BIGINT NOT NULL
                   CONSTRAINT fk_gm_group
                   REFERENCES groups_table(group_id)
                   ON DELETE CASCADE,
    user_id   BIGINT NOT NULL
                   CONSTRAINT fk_gm_user
                   REFERENCES users(user_id)
                   ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON group_members(user_id);

-- --------------------  7. NOTIFICATIONS  --------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_id  BIGSERIAL PRIMARY KEY,
    message          TEXT         NOT NULL,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    type             VARCHAR(50)  NOT NULL,
    user_id          BIGINT       NOT NULL
                                CONSTRAINT fk_notification_user
                                REFERENCES users(user_id)
                                ON DELETE CASCADE,
    task_id          BIGINT
                                CONSTRAINT fk_notification_task
                                REFERENCES tasks(task_id)
                                ON DELETE SET NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id  ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(user_id, is_read);

-- ============================================================
-- MySQL Variant  (uncomment and run separately if using MySQL)
-- ============================================================
-- CREATE TABLE IF NOT EXISTS users (
--     user_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
--     name             VARCHAR(100)  NOT NULL,
--     email            VARCHAR(150)  NOT NULL UNIQUE,
--     role             VARCHAR(20)   NOT NULL,
--     password_hash    VARCHAR(255)  NOT NULL,
--     created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at       DATETIME
-- );
--
-- CREATE TABLE IF NOT EXISTS groups_table (
--     group_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
--     group_name  VARCHAR(100) NOT NULL,
--     class_id    VARCHAR(50)  NOT NULL,
--     leader_id   BIGINT       NOT NULL,
--     created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at  DATETIME,
--     FOREIGN KEY (leader_id) REFERENCES users(user_id) ON DELETE RESTRICT
-- );
--
-- CREATE TABLE IF NOT EXISTS tasks (
--     task_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
--     task_name             VARCHAR(200) NOT NULL,
--     description           TEXT,
--     deadline              DATETIME,
--     progress              INT          NOT NULL DEFAULT 0,
--     status                VARCHAR(20)  NOT NULL DEFAULT 'TODO',
--     last_progress_update  DATETIME,
--     group_id              BIGINT       NOT NULL,
--     assigned_to           BIGINT,
--     created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at            DATETIME,
--     FOREIGN KEY (group_id)    REFERENCES groups_table(group_id) ON DELETE CASCADE,
--     FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL
-- );
--
-- CREATE TABLE IF NOT EXISTS submissions (
--     submission_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
--     file_url       VARCHAR(500) NOT NULL,
--     submitted_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     task_id        BIGINT       NOT NULL,
--     FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
-- );
--
-- CREATE TABLE IF NOT EXISTS peer_reviews (
--     review_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
--     score        INT    NOT NULL,
--     comment      TEXT,
--     group_id     BIGINT NOT NULL,
--     reviewer_id  BIGINT NOT NULL,
--     reviewee_id  BIGINT NOT NULL,
--     created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (group_id)    REFERENCES groups_table(group_id) ON DELETE CASCADE,
--     FOREIGN KEY (reviewer_id) REFERENCES users(user_id)  ON DELETE CASCADE,
--     FOREIGN KEY (reviewee_id) REFERENCES users(user_id)  ON DELETE CASCADE,
--     UNIQUE KEY uk_peer_review_pair (group_id, reviewer_id, reviewee_id)
-- );
