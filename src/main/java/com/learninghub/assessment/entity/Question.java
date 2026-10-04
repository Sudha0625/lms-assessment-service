package com.learninghub.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;

@Entity @Table(name = "question") @Getter @Setter
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String assessmentId;
    private int number;
    private String section = "Multiple Choice";
    private int points = 1;
    private String kind = "choice";
    @Column(columnDefinition = "text") private String text;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, String>> options = new ArrayList<>();
    private String correct;
    @Column(columnDefinition = "text") private String explanation;
    private String topic;
}
