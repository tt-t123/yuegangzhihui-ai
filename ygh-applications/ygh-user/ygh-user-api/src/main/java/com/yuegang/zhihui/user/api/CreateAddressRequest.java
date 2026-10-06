package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest( // 定义公共记录类：创建地址请求 DTO no usages
                                    @Size(max = 32) String label, // 校验：标签最大长度为32字符 no usages
                                    @NotBlank @Size(max = 80) String recipientName, // 校验：收货人姓名不能为空且最大80字符 no usages
                                    @NotBlank @Pattern(regexp = "^[0-9()+ -]{6,24}$") String recipientPhone, // 校验：电话不能为空且符合6-24位国
                                    @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String countryCode, // 校验：国家代码不能为空且必须为2位大写字母（IS
                                    @Size(max = 16) String provinceCode, // 校验：省份代码最大长度16 no usages
                                    @NotBlank @Size(max = 64) String provinceName, // 校验：城市名称不能为空且最大64字符 no usages
                                    @NotBlank @Size(max = 64) String cityName, // 校验：城市名称不能为空且最大64字符 no usages
                                    @NotBlank @Size(max = 64) String districtName, // 校验：区县名不能为空且最大64字符 no usages
                                    @NotBlank @Size(max = 500) String addressDetail, // 校验：详细地址不能为空且最大500字符 no usages
                                    @Size(max = 16) String postalCode, // 校验：邮政编码最大长度16 no usages
                                    boolean defaultAddress // 属性：是否设为默认地址 no usages
) { // 类体开始
    @Override
    public String toString() { return "CreateAddressRequest[pii=REDACTED]"; } // 重写：日志脱敏，防止将用户个人隐私（PII）泄露到日志文件
}
