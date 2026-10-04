INSERT INTO term VALUES ('q1-2026','Q1 · Fall 2026','2026-08-24','2026-10-23',true,30,1,'2026-27');
INSERT INTO term VALUES ('q2-2026','Q2 · Winter 2026','2026-10-26','2027-01-15',false,30,2,'2026-27');

INSERT INTO assessment (id,term_id,title,subtitle,subject,type,grade_level,teacher,color,due_at,points,attempts_allowed,time_limit_minutes,instructions_intro,instructions_steps,teacher_username) VALUES
('unit1-quiz','q1-2026','Unit 1 Quiz','Foundations of Algebra','AP Pre Calculus','Quiz','Junior','Dr John Anderson','blue','2026-08-30 23:59',3,1,20,'Answer every question. One attempt.','["Read each question carefully.","Pick the best answer."]','janderson'),
('unit2-quiz','q1-2026','Unit 2 Quiz','Functions and Graphs','AP Pre Calculus','Quiz','Junior','Dr John Anderson','blue','2026-10-15 23:59',3,2,30,'Two attempts allowed.','["Pick the best answer."]','janderson'),
('lab-safety','q1-2026','Lab Safety Quiz','Safety Fundamentals','AP Chemistry','Quiz','Junior','Dr Priya Patel','red','2026-10-12 23:59',3,1,20,'Safety first.','["Pick the best answer."]','ppatel'),
('lab-report','q1-2026','Lab Report: Chemical Reactions','Reactions','AP Chemistry','Assignment','Junior','Dr Priya Patel','red','2026-10-20 23:59',100,1,NULL,'Investigate reactions and analyse the results.','["Read the lab manual","Record observations","Answer the questions"]','ppatel');

INSERT INTO question (assessment_id,number,section,points,kind,text,options,correct,explanation,topic) VALUES
('unit1-quiz',1,'Multiple Choice',1,'choice','Which equation has slope 2 and y-intercept 1?','[{"key":"A","text":"y = 2x + 1"},{"key":"B","text":"y = x + 2"},{"key":"C","text":"y = 2x - 1"},{"key":"D","text":"y = x - 1"}]','A','Slope-intercept form is y = mx + b.','Linear equations'),
('unit1-quiz',2,'Multiple Choice',1,'choice','Slope of the line through (0,0) and (2,6)?','[{"key":"A","text":"2"},{"key":"B","text":"3"},{"key":"C","text":"6"},{"key":"D","text":"12"}]','B',NULL,'Slope'),
('unit1-quiz',3,'Multiple Choice',1,'choice','What is f(3) if f(x)=x^2?','[{"key":"A","text":"6"},{"key":"B","text":"9"},{"key":"C","text":"3"},{"key":"D","text":"12"}]','B',NULL,'Functions'),
('unit2-quiz',1,'Multiple Choice',1,'choice','The domain of f(x)=sqrt(x) is?','[{"key":"A","text":"x>=0"},{"key":"B","text":"all reals"},{"key":"C","text":"x>0"},{"key":"D","text":"x<=0"}]','A',NULL,'Domain'),
('unit2-quiz',2,'Multiple Choice',1,'choice','Which is an even function?','[{"key":"A","text":"x^3"},{"key":"B","text":"x^2"},{"key":"C","text":"x"},{"key":"D","text":"2^x"}]','B',NULL,'Symmetry'),
('unit2-quiz',3,'Multiple Choice',1,'choice','Inverse of f(x)=x+5?','[{"key":"A","text":"x-5"},{"key":"B","text":"5x"},{"key":"C","text":"x/5"},{"key":"D","text":"-x-5"}]','A',NULL,'Inverses'),
('lab-safety',1,'Multiple Choice',1,'choice','Required eye protection in lab?','[{"key":"A","text":"Glasses"},{"key":"B","text":"Goggles"},{"key":"C","text":"None"},{"key":"D","text":"Sunglasses"}]','B',NULL,'PPE'),
('lab-safety',2,'Multiple Choice',1,'choice','You spill acid. First action?','[{"key":"A","text":"Ignore"},{"key":"B","text":"Tell the teacher"},{"key":"C","text":"Leave"},{"key":"D","text":"Wipe with a paper towel"}]','B',NULL,'Spills'),
('lab-safety',3,'Multiple Choice',1,'choice','Where do you dispose of broken glass?','[{"key":"A","text":"Trash"},{"key":"B","text":"Sink"},{"key":"C","text":"Glass bin"},{"key":"D","text":"Floor"}]','C',NULL,'Disposal');

INSERT INTO attempt (assessment_id,username,started_at,completed_at,seconds_taken,answers,correct,incorrect,skipped,percent,status) VALUES
('unit1-quiz','student1','2026-08-30 20:00','2026-08-30 20:15',900,'{"1":"A","2":"B","3":"B"}',3,0,0,100,'Completed'),
('unit2-quiz','student1','2026-09-15 19:00','2026-09-15 19:20',1200,'{"1":"A","2":"B","3":"C"}',2,1,0,67,'Completed');
