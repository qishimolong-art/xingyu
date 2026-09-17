package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionApplyReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution.ErpVoucherAttributionSearchSourceBizReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherAttributionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherAttributionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAttributionStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_ALREADY_GENERATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_BOOK_NOT_OPEN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_MONTH_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ATTRIBUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_BIZ_APPROVED_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ErpVoucherAttributionServiceImpl} 单元测试。
 *
 * 覆盖：
 *  - createVoucherAttribution / updateVoucherAttribution / deleteVoucherAttribution / 各 getter
 *  - applyAttribution：批量应用归属年月（状态翻到 ATTRIBUTED）
 *  - generateVouchers：批量生成空壳凭证（状态翻到 GENERATED）
 *  - searchSourceBizPage：按 sourceBizType 路由（覆盖销售出库/销售退货/采购入库/其他应收/未实现类型 等）
 *
 * Bug 暴露用例（4 个）：
 *  - H2：applyAttribution 缺少"归属月份不能早于业务日期月份"校验
 *  - H3：generateVouchers 未校验 BookOpen 该期间是否启用对应凭证类型
 *  - H5：generateVouchers 不再生成 0.01 占位空壳分录
 *  - H8：searchSourceBizPage 客户/供应商 partyName 模糊查询在内存里过滤，
 *        导致 list 被过滤但 total 未跟随过滤 → 分页计数与可见数据不一致
 */
@DisplayName("ErpVoucherAttributionServiceImpl 单元测试")
public class ErpVoucherAttributionServiceImplTest extends BaseMockitoUnitTest {

    @Mock private ErpVoucherGenerationService generationService;
    @Mock private ErpVoucherSourceReader ruleSourceReader;

    @InjectMocks
    private ErpVoucherAttributionServiceImpl attributionService;

