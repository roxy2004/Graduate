USE graduate;
SET NAMES utf8mb4;

SET @username := 'xwd';
SET @uid := (
  SELECT id
  FROM `user`
  WHERE username COLLATE utf8mb4_unicode_ci = @username COLLATE utf8mb4_unicode_ci
  ORDER BY id
  LIMIT 1
);
SET @uid := IFNULL(@uid, 1);

-- clear runtime data for idempotent rerun
DELETE ma
FROM mistake_analysis ma
INNER JOIN learning_record lr ON lr.id = ma.record_id
WHERE lr.user_id = @uid;

DELETE FROM learning_record WHERE user_id = @uid;
DELETE FROM learner_knowledge_state WHERE user_id = @uid;
DELETE FROM user_learning_progress WHERE user_id = @uid;
DELETE FROM user_learning_note WHERE user_id = @uid;
DELETE FROM user_learning_session WHERE user_id = @uid;
DELETE lri
FROM learning_route_item lri
INNER JOIN learning_route lr ON lr.id = lri.route_id
WHERE lr.user_id = @uid;
DELETE FROM learning_route_feedback WHERE user_id = @uid;
DELETE FROM learning_route WHERE user_id = @uid;

-- 1) learning_record
INSERT INTO learning_record
(user_id, question_id, user_answer, is_correct, score, time_spent, attempt_no, answered_at, created_at)
SELECT
  @uid,
  qb.id,
  CASE
    WHEN wr.need_wrong = 0 THEN qb.correct_answer
    WHEN qb.correct_answer = 'A' THEN 'B'
    WHEN qb.correct_answer = 'B' THEN 'C'
    WHEN qb.correct_answer = 'C' THEN 'D'
    ELSE 'A'
  END AS user_answer,
  1 - wr.need_wrong AS is_correct,
  CASE WHEN wr.need_wrong = 0 THEN 5.00 ELSE 0.00 END AS score,
  40 + MOD(qb.id * 11, 260) AS time_spent,
  1 AS attempt_no,
  DATE_SUB(NOW(), INTERVAL MOD(qb.id, 14) DAY) + INTERVAL MOD(qb.id * 97, 78000) SECOND AS answered_at,
  NOW() AS created_at
FROM question_bank qb
INNER JOIN (
  SELECT
    q.id,
    CASE
      WHEN MOD(CRC32(CONCAT('xwd-seed-', q.id)), 100)
           < (100 * LEAST(0.90, GREATEST(0.10, q.difficulty * 0.62)))
      THEN 1 ELSE 0
    END AS need_wrong
  FROM question_bank q
  WHERE q.status = 1
) wr ON wr.id = qb.id
WHERE qb.status = 1;

-- 2) mistake_analysis
INSERT INTO mistake_analysis
(record_id, user_id, kp_id, error_type, weakness_score, suggestion, raw_llm_output, created_at, updated_at)
SELECT
  lr.id,
  @uid,
  qkr.kp_id,
  ELT(1 + MOD(lr.id, 4), 'concept', 'reasoning', 'careless', 'memory') AS error_type,
  ROUND(LEAST(0.95, 0.45 + qb.difficulty * 0.45), 2) AS weakness_score,
  '建议先复习对应知识点定义，再完成3道同类题。',
  NULL,
  lr.answered_at,
  NOW()
FROM learning_record lr
INNER JOIN question_bank qb ON qb.id = lr.question_id
LEFT JOIN question_knowledge_point_rel qkr ON qkr.question_id = qb.id
WHERE lr.user_id = @uid
  AND lr.is_correct = 0;

-- 3) learner_knowledge_state
INSERT INTO learner_knowledge_state
(user_id, kp_id, mastery_level, confidence, last_practiced_at, updated_at)
SELECT
  @uid,
  qkr.kp_id,
  ROUND(
    LEAST(0.98, GREATEST(0.12,
      0.32
      + 0.56 * AVG(CASE WHEN lr.is_correct = 1 THEN 1 ELSE 0 END)
      + 0.12 * (1 - AVG(qb.difficulty))
    )),
    2
  ) AS mastery_level,
  ROUND(LEAST(0.95, 0.45 + COUNT(*) / 30), 2) AS confidence,
  MAX(lr.answered_at) AS last_practiced_at,
  NOW() AS updated_at
