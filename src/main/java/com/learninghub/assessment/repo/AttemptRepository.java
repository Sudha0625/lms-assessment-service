package com.learninghub.assessment.repo;

import com.learninghub.assessment.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByAssessmentIdAndUsernameOrderByIdDesc(String assessmentId, String username);
    List<Attempt> findByUsernameOrderByIdDesc(String username);
    List<Attempt> findByAssessmentIdOrderByIdDesc(String assessmentId);
    List<Attempt> findByAssessmentIdInOrderByIdDesc(Collection<String> assessmentIds);
    Optional<Attempt> findFirstByAssessmentIdAndUsernameAndStatus(String assessmentId, String username, String status);
}
