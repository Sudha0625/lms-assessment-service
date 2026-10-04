package com.learninghub.assessment.repo;

import com.learninghub.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, String> {
    List<Assessment> findByTermIdOrderByDueAtDesc(String termId);
    List<Assessment> findByTermIdAndTeacherUsernameOrderByDueAtDesc(String termId, String teacherUsername);
    Optional<Assessment> findFirstByTeacherUsernameAndTeacherIsNotNull(String teacherUsername);
}