FROM learning_record lr
INNER JOIN question_bank qb ON qb.id = lr.question_id
INNER JOIN question_knowledge_point_rel qkr ON qkr.question_id = qb.id
WHERE lr.user_id = @uid
GROUP BY qkr.kp_id;

-- 4) user_learning_progress
INSERT INTO user_learning_progress
(user_id, section_id, progress_percent, total_seconds, completed, last_learned_at, created_at, updated_at)
SELECT
  @uid,
  cs.id,
  LEAST(100, 30 + MOD(cs.id * 13, 70)) AS progress_percent,
  900 + MOD(cs.id * 83, 4800) AS total_seconds,
  CASE WHEN MOD(cs.id, 4) = 0 THEN 1 ELSE 0 END AS completed,
  DATE_SUB(NOW(), INTERVAL MOD(cs.id, 9) DAY) AS last_learned_at,
  NOW(),
  NOW()
FROM course_section cs
ORDER BY cs.id;

-- 5) user_learning_note
INSERT INTO user_learning_note
(user_id, section_id, note_content, note_time_sec, created_at, updated_at)
SELECT
  @uid,
  p.section_id,
  CONCAT('笔记：', cs.title, '；重点关注定义、复杂度、典型陷阱。'),
  60 + MOD(p.section_id * 17, 900),
  NOW(),
  NOW()
FROM user_learning_progress p
INNER JOIN course_section cs ON cs.id = p.section_id
WHERE p.user_id = @uid;

-- 6) user_learning_session
INSERT INTO user_learning_session
(user_id, section_id, start_at, end_at, duration_sec, device_info, created_at)
SELECT
  @uid,
  cs.id,
  DATE_SUB(NOW(), INTERVAL MOD(cs.id, 12) DAY) + INTERVAL MOD(cs.id * 37, 3600) SECOND,
  DATE_SUB(NOW(), INTERVAL MOD(cs.id, 12) DAY) + INTERVAL (1200 + MOD(cs.id * 29, 2600)) SECOND,
  1200 + MOD(cs.id * 29, 2600),
  'web',
  NOW()
FROM course_section cs
ORDER BY cs.id;

-- 7) learning_route + items + feedback
INSERT INTO learning_route
(user_id, title, route_type, status, generated_by, summary, created_at)
VALUES
(@uid, 'xwd 两周补强路线', 'weakness', 'active', 'demo_seed', '优先补强错题高频知识点，再进行综合训练。', NOW());

SET @route_id := LAST_INSERT_ID();

INSERT INTO learning_route_item
(route_id, item_type, item_id, reason, priority, estimated_minutes, sort_no, completed, created_at)
SELECT
  @route_id,
  'section',
  t.section_id,
  CONCAT('建议学习：', t.title),
  1 + MOD(t.section_id, 3),
  20 + MOD(t.section_id, 20),
  t.rn,
  CASE WHEN t.rn <= 2 THEN 1 ELSE 0 END,
  NOW()
FROM (
  SELECT
    cs.id AS section_id,
    cs.title,
    ROW_NUMBER() OVER (ORDER BY cs.id) AS rn
  FROM course_section cs
) t;

INSERT INTO learning_route_feedback
(route_id, user_id, rating, comment, followed, created_at)
VALUES
(@route_id, @uid, 4, '整体路线合理，建议增加图算法综合题。', 1, NOW());

-- verify runtime tables
SELECT 'learning_record' t, COUNT(*) c FROM learning_record WHERE user_id = @uid
UNION ALL
SELECT 'mistake_analysis', COUNT(*) FROM mistake_analysis WHERE user_id = @uid
UNION ALL
SELECT 'learner_knowledge_state', COUNT(*) FROM learner_knowledge_state WHERE user_id = @uid
UNION ALL
SELECT 'user_learning_progress', COUNT(*) FROM user_learning_progress WHERE user_id = @uid
UNION ALL
SELECT 'user_learning_note', COUNT(*) FROM user_learning_note WHERE user_id = @uid
UNION ALL
SELECT 'user_learning_session', COUNT(*) FROM user_learning_session WHERE user_id = @uid
UNION ALL
SELECT 'learning_route', COUNT(*) FROM learning_route WHERE user_id = @uid
UNION ALL
SELECT 'learning_route_item', COUNT(*) FROM learning_route_item WHERE route_id = @route_id
UNION ALL
SELECT 'learning_route_feedback', COUNT(*) FROM learning_route_feedback WHERE user_id = @uid;
