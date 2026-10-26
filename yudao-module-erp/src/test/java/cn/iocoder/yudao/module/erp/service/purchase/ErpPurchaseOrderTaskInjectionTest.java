package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import com.mzt.logapi.service.ILogRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 覆盖真实 Spring 按名称注入，避免 Mockito 注入掩盖与日志 Bean 的名称冲突。 */
class ErpPurchaseOrderTaskInjectionTest {

    static Stream<Class<?>> taskTypes() {
        return Stream.of(ErpPurchaseOrderImportTaskServiceImpl.class,
                ErpPurchaseOrderDetailImportTaskServiceImpl.class,
                ErpPurchaseOrderExportTaskServiceImpl.class);
    }

    @ParameterizedTest
    @MethodSource("taskTypes")
    void injectErpRecordServiceWhenLogBeanHasSameFieldName(Class<?> taskType) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            ErpImportExportRecordService erpRecords = mock(ErpImportExportRecordService.class);
            ILogRecordService logRecords = mock(ILogRecordService.class);
            context.getBeanFactory().registerSingleton("erpImportExportRecordServiceImpl", erpRecords);
            context.getBeanFactory().registerSingleton("recordService", logRecords);
            for (Field field : taskType.getDeclaredFields()) {
                Resource resource = field.getAnnotation(Resource.class);
                if (resource == null || field.getType() == ErpImportExportRecordService.class) {
                    continue;
                }
                String beanName = resource.name().isEmpty() ? field.getName() : resource.name();
                context.getBeanFactory().registerSingleton(beanName, mock(field.getType()));
            }
            context.register(taskType);
            context.refresh();

            assertSame(erpRecords, ReflectionTestUtils.getField(context.getBean(taskType), "recordService"));
            assertSame(logRecords, context.getBean("recordService"));
            verifyNoInteractions(erpRecords, logRecords);
        }
    }

    @Test
    void unqualifiedResourceReproducesStartupFailure() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("erpImportExportRecordServiceImpl",
                    mock(ErpImportExportRecordService.class));
            context.getBeanFactory().registerSingleton("recordService", mock(ILogRecordService.class));
            context.register(UnqualifiedConsumer.class);

            BeanCreationException failure = assertThrows(BeanCreationException.class, context::refresh);
            assertTrue(failure.getMostSpecificCause() instanceof BeanNotOfRequiredTypeException);
        }
    }

    static class UnqualifiedConsumer {
        @Resource
        private ErpImportExportRecordService recordService;
    }
}
