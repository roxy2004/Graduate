-- 专项学习资源：按小节主题更新为相关学习链接
-- 视频：B 站「王道计算机考研 数据结构」公开合集分 P 直链（可直接播放）
-- 文档：线性表用《Hello 算法》章节（OI Wiki 的 /ds/list/ 已 404）；其余仍用 OI Wiki 专题页
USE graduate;

UPDATE learning_resource lr
JOIN course_section cs ON cs.id = lr.section_id
SET lr.url = CASE
  WHEN lr.resource_type = 'video' AND cs.sort_no = 1 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=10'
  WHEN lr.resource_type = 'video' AND cs.sort_no = 2 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=20'
  WHEN lr.resource_type = 'video' AND cs.sort_no = 3 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=35'
  WHEN lr.resource_type = 'video' AND cs.sort_no = 4 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=50'
  WHEN lr.resource_type = 'video' AND cs.sort_no = 5 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=65'
  WHEN lr.resource_type = 'video' AND cs.sort_no = 6 THEN 'https://www.bilibili.com/video/BV1b7411N798?p=90'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 1 THEN 'https://www.hello-algo.com/chapter_array_and_linkedlist/'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 2 THEN 'https://oi-wiki.org/ds/stack/'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 3 THEN 'https://oi-wiki.org/string/kmp/'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 4 THEN 'https://oi-wiki.org/graph/tree-basic/'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 5 THEN 'https://oi-wiki.org/graph/shortest-path/'
  WHEN lr.resource_type = 'doc' AND cs.sort_no = 6 THEN 'https://oi-wiki.org/basic/sort-intro/'
  ELSE lr.url
END
WHERE lr.resource_type IN ('video', 'doc');
