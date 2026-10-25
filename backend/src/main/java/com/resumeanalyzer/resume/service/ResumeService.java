package com.resumeanalyzer.resume.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.resume.dto.ResumeDetails;
import com.resumeanalyzer.resume.dto.ResumeFileDownload;
import com.resumeanalyzer.resume.dto.ResumeSummary;
import com.resumeanalyzer.resume.entity.Resume;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.unit.DataSize;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserAccountRepository userRepository;
    private final ResumeFileStorage fileStorage;
    private final ResumeTextExtractor textExtractor;
    private final long maxFileSize;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserAccountRepository userRepository,
            ResumeFileStorage fileStorage,
            ResumeTextExtractor textExtractor,
            @Value("${app.resume.max-file-size:10MB}") String maxFileSize) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.textExtractor = textExtractor;
        this.maxFileSize = DataSize.parse(maxFileSize).toBytes();
    }

    @Transactional
    public ResumeDetails upload(UserPrincipal principal, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeException(ResumeException.Reason.INVALID_FILE, "A non-empty resume file is required");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResumeException(
                    ResumeException.Reason.FILE_TOO_LARGE,
                    "The resume file exceeds the maximum allowed size");
        }

        String originalFileName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        byte[] content = readContent(file);
        validateSignature(content, extension);
        String extractedText = textExtractor.extract(content, extension);
        String storageKey = fileStorage.store(content, extension);

        try {
            Resume resume = resumeRepository.saveAndFlush(new Resume(
                    userRepository.getReferenceById(principal.userId()),
                    titleFrom(originalFileName),
                    originalFileName,
                    extension.toUpperCase(Locale.ROOT),
                    storageKey,
                    extractedText));
            return toDetails(resume);
        } catch (RuntimeException exception) {
            try {
                fileStorage.delete(storageKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ResumeSummary> list(UserPrincipal principal) {
        return resumeRepository.findAllByUser_IdOrderByCreatedAtDesc(principal.userId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDetails get(UserPrincipal principal, Long resumeId) {
        return toDetails(findOwnedResume(principal, resumeId));
    }

    @Transactional(readOnly = true)
    public ResumeFileDownload download(UserPrincipal principal, Long resumeId) {
        Resume resume = findOwnedResume(principal, resumeId);
        return new ResumeFileDownload(
                fileStorage.load(resume.getStorageKey()),
                resume.getOriginalFileName(),
                resume.getFileType());
    }

    @Transactional
    public void delete(UserPrincipal principal, Long resumeId) {
        Resume resume = findOwnedResume(principal, resumeId);
        resumeRepository.delete(resume);
        resumeRepository.flush();
        fileStorage.delete(resume.getStorageKey());
    }

    private Resume findOwnedResume(UserPrincipal principal, Long resumeId) {
        return resumeRepository.findByIdAndUser_Id(resumeId, principal.userId())
                .orElseThrow(() -> new ResumeException(
                        ResumeException.Reason.NOT_FOUND,
                        "Resume not found"));
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResumeException(
                    ResumeException.Reason.INVALID_FILE,
                    "The uploaded resume file could not be read",
                    exception);
        }
    }

    private void validateSignature(byte[] content, String extension) {
        boolean valid = switch (extension) {
            case "pdf" -> startsWithPdfHeader(content);
            case "docx" -> content.length >= 4
                    && content[0] == 'P' && content[1] == 'K'
                    && content[2] == 3 && content[3] == 4;
            default -> false;
        };
        if (!valid) {
            throw new ResumeException(
                    ResumeException.Reason.INVALID_FILE,
                    "The file content does not match its PDF or DOCX extension");
        }
    }

    private boolean startsWithPdfHeader(byte[] content) {
        byte[] header = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int limit = Math.min(content.length - header.length, 1024);
        for (int offset = 0; offset <= limit; offset++) {
            boolean matches = true;
            for (int index = 0; index < header.length; index++) {
                if (content[offset + index] != header[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResumeException(ResumeException.Reason.INVALID_FILE, "The uploaded file must have a name");
        }
        String normalized = fileName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.isBlank()
                || name.length() > 255
                || name.chars().anyMatch(Character::isISOControl)) {
            throw new ResumeException(ResumeException.Reason.INVALID_FILE, "The uploaded file name is invalid");
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw new ResumeException(
                    ResumeException.Reason.INVALID_FILE,
                    "Only PDF and DOCX resumes are supported");
        }
        return extension;
    }

    private String titleFrom(String fileName) {
        String title = fileName.substring(0, fileName.lastIndexOf('.')).strip();
        return title.isEmpty() ? "Resume" : title;
    }

    private ResumeSummary toSummary(Resume resume) {
        return new ResumeSummary(
                resume.getId(),
                resume.getTitle(),
                resume.getOriginalFileName(),
                resume.getFileType(),
                resume.getCreatedAt());
    }

    private ResumeDetails toDetails(Resume resume) {
        return new ResumeDetails(
                resume.getId(),
                resume.getTitle(),
                resume.getOriginalFileName(),
                resume.getFileType(),
                resume.getExtractedText(),
                resume.getCreatedAt());
    }
}
