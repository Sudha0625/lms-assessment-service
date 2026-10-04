package com.learninghub.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "assessment") @Getter @Setter
public class Assessment {
    @Id private String id;
    private String termId;
    private String title;
    private String subtitle;
    private String subject;
    private String type;            // Quiz | Test | Assignment
    private String gradeLevel;
    private String teacher;         // display name
    private String color = "blue";
    private LocalDateTime dueAt;
    private int points = 100;
    private int attemptsAllowed = 1;
    private Integer timeLimitMinutes;
    @Column(columnDefinition = "text") private String instructionsIntro;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<String> instructionsSteps = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, Object>> files = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, Object>> rubric = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, Object>> relatedLearning = new ArrayList<>();
    private String teacherUsername;
}
