-- 若希望 item_type 使用 knowledge_point 等自定义取值，可将列改为 VARCHAR（与 route_type 类似）。
-- 修改后可在 LearningRouteMaintenanceServiceImpl.ITEM_TYPE_DB 中改为 "knowledge_point"。

USE graduate;

ALTER TABLE learning_route_item
  MODIFY COLUMN item_type VARCHAR(32) NOT NULL COMMENT 'section=小节路线；knowledge_point=知识点步骤等';
