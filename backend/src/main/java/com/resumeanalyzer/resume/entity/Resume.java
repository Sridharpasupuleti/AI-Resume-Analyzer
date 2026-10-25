package com.resumeanalyzer.resume.entity;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "storage_key", nullable = false, unique = true, length = 80)
    private String storageKey;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Resume() {
    }

    public Resume(
            UserAccount user,
            String title,
            String originalFileName,
            String fileType,
            String storageKey,
            String extractedText) {
        this.user = user;
        this.title = title;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.storageKey = storageKey;
        this.extractedText = extractedText;
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

    public String getTitle() {
        return title;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
