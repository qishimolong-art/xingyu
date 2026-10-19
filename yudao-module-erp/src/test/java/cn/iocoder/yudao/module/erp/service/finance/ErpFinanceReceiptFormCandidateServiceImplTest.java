package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidatePageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpFinanceReceiptFormCandidateServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceReceiptServiceImpl service;

    @Mock
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Mock
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getFormCandidatesUsesRawSettlementAmountsAndFiltersFullyAllocatedDocuments() {
        LocalDateTime createEarly = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime createMiddle = LocalDateTime.of(2026, 9, 2, 9, 0);
        LocalDateTime createLate = LocalDateTime.of(2026, 9, 3, 9, 0);
        when(saleOutMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Arrays.asList(
                saleOut(100L, "XSCK-100", createMiddle, new BigDecimal("300")),
                saleOut(101L, "XSCK-101", createLate, new BigDecimal("50"))));
        when(saleOutItemMapper.selectOriginalSettlementProductPriceMap(any())).thenReturn(Collections.emptyMap());
        when(saleReturnMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.singletonList(
                saleReturn(200L, "XSTH-200", createEarly, new BigDecimal("80"))));
        when(salePriceAdjustMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.singletonList(
                salePriceAdjust(300L, "XSTJ-300", createLate, new BigDecimal("-30"))));
        when(receivableMiscMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Arrays.asList(
                receivableMisc(400L, "XSQT-400", createLate, new BigDecimal("60")),
                receivableMisc(401L, "XSQT-401", createLate, new BigDecimal("20"))));
        when(saleOutMapper.selectPage(any(ErpSaleOutPageReqVO.class))).thenReturn(new PageResult<>(
                Collections.singletonList(saleOut(100L, "XSCK-100", createMiddle, new BigDecimal("300"))), 1L));
        when(saleReturnMapper.selectPage(any(ErpSaleReturnPageReqVO.class))).thenReturn(new PageResult<>(
                Collections.singletonList(saleReturn(200L, "XSTH-200", createEarly, new BigDecimal("80"))), 1L));
        when(salePriceAdjustMapper.selectPage(any(ErpSalePriceAdjustPageReqVO.class))).thenReturn(new PageResult<>(
                Collections.singletonList(salePriceAdjust(300L, "XSTJ-300", createLate,
                        new BigDecimal("-30"))), 1L));
        when(receivableMiscMapper.selectPage(any(ErpReceivableMiscPageReqVO.class))).thenReturn(new PageResult<>(
                Collections.singletonList(receivableMisc(400L, "XSQT-400", createLate,
                        new BigDecimal("60"))), 1L));
        Map<Long, BigDecimal> outAllocated = new HashMap<>();
        outAllocated.put(100L, new BigDecimal("100"));
        outAllocated.put(101L, new BigDecimal("50"));
        when(financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.SALE_OUT.getType())))
                .thenReturn(outAllocated);
        when(financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.SALE_RETURN.getType())))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("-20")));
        when(financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                any(), eq(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType())))
                .thenReturn(Collections.emptyMap());
        Map<Long, BigDecimal> miscAllocated = new HashMap<>();
        miscAllocated.put(400L, new BigDecimal("25"));
        miscAllocated.put(401L, new BigDecimal("20"));
        when(receivableMiscMapper.selectOccupiedAmounts(
                any(), eq(ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE)))
                .thenReturn(miscAllocated);
        when(receivableMiscMapper.selectSettlementAmountSumMapBySourceMiscIds(
                any(), eq(ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE)))
                .thenReturn(miscAllocated);
        when(customerService.getCustomerMap(any())).thenReturn(Collections.singletonMap(4L,
                new ErpCustomerDO().setId(4L).setName("项目客户")));
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(7L);
        dept.setName("兴宇路通");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(7L, dept));

        List<ErpFinanceReceiptFormCandidateRespVO> result = service.getFormCandidates(new ErpFinanceReceiptFormCandidateReqVO()
                .setCustomerId(4L).setDeptId(7L).setNo("XS"));

        assertThat(result).extracting(ErpFinanceReceiptFormCandidateRespVO::getBizNo)
                .containsExactly("XSTH-200", "XSCK-100", "XSTJ-300", "XSQT-400");
        assertThat(result.get(0)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.SALE_RETURN.getType());
            assertThat(row.getCustomerName()).isEqualTo("项目客户");
            assertThat(row.getDeptName()).isEqualTo("兴宇路通");
            assertThat(row.getTotalPrice()).isEqualByComparingTo("-80.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("-20.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("-60.00");
        });
        assertThat(result.get(1)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.SALE_OUT.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("300.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("100.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("200.00");
        });
        assertThat(result.get(2)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("-30.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("-30.00");
        });
        assertThat(result.get(3)).satisfies(row -> {
            assertThat(row.getBizType()).isEqualTo(ErpBizTypeEnum.RECEIVABLE_MISC.getType());
            assertThat(row.getTotalPrice()).isEqualByComparingTo("60.00");
            assertThat(row.getAllocatedPrice()).isEqualByComparingTo("25.00");
            assertThat(row.getUnallocatedPrice()).isEqualByComparingTo("35.00");
        });
        assertThat(result).noneMatch(row -> row.getBizId().equals(101L));
        assertThat(result).noneMatch(row -> row.getBizId().equals(401L));
        verifyNoInteractions(fieldPermissionMasker);

        ErpFinanceReceiptFormCandidatePageReqVO pageReqVO = new ErpFinanceReceiptFormCandidatePageReqVO();
        pageReqVO.setCustomerId(4L);
        pageReqVO.setDeptId(7L);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(2);
        pageReqVO.setDocumentTotalPrice(new BigDecimal("250"));
        ErpFinanceReceiptFormCandidatePageReqVO.SelectedItem missingSelected =
                new ErpFinanceReceiptFormCandidatePageReqVO.SelectedItem();
        missingSelected.setBizType(ErpBizTypeEnum.SALE_OUT.getType());
        missingSelected.setBizId(101L);
        missingSelected.setAmount(new BigDecimal("10"));
        pageReqVO.setSelectedItems(Collections.singletonList(missingSelected));

        ErpFinanceReceiptFormCandidatePageRespVO page = service.getFormCandidatePage(pageReqVO);

        assertThat(page.getTotal()).isEqualTo(4L);
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getUnavailableSelectedItems()).extracting(
                ErpFinanceReceiptFormCandidatePageReqVO.SelectedItem::getBizId).containsExactly(101L);
        assertThat(page.getList()).allMatch(row -> row.getAvailable() != null);
        verify(saleOutMapper).selectPage(argThat(source -> source.getPageNo() == 1
                && source.getPageSize() == 2 && Boolean.TRUE.equals(source.getReceiptEnable())));
        verify(saleOutMapper).selectPage(argThat(source -> source.getPageNo() == 1
                && source.getPageSize() == 200 && "outTime".equals(source.getOrderField())));
    }

    private ErpSaleOutDO saleOut(Long id, String no, LocalDateTime createTime, BigDecimal totalPrice) {
        ErpSaleOutDO row = new ErpSaleOutDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setCustomerId(4L).setDeptId(7L)
                .setOutTime(LocalDateTime.of(2026, 9, 2, 10, 0))
                .setTotalPrice(totalPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpSaleReturnDO saleReturn(Long id, String no, LocalDateTime createTime, BigDecimal totalPrice) {
        ErpSaleReturnDO row = new ErpSaleReturnDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setCustomerId(4L).setDeptId(7L)
                .setReturnTime(LocalDateTime.of(2026, 9, 1, 10, 0))
                .setTotalPrice(totalPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpSalePriceAdjustDO salePriceAdjust(Long id, String no, LocalDateTime createTime,
                                                 BigDecimal totalAdjustPrice) {
        ErpSalePriceAdjustDO row = new ErpSalePriceAdjustDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setCustomerId(4L).setDeptId(7L)
                .setAdjustDate(LocalDateTime.of(2026, 9, 4, 10, 0))
                .setTotalAdjustPrice(totalAdjustPrice);
        row.setCreateTime(createTime);
        return row;
    }

    private ErpReceivableMiscDO receivableMisc(Long id, String no, LocalDateTime createTime, BigDecimal amount) {
        ErpReceivableMiscDO row = new ErpReceivableMiscDO().setId(id).setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setCustomerId(4L).setDeptId(7L)
                .setBizTime(LocalDateTime.of(2026, 9, 4, 11, 0))
                .setAmount(amount);
        row.setCreateTime(createTime);
        return row;
    }

}
