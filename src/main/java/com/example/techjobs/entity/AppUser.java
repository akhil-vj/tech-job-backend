package com.example.techjobs.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="users")
public class AppUser {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String name;
  @Column(unique=true,nullable=false) public String email;
  @JsonIgnore public String password;   // BCrypt hash, never sent to the client
  public String role="USER";            // USER or ADMIN
  public LocalDateTime createdAt=LocalDateTime.now();
}
