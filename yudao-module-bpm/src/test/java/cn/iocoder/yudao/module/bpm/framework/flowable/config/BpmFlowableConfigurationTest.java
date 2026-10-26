package cn.iocoder.yudao.module.bpm.framework.flowable.config;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BpmFlowableConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            // 让限定类型的消费者先初始化，覆盖 Flowable 引擎先于线程池创建的场景。
            .withUserConfiguration(FlowableExecutorConsumerConfiguration.class, BpmFlowableConfiguration.class)
            .withBean(AdminUserApi.class, () -> mock(AdminUserApi.class))
            .withBean(BpmTaskCandidateStrategy.class, () -> mock(BpmTaskCandidateStrategy.class));

    @Test
    void resolvesApplicationExecutorBeforeInitialization() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExecutorConsumer.class).executor)
                    .isSameAs(context.getBean("applicationTaskExecutor"));
        });
    }

    @Test
    void resolvesApplicationExecutorWithSeparateImportExecutor() {
        runner.withUserConfiguration(ImportExecutorConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            ThreadPoolTaskExecutor applicationExecutor = context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor importExecutor = context.getBean("erpProductImportTaskExecutor", ThreadPoolTaskExecutor.class);
            assertThat(context.getBean(ExecutorConsumer.class).executor).isSameAs(applicationExecutor);
            assertThat(applicationExecutor).isNotSameAs(importExecutor);
            assertThat(applicationExecutor.getThreadNamePrefix()).isEqualTo("flowable-task-Executor-");
            assertThat(importExecutor.getThreadNamePrefix()).isEqualTo("erp-product-import-");
        });
    }

    @Test
    void preservesExplicitApplicationExecutor() {
        runner.withBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class, () -> {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("custom-application-");
            return executor;
        }).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExecutorConsumer.class).executor)
                    .isSameAs(context.getBean("applicationTaskExecutor"));
            assertThat(context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class).getThreadNamePrefix())
                    .isEqualTo("custom-application-");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class FlowableExecutorConsumerConfiguration {
        @Bean
        ExecutorConsumer flowableExecutorConsumer(
                @Qualifier("applicationTaskExecutor") AsyncListenableTaskExecutor executor) {
            return new ExecutorConsumer(executor);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ImportExecutorConfiguration {
        @Bean("erpProductImportTaskExecutor")
        ThreadPoolTaskExecutor importExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("erp-product-import-");
            return executor;
        }
    }

    static class ExecutorConsumer {
        final AsyncListenableTaskExecutor executor;

        ExecutorConsumer(AsyncListenableTaskExecutor executor) {
            this.executor = executor;
        }
    }
}
