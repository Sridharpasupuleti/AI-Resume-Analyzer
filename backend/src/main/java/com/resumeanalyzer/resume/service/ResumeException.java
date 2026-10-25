package com.resumeanalyzer.resume.service;

public class ResumeException extends RuntimeException {

    public enum Reason {
        INVALID_FILE,
        FILE_TOO_LARGE,
        NOT_FOUND,
        EXTRACTION_FAILED,
        STORAGE_FAILURE
    }

    private final Reason reason;

    public ResumeException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ResumeException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
