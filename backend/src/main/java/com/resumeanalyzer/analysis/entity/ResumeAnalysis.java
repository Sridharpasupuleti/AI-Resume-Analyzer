package com.resumeanalyzer.analysis.entity;

import java.time.Instant;

import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import com.resumeanalyzer.job.entity.Job;
import com.resumeanalyzer.resume.entity.Resume;
import com.resumeanalyzer.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resume_analyses")
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(nullable = false, length = 1000)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "JSON")
    private ResumeAnalysisResult result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ResumeAnalysis() {
    }

    public ResumeAnalysis(
            UserAccount user,
            Resume resume,
            Job job,
            ResumeAnalysisResult result) {
        this.user = user;
        this.resume = resume;
        this.job = job;
        this.overallScore = result.overallScore();
        this.summary = result.summary();
        this.result = result;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Resume getResume() {
        return resume;
    }

    public Job getJob() {
        return job;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public ResumeAnalysisResult getResult() {
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
