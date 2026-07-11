package com.chessdna.chessdnaanalyzer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private int gameCount;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Column(columnDefinition = "TEXT")
    private String coachingReport;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getGameCount() { return gameCount; }
    public void setGameCount(int gameCount) { this.gameCount = gameCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getCoachingReport() { return coachingReport; }
    public void setCoachingReport(String coachingReport) { this.coachingReport = coachingReport; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}