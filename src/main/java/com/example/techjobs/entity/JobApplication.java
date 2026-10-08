package com.example.techjobs.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="applications",uniqueConstraints=@UniqueConstraint(columnNames={"userId","jobId"}))
public class JobApplication {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long userId; public Long jobId;
  public LocalDateTime appliedAt=LocalDateTime.now();
  public String status="APPLIED";  // APPLIED, UNDER_REVIEW, SHORTLISTED, REJECTED, SELECTED
}
