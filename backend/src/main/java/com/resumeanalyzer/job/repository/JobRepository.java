package com.resumeanalyzer.job.repository;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Job> findByIdAndUser_Id(Long id, Long userId);
}
