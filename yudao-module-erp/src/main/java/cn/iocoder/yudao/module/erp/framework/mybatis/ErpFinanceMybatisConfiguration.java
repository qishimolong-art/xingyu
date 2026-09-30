package cn.iocoder.yudao.module.erp.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 收付款锁定查询的局部兼容配置。 */
@Configuration(proxyBeanMethods = false)
public class ErpFinanceMybatisConfiguration {

    @Bean
    public SmartInitializingSingleton erpFinanceLockOrderRegistration(MybatisPlusInterceptor interceptor) {
        // 等租户、数据权限等单例完成注册，再追加到拦截器链末尾。
        return () -> interceptor.addInnerInterceptor(new ErpFinanceLockOrderInterceptor());
    }
}
