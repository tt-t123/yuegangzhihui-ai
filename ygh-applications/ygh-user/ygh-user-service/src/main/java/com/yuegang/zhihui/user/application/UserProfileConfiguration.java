package com.yuegang.zhihui.user.application;

import com.yuegang.zhihui.user.domain.AddressRepository;
import com.yuegang.zhihui.user.domain.UserProfileRepository;
import com.yuegang.zhihui.user.infrastructure.AddressCipher;
import com.yuegang.zhihui.user.infrastructure.JdbcAddressRepository;
import com.yuegang.zhihui.user.infrastructure.JdbcUserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/** Spring配置类，配置用户和地址相关的 Bean */
@Configuration(proxyBeanMethods = false)
class UserProfileConfiguration {

    @Bean
    UserProfileRepository userProfileRepository(DataSource dataSource, AddressCipher cipher) {
        return new JdbcUserProfileRepository(dataSource, cipher);
    }

    @Bean
    UserProfileService userProfileService(UserProfileRepository repository) {
        return new UserProfileService(repository);
    }

    /** 注册地址脱敏加密器，使用2参数构造函数，密钥构造在AddressCipher内部完成 */
    @Bean
    AddressCipher addressCipher(@Value("${ygh.user.pii-key-base64}") String keyBase64,
                                @Value("${ygh.user.pii-key-version:1}") int version) {
        return new AddressCipher(keyBase64, version);
    }

    @Bean
    AddressRepository addressRepository(DataSource dataSource, AddressCipher cipher) {
        return new JdbcAddressRepository(dataSource, cipher);
    }

    @Bean
    UserIdGenerator userIdGenerator(@Value("${ygh.user.id-worker:2}") long worker) {
        return new UserIdGenerator(worker, Clock.systemUTC());
    }

    @Bean
    AddressService addressService(AddressRepository repository, UserIdGenerator ids) {
        return new AddressService(repository, ids);
    }

    @Bean
    OrganizationService organizationService(DataSource dataSource, UserIdGenerator ids) {
        return new OrganizationService(dataSource, ids);
    }
}
