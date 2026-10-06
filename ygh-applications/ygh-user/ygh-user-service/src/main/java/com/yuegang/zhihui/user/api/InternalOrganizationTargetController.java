package com.yuegang.zhihui.user.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.user.security.UserInternalServiceVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.List;

/** 该类用于微服务内部调用，根据组织架构（部门、职位等）查询对应的用户 ID 列表 */
@RestController // 标识为控制器类
@RequestMapping("/internal/v1/organization")
public final class InternalOrganizationTargetController { // 定义内部组织目标控制器类
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final UserInternalServiceVerifier verifier; // 声明内部服务验证器

    public InternalOrganizationTargetController(DataSource d, UserInternalServiceVerifier v) { // 构造函数初始化数据源和验证器
        jdbc = new JdbcTemplate(d);
        verifier = v;
    }

    @GetMapping("/targets")
    ApiResponse<List<String>> targets(@RequestParam String type, @RequestParam String id, HttpServletRequest r) { // 根据类型查询目标用户 ID 集合
        verifier.verify(r); // 执行内部服务间的签名验证（鉴权）
        List<String> users = switch (type) { // 根据查询类型执行不同的 SQL 逻辑
            case "DEPARTMENT" -> { // 按部门查询在职员工对应的用户ID
                long target = positive(id); // 转换并校验 ID 为长整型
                yield jdbc.queryForList(
                    "SELECT CAST(user_id AS CHAR) FROM user_employee WHERE department_id=? AND employment_status='ACTIVE'",
                    String.class, target);
            }
            case "POSITION" -> { // 按职位查询在职员工对应的用户ID
                long target = positive(id); // 转换并校验 ID 为长整型
                yield jdbc.queryForList(
                    "SELECT CAST(e.user_id AS CHAR) FROM user_employee e JOIN user_employee_position ep ON ep.employee_id=e.id WHERE ep.position_id=? AND e.employment_status='ACTIVE'",
                    String.class, target);
            }
            case "EMPLOYEE" -> { // 按员工ID/用户ID/工号查询其对应的用户ID（确保在职）
                Long target = positiveOrNull(id); // 尝试解析为数字ID，非数字时返回null以支持工号匹配
                yield jdbc.queryForList(
                    "SELECT CAST(user_id AS CHAR) FROM user_employee WHERE (id=? OR user_id=? OR employee_no=?) AND employment_status='ACTIVE'",
                    String.class, target, target, id);
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 类型不匹配抛出校验异常
        };
        return ApiResponse.success(users, TraceIdResolver.resolve(r)); // 返回查询结果及链路ID
    }

    private static long positive(String x) { // 内部静态辅助方法：将字符串解析为正数 ID
        try {
            long v = Long.parseLong(x); // 解析长整型
            if (v <= 0) throw new NumberFormatException(); // 小于等于0则视为无效格式
       return v;
        } catch (Exception e){
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static Long positiveOrNull(String value) { // 内部静态辅助方法：将字符串解析为正数 ID，解析失败返回 null
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
