package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpMiscSettlementPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpMiscSettlementRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpMiscSettlementPermissionTest {
    @ParameterizedTest
    @ValueSource(strings = {"Receivable", "Payable"})
    void masksBothOriginalAndFinanceFieldsAndKeepsQueryPermission(String side) throws Exception {
        for (boolean hiddenOnOriginal : new boolean[]{true, false}) {
            ErpMiscSettlementRespVO row = new ErpMiscSettlementRespVO().setDocumentId(9L)
                    .setAmount(new BigDecimal("300")).setAccountId(1L).setAccountName("account")
                    .setHandlerId(2L).setHandlerName("handler");
            Object controller = controller(side, true, row, hiddenOnOriginal);
            Method method = controller.getClass().getMethod("settlementPage", ErpMiscSettlementPageReqVO.class);
            assertEquals("@ss.hasPermission('erp:" + side.toLowerCase() + "-misc:query')",
                    method.getAnnotation(PreAuthorize.class).value());
            ErpMiscSettlementPageReqVO req = new ErpMiscSettlementPageReqVO().setId(1L);
            assertEquals(20, req.getPageSize());
            CommonResult<?> result = (CommonResult<?>) method.invoke(controller, req);
            PageResult<?> page = (PageResult<?>) result.getData();
            assertEquals(1L, page.getTotal());
            assertNull(row.getAmount());
            assertNull(row.getAccountId());
            assertNull(row.getAccountName());
            if (!hiddenOnOriginal) {
                assertNull(row.getHandlerId());
                assertNull(row.getHandlerName());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Receivable", "Payable"})
    void inaccessibleOriginalReturnsNoDetails(String side) throws Exception {
        Object controller = controller(side, false, new ErpMiscSettlementRespVO(), false);
        CommonResult<?> result = (CommonResult<?>) controller.getClass()
                .getMethod("settlementPage", ErpMiscSettlementPageReqVO.class)
                .invoke(controller, new ErpMiscSettlementPageReqVO().setId(1L));
        assertEquals(0L, ((PageResult<?>) result.getData()).getTotal());
        Object mapper = ReflectionTestUtils.getField(controller, side.toLowerCase() + "MiscMapper");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Receivable", "Payable"})
    void originalAmountMaskAlsoMasksReservations(String side) throws Exception {
        Object controller = controller(side, true, new ErpMiscSettlementRespVO(), true);
        Class<?> voClass = Class.forName("cn.iocoder.yudao.module.erp.controller.admin.finance." + side.toLowerCase()
                + ".vo.misc.Erp" + side + "MiscRespVO");
        Object vo = voClass.getDeclaredConstructor().newInstance();
        for (String field : Arrays.asList("amount", "settledAmount", "balanceAmount", "pendingTransferAmount", "transferAvailableAmount")) {
            ReflectionTestUtils.setField(vo, field, new BigDecimal("300"));
        }
        ReflectionTestUtils.invokeMethod(controller, "maskForm", vo);
        for (String field : Arrays.asList("amount", "settledAmount", "balanceAmount", "pendingTransferAmount", "transferAvailableAmount")) {
            assertNull(ReflectionTestUtils.getField(vo, field), field);
        }
    }

    private Object controller(String side, boolean accessible, ErpMiscSettlementRespVO row, boolean hiddenOnOriginal) throws Exception {
        String low = side.toLowerCase();
        String doc = side.equals("Receivable") ? "receipt" : "payment";
        Class<?> type = Class.forName("cn.iocoder.yudao.module.erp.controller.admin.finance." + low + ".Erp" + side + "MiscController");
        Object controller = type.getDeclaredConstructor().newInstance();
        for (Field f : type.getDeclaredFields()) {
            if (f.getAnnotation(javax.annotation.Resource.class) == null) continue;
            Object dependency;
            if (f.getName().equals("fieldPermissionMasker")) {
                PermissionApi permission = mock(PermissionApi.class);
                when(permission.getCurrentUserHiddenFields("erp_finance_" + low + "_misc"))
                        .thenReturn(hiddenOnOriginal ? Arrays.asList("amount", "accountId") : Collections.emptyList());
                when(permission.getCurrentUserHiddenFields("erp_finance_" + doc))
                        .thenReturn(hiddenOnOriginal ? Collections.emptyList() : Arrays.asList(doc + "Price", "accountId", "financeUserId"));
                dependency = new ErpFinanceFieldPermissionMasker();
                ReflectionTestUtils.setField(dependency, "permissionApi", permission);
            } else {
                dependency = mock(f.getType(), invocation -> {
                    if (invocation.getMethod().getName().equals("get" + side + "Misc")) {
                        return accessible ? Class.forName("cn.iocoder.yudao.module.erp.dal.dataobject.finance." + low + ".Erp" + side + "MiscDO").getDeclaredConstructor().newInstance() : null;
                    }
                    if (invocation.getMethod().getName().equals("selectSettlementPage")) {
                        return new Page<ErpMiscSettlementRespVO>(1, 20, 1).setRecords(Collections.singletonList(row));
                    }
                    return RETURNS_DEFAULTS.answer(invocation);
                });
            }
            ReflectionTestUtils.setField(controller, f.getName(), dependency);
        }
        return controller;
    }
}
