package com.learninghub.assessment.repo;

import com.learninghub.assessment.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, String> {
    Optional<Term> findByCurrentTrue();
}
