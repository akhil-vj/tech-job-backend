package com.example.techjobs.repository;
import com.example.techjobs.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProfileRepository extends JpaRepository<UserProfile,Long> { Optional<UserProfile> findByUserId(Long id); }
