package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpFinancePaymentFormCandidateServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinancePaymentServiceImpl service;

    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Mock
    private ErpPayableMiscMapper payableMiscMapper;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getFormCandidatesUsesRawSettlementAmountsAndFiltersFullyAllocatedDocuments() {
        LocalDateTime createEarly = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime createMiddle = LocalDateTime.of(2026, 9, 2, 9, 0);
        LocalDateTime createLate = LocalDateTime.of(2026, 9, 3, 9, 0);
        when(purchaseInMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Arrays.asList(
                purchaseIn(100L, "CGRK-100", createMiddle, new BigDecimal("300")),
                purchaseIn(101L, "CGRK-101", createLate, new BigDecimal("50"))));
        when(purchaseInService.getPurchaseInItemListByInIds(any())).thenReturn(Collections.emptyList());
        when(purchaseReturnMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.singletonList(
                purchaseReturn(200L, "CGTH-200", createEarly, new BigDecimal("80"))));
        when(purchasePriceAdjustMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.singletonList(
                purchasePriceAdjust(300L, "CGTJ-300", createLate, new BigDecimal("-30"))));
        when(payableMiscMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Arrays.asList(
                payableMisc(400L, "CGQT-400", createLate, new BigDecimal("60")),
                payableMisc(401L, "CGQT-401", createLate, new BigDecimal("20"))));
        Map<Long, BigDecimal> inAllocated = new HashMap<>();
        inAllocated.put(100L, new BigDecimal("100"));
        inAllocated.put(101L, new BigDecimal("50"));
        when(financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.PURCHASE_IN.getType())))
                .thenReturn(inAllocated);
        when(financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType())))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("-20")));
        when(financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())))
                .thenReturn(Collections.emptyMap());
        Map<Long, BigDecimal> miscAllocated = new HashMap<>();
        miscAllocated.put(400L, new BigDecimal("25"));
        miscAllocated.put(401L, new BigDecimal("20"));
        when(financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.PAYABLE_MISC.getType())))
                .thenReturn(miscAllocated);
        when(supplierService.getSupplierMap(any())).thenReturn(Collections.singletonMap(4L,
                new ErpSupplierDO().setId(4L).setName("项目轮胎")));
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(7L);
        dept.setName("兴宇路通");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(7L, dept));

        List<ErpFinancePaymentFormCandidateRespVO> result = service.getFormCandidates(new ErpFinancePaymentFormCandidateReqVO()
                .setSupplierId(4L).setDeptId(7L).setNo("CG"));

        assertThat(result).extracting(ErpFinancePaymentFormCandidateRespVO::getBizNo)
                .containsExactly("CGTH-200", "CGRK-100", "CGTJ-300", "CGQT-400");
        assertThat(result.get(0)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_RETURN.getType());
            assertThat(row.getSupplierName()).isEqualTo("项目轮胎");
            assertThat(row.getDeptName()).isEqualTo("兴宇路通");
            assertThat(row.getTotalPrice()).isEqualByComparingTo("-80.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("-20.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("-60.00");
        });
        assertThat(result.get(1)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_IN.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("300.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("100.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("200.00");
        });
        assertThat(result.get(2)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("-30.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("-30.00");
        });
        assertThat(result.get(3)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.PAYABLE_MISC.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("60.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("25.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("35.00");
        });
        assertThat(result).noneMatch(row -> row.getBizId().equals(101L));
        assertThat(result).noneMatch(row -> row.getBizId().equals(401L));
        verifyNoInteractions(fieldPermissionMasker);
    }

    private ErpPurchaseInDO purchaseIn(Long id, String no, LocalDateTime createTime, BigDecimal totalPrice) {
        ErpPurchaseInDO row = new ErpPurchaseInDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSupplierId(4L).setDeptId(7L)
                .setInTime(LocalDateTime.of(2026, 9, 2, 10, 0))
                .setTotalPrice(totalPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpPurchaseReturnDO purchaseReturn(Long id, String no, LocalDateTime createTime, BigDecimal totalPrice) {
        ErpPurchaseReturnDO row = new ErpPurchaseReturnDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSupplierId(4L).setDeptId(7L)
                .setReturnTime(LocalDateTime.of(2026, 9, 1, 10, 0))
                .setTotalPrice(totalPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpPurchasePriceAdjustDO purchasePriceAdjust(Long id, String no, LocalDateTime createTime,
                                                         BigDecimal totalAdjustPrice) {
        ErpPurchasePriceAdjustDO row = new ErpPurchasePriceAdjustDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSupplierId(4L).setDeptId(7L)
                .setAdjustTime(LocalDateTime.of(2026, 9, 4, 10, 0))
                .setTotalAdjustPrice(totalAdjustPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpPayableMiscDO payableMisc(Long id, String no, LocalDateTime createTime, BigDecimal amount) {
        ErpPayableMiscDO row = new ErpPayableMiscDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSupplierId(4L).setDeptId(7L)
                .setBizTime(LocalDateTime.of(2026, 9, 4, 11, 0))
                .setAmount(amount);
        row.setCreateTime(createTime);
        return row;
    }

}
