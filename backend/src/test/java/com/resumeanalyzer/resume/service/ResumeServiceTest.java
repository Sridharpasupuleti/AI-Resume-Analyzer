package com.resumeanalyzer.resume.service;

import java.util.Optional;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.resume.entity.Resume;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.user.entity.UserAccount;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private ResumeFileStorage fileStorage;

    @Mock
    private ResumeTextExtractor textExtractor;

    private ResumeService resumeService;
    private final UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(
                resumeRepository,
                userRepository,
                fileStorage,
                textExtractor,
                "10MB");
    }

    @Test
    void uploadValidatesContentExtractsTextAndPersistsForAuthenticatedOwner() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\fakepath\\Casey Resume.PDF",
                "application/pdf",
                "%PDF-1.7 content".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        UserAccount user = new UserAccount("Casey", "casey@example.com", "hash");
        when(textExtractor.extract(any(byte[].class), org.mockito.ArgumentMatchers.eq("pdf")))
                .thenReturn("Casey Example");
        when(fileStorage.store(any(byte[].class), org.mockito.ArgumentMatchers.eq("pdf")))
                .thenReturn("generated-key.pdf");
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(resumeRepository.saveAndFlush(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var details = resumeService.upload(principal, file);

        var savedResume = org.mockito.ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).saveAndFlush(savedResume.capture());
        assertEquals(user, savedResume.getValue().getUser());
        assertEquals("Casey Resume", savedResume.getValue().getTitle());
        assertEquals("Casey Resume.PDF", savedResume.getValue().getOriginalFileName());
        assertEquals("PDF", savedResume.getValue().getFileType());
        assertEquals("Casey Example", details.extractedText());
        verify(fileStorage).store(any(byte[].class), org.mockito.ArgumentMatchers.eq("pdf"));
    }

    @Test
    void uploadRejectsUnsupportedTypesBeforeStoringAnything() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "resume".getBytes());

        ResumeException exception = assertThrows(
                ResumeException.class,
                () -> resumeService.upload(principal, file));

        assertEquals(ResumeException.Reason.INVALID_FILE, exception.getReason());
        verify(fileStorage, never()).store(any(byte[].class), any());
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void uploadRejectsExtensionThatDoesNotMatchFileContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "not a PDF".getBytes());

        ResumeException exception = assertThrows(
                ResumeException.class,
                () -> resumeService.upload(principal, file));

        assertEquals(ResumeException.Reason.INVALID_FILE, exception.getReason());
        verify(textExtractor, never()).extract(any(byte[].class), any());
        verify(fileStorage, never()).store(any(byte[].class), any());
    }

    @Test
    void userCannotRetrieveAnotherUsersResume() {
        when(resumeRepository.findByIdAndUser_Id(21L, principal.userId())).thenReturn(Optional.empty());

        ResumeException exception = assertThrows(
                ResumeException.class,
                () -> resumeService.get(principal, 21L));

        assertEquals(ResumeException.Reason.NOT_FOUND, exception.getReason());
    }

    @Test
    void deletionIsOwnerScopedAndRemovesMetadataBeforeTheFile() {
        Resume resume = new Resume(
                new UserAccount("Casey", "casey@example.com", "hash"),
                "Casey Resume",
                "Casey Resume.pdf",
                "PDF",
                "resume-key.pdf",
                "Casey Example");
        when(resumeRepository.findByIdAndUser_Id(21L, principal.userId())).thenReturn(Optional.of(resume));

        resumeService.delete(principal, 21L);

        var order = inOrder(resumeRepository, fileStorage);
        order.verify(resumeRepository).delete(resume);
        order.verify(resumeRepository).flush();
        order.verify(fileStorage).delete("resume-key.pdf");
    }
}
