-- ============================================================
-- 为 user.id 对应用户（默认 xwd）生成做题记录与专项学习相关演示数据
-- 依赖：question_bank 中已有题目（例如 source = '408高区分训练集'）
-- 执行前请确认数据库名；脚本会通过 user.id 自动解析 @uid
-- ============================================================

USE graduate;

SET @username := 'xwd';
SET @uid := (
  SELECT id
  FROM `user`
  WHERE username = @username
  ORDER BY id
  LIMIT 1
);

-- 若未找到用户名，则回退到 1（你也可直接改成目标 id）
SET @uid := IFNULL(@uid, 1);
-- 与 question_bank 中题库来源一致；若你用的不是该名称，只改这一处即可
SET @qsrc := '408高区分训练集';

-- ---------- 清理该用户旧数据（按外键/引用顺序） ----------
DELETE ma FROM mistake_analysis ma
  INNER JOIN learning_record lr ON lr.id = ma.record_id
  WHERE lr.user_id = @uid;

DELETE FROM learning_record WHERE user_id = @uid;
DELETE FROM learner_knowledge_state WHERE user_id = @uid;

DELETE lri FROM learning_route_item lri
  INNER JOIN learning_route lr ON lr.id = lri.route_id
  WHERE lr.user_id = @uid;
DELETE FROM learning_route WHERE user_id = @uid;

DELETE FROM user_learning_session WHERE user_id = @uid;

-- ---------- 1) 做题记录：难度越高，错题概率越高（可复现的伪随机） ----------
INSERT INTO learning_record (user_id, question_id, user_answer, is_correct, time_spent, created_at)
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
  25 + MOD(qb.id, 200) AS time_spent,
  DATE_SUB(NOW(), INTERVAL (MOD(qb.id, 24)) DAY) + INTERVAL (MOD(qb.id, 80000)) SECOND AS created_at
FROM question_bank qb
INNER JOIN (
  SELECT
    q.id,
    CASE
      WHEN MOD(CRC32(CONCAT('seed408', q.id)), 100)
           < (100 * LEAST(0.88, GREATEST(0.10, q.difficulty * 0.62)))
      THEN 1
      ELSE 0
    END AS need_wrong
  FROM question_bank q
  WHERE q.source = @qsrc
) wr ON wr.id = qb.id
WHERE qb.source = @qsrc;

-- ---------- 2) 错题分析（仅错题） ----------
INSERT INTO mistake_analysis (record_id, knowledge_point, error_type, suggestion, raw_llm_output, created_at)
SELECT
  lr.id,
  COALESCE(kp.name, CONCAT('KP', qb.knowledge_point_ids)),
  ELT(1 + MOD(lr.id, 3), '概念混淆', '推理缺失', '审题偏差'),
  '对照教材例题复盘，并完成同知识点变式训练。',
  NULL,
  lr.created_at
FROM learning_record lr
INNER JOIN question_bank qb ON qb.id = lr.question_id
LEFT JOIN knowledge_point kp ON kp.id = CAST(qb.knowledge_point_ids AS UNSIGNED)
WHERE lr.user_id = @uid
  AND lr.is_correct = 0;

-- ---------- 3) 学习者知识点掌握度（按题目表上的 kp 汇总） ----------
INSERT INTO learner_knowledge_state (user_id, kp_id, mastery_level, last_practiced_at, updated_at)
SELECT
  @uid,
  CAST(qb.knowledge_point_ids AS UNSIGNED) AS kp_id,
  ROUND(
    LEAST(0.98, GREATEST(0.12,
      0.35
      + 0.55 * AVG(CASE WHEN lr.is_correct = 1 THEN 1 ELSE 0 END)
      + 0.10 * (1 - AVG(qb.difficulty))
    )),
    4
  ) AS mastery_level,
  MAX(lr.created_at) AS last_practiced_at,
  NOW() AS updated_at
FROM learning_record lr
INNER JOIN question_bank qb ON qb.id = lr.question_id
WHERE lr.user_id = @uid
  AND qb.knowledge_point_ids IS NOT NULL
  AND qb.knowledge_point_ids REGEXP '^[0-9]+$'
