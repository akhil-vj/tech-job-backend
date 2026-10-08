package com.example.techjobs.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
@Entity @Table(name="jobs",uniqueConstraints=@UniqueConstraint(columnNames={"externalJobId","source"}))
public class Job {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String externalJobId; public String source;
  @NotBlank public String title; @NotBlank public String company;
  public String location;
  @Column(length=8000) public String description;
  public String salary;
  public String requiredSkills;    // comma separated
  public int requiredExperience;   // minimum years
  public String employmentType; public String applicationUrl; public String postedDate;
  public LocalDateTime createdAt=LocalDateTime.now();
}
