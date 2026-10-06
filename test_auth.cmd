@echo off
echo YGH_AUTH_PORT=%YGH_AUTH_PORT%
set YGH_AUTH_PORT=8081
echo YGH_AUTH_PORT=%YGH_AUTH_PORT%
E:\yuegang-zhihui-ai\apache-maven-3.9.16\bin\mvn.cmd -f ygh-platform/ygh-auth-service/pom.xml spring-boot:run "-Dspring-boot.run.main-class=com.yuegang.zhihui.auth.AuthApplication" "-Dmaven.test.skip=true" "-Dmaven.antrun.skip=true" "-Djacoco.skip=true"
