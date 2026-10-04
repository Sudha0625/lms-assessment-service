package com.learninghub.assessment.web;

import com.learninghub.assessment.service.AssessmentService;
import com.learninghub.assessment.service.Dtos;
import com.learninghub.assessment.service.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {
    private final AssessmentService svc;
    public AssessmentController(AssessmentService svc) { this.svc = svc; }

    // ----- views (all roles) -----
    @GetMapping("/list")        public List<Map<String, Object>> list(CurrentUser u) { return svc.list(u); }
    @GetMapping("/performance") public List<Map<String, Object>> performance(CurrentUser u) { return svc.performance(u); }
    @GetMapping("/summary")     public Map<String, Object> summary(CurrentUser u) { return svc.summary(u); }

    // ----- teacher / admin: submissions and grading -----
    @GetMapping("/submissions")
    public List<Map<String, Object>> submissions(CurrentUser u, @RequestParam(required = false) String assessment) {
        return svc.submissions(u, assessment);
    }
    @GetMapping("/submissions/{attemptId}")
    public Map<String, Object> submission(CurrentUser u, @PathVariable Long attemptId) { return svc.submission(attemptId, u); }
    @PostMapping("/submissions/{attemptId}/grade")
    public Map<String, Object> grade(CurrentUser u, @PathVariable Long attemptId, @RequestBody GradeReq body) {
        return svc.grade(attemptId, u, body);
    }

    // ----- teacher / admin: authoring CRUD -----
    @PostMapping("/teacher/assessments")
    public ResponseEntity<Map<String, Object>> create(CurrentUser u, @RequestBody AssessmentReq body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.create(u, body));
    }
    @GetMapping("/teacher/assessments/{id}")
    public Map<String, Object> getForm(CurrentUser u, @PathVariable String id) { return svc.getForm(id, u); }
    @PutMapping("/teacher/assessments/{id}")
    public Map<String, Object> update(CurrentUser u, @PathVariable String id, @RequestBody AssessmentReq body) { return svc.update(id, u, body); }
    @DeleteMapping("/teacher/assessments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser u, @PathVariable String id) { svc.delete(id, u); }

    // ----- one assessment (all roles) -----
    @GetMapping("/{id}")           public Map<String, Object> details(CurrentUser u, @PathVariable String id) { return svc.details(id, u); }
    @GetMapping("/{id}/questions") public Map<String, Object> questions(CurrentUser u, @PathVariable String id) { return svc.quizQuestions(id, u); }
    @GetMapping("/{id}/review")    public Map<String, Object> review(CurrentUser u, @PathVariable String id) { return svc.review(id, u); }

    // ----- student attempts -----
    @PostMapping("/{id}/start")  public Map<String, Object> start(CurrentUser u, @PathVariable String id) { return svc.start(id, u); }
    @PutMapping("/{id}/answers") public Map<String, Object> answers(CurrentUser u, @PathVariable String id, @RequestBody AnswersReq body) { return svc.saveAnswers(id, u, body); }
    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submit(CurrentUser u, @PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.submit(id, u, Dtos.parseAnswers(body)));
    }
    @PostMapping("/{id}/assignment")
    public ResponseEntity<Map<String, Object>> assignment(CurrentUser u, @PathVariable String id, @RequestBody AssignmentReq body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.submitAssignment(id, u, body));
    }
}
