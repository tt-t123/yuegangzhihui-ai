package com.yuegang.zhihui.security;

import com.yuegang.zhihui.common.security.AccountStatusProvider;
import com.yuegang.zhihui.common.security.SessionRevocationStore;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// 会话安全契约（SessionScurity/SessionRevocationStore）单元测试类
class SessionSecurityContractTest {

    // 标注为 JUnit5 测试方法
    @Test
    // 测试方法：验证允许基础设施层提供会话撤销与账号启用状态判定
    void shouldAllowInfrastructureToProvideRevocationAndAccountState() {
        // 实例化测试用的伪会话撤销存储
        SessionRevocationStore revocations = new FakeRevocationStore();
        // 使用 Lambda 实现账号状态提供者，判定 disabled-user
        AccountStatusProvider accounts = userId -> !"disabled-user".equals(userId);

        // 撤销指定 Token ID
        revocations.revokeToken("token-1", Instant.parse("2026-07-17T09:00:00z"));
        // 撤销 user-1 在 08:00:00 前签发的所有会话
        revocations.revokeUserSessionsIssuedBefore(
                "user-1", // 用户ID
                Instant.parse("2026-07-17T08:00:00z") // 时间截止点
        ); //撤销调用结果

        assertThat(revocations.isTokenRevoked("token-1")).isTrue(); // 校验token-1已被撤销
        assertThat(accounts.isEnabled("disabled-user")).isFalse(); // 验证被禁用的用户返回false
    }

    private static final class FakeRevocationStore implements SessionRevocationStore { //静态私有内存假对象：模拟会话撤销存储基础设施

        private String revokeToken;      // 记录被撤销的 Token
        private String revokedUser;      // 记录被批量撤销会话的用户 ID
        private Instant revokeBefore;    // 记录批量撤销的时间截止点

        @Override
        public void revokeToken(String tokenId, Instant expiresArts) {  //撤销单个Token
            revokeToken = tokenId; //记录Token ID
        }

        public void  revokeUserSessionsIssuedBefore(String userID, Instant issuedBefore){ //批量撤销遭遇指定时间的公告
            revokedUser = userID; //记录用户ID
            revokeBefore = issuedBefore;//记录截止时间戳
        }

        @Override //实现接口方法
        public boolean isTokenRevoked(String tokenId){
            return tokenId.equals(revokedToken);
        }
        public String getRevokeToken() {
            return revokeToken;
        }

        @Override // 实现接口方法
        public boolean isUserSessionRevoked(String userId, Instant issueAt) {
            return userId.equals(revokedUser) && issueAt.isBefore(revokeBefore);
        }
    }

}