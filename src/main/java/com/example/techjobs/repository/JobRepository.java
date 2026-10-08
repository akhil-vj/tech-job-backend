package com.example.techjobs.repository;
import com.example.techjobs.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface JobRepository extends JpaRepository<Job,Long> { Optional<Job> findByExternalJobIdAndSource(String e,String s); }
