CREATE TABLE resume_analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    overall_score INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    result_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_resume_analyses_score CHECK (overall_score BETWEEN 0 AND 100),
    CONSTRAINT fk_resume_analyses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_resume_analyses_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT fk_resume_analyses_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    INDEX idx_resume_analyses_user_created (user_id, created_at),
    INDEX idx_resume_analyses_resume (resume_id),
    INDEX idx_resume_analyses_job (job_id)
);
