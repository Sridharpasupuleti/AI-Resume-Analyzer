package com.resumeanalyzer.job.service;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.UpdateJobRequest;
import com.resumeanalyzer.job.entity.Job;
import com.resumeanalyzer.job.repository.JobRepository;
import com.resumeanalyzer.user.entity.UserAccount;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserAccountRepository userRepository;

    private JobService jobService;
    private final UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, userRepository);
    }

    @Test
    void createsManualJobForAuthenticatedUserAndNormalizesFields() {
        UserAccount user = new UserAccount("Casey", "casey@example.com", "hash");
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = jobService.create(principal, new CreateJobRequest(
                " Example Inc. ",
                " Java Engineer ",
                " Build services. ",
                " https://example.com/jobs/1 ",
                " Remote "));

        var savedJob = org.mockito.ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(savedJob.capture());
        assertEquals(user, savedJob.getValue().getUser());
        assertEquals("Example Inc.", response.companyName());
        assertEquals("Java Engineer", response.jobTitle());
        assertEquals("Build services.", response.jobDescription());
        assertEquals("https://example.com/jobs/1", response.jobUrl());
        assertEquals("Remote", response.location());
        assertEquals("MANUAL", response.source());
    }

    @Test
    void listReturnsOnlyJobsForAuthenticatedUser() {
        when(jobRepository.findAllByUser_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        assertEquals(List.of(), jobService.list(principal));

        verify(jobRepository).findAllByUser_IdOrderByCreatedAtDesc(7L);
    }

    @Test
    void getHidesJobsThatDoNotBelongToAuthenticatedUser() {
        when(jobRepository.findByIdAndUser_Id(21L, 7L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.get(principal, 21L));
    }

    @Test
    void updateChangesOnlyEditableJobFields() {
        Job job = new Job(
                new UserAccount("Casey", "casey@example.com", "hash"),
                "Old Company",
                "Old Title",
                "Old description",
                null,
                null);
        when(jobRepository.findByIdAndUser_Id(21L, 7L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        var response = jobService.update(principal, 21L, new UpdateJobRequest(
                null,
                "New Title",
                "New description",
                null,
                "Remote"));

        assertNull(response.companyName());
        assertEquals("New Title", response.jobTitle());
        assertEquals("New description", response.jobDescription());
        assertEquals("Remote", response.location());
        assertEquals("MANUAL", response.source());
        verify(jobRepository).save(job);
    }

    @Test
    void deleteDoesNotDeleteJobsOwnedByAnotherUser() {
        when(jobRepository.findByIdAndUser_Id(21L, 7L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.delete(principal, 21L));

        verify(jobRepository, never()).delete(any(Job.class));
    }
}
