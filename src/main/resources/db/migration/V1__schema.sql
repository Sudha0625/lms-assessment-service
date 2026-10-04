CREATE TABLE term (
  id text PRIMARY KEY, name text NOT NULL, start_date date NOT NULL, end_date date NOT NULL,
  is_current boolean NOT NULL DEFAULT false, max_credits integer NOT NULL DEFAULT 30,
  quarter integer, academic_year text);
CREATE TABLE assessment (
  id text PRIMARY KEY,
  term_id text NOT NULL REFERENCES term(id) ON DELETE CASCADE,
  title text NOT NULL, subtitle text, subject text NOT NULL, type text NOT NULL, grade_level text,
  teacher text, color text NOT NULL DEFAULT 'blue', due_at timestamp NOT NULL,
  points integer NOT NULL DEFAULT 100, attempts_allowed integer NOT NULL DEFAULT 1,
  time_limit_minutes integer, instructions_intro text,
  instructions_steps jsonb NOT NULL DEFAULT '[]', files jsonb NOT NULL DEFAULT '[]',
  rubric jsonb NOT NULL DEFAULT '[]', related_learning jsonb NOT NULL DEFAULT '[]',
  teacher_username text);
CREATE INDEX ix_assessment_teacher ON assessment(teacher_username);
CREATE INDEX ix_assessment_term ON assessment(term_id);
CREATE TABLE question (
  id bigserial PRIMARY KEY,
  assessment_id text NOT NULL REFERENCES assessment(id) ON DELETE CASCADE,
  number integer NOT NULL, section text NOT NULL DEFAULT 'Multiple Choice',
  points integer NOT NULL DEFAULT 1, kind text NOT NULL DEFAULT 'choice',
  text text NOT NULL, options jsonb NOT NULL DEFAULT '[]', correct text NOT NULL,
  explanation text, topic text,
  UNIQUE (assessment_id, number));
CREATE TABLE attempt (
  id bigserial PRIMARY KEY,
  assessment_id text NOT NULL REFERENCES assessment(id) ON DELETE CASCADE,
  username text NOT NULL, started_at timestamp, completed_at timestamp, seconds_taken integer,
  answers jsonb NOT NULL DEFAULT '{}', correct integer NOT NULL DEFAULT 0,
  incorrect integer NOT NULL DEFAULT 0, skipped integer NOT NULL DEFAULT 0,
  percent integer NOT NULL DEFAULT 0, status text NOT NULL DEFAULT 'Completed',
  deadline_at timestamp, auto_submitted boolean NOT NULL DEFAULT false,
  submission_text text, submission_files jsonb NOT NULL DEFAULT '[]',
  points_awarded integer, feedback text, graded_by text, graded_at timestamp);
CREATE INDEX ix_attempt_assessment_status ON attempt(assessment_id, status);
CREATE INDEX ix_attempt_user ON attempt(username, assessment_id);
