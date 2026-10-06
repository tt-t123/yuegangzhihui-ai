package com.yuegang.zhihui.user.security;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

/** Spring配置类，用于将安全相关的类注入到 IoC 容器中 */
@Configuration(proxyBeanMethods = false)
public class UserSecurityConfiguration {

    @Bean
    TrustedUserContextResolver trustedUserContextResolver(
            @Value("${ygh.internal-request.hmac-base64}") String encodedSecret, Clock clock) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("internal request secret is malformed", malformed);
        }
        try {
            return new TrustedUserContextResolver(secret, clock);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    @Bean
    UserInternalServiceVerifier userInternalServiceVerifier(
            @Value("${ygh.internal-request.hmac-base64}") String encodedSecret) {
        byte[] secret = Base64.getDecoder().decode(encodedSecret);
        try {
            return new UserInternalServiceVerifier(secret);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }
}