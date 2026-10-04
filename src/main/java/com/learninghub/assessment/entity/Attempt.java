package com.learninghub.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "attempt") @Getter @Setter
public class Attempt {
    public static final String IN_PROGRESS = "InProgress", COMPLETED = "Completed",
            SUBMITTED = "Submitted", GRADED = "Graded";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String assessmentId;
    private String username;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer secondsTaken;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, String> answers = new HashMap<>();
    private int correct;
    private int incorrect;
    private int skipped;
    private int percent;
    private String status = COMPLETED;
    private LocalDateTime deadlineAt;
    private boolean autoSubmitted;
    @Column(columnDefinition = "text") private String submissionText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, Object>> submissionFiles = new ArrayList<>();
    private Integer pointsAwarded;
    @Column(columnDefinition = "text") private String feedback;
    private String gradedBy;
    private LocalDateTime gradedAt;
}
