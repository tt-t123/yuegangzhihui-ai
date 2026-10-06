package com.yuegang.zhihui.system.application;


import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.DictionaryAdminView;
import com.yuegang.zhihui.system.api.SaveDictionaryItemRequest;
import com.yuegang.zhihui.system.api.SaveDictionaryTypeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

// 该服务负责系统字典类型（分类）与字典项（值）的后台管理维护
public class SystemDictionaryAdministrationService {
    // 声明模板
    private final JdbcTemplate jdbc;

    // 构造函数
    public SystemDictionaryAdministrationService(DataSource d) {
        // 初始化
        jdbc = new JdbcTemplate(d);
    }

    // 方法：查询全量字典配置（后台管理用）
    public List<DictionaryAdminView> all() {
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_dictionary_type ORDER BY code", ( r,  n) -> view(r));
    }

    @Transactional
    public DictionaryAdminView saveType(SaveDictionaryTypeRequest c) {
        validateType(c);
        int changed;
        if (c.version() == 0) {
            Boolean exists = jdbc.query("SELECT TRUE FROM system_dictionary_type WHERE code=?",
                    rs -> rs.next() ? Boolean.TRUE : Boolean.FALSE, c.code());
            if (exists) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
            changed = jdbc.update("INSERT INTO system_dictionary_type(id,code,name,enabled) VALUES(?,?,?,?)",
                    nextId(), c.code(), c.name().trim(), c.enabled());
        } else {
            changed = jdbc.update("UPDATE system_dictionary_type SET name=?,enabled=?,version=version+1 WHERE code=? AND version=?",
                    c.name().trim(), c.enabled(), c.code(), c.version());
        }

        if (changed < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 并发修改冲突报错
        }
        return byCode(c.code()); // 返回最新状态
    }

    @Transactional
    public DictionaryAdminView saveItem(String code, SaveDictionaryItemRequest c) {
        validateTypeCode(code);
        validateItem(c);
        Long typeId = jdbc.query("SELECT id FROM system_dictionary_type WHERE code=?",
                (ResultSet r) -> r.next() ? r.getLong(1) : null, code);
        if (typeId == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        int changed;
        if (c.version() == 0) {
            Boolean exists = jdbc.query("SELECT TRUE FROM system_dictionary_item WHERE type_id=? AND item_key=?",
                    rs -> rs.next() ? Boolean.TRUE : Boolean.FALSE, typeId, c.key());
            if (exists) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
            changed = jdbc.update("INSERT INTO system_dictionary_item(id,type_id,item_key,item_value,sort_order,enabled) VALUES(?,?,?,?,?,?)",
                    nextId(), typeId, c.key(), c.value().trim(), c.sortOrder(), c.enabled());
        } else {
            changed = jdbc.update("UPDATE system_dictionary_item SET item_value=?,sort_order=?,enabled=?,version=version+1 WHERE type_id=? AND item_key=? AND version=?",
                    c.value().trim(), c.sortOrder(), c.enabled(), typeId, c.key(), c.version());
        }

        if (changed < 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 并发冲突

        return byCode(code); // 返回更新后的完整字典对象
    }

    private DictionaryAdminView byCode(String code) {
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_dictionary_type WHERE code=?",
                rs -> {
                    if (!rs.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
                    return view(rs);
                }, code);
    }

    private DictionaryAdminView view(java.sql.ResultSet r) throws java.sql.SQLException { // 核心映射逻辑
        long id = r.getLong(1); // 获取主表ID
        // 级联查询该类型下的所有明细项
        var items = jdbc.query("SELECT item_key,item_value,sort_order,enabled,version FROM system_dictionary_item WHERE type_id=? ORDER BY sort_order,item_key", (ResultSet i, int n) -> new DictionaryAdminView.Item(
                i.getString(1),
                i.getString(2),
                i.getInt(3),
                i.getBoolean(4),
                i.getLong(5)
        ), id);

        return new DictionaryAdminView(
                r.getString(2),
                r.getString(3),
                r.getBoolean(4),
                r.getLong(5),
                items
        );
    } // 结束

    private static long nextId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private static void validateType(SaveDictionaryTypeRequest c) {
        if (c == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        validateTypeCode(c.code());
        if (c.name() == null || c.name().isBlank() || c.name().length() > 100 || c.version() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateTypeCode(String code) {
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateItem(SaveDictionaryItemRequest c) {
        if (c == null
                || c.key() == null || c.key().isBlank() || c.key().length() > 64
                || c.value() == null || c.value().isBlank() || c.value().length() > 500
                || c.version() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}