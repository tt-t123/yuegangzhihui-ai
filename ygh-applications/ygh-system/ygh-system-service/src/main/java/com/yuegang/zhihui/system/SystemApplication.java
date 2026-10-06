package com.yuegang.zhihui.system;

import com.yuegang.zhihui.common.mybatis.AuditorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SystemApplication {
    public static void main(String[] a) {
        SpringApplication.run(SystemApplication.class, a);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditorProvider systemAuditorProvider() {
        return AuditorProvider.system();
    }
}
