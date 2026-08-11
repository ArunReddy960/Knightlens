package com.chessdna.chessdnaanalyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {
    List<AnalysisJob> findTop10ByStatusOrderByUpdatedAtDesc(String status);
}