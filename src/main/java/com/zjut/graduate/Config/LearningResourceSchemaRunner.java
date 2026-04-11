package com.zjut.graduate.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 若未手动执行 sql/migrate_teacher_chapter_resources.sql，则在启动时补齐 learning_resource.chapter_id，
 * 避免教师/学生端「章节资料」接口报 Unknown column。
 * 生产环境若禁止自动 DDL，请设置 graduate.schema.auto-ensure-learning-resource-chapter-id=false。
 */
@Component
@Order(0)
public class LearningResourceSchemaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LearningResourceSchemaRunner.class);

    private final DataSource dataSource;

    @Value("${graduate.schema.auto-ensure-learning-resource-chapter-id:true}")
    private boolean enabled;

    public LearningResourceSchemaRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            if (url == null || !url.toLowerCase().contains("mysql")) {
                return;
            }
            String catalog = conn.getCatalog();
            if (catalog == null || catalog.isEmpty()) {
                log.warn("跳过 learning_resource 结构检查：无法取得当前 catalog");
                return;
            }
            if (columnExists(conn.getMetaData(), catalog, "learning_resource", "chapter_id")) {
                return;
            }
            log.info("检测到 learning_resource 缺少 chapter_id，正在自动执行 DDL（可关闭 graduate.schema.auto-ensure-learning-resource-chapter-id）");
            try (Statement st = conn.createStatement()) {
                try {
                    st.executeUpdate("ALTER TABLE learning_resource MODIFY COLUMN section_id BIGINT NULL");
                } catch (SQLException e) {
                    log.debug("MODIFY section_id 跳过或失败: {}", e.getMessage());
                }
                st.executeUpdate("ALTER TABLE learning_resource ADD COLUMN chapter_id BIGINT NULL DEFAULT NULL "
                        + "COMMENT '章节级资源 course_chapter.id'");
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE INDEX idx_learning_resource_chapter_id ON learning_resource (chapter_id)");
            } catch (SQLException e) {
                log.debug("CREATE INDEX 跳过或失败: {}", e.getMessage());
            }
            log.info("learning_resource.chapter_id 已就绪");
        } catch (Exception e) {
            log.error("自动补齐 learning_resource.chapter_id 失败，请手动执行 sql/migrate_teacher_chapter_resources.sql。原因: {}",
                    e.getMessage());
        }
    }

    private boolean columnExists(DatabaseMetaData md, String catalog, String table, String column) throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, null, table, column)) {
            return rs.next();
        }
    }
}
