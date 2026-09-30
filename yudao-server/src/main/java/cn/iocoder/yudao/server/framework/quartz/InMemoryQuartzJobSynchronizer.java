package cn.iocoder.yudao.server.framework.quartz;

import cn.iocoder.yudao.module.infra.service.job.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 内存模式 Quartz 任务同步器。
 *
 * <p>RAMJobStore 在后端重启后不会保留任务，因此需要从 infra_job 重新装载。
 * JDBC JobStore 自身会持久化任务，本同步器不会启用。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.quartz", name = "job-store-type", havingValue = "memory")
@Slf4j
public class InMemoryQuartzJobSynchronizer implements ApplicationRunner {

    private final JobService jobService;
    private final Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 同步过程会先创建再暂停数据库中已停用的任务，先让 Scheduler 待机可避免暂停前的极短竞态。
        scheduler.standby();
        try {
            jobService.syncJob();
        } finally {
            scheduler.start();
        }
        log.info("[run][内存模式 Quartz 任务已从数据库同步完成]");
    }

}
