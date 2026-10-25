package com.resumeanalyzer.job.service;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException() {
        super("Job not found");
    }
}