GROUP BY CAST(qb.knowledge_point_ids AS UNSIGNED);

-- ---------- 4) 专项学习会话（依赖 course_section 有数据；若无则本段插入 0 行）
--     时长与预估分钟、小节多维哈希挂钩，避免 id 线性规律 ----------
INSERT INTO user_learning_session (user_id, section_id, start_at, end_at, duration_sec, device_info, created_at)
SELECT
  @uid,
  cs.id,
  DATE_SUB(NOW(), INTERVAL (1 + MOD(ABS(CRC32(CONCAT('u1ss0|', cs.id))), 18)) DAY),
  DATE_SUB(NOW(), INTERVAL (1 + MOD(ABS(CRC32(CONCAT('u1ss0|', cs.id))), 18)) DAY)
    + INTERVAL GREATEST(5, 8 + MOD(ABS(CRC32(CONCAT('u1ss1|', cs.id))), 55)) MINUTE,
  GREATEST(
    240,
    LEAST(
      5400,
      FLOOR(GREATEST(1, IFNULL(cs.estimated_minutes, 20)) * 60 * (0.2 + MOD(ABS(CRC32(CONCAT('u1dur|', cs.id))), 72) / 100.0))
    )
  ),
  'web',
  DATE_SUB(NOW(), INTERVAL (1 + MOD(ABS(CRC32(CONCAT('u1ss0|', cs.id))), 18)) DAY)
FROM course_section cs
WHERE cs.is_active = 1
ORDER BY cs.id
LIMIT 18;

-- ---------- 5) 学习路线推荐（首页/预测页） ----------
INSERT INTO learning_route (user_id, title, route_type, status, generated_by, summary, created_at)
VALUES (
  @uid,
  '基于薄弱知识点的两周补强路线',
  'weakness',
  'active',
  'demo_seed',
  '优先巩固错题率较高的知识点小节，穿插中等难度综合题。',
  NOW()
);

SET @route_id := LAST_INSERT_ID();

INSERT INTO learning_route_item (route_id, item_type, item_id, reason, priority, estimated_minutes, sort_no, completed)
SELECT
  @route_id,
  'section',
  t.section_id,
  CONCAT(
    '错题指向：巩固「', t.title, '」',
    ELT(1 + MOD(t.section_id, 3), '（概念+例题）', '（变式训练）', '（限时复盘）'),
    '；建议投入约 ', t.estimated_minutes, ' 分钟'
  ),
  1 + MOD(ABS(CRC32(CONCAT('u1pri|', t.section_id))), 4),
  GREATEST(10, LEAST(75, t.estimated_minutes + MOD(ABS(CRC32(CONCAT('u1adj|', t.section_id))), 9) - 4)),
  t.sort_no,
  CASE WHEN MOD(ABS(CRC32(CONCAT('u1cmp|', t.section_id))), 5) = 0 THEN 1 ELSE 0 END
FROM (
  SELECT
    cs.id AS section_id,
    cs.title,
    cs.estimated_minutes,
    ROW_NUMBER() OVER (ORDER BY cs.id) AS sort_no
  FROM course_section cs
  WHERE cs.is_active = 1
  ORDER BY cs.id
  LIMIT 6
) t;

-- ---------- 校验 ----------
SELECT COUNT(*) AS learning_record_cnt FROM learning_record WHERE user_id = @uid;
SELECT SUM(is_correct = 1) AS correct_cnt, SUM(is_correct = 0) AS wrong_cnt FROM learning_record WHERE user_id = @uid;
SELECT COUNT(*) AS mistake_analysis_cnt FROM mistake_analysis ma
  INNER JOIN learning_record lr ON lr.id = ma.record_id WHERE lr.user_id = @uid;
SELECT COUNT(*) AS kp_state_cnt FROM learner_knowledge_state WHERE user_id = @uid;
SELECT COUNT(*) AS session_cnt FROM user_learning_session WHERE user_id = @uid;
SELECT id, title FROM learning_route WHERE user_id = @uid ORDER BY created_at DESC LIMIT 1;
