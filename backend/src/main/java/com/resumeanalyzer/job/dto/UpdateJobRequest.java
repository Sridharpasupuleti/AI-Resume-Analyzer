package com.resumeanalyzer.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateJobRequest(
        @Size(max = 150) String companyName,
        @NotBlank @Size(max = 150) String jobTitle,
        @NotBlank @Size(max = 100000) String jobDescription,
        @URL @Size(max = 2048) String jobUrl,
        @Size(max = 150) String location) {
}
