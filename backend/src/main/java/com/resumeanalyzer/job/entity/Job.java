package com.resumeanalyzer.job.entity;

import java.time.Instant;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "job_description", nullable = false, columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Column(name = "job_url", length = 2048)
    private String jobUrl;

    @Column(length = 150)
    private String location;

    @Column(nullable = false, length = 30, updatable = false)
    private String source = "MANUAL";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    public Job(
            UserAccount user,
            String companyName,
            String jobTitle,
            String jobDescription,
            String jobUrl,
            String location) {
        this.user = user;
        updateDetails(companyName, jobTitle, jobDescription, jobUrl, location);
    }

    public void updateDetails(
            String companyName,
            String jobTitle,
            String jobDescription,
            String jobUrl,
            String location) {
        this.companyName = normalizeOptional(companyName);
        this.jobTitle = jobTitle.strip();
        this.jobDescription = jobDescription.strip();
        this.jobUrl = normalizeOptional(jobUrl);
        this.location = normalizeOptional(location);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
