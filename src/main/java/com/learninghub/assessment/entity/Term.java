package com.learninghub.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "term") @Getter @Setter
public class Term {
    @Id private String id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(name = "is_current") private boolean current;
    private int maxCredits;
    private Integer quarter;
    private String academicYear;
}
