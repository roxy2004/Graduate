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
 * 为 course_section 增加 chapter_id 并按原 sort_no 规则回填，使教师新建章节、小节资源与学生专项学习一致。
 */
@Component
@Order(1)
public class CourseSectionChapterSchemaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourseSectionChapterSchemaRunner.class);

    private final DataSource dataSource;

    @Value("${graduate.schema.auto-ensure-course-section-chapter-id:true}")
    private boolean enabled;

    public CourseSectionChapterSchemaRunner(DataSource dataSource) {
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
                return;
            }
            DatabaseMetaData md = conn.getMetaData();
            if (columnExists(md, catalog, "course_section", "chapter_id")) {
                backfillNullChapterIds(conn);
                return;
            }
            log.info("检测到 course_section 缺少 chapter_id，正在自动 ADD 并回填（可关闭 graduate.schema.auto-ensure-course-section-chapter-id）");
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE course_section ADD COLUMN chapter_id BIGINT NULL DEFAULT NULL "
                        + "COMMENT '关联 course_chapter.id' AFTER course_id");
            }
            backfillNullChapterIds(conn);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE INDEX idx_course_section_chapter_id ON course_section (chapter_id)");
            } catch (SQLException e) {
                log.debug("CREATE INDEX idx_course_section_chapter_id: {}", e.getMessage());
            }
            log.info("course_section.chapter_id 已就绪并完成回填");
        } catch (Exception e) {
            log.error("自动处理 course_section.chapter_id 失败，请手动执行 sql/migrate_course_section_chapter_id.sql。原因: {}",
                    e.getMessage());
        }
    }

    private void backfillNullChapterIds(Connection conn) throws SQLException {
        String sql = "UPDATE course_section s SET s.chapter_id = (" +
                "CASE " +
                "  WHEN s.sort_no BETWEEN 1 AND 3 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 1 LIMIT 1) " +
                "  WHEN s.sort_no = 4 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 2 LIMIT 1) " +
                "  WHEN s.sort_no = 5 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 3 LIMIT 1) " +
                "  WHEN s.sort_no = 6 THEN (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id AND cc.sort_no = 4 LIMIT 1) " +
                "  ELSE (SELECT id FROM course_chapter cc WHERE cc.course_id = s.course_id ORDER BY cc.sort_no LIMIT 1) " +
                "END) " +
                "WHERE s.chapter_id IS NULL";
        try (Statement st = conn.createStatement()) {
            int n = st.executeUpdate(sql);
            if (n > 0) {
                log.info("已回填 course_section.chapter_id 共 {} 行", n);
            }
        }
    }

    private boolean columnExists(DatabaseMetaData md, String catalog, String table, String column) throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, null, table, column)) {
            return rs.next();
        }
    }
}
