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

-- 0) 小节预估时长（分钟）：初始值按小节互不相同，约 8～178
UPDATE course_section cs
SET estimated_minutes = GREATEST(8, LEAST(178,
  10
  + MOD(cs.id, 9) * 7
  + MOD(ABS(CRC32(CONCAT('em|', cs.id, '|', IFNULL(cs.course_id, 0), '|', IFNULL(cs.chapter_id, 0), '|', IFNULL(cs.sort_no, 0)))), 72)
))
WHERE cs.is_active = 1;

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

-- 4) user_learning_progress：每小节已学秒数、完成度、最近学习时间初始值互不相同
INSERT INTO user_learning_progress
(user_id, section_id, progress_percent, total_seconds, completed, last_learned_at, created_at, updated_at)
SELECT
  @uid,
  x.section_id,
  LEAST(100, GREATEST(0, ROUND(100 * LEAST(1.0, x.ratio)))) AS progress_percent,
  GREATEST(
    40,
    LEAST(
      x.estimated_minutes * 120,
      FLOOR(GREATEST(1, x.estimated_minutes) * 60 * LEAST(1.2, x.ratio))
        + MOD(ABS(CRC32(CONCAT('tsj|', x.section_id, '|', @uid))), 240)
    )
  ) AS total_seconds,
  CASE WHEN x.ratio >= 0.91 THEN 1 ELSE 0 END AS completed,
  DATE_SUB(
    DATE_SUB(
      DATE_SUB(
        NOW(),
        INTERVAL (1 + MOD(ABS(CRC32(CONCAT('lld|', x.section_id, '|', IFNULL(x.chapter_id, 0)))), 52)) DAY
      ),
      INTERVAL MOD(ABS(CRC32(CONCAT('llh|', x.section_id, '|', @uid))), 47) HOUR
    ),
    INTERVAL MOD(ABS(CRC32(CONCAT('llm|', x.section_id))), 59) MINUTE
  ) AS last_learned_at,
  NOW(),
  NOW()
FROM (
  SELECT
    cs.id AS section_id,
    cs.estimated_minutes,
    cs.chapter_id,
    LEAST(1.2, GREATEST(0.05,
      0.04 + (MOD(ABS(CRC32(CONCAT('ratio|', cs.id, '|', IFNULL(cs.chapter_id, 0), '|', IFNULL(cs.sort_no, 0), '|', IFNULL(cs.course_id, 0)))), 956) / 1000.0) * 1.12
    )) AS ratio
  FROM course_section cs
  WHERE cs.is_active = 1
) x
ORDER BY x.section_id;

-- 5) user_learning_note：每小节笔记条数 0～4 不等，正文与时间戳随条号变化
INSERT INTO user_learning_note
(user_id, section_id, note_content, note_time_sec, created_at, updated_at)
SELECT
  @uid,
  x.section_id,
  CONCAT(
    '[#', k.seq, '/', x.note_cnt, '] ',
    '[标签:', ELT(1 + MOD(x.section_id + k.seq, 5), '概念', '证明', '实现', '易错', '综合'), '] ',
    cs.title,
    ' | 自评/预估耗时比 ', ROUND(p.total_seconds / GREATEST(60, cs.estimated_minutes * 60), 2),
    ' | ',
    ELT(
      1 + MOD(ABS(CRC32(CONCAT('nbody|', x.section_id, '|', k.seq, '|', IFNULL(cs.chapter_id, 0)))), 8),
      '需二刷：例题边界仍未完全掌握。',
      '已理解主流程，准备做相关中等题巩固。',
      '疑问：复杂度分析里低阶项是否可忽略？',
      '收获：整理思维导图，准备口述复盘。',
      '卡点：递归出口与迭代写法易混。',
      '计划：本周内各做一套限时小测。',
      '提醒：与前一节符号定义冲突需注意。',
      '拓展：补充阅读两篇背景材料。'
    )
  ),
  12 + MOD(ABS(CRC32(CONCAT('nts|', x.section_id, '|', k.seq, '|', @uid))), 4180) AS note_time_sec,
  NOW(),
  NOW()
FROM (
  SELECT
    cs.id AS section_id,
    MOD(ABS(CRC32(CONCAT('ncnt|', cs.id, '|', IFNULL(cs.course_id, 0), '|', IFNULL(cs.chapter_id, 0)))), 5) AS note_cnt
  FROM course_section cs
  WHERE cs.is_active = 1
) x
INNER JOIN course_section cs ON cs.id = x.section_id
INNER JOIN user_learning_progress p ON p.section_id = x.section_id AND p.user_id = @uid
CROSS JOIN (
  SELECT 1 AS seq
  UNION ALL SELECT 2
  UNION ALL SELECT 3
  UNION ALL SELECT 4
) k
WHERE k.seq >= 1
  AND k.seq <= x.note_cnt
ORDER BY x.section_id, k.seq;

-- 6) user_learning_session：单次会话时长与预估相关且按小节独立哈希
INSERT INTO user_learning_session
(user_id, section_id, start_at, end_at, duration_sec, device_info, created_at)
SELECT
  @uid,
  cs.id,
  DATE_SUB(NOW(), INTERVAL (1 + MOD(ABS(CRC32(CONCAT('ss0|', cs.id))), 22)) DAY)
    + INTERVAL MOD(ABS(CRC32(CONCAT('ss1|', cs.id))), 5400) SECOND AS start_at,
  DATE_SUB(NOW(), INTERVAL (1 + MOD(ABS(CRC32(CONCAT('ss0|', cs.id))), 22)) DAY)
    + INTERVAL (MOD(ABS(CRC32(CONCAT('ss1|', cs.id))), 5400)
      + GREATEST(300, LEAST(6500, FLOOR(GREATEST(1, cs.estimated_minutes) * 60 * (0.22 + MOD(ABS(CRC32(CONCAT('dur|', cs.id))), 68) / 100.0))))) SECOND AS end_at,
  GREATEST(
    200,
    LEAST(
      7200,
      FLOOR(GREATEST(1, cs.estimated_minutes) * 60 * (0.22 + MOD(ABS(CRC32(CONCAT('dur|', cs.id))), 68) / 100.0))
    )
  ) AS duration_sec,
  'web',
  NOW()
FROM course_section cs
WHERE cs.is_active = 1
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
  CONCAT(
    '路线项#', t.rn,
    ' 建议学习「', t.title, '」',
    ELT(1 + MOD(t.section_id, 4), '（优先概念）', '（优先练习）', '（查漏补缺）', '（综合巩固）'),
    '；参考课时约 ', t.estimated_minutes, ' 分钟'
  ),
  1 + MOD(ABS(CRC32(CONCAT('pri|', t.section_id))), 4),
  GREATEST(8, LEAST(90, t.estimated_minutes + MOD(ABS(CRC32(CONCAT('adj|', t.section_id))), 11) - 5)),
  t.rn,
  CASE WHEN t.rn <= 2 THEN 1 ELSE 0 END,
  NOW()
FROM (
  SELECT
    cs.id AS section_id,
    cs.title,
    cs.estimated_minutes,
    ROW_NUMBER() OVER (ORDER BY cs.id) AS rn
  FROM course_section cs
  WHERE cs.is_active = 1
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
