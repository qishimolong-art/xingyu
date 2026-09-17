package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDeptCreditDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDeptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerDeptCreditMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_BLOCKED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DEPT_CREDIT_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DEPT_CREDIT_DUPLICATE_DEPT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_SALE_DEPT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerServiceImpl} Mockito 单元测试
 */
public class ErpCustomerServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerServiceImpl customerService;

    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpCustomerDeptMapper customerDeptMapper;
    @Mock
    private ErpCustomerDeptCreditMapper customerDeptCreditMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Mock
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Mock
    private ErpReceivableWriteOffMapper receivableWriteOffMapper;
    @Mock
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Mock
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpReceivableAccountMapper receivableAccountMapper;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    /**
     * 在测试类加载时初始化 MyBatis-Plus 的 TableInfo 缓存（含 lambda 缓存）。
     * 否则纯 Mockito 上下文中调用 LambdaUpdateWrapper.set(SFunction, ...) 时
     * 会抛 "MybatisPlus can not find lambda cache for this entity" 异常。
     */
    @BeforeAll
    public static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpCustomerMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpCustomerDO.class);
    }

    @BeforeEach
    public void setUp() {
        // 注入匿名子类，固定返回 prefix + "20260520000001"
        ReflectionTestUtils.setField(customerService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    // ==================== generated sale validation ====================

    @Test
    public void testValidateCustomerForGeneratedSale_multiDeptMatch_success() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("大邑逸鑫汽修")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(true)
                .setCreditEnabled(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptMapper.selectListByCustomerId(14L)).thenReturn(Arrays.asList(
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(128L),
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(102L)));

        ErpCustomerDO result = customerService.validateCustomerForGeneratedSale(14L, 102L);

        assertSame(customer, result);
        verify(customerMapper).selectById(14L);
        verify(customerDeptMapper).selectListByCustomerId(14L);
    }

    @Test
    public void testValidateCustomerForGeneratedSale_primaryDeptMatch_success() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("大邑逸鑫汽修")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(false)
                .setCreditEnabled(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);

        assertSame(customer, customerService.validateCustomerForGeneratedSale(14L, 128L));
        verify(customerDeptMapper, never()).selectListByCustomerId(any());
    }

    @Test
    public void testValidateCustomerForGeneratedSale_deptNotAssigned_throwException() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("大邑逸鑫汽修")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(true)
                .setCreditEnabled(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptMapper.selectListByCustomerId(14L)).thenReturn(Collections.singletonList(
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(102L)));

        assertServiceException(() -> customerService.validateCustomerForGeneratedSale(14L, 132L),
                CUSTOMER_SALE_DEPT_NOT_ALLOWED);
    }

    @Test
    public void testValidateCustomerForGeneratedSale_customerInvalid_throwException() {
        when(customerMapper.selectById(14L)).thenReturn(null);
        assertServiceException(() -> customerService.validateCustomerForGeneratedSale(14L, 102L),
                CUSTOMER_NOT_EXISTS);

        ErpCustomerDO disabled = new ErpCustomerDO().setId(15L).setName("停用客户")
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(customerMapper.selectById(15L)).thenReturn(disabled);
        assertServiceException(() -> customerService.validateCustomerForGeneratedSale(15L, 102L),
                CUSTOMER_NOT_ENABLE, "停用客户");
    }

    @Test
    public void testValidateCustomerForGeneratedSale_creditBlocked_throwException() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("授信超限客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(102L).setAllowMultiDept(false)
                .setCreditEnabled(true).setCreditLimit(new BigDecimal("100"));
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(receivableAccountMapper.selectByCustomerId(14L)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("100")));
        when(saleOutMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(saleReturnMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(salePriceAdjustMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(financeReceiptMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableWriteOffMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableOtherMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertServiceException(() -> customerService.validateCustomerForGeneratedSale(14L, 102L),
                CUSTOMER_CREDIT_BLOCKED, "授信超限客户", "欠款金额 100 已达到授信金额 100");
    }

    @Test
    public void testGetCustomerSaleDeptIdsIgnoreDataPermission_success() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("小程序客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(true);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptMapper.selectListByCustomerId(14L)).thenReturn(Arrays.asList(
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(128L),
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(102L)));

        List<Long> result = customerService.getCustomerSaleDeptIdsIgnoreDataPermission(14L);

        assertEquals(Arrays.asList(128L, 102L), result);
        verify(customerMapper).selectById(14L);
        verify(customerDeptMapper).selectListByCustomerId(14L);
    }

    @Test
    public void testGetCustomerSaleDeptIdsIgnoreDataPermission_customerInvalid_throwException() {
        when(customerMapper.selectById(14L)).thenReturn(null);
        assertServiceException(() -> customerService.getCustomerSaleDeptIdsIgnoreDataPermission(14L),
                CUSTOMER_NOT_EXISTS);

        ErpCustomerDO disabled = new ErpCustomerDO().setId(15L).setName("停用客户")
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(customerMapper.selectById(15L)).thenReturn(disabled);
        assertServiceException(() -> customerService.getCustomerSaleDeptIdsIgnoreDataPermission(15L),
                CUSTOMER_NOT_ENABLE, "停用客户");
    }

    @Test
    public void testValidateCustomerForSale_deptCreditDisabled_bypassGlobalCredit() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("部门授信关闭客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(102L)
                .setCreditEnabled(true).setCreditLimit(new BigDecimal("100"));
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptCreditMapper.selectByCustomerIdAndDeptId(14L, 102L)).thenReturn(
                new ErpCustomerDeptCreditDO().setCustomerId(14L).setDeptId(102L).setCreditEnabled(false));

        assertSame(customer, customerService.validateCustomerForSale(14L, 102L));

        verify(receivableAccountMapper, never()).selectByCustomerId(any());
        verify(receivableAccountMapper, never()).selectByCustomerIdAndDeptId(any(), any());
    }

    @Test
    public void testValidateCustomerForSale_withoutDeptCredit_fallbackGlobalCredit() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("全局授信客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(102L)
                .setCreditEnabled(true).setCreditLimit(new BigDecimal("100"));
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptCreditMapper.selectByCustomerIdAndDeptId(14L, 102L)).thenReturn(null);
        when(receivableAccountMapper.selectByCustomerId(14L)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("100")));
        when(saleOutMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(saleReturnMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(salePriceAdjustMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(financeReceiptMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableWriteOffMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableOtherMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertServiceException(() -> customerService.validateCustomerForSale(14L, 102L),
                CUSTOMER_CREDIT_BLOCKED, "全局授信客户", "欠款金额 100 已达到授信金额 100");
        verify(receivableAccountMapper).selectByCustomerId(14L);
        verify(receivableAccountMapper, never()).selectByCustomerIdAndDeptId(any(), any());
    }

    @Test
    public void testValidateCustomerForSale_deptCreditEnabled_useDeptBalance() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("部门授信客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(102L)
                .setCreditEnabled(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptCreditMapper.selectByCustomerIdAndDeptId(14L, 102L)).thenReturn(
                new ErpCustomerDeptCreditDO().setCustomerId(14L).setDeptId(102L)
                        .setCreditEnabled(true).setCreditLimit(new BigDecimal("100")));
        when(receivableAccountMapper.selectByCustomerIdAndDeptId(14L, 102L)).thenReturn(
                new ErpReceivableAccountDO().setReceivableBalance(new BigDecimal("100")));
        when(saleOutMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(saleReturnMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(salePriceAdjustMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(financeReceiptMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableWriteOffMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(receivableOtherMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertServiceException(() -> customerService.validateCustomerForSale(14L, 102L),
                CUSTOMER_CREDIT_BLOCKED, "部门授信客户", "欠款金额 100 已达到授信金额 100");
        verify(receivableAccountMapper).selectByCustomerIdAndDeptId(14L, 102L);
        verify(receivableAccountMapper, never()).selectByCustomerId(any());
    }

    // ==================== department credit ====================

    @Test
    public void testGetCustomerDeptCredit_returnAssignedDeptItems() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("部门授信客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(true);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptMapper.selectListByCustomerId(14L)).thenReturn(Arrays.asList(
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(128L),
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(102L)));
        when(customerDeptCreditMapper.selectListByCustomerId(14L)).thenReturn(Collections.singletonList(
                new ErpCustomerDeptCreditDO().setId(900L).setCustomerId(14L).setDeptId(102L)
                        .setCreditEnabled(true).setCreditLimit(new BigDecimal("5000"))
                        .setCreditTermDays(30).setRemark("重点部门")));
        Map<Long, DeptRespDTO> deptMap = new LinkedHashMap<>();
        deptMap.put(128L, buildDept(128L, "总部"));
        deptMap.put(102L, buildDept(102L, "销售一部"));
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);

        ErpCustomerDeptCreditRespVO result = customerService.getCustomerDeptCredit(14L);

        assertEquals(14L, result.getId());
        assertEquals(Arrays.asList(128L, 102L), result.getDeptIds());
        assertEquals(2, result.getItems().size());
        assertEquals(128L, result.getItems().get(0).getDeptId());
        assertEquals(false, result.getItems().get(0).getCreditEnabled());
        assertEquals(102L, result.getItems().get(1).getDeptId());
        assertEquals(900L, result.getItems().get(1).getId());
        assertEquals(new BigDecimal("5000"), result.getItems().get(1).getCreditLimit());
        assertEquals("销售一部", result.getItems().get(1).getDeptName());
    }

    @Test
    public void testUpdateCustomerDeptCredit_replaceConfigs() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setName("部门授信客户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(128L).setAllowMultiDept(true);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        when(customerDeptMapper.selectListByCustomerId(14L)).thenReturn(Arrays.asList(
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(128L),
                new ErpCustomerDeptDO().setCustomerId(14L).setDeptId(102L)));
        ErpCustomerDeptCreditSaveReqVO reqVO = new ErpCustomerDeptCreditSaveReqVO();
        reqVO.setId(14L);
        ErpCustomerDeptCreditSaveReqVO.Item enabledItem = new ErpCustomerDeptCreditSaveReqVO.Item();
        enabledItem.setDeptId(102L);
        enabledItem.setCreditEnabled(true);
        enabledItem.setCreditLimit(new BigDecimal("5000"));
        enabledItem.setCreditTermDays(30);
        ErpCustomerDeptCreditSaveReqVO.Item disabledItem = new ErpCustomerDeptCreditSaveReqVO.Item();
        disabledItem.setDeptId(128L);
        disabledItem.setCreditEnabled(false);
        disabledItem.setCreditLimit(new BigDecimal("999"));
        disabledItem.setCreditTermDays(10);
        reqVO.setItems(Arrays.asList(enabledItem, disabledItem));

        customerService.updateCustomerDeptCredit(reqVO);

        verify(deptApi).validateDeptList(argThat(deptIds -> deptIds.containsAll(Arrays.asList(102L, 128L))
                && deptIds.size() == 2));
        verify(customerDeptCreditMapper).deleteByCustomerId(14L);
        verify(customerDeptCreditMapper).insertBatch(ArgumentMatchers.<Collection<ErpCustomerDeptCreditDO>>argThat(credits -> {
            List<ErpCustomerDeptCreditDO> list = Arrays.asList(credits.toArray(new ErpCustomerDeptCreditDO[0]));
            ErpCustomerDeptCreditDO enabled = list.stream()
                    .filter(credit -> Long.valueOf(102L).equals(credit.getDeptId()))
                    .findFirst().orElse(null);
            ErpCustomerDeptCreditDO disabled = list.stream()
                    .filter(credit -> Long.valueOf(128L).equals(credit.getDeptId()))
                    .findFirst().orElse(null);
            return list.size() == 2
                    && enabled != null
                    && Long.valueOf(14L).equals(enabled.getCustomerId())
                    && Boolean.TRUE.equals(enabled.getCreditEnabled())
                    && new BigDecimal("5000").equals(enabled.getCreditLimit())
                    && Integer.valueOf(30).equals(enabled.getCreditTermDays())
                    && disabled != null
                    && Boolean.FALSE.equals(disabled.getCreditEnabled())
                    && disabled.getCreditLimit() == null
                    && disabled.getCreditTermDays() == null;
        }));
    }

    @Test
    public void testUpdateCustomerDeptCredit_duplicateDept_throwException() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setDeptId(128L).setAllowMultiDept(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        ErpCustomerDeptCreditSaveReqVO reqVO = new ErpCustomerDeptCreditSaveReqVO();
        reqVO.setId(14L);
        ErpCustomerDeptCreditSaveReqVO.Item first = new ErpCustomerDeptCreditSaveReqVO.Item();
        first.setDeptId(128L);
        ErpCustomerDeptCreditSaveReqVO.Item second = new ErpCustomerDeptCreditSaveReqVO.Item();
        second.setDeptId(128L);
        reqVO.setItems(Arrays.asList(first, second));

        assertServiceException(() -> customerService.updateCustomerDeptCredit(reqVO),
                CUSTOMER_DEPT_CREDIT_DUPLICATE_DEPT);
        verify(customerDeptCreditMapper, never()).deleteByCustomerId(any());
        verify(customerDeptCreditMapper, never()).insertBatch(any());
    }

    @Test
    public void testUpdateCustomerDeptCredit_deptNotAllowed_throwException() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setDeptId(128L).setAllowMultiDept(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        ErpCustomerDeptCreditSaveReqVO reqVO = new ErpCustomerDeptCreditSaveReqVO();
        reqVO.setId(14L);
        ErpCustomerDeptCreditSaveReqVO.Item item = new ErpCustomerDeptCreditSaveReqVO.Item();
        item.setDeptId(999L);
        reqVO.setItems(Collections.singletonList(item));

        assertServiceException(() -> customerService.updateCustomerDeptCredit(reqVO),
                CUSTOMER_DEPT_CREDIT_DEPT_NOT_ALLOWED);
        verify(customerDeptCreditMapper, never()).deleteByCustomerId(any());
        verify(customerDeptCreditMapper, never()).insertBatch(any());
    }

    @Test
    public void testUpdateCustomerDeptCredit_enabledWithoutConfig_throwException() {
        ErpCustomerDO customer = new ErpCustomerDO().setId(14L).setDeptId(128L).setAllowMultiDept(false);
        when(customerMapper.selectById(14L)).thenReturn(customer);
        ErpCustomerDeptCreditSaveReqVO reqVO = new ErpCustomerDeptCreditSaveReqVO();
        reqVO.setId(14L);
        ErpCustomerDeptCreditSaveReqVO.Item item = new ErpCustomerDeptCreditSaveReqVO.Item();
        item.setDeptId(128L);
        item.setCreditEnabled(true);
        reqVO.setItems(Collections.singletonList(item));

        assertServiceException(() -> customerService.updateCustomerDeptCredit(reqVO),
                CUSTOMER_CREDIT_CONFIG_REQUIRED);
        verify(customerDeptCreditMapper, never()).deleteByCustomerId(any());
        verify(customerDeptCreditMapper, never()).insertBatch(any());
    }

    // ==================== create ====================

    @Test
    public void testCreateCustomer_normalCase_returnId() {
        // 准备：未传 code/memberCode/platformCode/sort，由 Service 自动生成
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setName("测试客户");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // mock：insert 时回填 id
        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(123L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));

        // 执行
        Long resultId = customerService.createCustomer(reqVO);

        // 断言
        assertNotNull(resultId);
        assertEquals(123L, resultId);
        // 校验：自动生成编码 + sort 默认 0
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "测试客户".equals(customer.getName())
                        && (ErpNoRedisDAO.CUSTOMER_NO_PREFIX + "20260520000001").equals(customer.getCode())
                        && (ErpNoRedisDAO.MEMBER_NO_PREFIX + "20260520000001").equals(customer.getMemberCode())
                        && (ErpNoRedisDAO.PLATFORM_NO_PREFIX + "20260520000001").equals(customer.getPlatformCode())
                        && Integer.valueOf(0).equals(customer.getSort())));
    }

    @Test
    public void testCreateCustomer_withProvidedCodes_keepOriginal() {
        // 准备：用户已传 code/memberCode/platformCode/sort，不应被覆盖
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setCode(" CUSTOM001 ");
        reqVO.setMemberCode("MEMBER001");
        reqVO.setPlatformCode("PLAT001");
        reqVO.setName("自定义客户");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setSort(99);

        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(124L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));

        // 执行
        Long resultId = customerService.createCustomer(reqVO);

        // 断言
        assertEquals(124L, resultId);
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "CUSTOM001".equals(customer.getCode())
                        && "MEMBER001".equals(customer.getMemberCode())
                        && "PLAT001".equals(customer.getPlatformCode())
                        && Integer.valueOf(99).equals(customer.getSort())));
    }

    @Test
    public void testCreateCustomer_withBlankManualCode_generateCode() {
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setCode("   ");
        reqVO.setName("空白编码客户");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(125L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));

        Long resultId = customerService.createCustomer(reqVO);

        assertEquals(125L, resultId);
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                (ErpNoRedisDAO.CUSTOMER_NO_PREFIX + "20260520000001").equals(customer.getCode())));
    }

    @Test
    public void testCreateCustomer_duplicateManualCode_throwException() {
        when(customerMapper.selectByCodeExcludeId(eq("CUSTOM001"), eq(null)))
                .thenReturn(new ErpCustomerDO().setId(10L).setCode("CUSTOM001"));
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setCode(" CUSTOM001 ");
        reqVO.setName("重复编码客户");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        assertServiceException(() -> customerService.createCustomer(reqVO),
                CUSTOMER_CODE_DUPLICATE, "CUSTOM001");
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
    }

    @Test
    public void testCreateCustomer_duplicateName_throwException() {
        when(customerMapper.selectByNameExcludeId(eq("重复客户"), eq(null)))
                .thenReturn(new ErpCustomerDO().setId(10L).setName("重复客户"));
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setCode("CUSTOM002");
        reqVO.setName(" 重复客户 ");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        assertServiceException(() -> customerService.createCustomer(reqVO),
                CUSTOMER_NAME_DUPLICATE, "重复客户");
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
    }

    // ==================== update ====================

    @Test
    public void testUpdateCustomer_normalCase_success() {
        Long id = 200L;
        // 准备：existing 客户存在
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("旧名称")
                .setCode("KH000200")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(customerMapper.selectById(eq(id))).thenReturn(exist);

        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setId(id);
        reqVO.setName("新名称");
        reqVO.setCode("SHOULD-NOT-CHANGE");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // 执行
        customerService.updateCustomer(reqVO);

        // 断言：updateById 被调用，name 已更新
        verify(customerMapper).updateById(ArgumentMatchers.<ErpCustomerDO>argThat(update ->
                id.equals(update.getId())
                        && "新名称".equals(update.getName())
                        && "KH000200".equals(update.getCode())));
    }

    @Test
    public void testUpdateCustomer_notExists_throwException() {
        Long id = 201L;
        // 准备：客户不存在
        when(customerMapper.selectById(eq(id))).thenReturn(null);

        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setId(id);
        reqVO.setName("不存在");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // 执行 & 断言
        assertServiceException(() -> customerService.updateCustomer(reqVO), CUSTOMER_NOT_EXISTS);
        // 未走到 updateById
        verify(customerMapper, never()).updateById(any(ErpCustomerDO.class));
    }

    @Test
    public void testUpdateCustomer_duplicateName_throwException() {
        Long id = 202L;
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("旧名称")
                .setCode("KH000202")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(customerMapper.selectById(eq(id))).thenReturn(exist);
        when(customerMapper.selectByNameExcludeId(eq("重复客户"), eq(id)))
                .thenReturn(new ErpCustomerDO().setId(203L).setName("重复客户"));

        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setId(id);
        reqVO.setName(" 重复客户 ");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        assertServiceException(() -> customerService.updateCustomer(reqVO),
                CUSTOMER_NAME_DUPLICATE, "重复客户");
        verify(customerMapper, never()).updateById(any(ErpCustomerDO.class));
    }

    // ==================== delete ====================

    @Test
    public void testDeleteCustomer_normalCase_success() {
        Long id = 300L;
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("待删除")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(customerMapper.selectById(eq(id))).thenReturn(exist);

        // 执行
        customerService.deleteCustomer(id);

        // 断言
        verify(customerMapper).deleteById(eq(id));
    }

    @Test
    public void testDeleteCustomer_notExists_throwException() {
        Long id = 301L;
        when(customerMapper.selectById(eq(id))).thenReturn(null);

        // 执行 & 断言
        assertServiceException(() -> customerService.deleteCustomer(id), CUSTOMER_NOT_EXISTS);
        verify(customerMapper, never()).deleteById(ArgumentMatchers.<Long>any());
    }

    // ==================== getCustomer / validateCustomer ====================

    @Test
    public void testGetCustomer_returnDO() {
        Long id = 400L;
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("客户A");
        when(customerMapper.selectById(eq(id))).thenReturn(exist);

        ErpCustomerDO result = customerService.getCustomer(id);

        assertSame(exist, result);
    }

    @Test
    public void testValidateCustomer_normalCase_returnDO() {
        Long id = 410L;
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("客户B")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(customerMapper.selectById(eq(id))).thenReturn(exist);

        ErpCustomerDO result = customerService.validateCustomer(id);

        assertSame(exist, result);
    }

    @Test
    public void testValidateCustomer_notExists_throwException() {
        Long id = 411L;
        when(customerMapper.selectById(eq(id))).thenReturn(null);

        assertServiceException(() -> customerService.validateCustomer(id), CUSTOMER_NOT_EXISTS);
    }

    @Test
    public void testValidateCustomer_disabled_throwException() {
        Long id = 412L;
        ErpCustomerDO exist = new ErpCustomerDO().setId(id).setName("已停用客户")
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(customerMapper.selectById(eq(id))).thenReturn(exist);

        // 异常带客户名称作为 msgArg
        assertServiceException(() -> customerService.validateCustomer(id), CUSTOMER_NOT_ENABLE, "已停用客户");
    }

    // ==================== list / page / listByStatus ====================

    @Test
    public void testGetCustomerList_returnList() {
        List<Long> ids = Arrays.asList(500L, 501L);
        List<ErpCustomerDO> list = Arrays.asList(
                new ErpCustomerDO().setId(500L), new ErpCustomerDO().setId(501L));
        when(customerMapper.selectByIds(eq(ids))).thenReturn(list);

        List<ErpCustomerDO> result = customerService.getCustomerList(ids);

        assertEquals(2, result.size());
        assertSame(list, result);
    }

    @Test
    public void testGetCustomerList_emptyIds_skipMapper() {
        assertTrue(customerService.getCustomerList(null).isEmpty());
        assertTrue(customerService.getCustomerList(Collections.emptyList()).isEmpty());

        verify(customerMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetCustomerPage_returnPage() {
        ErpCustomerPageReqVO reqVO = new ErpCustomerPageReqVO();
        reqVO.setName("张");
        PageResult<ErpCustomerDO> page = new PageResult<>(
                Collections.singletonList(new ErpCustomerDO().setId(600L).setName("张三")), 1L);
        when(customerMapper.selectPage(eq(reqVO))).thenReturn(page);

        PageResult<ErpCustomerDO> result = customerService.getCustomerPage(reqVO);

        assertSame(page, result);
    }

    @Test
    public void testGetCustomerListByStatus_returnList() {
        Integer status = CommonStatusEnum.ENABLE.getStatus();
        List<ErpCustomerDO> list = Collections.singletonList(
                new ErpCustomerDO().setId(700L).setStatus(status));
        when(customerMapper.selectListByStatus(eq(status))).thenReturn(list);

        List<ErpCustomerDO> result = customerService.getCustomerListByStatus(status);

        assertSame(list, result);
    }

    // ==================== importCustomerList ====================

    @Test
    public void testImportCustomerList_normalCase_insertAll() {
        // 准备：两条合法 + 一条 name 为空（跳过）+ 一条 null（跳过）
        ErpCustomerImportExcelVO valid1 = new ErpCustomerImportExcelVO();
        valid1.setName("导入客户A");
        valid1.setCode("IMP001");
        // status / sort 缺省，应被自动填默认

        ErpCustomerImportExcelVO valid2 = new ErpCustomerImportExcelVO();
        valid2.setName("导入客户B");
        valid2.setStatus(CommonStatusEnum.DISABLE.getStatus());
        valid2.setSort(50);

        ErpCustomerImportExcelVO blankName = new ErpCustomerImportExcelVO();
        blankName.setName("");
        blankName.setCode("BLANK");

        List<ErpCustomerImportExcelVO> list = Arrays.asList(valid1, valid2, blankName, null);

        // 执行
        ErpCustomerImportRespVO result = customerService.importCustomerList(list);

        // 断言：合法 2 条被 insert，name 空和 null 跳过
        assertEquals(2, result.getSuccessCount());
        assertEquals(2, result.getCreateCount());
        assertEquals(0, result.getUpdateCount());
        assertEquals(0, result.getFailureCount());
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "导入客户A".equals(customer.getName())
                        && CommonStatusEnum.ENABLE.getStatus().equals(customer.getStatus())
                        && Integer.valueOf(0).equals(customer.getSort())
                        && ErpMnemonicCodeUtils.buildPinyinCode("导入客户A").equals(customer.getPinyinCode())
                        && ErpMnemonicCodeUtils.buildWubiCode("导入客户A").equals(customer.getWubiCode())));
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "导入客户B".equals(customer.getName())
                        && CommonStatusEnum.DISABLE.getStatus().equals(customer.getStatus())
                        && Integer.valueOf(50).equals(customer.getSort())
                        && ErpMnemonicCodeUtils.buildPinyinCode("导入客户B").equals(customer.getPinyinCode())
                        && ErpMnemonicCodeUtils.buildWubiCode("导入客户B").equals(customer.getWubiCode())));
        // blankName 和 null 都被跳过：只 insert 2 次
        verify(customerMapper, org.mockito.Mockito.times(2)).insert(any(ErpCustomerDO.class));
    }

    @Test
    public void testImportCustomerList_existingCode_updateWithoutBlankOverwrite() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("更新客户");
        row.setCode(" EXIST001 ");
        row.setContact(" ");
        row.setMobile("13900000000");
        row.setTelephone("");
        row.setDetailAddress(null);
        row.setTaxNo("TAX001");

        ErpCustomerDO existing = new ErpCustomerDO().setId(900L).setCode("EXIST001").setName("旧客户")
                .setContact("旧联系人").setMobile("13800000000").setTelephone("028-0000")
                .setDetailAddress("旧地址").setStatus(CommonStatusEnum.ENABLE.getStatus()).setSort(20);
        when(customerMapper.selectListByCodes(any())).thenReturn(Collections.singletonList(existing));
        when(customerMapper.selectById(eq(900L))).thenReturn(existing.setName("更新客户").setMobile("13900000000"));

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getCreateCount());
        assertEquals(1, result.getUpdateCount());
        assertEquals(0, result.getFailureCount());
        verify(customerMapper).updateById(ArgumentMatchers.<ErpCustomerDO>argThat(update ->
                Long.valueOf(900L).equals(update.getId())
                        && "EXIST001".equals(update.getCode())
                        && "更新客户".equals(update.getName())
                        && update.getContact() == null
                        && "13900000000".equals(update.getMobile())
                        && update.getTelephone() == null
                        && update.getDetailAddress() == null
                        && "TAX001".equals(update.getTaxNo())
                        && ErpMnemonicCodeUtils.buildPinyinCode("更新客户").equals(update.getPinyinCode())
                        && ErpMnemonicCodeUtils.buildWubiCode("更新客户").equals(update.getWubiCode())
                        && update.getStatus() == null
                        && update.getSort() == null));
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
        verify(customerDeptMapper, never()).deleteByCustomerId(eq(900L));
    }

    @Test
    public void testImportCustomerList_partialFailure_continueNextRow() {
        ErpCustomerImportExcelVO failed = new ErpCustomerImportExcelVO();
        failed.setName("失败客户");
        failed.setCode("FAIL001");
        ErpCustomerImportExcelVO success = new ErpCustomerImportExcelVO();
        success.setName("成功客户");
        success.setCode("OK001");
        when(customerMapper.selectByCodeExcludeId(eq("FAIL001"), eq(null)))
                .thenThrow(new IllegalArgumentException("模拟失败"));

        ErpCustomerImportRespVO result = customerService.importCustomerList(Arrays.asList(failed, success));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getUpdateCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(2, result.getFailureDetails().get(0).getRowNo());
        assertEquals("FAIL001", result.getFailureDetails().get(0).getCode());
        assertEquals("模拟失败", result.getFailureDetails().get(0).getReason());
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "成功客户".equals(customer.getName()) && "OK001".equals(customer.getCode())));
    }

    @Test
    public void testImportCustomerList_createWithOwnerDept() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("部门客户");
        row.setCode("DEPT001");
        row.setDeptName("销售部");
        mockImportDeptContext(true);
        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(501L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                Long.valueOf(10L).equals(customer.getDeptId())
                        && Boolean.FALSE.equals(customer.getAllowMultiDept())));
        verify(customerDeptMapper).deleteByCustomerId(501L);
        verify(customerDeptMapper, never()).insertBatch(any());
    }

    @Test
    public void testImportCustomerList_createWithMultiDept() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("多部门客户");
        row.setCode("DEPT002");
        row.setDeptNames("销售部 / 一组、销售部 / 二组");
        mockImportDeptContext(true);
        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(502L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                Long.valueOf(11L).equals(customer.getDeptId())
                        && Boolean.TRUE.equals(customer.getAllowMultiDept())));
        verify(customerDeptMapper).insertBatch(ArgumentMatchers.<Collection<ErpCustomerDeptDO>>argThat(depts ->
                depts.stream().map(ErpCustomerDeptDO::getDeptId).collect(java.util.stream.Collectors.toList())
                        .containsAll(Arrays.asList(11L, 12L)) && depts.size() == 2));
    }

    @Test
    public void testImportCustomerList_existingCode_overwriteDeptDistribution() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("更新部门客户");
        row.setCode("EXIST002");
        row.setDeptName("售后部");
        row.setDeptNames("售后部;销售部 / 二组");
        ErpCustomerDO existing = new ErpCustomerDO().setId(902L).setCode("EXIST002")
                .setName("旧客户").setDeptId(10L).setAllowMultiDept(true);
        when(customerMapper.selectListByCodes(any())).thenReturn(Collections.singletonList(existing));
        when(customerMapper.selectById(eq(902L))).thenReturn(existing);
        mockImportDeptContext(true);

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getUpdateCount());
        assertEquals(0, result.getFailureCount());
        verify(customerMapper).updateById(ArgumentMatchers.<ErpCustomerDO>argThat(update ->
                Long.valueOf(902L).equals(update.getId())
                        && Long.valueOf(20L).equals(update.getDeptId())
                        && Boolean.TRUE.equals(update.getAllowMultiDept())));
        verify(customerDeptMapper).deleteByCustomerId(902L);
        verify(customerDeptMapper).insertBatch(ArgumentMatchers.<Collection<ErpCustomerDeptDO>>argThat(depts ->
                depts.stream().map(ErpCustomerDeptDO::getDeptId).collect(java.util.stream.Collectors.toList())
                        .containsAll(Arrays.asList(20L, 12L)) && depts.size() == 2));
    }

    @Test
    public void testImportCustomerList_deptWithoutDistributePermission_failure() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("无权限客户");
        row.setCode("NOPERM001");
        row.setDeptName("销售部");
        when(permissionApi.hasAnyPermissions(any(), eq("erp:customer:dept-distribute"))).thenReturn(false);

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("无客户分配部门权限"));
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
    }

    @Test
    public void testImportCustomerList_deptOutOfPermission_failure() {
        ErpCustomerImportExcelVO row = new ErpCustomerImportExcelVO();
        row.setName("超范围客户");
        row.setCode("LIMIT001");
        row.setDeptName("售后部");
        mockImportDeptContext(false, 10L, 11L, 12L);

        ErpCustomerImportRespVO result = customerService.importCustomerList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("部门超出当前用户可操作范围"));
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
    }

    @Test
    public void testImportCustomerList_emptyList_skip() {
        // null 列表：直接 return
        ErpCustomerImportRespVO nullResult = customerService.importCustomerList(null);
        assertEquals(0, nullResult.getSuccessCount());
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));

        // 空列表：直接 return
        ErpCustomerImportRespVO emptyResult = customerService.importCustomerList(Collections.emptyList());
        assertEquals(0, emptyResult.getSuccessCount());
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));
    }

    // ==================== batchUpdateCustomer ====================

    @Test
    public void testBatchUpdateCustomer_partialFields_doUpdate() {
        ErpCustomerBatchUpdateReqVO reqVO = new ErpCustomerBatchUpdateReqVO();
        reqVO.setIds(Arrays.asList(801L, 802L));
        reqVO.setSaleUserId(900L);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        reqVO.setPriceLevel(2);
        reqVO.setRemark("批量备注");
        when(customerMapper.selectById(eq(801L))).thenReturn(new ErpCustomerDO().setId(801L).setName("客户A"));
        when(customerMapper.selectById(eq(802L))).thenReturn(new ErpCustomerDO().setId(802L).setName("客户B"));

        // 执行
        customerService.batchUpdateCustomer(reqVO);

        // 断言：调用 update(null, wrapper)
        verify(customerMapper).update(eq(null),
                ArgumentMatchers.<LambdaUpdateWrapper<ErpCustomerDO>>any());
    }

    @Test
    public void testBatchUpdateCustomer_noField_skipUpdate() {
        // 只传 ids，没有任何要更新的字段
        ErpCustomerBatchUpdateReqVO reqVO = new ErpCustomerBatchUpdateReqVO();
        reqVO.setIds(Arrays.asList(810L, 811L));

        // 执行
        customerService.batchUpdateCustomer(reqVO);

        // 断言：不应调用 update
        verify(customerMapper, never()).update(any(),
                ArgumentMatchers.<LambdaUpdateWrapper<ErpCustomerDO>>any());
    }

    @Test
    public void testBatchUpdateCustomer_allOptionalFields_doUpdate() {
        // 覆盖所有可选字段分支
        ErpCustomerBatchUpdateReqVO reqVO = new ErpCustomerBatchUpdateReqVO();
        reqVO.setIds(Arrays.asList(820L));
        reqVO.setSaleUserId(901L);
        reqVO.setDeveloperUserId(902L);
        reqVO.setDeptId(903L);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setPriceLevel(3);
        reqVO.setRouteId(904L);
        reqVO.setFreightExplainId(905L);
        reqVO.setRemark("全量备注");

        customerService.batchUpdateCustomer(reqVO);

        verify(customerMapper).update(eq(null),
                ArgumentMatchers.<LambdaUpdateWrapper<ErpCustomerDO>>any());
    }

    private DeptRespDTO buildDept(Long id, String name) {
        return buildDept(id, name, 0L, CommonStatusEnum.ENABLE.getStatus());
    }

    private DeptRespDTO buildDept(Long id, String name, Long parentId, Integer status) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setStatus(status);
        return dept;
    }

    private void mockImportDeptContext(boolean all, Long... allowedDeptIds) {
        when(permissionApi.hasAnyPermissions(any(), eq("erp:customer:dept-distribute"))).thenReturn(true);
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(all);
        permission.setDeptIds(new java.util.LinkedHashSet<>(Arrays.asList(allowedDeptIds)));
        when(permissionApi.getDeptDataPermission(any(), eq("erp_customer"))).thenReturn(permission);
        when(deptApi.getDeptListByStatus(eq(null))).thenReturn(Arrays.asList(
                buildDept(10L, "销售部", 0L, CommonStatusEnum.ENABLE.getStatus()),
                buildDept(11L, "一组", 10L, CommonStatusEnum.ENABLE.getStatus()),
                buildDept(12L, "二组", 10L, CommonStatusEnum.ENABLE.getStatus()),
                buildDept(20L, "售后部", 0L, CommonStatusEnum.ENABLE.getStatus()),
                buildDept(30L, "停用部", 0L, CommonStatusEnum.DISABLE.getStatus())));
    }

}
