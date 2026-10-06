package com.yuegang.zhihui.system.application;


import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.SystemSettingView;
import com.yuegang.zhihui.system.api.UpdateSystemSettingRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** 该服务负责通用系统全局设置（Key-Value 格式）的管理，包含敏感数据脱敏与审计记录 */
public final class SystemSettingService {
    private final JdbcTemplate jdbc;

    public SystemSettingService(DataSource d) {
        jdbc = new JdbcTemplate(d);
    }

    public List<SystemSettingView> list() {
        return jdbc.query("SELECT setting_key,setting_value,value_type,secret,version FROM system_setting ORDER BY setting_key",
                (ResultSet r, int n) -> new SystemSettingView(
                        r.getString(1),
                        r.getBoolean(4) ? "[REDACTED]" : r.getString(2),
                        r.getString(3),
                        r.getBoolean(4),
                        r.getLong(5)
                ));
    }

    public SystemSettingView update(String key, UpdateSystemSettingRequest c, long operator) {
        if (!key.matches("[a-z][a-z0-9._-]{1,127}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String old = jdbc.query("SELECT setting_value FROM system_setting WHERE setting_key=?",
                r -> r.next() ? r.getString(1) : null, key);
        int n;

        if (old == null && c.version() == 0) {
            n = jdbc.update("INSERT INTO system_setting(setting_key,setting_value,value_type,secret,updated_by) VALUES(?,?,?,?,?)",
                    key, c.value(), c.valueType(), c.secret(), operator);
        } else {
            n = jdbc.update("UPDATE system_setting SET setting_value=?,value_type=?,secret=?,updated_by=?,version=version+1 WHERE setting_key=? AND version=?",
                    c.value(), c.valueType(), c.secret(), operator, key, c.version());
        }

        if (n != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
        }

        jdbc.update("INSERT INTO system_configuration_audit(id,config_type,config_key,old_digest,new_digest,operator_user_id) VALUES(?,?,?,?,?,?)",
                nextId(), "SETTING", key, old == null ? null : digest(old), digest(c.value()), operator);

        return jdbc.query("SELECT setting_key,setting_value,value_type,secret,version FROM system_setting WHERE setting_key=?", r -> {
            r.next();
            return new SystemSettingView(
                    r.getString(1),
                    r.getBoolean(4) ? "[REDACTED]" : r.getString(2),
                    r.getString(3),
                    r.getBoolean(4),
                    r.getLong(5)
            );
        }, key);
    }

    /** 生成SHA-256哈希摘要，用于审计日志脱敏存储 */
    private static String digest(String x) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 内部审计表唯一ID生成器，基于UUID高位转无符号long */
    private static long nextId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }
}
