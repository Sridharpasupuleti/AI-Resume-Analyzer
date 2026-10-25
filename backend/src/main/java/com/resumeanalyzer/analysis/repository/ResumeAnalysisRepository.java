package com.resumeanalyzer.analysis.repository;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.analysis.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<ResumeAnalysis> findByIdAndUser_Id(Long id, Long userId);
}
