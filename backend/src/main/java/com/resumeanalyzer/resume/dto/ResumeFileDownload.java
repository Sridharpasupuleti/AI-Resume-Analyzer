package com.resumeanalyzer.resume.dto;

import org.springframework.core.io.Resource;

public record ResumeFileDownload(Resource resource, String originalFileName, String fileType) {
}
