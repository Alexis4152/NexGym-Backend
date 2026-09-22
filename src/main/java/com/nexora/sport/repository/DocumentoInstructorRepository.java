package com.nexora.sport.repository;

import com.nexora.sport.model.DocumentoInstructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoInstructorRepository extends JpaRepository<DocumentoInstructor, Long> {
    List<DocumentoInstructor> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);
}
