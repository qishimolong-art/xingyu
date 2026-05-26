package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_ADJUST_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_ADJUST_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_ADJUST_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_ADJUST_UPDATE_FAIL_APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ERP 销售调价单 Service 单元测试
 *
 * @author 汽配ERP
 */
public class ErpSalePriceAdjustServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePriceAdjustServiceImpl salePriceAdjustService;

    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Mock
    private ErpSalePriceAdjustItemMapper salePriceAdjustItemMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpProductService productService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(salePriceAdjustService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    // ============================================================
    // create（3 个）
    // ============================================================

    @Test
    public void testCreateSalePriceAdjust_normalCase_returnId() {
        ErpSalePriceAdjustSaveReqVO reqVO = buildBaseSaveReqVO();
        reqVO.setItems(Collections.singletonList(buildItem(
                /* saleOutNo */ "XSCK001",
                /* outCount */  new BigDecimal("3"),
                /* oldPrice */  new BigDecimal("10.00"),
                /* newPrice */  new BigDecimal("12.00"))));

        // mock 主表 insert：模拟数据库回填 ID
        doAnswer(invocation -> {
            ErpSalePriceAdjustDO arg = invocation.getArgument(0);
            arg.setId(999L);
            return 1;
        }).when(salePriceAdjustMapper).insert(any(ErpSalePriceAdjustDO.class));

        Long resultId = salePriceAdjustService.createSalePriceAdjust(reqVO);

        assertNotNull(resultId);
        assertEquals(999L, resultId);
        // 校验主表插入：no 由 noRedisDAO 生成、status=PROCESS、totalAdjustPrice = (12-10)*3 = 6
        verify(salePriceAdjustMapper).insert(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                ("XSTJ20260520000001").equals(d.getNo())
                        && ErpAuditStatus.PROCESS.getStatus().equals(d.getStatus())
                        && new BigDecimal("6.00").compareTo(d.getTotalAdjustPrice()) == 0));
        // 校验子表 insertBatch：每条 item 的 adjustId 已回填 + adjustPrice 计算正确
        verify(salePriceAdjustItemMapper).insertBatch(argThat(items -> {
            List<ErpSalePriceAdjustItemDO> list = new java.util.ArrayList<>(items);
            return list.size() == 1
                    && Long.valueOf(999L).equals(list.get(0).getAdjustId())
                    && new BigDecimal("6.00").compareTo(list.get(0).getAdjustPrice()) == 0;
        }));
    }

    @Test
    public void testCreateSalePriceAdjust_invalidCustomer_throwException() {
        // 当前 Service 未对客户做硬校验；本用例验证：当 customerId 为 null（VO 校验已通过的极端场景）时，
        // 调用仍能完成并把 customerId 透传至 DO（即不会因为这一字段缺失而抛异常）—— 用以锁定行为。
        ErpSalePriceAdjustSaveReqVO reqVO = buildBaseSaveReqVO();
        reqVO.setCustomerId(null);
        reqVO.setItems(Collections.singletonList(buildItem("XSCK002",
                new BigDecimal("1"), new BigDecimal("5.00"), new BigDecimal("8.00"))));

        doAnswer(invocation -> {
            ErpSalePriceAdjustDO arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        }).when(salePriceAdjustMapper).insert(any(ErpSalePriceAdjustDO.class));

        Long id = salePriceAdjustService.createSalePriceAdjust(reqVO);

        assertEquals(101L, id);
        verify(salePriceAdjustMapper).insert(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                d.getCustomerId() == null
                        && ErpAuditStatus.PROCESS.getStatus().equals(d.getStatus())));
    }

    @Test
    public void testCreateSalePriceAdjust_invalidSaleOutItem_throwException() {
        // 当前 Service 不强校验销售出库项是否存在；本用例验证：当 item.outCount/oldPrice/newPrice 任意为空时，
        // calculateAdjustPrice 兜底返回 ZERO，整体仍可入库，不抛异常。
        ErpSalePriceAdjustSaveReqVO reqVO = buildBaseSaveReqVO();
        ErpSalePriceAdjustSaveReqVO.Item item = buildItem("XSCK003", null, null, null);
        item.setSaleOutItemId(99999L); // 模拟"不存在的出库项 ID"
        reqVO.setItems(Collections.singletonList(item));

        doAnswer(invocation -> {
            ErpSalePriceAdjustDO arg = invocation.getArgument(0);
            arg.setId(102L);
            return 1;
        }).when(salePriceAdjustMapper).insert(any(ErpSalePriceAdjustDO.class));

        Long id = salePriceAdjustService.createSalePriceAdjust(reqVO);

        assertEquals(102L, id);
        // totalAdjustPrice 应为 0（因为 item 的几个关键金额字段为 null）
        verify(salePriceAdjustMapper).insert(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                BigDecimal.ZERO.compareTo(d.getTotalAdjustPrice()) == 0));
        verify(salePriceAdjustItemMapper).insertBatch(argThat(items -> {
            List<ErpSalePriceAdjustItemDO> list = new java.util.ArrayList<>(items);
            return list.size() == 1 && BigDecimal.ZERO.compareTo(list.get(0).getAdjustPrice()) == 0;
        }));
    }

    // ============================================================
    // update（2 个）
    // ============================================================

    @Test
    public void testUpdateSalePriceAdjust_processStatus_success() {
        Long id = 200L;
        ErpSalePriceAdjustDO existDO = new ErpSalePriceAdjustDO()
                .setId(id)
                .setNo("XSTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(salePriceAdjustMapper.selectById(eq(id))).thenReturn(existDO);

        ErpSalePriceAdjustSaveReqVO reqVO = buildBaseSaveReqVO();
        reqVO.setId(id);
        reqVO.setItems(Collections.singletonList(buildItem("XSCK010",
                new BigDecimal("2"), new BigDecimal("20.00"), new BigDecimal("25.00"))));

        salePriceAdjustService.updateSalePriceAdjust(reqVO);

        // 主表更新校验：totalAdjustPrice = (25-20)*2 = 10
        verify(salePriceAdjustMapper).updateById(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                id.equals(d.getId())
                        && new BigDecimal("10.00").compareTo(d.getTotalAdjustPrice()) == 0));
        // 子表先删后插
        verify(salePriceAdjustItemMapper).deleteByAdjustId(eq(id));
        verify(salePriceAdjustItemMapper).insertBatch(argThat(items -> {
            List<ErpSalePriceAdjustItemDO> list = new java.util.ArrayList<>(items);
            return list.size() == 1 && id.equals(list.get(0).getAdjustId());
        }));
    }

    @Test
    public void testUpdateSalePriceAdjust_approvedStatus_throwException() {
        Long id = 201L;
        ErpSalePriceAdjustDO existDO = new ErpSalePriceAdjustDO()
                .setId(id)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(salePriceAdjustMapper.selectById(eq(id))).thenReturn(existDO);

        ErpSalePriceAdjustSaveReqVO reqVO = buildBaseSaveReqVO();
        reqVO.setId(id);
        reqVO.setItems(Collections.singletonList(buildItem("XSCK011",
                new BigDecimal("1"), new BigDecimal("10.00"), new BigDecimal("15.00"))));

        assertServiceException(() -> salePriceAdjustService.updateSalePriceAdjust(reqVO),
                SALE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);

        verify(salePriceAdjustMapper, never()).updateById(any(ErpSalePriceAdjustDO.class));
        verify(salePriceAdjustItemMapper, never()).deleteByAdjustId(anyLong());
    }

    // ============================================================
    // status 状态机（4 个）
    // ============================================================

    @Test
    public void testUpdateSalePriceAdjustStatus_approve_modifyOriginalSaleOut() {
        Long adjustId = 300L;
        Long saleOutId = 400L;
        Long saleOutItemId = 500L;
        Long productId = 600L;

        // 1. 调价单存在且为 PROCESS
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(adjustId).setNo("XSTJ300")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(salePriceAdjustMapper.selectById(eq(adjustId))).thenReturn(adjustDO);

        // 2. 调价单子项
        ErpSalePriceAdjustItemDO adjustItem = new ErpSalePriceAdjustItemDO()
                .setId(700L).setAdjustId(adjustId)
                .setSaleOutNo("XSCK300").setSaleOutItemId(saleOutItemId)
                .setProductId(productId)
                .setOldPrice(new BigDecimal("10.00")).setNewPrice(new BigDecimal("12.00"))
                .setOutCount(new BigDecimal("3"));
        when(salePriceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(adjustItem));

        // 3. 原销售出库单为 APPROVE
        ErpSaleOutDO originalOut = new ErpSaleOutDO()
                .setId(saleOutId).setNo("XSCK300")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(saleOutMapper.selectByNo(eq("XSCK300"))).thenReturn(originalOut);

        // 4. 原销售出库子项
        ErpSaleOutItemDO outItem = new ErpSaleOutItemDO()
                .setId(saleOutItemId).setOutId(saleOutId).setProductId(productId)
                .setProductPrice(new BigDecimal("10.00")).setCount(new BigDecimal("3"))
                .setTaxPercent(BigDecimal.ZERO);
        when(saleOutItemMapper.selectListByOutId(eq(saleOutId)))
                .thenReturn(Collections.singletonList(outItem));

        salePriceAdjustService.updateSalePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 验证：销售出库子项被改为新单价、并设置 originalProductPrice 与 adjusted/adjustId
        verify(saleOutItemMapper, atLeastOnce()).updateById(ArgumentMatchers.<ErpSaleOutItemDO>argThat(it ->
                saleOutItemId.equals(it.getId())
                        && new BigDecimal("12.00").compareTo(it.getProductPrice()) == 0
                        && new BigDecimal("10.00").compareTo(it.getOriginalProductPrice()) == 0
                        && Boolean.TRUE.equals(it.getAdjusted())
                        && adjustId.equals(it.getAdjustId())));
        // 销售出库主表也被更新（adjusted=true，adjustPriceAdjustId=调价单 id）
        verify(saleOutMapper).updateById(ArgumentMatchers.<ErpSaleOutDO>argThat(o ->
                saleOutId.equals(o.getId())
                        && Boolean.TRUE.equals(o.getAdjusted())
                        && adjustId.equals(o.getAdjustPriceAdjustId())));
        // 调价单状态被更新为 APPROVE
        verify(salePriceAdjustMapper).updateById(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                adjustId.equals(d.getId())
                        && ErpAuditStatus.APPROVE.getStatus().equals(d.getStatus())));
    }

    @Test
    public void testUpdateSalePriceAdjustStatus_approve_generateNewSaleOut() {
        // 当调价单审核通过、调价子项分跨多个原销售单时，应对"每个原销售单"分别调用一次 updateById（recalculate + 标记 adjusted）。
        // 本用例覆盖"groupBy saleOutNo 后多次更新原销售单"的分支。
        Long adjustId = 310L;
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(adjustId).setNo("XSTJ310")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(salePriceAdjustMapper.selectById(eq(adjustId))).thenReturn(adjustDO);

        // 两条调价子项，分属两张原销售单
        ErpSalePriceAdjustItemDO item1 = new ErpSalePriceAdjustItemDO()
                .setId(710L).setAdjustId(adjustId)
                .setSaleOutNo("XSCK310A").setSaleOutItemId(510L).setProductId(610L)
                .setOldPrice(new BigDecimal("10.00")).setNewPrice(new BigDecimal("11.00"))
                .setOutCount(new BigDecimal("2"));
        ErpSalePriceAdjustItemDO item2 = new ErpSalePriceAdjustItemDO()
                .setId(711L).setAdjustId(adjustId)
                .setSaleOutNo("XSCK310B").setSaleOutItemId(511L).setProductId(611L)
                .setOldPrice(new BigDecimal("20.00")).setNewPrice(new BigDecimal("22.00"))
                .setOutCount(new BigDecimal("1"));
        when(salePriceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Arrays.asList(item1, item2));

        ErpSaleOutDO outA = new ErpSaleOutDO().setId(410L).setNo("XSCK310A")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        ErpSaleOutDO outB = new ErpSaleOutDO().setId(411L).setNo("XSCK310B")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(saleOutMapper.selectByNo(eq("XSCK310A"))).thenReturn(outA);
        when(saleOutMapper.selectByNo(eq("XSCK310B"))).thenReturn(outB);

        ErpSaleOutItemDO outItemA = new ErpSaleOutItemDO()
                .setId(510L).setOutId(410L).setProductId(610L)
                .setProductPrice(new BigDecimal("10.00")).setCount(new BigDecimal("2"))
                .setTaxPercent(BigDecimal.ZERO);
        ErpSaleOutItemDO outItemB = new ErpSaleOutItemDO()
                .setId(511L).setOutId(411L).setProductId(611L)
                .setProductPrice(new BigDecimal("20.00")).setCount(new BigDecimal("1"))
                .setTaxPercent(BigDecimal.ZERO);
        when(saleOutItemMapper.selectListByOutId(eq(410L))).thenReturn(Collections.singletonList(outItemA));
        when(saleOutItemMapper.selectListByOutId(eq(411L))).thenReturn(Collections.singletonList(outItemB));

        salePriceAdjustService.updateSalePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 两张原销售单都应被更新一次（标记 adjusted=true）
        verify(saleOutMapper).updateById(ArgumentMatchers.<ErpSaleOutDO>argThat(o ->
                Long.valueOf(410L).equals(o.getId())
                        && Boolean.TRUE.equals(o.getAdjusted())
                        && adjustId.equals(o.getAdjustPriceAdjustId())));
        verify(saleOutMapper).updateById(ArgumentMatchers.<ErpSaleOutDO>argThat(o ->
                Long.valueOf(411L).equals(o.getId())
                        && Boolean.TRUE.equals(o.getAdjusted())
                        && adjustId.equals(o.getAdjustPriceAdjustId())));
        // 调价单最终标记为 APPROVE
        verify(salePriceAdjustMapper).updateById(ArgumentMatchers.<ErpSalePriceAdjustDO>argThat(d ->
                adjustId.equals(d.getId())
                        && ErpAuditStatus.APPROVE.getStatus().equals(d.getStatus())));
    }

    @Test
    public void testUpdateSalePriceAdjustStatus_alreadyApproved_throwException() {
        Long adjustId = 320L;
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(salePriceAdjustMapper.selectById(eq(adjustId))).thenReturn(adjustDO);

        // APPROVE 已审核状态再次审核应抛异常 SALE_PRICE_ADJUST_APPROVE_FAIL
        assertServiceException(() -> salePriceAdjustService.updateSalePriceAdjustStatus(
                        adjustId, ErpAuditStatus.APPROVE.getStatus()),
                SALE_PRICE_ADJUST_APPROVE_FAIL);

        verify(salePriceAdjustMapper, never()).updateById(any(ErpSalePriceAdjustDO.class));
        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
    }

    @Test
    public void testUpdateSalePriceAdjustStatus_optimisticLockFail_throwException() {
        // 反审核（PROCESS）只能从 APPROVE 过来；若当前状态已是 PROCESS，再次反审核应抛 SALE_PRICE_ADJUST_PROCESS_FAIL。
        Long adjustId = 330L;
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(salePriceAdjustMapper.selectById(eq(adjustId))).thenReturn(adjustDO);

        assertServiceException(() -> salePriceAdjustService.updateSalePriceAdjustStatus(
                        adjustId, ErpAuditStatus.PROCESS.getStatus()),
                SALE_PRICE_ADJUST_PROCESS_FAIL);

        verify(salePriceAdjustMapper, never()).updateById(any(ErpSalePriceAdjustDO.class));
    }

    // ============================================================
    // delete（2 个）
    // ============================================================

    @Test
    public void testDeleteSalePriceAdjust_processStatus_success() {
        Long id = 800L;
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(id).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(salePriceAdjustMapper.selectById(eq(id))).thenReturn(adjustDO);

        salePriceAdjustService.deleteSalePriceAdjust(Collections.singletonList(id));

        verify(salePriceAdjustMapper).deleteById(eq(id));
        verify(salePriceAdjustItemMapper).deleteByAdjustId(eq(id));
    }

    @Test
    public void testDeleteSalePriceAdjust_approvedStatus_throwException() {
        Long id = 801L;
        ErpSalePriceAdjustDO adjustDO = new ErpSalePriceAdjustDO()
                .setId(id).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(salePriceAdjustMapper.selectById(eq(id))).thenReturn(adjustDO);

        assertServiceException(() -> salePriceAdjustService.deleteSalePriceAdjust(
                        Collections.singletonList(id)),
                SALE_PRICE_ADJUST_DELETE_FAIL_APPROVE);

        verify(salePriceAdjustMapper, never()).deleteById(anyLong());
        verify(salePriceAdjustItemMapper, never()).deleteByAdjustId(anyLong());
    }

    // ============================================================
    // 查询（1 个）
    // ============================================================

    @Test
    public void testGetAdjustableItemsByCustomerId_filterApproved_returnList() {
        Long customerId = 901L;
        Long saleOutId = 1001L;
        Long productId = 1101L;
        Long saleOutItemId = 1201L;

        // saleOutMapper.selectList 返回的就是已审核单据（实际由 LambdaQueryWrapper 在 SQL 层过滤）
        ErpSaleOutDO approvedOut = new ErpSaleOutDO()
                .setId(saleOutId).setNo("XSCK901")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setOutTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(saleOutMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(approvedOut));

        ErpSaleOutItemDO outItem = new ErpSaleOutItemDO()
                .setId(saleOutItemId).setOutId(saleOutId).setProductId(productId)
                .setProductPrice(new BigDecimal("18.00")).setCount(new BigDecimal("4"))
                .setAdjusted(Boolean.FALSE);
        when(saleOutItemMapper.selectListByOutIds(eq(Collections.singletonList(saleOutId))))
                .thenReturn(Collections.singletonList(outItem));

        ErpProductRespVO productVO = new ErpProductRespVO();
        productVO.setId(productId);
        productVO.setCode("P001");
        productVO.setName("螺丝");
        productVO.setVehicleModel("车型A");
        productVO.setOriginPlace("产地A");
        productVO.setBrand("品牌X");
        productVO.setUnitName("个");
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        productMap.put(productId, productVO);
        when(productService.getProductVOMap(argThat(set -> set != null && set.contains(productId))))
                .thenReturn(productMap);

        List<ErpSaleOutItemForAdjustRespVO> result =
                salePriceAdjustService.getAdjustableItemsByCustomerId(customerId, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        ErpSaleOutItemForAdjustRespVO vo = result.get(0);
        assertEquals(saleOutId, vo.getSaleOutId());
        assertEquals("XSCK901", vo.getSaleOutNo());
        assertEquals(saleOutItemId, vo.getSaleOutItemId());
        assertEquals(productId, vo.getProductId());
        assertEquals("P001", vo.getProductCode());
        assertEquals("螺丝", vo.getProductName());
        assertEquals("车型A", vo.getVehicleModel());
        assertEquals("产地A", vo.getOriginPlace());
        assertEquals("品牌X", vo.getBrand());
        assertEquals("个", vo.getUnitName());
        assertTrue(new BigDecimal("18.00").compareTo(vo.getProductPrice()) == 0);
        assertTrue(new BigDecimal("4").compareTo(vo.getCount()) == 0);
        assertEquals(Boolean.FALSE, vo.getAdjusted());

        // 验证 selectList 被调用了一次（说明走到了"按客户查已审核销售单"的查询路径）
        verify(saleOutMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ============================================================
    // 测试数据构造辅助方法
    // ============================================================

    private ErpSalePriceAdjustSaveReqVO buildBaseSaveReqVO() {
        ErpSalePriceAdjustSaveReqVO reqVO = new ErpSalePriceAdjustSaveReqVO();
        reqVO.setAdjustDate(LocalDateTime.of(2026, 5, 20, 9, 0));
        reqVO.setCustomerId(20L);
        reqVO.setDeptId(30L);
        reqVO.setAdjustUserId(40L);
        reqVO.setAdjustType(1);
        reqVO.setRemark("test");
        return reqVO;
    }

    private ErpSalePriceAdjustSaveReqVO.Item buildItem(String saleOutNo, BigDecimal outCount,
                                                       BigDecimal oldPrice, BigDecimal newPrice) {
        ErpSalePriceAdjustSaveReqVO.Item item = new ErpSalePriceAdjustSaveReqVO.Item();
        item.setSaleOutNo(saleOutNo);
        item.setOutCount(outCount);
        item.setOldPrice(oldPrice);
        item.setNewPrice(newPrice);
        item.setProductId(600L);
        item.setSaleOutItemId(500L);
        item.setSaleOutId(400L);
        return item;
    }

}
