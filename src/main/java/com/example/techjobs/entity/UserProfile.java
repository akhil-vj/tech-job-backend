package com.example.techjobs.entity;
import jakarta.persistence.*;
@Entity @Table(name="user_profiles")
public class UserProfile {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(unique=true) public Long userId;
  public String jobTitle; public int experienceYears; public String qualification;
  public String skills;            // comma separated
  public String preferredLocation; public boolean remotePreference;
  public String cvType;            // "UPLOADED" or "MANUAL"
  @Column(columnDefinition="TEXT") public String cvData; // Base64 if UPLOADED, text if MANUAL
}
