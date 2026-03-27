package com.eroticaforge.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 与 {@link JacksonTypeHandler} 相同的 Jackson 序列化语义，写入 PostgreSQL 时使用 {@link PGobject}{@code jsonb}，
 * 避免驱动将 JSON 当 {@code varchar} 绑定导致列类型不匹配。
 */
public class PostgresJsonbJacksonTypeHandler extends JacksonTypeHandler {

    public PostgresJsonbJacksonTypeHandler(Class<?> clazz) {
        super(clazz);
    }

    public PostgresJsonbJacksonTypeHandler(Class<?> clazz, Field field) {
        super(clazz, field);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pg = new PGobject();
        pg.setType("jsonb");
        pg.setValue(toJson(parameter));
        ps.setObject(i, pg);
    }
}
