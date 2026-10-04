package com.learninghub.assessment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Dtos {
    public record QuestionReq(String text, List<Map<String, String>> options, String correct,
                              Integer points, String topic, String section, String explanation) {}
    public record AssessmentReq(String title, String subtitle, String subject, String gradeLevel, String type,
                                LocalDateTime dueAt, Integer points, Integer attemptsAllowed, Integer timeLimitMinutes,
                                String instructionsIntro, List<String> instructionsSteps, String color,
                                List<QuestionReq> questions) {}
    public record AnswersReq(Map<String, String> answers, Integer secondsTaken) {}
    public record AssignmentReq(String text, List<Map<String, Object>> files) {}
    public record GradeReq(Integer points, String feedback) {}

    /** Accepts {"answers": {...}, "secondsTaken": n} or a bare {"1": "B", ...} map. */
    @SuppressWarnings("unchecked")
    public static AnswersReq parseAnswers(Map<String, Object> body) {
        if (body == null) return null;
        if (body.get("answers") instanceof Map<?, ?> a) {
            Map<String, String> ans = new java.util.HashMap<>();
            ((Map<String, Object>) a).forEach((k, v) -> { if (v != null) ans.put(k, String.valueOf(v)); });
            Object st = body.get("secondsTaken");
            return new AnswersReq(ans, st instanceof Number n ? n.intValue() : null);
        }
        Map<String, String> ans = new java.util.HashMap<>();
        body.forEach((k, v) -> { if (v != null && k.matches("\\d+")) ans.put(k, String.valueOf(v)); });
        return new AnswersReq(ans, null);
    }
}
