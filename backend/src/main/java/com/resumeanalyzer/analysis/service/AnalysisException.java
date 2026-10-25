package com.resumeanalyzer.analysis.service;

public class AnalysisException extends RuntimeException {

    public enum Reason {
        RESOURCE_NOT_FOUND,
        INVALID_RESULT,
        REQUEST_TOO_LARGE,
        PROVIDER_NOT_CONFIGURED,
        PROVIDER_FAILURE
    }

    private final Reason reason;

    public AnalysisException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AnalysisException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
