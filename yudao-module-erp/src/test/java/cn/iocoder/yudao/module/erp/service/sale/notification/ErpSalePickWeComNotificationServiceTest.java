package cn.iocoder.yudao.module.erp.service.sale.notification;

import cn.hutool.core.util.ReflectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryPickTaskMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.framework.wecom.config.ErpSalePickWeComNotificationProperties;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.wecom.WeComClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpSalePickWeComNotificationServiceTest extends BaseMockitoUnitTest {

    private ErpSalePickWeComNotificationService service;
    private ErpSalePickWeComNotificationProperties properties;

    @Mock
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private WeComClientService weComClientService;

    @BeforeEach
    void setUp() {
        properties = new ErpSalePickWeComNotificationProperties();
        properties.setClientKey("sale-pick");
        properties.setEntryUrl("http://erp.scxyltd.com/wecom-login.html");
        service = new ErpSalePickWeComNotificationService();
        ReflectUtil.setFieldValue(service, "properties", properties);
        ReflectUtil.setFieldValue(service, "pickTaskMapper", pickTaskMapper);
        ReflectUtil.setFieldValue(service, "warehouseService", warehouseService);
        ReflectUtil.setFieldValue(service, "adminUserApi", adminUserApi);
        ReflectUtil.setFieldValue(service, "weComClientService", weComClientService);
    }

    @Test
    void sendTaskCreatedNotification_disabledDoesNothing() {
        properties.setEnabled(false);

        service.sendTaskCreatedNotification(1L, 100L);

        verifyNoInteractions(pickTaskMapper, warehouseService, adminUserApi, weComClientService);
    }

    @Test
    void sendTaskCreatedNotification_sendsToEnabledPickerWithDirectLink() {
        properties.setEnabled(true);
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setId(100L).setWarehouseId(20L).setWarehouseName("成都仓")
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceNo("XSST001")
                .setCustomerName("测试客户<一店>").setTotalItemCount(3);
        AdminUserRespDTO enabledUser = new AdminUserRespDTO();
        enabledUser.setId(7L);
        enabledUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
        enabledUser.setMobile("+86 138-0013-8000");
        AdminUserRespDTO disabledUser = new AdminUserRespDTO();
        disabledUser.setId(8L);
        disabledUser.setStatus(CommonStatusEnum.DISABLE.getStatus());
        disabledUser.setMobile("13900139000");
        Map<Long, AdminUserRespDTO> users = new HashMap<>();
        users.put(7L, enabledUser);
        users.put(8L, disabledUser);
        when(pickTaskMapper.selectById(100L)).thenReturn(task);
        when(warehouseService.getWarehousePickerUserIds(20L)).thenReturn(Arrays.asList(7L, 8L));
        when(adminUserApi.getUserMap(Arrays.asList(7L, 8L))).thenReturn(users);
        when(weComClientService.getUserIdByMobile("13800138000", "sale-pick"))
                .thenReturn("wecom-zhangsan");

        service.sendTaskCreatedNotification(1L, 100L);

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(weComClientService).sendTextCardMessage(eq("wecom-zhangsan"), eq("新的拣货任务"),
                descriptionCaptor.capture(), urlCaptor.capture(), eq("进入拣货"), eq("sale-pick"));
        assertTrue(descriptionCaptor.getValue().contains("销售手推车 · XSST001"));
        assertTrue(descriptionCaptor.getValue().contains("测试客户&lt;一店&gt;"));
        assertEquals("http://erp.scxyltd.com/wecom-login.html?tenantId=1&workbench=sale-pick&taskId=100",
                urlCaptor.getValue());
        verify(weComClientService, never()).getUserIdByMobile("13900139000", "sale-pick");
    }

    @Test
    void sendTaskCreatedNotification_onePickerFailureDoesNotBlockOthers() {
        properties.setEnabled(true);
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setId(100L).setWarehouseId(20L).setWarehouseName("成都仓")
                .setSourceType(ErpSaleBizSourceTypeEnum.STOCK_OUT.getType()).setSourceNo("QTCK001")
                .setTotalItemCount(1);
        AdminUserRespDTO first = enabledUser(7L, "13800138000");
        AdminUserRespDTO second = enabledUser(8L, "13900139000");
        when(pickTaskMapper.selectById(100L)).thenReturn(task);
        when(warehouseService.getWarehousePickerUserIds(20L)).thenReturn(Arrays.asList(7L, 8L));
        when(adminUserApi.getUserMap(Arrays.asList(7L, 8L))).thenReturn(Map.of(7L, first, 8L, second));
        when(weComClientService.getUserIdByMobile("13800138000", "sale-pick"))
                .thenThrow(new IllegalStateException("成员不存在"));
        when(weComClientService.getUserIdByMobile("13900139000", "sale-pick"))
                .thenReturn("wecom-lisi");

        service.sendTaskCreatedNotification(1L, 100L);

        verify(weComClientService).sendTextCardMessage(eq("wecom-lisi"), eq("新的拣货任务"),
                anyString(), anyString(), eq("进入拣货"), eq("sale-pick"));
    }

    @Test
    void sendTaskCreatedNotification_noPickerSkipsWeComCalls() {
        properties.setEnabled(true);
        when(pickTaskMapper.selectById(100L)).thenReturn(new ErpSalePickDeliveryPickTaskDO()
                .setId(100L).setWarehouseId(20L));
        when(warehouseService.getWarehousePickerUserIds(20L)).thenReturn(Collections.emptyList());

        service.sendTaskCreatedNotification(1L, 100L);

        verifyNoInteractions(adminUserApi, weComClientService);
    }

    private AdminUserRespDTO enabledUser(Long id, String mobile) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setMobile(mobile);
        return user;
    }

}
