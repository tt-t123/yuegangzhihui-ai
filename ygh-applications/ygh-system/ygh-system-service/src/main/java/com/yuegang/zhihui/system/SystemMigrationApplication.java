package com.yuegang.zhihui.system;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class SystemMigrationApplication {
    private SystemMigrationApplication() {
    }

    /**
     * 数据库迁移应用入口：以非 Web 方式启动，并禁用 Nacos 服务发现，避免迁移过程向注册中心注册实例
     */
    public static void main(String[] a) {
        try (var ignored = new SpringApplicationBuilder(SystemApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.cloud.nacos.discovery.enabled=false")
                .run(a)) {
        }
    }
}

