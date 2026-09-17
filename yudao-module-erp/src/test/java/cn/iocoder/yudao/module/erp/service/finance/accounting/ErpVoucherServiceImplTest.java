package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherItemSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpAccountingSubjectMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceTypeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_NOT_LEAF;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_AUDIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_DEBIT_CREDIT_NOT_BALANCE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_ITEM_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VOUCHER_PROCESS_FAIL;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpVoucherServiceImpl} 单元测试。
 */
class ErpVoucherServiceImplTest extends BaseMockitoUnitTest {

    @Mock private cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherAuxiliarySupport auxiliarySupport;

    @InjectMocks
    private ErpVoucherServiceImpl voucherService;

    @Mock
    private ErpVoucherMapper voucherMapper;
    @Mock
    private ErpVoucherItemMapper voucherItemMapper;
    @Mock
    private ErpAccountingSubjectMapper subjectMapper;
    @Mock
    private AdminUserApi adminUserApi;

    private final AtomicReference<String> nextNoRef = new AtomicReference<>("记-202605-000001");

    @BeforeEach
    void setupNoRedisDAO() {
        ReflectionTestUtils.setField(voucherService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generateMonthly(String prefix) {
                return nextNoRef.get();
            }

            @Override
            public String generateMonthly(String voucherWord, YearMonth voucherYM) {
                return nextNoRef.get();
            }
        });
    }

    // ==================== Helpers ====================

    private ErpVoucherSaveReqVO buildSaveReqVO(BigDecimal debit, BigDecimal credit) {
        ErpVoucherSaveReqVO vo = new ErpVoucherSaveReqVO();
        vo.setVoucherDate(LocalDate.of(2026, 5, 15));
        vo.setSummary("测试摘要");

        ErpVoucherItemSaveReqVO item1 = new ErpVoucherItemSaveReqVO();
        item1.setSubjectId(100L);
        item1.setDebitAmount(debit);
        item1.setCreditAmount(BigDecimal.ZERO);
        item1.setSummary("分录1");

        ErpVoucherItemSaveReqVO item2 = new ErpVoucherItemSaveReqVO();
        item2.setSubjectId(200L);
        item2.setDebitAmount(BigDecimal.ZERO);
        item2.setCreditAmount(credit);
        item2.setSummary("分录2");

        vo.setItems(Arrays.asList(item1, item2));
        return vo;
    }

    private List<ErpAccountingSubjectDO> buildLeafSubjects() {
        return Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001").setSubjectName("现金").setIsLeaf(true).setEnable(true),
                new ErpAccountingSubjectDO().setId(200L).setSubjectCode("1002").setSubjectName("银行").setIsLeaf(true).setEnable(true)
        );
    }

    private ErpVoucherItemDO buildBalancedItemDO(Long subjectId, BigDecimal debit, BigDecimal credit) {
        return new ErpVoucherItemDO()
                .setSubjectId(subjectId)
                .setDebitAmount(debit)
                .setCreditAmount(credit);
    }

    // ==================== createVoucher ====================

    @Test
    void testCreateVoucher_success() {
        // 准备参数
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));
        nextNoRef.set("记-202605-000001");

        // mock
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(buildLeafSubjects());
        when(voucherMapper.selectByVoucherNo("记-202605-000001")).thenReturn(null);
        doAnswer(inv -> {
            ErpVoucherDO arg = inv.getArgument(0);
            arg.setId(999L);
            return null;
        }).when(voucherMapper).insert(any(ErpVoucherDO.class));

        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);

            // 调用
            Long id = voucherService.createVoucher(vo);

            // 断言
            assertEquals(999L, id);
            ArgumentCaptor<ErpVoucherDO> captor = ArgumentCaptor.forClass(ErpVoucherDO.class);
            verify(voucherMapper).insert(captor.capture());
            ErpVoucherDO saved = captor.getValue();
            assertEquals("记-202605-000001", saved.getVoucherNo());
            assertEquals(ErpVoucherAuditStatusEnum.PROCESS.getStatus(), saved.getAuditStatus());
            assertEquals(ErpVoucherSourceTypeEnum.MANUAL.getType(), saved.getSourceType());
            assertEquals(Boolean.FALSE, saved.getGenerateBusinessDoc());
            assertEquals(100L, saved.getMakerUserId());
            assertThat(saved.getTotalDebit()).isEqualByComparingTo("100.00");
            assertThat(saved.getTotalCredit()).isEqualByComparingTo("100.00");

            verify(voucherItemMapper).insertBatch(anyList());
        }
    }

    @Test
    void testCreateVoucher_emptyItems() {
        ErpVoucherSaveReqVO vo = new ErpVoucherSaveReqVO();
        vo.setVoucherDate(LocalDate.of(2026, 5, 15));
        vo.setItems(null);

        assertServiceException(() -> voucherService.createVoucher(vo), VOUCHER_ITEM_EMPTY);

        // empty list 也走同一分支
        vo.setItems(Collections.emptyList());
        assertServiceException(() -> voucherService.createVoucher(vo), VOUCHER_ITEM_EMPTY);
    }

    @Test
    void testCreateVoucher_debitCreditBothPositive() {
        ErpVoucherSaveReqVO vo = new ErpVoucherSaveReqVO();
        vo.setVoucherDate(LocalDate.of(2026, 5, 15));
        ErpVoucherItemSaveReqVO item = new ErpVoucherItemSaveReqVO();
        item.setSubjectId(100L);
        item.setDebitAmount(new BigDecimal("100.00"));
        item.setCreditAmount(new BigDecimal("100.00"));
        vo.setItems(singletonList(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.createVoucher(vo));
        assertEquals(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(), ex.getCode());
    }

    @Test
    void testCreateVoucher_debitCreditBothZero() {
        ErpVoucherSaveReqVO vo = new ErpVoucherSaveReqVO();
        vo.setVoucherDate(LocalDate.of(2026, 5, 15));
        ErpVoucherItemSaveReqVO item = new ErpVoucherItemSaveReqVO();
        item.setSubjectId(100L);
        item.setDebitAmount(BigDecimal.ZERO);
        item.setCreditAmount(BigDecimal.ZERO);
        vo.setItems(singletonList(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.createVoucher(vo));
        assertEquals(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(), ex.getCode());
    }

    @Test
    void testCreateVoucher_notBalanced() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("80.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.createVoucher(vo));
        assertEquals(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(), ex.getCode());
    }

    @Test
    void testCreateVoucher_subjectNotExists() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));

        // 只返回 1 个，而要求 2 个
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(
                singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001").setSubjectName("现金").setIsLeaf(true).setEnable(true))
        );

        assertServiceException(() -> voucherService.createVoucher(vo), ACCOUNTING_SUBJECT_NOT_EXISTS);
    }

    @Test
    void testCreateVoucher_subjectNotLeaf() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));

        // 一个非末级科目
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001").setSubjectName("现金").setIsLeaf(false),
                new ErpAccountingSubjectDO().setId(200L).setSubjectCode("1002").setSubjectName("银行").setIsLeaf(true).setEnable(true)
        ));

        assertServiceException(() -> voucherService.createVoucher(vo), ACCOUNTING_SUBJECT_NOT_LEAF);
    }

    @Test
    void testCreateVoucher_voucherNoExists() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));
        nextNoRef.set("记-202605-000001");

        when(subjectMapper.selectByIds(anyCollection())).thenReturn(buildLeafSubjects());
        when(voucherMapper.selectByVoucherNo("记-202605-000001")).thenReturn(new ErpVoucherDO());

        assertServiceException(() -> voucherService.createVoucher(vo), VOUCHER_NO_EXISTS);
    }

    // ==================== updateVoucher ====================

    @Test
    void testUpdateVoucher_success() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));
        vo.setId(500L);

        // mock 已存在的凭证（PROCESS 状态）
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setVoucherWord("记");
        existing.setVoucherNo("记-202605-000001");
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        existing.setSourceType(ErpVoucherSourceTypeEnum.MANUAL.getType());
        when(voucherMapper.selectById(500L)).thenReturn(existing);
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(buildLeafSubjects());

        voucherService.updateVoucher(vo);

        // 主表更新被调用
        ArgumentCaptor<ErpVoucherDO> captor = ArgumentCaptor.forClass(ErpVoucherDO.class);
        verify(voucherMapper).updateById(captor.capture());
        ErpVoucherDO saved = captor.getValue();
        // 关键字段保留原值
        assertEquals("记", saved.getVoucherWord());
        assertEquals("记-202605-000001", saved.getVoucherNo());
        assertEquals(ErpVoucherAuditStatusEnum.PROCESS.getStatus(), saved.getAuditStatus());

        // 子表先删后插
        verify(voucherItemMapper).deleteByVoucherId(500L);
        verify(voucherItemMapper).insertBatch(anyList());
    }

    @Test
    void testUpdateVoucher_notExists() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));
        vo.setId(999L);

        when(voucherMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> voucherService.updateVoucher(vo), VOUCHER_NOT_EXISTS);
    }

    @Test
    void testUpdateVoucher_notProcess() {
        ErpVoucherSaveReqVO vo = buildSaveReqVO(new BigDecimal("100.00"), new BigDecimal("100.00"));
        vo.setId(500L);

        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.updateVoucher(vo));
        assertEquals(VOUCHER_AUDIT_FAIL.getCode(), ex.getCode());
    }

    // ==================== deleteVoucher ====================

    @Test
    void testDeleteVoucher_success() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        voucherService.deleteVoucher(500L);

        verify(voucherMapper).deleteById(500L);
        verify(voucherItemMapper).deleteByVoucherId(500L);
    }

    @Test
    void testDeleteVoucher_notExists() {
        when(voucherMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> voucherService.deleteVoucher(999L), VOUCHER_NOT_EXISTS);
    }

    @Test
    void testDeleteVoucher_notProcess() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.deleteVoucher(500L));
        assertEquals(VOUCHER_AUDIT_FAIL.getCode(), ex.getCode());
    }

    // ==================== auditVoucher ====================

    @Test
    void testAuditVoucher_success() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        // 平衡的分录
        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("100.00"))
        );
        when(voucherItemMapper.selectListByVoucherId(500L)).thenReturn(items);
        when(voucherMapper.updateByIdAndAuditStatus(eq(500L),
                eq(ErpVoucherAuditStatusEnum.PROCESS.getStatus()), any(ErpVoucherDO.class))).thenReturn(1);
        // adminUserApi 调用安全降级
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("张三"));

        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);

            voucherService.auditVoucher(500L);

            verify(voucherMapper).updateByIdAndAuditStatus(eq(500L),
                    eq(ErpVoucherAuditStatusEnum.PROCESS.getStatus()), any(ErpVoucherDO.class));
        }
    }

    @Test
    void testAuditVoucher_notExists() {
        when(voucherMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> voucherService.auditVoucher(999L), VOUCHER_NOT_EXISTS);
    }

    @Test
    void testAuditVoucher_notProcess() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        assertServiceException(() -> voucherService.auditVoucher(500L), VOUCHER_AUDIT_FAIL);
    }

    @Test
    void testAuditVoucher_emptyItems() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);
        when(voucherItemMapper.selectListByVoucherId(500L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> voucherService.auditVoucher(500L), VOUCHER_ITEM_EMPTY);
    }

    @Test
    void testAuditVoucher_notBalanced() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        // 不平衡的分录
        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("80.00"))
        );
        when(voucherItemMapper.selectListByVoucherId(500L)).thenReturn(items);

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.auditVoucher(500L));
        assertEquals(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(), ex.getCode());
    }

    @Test
    void testAuditVoucher_optimisticLockFail() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("100.00"))
        );
        when(voucherItemMapper.selectListByVoucherId(500L)).thenReturn(items);
        when(voucherMapper.updateByIdAndAuditStatus(eq(500L),
                eq(ErpVoucherAuditStatusEnum.PROCESS.getStatus()), any(ErpVoucherDO.class))).thenReturn(0);
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);

            assertServiceException(() -> voucherService.auditVoucher(500L), VOUCHER_AUDIT_FAIL);
        }
    }

    // ==================== processVoucher ====================

    @Test
    void testProcessVoucher_success() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);
        when(voucherMapper.updateByIdAndAuditStatus(eq(500L),
                eq(ErpVoucherAuditStatusEnum.APPROVE.getStatus()), any(ErpVoucherDO.class))).thenReturn(1);

        voucherService.processVoucher(500L);

        verify(voucherMapper).updateByIdAndAuditStatus(eq(500L),
                eq(ErpVoucherAuditStatusEnum.APPROVE.getStatus()), any(ErpVoucherDO.class));
    }

    @Test
    void testProcessVoucher_notExists() {
        when(voucherMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> voucherService.processVoucher(999L), VOUCHER_NOT_EXISTS);
    }

    @Test
    void testProcessVoucher_notApprove() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);

        assertServiceException(() -> voucherService.processVoucher(500L), VOUCHER_PROCESS_FAIL);
    }

    @Test
    void testProcessVoucher_optimisticLockFail() {
        ErpVoucherDO existing = new ErpVoucherDO();
        existing.setId(500L);
        existing.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectById(500L)).thenReturn(existing);
        when(voucherMapper.updateByIdAndAuditStatus(eq(500L),
                eq(ErpVoucherAuditStatusEnum.APPROVE.getStatus()), any(ErpVoucherDO.class))).thenReturn(0);

        assertServiceException(() -> voucherService.processVoucher(500L), VOUCHER_PROCESS_FAIL);
    }

    // ==================== get / page / list ====================

    @Test
    void testGetVoucher() {
        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(500L);
        when(voucherMapper.selectById(500L)).thenReturn(voucher);

        ErpVoucherDO result = voucherService.getVoucher(500L);

        assertNotNull(result);
        assertEquals(500L, result.getId());
    }

    @Test
    void testGetVoucherPage() {
        ErpVoucherPageReqVO pageReqVO = new ErpVoucherPageReqVO();
        PageResult<ErpVoucherDO> expected = new PageResult<>(singletonList(new ErpVoucherDO()), 1L);
        when(voucherMapper.selectPage(pageReqVO)).thenReturn(expected);

        PageResult<ErpVoucherDO> result = voucherService.getVoucherPage(pageReqVO);

        assertEquals(1L, result.getTotal());
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    void testGetVoucherItemListByVoucherId() {
        ErpVoucherItemDO item = new ErpVoucherItemDO();
        item.setVoucherId(500L);
        when(voucherItemMapper.selectListByVoucherId(500L)).thenReturn(singletonList(item));

        List<ErpVoucherItemDO> result = voucherService.getVoucherItemListByVoucherId(500L);

        assertThat(result).hasSize(1);
        assertEquals(500L, result.get(0).getVoucherId());
    }

    // ==================== createVoucherFromBiz ====================

    @Test
    void testCreateVoucherFromBiz_success() {
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(buildLeafSubjects());
        nextNoRef.set("记-202605-000001");

        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("100.00"))
        );

        when(voucherMapper.selectByVoucherNo("记-202605-000001")).thenReturn(null);
        doAnswer(inv -> {
            ErpVoucherDO arg = inv.getArgument(0);
            arg.setId(888L);
            return null;
        }).when(voucherMapper).insert(any(ErpVoucherDO.class));

        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);

            Long id = voucherService.createVoucherFromBiz(
                    7, 1001L, "CGRK001",
                    new BigDecimal("100.00"),
                    LocalDate.of(2026, 5, 15),
                    "采购入库自动生成",
                    items);

            assertEquals(888L, id);
            ArgumentCaptor<ErpVoucherDO> captor = ArgumentCaptor.forClass(ErpVoucherDO.class);
            verify(voucherMapper).insert(captor.capture());
            ErpVoucherDO saved = captor.getValue();
            assertEquals(ErpVoucherSourceTypeEnum.AUTO.getType(), saved.getSourceType());
            assertEquals(Boolean.TRUE, saved.getGenerateBusinessDoc());
            assertEquals(7, saved.getSourceBizType());
            assertEquals(1001L, saved.getSourceBizId());
            assertEquals("CGRK001", saved.getSourceBizNo());
            assertEquals("采购入库自动生成", saved.getSummary());
            assertEquals("记-202605-000001", saved.getVoucherNo());
            verify(voucherItemMapper).insertBatch(anyList());
        }
    }

    @Test
    void testCreateVoucherFromBiz_emptyItems() {
        assertServiceException(() -> voucherService.createVoucherFromBiz(
                7, 1001L, "CGRK001", BigDecimal.ZERO, LocalDate.of(2026, 5, 15), "test", null), VOUCHER_ITEM_EMPTY);

        assertServiceException(() -> voucherService.createVoucherFromBiz(
                7, 1001L, "CGRK001", BigDecimal.ZERO, LocalDate.of(2026, 5, 15), "test", Collections.emptyList()),
                VOUCHER_ITEM_EMPTY);
    }

    @Test
    void testCreateVoucherFromBiz_notBalanced() {
        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("80.00"))
        );

        ServiceException ex = assertThrows(ServiceException.class, () -> voucherService.createVoucherFromBiz(
                7, 1001L, "CGRK001", new BigDecimal("100.00"), LocalDate.of(2026, 5, 15), "test", items));
        assertEquals(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(), ex.getCode());
        // 不应该走到 mapper.insert
        verify(voucherMapper, never()).insert(any(ErpVoucherDO.class));
    }

    @Test
    void testCreateVoucherFromBiz_voucherNoExists() {
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(buildLeafSubjects());
        nextNoRef.set("记-202605-000001");

        List<ErpVoucherItemDO> items = Arrays.asList(
                buildBalancedItemDO(100L, new BigDecimal("100.00"), BigDecimal.ZERO),
                buildBalancedItemDO(200L, BigDecimal.ZERO, new BigDecimal("100.00"))
        );

        when(voucherMapper.selectByVoucherNo("记-202605-000001")).thenReturn(new ErpVoucherDO());

        assertServiceException(() -> voucherService.createVoucherFromBiz(
                7, 1001L, "CGRK001", new BigDecimal("100.00"), LocalDate.of(2026, 5, 15), "test", items), VOUCHER_NO_EXISTS);
    }

}
