package com.example.techjobs.repository;
import com.example.techjobs.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<AppUser,Long> { Optional<AppUser> findByEmail(String e); }
