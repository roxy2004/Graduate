-- 知识点绑定专项学习小节，用于「练习错题 AI 笔记」收藏落点。
-- 若列已存在会报错，可忽略或先检查 information_schema。

USE graduate;

ALTER TABLE knowledge_point
  ADD COLUMN anchor_section_id BIGINT NULL DEFAULT NULL COMMENT '专项学习小节 course_section.id' AFTER difficulty_ref;

UPDATE knowledge_point kp
INNER JOIN course_section s ON s.is_active = 1 AND s.title = kp.name
SET kp.anchor_section_id = s.id
WHERE kp.anchor_section_id IS NULL;
