package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.AiProviderConfigView;
import com.yuegang.zhihui.system.api.InternalAiProviderConfig;
import com.yuegang.zhihui.system.api.UpdateAiProviderConfigRequest;
import com.yuegang.zhihui.system.security.SystemSecretCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

// 该业务类负责管理 AI 供应商（如豆包）的配置信息，包含密钥加密存储与配置变更审计。
public class AiProviderConfigService { // 定义最终类：AI 供应商配置服务
    private static final String SELECT = """
            SELECT provider,base_url,chat_model,embedding_model,web_search_enabled,api_key_ciphertext,api_key_nonce,
            version,updated_at
            FROM system_ai_provider_config WHERE config_id=1
            """; // 定义静态常量：查询单条 AI 配置的 SQL 语句

    private final JdbcTemplate jdbc; // 声明 JDBC 模板对象
    private final SystemSecretCipher secrets; // 声明密钥加密对象

    public AiProviderConfigService(DataSource dataSource, SystemSecretCipher secrets) { // 构造函数
        this.jdbc = new JdbcTemplate(dataSource); // 初始化
        this.secrets = secrets; // 初始化加密器
    } // 构造函数结束

    public AiProviderConfigView view() { // 方法：获取前端展示用的配置视图 1 usage
        return jdbc.queryForObject(SELECT, (ResultSet row, int index) -> toView( // 执行查询并转换为视图对象
                row.getString(1), row.getString(2), row.getString(3), row.getString(4),
                row.getBoolean(5), row.getString(6), row.getLong(8), row.getTimestamp(9)));
    } // 方法结束

    public InternalAiProviderConfig internal() { // 方法：获取系统内部调用的配置（含解密后密钥） 1 usage
        return jdbc.queryForObject(SELECT, (ResultSet row, int index) -> new InternalAiProviderConfig(
                row.getString(1), row.getString(2), row.getString(3), row.getString(4),
                row.getBoolean(5), secrets.decrypt(row.getString(6), row.getString(7)), row.getLong(8)));
    }

    public AiProviderConfigView update(UpdateAiProviderConfigRequest request, long operator) { // 方法：更新 AI 配置 2 usages
        validateBaseUrl(request.baseUrl()); // 校验传入的基础 URL 格式
        var current = jdbc.queryForMap(SELECT); // 获取数据库中当前的配置快照
        String oldCiphertext = (String) current.get("api_key_ciphertext"); // 获取旧的
        String ciphertext = oldCiphertext; // 默认密文不变
        String nonce = (String) current.get("api_key_nonce"); // 默认随机数不变
        if (request.apiKey() != null && !request.apiKey().isBlank()) { // 如果请求中携带了新的明文密钥
            String normalized = request.apiKey().trim(); // 去除首尾空格
            if (normalized.length() < 16) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 密钥长度不足则抛错
            var encrypted = secrets.encrypt(normalized); // 调用加密器加密密钥
            ciphertext = encrypted.ciphertext(); // 更新密文
            nonce = encrypted.nonce(); // 更新加密随机数
        } // 密钥处理结束
        int updated = jdbc.update("""
            UPDATE system_ai_provider_config SET provider=?,base_url=?,chat_model=?,embedding_model=?,
            web_search_enabled=?,api_key_ciphertext=?,api_key_nonce=?,
            updated_by=?,version=version+1
            WHERE config_id=1 AND version=?
            """, request.provider(), request.baseUrl().trim(), request.chatModel().trim(), // 执行更新SQL
                request.embeddingModel().trim(), request.webSearchEnabled(), ciphertext, nonce, operator, // 绑定参数
                request.version()); // 使用版本号进行乐观锁校验
        if (updated != 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 更新行数不为1说明版本冲突
        //计算旧配置的哈希摘要，用于审计
        String oldDigest = digest(String.join("|",
                current.get("provider").toString(),
                current.get("base_url").toString(),
                current.get("chat_model").toString(),
                current.get("embedding_model").toString(),
                current.get("web_search_enabled").toString(),
                oldCiphertext == null ? "" : oldCiphertext));
// 计算新配置的哈希摘要
        String newDigest = digest(String.join("|",
                request.provider(), request.baseUrl(),
                request.chatModel(), request.embeddingModel(), Boolean.toString(request.webSearchEnabled()),
                ciphertext == null ? "" : ciphertext));
// 将变更记录插入配置审计表
        jdbc.update("""
        INSERT INTO system_configuration_audit(
            id,config_type,config_key,old_digest,new_digest,operator_user_id) VALUES(?,?,?,?,?,?)
        """, nextId(), "AI_PROVIDER", "primary", oldDigest, newDigest, operator);
        return view(); // 返回更新后的视图

    }


    private static AiProviderConfigView toView(String provider, String baseUrl, String chatModel,
                                               String embeddingModel, boolean webSearchEnabled,
                                               String ciphertext, long version,
                                               Timestamp updatedAt) { // 内部转换方法
        boolean configured = ciphertext != null && !ciphertext.isBlank(); // 判断密钥是否已配置
        OffsetDateTime changed = updatedAt == null ? null : updatedAt.toInstant().atOffset(ZoneOffset.UTC); // 时间戳转 OffsetDateTime
        return new AiProviderConfigView(provider, baseUrl, chatModel, embeddingModel, webSearchEnabled, configured, configured ? "••••••••" : "未配置", version, changed); // 返回脱敏后的视图对象
    } // 方法结束

    private static void validateBaseUrl(String value) {
        try { // 开启校验
            URI uri = URI.create(value.trim()); // 创建 URI 对象
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null // 强制要求 HTTP 和主机名
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) { // 禁止用户信息、查询串等
                throw new IllegalArgumentException(); // 不符合要求抛出异常
            }
        } catch (RuntimeException failure) { // 捕获解析异常
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 统一抛出校验失败业务异常
        }
    }

    private static String digest(String value) { // 私有静态方法：生成数据摘要
        try { // 开启哈希计算
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256") // 使用 SHA-256 算法
                    .digest(value.getBytes(StandardCharsets.UTF_8))); // 返回十六进制哈希字符串
        } catch (Exception impossible) { // 理论上不会发生的异常处理
            throw new IllegalStateException(impossible); // 抛出非法状态异常
        }
    } // 方法结束

    private static long nextId() { // no usages
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // 利用 UUID 生成正数长整型 ID
    } // 方法结束

}

