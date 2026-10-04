package com.learninghub.assessment.service;

import com.learninghub.assessment.client.EnrollmentClient;
import com.learninghub.assessment.client.UserClient;
import com.learninghub.assessment.entity.*;
import com.learninghub.assessment.repo.*;
import com.learninghub.assessment.service.Dtos.*;
import com.learninghub.assessment.web.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(noRollbackFor = ResponseStatusException.class)
public class AssessmentService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US);

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final TermRepository terms;
    private final EnrollmentClient enrollment;
    private final UserClient users;

    public AssessmentService(AssessmentRepository a, QuestionRepository q, AttemptRepository at,
                             TermRepository t, EnrollmentClient e, UserClient u) {
        this.assessments = a; this.questions = q; this.attempts = at; this.terms = t; this.enrollment = e; this.users = u;
    }

    // ---------- helpers ----------
    static ResponseStatusException err(HttpStatus s, String msg) { return new ResponseStatusException(s, msg); }
    private Term currentTerm() { return terms.findByCurrentTrue().orElseThrow(() -> err(HttpStatus.NOT_FOUND, "No current term")); }
    private Assessment find(String id) { return assessments.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Unknown assessment")); }
    private void ownerOrAdmin(Assessment a, CurrentUser u) { ownerOrAdmin(a, u, "Not your assessment"); }
    private void ownerOrAdmin(Assessment a, CurrentUser u, String msg) {
        if (u.isStudent() || (u.isTeacher() && !u.username().equals(a.getTeacherUsername())))
            throw err(HttpStatus.FORBIDDEN, msg);
    }
    private void teacherOnly(CurrentUser u) { if (u.isStudent()) throw err(HttpStatus.FORBIDDEN, "Teachers and admins only"); }
    private void studentOnly(CurrentUser u) { if (!u.isStudent()) throw err(HttpStatus.FORBIDDEN, "Students only"); }
    private static String d(LocalDateTime t) { return t == null ? null : t.format(STAMP); }
    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((String) kv[i], kv[i + 1]);
        return map;
    }
    private static boolean finished(Attempt a) { return !Attempt.IN_PROGRESS.equals(a.getStatus()); }
    private static String cap(String s) { return s == null || s.isBlank() ? s : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase(); }

    private List<Assessment> visible(CurrentUser u) {
        String term = currentTerm().getId();
        if (u.isTeacher()) return assessments.findByTermIdAndTeacherUsernameOrderByDueAtDesc(term, u.username());
        List<Assessment> all = assessments.findByTermIdOrderByDueAtDesc(term);
        if (!u.isStudent()) return all;
        Set<String> subjects = enrollment.enrolledSubjects(u.username());   // null => enrollment-service down
        return subjects == null ? all : all.stream().filter(a -> subjects.contains(a.getSubject())).toList();
    }

    private Map<String, Object> base(Assessment a) {
        return m("id", a.getId(), "title", a.getTitle(), "subtitle", a.getSubtitle(), "subject", a.getSubject(),
                "type", a.getType(), "gradeLevel", a.getGradeLevel(), "due", a.getDueAt().format(DATE),
                "dueTime", a.getDueAt().format(TIME), "color", a.getColor(), "dueAt", a.getDueAt().format(ISO),
                "timeLimitMinutes", a.getTimeLimitMinutes());
    }

    private String studentStatus(Assessment a, List<Attempt> mine) {
        if (!mine.isEmpty()) {
            return switch (mine.get(0).getStatus()) {
                case Attempt.IN_PROGRESS -> "In Progress";
                case Attempt.SUBMITTED -> "Submitted";
                default -> "Completed";
            };
        }
        LocalDateTime now = LocalDateTime.now();
        if (a.getDueAt().isBefore(now)) return "Overdue";
        if (a.getDueAt().toLocalDate().equals(now.toLocalDate())) return "Due Today";
        return "Upcoming";
    }

    private static String actionFor(Assessment a, String status) {
        return switch (status) {
            case "Completed" -> "Review";
            case "Submitted" -> "View Submission";
            case "In Progress" -> "Resume";
            case "Overdue" -> "View Details";
            default -> "Assignment".equals(a.getType()) ? "Submit" : "Start";
        };
    }

    private static Optional<Integer> avg(Collection<Attempt> list) {
        return list.stream().filter(a -> Attempt.COMPLETED.equals(a.getStatus()) || Attempt.GRADED.equals(a.getStatus()))
                .mapToInt(Attempt::getPercent).average().stream().mapToInt(x -> (int) Math.round(x)).boxed().findFirst();
    }

    /** An in-progress attempt whose deadline has passed is finalised from its saved answers (auto-submitted). */
    private void finalizeExpired(CurrentUser u) {
        if (!u.isStudent()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Attempt x : attempts.findByUsernameOrderByIdDesc(u.username())) {
            if (Attempt.IN_PROGRESS.equals(x.getStatus()) && x.getDeadlineAt() != null && x.getDeadlineAt().isBefore(now))
                assessments.findById(x.getAssessmentId()).ifPresent(a -> score(x, a, x.getAnswers(), null));
        }
    }

    // ---------- views ----------
    public List<Map<String, Object>> list(CurrentUser u) {
        finalizeExpired(u);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Assessment a : visible(u)) {
            Map<String, Object> row = base(a);
            if (u.isStudent()) {
                List<Attempt> mine = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(a.getId(), u.username());
                String status = studentStatus(a, mine);
                Attempt latest = mine.isEmpty() ? null : mine.get(0);
                boolean scored = latest != null && (Attempt.COMPLETED.equals(latest.getStatus()) || Attempt.GRADED.equals(latest.getStatus()));
                row.put("status", status);
                row.put("score", scored ? latest.getPercent() + "%" : null);
                row.put("action", actionFor(a, status));
                row.put("secondsLeft", latest != null && Attempt.IN_PROGRESS.equals(latest.getStatus()) ? secondsLeft(latest) : null);
            } else {
                List<Attempt> done = attempts.findByAssessmentIdOrderByIdDesc(a.getId()).stream().filter(AssessmentService::finished).toList();
                row.put("status", a.getDueAt().isAfter(LocalDateTime.now()) ? "Open" : "Closed");
                row.put("teacher", a.getTeacher());
                row.put("teacherUsername", a.getTeacherUsername());
                row.put("submissions", done.size());
                row.put("toGrade", done.stream().filter(x -> Attempt.SUBMITTED.equals(x.getStatus())).count());
                row.put("average", avg(done).map(v -> v + "%").orElse(null));
                row.put("score", null);
                row.put("action", "View");
            }
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> performance(CurrentUser u) {
        List<Attempt> src = u.isAdmin() ? attempts.findAll() : attempts.findByUsernameOrderByIdDesc(u.username());
        Map<String, Assessment> byId = assessments.findAll().stream().collect(Collectors.toMap(Assessment::getId, x -> x));
        Set<String> seen = new HashSet<>();
        Map<String, List<Attempt>> bySubject = new LinkedHashMap<>();
        for (Attempt a : src.stream().sorted(Comparator.comparing(Attempt::getId)).toList()) {
            Assessment as = byId.get(a.getAssessmentId());
            boolean scored = Attempt.COMPLETED.equals(a.getStatus()) || Attempt.GRADED.equals(a.getStatus());
            if (as == null || !scored) continue;
            bySubject.computeIfAbsent(as.getSubject(), k -> new ArrayList<>()).add(a);
        }
        if (!u.isAdmin()) {      // keep only the latest scored attempt per assessment
            bySubject.replaceAll((k, list) -> {
                Map<String, Attempt> latest = new LinkedHashMap<>();
                list.forEach(x -> latest.put(x.getAssessmentId() + "|" + x.getUsername(), x));
                return new ArrayList<>(latest.values());
            });
        }
        List<Map<String, Object>> out = new ArrayList<>();
        bySubject.forEach((subject, list) -> out.add(m("subject", subject, "percent", avg(list).orElse(0),
                "color", byId.get(list.get(0).getAssessmentId()).getColor())));
        return out;
    }

    public Map<String, Object> summary(CurrentUser u) {
        finalizeExpired(u);
        Term t = currentTerm();
        List<Assessment> mine = visible(u);
        Map<String, Object> term = m("start", t.getStartDate().format(DATE), "name", t.getName(), "end", t.getEndDate().format(DATE));
        List<Attempt> src = u.isStudent() ? attempts.findByUsernameOrderByIdDesc(u.username())
                : attempts.findByAssessmentIdInOrderByIdDesc(mine.stream().map(Assessment::getId).toList());
        List<Attempt> done = src.stream().filter(AssessmentService::finished).toList();
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(m("label", "Total Assessments", "value", String.valueOf(mine.size()), "sub", u.isStudent() ? "This term" : u.isAdmin() ? "All teachers, this term" : "Your assessments, this term", "color", "blue", "icon", "total"));
        if (u.isStudent()) {
            stats.add(m("label", "Average Score", "value", avg(done).map(v -> v + "%").orElse("-"), "sub", "Across " + done.stream().filter(x -> !Attempt.SUBMITTED.equals(x.getStatus())).count() + " results", "color", "green", "icon", "score"));
            stats.add(m("label", "High Score", "value", String.valueOf(done.stream().filter(x -> x.getPercent() >= 90).count()), "sub", "Scored 90%+", "color", "purple", "icon", "trophy"));
            Set<String> finishedIds = done.stream().map(Attempt::getAssessmentId).collect(Collectors.toSet());
            LocalDateTime nowT = LocalDateTime.now();
            long dueToday = mine.stream().filter(x -> !finishedIds.contains(x.getId()) && x.getDueAt().isAfter(nowT)
                    && x.getDueAt().toLocalDate().equals(nowT.toLocalDate())).count();
            long dueWeek = mine.stream().filter(x -> !finishedIds.contains(x.getId()) && x.getDueAt().isAfter(nowT)
                    && x.getDueAt().isBefore(nowT.plusDays(7))).count();
            stats.add(m("label", "Upcoming This Week", "value", String.valueOf(dueWeek),
                    "sub", dueToday + " due today · " + dueWeek + " due this week", "color", "orange", "icon", "calendar"));
        } else {
            stats.add(m("label", "Submissions", "value", String.valueOf(done.size()), "sub", done.stream().map(Attempt::getUsername).distinct().count() + (done.stream().map(Attempt::getUsername).distinct().count() == 1 ? " student" : " students"), "color", "green", "icon", "score"));
            stats.add(m("label", "Average Score", "value", avg(done).map(v -> v + "%").orElse("-"), "sub", "Across all submissions", "color", "purple", "icon", "trophy"));
            long overdue = mine.stream().filter(x -> x.getDueAt().isBefore(LocalDateTime.now())).count();
            stats.add(m("label", "Overdue", "value", String.valueOf(overdue), "sub", "Past their due date", "color", "red", "icon", "alert"));
        }
        Map<String, Assessment> byId = mine.stream().collect(Collectors.toMap(Assessment::getId, x -> x, (x, y) -> x));
        List<Map<String, Object>> recent = new ArrayList<>();
        for (Attempt a : done.stream().sorted(Comparator.comparing(Attempt::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList()) {
            Assessment as = byId.get(a.getAssessmentId());
            if (as == null || recent.size() >= 4 || Attempt.SUBMITTED.equals(a.getStatus())) continue;
            Map<String, Object> r = m("title", as.getTitle(), "subject", as.getSubject(), "score", a.getPercent() + "%",
                    "date", a.getCompletedAt() == null ? null : a.getCompletedAt().format(DATE), "color", as.getColor());
            if (!u.isStudent()) r.put("student", a.getUsername());
            recent.add(r);
        }
        return m("term", term, "stats", stats, "recentResults", recent, "role", u.role());
    }

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);

    private static String remaining(Duration d) {
        if (d.isNegative() || d.isZero()) return "Closed";
        long days = d.toDays(), hours = d.toHours() % 24, mins = d.toMinutes() % 60;
        return days > 0 ? days + " days " + hours + " hours" : hours > 0 ? hours + " hours " + mins + " minutes" : mins + " minutes";
    }

    public Map<String, Object> details(String id, CurrentUser u) {
        finalizeExpired(u);
        Assessment a = find(id);
        LocalDateTime now = LocalDateTime.now();
        boolean closed = a.getDueAt().isBefore(now);
        boolean assignment = "Assignment".equals(a.getType());
        List<Attempt> mine = u.isStudent() ? attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, u.username()) : List.of();
        List<Attempt> done = mine.stream().filter(AssessmentService::finished).toList();
        Attempt open = mine.stream().filter(x -> Attempt.IN_PROGRESS.equals(x.getStatus())).findFirst().orElse(null);
        Attempt latest = mine.isEmpty() ? null : mine.get(0);

        String status = u.isStudent() ? studentStatus(a, mine) : (closed ? "Closed" : "Open");
        String action = u.isStudent() ? actionFor(a, status) : "View";
        boolean canStart = u.isStudent() && !assignment && !closed && open == null && done.size() < a.getAttemptsAllowed();
        boolean canRetake = u.isStudent() && !assignment && !closed && open == null && !done.isEmpty() && done.size() < a.getAttemptsAllowed();

        Map<String, Object> st = m("status", status, "action", action, "dueDate", a.getDueAt().format(DUE),
                "timeRemaining", remaining(Duration.between(now, a.getDueAt())),
                "secondsToDue", Math.max(0, Duration.between(now, a.getDueAt()).getSeconds()), "closed", closed,
                "points", String.valueOf(a.getPoints()), "attempts", done.size() + " of " + a.getAttemptsAllowed() + " used",
                "attemptsUsed", done.size(), "attemptsAllowed", a.getAttemptsAllowed(),
                "timeLimitMinutes", a.getTimeLimitMinutes(),
                "timeLimit", a.getTimeLimitMinutes() == null ? "Until the due date" : a.getTimeLimitMinutes() + " minutes",
                "canStart", canStart, "canRetake", canRetake);

        // Assignment submission (text, files, grade) or the quiz result of the latest finished attempt
        Attempt res = done.isEmpty() ? null : done.get(0);
        boolean graded = res != null && Attempt.GRADED.equals(res.getStatus());
        int total = assignment ? 0 : questions.findByAssessmentIdOrderByNumber(id).size();
        Map<String, Object> sub = m("open", !closed && !graded,
                "note", closed ? "Submissions closed on " + a.getDueAt().format(DUE) : "You can submit until " + a.getDueAt().format(DUE),
                "submitted", res != null, "status", res == null ? null : res.getStatus(),
                "submittedAt", res == null ? null : d(res.getCompletedAt()),
                "text", res == null ? null : res.getSubmissionText(),
                "files", res == null ? new ArrayList<>() : res.getSubmissionFiles(),
                "points", a.getPoints(), "pointsAwarded", res == null ? null : res.getPointsAwarded(),
                "percent", res == null || Attempt.SUBMITTED.equals(res.getStatus()) ? null : res.getPercent(),
                "feedback", res == null ? null : res.getFeedback(), "gradedBy", res == null ? null : res.getGradedBy(),
                "gradedAt", res == null ? null : d(res.getGradedAt()),
                "autoSubmitted", res != null && res.isAutoSubmitted(),
                "correct", res == null || assignment ? null : res.getCorrect(), "total", total);

        Map<String, Object> out = m("id", a.getId(), "title", a.getTitle(), "subject", a.getSubject(), "type", a.getType(),
                "gradeLevel", a.getGradeLevel(), "teacher", a.getTeacher(), "due", a.getDueAt().format(STAMP),
                "instructions", m("intro", a.getInstructionsIntro(), "steps", a.getInstructionsSteps()),
                "files", a.getFiles(), "questionsPreview", new ArrayList<>(), "status", st,
                "attempt", open == null ? null : attemptMap(open),
                "classInfo", m("teacher", a.getTeacher(), "gradeLevel", a.getGradeLevel(), "subject", a.getSubject()),
                "rubric", a.getRubric(), "submission", sub);
        return out;
    }

    private Map<String, Object> questionMap(Question q, int total, boolean withAnswer) {
        Map<String, Object> r = m("id", q.getId(), "number", q.getNumber(), "total", total, "section", q.getSection(),
                "points", q.getPoints(), "kind", q.getKind(), "text", q.getText(), "options", q.getOptions(), "topic", q.getTopic());
        if (withAnswer) { r.put("correct", q.getCorrect()); r.put("explanation", q.getExplanation()); }
        return r;
    }

    public Map<String, Object> quizQuestions(String id, CurrentUser u) {
        finalizeExpired(u);
        Assessment a = find(id);
        List<Question> qs = questions.findByAssessmentIdOrderByNumber(id);
        List<Map<String, Object>> qmaps = qs.stream().map(q -> questionMap(q, qs.size(), false)).toList();
        Attempt open = attempts.findFirstByAssessmentIdAndUsernameAndStatus(id, u.username(), Attempt.IN_PROGRESS).orElse(null);

        long secs = open != null ? secondsLeft(open)
                : a.getTimeLimitMinutes() == null ? 0 : a.getTimeLimitMinutes() * 60L;
        String timeRemaining = a.getTimeLimitMinutes() == null && open == null ? null
                : String.format("%d:%02d", secs / 60, secs % 60);

        Map<String, Object> out = m("id", a.getId(), "title", a.getTitle(), "type", a.getType(),
                "subtitle", a.getSubtitle() + " · " + a.getSubject() + " (" + a.getGradeLevel() + ")",
                "timeLimitMinutes", a.getTimeLimitMinutes(), "timeRemaining", timeRemaining,
                "attempt", open == null ? null : attemptMap(open),
                "totalQuestions", qs.size(), "questions", qmaps,
                "question", qmaps.isEmpty() ? null : qmaps.get(0),
                "topics", qs.stream().map(Question::getTopic).filter(Objects::nonNull).distinct().toList());
        return out;
    }

    private Map<String, Object> attemptMap(Attempt x) {
        return m("attemptId", x.getId(), "status", x.getStatus(), "startedAt", d(x.getStartedAt()),
                "deadlineAt", x.getDeadlineAt() == null ? null : x.getDeadlineAt().format(ISO),
                "secondsLeft", secondsLeft(x), "answers", x.getAnswers());
    }

    private Map<String, Object> submissionMap(Attempt x) {
        return m("attemptId", x.getId(), "status", x.getStatus(), "text", x.getSubmissionText(), "files", x.getSubmissionFiles(),
                "pointsAwarded", x.getPointsAwarded(), "feedback", x.getFeedback(), "gradedBy", x.getGradedBy(), "gradedAt", d(x.getGradedAt()));
    }

    private static Long secondsLeft(Attempt x) {
        return x.getDeadlineAt() == null ? null : Math.max(0, Duration.between(LocalDateTime.now(), x.getDeadlineAt()).getSeconds());
    }

    private static String mmss(Integer secs) { return secs == null ? null : String.format("%d:%02d", secs / 60, secs % 60); }

    private Map<String, Object> reviewMap(Assessment a, Attempt x) {
        List<Question> qs = questions.findByAssessmentIdOrderByNumber(a.getId());
        List<Map<String, Object>> rq = new ArrayList<>();
        List<Map<String, Object>> topics = new ArrayList<>();
        int earned = 0, possible = 0;
        for (Question q : qs) {
            String chosen = x.getAnswers().get(String.valueOf(q.getNumber()));
            boolean skipped = chosen == null || chosen.isBlank();
            boolean right = !skipped && chosen.equals(q.getCorrect());
            String result = skipped ? "Skipped" : right ? "Correct" : "Incorrect";
            earned += right ? q.getPoints() : 0;
            possible += q.getPoints();
            Map<String, Object> row = questionMap(q, qs.size(), true);
            row.put("chosen", chosen); row.put("result", result);
            row.put("pointsLabel", "Points: " + (right ? q.getPoints() : 0) + " / " + q.getPoints());
            rq.add(row);
            topics.add(m("n", q.getNumber(), "topic", q.getTopic(), "result", result.toLowerCase()));
        }
        Map<String, Object> out = m("id", a.getId(), "title", a.getTitle(),
                "subtitle", a.getSubtitle() + " (" + a.getGradeLevel() + ") · " + a.getSubject(),
                "completedOn", d(x.getCompletedAt() != null ? x.getCompletedAt() : x.getStartedAt()),
                "questions", qs.size(), "minutes", x.getSecondsTaken() == null ? null : Math.round(x.getSecondsTaken() / 60f),
                "results", m("correct", x.getCorrect(), "total", qs.size(), "percent", x.getPercent(), "incorrect", x.getIncorrect(),
                        "skipped", x.getSkipped(), "timeTaken", mmss(x.getSecondsTaken()), "pointsEarned", earned + " / " + possible,
                        "score", x.getPercent() + "%", "status", x.getStatus(), "autoSubmitted", x.isAutoSubmitted()),
                "questionTopics", topics, "reviewQuestion", rq.isEmpty() ? null : rq.get(0), "reviewQuestions", rq,
                "relatedLearning", a.getRelatedLearning(),
                "submission", "Assignment".equals(a.getType()) ? submissionMap(x) : null);
        out.put("attemptId", x.getId());     // extras used by the teacher's submission view
        out.put("student", x.getUsername());
        return out;
    }

    public Map<String, Object> review(String id, CurrentUser u) {
        finalizeExpired(u);
        Assessment a = find(id);
        Attempt x = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, u.username()).stream().filter(AssessmentService::finished)
                .findFirst().orElseThrow(() -> err(HttpStatus.NOT_FOUND, "No completed attempt"));
        return reviewMap(a, x);
    }

    // ---------- student attempts ----------
    public Map<String, Object> start(String id, CurrentUser u) {
        studentOnly(u);
        Assessment a = find(id);
        if ("Assignment".equals(a.getType())) throw err(HttpStatus.CONFLICT, "Assignments are submitted, not started");
        if (a.getDueAt().isBefore(LocalDateTime.now())) throw err(HttpStatus.CONFLICT, "Closed (past due)");
        Optional<Attempt> open = attempts.findFirstByAssessmentIdAndUsernameAndStatus(id, u.username(), Attempt.IN_PROGRESS);
        if (open.isPresent()) {
            Map<String, Object> r = attemptMap(open.get()); r.put("resumed", true); r.put("timeLimitMinutes", a.getTimeLimitMinutes()); return r;
        }
        long used = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, u.username()).stream().filter(AssessmentService::finished).count();
        if (used >= a.getAttemptsAllowed()) throw err(HttpStatus.CONFLICT, "No attempts left");
        Attempt x = new Attempt();
        x.setAssessmentId(id); x.setUsername(u.username()); x.setStatus(Attempt.IN_PROGRESS);
        x.setStartedAt(LocalDateTime.now());
        x.setDeadlineAt(x.getStartedAt().plusMinutes(a.getTimeLimitMinutes() == null ? 60 : a.getTimeLimitMinutes()));
        attempts.save(x);
        Map<String, Object> r = attemptMap(x); r.put("resumed", false); r.put("timeLimitMinutes", a.getTimeLimitMinutes());
        return r;
    }

    public Map<String, Object> saveAnswers(String id, CurrentUser u, AnswersReq body) {
        studentOnly(u);
        Attempt x = attempts.findFirstByAssessmentIdAndUsernameAndStatus(id, u.username(), Attempt.IN_PROGRESS)
                .orElseThrow(() -> err(HttpStatus.CONFLICT, "No attempt in progress"));
        if (x.getDeadlineAt() != null && x.getDeadlineAt().isBefore(LocalDateTime.now())) {
            score(x, find(id), x.getAnswers(), null);
            throw err(HttpStatus.CONFLICT, "Time is up");
        }
        Map<String, String> ans = body == null || body.answers() == null ? Map.of() : body.answers();
        x.setAnswers(new HashMap<>(ans));
        return m("attemptId", x.getId(), "saved", ans.size(), "secondsLeft", secondsLeft(x));
    }

    public Map<String, Object> submit(String id, CurrentUser u, AnswersReq body) {
        studentOnly(u);
        Assessment a = find(id);
        if ("Assignment".equals(a.getType())) throw err(HttpStatus.CONFLICT, "Use /assignment for assignments");
        Attempt x = attempts.findFirstByAssessmentIdAndUsernameAndStatus(id, u.username(), Attempt.IN_PROGRESS).orElse(null);
        if (x == null) {
            long used = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, u.username()).stream().filter(AssessmentService::finished).count();
            if (a.getDueAt().isBefore(LocalDateTime.now())) throw err(HttpStatus.CONFLICT, "Closed (past due)");
            if (used >= a.getAttemptsAllowed()) throw err(HttpStatus.CONFLICT, "No attempts left");
            x = new Attempt(); x.setAssessmentId(id); x.setUsername(u.username()); x.setStartedAt(LocalDateTime.now());
        }
        Map<String, String> ans = body != null && body.answers() != null ? body.answers() : x.getAnswers();
        score(x, a, ans, body == null ? null : body.secondsTaken());
        attempts.save(x);
        return m("attemptId", x.getId(), "correct", x.getCorrect(), "incorrect", x.getIncorrect(), "skipped", x.getSkipped(),
                "total", x.getCorrect() + x.getIncorrect() + x.getSkipped(), "percent", x.getPercent(), "status", x.getStatus(),
                "secondsTaken", x.getSecondsTaken(), "autoSubmitted", x.isAutoSubmitted());
    }

    private void score(Attempt x, Assessment a, Map<String, String> ans, Integer secondsTaken) {
        List<Question> qs = questions.findByAssessmentIdOrderByNumber(a.getId());
        int ok = 0, bad = 0, skip = 0;
        for (Question q : qs) {
            String c = ans.get(String.valueOf(q.getNumber()));
            if (c == null || c.isBlank()) skip++; else if (c.equals(q.getCorrect())) ok++; else bad++;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean auto = x.getDeadlineAt() != null && x.getDeadlineAt().isBefore(now);
        LocalDateTime end = auto ? x.getDeadlineAt() : now;
        x.setAnswers(new HashMap<>(ans));
        x.setCorrect(ok); x.setIncorrect(bad); x.setSkipped(skip);
        x.setPercent(qs.isEmpty() ? 0 : Math.round(ok * 100f / qs.size()));
        x.setStatus(Attempt.COMPLETED); x.setCompletedAt(end);
        x.setAutoSubmitted(auto);
        x.setSecondsTaken(secondsTaken != null ? secondsTaken
                : x.getStartedAt() == null ? null : (int) Duration.between(x.getStartedAt(), end).getSeconds());
    }

    public Map<String, Object> submitAssignment(String id, CurrentUser u, AssignmentReq body) {
        studentOnly(u);
        Assessment a = find(id);
        if (!"Assignment".equals(a.getType())) throw err(HttpStatus.CONFLICT, "Not an assignment");
        if (a.getDueAt().isBefore(LocalDateTime.now())) throw err(HttpStatus.CONFLICT, "Closed (past due)");
        Attempt prev = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, u.username()).stream().findFirst().orElse(null);
        if (prev != null && Attempt.GRADED.equals(prev.getStatus())) throw err(HttpStatus.CONFLICT, "Already graded");
        boolean again = prev != null;
        Attempt x = again ? prev : new Attempt();
        x.setAssessmentId(id); x.setUsername(u.username()); x.setStatus(Attempt.SUBMITTED);
        x.setSubmissionText(body.text()); x.setSubmissionFiles(body.files() == null ? new ArrayList<>() : body.files());
        x.setStartedAt(LocalDateTime.now()); x.setCompletedAt(LocalDateTime.now());
        attempts.save(x);
        return m("attemptId", x.getId(), "status", "Submitted", "submittedAt", d(x.getCompletedAt()),
                "files", x.getSubmissionFiles().size(), "resubmitted", again);
    }

    // ---------- teacher: submissions & grading ----------
    public List<Map<String, Object>> submissions(CurrentUser u, String assessmentId) {
        teacherOnly(u);
        List<Assessment> mine = assessmentId != null ? List.of(find(assessmentId))
                : u.isAdmin() ? assessments.findAll() : visible(u);      // admin: every assessment, any term
        mine.forEach(a -> ownerOrAdmin(a, u));
        Map<String, Assessment> byId = mine.stream().collect(Collectors.toMap(Assessment::getId, x -> x));
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Integer> quizPoints = new HashMap<>();
        for (Attempt x : attempts.findByAssessmentIdInOrderByIdDesc(byId.keySet())) {
            if (!finished(x)) continue;
            Assessment a = byId.get(x.getAssessmentId());
            boolean scored = !Attempt.SUBMITTED.equals(x.getStatus());
            out.add(m("attemptId", x.getId(), "assessmentId", a.getId(), "title", a.getTitle(), "subject", a.getSubject(),
                    "type", a.getType(), "gradeLevel", a.getGradeLevel(), "student", x.getUsername(), "status", x.getStatus(),
                    "completedOn", d(x.getCompletedAt()), "score", scored ? x.getPercent() + "%" : null,
                    "percent", scored ? x.getPercent() : null, "correct", x.getCorrect(), "incorrect", x.getIncorrect(),
                    "skipped", x.getSkipped(), "total", "Assignment".equals(a.getType()) ? 0 : x.getCorrect() + x.getIncorrect() + x.getSkipped(),
                    "secondsTaken", x.getSecondsTaken(), "autoSubmitted", x.isAutoSubmitted(),
                    "points", "Assignment".equals(a.getType()) ? a.getPoints()
                            : quizPoints.computeIfAbsent(a.getId(), k -> questions.findByAssessmentIdOrderByNumber(k).stream().mapToInt(Question::getPoints).sum()), "pointsAwarded", x.getPointsAwarded(), "files", x.getSubmissionFiles().size()));
        }
        return out;
    }

    public Map<String, Object> submission(Long attemptId, CurrentUser u) {
        teacherOnly(u);
        Attempt x = attempts.findById(attemptId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Unknown attempt"));
        Assessment a = find(x.getAssessmentId());
        ownerOrAdmin(a, u);
        return reviewMap(a, x);
    }

    public Map<String, Object> grade(Long attemptId, CurrentUser u, GradeReq body) {
        teacherOnly(u);
        Attempt x = attempts.findById(attemptId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Unknown attempt"));
        Assessment a = find(x.getAssessmentId());
        ownerOrAdmin(a, u);
        if (!"Assignment".equals(a.getType())) throw err(HttpStatus.CONFLICT, "Not an assignment submission");
        if (body == null || body.points() == null || body.points() < 0 || body.points() > a.getPoints())
            throw err(HttpStatus.BAD_REQUEST, "points must be 0.." + a.getPoints());
        x.setPointsAwarded(body.points()); x.setFeedback(body.feedback());
        x.setPercent(Math.round(body.points() * 100f / a.getPoints()));
        x.setStatus(Attempt.GRADED); x.setGradedBy(u.username()); x.setGradedAt(LocalDateTime.now());
        return m("attemptId", x.getId(), "student", x.getUsername(), "status", x.getStatus(), "pointsAwarded", x.getPointsAwarded(),
                "points", a.getPoints(), "percent", x.getPercent(), "feedback", x.getFeedback(),
                "gradedBy", x.getGradedBy(), "gradedAt", d(x.getGradedAt()));
    }

    // ---------- teacher: CRUD on assessments ----------
    public Map<String, Object> create(CurrentUser u, AssessmentReq r) {
        teacherOnly(u);
        validate(r);
        Assessment a = new Assessment();
        a.setId(uniqueId(r.title(), r.gradeLevel()));
        a.setTermId(currentTerm().getId());
        a.setTeacherUsername(u.username());
        a.setTeacher(teacherName(u.username()));
        apply(a, r);
        assessments.save(a);
        saveQuestions(a.getId(), r);
        return form(a);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForm(String id, CurrentUser u) {
        teacherOnly(u);
        Assessment a = find(id);
        ownerOrAdmin(a, u, "Not the owner");
        return form(a);
    }

    public Map<String, Object> update(String id, CurrentUser u, AssessmentReq r) {
        teacherOnly(u);
        Assessment a = find(id);
        ownerOrAdmin(a, u, "Not the owner");
        validate(r);
        apply(a, r);
        questions.deleteByAssessmentId(id);
        questions.flush();
        saveQuestions(id, r);
        return form(a);
    }

    public void delete(String id, CurrentUser u) {
        teacherOnly(u);
        Assessment a = find(id);
        ownerOrAdmin(a, u, "Not the owner");
        assessments.delete(a);      // questions and attempts go via ON DELETE CASCADE
    }

    private void validate(AssessmentReq r) {
        if (r == null || blank(r.title()) || blank(r.subject()) || blank(r.gradeLevel()) || blank(r.type()) || r.dueAt() == null)
            throw err(HttpStatus.BAD_REQUEST, "title, subject, gradeLevel, type and dueAt are required");
        if (r.questions() != null) for (QuestionReq q : r.questions()) {
            if (q.options() == null || q.options().size() < 2) throw err(HttpStatus.BAD_REQUEST, "Each question needs at least 2 options");
            if (q.options().stream().noneMatch(o -> Objects.equals(o.get("key"), q.correct())))
                throw err(HttpStatus.BAD_REQUEST, "correct must be one of the option keys");
        }
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private void apply(Assessment a, AssessmentReq r) {
        a.setTitle(r.title()); a.setSubtitle(r.subtitle()); a.setSubject(r.subject());
        a.setGradeLevel(cap(r.gradeLevel())); a.setType(r.type()); a.setDueAt(r.dueAt());
        a.setPoints(r.points() == null ? 100 : r.points());
        a.setAttemptsAllowed(r.attemptsAllowed() == null ? 1 : r.attemptsAllowed());
        a.setTimeLimitMinutes(r.timeLimitMinutes());
        a.setInstructionsIntro(r.instructionsIntro());
        a.setInstructionsSteps(r.instructionsSteps() == null ? new ArrayList<>() : r.instructionsSteps());
        a.setColor(blank(r.color()) ? "blue" : r.color());
    }

    private void saveQuestions(String assessmentId, AssessmentReq r) {
        if (r.questions() == null) return;
        int n = 1;
        for (QuestionReq qr : r.questions()) {
            Question q = new Question();
            q.setAssessmentId(assessmentId); q.setNumber(n++);
            q.setText(qr.text()); q.setOptions(qr.options()); q.setCorrect(qr.correct());
            if (qr.points() != null) q.setPoints(qr.points());
            if (qr.section() != null) q.setSection(qr.section());
            q.setTopic(qr.topic()); q.setExplanation(qr.explanation());
            questions.save(q);
        }
    }

    private String uniqueId(String title, String grade) {
        String base = (title + " " + grade).toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String id = base; int i = 2;
        while (assessments.existsById(id)) id = base + "-" + i++;
        return id;
    }

    private Map<String, Object> form(Assessment a) {
        List<Question> qs = questions.findByAssessmentIdOrderByNumber(a.getId());
        return m("id", a.getId(), "title", a.getTitle(), "subtitle", a.getSubtitle(), "subject", a.getSubject(),
                "gradeLevel", a.getGradeLevel(), "type", a.getType(), "teacher", a.getTeacher(),
                "teacherUsername", a.getTeacherUsername(), "color", a.getColor(), "dueAt", a.getDueAt().format(ISO),
                "points", a.getPoints(), "attemptsAllowed", a.getAttemptsAllowed(), "timeLimitMinutes", a.getTimeLimitMinutes(),
                "instructionsIntro", a.getInstructionsIntro(), "instructionsSteps", a.getInstructionsSteps(),
                "termId", a.getTermId(),
                "questions", qs.stream().map(q -> questionMap(q, qs.size(), true)).toList(),
                "submissions", attempts.findByAssessmentIdOrderByIdDesc(a.getId()).stream().filter(AssessmentService::finished).count());
    }

    /** Teacher display name: user-service first, then an existing assessment by the same teacher, then the username. */
    private String teacherName(String username) {
        String n = users.displayName(username);
        if (n != null) return n;
        return assessments.findFirstByTeacherUsernameAndTeacherIsNotNull(username).map(Assessment::getTeacher).orElse(username);
    }
}
