package com.yuegang.zhihui.system.application;


import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.PermissionView;
import com.yuegang.zhihui.system.api.RoleView;
import com.yuegang.zhihui.system.api.UpsertPermissionRequest;
import com.yuegang.zhihui.system.api.UpsertRoleRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.*;

// 该服务负责角色与权限的增删改查管理，包含复杂的事务编排。
public class RoleAdministrationService { // 定义最终类：角色管理服务
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final TransactionTemplate tx; // 声明事务模板

    public RoleAdministrationService(DataSource dataSource) { // 构造函数
        jdbc = new JdbcTemplate(dataSource); // 初始化 JDBC 模板
        tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource)); // 初始化事务模板
    } // 构造结束

    public List<RoleView> roles() { // 方法：查询所有角色列表
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_role ORDER BY built_in DESC,code",
                (ResultSet r, int n) -> role(r.getLong(1), r.getString(2),
                        r.getString(3), r.getBoolean(4), r.getLong(5))); // 查询并按内置角色优先排序
    } // 结束

    public RoleView saveRole(String code, UpsertRoleRequest command) { // 方法：保存或更新角色
        if (!code.equals(command.code())) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 校验路径编码与请求一致性
        return tx.execute((TransactionStatus status) -> { // 执行编排事务
            Long id = jdbc.query(
                    "SELECT id FROM system_role WHERE code=?",
                    (ResultSet r) -> r.next() ? r.getLong(1) : null,
                    code
            ); // 根据编码查询角色ID
            if (id == null) { // 如果角色不存在，执行新增逻辑
                if (command.version() != 0) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 新增角色版本必须为0
                id = next(); // 生成新ID
                jdbc.update(
                        "INSERT INTO system_role(id,code,name,enabled,built_in) VALUES(?,?,?,?,FALSE)",
                        id, code, command.name(), command.enabled()
                ); // 插入记录
            } else if (jdbc.update(
                    "UPDATE system_role SET name=?,enabled=?,version=version+1 WHERE id=? AND version=? AND NOT (built_in=TRUE AND code='ADMIN' AND ?=FALSE)",
                    command.name(), command.enabled(), id, command.version(), command.enabled()
            ) == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 执行更新逻辑（含乐观锁），且禁止禁用内置超级管理员角色
            }
            // 处理权限关联：检验传入的所有权限编码是否在数据库中且均已启用
            List<Map<String, Object>> permissions = command.permissions().isEmpty()
                    ? List.of()
                    : jdbc.queryForList(
                    "SELECT id,code FROM system_permission WHERE code IN ("
                    + String.join(",", Collections.nCopies(command.permissions().size(), "?")) + ") AND enabled=TRUE",
                    command.permissions().toArray()
            );
            if (permissions.size() != command.permissions().size()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 权限数量不匹配说明有无效权限码
            }
            jdbc.update("DELETE FROM system_role_permission WHERE role_id=?", id); // 清空旧的权限关联
            for (Map<String, Object> permission : permissions) { // 更新建立新的权限关联
                jdbc.update(
                        "INSERT INTO system_role_permission(role_id,permission_id) VALUES(?,?)",
                        id, ((Number) permission.get("id")).longValue()
                );
            }
            return getRole(id); // 返回更新后角色视图
        });
    }

    public List<PermissionView> permissions() { // 方法：查询权限清单
        return jdbc.query(
                "SELECT id,code,name,resource_type,enabled FROM system_permission ORDER BY resource_type,code",
                (r, n) -> new PermissionView(
                        Long.toString(r.getLong(1)),
                        r.getString(2),
                        r.getString(3),
                        r.getString(4),
                        r.getBoolean(5)
                )
        );
    } // 结束

    public PermissionView savePermission(String code, UpsertPermissionRequest command) { // 方法：保存/更新权限点
        if (!code.equals(command.code())) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 校验一致性
        // 使用 ON DUPLICATE KEY UPDATE 简化新增/更新逻辑
        jdbc.update(
                "INSERT INTO system_permission(id,code,name,resource_type,enabled) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),resource_type=VALUES(resource_type),enabled=VALUES(enabled)",
                next(), code, command.name(), command.resourceType(), command.enabled()
        );
        return jdbc.query("SELECT id,code,name,resource_type,enabled FROM system_permission WHERE code=?", (ResultSet r) -> {
            r.next();
            return new PermissionView(
                    Long.toString(r.getLong(1)),
                    r.getString(2),
                    r.getString(3),
                    r.getString(4),
                    r.getBoolean(5)
            );
        }, code);
    }

    private RoleView getRole(long id) { // 私有方法：根据 ID 获取角色视图（含权限列表）
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_role WHERE id=?", (ResultSet r) -> {
            if (!r.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            return role(r.getLong(1), r.getString(2), r.getString(3), r.getBoolean(4), r.getLong(5));
        }, id);
    }

    private RoleView role(long id, String code, String name, boolean enabled, long version) { // 私有方法：深度组装角色（含权限点列表）
        Set<String> permissions = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT p.code FROM system_permission p JOIN system_role_permission rp " +
                        "ON rp.permission_id=p.id WHERE rp.role_id=? ORDER BY p.code", String.class, id));
        // 带权限集合的RoleView重载构造器
        return new RoleView(Long.toString(id), code, name, enabled, version, permissions);
    } // 结束

    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // 生成 64 位正整数主键
    } // 结束
}