package com.resumeanalyzer.resume.service;

import org.springframework.core.io.Resource;

public interface ResumeFileStorage {

    String store(byte[] content, String extension);

    Resource load(String storageKey);

    void delete(String storageKey);
}
