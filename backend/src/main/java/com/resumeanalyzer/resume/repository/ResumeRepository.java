package com.resumeanalyzer.resume.repository;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Resume> findByIdAndUser_Id(Long id, Long userId);
}