    @Mock
    private ErpVoucherAttributionMapper attributionMapper;
    @Mock
    private ErpVoucherService voucherService;
    @Mock
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Mock
    private ErpOtherPayableMapper otherPayableMapper;
    @Mock
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Mock
    private ErpFinancePaymentMapper financePaymentMapper;
    @Mock
    private ErpPreReceiptMapper preReceiptMapper;
    @Mock
    private ErpPrePaymentMapper prePaymentMapper;
    @Mock
    private ErpPreReceivableMapper preReceivableMapper;
    @Mock
    private ErpStockInMapper stockInMapper;
    @Mock
    private ErpStockOutMapper stockOutMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService bookOpenService;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        lenient().when(generationService.previewOne(any())).thenReturn(new Preview().setStatus("INCOMPLETE"));
        lenient().when(ruleSourceReader.page(any(), any(), any(), any())).thenReturn(PageResult.empty());
        // H3 修复后 generateVouchers 强制校验 isVoucherTypeEnabled；默认放行，单测可按需覆盖为 false
        lenient().when(bookOpenService.isVoucherTypeEnabled(any(), any())).thenReturn(true);
    }

    // ==================== 工具方法 ====================

    /** 构造一个标准 SaveReq（业务日期 2026-05-14 10:00、bizType=8 采购入库） */
    private ErpVoucherAttributionSaveReqVO buildSaveReq() {
        ErpVoucherAttributionSaveReqVO req = new ErpVoucherAttributionSaveReqVO();
        req.setBizType(8);
        req.setBizId(1024L);
        req.setBizNo("CGRK202605000001");
        req.setBizDate(LocalDateTime.of(2026, 5, 14, 10, 0));
        req.setBizAmount(new BigDecimal("113.00"));
        req.setTransactionParty("供应商A");
        return req;
    }

    /** 构造一条已存在的 DO（status=ATTRIBUTED 已归属） */
    private ErpVoucherAttributionDO buildExistingDO(Long id, Integer status) {
        return new ErpVoucherAttributionDO()
                .setId(id)
                .setBizType(8)
                .setBizId(1024L)
                .setBizNo("CGRK202605000001")
                .setBizDate(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setBizAmount(new BigDecimal("113.00"))
                .setVoucherMakeDate(LocalDate.of(2026, 5, 14))
                .setAttributionYear(2026)
                .setAttributionMonth(5)
                .setAttributionStatus(status);
    }

    private List<ErpVoucherItemDO> buildVoucherItems() {
        return Arrays.asList(
                new ErpVoucherItemDO().setSubjectId(1L).setDebitAmount(new BigDecimal("113.00")).setCreditAmount(BigDecimal.ZERO),
                new ErpVoucherItemDO().setSubjectId(2L).setDebitAmount(BigDecimal.ZERO).setCreditAmount(new BigDecimal("113.00"))
        );
    }

    private void mockPurchaseInVoucher() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO()
                .setId(1024L).setNo("CGRK202605000001").setSupplierId(30L)
                .setInTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setTotalPrice(new BigDecimal("113.00"));
        when(purchaseInMapper.selectById(eq(1024L))).thenReturn(purchaseIn);
        when(supplierService.getSupplier(eq(30L))).thenReturn(new ErpSupplierDO().setId(30L).setName("供应商A"));
        when(autoVoucherBuilder.buildPurchaseInItems(eq(purchaseIn), eq("供应商A"))).thenReturn(buildVoucherItems());
    }

    // ====================================================================
    // createVoucherAttribution（5 用例）
    // ====================================================================

    @Test
    @DisplayName("createVoucherAttribution：正常创建 - bizType=8 + 默认 status=10 未归属 + voucherMakeDate 取 bizDate 当天")
    public void testCreate_normalCase() {
        ErpVoucherAttributionSaveReqVO req = buildSaveReq();

        when(attributionMapper.insert(any(ErpVoucherAttributionDO.class))).thenAnswer(inv -> {
            ErpVoucherAttributionDO do_ = inv.getArgument(0);
            do_.setId(5000L);
            // 验证默认 status = UNATTRIBUTED(10)
            assertEquals(ErpAttributionStatusEnum.UNATTRIBUTED.getStatus(), do_.getAttributionStatus());
            // 验证 voucherMakeDate 兜底为 bizDate 当天
            assertEquals(LocalDate.of(2026, 5, 14), do_.getVoucherMakeDate());
            return 1;
        });

        Long id = attributionService.createVoucherAttribution(req);

        assertEquals(5000L, id);
        verify(attributionMapper).insert(any(ErpVoucherAttributionDO.class));
    }

    @Test
    @DisplayName("createVoucherAttribution：bizId 为空 - 当前实现允许（VO 未对 bizId 加 NotNull，固化行为）")
    public void testCreate_emptyBizId() {
        ErpVoucherAttributionSaveReqVO req = buildSaveReq();
        req.setBizId(null);

        when(attributionMapper.insert(any(ErpVoucherAttributionDO.class))).thenAnswer(inv -> {
            ((ErpVoucherAttributionDO) inv.getArgument(0)).setId(5001L);
            return 1;
        });

        Long id = attributionService.createVoucherAttribution(req);

        assertEquals(5001L, id);
        verify(attributionMapper).insert(any(ErpVoucherAttributionDO.class));
    }

    @Test
    @DisplayName("createVoucherAttribution：bizType=999 非法值 - 当前实现未校验，允许插入（固化行为，需后续补强校验）")
    public void testCreate_invalidSourceBizType() {
        ErpVoucherAttributionSaveReqVO req = buildSaveReq();
        req.setBizType(999); // 非 20 种之内

        when(attributionMapper.insert(any(ErpVoucherAttributionDO.class))).thenAnswer(inv -> {
            ((ErpVoucherAttributionDO) inv.getArgument(0)).setId(5002L);
            return 1;
        });

        Long id = attributionService.createVoucherAttribution(req);

        assertEquals(5002L, id);
    }

    @Test
    @DisplayName("createVoucherAttribution：默认 attributionStatus=10（未归属）— 显式断言初始状态")
    public void testCreate_attributionStatusInitial() {
        ErpVoucherAttributionSaveReqVO req = buildSaveReq();

        ArgumentCaptor<ErpVoucherAttributionDO> captor = ArgumentCaptor.forClass(ErpVoucherAttributionDO.class);
        when(attributionMapper.insert(captor.capture())).thenAnswer(inv -> {
            captor.getValue().setId(5003L);
            return 1;
        });

        attributionService.createVoucherAttribution(req);

        ErpVoucherAttributionDO inserted = captor.getValue();
        assertEquals(ErpAttributionStatusEnum.UNATTRIBUTED.getStatus(), inserted.getAttributionStatus());
    }

    @Test
    @DisplayName("createVoucherAttribution：同 (bizType, bizId) 重复创建 - 当前未校验唯一性，允许插入（固化行为）")
    public void testCreate_duplicateAttribution() {
        ErpVoucherAttributionSaveReqVO req = buildSaveReq();

        when(attributionMapper.insert(any(ErpVoucherAttributionDO.class))).thenAnswer(inv -> {
            ((ErpVoucherAttributionDO) inv.getArgument(0)).setId(5004L);
            return 1;
        });

        // 第一次创建
        attributionService.createVoucherAttribution(req);
        // 第二次再创建同 bizType+bizId（当前实现无重复校验，仍能落库）
        ErpVoucherAttributionSaveReqVO req2 = buildSaveReq();
        when(attributionMapper.insert(any(ErpVoucherAttributionDO.class))).thenAnswer(inv -> {
            ((ErpVoucherAttributionDO) inv.getArgument(0)).setId(5005L);
            return 1;
        });
        attributionService.createVoucherAttribution(req2);

        verify(attributionMapper, times(2)).insert(any(ErpVoucherAttributionDO.class));
    }

    // ====================================================================
    // updateVoucherAttribution（2 用例）
    // ====================================================================

    @Test
    @DisplayName("updateVoucherAttribution：正常更新 - 保留原 status 与 voucherId")
    public void testUpdate_normalCase() {
        Long id = 5000L;
        ErpVoucherAttributionDO existing = buildExistingDO(id, ErpAttributionStatusEnum.ATTRIBUTED.getStatus())
                .setVoucherId(8888L);
        when(attributionMapper.selectById(eq(id))).thenReturn(existing);

        ErpVoucherAttributionSaveReqVO req = buildSaveReq();
        req.setId(id);
        req.setBizAmount(new BigDecimal("226.00"));

        ArgumentCaptor<ErpVoucherAttributionDO> captor = ArgumentCaptor.forClass(ErpVoucherAttributionDO.class);
        when(attributionMapper.updateById(captor.capture())).thenReturn(1);

        attributionService.updateVoucherAttribution(req);

        ErpVoucherAttributionDO updated = captor.getValue();
        // 验证状态与凭证 ID 被保留
        assertEquals(ErpAttributionStatusEnum.ATTRIBUTED.getStatus(), updated.getAttributionStatus());
        assertEquals(8888L, updated.getVoucherId());
        // 验证 bizAmount 被更新
        assertEquals(new BigDecimal("226.00"), updated.getBizAmount());
    }

    @Test
    @DisplayName("updateVoucherAttribution：归属记录不存在 - 抛 VOUCHER_ATTRIBUTION_NOT_EXISTS")
    public void testUpdate_notExists() {
        Long id = 9999L;
        when(attributionMapper.selectById(eq(id))).thenReturn(null);

        ErpVoucherAttributionSaveReqVO req = buildSaveReq();
        req.setId(id);

        assertServiceException(() -> attributionService.updateVoucherAttribution(req),
                VOUCHER_ATTRIBUTION_NOT_EXISTS);
        verify(attributionMapper, never()).updateById(any(ErpVoucherAttributionDO.class));
    }

    // ====================================================================
    // applyAttribution（4 用例，含 H2 Bug）
    // ====================================================================

    @Test
    @DisplayName("applyAttribution：正常 - 批量应用归属年月 → status=ATTRIBUTED")
    public void testApply_normalCase() {
        ErpVoucherAttributionDO d1 = buildExistingDO(101L, ErpAttributionStatusEnum.UNATTRIBUTED.getStatus());
        ErpVoucherAttributionDO d2 = buildExistingDO(102L, ErpAttributionStatusEnum.UNATTRIBUTED.getStatus());
        when(attributionMapper.selectByIds(eq(Arrays.asList(101L, 102L))))
                .thenReturn(Arrays.asList(d1, d2));

        ErpVoucherAttributionApplyReqVO reqVO = new ErpVoucherAttributionApplyReqVO();
        reqVO.setIds(Arrays.asList(101L, 102L));
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(5);

        attributionService.applyAttribution(reqVO);

        ArgumentCaptor<ErpVoucherAttributionDO> captor = ArgumentCaptor.forClass(ErpVoucherAttributionDO.class);
        verify(attributionMapper, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(updated -> {
            assertEquals(ErpAttributionStatusEnum.ATTRIBUTED.getStatus(), updated.getAttributionStatus());
            assertEquals(2026, updated.getAttributionYear());
            assertEquals(5, updated.getAttributionMonth());
        });
    }

    @Test
    @DisplayName("applyAttribution：传入空查询结果（mapper.selectByIds 返回空）- 抛 VOUCHER_ATTRIBUTION_NOT_EXISTS")
    public void testApply_emptyIds() {
        // 模拟传入的 ids 没查到任何 DO
        when(attributionMapper.selectByIds(anyCollection())).thenReturn(Collections.emptyList());

        ErpVoucherAttributionApplyReqVO reqVO = new ErpVoucherAttributionApplyReqVO();
        reqVO.setIds(Arrays.asList(999L)); // 不存在的 id
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(5);

        assertServiceException(() -> attributionService.applyAttribution(reqVO),
                VOUCHER_ATTRIBUTION_NOT_EXISTS);
        verify(attributionMapper, never()).updateById(any(ErpVoucherAttributionDO.class));
    }

    @Test
    @DisplayName("applyAttribution：H2 已修复 - 归属月份(3月)早于业务日期月份(5月)，抛 VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ")
    public void testApply_attributionMonthBeforeBizDate_bugH2() {
        // bizDate = 2026-05-14, attrMonth=3 < bizMonth=5
        // H2 修复：validateAttributionMonth 加下界校验，attrYM < bizYM 时抛 VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ
        ErpVoucherAttributionDO d1 = buildExistingDO(201L, ErpAttributionStatusEnum.UNATTRIBUTED.getStatus());
        when(attributionMapper.selectByIds(eq(Collections.singletonList(201L))))
                .thenReturn(Collections.singletonList(d1));

        ErpVoucherAttributionApplyReqVO reqVO = new ErpVoucherAttributionApplyReqVO();
        reqVO.setIds(Collections.singletonList(201L));
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(3); // 关键：3 月早于业务发生 5 月

        assertServiceException(() -> attributionService.applyAttribution(reqVO),
                VOUCHER_ATTRIBUTION_MONTH_BEFORE_BIZ);
        verify(attributionMapper, never()).updateById(any(ErpVoucherAttributionDO.class));
    }

    @Test
    @DisplayName("applyAttribution：已生成凭证(status=30) - 抛 VOUCHER_ATTRIBUTION_ALREADY_GENERATED")
    public void testApply_alreadyGenerated() {
        ErpVoucherAttributionDO d1 = buildExistingDO(301L, ErpAttributionStatusEnum.GENERATED.getStatus())
                .setVoucherId(8888L);
        when(attributionMapper.selectByIds(eq(Collections.singletonList(301L))))
                .thenReturn(Collections.singletonList(d1));

        ErpVoucherAttributionApplyReqVO reqVO = new ErpVoucherAttributionApplyReqVO();
        reqVO.setIds(Collections.singletonList(301L));
        reqVO.setAttributionYear(2026);
        reqVO.setAttributionMonth(5);

        assertServiceException(() -> attributionService.applyAttribution(reqVO),
                VOUCHER_ATTRIBUTION_ALREADY_GENERATED);
        verify(attributionMapper, never()).updateById(any(ErpVoucherAttributionDO.class));
    }

    // ====================================================================
    // deleteVoucherAttribution（3 用例）
    // ====================================================================

    @Test
    @DisplayName("deleteVoucherAttribution：status=10 未归属 - 允许删除")
    public void testDelete_normalCase() {
        Long id = 401L;
        when(attributionMapper.selectById(eq(id)))
                .thenReturn(buildExistingDO(id, ErpAttributionStatusEnum.UNATTRIBUTED.getStatus()));

        attributionService.deleteVoucherAttribution(id);

        verify(attributionMapper).deleteById(eq(id));
    }

    @Test
    @DisplayName("deleteVoucherAttribution：status=20 已归属（未生成凭证）- 当前实现允许删除（固化行为）")
    public void testDelete_alreadyApplied_canDelete() {
        Long id = 402L;
        when(attributionMapper.selectById(eq(id)))
                .thenReturn(buildExistingDO(id, ErpAttributionStatusEnum.ATTRIBUTED.getStatus()));

        attributionService.deleteVoucherAttribution(id);

        verify(attributionMapper).deleteById(eq(id));
    }

    @Test
    @DisplayName("deleteVoucherAttribution：status=30 已生成凭证 - 抛 VOUCHER_ATTRIBUTION_ALREADY_GENERATED")
    public void testDelete_alreadyGenerated_forbidden() {
        Long id = 403L;
        when(attributionMapper.selectById(eq(id)))
                .thenReturn(buildExistingDO(id, ErpAttributionStatusEnum.GENERATED.getStatus())
                        .setVoucherId(8888L));

        assertServiceException(() -> attributionService.deleteVoucherAttribution(id),
                VOUCHER_ATTRIBUTION_ALREADY_GENERATED);
        verify(attributionMapper, never()).deleteById(anyLong());
    }

    // ====================================================================
    // generateVouchers（6 用例，含 H3 + H5 Bug）
    // ====================================================================

    @Test
    @DisplayName("旧生成接口将来源、实际日期和预览令牌完整交给统一服务")
    public void testGenerateDelegatesWithPreviewToken() {
        when(attributionMapper.selectById(501L)).thenReturn(buildExistingDO(501L, 20));
        when(generationService.generate(any())).thenReturn(Collections.singletonList(7001L));
        ErpVoucherAttributionGenerateReqVO req = new ErpVoucherAttributionGenerateReqVO();
        req.setIds(Collections.singletonList(501L));
        req.setPreviewTokens(Collections.singletonMap(501L, "preview-v1"));
        assertThat(attributionService.generateVouchers(req)).containsExactly(7001L);
        ArgumentCaptor<Batch> captor = ArgumentCaptor.forClass(Batch.class);
        verify(generationService).generate(captor.capture());
        Request item = captor.getValue().getItems().get(0);
        assertEquals(8, item.getBizType());
        assertEquals(1024L, item.getBizId());
        assertEquals(LocalDate.of(2026, 5, 14), item.getVoucherDate());
        assertEquals("preview-v1", item.getPreviewToken());
        assertEquals(5, item.getAttributionMonth());
        verifyNoInteractions(voucherService, autoVoucherBuilder);
    }

    @Test
    public void testGenerateMissingAttribution() {
        ErpVoucherAttributionGenerateReqVO req = new ErpVoucherAttributionGenerateReqVO();
        req.setIds(Collections.singletonList(999L));
        assertServiceException(() -> attributionService.generateVouchers(req), VOUCHER_ATTRIBUTION_NOT_EXISTS);
        verifyNoInteractions(generationService, voucherService);
    }

    @Test
    @DisplayName("searchSourceBizPage：销售出库(type=2) - 透传到 saleOutMapper + 客户名回填")
    public void testSearchSourceBiz_saleOut_normal() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(2);

        ErpSaleOutDO sale = new ErpSaleOutDO()
                .setId(11L).setNo("XSCK202605000001")
                .setCustomerId(20L)
                .setOutTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setTotalPrice(new BigDecimal("200.00"))
                .setRemark("销售单 A");
        when(saleOutMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(sale), 1L));

        Map<Long, ErpCustomerDO> customerMap = new HashMap<>();
        customerMap.put(20L, new ErpCustomerDO().setId(20L).setName("客户甲"));
        when(customerService.getCustomerMap(anyCollection())).thenReturn(customerMap);

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertThat(result.getList()).hasSize(1);
        assertEquals("XSCK202605000001", result.getList().get(0).getBizNo());
        assertEquals("客户甲", result.getList().get(0).getTransactionParty());
        assertEquals(ErpAttributionStatusEnum.UNATTRIBUTED.getStatus(),
                result.getList().get(0).getAttributionStatus());
    }

    @Test
    @DisplayName("searchSourceBizPage：销售退货(type=3) - 透传到 saleReturnMapper + 客户名回填")
    public void testSearchSourceBiz_saleReturn_normal() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(3);

        ErpSaleReturnDO ret = new ErpSaleReturnDO()
                .setId(12L).setNo("XSTH202605000001")
                .setCustomerId(20L)
                .setReturnTime(LocalDateTime.of(2026, 5, 16, 9, 0))
                .setTotalPrice(new BigDecimal("80.00"));
        when(saleReturnMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(ret), 1L));

        Map<Long, ErpCustomerDO> customerMap = new HashMap<>();
        customerMap.put(20L, new ErpCustomerDO().setId(20L).setName("客户甲"));
        when(customerService.getCustomerMap(anyCollection())).thenReturn(customerMap);

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertThat(result.getList()).hasSize(1);
        assertEquals("客户甲", result.getList().get(0).getTransactionParty());
    }

    @Test
    @DisplayName("searchSourceBizPage：采购入库(type=8) - 透传到 purchaseInMapper + 供应商名回填")
    public void testSearchSourceBiz_purchaseIn_normal() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(8);

        ErpPurchaseInDO in = new ErpPurchaseInDO()
                .setId(21L).setNo("CGRK202605000001")
                .setSupplierId(30L)
                .setInTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setTotalPrice(new BigDecimal("300.00"));
        when(purchaseInMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(in), 1L));

        Map<Long, ErpSupplierDO> supplierMap = new HashMap<>();
        supplierMap.put(30L, new ErpSupplierDO().setId(30L).setName("供应商乙"));
        when(supplierService.getSupplierMap(anyCollection())).thenReturn(supplierMap);

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("供应商乙", result.getList().get(0).getTransactionParty());
        assertEquals(8, result.getList().get(0).getBizType());
    }

    @Test
    @DisplayName("searchSourceBizPage：其他应收(type=4) - 直接表内 partyName like，无需查 customerService")
    public void testSearchSourceBiz_otherReceivable_filterByPartyName() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(4);
        reqVO.setPartyName("租户");

        ErpOtherReceivableDO o = new ErpOtherReceivableDO()
                .setId(41L).setNo("QTYS202605000001")
                .setPartyName("租户A")
                .setBizTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setActualAmount(new BigDecimal("50.00"))
                .setTotalAmount(new BigDecimal("100.00"));
        when(otherReceivableMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(o), 1L));

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("租户A", result.getList().get(0).getTransactionParty());
        // 验证金额优先取 actualAmount
        assertEquals(new BigDecimal("50.00"), result.getList().get(0).getBizAmount());
        // 其他应收无需查 customerService（partyName 是表内字段，可走 SQL 模糊）
        verifyNoInteractions(customerService);
    }

    @Test
    @DisplayName("searchSourceBizPage：单据类型为空 - 聚合可生成凭证的业务单据")
    public void testSearchSourceBiz_sourceBizTypeNull_searchAllGeneratable() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        when(saleReturnMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(otherReceivableMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(purchaseInMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(purchaseReturnMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(otherPayableMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(stockOutMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(stockInMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(preReceiptMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        when(preReceivableMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        ErpSaleOutDO sale = new ErpSaleOutDO()
                .setId(11L).setNo("XSCK202605000001")
                .setCustomerId(20L)
                .setOutTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setTotalPrice(new BigDecimal("200.00"));
        when(saleOutMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(sale), 1L));
        Map<Long, ErpCustomerDO> customerMap = new HashMap<>();
        customerMap.put(20L, new ErpCustomerDO().setId(20L).setName("客户甲"));
        when(customerService.getCustomerMap(anyCollection())).thenReturn(customerMap);

        ErpPrePaymentDO prePayment = new ErpPrePaymentDO()
                .setId(22L).setNo("YFK202605000001")
                .setBizTime(LocalDateTime.of(2026, 5, 15, 9, 0))
                .setActualAmount(new BigDecimal("60.00"))
                .setPartyName("供应商乙");
        when(prePaymentMapper.selectPage(any(ErpVoucherAttributionSearchSourceBizReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(prePayment), 1L));

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(2L, result.getTotal());
        assertThat(result.getList()).hasSize(2);
        assertEquals(ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType(), result.getList().get(0).getBizType());
        assertEquals(ErpVoucherSourceBizTypeEnum.SALE_OUT.getType(), result.getList().get(1).getBizType());
    }

    @Test
    @DisplayName("searchSourceBizPage：预付款(type=22) - 返回可生成凭证的资金单据")
    public void testSearchSourceBiz_prePayment_normal() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType());

        ErpPrePaymentDO prePayment = new ErpPrePaymentDO()
                .setId(22L).setNo("YFK202605000001")
                .setBizTime(LocalDateTime.of(2026, 5, 15, 9, 0))
                .setActualAmount(new BigDecimal("60.00"))
                .setPartyName("供应商乙");
        when(prePaymentMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(prePayment), 1L));

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType(), result.getList().get(0).getBizType());
        assertEquals(new BigDecimal("60.00"), result.getList().get(0).getBizAmount());
        assertEquals("供应商乙", result.getList().get(0).getTransactionParty());
    }

    @Test
    @DisplayName("未接入的来源明确报错，不能显示为没有单据")
    public void testSearchSourceBiz_unimplementedType_explainsReason() {
        ErpVoucherAttributionSearchSourceBizReqVO req = new ErpVoucherAttributionSearchSourceBizReqVO();
        req.setSourceBizType(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attributionService.searchSourceBizPage(req))
                .hasMessageContaining("尚未接入");
        verifyNoInteractions(saleOutMapper, purchaseInMapper);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {6, 12, 18})
    void searchNewFundSources(int type) {
        Map<String,Object> header = new HashMap<>();
        header.put("id", 66L); header.put("no", "FUND66");
        header.put(type == 6 ? "receiptTime" : type == 12 ? "paymentTime" : "transferTime", LocalDateTime.of(2026,5,14,10,0));
        header.put(type == 6 ? "receiptPrice" : type == 12 ? "paymentPrice" : "transferPrice", new BigDecimal("100.00"));
        when(ruleSourceReader.page(any(), any(), any(), any())).thenReturn(new PageResult<>(Collections.singletonList(header), 1L));
        ErpVoucherAttributionSearchSourceBizReqVO req = new ErpVoucherAttributionSearchSourceBizReqVO(); req.setSourceBizType(type);
        ErpVoucherAttributionRespVO row = attributionService.searchSourceBizPage(req).getList().get(0);
        assertEquals(type, row.getBizType()); assertEquals(66L, row.getBizId());
        assertEquals(new BigDecimal("100.00"), row.getBizAmount());
        assertEquals("INCOMPLETE", row.getGenerationStatus());
    }

    @Test
    @DisplayName("searchSourceBizPage：按日期区间过滤 - reqVO 透传到 mapper（验证 PageParam 入参原样传递）")
    public void testSearchSourceBiz_filterByDateRange() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(8);
        reqVO.setBizDateStart(LocalDate.of(2026, 5, 1));
        reqVO.setBizDateEnd(LocalDate.of(2026, 5, 31));

        // 关键：getBizDateStartTime()/getBizDateEndTime() 在 LambdaQueryWrapper 里使用，
        // mock mapper 只关心 reqVO 实例本身被透传
        when(purchaseInMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        assertEquals(0L, result.getTotal());
        verify(purchaseInMapper).selectPage(eq(reqVO), any(Wrapper.class));
        // 验证日期 getter 工作正确
        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0, 0), reqVO.getBizDateStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 31, 23, 59, 59, 999_999_999),
                reqVO.getBizDateEndTime());
    }

    @Test
    @DisplayName("searchSourceBizPage：按单号 like 过滤 - bizNo 透传到 mapper")
    public void testSearchSourceBiz_filterByBizNo() {
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(8);
        reqVO.setBizNo("CGRK");

        when(purchaseInMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        attributionService.searchSourceBizPage(reqVO);

        verify(purchaseInMapper).selectPage(eq(reqVO), any(Wrapper.class));
        assertEquals("CGRK", reqVO.getBizNo());
    }

    @Test
    @DisplayName("searchSourceBizPage：H5 修复 - 客户/供应商 partyName 下推到 SQL，total 与 list 一致")
    public void testSearchSourceBiz_customerNameLike_loadAllInMemory_bugH8() {
        // H5 修复：销售出库等"客户/供应商"维度的 partyName 过滤改为 SQL 下推
        //   1) 先调用 customerService.getCustomerListByNameLike(partyName) 查匹配客户 ID 列表
        //   2) 用 inIfPresent(customerId, ids) 在 SQL 层过滤 → total 与 list 一致
        //   3) 不再有内存 stream().filter(matchesPartyName) 二次过滤路径
        ErpVoucherAttributionSearchSourceBizReqVO reqVO = new ErpVoucherAttributionSearchSourceBizReqVO();
        reqVO.setSourceBizType(2);
        reqVO.setPartyName("客户A");

        // mock customerService.getCustomerListByNameLike：返回 1 条匹配客户
        ErpCustomerDO matchedCustomer = new ErpCustomerDO().setId(2003L).setName("客户A特别版");
        when(customerService.getCustomerListByNameLike("客户A"))
                .thenReturn(Collections.singletonList(matchedCustomer));

        // mock：mapper 在 SQL 已 IN(2003) 过滤的情况下只返回 1 条 + total=1
        ErpSaleOutDO matchedSaleOut = new ErpSaleOutDO()
                .setId(1003L)
                .setNo("XSCK3")
                .setCustomerId(2003L)
                .setOutTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .setTotalPrice(new BigDecimal("100"));
        when(saleOutMapper.selectPage(eq(reqVO), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(matchedSaleOut), 1L));

        // mock customerService.getCustomerMap：用于分页内根据 customerId 回填名称
        Map<Long, ErpCustomerDO> customerMap = new HashMap<>();
        customerMap.put(2003L, matchedCustomer);
        when(customerService.getCustomerMap(anyCollection())).thenReturn(customerMap);

        PageResult<ErpVoucherAttributionRespVO> result = attributionService.searchSourceBizPage(reqVO);

        // 修复后断言：list 1 条 + total=1，分页器一致
        assertThat(result.getList()).hasSize(1);
        assertEquals("客户A特别版", result.getList().get(0).getTransactionParty());
        assertEquals(1L, result.getTotal());
        // 验证 H5 关键路径：调用了 customerService.getCustomerListByNameLike（SQL 下推路径）
        verify(customerService).getCustomerListByNameLike("客户A");
    }

    // ====================================================================
    // getVoucherAttribution / getVoucherAttributionPage（2 用例）
    // ====================================================================

    @Test
    @DisplayName("getVoucherAttribution：透传 mapper.selectById")
    public void testGetAttribution_normal() {
        Long id = 9000L;
        ErpVoucherAttributionDO existing = buildExistingDO(id, ErpAttributionStatusEnum.ATTRIBUTED.getStatus());
        when(attributionMapper.selectById(eq(id))).thenReturn(existing);

        ErpVoucherAttributionDO result = attributionService.getVoucherAttribution(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(8, result.getBizType());
    }

    @Test
    @DisplayName("getVoucherAttributionPage：透传 mapper.selectPage")
    public void testGetAttributionPage_normal() {
        ErpVoucherAttributionPageReqVO reqVO = new ErpVoucherAttributionPageReqVO();
        PageResult<ErpVoucherAttributionDO> page = new PageResult<>(
                Collections.singletonList(buildExistingDO(9001L, ErpAttributionStatusEnum.UNATTRIBUTED.getStatus())),
                1L);
        when(attributionMapper.selectPage(eq(reqVO))).thenReturn(page);

        PageResult<ErpVoucherAttributionDO> result = attributionService.getVoucherAttributionPage(reqVO);

        assertThat(result.getList()).hasSize(1);
        assertEquals(1L, result.getTotal());
        verify(attributionMapper).selectPage(eq(reqVO));
    }

}
