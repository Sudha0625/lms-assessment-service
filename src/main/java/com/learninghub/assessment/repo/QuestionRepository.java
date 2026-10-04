package com.learninghub.assessment.repo;

import com.learninghub.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessmentIdOrderByNumber(String assessmentId);
    void deleteByAssessmentId(String assessmentId);
}
