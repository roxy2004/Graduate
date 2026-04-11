-- 教师端「章节资料」依赖：learning_resource.chapter_id，且 section_id 需可为 NULL。
-- 可重复执行：已存在列/索引时自动跳过。
-- 在 MySQL 客户端执行：SOURCE sql/migrate_teacher_chapter_resources.sql;
USE graduate;

-- 1) section_id 改为可空（若已是 YES 则跳过）
SET @sec_nullable := (
  SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'learning_resource' AND COLUMN_NAME = 'section_id'
  LIMIT 1
);
SET @sql1 := IF(@sec_nullable = 'YES',
  'SELECT ''section_id already nullable'' AS migrate_msg',
  'ALTER TABLE learning_resource MODIFY COLUMN section_id BIGINT NULL'
);
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 2) 增加 chapter_id（若已存在则跳过）
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'learning_resource' AND COLUMN_NAME = 'chapter_id'
);
SET @sql2 := IF(@col_exists > 0,
  'SELECT ''chapter_id column already exists'' AS migrate_msg',
  'ALTER TABLE learning_resource ADD COLUMN chapter_id BIGINT NULL DEFAULT NULL COMMENT ''章节级资源 course_chapter.id'' AFTER section_id'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3) 索引（若已存在则跳过）
SET @idx_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'learning_resource' AND INDEX_NAME = 'idx_learning_resource_chapter_id'
);
SET @sql3 := IF(@idx_exists > 0,
  'SELECT ''index idx_learning_resource_chapter_id already exists'' AS migrate_msg',
  'CREATE INDEX idx_learning_resource_chapter_id ON learning_resource (chapter_id)'
);
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SELECT 'migrate_teacher_chapter_resources done' AS migrate_msg;
