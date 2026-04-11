-- 将「已作答但耗时为 0 或未写」的练习记录补齐为随机用时（秒），便于展示与统计。
-- 执行前请备份；按需修改随机区间（下方 20～300 秒）。
USE graduate;

UPDATE learning_record
SET time_spent = FLOOR(20 + RAND() * 281)
WHERE COALESCE(time_spent, 0) = 0;

-- 查看影响行数（MySQL 8+ 可在同一客户端看 ROW_COUNT()；否则用 SELECT 预估）
-- SELECT COUNT(*) FROM learning_record WHERE COALESCE(time_spent, 0) = 0;
