-- course_section 与 course_chapter 显式关联（学生专项学习、章节笔记/资源按真实章节展示）
USE graduate;

SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_section' AND COLUMN_NAME = 'chapter_id'
);
SET @sql := IF(@col_exists > 0,
  'SELECT ''chapter_id already exists'' AS msg',
  'ALTER TABLE course_section ADD COLUMN chapter_id BIGINT NULL DEFAULT NULL COMMENT ''course_chapter.id'' AFTER course_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE course_section s
SET s.chapter_id = (
  CASE
    WHEN s.sort_no BETWEEN 1 AND 3 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 1 LIMIT 1)
    WHEN s.sort_no = 4 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 2 LIMIT 1)
    WHEN s.sort_no = 5 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 3 LIMIT 1)
    WHEN s.sort_no = 6 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 4 LIMIT 1)
    ELSE (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id ORDER BY cc.sort_no LIMIT 1)
  END
)
WHERE s.chapter_id IS NULL;

SET @idx_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_section' AND INDEX_NAME = 'idx_course_section_chapter_id'
);
SET @sql2 := IF(@idx_exists > 0,
  'SELECT 1',
  'CREATE INDEX idx_course_section_chapter_id ON course_section (chapter_id)'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
