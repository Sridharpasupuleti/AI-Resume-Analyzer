package com.resumeanalyzer.resume.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalResumeFileStorage implements ResumeFileStorage {

    private final Path storageDirectory;

    public LocalResumeFileStorage(@Value("${app.resume.storage-dir:./data/resumes}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String extension) {
        String storageKey = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(storageDirectory);
            Files.write(
                    resolve(storageKey),
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            return storageKey;
        } catch (IOException exception) {
            throw new ResumeException(
                    ResumeException.Reason.STORAGE_FAILURE,
                    "The resume file could not be stored",
                    exception);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResumeException(
                    ResumeException.Reason.STORAGE_FAILURE,
                    "The stored resume file is unavailable");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new ResumeException(
                    ResumeException.Reason.STORAGE_FAILURE,
                    "The stored resume file could not be deleted",
                    exception);
        }
    }

    private Path resolve(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.getParent().equals(storageDirectory)) {
            throw new ResumeException(
                    ResumeException.Reason.STORAGE_FAILURE,
                    "The stored resume key is invalid");
        }
        return path;
    }
}
