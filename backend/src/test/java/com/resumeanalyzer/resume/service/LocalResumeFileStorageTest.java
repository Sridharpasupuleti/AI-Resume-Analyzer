package com.resumeanalyzer.resume.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalResumeFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesLoadsAndDeletesFilesInsideConfiguredDirectory() throws Exception {
        LocalResumeFileStorage storage = new LocalResumeFileStorage(temporaryDirectory.toString());
        byte[] content = "resume contents".getBytes(StandardCharsets.UTF_8);

        String storageKey = storage.store(content, "pdf");

        assertEquals("resume contents", new String(
                storage.load(storageKey).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
        storage.delete(storageKey);
        assertThrows(ResumeException.class, () -> storage.load(storageKey));
    }

    @Test
    void rejectsStorageKeysThatEscapeTheConfiguredDirectory() {
        LocalResumeFileStorage storage = new LocalResumeFileStorage(temporaryDirectory.toString());

        assertThrows(ResumeException.class, () -> storage.load("..\\outside.pdf"));
    }
}
