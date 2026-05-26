package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpBookOpenVoucherConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BOOK_OPEN_PERIOD_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpBookOpenServiceImpl} 单元测试。
 *
 * 覆盖：
 *  - createBookOpen：单号生成 / 三元组重复校验 / 默认 11 条 voucher_config / 扫描失败吞噬
 *  - updateBookOpen：存在性校验 / no 字段保护 / 不重置 voucher_config
 *  - deleteBookOpen：级联删除 + 不存在抛错
 *  - isVoucherTypeEnabled：5 种分支（启用/未启用 cfg/未启用 BookOpen/无 BookOpen/无 cfg）
 *  - getBookOpen / getBookOpenPage / getBookOpenVoucherConfigList / updateBookOpenVoucherConfigs：基础透传 + 校验
 *
 * 重点 Bug 暴露：
 *  - S5：scanAndGenerateVouchersAfterCreate 异常被静默吞掉，主流程仍提示开账成功
 *  - S6：BookOpen 的三元组校验依赖 chainName，相同 (fiscalYear, period) 不同 chainName 可重复开账
 *  - S7：默认 voucher_config 11 条，新增 ErpVoucherTypeEnum 时需手动同步（虽用 values() 但 enabled=true 是硬编码默认）
 */
@DisplayName("ErpBookOpenServiceImpl 单元测试")
public class ErpBookOpenServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpBookOpenServiceImpl bookOpenService;

    // ========== 基础 mapper / dao ==========
    @Mock
    private ErpBookOpenMapper bookOpenMapper;
    @Mock
    private ErpBookOpenVoucherConfigMapper voucherConfigMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private AdminUserApi adminUserApi;

    // ========== 凭证相关 ==========
    @Mock
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Mock
    private ErpVoucherService voucherService;
    @Mock
    private ErpVoucherMapper voucherMapper;

    // ========== 11 个业务表 Mapper ==========
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Mock
    private ErpPreReceiptMapper preReceiptMapper;
    @Mock
    private ErpPrePaymentMapper prePaymentMapper;
    @Mock
    private ErpPreReceivableMapper preReceivableMapper;
    @Mock
    private ErpOtherPayableMapper otherPayableMapper;
    @Mock
    private ErpStockInMapper stockInMapper;
    @Mock
    private ErpStockOutMapper stockOutMapper;
    @Mock
    private ErpStockOutItemMapper stockOutItemMapper;

    // ========== 业务 Service ==========
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpStockService stockService;

    // ========== 事务管理 ==========
    @Mock
    private PlatformTransactionManager transactionManager;

    // ---------- 公共构造工具 ----------

    /** 构造 SaveReqVO 基础对象（2026-05 总部） */
    private ErpBookOpenSaveReqVO buildBaseReq() {
        ErpBookOpenSaveReqVO req = new ErpBookOpenSaveReqVO();
        req.setChainName("总部");
        req.setFiscalYear(2026);
        req.setPeriod(5);
        req.setStartDate(LocalDate.of(2026, 5, 1));
        return req;
    }

    /** mock 单号生成相关：noRedisDAO.generate + selectByNo 返回 null */
    private void mockNoGeneration(String no) {
        when(noRedisDAO.generate(eq(ErpNoRedisDAO.BOOK_OPEN_NO_PREFIX))).thenReturn(no);
        lenient().when(bookOpenMapper.selectByNo(eq(no))).thenReturn(null);
    }

    /** mock insert 回填 ID */
    private void mockInsertReturnId(Long generatedId) {
        when(bookOpenMapper.insert(any(ErpBookOpenDO.class))).thenAnswer(inv -> {
            ((ErpBookOpenDO) inv.getArgument(0)).setId(generatedId);
            return 1;
        });
    }

    /** 让 scanAndGenerateVouchersAfterCreate 立即返回（voucher_config 列表为空跳过 scan） */
    private void mockSkipScan() {
        lenient().when(voucherConfigMapper.selectListByBookOpenId(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    // ==================== createBookOpen ====================

    @Test
    @DisplayName("createBookOpen：正常创建 - 生成单号 + 默认 11 条 voucher_config + 操作人回填")
    public void testCreate_normalCase() {
        // mock 三元组不重复
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(eq("总部"), eq(2026), eq(5)))
                .thenReturn(null);
        mockNoGeneration("KZ20260514000001");
        when(adminUserApi.getUser(eq(99L))).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1000L);
        mockSkipScan();

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            Long id = bookOpenService.createBookOpen(buildBaseReq());

            assertEquals(1000L, id);
        }

        // 验证主表插入字段
        ArgumentCaptor<ErpBookOpenDO> insertCaptor = ArgumentCaptor.forClass(ErpBookOpenDO.class);
        verify(bookOpenMapper).insert(insertCaptor.capture());
        ErpBookOpenDO captured = insertCaptor.getValue();
        assertEquals("KZ20260514000001", captured.getNo());
        assertEquals("总部", captured.getChainName());
        assertEquals(Boolean.TRUE, captured.getOpened());
        assertEquals(99L, captured.getOperatorUserId());
        assertEquals("admin", captured.getOperator());
        assertNotNull(captured.getOperateTime());
        // 验证默认 11 条 voucher_config
        verify(voucherConfigMapper, times(1)).insertBatch(any(Collection.class));
    }

    @Test
    @DisplayName("createBookOpen：默认 opened=true（DO 仅有 opened 字段，无 currentPeriod 设计）")
    public void testCreate_setOpenedTrueByDefault() {
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(any(), any(), any())).thenReturn(null);
        mockNoGeneration("KZ20260514000002");
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1001L);
        mockSkipScan();

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            bookOpenService.createBookOpen(buildBaseReq());
        }

        ArgumentCaptor<ErpBookOpenDO> captor = ArgumentCaptor.forClass(ErpBookOpenDO.class);
        verify(bookOpenMapper).insert(captor.capture());
        // Service 中 setOpened(true) 强制；不存在切换"当前期"逻辑
        assertEquals(Boolean.TRUE, captor.getValue().getOpened());
        // 未触发"清理其他 BookOpen 的 currentPeriod"逻辑（不存在该字段）
        verify(bookOpenMapper, never()).updateById(any(ErpBookOpenDO.class));
    }

    @Test
    @DisplayName("createBookOpen：fiscalYear 边界值（如 0）- 已修复 S5，应抛 BOOK_OPEN_PERIOD_INVALID")
    public void testCreate_invalidYear() {
        // S5 修复后：Service 层 validatePeriod 校验 fiscalYear ∈ [1900, 9999]，传 0 应被拒
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setFiscalYear(0);

        assertServiceException(() -> bookOpenService.createBookOpen(req),
                BOOK_OPEN_PERIOD_INVALID);

        verify(bookOpenMapper, never()).insert(any(ErpBookOpenDO.class));
    }

    @Test
    @DisplayName("createBookOpen：period 越界（如 13）- 已修复 S5，应抛 BOOK_OPEN_PERIOD_INVALID")
    public void testCreate_invalidPeriod() {
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setPeriod(13);

        assertServiceException(() -> bookOpenService.createBookOpen(req),
                BOOK_OPEN_PERIOD_INVALID);

        verify(bookOpenMapper, never()).insert(any(ErpBookOpenDO.class));
    }

    @Test
    @DisplayName("createBookOpen：Bug S6 暴露 - 三元组校验依赖 chainName，相同 (year, period) 不同 chainName 允许重复开账")
    public void testCreate_duplicatePeriod_silentlyAllowed_bugS6() {
        // 已有 chainName=分部A + 2026-05 的开账记录；
        // 现在新建 chainName=分部B + 2026-05；
        // selectByChainNameAndYearAndPeriod("分部B", 2026, 5) 因 chainName 不同返回 null，校验放行；
        // 暴露 Bug S6：当前实现按 (chainName, year, period) 三元组判重，不限制同期间多连锁开账。
        // TODO 若客户期望整月只允许 1 个 BookOpen（不区分 chainName），需在 validateBookOpenDuplicate 内
        //      改用 selectByYearAndPeriod（已存在该方法）做强校验。
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setChainName("分部B");

        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(eq("分部B"), eq(2026), eq(5)))
                .thenReturn(null); // 不同 chainName 查不出已有记录
        mockNoGeneration("KZ20260514000005");
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1004L);
        mockSkipScan();

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            Long id = bookOpenService.createBookOpen(req);
            // Bug S6：未拦截，返回新 ID（如客户期望严格唯一，此处应抛 BOOK_OPEN_DUPLICATE）
            assertNotNull(id);
            assertEquals(1004L, id);
        }
    }

    @Test
    @DisplayName("createBookOpen：Bug S5 暴露 - 凭证扫描中 voucherService 抛异常被静默吞噬，主流程仍返回成功 ID")
    public void testCreate_scanThrowsButMainFlowSuccessful_bugS5() {
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(any(), any(), any())).thenReturn(null);
        mockNoGeneration("KZ20260514000006");
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1005L);

        // scan 阶段：返回 SALE(1) enabled=true 的 cfg
        ErpBookOpenVoucherConfigDO cfg = ErpBookOpenVoucherConfigDO.builder()
                .voucherType(ErpVoucherTypeEnum.SALE.getType())
                .enabled(true)
                .build();
        when(voucherConfigMapper.selectListByBookOpenId(anyLong())).thenReturn(Arrays.asList(cfg));

        // saleOutMapper 返回 1 条
        ErpSaleOutDO saleOut = new ErpSaleOutDO();
        saleOut.setId(100L);
        saleOut.setNo("XSCK001");
        saleOut.setOutTime(LocalDateTime.of(2026, 5, 15, 10, 0));
        when(saleOutMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(saleOut));
        lenient().when(saleReturnMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // 凭证未存在
        lenient().when(voucherMapper.selectListByBiz(any(), anyLong())).thenReturn(Collections.emptyList());

        // mock 事务模板能跑通
        TransactionStatus txStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(txStatus);

        // 关键：让 createVoucherFromBiz 抛 RuntimeException
        when(voucherService.createVoucherFromBiz(any(), anyLong(), anyString(), any(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("模拟凭证生成失败"));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            // Bug S5：异常被三层吞噬（callback try-catch + voucherType try-catch + 顶层 try-catch）
            //        主流程 "开账成功" 返回 ID，调用方无感知凭证补生成失败
            Long id = bookOpenService.createBookOpen(buildBaseReq());

            assertNotNull(id);
            assertEquals(1005L, id);
        }
        // 确实尝试过生成凭证（抛异常被吞）
        verify(voucherService, times(1))
                .createVoucherFromBiz(any(), anyLong(), anyString(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("createBookOpen：Bug S7 固化 - 默认 voucher_config 数量 == ErpVoucherTypeEnum.values().length (当前 11)")
    public void testCreate_defaultConfig11Items_bugS7() {
        // Bug S7：当前实现用 ErpVoucherTypeEnum.values() 遍历生成默认 cfg，
        //        默认 enabled=true 是硬编码。今后若枚举新增类型，默认勾选行为需复核。
        //        本测试固化"开账时初始化与枚举值数等长的 cfg 列表"行为，便于回归。
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(any(), any(), any())).thenReturn(null);
        mockNoGeneration("KZ20260514000007");
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1006L);
        mockSkipScan();

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            bookOpenService.createBookOpen(buildBaseReq());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<ErpBookOpenVoucherConfigDO>> captor =
                ArgumentCaptor.forClass((Class) Collection.class);
        verify(voucherConfigMapper).insertBatch(captor.capture());
        Collection<ErpBookOpenVoucherConfigDO> all = captor.getValue();
        // 11 条与 ErpVoucherTypeEnum.values() 长度一致
        assertEquals(11, all.size());
        assertEquals(ErpVoucherTypeEnum.values().length, all.size(),
                "迁移新增 voucher_type 时此处需手动同步：当前默认全部 enabled=true 是硬编码");
        // 验证全部默认 enabled=true 且 sort 单调递增 1..11
        List<ErpBookOpenVoucherConfigDO> list = new ArrayList<>(all);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(Boolean.TRUE, list.get(i).getEnabled());
            assertEquals(i + 1, list.get(i).getSort());
            assertEquals(1006L, list.get(i).getBookOpenId());
        }
    }

    @Test
    @DisplayName("createBookOpen：单号前缀使用 BOOK_OPEN_NO_PREFIX (KZ)")
    public void testCreate_noPrefixGenerate_useFixedNo() {
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(any(), any(), any())).thenReturn(null);
        mockNoGeneration("KZ20260514000008");
        lenient().when(adminUserApi.getUser(anyLong())).thenReturn(new AdminUserRespDTO().setNickname("admin"));
        mockInsertReturnId(1007L);
        mockSkipScan();

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            bookOpenService.createBookOpen(buildBaseReq());
        }

        // 验证调用 noRedisDAO.generate(prefix="KZ")
        verify(noRedisDAO, times(1)).generate(eq("KZ"));
        verify(noRedisDAO, times(1)).generate(eq(ErpNoRedisDAO.BOOK_OPEN_NO_PREFIX));
    }

    // ==================== updateBookOpen ====================

    @Test
    @DisplayName("updateBookOpen：正常更新 - 校验存在 + 校验三元组 + updateById")
    public void testUpdate_normalCase() {
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setId(1000L);

        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(eq("总部"), eq(2026), eq(5)))
                .thenReturn(null);

        bookOpenService.updateBookOpen(req);

        ArgumentCaptor<ErpBookOpenDO> captor = ArgumentCaptor.forClass(ErpBookOpenDO.class);
        verify(bookOpenMapper).updateById(captor.capture());
        assertEquals(1000L, captor.getValue().getId());
        assertEquals("总部", captor.getValue().getChainName());
    }

    @Test
    @DisplayName("updateBookOpen：记录不存在 - 抛 BOOK_OPEN_NOT_EXISTS")
    public void testUpdate_notExists() {
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setId(999L);

        when(bookOpenMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> bookOpenService.updateBookOpen(req), BOOK_OPEN_NOT_EXISTS);
        verify(bookOpenMapper, never()).updateById(any(ErpBookOpenDO.class));
    }

    @Test
    @DisplayName("updateBookOpen：三元组冲突（其他 BookOpen 占用相同 chainName+year+period）- 抛 BOOK_OPEN_DUPLICATE")
    public void testUpdate_currentPeriodTrue_clearOthers() {
        // DO 不含 currentPeriod 字段；该用例改为校验三元组冲突场景（最贴近"切换当前期会顶替别人"语义）
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setId(1000L);

        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));
        // 已有 id=2000 的同三元组记录占位
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(eq("总部"), eq(2026), eq(5)))
                .thenReturn(new ErpBookOpenDO().setId(2000L));

        assertServiceException(() -> bookOpenService.updateBookOpen(req), BOOK_OPEN_DUPLICATE);
        verify(bookOpenMapper, never()).updateById(any(ErpBookOpenDO.class));
    }

    @Test
    @DisplayName("updateBookOpen：改 fiscalYear / period - Service 层允许，且不重置 voucher_config")
    public void testUpdate_changeFiscalYearOrPeriod() {
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setId(1000L);
        req.setFiscalYear(2027);
        req.setPeriod(8);

        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO()
                .setId(1000L).setFiscalYear(2026).setPeriod(5));
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(eq("总部"), eq(2027), eq(8)))
                .thenReturn(null);

        bookOpenService.updateBookOpen(req);

        ArgumentCaptor<ErpBookOpenDO> captor = ArgumentCaptor.forClass(ErpBookOpenDO.class);
        verify(bookOpenMapper).updateById(captor.capture());
        assertEquals(2027, captor.getValue().getFiscalYear());
        assertEquals(8, captor.getValue().getPeriod());
        // 不重置 voucher_config（与 createBookOpen 的初始化分开）
        verify(voucherConfigMapper, never()).deleteByBookOpenId(anyLong());
        verify(voucherConfigMapper, never()).insertBatch(any(Collection.class));
    }

    @Test
    @DisplayName("updateBookOpen：no 字段不会被请求 VO 覆盖（SaveReqVO 无 no 字段，BeanUtils 拷贝后 no=null）")
    public void testUpdate_keepNoUnchanged() {
        // SaveReqVO 未声明 no 字段，BeanUtils.toBean 后 DO.no 为 null；
        // MyBatis Plus 默认 FieldStrategy.NOT_NULL，null 字段不会写库，从而保留原 no。
        ErpBookOpenSaveReqVO req = buildBaseReq();
        req.setId(1000L);

        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setNo("KZ20260101000001"));
        when(bookOpenMapper.selectByChainNameAndYearAndPeriod(any(), any(), any())).thenReturn(null);

        bookOpenService.updateBookOpen(req);

        ArgumentCaptor<ErpBookOpenDO> captor = ArgumentCaptor.forClass(ErpBookOpenDO.class);
        verify(bookOpenMapper).updateById(captor.capture());
        // 关键：toBean 后 no=null，依赖 MyBatis Plus null 字段不写
        assertNull(captor.getValue().getNo());
    }

    // ==================== deleteBookOpen ====================

    @Test
    @DisplayName("deleteBookOpen：正常删除 - 级联清理 voucher_config + 删除主表")
    public void testDelete_normalCase() {
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));

        bookOpenService.deleteBookOpen(1000L);

        verify(voucherConfigMapper, times(1)).deleteByBookOpenId(eq(1000L));
        verify(bookOpenMapper, times(1)).deleteById(eq(1000L));
    }

    @Test
    @DisplayName("deleteBookOpen：记录不存在 - 抛 BOOK_OPEN_NOT_EXISTS")
    public void testDelete_notExists() {
        when(bookOpenMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> bookOpenService.deleteBookOpen(999L), BOOK_OPEN_NOT_EXISTS);
        verify(voucherConfigMapper, never()).deleteByBookOpenId(anyLong());
        verify(bookOpenMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteBookOpen：opened=true 当前期 - 按代码现状仍可删除（无业务保护）")
    public void testDelete_currentPeriod() {
        // DO 字段是 opened；Service 删除时不区分 opened 状态，按代码现状 opened=true 仍可删
        // TODO 若客户期望"开账后期间不可删"，需在此处追加 opened 校验
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setOpened(true));

        bookOpenService.deleteBookOpen(1000L);

        verify(bookOpenMapper, times(1)).deleteById(eq(1000L));
    }

    // ==================== isVoucherTypeEnabled ====================

    @Test
    @DisplayName("isVoucherTypeEnabled：BookOpen 启用 + 凭证类型启用 - 返回 true")
    public void testIsVoucherTypeEnabled_normalEnabled() {
        LocalDate bizDate = LocalDate.of(2026, 5, 15);
        when(bookOpenMapper.selectByYearAndPeriod(eq(2026), eq(5))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setOpened(true));
        when(voucherConfigMapper.selectByBookOpenIdAndVoucherType(eq(1000L), eq(7)))
                .thenReturn(ErpBookOpenVoucherConfigDO.builder().enabled(true).build());

        boolean result = bookOpenService.isVoucherTypeEnabled(bizDate, 7);

        assertTrue(result);
    }

    @Test
    @DisplayName("isVoucherTypeEnabled：凭证类型已关闭 - 返回 false")
    public void testIsVoucherTypeEnabled_disabled() {
        LocalDate bizDate = LocalDate.of(2026, 5, 15);
        when(bookOpenMapper.selectByYearAndPeriod(eq(2026), eq(5))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setOpened(true));
        when(voucherConfigMapper.selectByBookOpenIdAndVoucherType(eq(1000L), eq(7)))
                .thenReturn(ErpBookOpenVoucherConfigDO.builder().enabled(false).build());

        boolean result = bookOpenService.isVoucherTypeEnabled(bizDate, 7);

        assertFalse(result);
    }

    @Test
    @DisplayName("isVoucherTypeEnabled：该期间未开账 - 返回 false（warn 日志，不抛）")
    public void testIsVoucherTypeEnabled_noBookOpenForDate() {
        LocalDate bizDate = LocalDate.of(2027, 8, 20);
        when(bookOpenMapper.selectByYearAndPeriod(eq(2027), eq(8))).thenReturn(null);

        boolean result = bookOpenService.isVoucherTypeEnabled(bizDate, 7);

        assertFalse(result);
        verify(voucherConfigMapper, never()).selectByBookOpenIdAndVoucherType(anyLong(), any());
    }

    @Test
    @DisplayName("isVoucherTypeEnabled：cfg 表无对应类型 - 返回 false")
    public void testIsVoucherTypeEnabled_configNotExists() {
        LocalDate bizDate = LocalDate.of(2026, 5, 15);
        when(bookOpenMapper.selectByYearAndPeriod(eq(2026), eq(5))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setOpened(true));
        when(voucherConfigMapper.selectByBookOpenIdAndVoucherType(eq(1000L), eq(99)))
                .thenReturn(null);

        boolean result = bookOpenService.isVoucherTypeEnabled(bizDate, 99);

        assertFalse(result);
    }

    @Test
    @DisplayName("isVoucherTypeEnabled：BookOpen.opened=false 未启用 - 返回 false")
    public void testIsVoucherTypeEnabled_bookOpenInactive() {
        LocalDate bizDate = LocalDate.of(2026, 5, 15);
        when(bookOpenMapper.selectByYearAndPeriod(eq(2026), eq(5))).thenReturn(
                new ErpBookOpenDO().setId(1000L).setOpened(false));

        boolean result = bookOpenService.isVoucherTypeEnabled(bizDate, 7);

        assertFalse(result);
        verify(voucherConfigMapper, never()).selectByBookOpenIdAndVoucherType(anyLong(), any());
    }

    // ==================== get / page / list ====================

    @Test
    @DisplayName("getBookOpen：透传 mapper.selectById")
    public void testGetBookOpen_normal() {
        ErpBookOpenDO doMock = new ErpBookOpenDO().setId(1000L).setNo("KZ001");
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(doMock);

        ErpBookOpenDO result = bookOpenService.getBookOpen(1000L);

        assertNotNull(result);
        assertEquals(1000L, result.getId());
        assertEquals("KZ001", result.getNo());
    }

    @Test
    @DisplayName("getBookOpen：不存在 - 返回 null（不抛异常）")
    public void testGetBookOpen_notFound_returnNull() {
        when(bookOpenMapper.selectById(eq(999L))).thenReturn(null);

        ErpBookOpenDO result = bookOpenService.getBookOpen(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("getBookOpenPage：透传 mapper.selectPage - 正常分页返回")
    public void testGetBookOpenPage_normal() {
        ErpBookOpenPageReqVO req = new ErpBookOpenPageReqVO();
        PageResult<ErpBookOpenDO> page = new PageResult<>(
                Arrays.asList(new ErpBookOpenDO().setId(1L), new ErpBookOpenDO().setId(2L)), 2L);
        when(bookOpenMapper.selectPage(eq(req))).thenReturn(page);

        PageResult<ErpBookOpenDO> result = bookOpenService.getBookOpenPage(req);

        assertThat(result.getList()).hasSize(2);
        assertEquals(2L, result.getTotal());
    }

    @Test
    @DisplayName("getBookOpenPage：空结果 - 不抛异常，返回 total=0")
    public void testGetBookOpenPage_emptyResult() {
        ErpBookOpenPageReqVO req = new ErpBookOpenPageReqVO();
        when(bookOpenMapper.selectPage(eq(req))).thenReturn(
                new PageResult<>(Collections.emptyList(), 0L));

        PageResult<ErpBookOpenDO> result = bookOpenService.getBookOpenPage(req);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("getBookOpenPage：按 fiscalYear 过滤 - 透传 fiscalYear 入参到 Mapper")
    public void testGetBookOpenList_filterByYear() {
        ErpBookOpenPageReqVO req = new ErpBookOpenPageReqVO();
        req.setFiscalYear(2026);
        PageResult<ErpBookOpenDO> page = new PageResult<>(
                Collections.singletonList(new ErpBookOpenDO().setId(1L).setFiscalYear(2026)), 1L);
        when(bookOpenMapper.selectPage(eq(req))).thenReturn(page);

        PageResult<ErpBookOpenDO> result = bookOpenService.getBookOpenPage(req);

        assertEquals(1, result.getList().size());
        assertEquals(2026, result.getList().get(0).getFiscalYear());
        // 验证 fiscalYear 参数被透传
        verify(bookOpenMapper, times(1)).selectPage(eq(req));
    }

    // ==================== voucher_config 列表 / 批量更新 ====================

    @Test
    @DisplayName("getBookOpenVoucherConfigList：透传查询 - 含主表存在性校验 + 返回 11 条")
    public void testGetVoucherConfigList_normal() {
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));
        List<ErpBookOpenVoucherConfigDO> cfgs = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            cfgs.add(ErpBookOpenVoucherConfigDO.builder()
                    .bookOpenId(1000L).voucherType(i).enabled(true).sort(i).build());
        }
        when(voucherConfigMapper.selectListByBookOpenId(eq(1000L))).thenReturn(cfgs);

        List<ErpBookOpenVoucherConfigDO> result = bookOpenService.getBookOpenVoucherConfigList(1000L);

        assertThat(result).hasSize(11);
        verify(bookOpenMapper, times(1)).selectById(eq(1000L));
        verify(voucherConfigMapper, times(1)).selectListByBookOpenId(eq(1000L));
    }

    @Test
    @DisplayName("updateBookOpenVoucherConfigs：正常批量更新 - 先删后插 + 校验存在性")
    public void testUpdateVoucherConfigs_normal() {
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));

        ErpBookOpenVoucherConfigSaveReqVO req = new ErpBookOpenVoucherConfigSaveReqVO();
        req.setBookOpenId(1000L);
        List<ErpBookOpenVoucherConfigSaveReqVO.Item> items = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            ErpBookOpenVoucherConfigSaveReqVO.Item item = new ErpBookOpenVoucherConfigSaveReqVO.Item();
            item.setVoucherType(i);
            item.setEnabled(i % 2 == 0);
            item.setSort(i);
            items.add(item);
        }
        req.setItems(items);

        bookOpenService.updateBookOpenVoucherConfigs(req);

        verify(voucherConfigMapper, times(1)).deleteByBookOpenId(eq(1000L));
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<ErpBookOpenVoucherConfigDO>> captor =
                ArgumentCaptor.forClass((Class) Collection.class);
        verify(voucherConfigMapper, times(1)).insertBatch(captor.capture());
        assertEquals(11, captor.getValue().size());
    }

    @Test
    @DisplayName("updateBookOpenVoucherConfigs：只传 3 项 - 实际 insert 3 条（先删旧再插新，列表语义不需补齐）")
    public void testUpdateVoucherConfigs_partialUpdate() {
        when(bookOpenMapper.selectById(eq(1000L))).thenReturn(new ErpBookOpenDO().setId(1000L));

        ErpBookOpenVoucherConfigSaveReqVO req = new ErpBookOpenVoucherConfigSaveReqVO();
        req.setBookOpenId(1000L);
        List<ErpBookOpenVoucherConfigSaveReqVO.Item> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ErpBookOpenVoucherConfigSaveReqVO.Item item = new ErpBookOpenVoucherConfigSaveReqVO.Item();
            item.setVoucherType(i);
            item.setEnabled(true);
            // 不设置 sort，验证默认按下标 +1 兜底
            items.add(item);
        }
        req.setItems(items);

        bookOpenService.updateBookOpenVoucherConfigs(req);

        verify(voucherConfigMapper, times(1)).deleteByBookOpenId(eq(1000L));
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<ErpBookOpenVoucherConfigDO>> captor =
                ArgumentCaptor.forClass((Class) Collection.class);
        verify(voucherConfigMapper, times(1)).insertBatch(captor.capture());
        Collection<ErpBookOpenVoucherConfigDO> list = captor.getValue();
        assertEquals(3, list.size());
        // sort 兜底：i+1 = 1,2,3
        List<ErpBookOpenVoucherConfigDO> arr = new ArrayList<>(list);
        for (int i = 0; i < arr.size(); i++) {
            assertEquals(i + 1, arr.get(i).getSort());
            assertEquals(Boolean.TRUE, arr.get(i).getEnabled());
        }
    }

    @Test
    @DisplayName("updateBookOpenVoucherConfigs：主表不存在 - 抛 BOOK_OPEN_NOT_EXISTS 且不触发删除/插入")
    public void testUpdateVoucherConfigs_bookOpenNotExists() {
        when(bookOpenMapper.selectById(eq(999L))).thenReturn(null);

        ErpBookOpenVoucherConfigSaveReqVO req = new ErpBookOpenVoucherConfigSaveReqVO();
        req.setBookOpenId(999L);
        ErpBookOpenVoucherConfigSaveReqVO.Item item = new ErpBookOpenVoucherConfigSaveReqVO.Item();
        item.setVoucherType(1);
        item.setEnabled(true);
        req.setItems(Collections.singletonList(item));

        assertServiceException(() -> bookOpenService.updateBookOpenVoucherConfigs(req), BOOK_OPEN_NOT_EXISTS);
        verify(voucherConfigMapper, never()).deleteByBookOpenId(anyLong());
        verify(voucherConfigMapper, never()).insertBatch(any(Collection.class));
    }

}
