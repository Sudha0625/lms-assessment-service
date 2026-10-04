-- Adds the instructions, files and rubric the details contract shows for the lab report.
UPDATE assessment SET
  instructions_intro = 'In this lab report, you will investigate different chemical reactions and analyze the results. Follow the steps below, record your observations, and answer the questions at the end.',
  instructions_steps = '["Read the lab manual and safety guidelines.","Conduct the three experiments as described.","Record your observations in the provided template."]'::jsonb,
  files = '[{"icon":"pdf","meta":"PDF · 2.4 MB","name":"Lab Manual - Chemical Reactions.pdf","action":"Download"},{"icon":"doc","meta":"DOCX · 485 KB","name":"Lab Report Template.docx","action":"Download"},{"icon":"video","meta":"Video · 10 min","name":"Introduction Video","action":"Watch"}]'::jsonb,
  rubric = '[{"label":"Lab Procedure & Observations","percent":30},{"label":"Data Analysis","percent":25},{"label":"Conclusions","percent":25},{"label":"Formatting","percent":20}]'::jsonb
WHERE id = 'lab-report';
