USE graduate;
SET NAMES utf8mb4;

SET @qsrc := '408高区分训练集';
SET @username := 'xwd';
SET @uid := (
  SELECT id
  FROM `user`
  WHERE username COLLATE utf8mb4_unicode_ci = @username COLLATE utf8mb4_unicode_ci
  ORDER BY id
  LIMIT 1
);
SET @uid := IFNULL(@uid, 1);

-- 当前题量
SET @base_cnt := (
  SELECT COUNT(*)
  FROM question_bank
  WHERE source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci
);

-- 仅当当前少于120时才扩容
-- 逻辑：以最早的24条为种子，生成4轮变式 => 新增96条，总计120
DROP TEMPORARY TABLE IF EXISTS tmp_seed_q;
CREATE TEMPORARY TABLE tmp_seed_q AS
SELECT
  qb.id AS seed_question_id,
  qb.content,
  qb.question_type,
  qb.options,
  qb.correct_answer,
  qb.explanation,
  qb.difficulty,
  qb.discrimination,
  qb.guess_prob,
  qb.cognitive_level,
  qb.source_tag,
  qb.source_url,
  qb.quality_score,
  qb.status,
  qb.created_by,
  ROW_NUMBER() OVER (ORDER BY qb.id) AS rn
FROM question_bank qb
WHERE qb.source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci
ORDER BY qb.id
LIMIT 24;

DROP TEMPORARY TABLE IF EXISTS tmp_variant_k;
CREATE TEMPORARY TABLE tmp_variant_k (k INT PRIMARY KEY);
INSERT INTO tmp_variant_k VALUES (1),(2),(3),(4);

INSERT INTO question_bank
(content, question_type, options, correct_answer, explanation, difficulty, discrimination, guess_prob, cognitive_level,
 source_tag, source_url, quality_score, status, created_by, created_at, updated_at)
SELECT
  CONCAT(s.content, '（训练变式', v.k, '）') AS content,
  s.question_type,
  s.options,
  s.correct_answer,
  CONCAT(COALESCE(s.explanation, ''), '；变式', v.k, '侧重边界条件与综合应用。') AS explanation,
  LEAST(0.95, s.difficulty + 0.03 * v.k) AS difficulty,
  LEAST(0.95, s.discrimination + 0.02 * v.k) AS discrimination,
  s.guess_prob,
  s.cognitive_level,
  s.source_tag,
  s.source_url,
  LEAST(0.98, s.quality_score + 0.01 * v.k) AS quality_score,
  s.status,
  s.created_by,
  NOW(),
  NOW()
FROM tmp_seed_q s
CROSS JOIN tmp_variant_k v
WHERE @base_cnt < 120
  AND NOT EXISTS (
    SELECT 1
    FROM question_bank q2
    WHERE q2.content = CONCAT(s.content, '（训练变式', v.k, '）')
      AND q2.source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci
  );

-- 为新增变式题补题目-知识点关系（继承种子题关系）
INSERT INTO question_knowledge_point_rel (question_id, kp_id, weight, created_at)
SELECT
  q_new.id AS question_id,
  rel.kp_id,
  rel.weight,
  NOW()
FROM question_bank q_new
INNER JOIN tmp_seed_q s
  ON q_new.content LIKE CONCAT(s.content, '（训练变式%）')
INNER JOIN question_knowledge_point_rel rel
  ON rel.question_id = s.seed_question_id
LEFT JOIN question_knowledge_point_rel rel_exist
  ON rel_exist.question_id = q_new.id
 AND rel_exist.kp_id = rel.kp_id
WHERE q_new.source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci
  AND rel_exist.id IS NULL;

-- 重新灌xwd学习行为数据（按全量题）
SOURCE c:/Users/XWD/IdeaProjects/Graduate/sql/seed_runtime_tables.sql;

-- 校验
SELECT COUNT(*) AS question_cnt
FROM question_bank
WHERE source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci;

SELECT
  MIN(difficulty) AS min_diff,
  MAX(difficulty) AS max_diff,
  MIN(discrimination) AS min_disc,
  MAX(discrimination) AS max_disc
FROM question_bank
WHERE source_tag COLLATE utf8mb4_unicode_ci = @qsrc COLLATE utf8mb4_unicode_ci;

SELECT
  CASE
    WHEN qb.difficulty < 0.40 THEN 'easy'
    WHEN qb.difficulty < 0.70 THEN 'medium'
    ELSE 'hard'
  END AS diff_band,
  COUNT(*) AS total_cnt,
  SUM(lr.is_correct = 1) AS correct_cnt,
  ROUND(SUM(lr.is_correct = 1) / COUNT(*), 4) AS accuracy
FROM learning_record lr
INNER JOIN question_bank qb ON qb.id = lr.question_id
WHERE lr.user_id = @uid
GROUP BY
  CASE
    WHEN qb.difficulty < 0.40 THEN 'easy'
    WHEN qb.difficulty < 0.70 THEN 'medium'
    ELSE 'hard'
  END
ORDER BY diff_band;
