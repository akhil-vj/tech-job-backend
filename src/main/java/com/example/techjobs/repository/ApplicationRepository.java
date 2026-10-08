package com.example.techjobs.repository;
import com.example.techjobs.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ApplicationRepository extends JpaRepository<JobApplication,Long> { List<JobApplication> findByUserId(Long id); boolean existsByUserIdAndJobId(Long u,Long j); }
