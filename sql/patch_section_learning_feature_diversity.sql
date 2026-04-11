-- 将小节「预估时长、已学秒数、完成度、最近学习时间、笔记条数」写成互不相同的初始特征（用户 xwd + 全部活跃小节）。
-- 会删除该用户全部学习笔记后按种子规则重建，以便笔记条数 0～4 与 seed_runtime_tables 一致。
-- 用法：mysql -u... -p graduate < sql/patch_section_learning_feature_diversity.sql

USE graduate;
SET NAMES utf8mb4;

SET @username := 'xwd';
SET @uid := (
  SELECT id FROM `user`
  WHERE username COLLATE utf8mb4_unicode_ci = @username COLLATE utf8mb4_unicode_ci
  ORDER BY id LIMIT 1
);
SET @uid := IFNULL(@uid, 1);

-- 1) 预估分钟：与 seed_runtime_tables 小节公式一致
UPDATE course_section cs
SET estimated_minutes = GREATEST(8, LEAST(178,
  10
  + MOD(cs.id, 9) * 7
  + MOD(ABS(CRC32(CONCAT('em|', cs.id, '|', IFNULL(cs.course_id, 0), '|', IFNULL(cs.chapter_id, 0), '|', IFNULL(cs.sort_no, 0)))), 72)
))
WHERE cs.is_active = 1;

-- 2) 学习进度：ratio + 已学秒微调 + 最近学习时间（日/时/分三层差异）
UPDATE user_learning_progress ulp
INNER JOIN (
  SELECT
    cs2.id AS sid,
    cs2.estimated_minutes,
    cs2.chapter_id,
    LEAST(1.2, GREATEST(0.05,
      0.04 + (MOD(ABS(CRC32(CONCAT('ratio|', cs2.id, '|', IFNULL(cs2.chapter_id, 0), '|', IFNULL(cs2.sort_no, 0), '|', IFNULL(cs2.course_id, 0)))), 956) / 1000.0) * 1.12
    )) AS ratio
  FROM course_section cs2
  WHERE cs2.is_active = 1
) r ON r.sid = ulp.section_id
SET
  ulp.progress_percent = LEAST(100, GREATEST(0, ROUND(100 * LEAST(1.0, r.ratio)))),
  ulp.total_seconds = GREATEST(
    40,
    LEAST(
      r.estimated_minutes * 120,
      FLOOR(GREATEST(1, r.estimated_minutes) * 60 * LEAST(1.2, r.ratio))
        + MOD(ABS(CRC32(CONCAT('tsj|', r.sid, '|', ulp.user_id))), 240)
    )
  ),
  ulp.completed = CASE WHEN r.ratio >= 0.91 THEN 1 ELSE 0 END,
  ulp.last_learned_at = DATE_SUB(
    DATE_SUB(
      DATE_SUB(
        NOW(),
        INTERVAL (1 + MOD(ABS(CRC32(CONCAT('lld|', r.sid, '|', IFNULL(r.chapter_id, 0)))), 52)) DAY
      ),
      INTERVAL MOD(ABS(CRC32(CONCAT('llh|', r.sid, '|', ulp.user_id))), 47) HOUR
    ),
    INTERVAL MOD(ABS(CRC32(CONCAT('llm|', r.sid))), 59) MINUTE
  ),
  ulp.updated_at = NOW()
WHERE ulp.user_id = @uid;

-- 3) 笔记：先清空该用户笔记，再按小节写入 0～4 条（与 seed 一致）
DELETE FROM user_learning_note WHERE user_id = @uid;

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
