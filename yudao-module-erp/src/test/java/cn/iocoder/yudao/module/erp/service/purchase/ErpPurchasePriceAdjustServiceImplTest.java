package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_ITEM_ADJUSTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_ITEM_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_NEW_PRICE_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_PRICE_ADJUST_UPDATE_FAIL_APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchasePriceAdjustServiceImpl} 的单元测试类
 */
public class ErpPurchasePriceAdjustServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchasePriceAdjustServiceImpl priceAdjustService;

    @Mock
    private ErpPurchasePriceAdjustMapper priceAdjustMapper;
    @Mock
    private ErpPurchasePriceAdjustItemMapper priceAdjustItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpStockService stockService;

    @BeforeEach
    public void setUp() {
        // 替换 Redis 序号生成器（不连接真实 Redis）
        ReflectionTestUtils.setField(priceAdjustService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    // ========== 构造工厂方法 ==========

    private ErpPurchasePriceAdjustSaveReqVO.Item buildVOItem(Long inId, Long inItemId, Long productId, BigDecimal newPrice) {
        ErpPurchasePriceAdjustSaveReqVO.Item item = new ErpPurchasePriceAdjustSaveReqVO.Item();
        item.setInId(inId);
        item.setInItemId(inItemId);
        item.setProductId(productId);
        item.setNewPrice(newPrice);
        item.setOldPrice(BigDecimal.ZERO);  // 服务端会用 inItem 中的 productPrice 覆盖
        item.setCount(BigDecimal.ZERO);     // 服务端会用 inItem 的 count 覆盖
        return item;
    }

    private ErpPurchasePriceAdjustSaveReqVO buildBaseReqVO(Integer adjustType, ErpPurchasePriceAdjustSaveReqVO.Item... items) {
        ErpPurchasePriceAdjustSaveReqVO vo = new ErpPurchasePriceAdjustSaveReqVO();
        vo.setAdjustTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        vo.setSupplierId(100L);
        vo.setAdjustType(adjustType);
        vo.setItems(items.length == 0 ? Collections.emptyList() : Arrays.asList(items));
        return vo;
    }

    private ErpPurchaseInItemDO buildInItem(Long id, Long inId, Long productId, Long warehouseId,
                                            BigDecimal price, BigDecimal count) {
        return new ErpPurchaseInItemDO()
                .setId(id).setInId(inId).setProductId(productId).setWarehouseId(warehouseId)
                .setProductPrice(price).setCount(count);
    }

    private ErpPurchasePriceAdjustItemDO buildAdjustItem(Long id, Long adjustId, Long inId, Long inItemId,
                                                        Long productId, Long warehouseId,
                                                        BigDecimal oldPrice, BigDecimal newPrice, BigDecimal count) {
        ErpPurchasePriceAdjustItemDO item = new ErpPurchasePriceAdjustItemDO();
        item.setId(id);
        item.setAdjustId(adjustId);
        item.setInId(inId);
        item.setInItemId(inItemId);
        item.setProductId(productId);
        item.setWarehouseId(warehouseId);
        item.setOldPrice(oldPrice);
        item.setNewPrice(newPrice);
        item.setCount(count);
        return item;
    }

    // ========== createPurchasePriceAdjust ==========

    @Test
    public void testCreatePurchasePriceAdjust_success() {
        // 准备
        Long inItemId = 500L;
        Long inId = 800L;
        Long productId = 200L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(inId, inItemId, productId, new BigDecimal("13.5"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, inId, productId, 7L,
                new BigDecimal("12.5"), new BigDecimal("100"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());
        when(priceAdjustMapper.selectByNo(any())).thenReturn(null);

        // 执行
        priceAdjustService.createPurchasePriceAdjust(reqVO);

        // 校验
        verify(supplierService).validateSupplier(eq(100L));
        ArgumentCaptor<ErpPurchasePriceAdjustDO> mainCaptor = ArgumentCaptor.forClass(ErpPurchasePriceAdjustDO.class);
        verify(priceAdjustMapper).insert(mainCaptor.capture());
        ErpPurchasePriceAdjustDO inserted = mainCaptor.getValue();
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals("CGTJ20260520000001", inserted.getNo());
        // 调价金额 = (13.5 - 12.5) * 100 = 100.00
        assertEquals(0, inserted.getTotalAdjustPrice().compareTo(new BigDecimal("100.00")));
        verify(priceAdjustItemMapper).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchasePriceAdjust_calculatesAdjustPrice() {
        // 验证 adjustPrice = (newPrice - oldPrice) * count，scale=2 HALF_UP
        Long inItemId = 500L;
        Long inId = 800L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(inId, inItemId, 200L, new BigDecimal("12.555"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_ITEM.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, inId, 200L, 7L,
                new BigDecimal("10.123"), new BigDecimal("3"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        priceAdjustService.createPurchasePriceAdjust(reqVO);

        ArgumentCaptor<List<ErpPurchasePriceAdjustItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceAdjustItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchasePriceAdjustItemDO> items = itemsCaptor.getValue();
        assertEquals(1, items.size());
        // (12.555 - 10.123) * 3 = 7.296 → scale=2 HALF_UP = 7.30
        assertEquals(0, items.get(0).getAdjustPrice().compareTo(new BigDecimal("7.30")));
        // oldPrice / count 用 inItem 的快照
        assertEquals(0, items.get(0).getOldPrice().compareTo(new BigDecimal("10.123")));
        assertEquals(0, items.get(0).getCount().compareTo(new BigDecimal("3")));
    }

    @Test
    public void testCreatePurchasePriceAdjust_itemsEmpty_throwException() {
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType());

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
        verify(priceAdjustMapper, never()).insert(any(ErpPurchasePriceAdjustDO.class));
    }

    @Test
    public void testCreatePurchasePriceAdjust_typeNull_throwException() {
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, 500L, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(null, voItem);

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_TYPE_INVALID);
        verify(priceAdjustMapper, never()).insert(any(ErpPurchasePriceAdjustDO.class));
    }

    @Test
    public void testCreatePurchasePriceAdjust_typeInvalid_throwException() {
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, 500L, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(99, voItem);

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_TYPE_INVALID);
    }

    @Test
    public void testCreatePurchasePriceAdjust_inItemNotExists_throwException() {
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(null);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
    }

    @Test
    public void testCreatePurchasePriceAdjust_inItemIdMismatch_throwException() {
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        // inItem.inId = 999L 与 voItem.inId=800L 不一致
        ErpPurchaseInItemDO mismatch = buildInItem(inItemId, 999L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(mismatch);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
    }

    @Test
    public void testCreatePurchasePriceAdjust_newPriceNegative_throwException() {
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("-1"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_NEW_PRICE_NEGATIVE);
    }

    @Test
    public void testCreatePurchasePriceAdjust_byItemAlreadyAdjusted_throwException() {
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_ITEM.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setAdjusted(true);  // 已被调过价
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        assertServiceException(() -> priceAdjustService.createPurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_ITEM_ADJUSTED);
    }

    @Test
    public void testCreatePurchasePriceAdjust_byInOrderSkipsAdjustedCheck() {
        // BY_IN_ORDER 模式下，inItem.adjusted=true 也允许（不抛异常）
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setAdjusted(true);
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        // 不抛异常
        priceAdjustService.createPurchasePriceAdjust(reqVO);
        verify(priceAdjustMapper).insert(any(ErpPurchasePriceAdjustDO.class));
    }

    @Test
    public void testCreatePurchasePriceAdjust_supplierIdNull_skipsValidation() {
        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);
        reqVO.setSupplierId(null);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        priceAdjustService.createPurchasePriceAdjust(reqVO);

        verify(supplierService, never()).validateSupplier(any());
    }

    @Test
    public void testCreatePurchasePriceAdjust_fillsRedundantProductFields() {
        // 验证产品冗余字段从 productMap 回填
        Long inItemId = 500L;
        Long productId = 200L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, productId, new BigDecimal("13"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, productId, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setWarehousePosition("A-01-02");
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(productId);
        product.setCode("P001");
        product.setName("螺丝");
        product.setUnitName("个");
        product.setVehicleModel("X5");
        product.setStandard("M8");
        product.setFeatureCode("FC-1");
        product.setOriginPlace("上海");
        product.setBrand("博世");
        product.setDrawingNo("DRW-1");
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        productMap.put(productId, product);
        when(productService.getProductVOMap(any())).thenReturn(productMap);

        priceAdjustService.createPurchasePriceAdjust(reqVO);

        ArgumentCaptor<List<ErpPurchasePriceAdjustItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceAdjustItemMapper).insertBatch(itemsCaptor.capture());
        ErpPurchasePriceAdjustItemDO insertedItem = itemsCaptor.getValue().get(0);
        assertEquals("P001", insertedItem.getProductCode());
        assertEquals("螺丝", insertedItem.getProductName());
        assertEquals("个", insertedItem.getProductUnitName());
        assertEquals("X5", insertedItem.getVehicleModel());
        assertEquals("M8", insertedItem.getStandard());
        assertEquals("FC-1", insertedItem.getFeatureCode());
        assertEquals("上海", insertedItem.getOriginPlace());
        assertEquals("博世", insertedItem.getBrand());
        assertEquals("DRW-1", insertedItem.getDrawingNo());
        assertEquals("A-01-02", insertedItem.getWarehousePosition());
        // 仓库 ID 来自 inItem
        assertEquals(Long.valueOf(7L), insertedItem.getWarehouseId());
    }

    // ========== updatePurchasePriceAdjust ==========

    @Test
    public void testUpdatePurchasePriceAdjust_success() {
        // 准备：现有未审批的调价单
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJOLD001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("15"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);
        reqVO.setId(adjustId);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        // 执行
        priceAdjustService.updatePurchasePriceAdjust(reqVO);

        // 校验：rebuild 策略
        ArgumentCaptor<ErpPurchasePriceAdjustDO> mainCaptor = ArgumentCaptor.forClass(ErpPurchasePriceAdjustDO.class);
        verify(priceAdjustMapper).updateById(mainCaptor.capture());
        ErpPurchasePriceAdjustDO updated = mainCaptor.getValue();
        // 单号保持不变
        assertEquals("CGTJOLD001", updated.getNo());
        // 状态保持 PROCESS
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), updated.getStatus());
        // 子表：先删除再插入
        verify(priceAdjustItemMapper).deleteByAdjustId(eq(adjustId));
        verify(priceAdjustItemMapper).insertBatch(anyList());
    }

    @Test
    public void testUpdatePurchasePriceAdjust_notExists_throwException() {
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType());
        reqVO.setId(10L);

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_NOT_EXISTS);
        verify(priceAdjustMapper, never()).updateById(any(ErpPurchasePriceAdjustDO.class));
    }

    @Test
    public void testUpdatePurchasePriceAdjust_alreadyApproved_throwException() {
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(existing);

        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType());
        reqVO.setId(10L);

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjust(reqVO),
                PURCHASE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);
        verify(priceAdjustMapper, never()).updateById(any(ErpPurchasePriceAdjustDO.class));
    }

    @Test
    public void testUpdatePurchasePriceAdjust_rebuildStrategy() {
        // 验证子表先 deleteByAdjustId 再 insertBatch
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        Long inItemId = 500L;
        ErpPurchasePriceAdjustSaveReqVO.Item voItem = buildVOItem(800L, inItemId, 200L, new BigDecimal("15"));
        ErpPurchasePriceAdjustSaveReqVO reqVO = buildBaseReqVO(
                ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType(), voItem);
        reqVO.setId(adjustId);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, 800L, 200L, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);
        when(productService.getProductVOMap(any())).thenReturn(new HashMap<>());

        priceAdjustService.updatePurchasePriceAdjust(reqVO);

        verify(priceAdjustItemMapper).deleteByAdjustId(eq(adjustId));
        ArgumentCaptor<List<ErpPurchasePriceAdjustItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceAdjustItemMapper).insertBatch(itemsCaptor.capture());
        // 重建后的 items：id=null, adjustId=adjustId
        for (ErpPurchasePriceAdjustItemDO item : itemsCaptor.getValue()) {
            assertEquals(null, item.getId());
            assertEquals(adjustId, item.getAdjustId());
        }
    }

    // ========== updatePurchasePriceAdjustStatus（核心算法 - 审批） ==========

    @Test
    public void testUpdatePurchasePriceAdjustStatus_processNotSupported_throwException() {
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjustStatus(
                        10L, ErpAuditStatus.PROCESS.getStatus()),
                PURCHASE_PRICE_ADJUST_PROCESS_FAIL);
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjustStatus(
                        10L, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_PRICE_ADJUST_APPROVE_FAIL);
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_optimisticLockFails_throwException() {
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        // 子项至少要有 1 项才会走到 updateByIdAndStatus
        ErpPurchasePriceAdjustItemDO subItem = buildAdjustItem(1L, adjustId, 800L, 500L,
                200L, 7L, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(subItem));
        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchasePriceAdjustDO.class)))
                .thenReturn(0);  // 乐观锁失败

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjustStatus(
                        adjustId, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_PRICE_ADJUST_APPROVE_FAIL);
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveItemsEmpty_throwException() {
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.emptyList());

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjustStatus(
                        adjustId, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveInItemNotExists_throwException() {
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        ErpPurchasePriceAdjustItemDO subItem = buildAdjustItem(1L, adjustId, 800L, 500L,
                200L, 7L, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(subItem));
        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchasePriceAdjustDO.class)))
                .thenReturn(1);
        // inItem 已被删除
        when(purchaseInItemMapper.selectById(eq(500L))).thenReturn(null);

        assertServiceException(() -> priceAdjustService.updatePurchasePriceAdjustStatus(
                        adjustId, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveSuccessFullFlow() {
        // 完整 6 步审批流程
        Long adjustId = 10L;
        Long inItemId = 500L;
        Long inId = 800L;
        Long productId = 200L;
        Long warehouseId = 7L;
        LocalDateTime adjustTime = LocalDateTime.of(2026, 5, 20, 10, 0);
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(adjustTime);
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        // 单条调价项：oldPrice=10, newPrice=12, count=5, delta=10
        ErpPurchasePriceAdjustItemDO subItem = buildAdjustItem(1L, adjustId, inId, inItemId,
                productId, warehouseId, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(subItem));

        // 乐观锁成功
        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchasePriceAdjustDO.class)))
                .thenReturn(1);

        // 入库项 - originalProductPrice 为 null（首次调价）
        ErpPurchaseInItemDO inItem = buildInItem(inItemId, inId, productId, warehouseId,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setTaxPercent(new BigDecimal("13"));  // 13%
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);

        // 入库主表
        ErpPurchaseInDO inDO = new ErpPurchaseInDO()
                .setId(inId).setNo("CGRK001").setInTime(LocalDateTime.of(2026, 5, 18, 10, 0))
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(inId))).thenReturn(inDO);

        // recalc 入库主表所需子项
        ErpPurchaseInItemDO inItemAfterUpdate = buildInItem(inItemId, inId, productId, warehouseId,
                new BigDecimal("12"), new BigDecimal("5"));
        inItemAfterUpdate.setTotalPrice(new BigDecimal("60"));
        inItemAfterUpdate.setTaxPrice(new BigDecimal("7.80"));
        when(purchaseInItemMapper.selectListByInId(eq(inId)))
                .thenReturn(Collections.singletonList(inItemAfterUpdate));

        // 执行
        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 1. 状态翻转
        ArgumentCaptor<ErpPurchasePriceAdjustDO> statusCaptor = ArgumentCaptor.forClass(ErpPurchasePriceAdjustDO.class);
        verify(priceAdjustMapper).updateByIdAndStatus(eq(adjustId),
                eq(ErpAuditStatus.PROCESS.getStatus()), statusCaptor.capture());
        ErpPurchasePriceAdjustDO statusUpdate = statusCaptor.getValue();
        assertEquals(ErpAuditStatus.APPROVE.getStatus(), statusUpdate.getStatus());
        assertNotNull(statusUpdate.getApproveTime());

        // 2. inItem 字段更新：productPrice=12, totalPrice=12*5=60, taxPrice=60*13/100=7.80, originalProductPrice=10, adjusted=true
        ArgumentCaptor<ErpPurchaseInItemDO> inItemCaptor = ArgumentCaptor.forClass(ErpPurchaseInItemDO.class);
        verify(purchaseInItemMapper).updateById(inItemCaptor.capture());
        ErpPurchaseInItemDO inItemUpdate = inItemCaptor.getValue();
        assertEquals(0, inItemUpdate.getProductPrice().compareTo(new BigDecimal("12")));
        assertEquals(0, inItemUpdate.getTotalPrice().compareTo(new BigDecimal("60")));
        assertEquals(0, inItemUpdate.getTaxPrice().compareTo(new BigDecimal("7.80")));
        // originalProductPrice = oldPrice = 10（首次写入）
        assertEquals(0, inItemUpdate.getOriginalProductPrice().compareTo(new BigDecimal("10")));
        assertTrue(Boolean.TRUE.equals(inItemUpdate.getAdjusted()));
        assertEquals(adjustId, inItemUpdate.getAdjustId());

        // 3. 入库主表 recalc
        ArgumentCaptor<ErpPurchaseInDO> inCaptor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(inCaptor.capture());
        ErpPurchaseInDO recalcedIn = inCaptor.getValue();
        assertTrue(Boolean.TRUE.equals(recalcedIn.getAdjusted()));

        // 4. adjustStockCostAmount 调用
        verify(stockService).adjustStockCostAmount(eq(productId), eq(warehouseId),
                any(BigDecimal.class), any(BigDecimal.class),
                eq(adjustId), eq("CGTJ001"), eq(adjustTime));

        // 5. 回写 lastPurchasePrice
        verify(productService).updateProductLastPurchasePrice(eq(productId), eq(new BigDecimal("12")));
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveSkipsOriginalPriceWhenAlreadySet() {
        // inItem.originalProductPrice 已存在时不再覆盖
        Long adjustId = 10L;
        Long inItemId = 500L;
        Long inId = 800L;
        Long productId = 200L;
        Long warehouseId = 7L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        ErpPurchasePriceAdjustItemDO subItem = buildAdjustItem(1L, adjustId, inId, inItemId,
                productId, warehouseId, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(subItem));

        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId), any(), any())).thenReturn(1);

        // inItem.originalProductPrice 已经存在（说明已被调过价一次）
        ErpPurchaseInItemDO inItem = buildInItem(inItemId, inId, productId, warehouseId,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setOriginalProductPrice(new BigDecimal("8"));  // 之前的初始进价
        inItem.setTaxPercent(BigDecimal.ZERO);
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);

        ErpPurchaseInDO inDO = new ErpPurchaseInDO().setId(inId).setNo("CGRK001")
                .setInTime(LocalDateTime.now())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(inId))).thenReturn(inDO);
        when(purchaseInItemMapper.selectListByInId(eq(inId))).thenReturn(Collections.emptyList());

        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        ArgumentCaptor<ErpPurchaseInItemDO> inItemCaptor = ArgumentCaptor.forClass(ErpPurchaseInItemDO.class);
        verify(purchaseInItemMapper).updateById(inItemCaptor.capture());
        // 不再覆盖 originalProductPrice，更新对象上不应携带该字段
        assertEquals(null, inItemCaptor.getValue().getOriginalProductPrice());
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveAggregatesDeltaPerProductWarehouse() {
        // 同 (productId, warehouseId) 多行差额聚合，调一次 adjustStockCostAmount
        Long adjustId = 10L;
        Long productId = 200L;
        Long warehouseId = 7L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        // 两行：同产品同仓库
        ErpPurchasePriceAdjustItemDO sub1 = buildAdjustItem(1L, adjustId, 800L, 501L,
                productId, warehouseId, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        ErpPurchasePriceAdjustItemDO sub2 = buildAdjustItem(2L, adjustId, 801L, 502L,
                productId, warehouseId, new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("3"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Arrays.asList(sub1, sub2));

        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId), any(), any())).thenReturn(1);

        ErpPurchaseInItemDO inItem1 = buildInItem(501L, 800L, productId, warehouseId,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem1.setTaxPercent(BigDecimal.ZERO);
        ErpPurchaseInItemDO inItem2 = buildInItem(502L, 801L, productId, warehouseId,
                new BigDecimal("10"), new BigDecimal("3"));
        inItem2.setTaxPercent(BigDecimal.ZERO);
        when(purchaseInItemMapper.selectById(eq(501L))).thenReturn(inItem1);
        when(purchaseInItemMapper.selectById(eq(502L))).thenReturn(inItem2);

        ErpPurchaseInDO inDO1 = new ErpPurchaseInDO().setId(800L).setNo("CGRK001")
                .setInTime(LocalDateTime.of(2026, 5, 18, 10, 0))
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        ErpPurchaseInDO inDO2 = new ErpPurchaseInDO().setId(801L).setNo("CGRK002")
                .setInTime(LocalDateTime.of(2026, 5, 19, 10, 0))
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(800L))).thenReturn(inDO1);
        when(purchaseInMapper.selectById(eq(801L))).thenReturn(inDO2);
        when(purchaseInItemMapper.selectListByInId(any())).thenReturn(Collections.emptyList());

        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 聚合: deltaFull = (12-10)*5 + (13-10)*3 = 10 + 9 = 19；sumInCount = 5 + 3 = 8
        ArgumentCaptor<BigDecimal> deltaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> sumCountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(stockService, times(1)).adjustStockCostAmount(eq(productId), eq(warehouseId),
                deltaCaptor.capture(), sumCountCaptor.capture(),
                eq(adjustId), any(), any());
        assertEquals(0, deltaCaptor.getValue().compareTo(new BigDecimal("19")));
        assertEquals(0, sumCountCaptor.getValue().compareTo(new BigDecimal("8")));
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_approveSkipsZeroDeltaStockAdjust() {
        // newPrice == oldPrice 时差额为 0，跳过 adjustStockCostAmount
        Long adjustId = 10L;
        Long inItemId = 500L;
        Long inId = 800L;
        Long productId = 200L;
        Long warehouseId = 7L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        // newPrice = oldPrice = 10
        ErpPurchasePriceAdjustItemDO subItem = buildAdjustItem(1L, adjustId, inId, inItemId,
                productId, warehouseId, new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("5"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Collections.singletonList(subItem));

        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId), any(), any())).thenReturn(1);

        ErpPurchaseInItemDO inItem = buildInItem(inItemId, inId, productId, warehouseId,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem.setTaxPercent(BigDecimal.ZERO);
        when(purchaseInItemMapper.selectById(eq(inItemId))).thenReturn(inItem);

        ErpPurchaseInDO inDO = new ErpPurchaseInDO().setId(inId).setNo("CGRK001")
                .setInTime(LocalDateTime.now())
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(inId))).thenReturn(inDO);
        when(purchaseInItemMapper.selectListByInId(eq(inId))).thenReturn(Collections.emptyList());

        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 差额为 0，不调用 adjustStockCostAmount
        verify(stockService, never()).adjustStockCostAmount(any(), any(), any(), any(), any(), any(), any());
        // lastPurchasePrice 仍然回写
        verify(productService).updateProductLastPurchasePrice(eq(productId), eq(new BigDecimal("10")));
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_lastPurchasePriceUsesLatestInTime() {
        // 同 productId 多行 inTime 不同时，回写 inTime 最晚的 newPrice
        Long adjustId = 10L;
        Long productId = 200L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setNo("CGTJ001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setAdjustTime(LocalDateTime.now());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        // 两个不同 inId（不同 inTime），都是同 productId
        ErpPurchasePriceAdjustItemDO subEarly = buildAdjustItem(1L, adjustId, 800L, 501L,
                productId, 7L, new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        ErpPurchasePriceAdjustItemDO subLate = buildAdjustItem(2L, adjustId, 801L, 502L,
                productId, 7L, new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("3"));
        when(priceAdjustItemMapper.selectListByAdjustId(eq(adjustId)))
                .thenReturn(Arrays.asList(subEarly, subLate));

        when(priceAdjustMapper.updateByIdAndStatus(eq(adjustId), any(), any())).thenReturn(1);

        ErpPurchaseInItemDO inItem1 = buildInItem(501L, 800L, productId, 7L,
                new BigDecimal("10"), new BigDecimal("5"));
        inItem1.setTaxPercent(BigDecimal.ZERO);
        ErpPurchaseInItemDO inItem2 = buildInItem(502L, 801L, productId, 7L,
                new BigDecimal("10"), new BigDecimal("3"));
        inItem2.setTaxPercent(BigDecimal.ZERO);
        when(purchaseInItemMapper.selectById(eq(501L))).thenReturn(inItem1);
        when(purchaseInItemMapper.selectById(eq(502L))).thenReturn(inItem2);

        // inDO1 inTime 较早；inDO2 inTime 较晚
        ErpPurchaseInDO inDOEarly = new ErpPurchaseInDO().setId(800L).setNo("CGRK001")
                .setInTime(LocalDateTime.of(2026, 5, 18, 10, 0))
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        ErpPurchaseInDO inDOLate = new ErpPurchaseInDO().setId(801L).setNo("CGRK002")
                .setInTime(LocalDateTime.of(2026, 5, 19, 10, 0))
                .setDiscountPercent(BigDecimal.ZERO).setOtherPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(800L))).thenReturn(inDOEarly);
        when(purchaseInMapper.selectById(eq(801L))).thenReturn(inDOLate);
        when(purchaseInItemMapper.selectListByInId(any())).thenReturn(Collections.emptyList());

        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, ErpAuditStatus.APPROVE.getStatus());

        // 取 inTime 最晚的 newPrice = 15
        verify(productService, times(1)).updateProductLastPurchasePrice(eq(productId),
                eq(new BigDecimal("15")));
    }

    @Test
    public void testUpdatePurchasePriceAdjustStatus_otherStatus_directUpdate() {
        // 给非 APPROVE/PROCESS 的状态时，直接 updateById
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        priceAdjustService.updatePurchasePriceAdjustStatus(adjustId, 30);

        ArgumentCaptor<ErpPurchasePriceAdjustDO> captor = ArgumentCaptor.forClass(ErpPurchasePriceAdjustDO.class);
        verify(priceAdjustMapper).updateById(captor.capture());
        ErpPurchasePriceAdjustDO updated = captor.getValue();
        assertEquals(adjustId, updated.getId());
        assertEquals(Integer.valueOf(30), updated.getStatus());
        // 不走审批流程
        verify(priceAdjustItemMapper, never()).selectListByAdjustId(any());
        verify(stockService, never()).adjustStockCostAmount(any(), any(), any(), any(), any(), any(), any());
    }

    // ========== deletePurchasePriceAdjust ==========

    @Test
    public void testDeletePurchasePriceAdjust_success() {
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO existing = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(existing);

        priceAdjustService.deletePurchasePriceAdjust(Collections.singletonList(adjustId));

        verify(priceAdjustMapper).deleteById(eq(adjustId));
        verify(priceAdjustItemMapper).deleteByAdjustId(eq(adjustId));
    }

    @Test
    public void testDeletePurchasePriceAdjust_alreadyApproved_throwException() {
        Long adjustId = 10L;
        ErpPurchasePriceAdjustDO approved = new ErpPurchasePriceAdjustDO()
                .setId(adjustId).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(priceAdjustMapper.selectById(eq(adjustId))).thenReturn(approved);

        assertServiceException(() -> priceAdjustService.deletePurchasePriceAdjust(
                        Collections.singletonList(adjustId)),
                PURCHASE_PRICE_ADJUST_DELETE_FAIL_APPROVE);
        verify(priceAdjustMapper, never()).deleteById(any(Long.class));
    }

    @Test
    public void testDeletePurchasePriceAdjust_notExists_throwException() {
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(null);

        assertServiceException(() -> priceAdjustService.deletePurchasePriceAdjust(
                        Collections.singletonList(10L)),
                PURCHASE_PRICE_ADJUST_NOT_EXISTS);
        verify(priceAdjustMapper, never()).deleteById(any(Long.class));
    }

    @Test
    public void testDeletePurchasePriceAdjust_emptyList_noOp() {
        priceAdjustService.deletePurchasePriceAdjust(Collections.emptyList());
        priceAdjustService.deletePurchasePriceAdjust(null);

        verify(priceAdjustMapper, never()).selectById(any());
        verify(priceAdjustMapper, never()).deleteById(any(Long.class));
        verify(priceAdjustItemMapper, never()).deleteByAdjustId(any());
    }

    // ========== get / page / item list 方法 ==========

    @Test
    public void testGetPurchasePriceAdjust() {
        ErpPurchasePriceAdjustDO adjustDO = new ErpPurchasePriceAdjustDO().setId(10L);
        when(priceAdjustMapper.selectById(eq(10L))).thenReturn(adjustDO);

        assertSame(adjustDO, priceAdjustService.getPurchasePriceAdjust(10L));
    }

    @Test
    public void testGetPurchasePriceAdjustPage() {
        ErpPurchasePriceAdjustPageReqVO reqVO = new ErpPurchasePriceAdjustPageReqVO();
        PageResult<ErpPurchasePriceAdjustDO> page = new PageResult<>(
                Collections.singletonList(new ErpPurchasePriceAdjustDO().setId(1L)), 1L);
        when(priceAdjustMapper.selectPage(eq(reqVO))).thenReturn(page);

        assertSame(page, priceAdjustService.getPurchasePriceAdjustPage(reqVO));
    }

    @Test
    public void testGetPurchasePriceAdjustItemListByAdjustId() {
        List<ErpPurchasePriceAdjustItemDO> items = Collections.singletonList(
                new ErpPurchasePriceAdjustItemDO());
        when(priceAdjustItemMapper.selectListByAdjustId(eq(10L))).thenReturn(items);

        assertSame(items, priceAdjustService.getPurchasePriceAdjustItemListByAdjustId(10L));
    }

    @Test
    public void testGetPurchasePriceAdjustItemListByAdjustIds_emptyInput_returnsEmpty() {
        List<ErpPurchasePriceAdjustItemDO> resultEmpty = priceAdjustService
                .getPurchasePriceAdjustItemListByAdjustIds(Collections.emptyList());
        assertTrue(resultEmpty.isEmpty());

        List<ErpPurchasePriceAdjustItemDO> resultNull = priceAdjustService
                .getPurchasePriceAdjustItemListByAdjustIds(null);
        assertTrue(resultNull.isEmpty());

        verify(priceAdjustItemMapper, never()).selectListByAdjustIds(any());
    }

    @Test
    public void testGetPurchasePriceAdjustItemListByAdjustIds_success() {
        Collection<Long> ids = Arrays.asList(1L, 2L);
        List<ErpPurchasePriceAdjustItemDO> items = Arrays.asList(
                new ErpPurchasePriceAdjustItemDO(),
                new ErpPurchasePriceAdjustItemDO());
        when(priceAdjustItemMapper.selectListByAdjustIds(eq(ids))).thenReturn(items);

        assertSame(items, priceAdjustService.getPurchasePriceAdjustItemListByAdjustIds(ids));
    }

}
