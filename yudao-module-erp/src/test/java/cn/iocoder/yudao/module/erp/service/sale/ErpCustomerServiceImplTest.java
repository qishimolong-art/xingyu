package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
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
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpReceivableAccountMapper receivableAccountMapper;

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
        customerService.importCustomerList(list);

        // 断言：合法 2 条被 insert，name 空和 null 跳过
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "导入客户A".equals(customer.getName())
                        && CommonStatusEnum.ENABLE.getStatus().equals(customer.getStatus())
                        && Integer.valueOf(0).equals(customer.getSort())));
        verify(customerMapper).insert(ArgumentMatchers.<ErpCustomerDO>argThat(customer ->
                "导入客户B".equals(customer.getName())
                        && CommonStatusEnum.DISABLE.getStatus().equals(customer.getStatus())
                        && Integer.valueOf(50).equals(customer.getSort())));
        // blankName 和 null 都被跳过：只 insert 2 次
        verify(customerMapper, org.mockito.Mockito.times(2)).insert(any(ErpCustomerDO.class));
    }

    @Test
    public void testImportCustomerList_emptyList_skip() {
        // null 列表：直接 return
        customerService.importCustomerList(null);
        verify(customerMapper, never()).insert(any(ErpCustomerDO.class));

        // 空列表：直接 return
        customerService.importCustomerList(Collections.emptyList());
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

}
