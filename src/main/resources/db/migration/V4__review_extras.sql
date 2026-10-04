-- Explanations for questions that have none, and related-learning links shown on the review page.
UPDATE question SET explanation = 'Option ' || correct || ' is the correct answer.' WHERE explanation IS NULL;
UPDATE assessment SET related_learning =
  '[{"icon":"video","kind":"Video","meta":"12 min","title":"Vertex Form of Quadratics"},{"icon":"practice","kind":"Practice","meta":"10 questions","title":"Practice Problems: Graphing Parabolas"}]'::jsonb
WHERE subject = 'AP Pre Calculus';
