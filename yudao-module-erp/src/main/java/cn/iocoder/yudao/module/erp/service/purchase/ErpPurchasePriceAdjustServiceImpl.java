package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.price.ErpPriceHistoryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PURCHASE_PRICE_ADJUST_TYPE;

/**
 * ERP 采购调价单 Service 实现类
 *
 * <p>审批算法：原单原地改 + 成本差额补账</p>
 * <ol>
 *   <li>审批通过后更新原入库项的 productPrice / totalPrice / taxPrice / adjusted / adjustId；首次调价时写 originalProductPrice</li>
 *   <li>按入库单维度重算 erp_purchase_in 的 totalProductPrice / totalTaxPrice / discountPrice / totalPrice，并标记 adjusted=true</li>
 *   <li>按 (productId, warehouseId) 聚合调价差额，调用 {@link ErpStockService#adjustStockCostAmount} 按比例摊分到当前在库的 cost_amount</li>
 *   <li>回写产品的 lastPurchasePrice（取同一产品中 inTime 最晚的 newPrice）</li>
 * </ol>
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpPurchasePriceAdjustServiceImpl implements ErpPurchasePriceAdjustService {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_price_adjust";

    @Resource
    private ErpPurchasePriceAdjustMapper priceAdjustMapper;
    @Resource
    private ErpPurchasePriceAdjustItemMapper priceAdjustItemMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpPriceHistoryService priceHistoryService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;

    @Resource
    @Lazy
    private ErpStockService stockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO reqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, reqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, reqVO.getItems());
        // 1. 主表 + 子表校验 + 回填
        validateMainForm(reqVO);
        List<ErpPurchasePriceAdjustItemDO> items = buildAndValidateItems(reqVO, null);

        // 2. 计算调价总金额
        BigDecimal totalAdjustPrice = sumAdjustPrice(items);

        // 3. 生成单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_PRICE_ADJUST_NO_PREFIX);
        if (priceAdjustMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_PRICE_ADJUST_NO_EXISTS);
        }

        // 4. 插入主表（PROCESS 状态）
        ErpPurchasePriceAdjustDO adjustDO = BeanUtils.toBean(reqVO, ErpPurchasePriceAdjustDO.class);
        adjustDO.setId(null);
        adjustDO.setNo(no);
        adjustDO.setStatus(ErpAuditStatus.PROCESS.getStatus());
        adjustDO.setAdjustTime(reqVO.getAdjustTime() != null ? reqVO.getAdjustTime() : LocalDateTime.now());
        adjustDO.setTotalAdjustPrice(totalAdjustPrice);
        fillDeptIdFromPurchaseIn(adjustDO, items);
        purchaseDocumentDefaultService.fillCreateDefaults(adjustDO);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(adjustDO);
        priceAdjustMapper.insert(adjustDO);

        // 5. 插入子表
        items.forEach(item -> item.setAdjustId(adjustDO.getId()));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        priceAdjustItemMapper.insertBatch(items);
        operateLogService.recordCreate(ERP_PURCHASE_PRICE_ADJUST_TYPE, adjustDO.getId(), adjustDO.getNo());

        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchasePriceAdjustDraft(ErpPurchasePriceAdjustDraftSaveReqVO reqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, reqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, reqVO.getItems());
        List<ErpPurchasePriceAdjustItemDO> items = buildDraftItems(reqVO.getAdjustType(), reqVO.getItems());
        if (CollUtil.isEmpty(items)) {
            throw exception(PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
        }
        ErpPurchasePriceAdjustDO adjustDO = BeanUtils.toBean(reqVO, ErpPurchasePriceAdjustDO.class);
        adjustDO.setId(null);
        adjustDO.setNo(noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_PRICE_ADJUST_NO_PREFIX));
        adjustDO.setStatus(ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus());
        adjustDO.setAdjustTime(reqVO.getAdjustTime() != null ? reqVO.getAdjustTime() : LocalDateTime.now());
        adjustDO.setTotalAdjustPrice(sumAdjustPrice(items));
        fillDeptIdFromPurchaseIn(adjustDO, items);
        purchaseDocumentDefaultService.fillCreateDefaults(adjustDO);
        priceAdjustMapper.insert(adjustDO);
        insertDraftItems(adjustDO.getId(), items);
        operateLogService.recordCreate(ERP_PURCHASE_PRICE_ADJUST_TYPE, adjustDO.getId(), adjustDO.getNo());
        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitPurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO reqVO) {
        return createPurchasePriceAdjust(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO reqVO) {
        // 1. 校验存在 + 未审批
        ErpPurchasePriceAdjustDO existDO = validateExists(reqVO.getId());
        if (ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus().equals(existDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_DRAFT_UPDATE_FAIL, existDO.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);
        }

        // 2. 主表 + 子表校验 + 回填（排除自己已有的 item ID，以便防重复规则正确）
        validateMainForm(reqVO);
        List<ErpPurchasePriceAdjustItemDO> items = buildAndValidateItems(reqVO, reqVO.getId());

        // 3. 计算调价总金额
        BigDecimal totalAdjustPrice = sumAdjustPrice(items);

        // 4. 更新主表
        ErpPurchasePriceAdjustDO updateDO = BeanUtils.toBean(reqVO, ErpPurchasePriceAdjustDO.class);
        updateDO.setNo(existDO.getNo()); // 保持单号不变
        updateDO.setStatus(existDO.getStatus());
        updateDO.setAdjustTime(reqVO.getAdjustTime() != null ? reqVO.getAdjustTime() : LocalDateTime.now());
        updateDO.setTotalAdjustPrice(totalAdjustPrice);
        fillDeptIdFromPurchaseIn(updateDO, items);
        if (updateDO.getAdjuster() == null) {
            updateDO.setAdjuster(existDO.getAdjuster());
        }
        if (updateDO.getDeptId() == null) {
            updateDO.setDeptId(existDO.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(updateDO);
        priceAdjustMapper.updateById(updateDO);

        // 5. 重建子表
        priceAdjustItemMapper.deleteByAdjustId(reqVO.getId());
        items.forEach(item -> {
            item.setId(null);
            item.setAdjustId(reqVO.getId());
        });
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        priceAdjustItemMapper.insertBatch(items);
        operateLogService.recordUpdate(ERP_PURCHASE_PRICE_ADJUST_TYPE, reqVO.getId(), existDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasePriceAdjustDraft(ErpPurchasePriceAdjustDraftSaveReqVO reqVO) {
        ErpPurchasePriceAdjustDO existDO = validateExists(reqVO.getId());
        if (!ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus().equals(existDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_DRAFT_UPDATE_FAIL, existDO.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, reqVO, existDO);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, reqVO.getItems(),
                priceAdjustItemMapper.selectListByAdjustId(reqVO.getId()));
        List<ErpPurchasePriceAdjustItemDO> items = buildDraftItems(reqVO.getAdjustType(), reqVO.getItems());
        ErpPurchasePriceAdjustDO updateDO = BeanUtils.toBean(reqVO, ErpPurchasePriceAdjustDO.class);
        updateDO.setId(existDO.getId());
        updateDO.setNo(existDO.getNo());
        updateDO.setStatus(existDO.getStatus());
        if (updateDO.getAdjustTime() == null) {
            updateDO.setAdjustTime(existDO.getAdjustTime());
        }
        updateDO.setTotalAdjustPrice(sumAdjustPrice(items));
        if (updateDO.getDeptId() == null) {
            updateDO.setDeptId(existDO.getDeptId());
        }
        if (updateDO.getAdjuster() == null) {
            updateDO.setAdjuster(existDO.getAdjuster());
        }
        if (priceAdjustMapper.updateByIdAndStatus(existDO.getId(),
                ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus(), updateDO) == 0) {
            throw exception(PURCHASE_PRICE_ADJUST_DRAFT_UPDATE_FAIL, existDO.getNo());
        }
        priceAdjustItemMapper.deleteByAdjustId(existDO.getId());
        insertDraftItems(existDO.getId(), items);
        operateLogService.recordUpdate(ERP_PURCHASE_PRICE_ADJUST_TYPE, existDO.getId(), existDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO reqVO) {
        ErpPurchasePriceAdjustDraftSaveReqVO draftReqVO =
                BeanUtils.toBean(reqVO, ErpPurchasePriceAdjustDraftSaveReqVO.class);
        updatePurchasePriceAdjustDraft(draftReqVO);
        submitPurchasePriceAdjust(reqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchasePriceAdjust(Long id) {
        ErpPurchasePriceAdjustDO adjustDO = validateExists(id);
        if (!ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus().equals(adjustDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL);
        }
        ErpPurchasePriceAdjustSaveReqVO reqVO = BeanUtils.toBean(adjustDO, ErpPurchasePriceAdjustSaveReqVO.class);
        reqVO.setItems(BeanUtils.toBean(priceAdjustItemMapper.selectListByAdjustId(id),
                ErpPurchasePriceAdjustSaveReqVO.Item.class));
        validateMainForm(reqVO);
        List<ErpPurchasePriceAdjustItemDO> items = buildAndValidateItems(reqVO, id);
        BigDecimal totalAdjustPrice = sumAdjustPrice(items);
        ErpPurchasePriceAdjustDO statusUpdate = new ErpPurchasePriceAdjustDO()
                .setStatus(ErpPurchasePriceAdjustStatusEnum.PROCESS.getStatus())
                .setTotalAdjustPrice(totalAdjustPrice);
        if (priceAdjustMapper.updateByIdAndStatus(id, ErpPurchasePriceAdjustStatusEnum.DRAFT.getStatus(),
                statusUpdate) == 0) {
            throw exception(PURCHASE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL);
        }
        priceAdjustItemMapper.deleteByAdjustId(id);
        items.forEach(item -> {
            item.setId(null);
            item.setAdjustId(id);
        });
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        priceAdjustItemMapper.insertBatch(items);
        operateLogService.recordUpdate(ERP_PURCHASE_PRICE_ADJUST_TYPE, id, adjustDO.getNo());
    }

    @Override
    public void updatePurchasePriceAdjustRemark(ErpPurchaseUpdateRemarkReqVO reqVO) {
        ErpPurchasePriceAdjustDO adjustDO = validateExists(reqVO.getId());
        priceAdjustMapper.updateById(new ErpPurchasePriceAdjustDO()
                .setId(reqVO.getId())
                .setRemark(reqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PURCHASE_PRICE_ADJUST_TYPE, reqVO.getId(), adjustDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasePriceAdjustStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(PURCHASE_PRICE_ADJUST_PROCESS_FAIL);
        }
        ErpPurchasePriceAdjustDO adjustDO = validateExists(id);
        if (!ErpAuditStatus.PROCESS.getStatus().equals(adjustDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_APPROVE_FAIL);
        }
        approveAdjust(adjustDO);
        operateLogService.recordStatus(ERP_PURCHASE_PRICE_ADJUST_TYPE, id, adjustDO.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchasePriceAdjust(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpPurchasePriceAdjustDO existDO = validateExists(id);
            if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(PURCHASE_PRICE_ADJUST_DELETE_FAIL_APPROVE);
            }
            priceAdjustMapper.deleteById(id);
            priceAdjustItemMapper.deleteByAdjustId(id);
            operateLogService.recordDelete(ERP_PURCHASE_PRICE_ADJUST_TYPE, id, existDO.getNo());
        }
    }

    @Override
    public ErpPurchasePriceAdjustDO getPurchasePriceAdjust(Long id) {
        ErpPurchasePriceAdjustDO adjust = priceAdjustMapper.selectById(id);
        fillPaymentPrice(adjust);
        return adjust;
    }

    @Override
    public ErpPurchasePriceAdjustDO validatePurchasePriceAdjust(Long id) {
        ErpPurchasePriceAdjustDO adjust = validateExists(id);
        fillPaymentPrice(adjust);
        return adjust;
    }

    @Override
    public PageResult<ErpPurchasePriceAdjustDO> getPurchasePriceAdjustPage(ErpPurchasePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpPurchasePriceAdjustDO> pageResult = priceAdjustMapper.selectPage(pageReqVO);
        fillPaymentPrice(pageResult.getList());
        return pageResult;
    }

    @Override
    public List<ErpPurchasePriceAdjustDO> getPurchasePriceAdjustList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return priceAdjustMapper.selectByIds(ids);
    }

    @Override
    public List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustId(Long adjustId) {
        return priceAdjustItemMapper.selectListByAdjustId(adjustId);
    }

    @Override
    public List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds) {
        if (CollUtil.isEmpty(adjustIds)) {
            return new ArrayList<>();
        }
        return priceAdjustItemMapper.selectListByAdjustIds(adjustIds);
    }

    private void fillDeptIdFromPurchaseIn(ErpPurchasePriceAdjustDO adjustDO, List<ErpPurchasePriceAdjustItemDO> items) {
        if (adjustDO.getDeptId() != null || CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> inIds = items.stream()
                .map(ErpPurchasePriceAdjustItemDO::getInId)
                .filter(inId -> inId != null)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(inIds)) {
            return;
        }
        CollUtil.emptyIfNull(purchaseInMapper.selectBatchIds(inIds)).stream()
                .map(ErpPurchaseInDO::getDeptId)
                .filter(deptId -> deptId != null)
                .findFirst()
                .ifPresent(adjustDO::setDeptId);
    }

    @Override
    public ErpPurchasePriceAdjustImportRespVO importPurchasePriceAdjustItems(List<ErpPurchasePriceAdjustImportExcelVO> list) {
        ErpPurchasePriceAdjustImportRespVO respVO = new ErpPurchasePriceAdjustImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Set<String> productCodes = list.stream()
                .map(ErpPurchasePriceAdjustImportExcelVO::getProductCode)
                .map(ErpPurchasePriceAdjustServiceImpl::trimToNull)
                .filter(code -> StrUtil.isNotBlank(code))
                .collect(Collectors.toSet());
        Map<String, ErpProductRespVO> productVOMap = new HashMap<>();
        Map<String, Long> productIdMap = new HashMap<>();
        if (!productCodes.isEmpty()) {
            productMapper.selectListByCodes(productCodes).forEach(product -> productIdMap.put(product.getCode(), product.getId()));
            productVOMap = productService.getProductVOList(productIdMap.values()).stream()
                    .collect(Collectors.toMap(ErpProductRespVO::getCode, item -> item, (a, b) -> a));
        }
        for (int i = 0; i < list.size(); i++) {
            ErpPurchasePriceAdjustImportExcelVO row = list.get(i);
            if (row == null || isEmptyImportRow(row)) {
                continue;
            }
            try {
                String productCode = trimToNull(row.getProductCode());
                if (productCode == null) {
                    throw new IllegalArgumentException("产品编码不能为空");
                }
                ErpProductRespVO product = productVOMap.get(productCode);
                if (product == null) {
                    throw new IllegalArgumentException("产品不存在：" + productCode);
                }
                BigDecimal count = requirePositiveCount(row.getCount(), "数量不能为空且必须大于0");
                BigDecimal newPrice = row.getNewPrice();
                if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("调价后单价不能小于0");
                }
                BigDecimal oldPrice = product.getLastPurchasePrice() != null
                        ? product.getLastPurchasePrice()
                        : (product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
                BigDecimal adjustPrice = newPrice.subtract(oldPrice).multiply(count).setScale(2, RoundingMode.HALF_UP);
                ErpPurchasePriceAdjustSaveReqVO.Item item = new ErpPurchasePriceAdjustSaveReqVO.Item();
                item.setProductId(product.getId());
                item.setProductCode(product.getCode());
                item.setProductName(product.getName());
                item.setProductUnitName(product.getUnitName());
                item.setVehicleModel(product.getVehicleModel());
                item.setStandard(product.getStandard());
                item.setFeatureCode(product.getFeatureCode());
                item.setOriginPlace(product.getOriginPlace());
                item.setBrand(product.getBrand());
                item.setDrawingNo(product.getDrawingNo());
                item.setCount(count);
                item.setOldPrice(oldPrice);
                item.setNewPrice(newPrice);
                respVO.getItems().add(item);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpPurchasePriceAdjustImportRespVO.FailureItem(
                        i + 2, row != null ? row.getProductCode() : null, ex.getMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseImportResultRespVO importPurchasePriceAdjustOrderList(List<ErpPurchasePriceAdjustOrderImportExcelVO> list) {
        ErpPurchaseImportResultRespVO respVO = new ErpPurchaseImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpSupplierDO> supplierMap = buildSupplierMap();
        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchasePriceAdjustOrderProductCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                productMap.values().stream().map(ErpProductDO::getId).collect(Collectors.toSet()));
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getPurchaseWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getName, warehouse -> warehouse, (a, b) -> a));

        List<PurchasePriceAdjustOrderImportGroup> groups = new ArrayList<>();
        PurchasePriceAdjustOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpPurchasePriceAdjustOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankPurchasePriceAdjustOrderRow(row)) {
                continue;
            }
            boolean hasMain = hasPurchasePriceAdjustOrderMainFields(row);
            boolean hasDetail = hasPurchasePriceAdjustOrderDetailFields(row);
            if (hasMain) {
                ErpSupplierDO supplier = resolveSupplier(row.getSupplierName(), supplierMap);
                currentGroup = new PurchasePriceAdjustOrderImportGroup(rowNo, row, supplier);
                groups.add(currentGroup);
                String orderNo = resolvePurchasePriceAdjustOrderNo(rowNo, row);
                String supplierName = trimToNull(row.getSupplierName());
                if (supplierName == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商不能为空");
                } else if (supplier == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商不存在：" + supplierName);
                } else if (CommonStatusEnum.isDisable(supplier.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商(" + supplier.getName() + ")未启用");
                }
                validateImportDate(respVO, rowNo, orderNo, null, "调价时间", row.getAdjustTime());
            } else if (hasDetail && currentGroup == null) {
                addImportFailure(respVO, rowNo, null, trimToNull(row.getProductCode()), "明细行前缺少调价单主表信息");
                continue;
            }
            if (!hasDetail) {
                continue;
            }
            String orderNo = currentGroup == null ? null : resolvePurchasePriceAdjustOrderNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            String productCode = trimToNull(row.getProductCode());
            boolean valid = true;
            ErpProductDO product = productMap.get(productCode);
            ErpWarehouseDO warehouse = trimToNull(row.getWarehouseName()) == null ? null : warehouseMap.get(trimToNull(row.getWarehouseName()));
            if (productCode == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品编码不能为空");
                valid = false;
            } else if (product == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品不存在");
                valid = false;
            }
            if (trimToNull(row.getWarehouseName()) != null && warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "仓库不存在：" + row.getWarehouseName());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "数量必须大于 0");
                valid = false;
            }
            if (row.getNewPrice() == null || row.getNewPrice().compareTo(BigDecimal.ZERO) < 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "调价后单价不能小于 0");
                valid = false;
            }
            if (valid) {
                currentGroup.getRows().add(new PurchasePriceAdjustOrderImportRow(rowNo, row, product, warehouse));
            }
        }

        for (PurchasePriceAdjustOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolvePurchasePriceAdjustOrderNo(group.getRowNo(), group.getMainRow()), null,
                        "采购调价单至少需要一行明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }

        for (PurchasePriceAdjustOrderImportGroup group : groups) {
            ErpPurchasePriceAdjustSaveReqVO saveReqVO = buildPurchasePriceAdjustOrderSaveReq(group, productVOMap);
            Long id = createPurchasePriceAdjust(saveReqVO);
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpPurchasePriceAdjustSaveReqVO buildPurchasePriceAdjustOrderSaveReq(PurchasePriceAdjustOrderImportGroup group,
                                                                                 Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchasePriceAdjustOrderImportExcelVO mainRow = group.getMainRow();
        ErpPurchasePriceAdjustSaveReqVO saveReqVO = new ErpPurchasePriceAdjustSaveReqVO();
        saveReqVO.setSupplierId(group.getSupplier().getId());
        saveReqVO.setAdjustType(ErpPurchasePriceAdjustTypeEnum.BY_ITEM.getType());
        saveReqVO.setAdjustTime(parseImportDate(mainRow.getAdjustTime(), LocalDateTime.now()));
        saveReqVO.setRemark(trimToNull(mainRow.getRemark()));
        saveReqVO.setItems(group.getRows().stream()
                .map(row -> buildPurchasePriceAdjustOrderItem(row, productVOMap))
                .collect(Collectors.toList()));
        return saveReqVO;
    }

    private ErpPurchasePriceAdjustSaveReqVO.Item buildPurchasePriceAdjustOrderItem(PurchasePriceAdjustOrderImportRow importRow,
                                                                                   Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchasePriceAdjustOrderImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpProductRespVO productVO = productVOMap.get(product.getId());
        ErpPurchasePriceAdjustSaveReqVO.Item item = new ErpPurchasePriceAdjustSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setWarehouseId(importRow.getWarehouse() == null ? null : importRow.getWarehouse().getId());
        item.setCount(row.getItemCount());
        item.setNewPrice(row.getNewPrice());
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        if (productVO != null) {
            item.setProductUnitName(productVO.getUnitName());
            item.setVehicleModel(productVO.getVehicleModel());
            item.setStandard(productVO.getStandard());
            item.setFeatureCode(productVO.getFeatureCode());
            item.setOriginPlace(productVO.getOriginPlace());
            item.setBrand(productVO.getBrand());
            item.setDrawingNo(productVO.getDrawingNo());
        }
        return item;
    }

    private Map<String, ErpSupplierDO> buildSupplierMap() {
        Map<String, ErpSupplierDO> map = new LinkedHashMap<>();
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        supplierService.getSupplierPage(pageReqVO).getList().forEach(supplier -> putSupplierKeys(map, supplier));
        return map;
    }

    private ErpSupplierDO resolveSupplier(String supplierName, Map<String, ErpSupplierDO> supplierMap) {
        return supplierMap.get(normalizeKey(supplierName));
    }

    private void putSupplierKeys(Map<String, ErpSupplierDO> map, ErpSupplierDO supplier) {
        putIfNotBlank(map, supplier.getCode(), supplier);
        putIfNotBlank(map, supplier.getOldCode(), supplier);
        putIfNotBlank(map, supplier.getName(), supplier);
        putIfNotBlank(map, supplier.getShortName(), supplier);
    }

    private <T> void putIfNotBlank(Map<String, T> map, String key, T value) {
        String normalized = normalizeKey(key);
        if (StrUtil.isNotBlank(normalized)) {
            map.putIfAbsent(normalized, value);
        }
    }

    private String normalizeKey(String value) {
        String normalized = trimToNull(value);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        return StrUtil.cleanBlank(normalized).toLowerCase(Locale.ROOT);
    }

    private static Set<String> extractPurchasePriceAdjustOrderProductCodes(List<ErpPurchasePriceAdjustOrderImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchasePriceAdjustOrderImportExcelVO row : list) {
            if (row == null) {
                continue;
            }
            String code = trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isBlankPurchasePriceAdjustOrderRow(ErpPurchasePriceAdjustOrderImportExcelVO row) {
        return row == null || !hasPurchasePriceAdjustOrderMainFields(row) && !hasPurchasePriceAdjustOrderDetailFields(row);
    }

    private boolean hasPurchasePriceAdjustOrderMainFields(ErpPurchasePriceAdjustOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getNo()))
                || StrUtil.isNotBlank(trimToNull(row.getSupplierName()))
                || StrUtil.isNotBlank(trimToNull(row.getAdjustTime()))
                || StrUtil.isNotBlank(trimToNull(row.getRemark()));
    }

    private boolean hasPurchasePriceAdjustOrderDetailFields(ErpPurchasePriceAdjustOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getProductCode()))
                || StrUtil.isNotBlank(trimToNull(row.getWarehouseName()))
                || row.getItemCount() != null
                || row.getNewPrice() != null;
    }

    private String resolvePurchasePriceAdjustOrderNo(Integer rowNo, ErpPurchasePriceAdjustOrderImportExcelVO row) {
        String no = trimToNull(row.getNo());
        if (no != null) {
            return no;
        }
        String supplierName = trimToNull(row.getSupplierName());
        if (supplierName != null) {
            return supplierName;
        }
        return "第 " + rowNo + " 行";
    }

    private boolean validateImportDate(ErpPurchaseImportResultRespVO respVO, Integer rowNo, String orderNo,
                                       String productCode, String label, String value) {
        if (StrUtil.isBlank(trimToNull(value))) {
            return true;
        }
        try {
            parseImportDate(value, null);
            return true;
        } catch (IllegalArgumentException ignored) {
            addImportFailure(respVO, rowNo, orderNo, productCode, label + "格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            return false;
        }
    }

    private LocalDateTime parseImportDate(String value, LocalDateTime defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        }) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

    private void addImportFailure(ErpPurchaseImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpPurchaseImportResultRespVO.FailureItem(
                rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    @Override
    public void updatePurchasePriceAdjustPaymentPrice(Long id, BigDecimal paymentPrice) {
        ErpPurchasePriceAdjustDO adjust = validateExists(id);
        BigDecimal settledPrice = paymentPrice == null ? BigDecimal.ZERO : paymentPrice;
        BigDecimal totalAdjustPrice = adjust.getTotalAdjustPrice() == null ? BigDecimal.ZERO : adjust.getTotalAdjustPrice();
        if (settledPrice.abs().compareTo(totalAdjustPrice.abs()) > 0) {
            throw exception(PURCHASE_PRICE_ADJUST_FAIL_PAYMENT_PRICE_EXCEED, settledPrice, totalAdjustPrice);
        }
    }

    // ========== 私有辅助方法 ==========

    private void fillPaymentPrice(ErpPurchasePriceAdjustDO adjust) {
        if (adjust == null) {
            return;
        }
        adjust.setPaymentPrice(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                adjust.getId(), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()));
    }

    private void fillPaymentPrice(List<ErpPurchasePriceAdjustDO> adjusts) {
        if (CollUtil.isEmpty(adjusts)) {
            return;
        }
        Map<Long, BigDecimal> paymentPriceMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                adjusts.stream().map(ErpPurchasePriceAdjustDO::getId).collect(Collectors.toSet()),
                ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
        adjusts.forEach(adjust -> adjust.setPaymentPrice(paymentPriceMap.getOrDefault(adjust.getId(), BigDecimal.ZERO)));
    }

    private ErpPurchasePriceAdjustDO validateExists(Long id) {
        ErpPurchasePriceAdjustDO adjustDO = priceAdjustMapper.selectById(id);
        if (adjustDO == null) {
            throw exception(PURCHASE_PRICE_ADJUST_NOT_EXISTS);
        }
        return adjustDO;
    }

    private void validateMainForm(ErpPurchasePriceAdjustSaveReqVO reqVO) {
        // 校验供应商
        if (reqVO.getSupplierId() == null) {
            throw exception(PURCHASE_PRICE_ADJUST_SUBMIT_SUPPLIER_REQUIRED);
        }
        supplierService.validateSupplier(reqVO.getSupplierId());
        // 校验子表非空
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
        }
        // 校验调价类型
        Integer type = reqVO.getAdjustType();
        if (type == null
                || (!ErpPurchasePriceAdjustTypeEnum.isByInOrder(type)
                && !ErpPurchasePriceAdjustTypeEnum.isByItem(type))) {
            throw exception(PURCHASE_PRICE_ADJUST_TYPE_INVALID);
        }
    }

    /**
     * 子表逐行校验 + 回填；返回构造好的 DO 列表
     *
     * @param reqVO           请求 VO
     * @param excludeAdjustId 更新场景排除自己的已存在子项（用于将来扩展；当前实现按重建策略，未用到）
     */
    private List<ErpPurchasePriceAdjustItemDO> buildDraftItems(Integer adjustType,
                                                               List<ErpPurchasePriceAdjustSaveReqVO.Item> itemReqs) {
        if (CollUtil.isEmpty(itemReqs)) {
            return Collections.emptyList();
        }
        boolean byInOrder = ErpPurchasePriceAdjustTypeEnum.isByInOrder(adjustType);
        return itemReqs.stream()
                .filter(item -> item != null && item.getProductId() != null && item.getCount() != null
                        && item.getCount().compareTo(BigDecimal.ZERO) > 0 && item.getNewPrice() != null
                        && (!byInOrder || (item.getInId() != null && item.getInItemId() != null)))
                .map(item -> {
                    ErpPurchasePriceAdjustItemDO draftItem =
                            BeanUtils.toBean(item, ErpPurchasePriceAdjustItemDO.class);
                    draftItem.setId(null);
                    BigDecimal oldPrice = draftItem.getOldPrice() != null
                            ? draftItem.getOldPrice() : BigDecimal.ZERO;
                    draftItem.setOldPrice(oldPrice);
                    draftItem.setAdjustPrice(draftItem.getNewPrice().subtract(oldPrice)
                            .multiply(draftItem.getCount()).setScale(2, RoundingMode.HALF_UP));
                    return draftItem;
                })
                .collect(Collectors.toList());
    }

    private void insertDraftItems(Long adjustId, List<ErpPurchasePriceAdjustItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> {
            item.setId(null);
            item.setAdjustId(adjustId);
        });
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        priceAdjustItemMapper.insertBatch(items);
    }

    private List<ErpPurchasePriceAdjustItemDO> buildAndValidateItems(ErpPurchasePriceAdjustSaveReqVO reqVO,
                                                                    Long excludeAdjustId) {
        Integer adjustType = reqVO.getAdjustType();
        List<ErpPurchasePriceAdjustItemDO> items = new ArrayList<>(reqVO.getItems().size());

        Map<Long, ErpPurchaseInItemDO> inItemMap = new HashMap<>();
        if (ErpPurchasePriceAdjustTypeEnum.isByInOrder(adjustType)) {
            Set<Long> inItemIds = reqVO.getItems().stream()
                    .map(ErpPurchasePriceAdjustSaveReqVO.Item::getInItemId)
                    .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            if (!inItemIds.isEmpty()) {
                inItemMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                        purchaseInItemMapper.selectBatchIds(inItemIds), ErpPurchaseInItemDO::getId);
            }
        }

        // 批量预取产品信息（减少循环内查询）；按入库单调价时以入库明细的 productId 为准
        Set<Long> productIds = reqVO.getItems().stream()
                .map(ErpPurchasePriceAdjustSaveReqVO.Item::getProductId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        productIds.addAll(inItemMap.values().stream()
                .map(ErpPurchaseInItemDO::getProductId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, ErpProductRespVO> productMap = productIds.isEmpty() ? new HashMap<>()
                : DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));

        if (ErpPurchasePriceAdjustTypeEnum.isByItem(adjustType)) {
            List<ErpPurchasePriceAdjustItemDO> result = new ArrayList<>(reqVO.getItems().size());
            Set<String> itemKeySet = new LinkedHashSet<>();
            for (ErpPurchasePriceAdjustSaveReqVO.Item voItem : reqVO.getItems()) {
                ErpProductRespVO product = productMap.get(voItem.getProductId());
                if (product == null) {
                    throw exception(PRODUCT_NOT_EXISTS);
                }
                if (voItem.getCount() == null || voItem.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
                }
                BigDecimal oldPrice = product.getLastPurchasePrice() != null
                        ? product.getLastPurchasePrice()
                        : (product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
                BigDecimal count = voItem.getCount();
                BigDecimal adjustPrice = voItem.getNewPrice().subtract(oldPrice).multiply(count)
                        .setScale(2, RoundingMode.HALF_UP);

                ErpPurchasePriceAdjustItemDO item = BeanUtils.toBean(voItem, ErpPurchasePriceAdjustItemDO.class);
                item.setId(null);
                item.setInId(null);
                item.setInItemId(null);
                item.setInNo(null);
                item.setOldPrice(oldPrice);
                item.setCount(count);
                item.setAdjustPrice(adjustPrice);
                if (item.getWarehouseId() == null) {
                    item.setWarehouseId(product.getDefaultWarehouseId());
                }
                fillProductSnapshot(item, product);
                if (item.getWarehousePosition() == null) {
                    item.setWarehousePosition(product.getShelf());
                }
                String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
                String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|0";
                if (!itemKeySet.add(itemKey)) {
                    throw exception(PURCHASE_PRICE_ADJUST_ITEM_DUPLICATE, productKey);
                }
                result.add(item);
            }
            Set<Long> warehouseIds = convertSet(result, ErpPurchasePriceAdjustItemDO::getWarehouseId);
            warehouseService.validPurchaseWarehouseList(warehouseIds);
            Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
            result.forEach(item -> fillDeptIdFromWarehouse(item, warehouseMap));
            return result;
        }

        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpPurchasePriceAdjustSaveReqVO.Item voItem : reqVO.getItems()) {
            // 校验入库项存在 + 入库单一致
            ErpPurchaseInItemDO inItem = inItemMap.get(voItem.getInItemId());
            if (inItem == null) {
                throw exception(PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
            }
            if (!inItem.getInId().equals(voItem.getInId())) {
                throw exception(PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
            }

            // 校验 newPrice >= 0
            if (voItem.getNewPrice() == null
                    || voItem.getNewPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PURCHASE_PRICE_ADJUST_NEW_PRICE_NEGATIVE);
            }

            // 防重复规则（Q3=β）：只在 BY_ITEM 时强校验
            if (ErpPurchasePriceAdjustTypeEnum.isByItem(adjustType)
                    && Boolean.TRUE.equals(inItem.getAdjusted())) {
                throw exception(PURCHASE_PRICE_ADJUST_ITEM_ADJUSTED);
            }

            // 以当前入库项单价为 oldPrice；count 以入库明细快照
            BigDecimal oldPrice = inItem.getProductPrice() != null ? inItem.getProductPrice() : BigDecimal.ZERO;
            BigDecimal count = inItem.getCount() != null ? inItem.getCount() : BigDecimal.ZERO;
            BigDecimal adjustPrice = voItem.getNewPrice().subtract(oldPrice).multiply(count)
                    .setScale(2, RoundingMode.HALF_UP);

            ErpPurchasePriceAdjustItemDO item = BeanUtils.toBean(voItem, ErpPurchasePriceAdjustItemDO.class);
            item.setId(null);
            item.setOldPrice(oldPrice);
            item.setCount(count);
            item.setAdjustPrice(adjustPrice);

            // 冗余信息：优先从产品资料带最新值；次选 inItem（历史快照）
            item.setProductId(inItem.getProductId());
            item.setWarehouseId(inItem.getWarehouseId());
            item.setDeptId(inItem.getDeptId());
            item.setInNo(voItem.getInNo()); // VO 未填也允许，Controller 拼装时可补

            ErpProductRespVO product = productMap.get(inItem.getProductId());
            if (product != null) {
                if (item.getProductCode() == null) item.setProductCode(product.getCode());
                if (item.getProductName() == null) item.setProductName(product.getName());
                if (item.getProductUnitName() == null) item.setProductUnitName(product.getUnitName());
                if (item.getVehicleModel() == null) item.setVehicleModel(product.getVehicleModel());
                if (item.getStandard() == null) item.setStandard(product.getStandard());
                if (item.getFeatureCode() == null) item.setFeatureCode(product.getFeatureCode());
                if (item.getOriginPlace() == null) item.setOriginPlace(product.getOriginPlace());
                if (item.getBrand() == null) item.setBrand(product.getBrand());
                if (item.getDrawingNo() == null) item.setDrawingNo(product.getDrawingNo());
            }
            // 货架位从入库项拿（入库项是快照，反映当时的放置）
            if (item.getWarehousePosition() == null) {
                item.setWarehousePosition(inItem.getWarehousePosition());
            }
            String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
            String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|0";
            if (!itemKeySet.add(itemKey)) {
                throw exception(PURCHASE_PRICE_ADJUST_ITEM_DUPLICATE, productKey);
            }
            items.add(item);
        }
        Set<Long> warehouseIds = convertSet(items, ErpPurchasePriceAdjustItemDO::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        items.forEach(item -> fillDeptIdFromWarehouse(item, warehouseMap));
        return items;
    }

    private void fillDeptIdFromWarehouse(ErpPurchasePriceAdjustItemDO item, Map<Long, ErpWarehouseDO> warehouseMap) {
        if (item.getDeptId() == null && item.getWarehouseId() != null) {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private BigDecimal sumAdjustPrice(List<ErpPurchasePriceAdjustItemDO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpPurchasePriceAdjustItemDO item : items) {
            if (item.getAdjustPrice() != null) {
                total = total.add(item.getAdjustPrice());
            }
        }
        return total;
    }

    private void fillProductSnapshot(ErpPurchasePriceAdjustItemDO item, ErpProductRespVO product) {
        if (item.getProductCode() == null) item.setProductCode(product.getCode());
        if (item.getProductName() == null) item.setProductName(product.getName());
        if (item.getProductUnitName() == null) item.setProductUnitName(product.getUnitName());
        if (item.getVehicleModel() == null) item.setVehicleModel(product.getVehicleModel());
        if (item.getStandard() == null) item.setStandard(product.getStandard());
        if (item.getFeatureCode() == null) item.setFeatureCode(product.getFeatureCode());
        if (item.getOriginPlace() == null) item.setOriginPlace(product.getOriginPlace());
        if (item.getBrand() == null) item.setBrand(product.getBrand());
        if (item.getDrawingNo() == null) item.setDrawingNo(product.getDrawingNo());
    }

    private boolean isEmptyImportRow(ErpPurchasePriceAdjustImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getProductCode())
                && row.getCount() == null && row.getNewPrice() == null;
    }

    private BigDecimal requirePositiveCount(BigDecimal count, String message) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return count;
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

    // ========== 审批核心算法 ==========

    private static class PurchasePriceAdjustOrderImportGroup {

        private final Integer rowNo;
        private final ErpPurchasePriceAdjustOrderImportExcelVO mainRow;
        private final ErpSupplierDO supplier;
        private final List<PurchasePriceAdjustOrderImportRow> rows = new ArrayList<>();

        private PurchasePriceAdjustOrderImportGroup(Integer rowNo, ErpPurchasePriceAdjustOrderImportExcelVO mainRow,
                                                    ErpSupplierDO supplier) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.supplier = supplier;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchasePriceAdjustOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpSupplierDO getSupplier() {
            return supplier;
        }

        public List<PurchasePriceAdjustOrderImportRow> getRows() {
            return rows;
        }

    }

    private static class PurchasePriceAdjustOrderImportRow {

        private final Integer rowNo;
        private final ErpPurchasePriceAdjustOrderImportExcelVO row;
        private final ErpProductDO product;
        private final ErpWarehouseDO warehouse;

        private PurchasePriceAdjustOrderImportRow(Integer rowNo, ErpPurchasePriceAdjustOrderImportExcelVO row,
                                                  ErpProductDO product, ErpWarehouseDO warehouse) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchasePriceAdjustOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public ErpWarehouseDO getWarehouse() {
            return warehouse;
        }

    }

    private void approveAdjust(ErpPurchasePriceAdjustDO adjustDO) {
        // 1. 读子项
        List<ErpPurchasePriceAdjustItemDO> items = priceAdjustItemMapper.selectListByAdjustId(adjustDO.getId());
        if (CollUtil.isEmpty(items)) {
            throw exception(PURCHASE_PRICE_ADJUST_ITEM_EMPTY);
        }
        warehouseService.validPurchaseWarehouseList(convertSet(items, ErpPurchasePriceAdjustItemDO::getWarehouseId));

        // 2. 乐观锁更新主表状态：PROCESS -> APPROVE
        LocalDateTime now = LocalDateTime.now();
        ErpPurchasePriceAdjustDO updateObj = new ErpPurchasePriceAdjustDO()
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setApproveTime(now);
        int affected = priceAdjustMapper.updateByIdAndStatus(adjustDO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(PURCHASE_PRICE_ADJUST_APPROVE_FAIL);
        }

        // 3. 遍历 items 更新原入库项的 productPrice，记录每个产品的 newPrice（取 inTime 最晚的）
        Map<Long, ErpPurchaseInItemDO> inItemCache = new HashMap<>();
        Map<Long, ErpPurchaseInDO> inCache = new HashMap<>();

        // 用于产品 lastPurchasePrice 回写：productId -> (inTime, newPrice)
        Map<Long, BigDecimal> productLatestPrice = new HashMap<>();
        Map<Long, LocalDateTime> productLatestInTime = new HashMap<>();

        // 按 (productId, warehouseId) 聚合差额 + 总入库量（用于 cost 摊分）
        Map<String, BigDecimal> stockDeltaMap = new HashMap<>();
        Map<String, BigDecimal> stockCountMap = new HashMap<>();
        Map<String, Long> stockProductIdMap = new HashMap<>();
        Map<String, Long> stockWarehouseIdMap = new HashMap<>();

        if (ErpPurchasePriceAdjustTypeEnum.isByItem(adjustDO.getAdjustType())) {
            for (ErpPurchasePriceAdjustItemDO item : items) {
                Long productId = item.getProductId();
                BigDecimal newPrice = item.getNewPrice();
                if (productId != null && newPrice != null) {
                    priceHistoryService.createPriceHistory(productId, 1, adjustDO.getSupplierId(),
                            newPrice, item.getCount(), 3, adjustDO.getId(), adjustDO.getNo(),
                            adjustDO.getAdjustTime() != null ? adjustDO.getAdjustTime() : now);
                    productLatestPrice.put(productId, newPrice);
                }
            }
            List<Map.Entry<Long, BigDecimal>> sortedEntries = productLatestPrice.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .collect(Collectors.toList());
            for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
                productService.updateProductLastPurchasePrice(entry.getKey(), entry.getValue());
            }
            return;
        }

        for (ErpPurchasePriceAdjustItemDO item : items) {
            // 3.1 读原入库项
            ErpPurchaseInItemDO inItem = purchaseInItemMapper.selectById(item.getInItemId());
            if (inItem == null) {
                throw exception(PURCHASE_PRICE_ADJUST_ITEM_NOT_EXISTS);
            }
            inItemCache.put(inItem.getId(), inItem);

            // 3.2 首次调价：记录 originalProductPrice
            BigDecimal oldPrice = inItem.getProductPrice() != null ? inItem.getProductPrice() : BigDecimal.ZERO;
            BigDecimal newPrice = item.getNewPrice();
            BigDecimal inCount = inItem.getCount() != null ? inItem.getCount() : BigDecimal.ZERO;

            ErpPurchaseInItemDO inItemUpdate = new ErpPurchaseInItemDO()
                    .setId(inItem.getId())
                    .setProductPrice(newPrice)
                    .setAdjusted(true)
                    .setAdjustId(adjustDO.getId());
            if (inItem.getOriginalProductPrice() == null) {
                inItemUpdate.setOriginalProductPrice(oldPrice);
            }
            BigDecimal newTotal = newPrice.multiply(inCount);
            inItemUpdate.setTotalPrice(newTotal);
            inItemUpdate.setTaxPercent(null); inItemUpdate.setTaxPrice(BigDecimal.ZERO);
            purchaseInItemMapper.updateById(inItemUpdate);

            // 3.3 聚合 (productId, warehouseId) 差额
            Long productId = inItem.getProductId();
            Long warehouseId = inItem.getWarehouseId();
            String key = productId + "::" + warehouseId;
            BigDecimal itemDelta = newPrice.subtract(oldPrice).multiply(inCount);
            stockDeltaMap.merge(key, itemDelta, BigDecimal::add);
            stockCountMap.merge(key, inCount, BigDecimal::add);
            stockProductIdMap.putIfAbsent(key, productId);
            stockWarehouseIdMap.putIfAbsent(key, warehouseId);

            // 3.4 lastPurchasePrice 选择器：取同一 productId 中 inTime 最晚的 newPrice
            ErpPurchaseInDO inDO = inCache.computeIfAbsent(inItem.getInId(), purchaseInMapper::selectById);
            LocalDateTime inTime = inDO != null ? inDO.getInTime() : null;
            if (inTime != null) {
                LocalDateTime current = productLatestInTime.get(productId);
                if (current == null || inTime.isAfter(current)) {
                    productLatestInTime.put(productId, inTime);
                    productLatestPrice.put(productId, newPrice);
                }
            } else {
                // 没有 inTime 的异常场景：至少记录一个回写值
                productLatestPrice.putIfAbsent(productId, newPrice);
            }
        }

        // 4. 按涉及的入库单重算 erp_purchase_in 主表
        Set<Long> touchedInIds = new HashSet<>();
        for (ErpPurchaseInItemDO inItem : inItemCache.values()) {
            touchedInIds.add(inItem.getInId());
        }
        for (Long inId : touchedInIds) {
            recalcPurchaseIn(inId);
        }

        // 5. 按 (productId, warehouseId) 调用 stockService.adjustStockCostAmount
        for (Map.Entry<String, BigDecimal> entry : stockDeltaMap.entrySet()) {
            String key = entry.getKey();
            BigDecimal deltaFull = entry.getValue();
            BigDecimal sumInCount = stockCountMap.get(key);
            Long productId = stockProductIdMap.get(key);
            Long warehouseId = stockWarehouseIdMap.get(key);
            if (warehouseId == null || deltaFull == null
                    || deltaFull.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            stockService.adjustStockCostAmount(productId, warehouseId, deltaFull, sumInCount,
                    adjustDO.getId(), adjustDO.getNo(), adjustDO.getAdjustTime() != null
                            ? adjustDO.getAdjustTime() : now);
        }

        // 6. 回写产品 lastPurchasePrice
        // 简化策略：若某产品无 inTime 信息，用 productLatestPrice 里已有的值（前面 putIfAbsent 兜底）
        List<Map.Entry<Long, BigDecimal>> sortedEntries = productLatestPrice.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey()))
                .collect(Collectors.toList());
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
            productService.updateProductLastPurchasePrice(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 按入库单重算主表 totalProductPrice / totalTaxPrice / discountPrice / totalPrice 并标记 adjusted=true
     */
    private void recalcPurchaseIn(Long inId) {
        ErpPurchaseInDO inDO = purchaseInMapper.selectById(inId);
        if (inDO == null) {
            return;
        }
        List<ErpPurchaseInItemDO> inItems = purchaseInItemMapper.selectListByInId(inId);
        BigDecimal totalProductPrice = BigDecimal.ZERO;
        BigDecimal totalTaxPrice = BigDecimal.ZERO;
        for (ErpPurchaseInItemDO inItem : inItems) {
            if (inItem.getTotalPrice() != null) {
                totalProductPrice = totalProductPrice.add(inItem.getTotalPrice());
            }
        }
        BigDecimal discountPercent = inDO.getDiscountPercent() != null ? inDO.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(totalProductPrice, discountPercent);
        if (discountPrice == null) {
            discountPrice = BigDecimal.ZERO;
        }
        BigDecimal feeAmount = inDO.getFeeAmount() != null ? inDO.getFeeAmount()
                : (inDO.getOtherPrice() != null ? inDO.getOtherPrice() : BigDecimal.ZERO);
        BigDecimal totalPrice = totalProductPrice.subtract(discountPrice).add(feeAmount);

        ErpPurchaseInDO updateIn = new ErpPurchaseInDO()
                .setId(inId)
                .setTotalProductPrice(totalProductPrice)
                .setTotalTaxPrice(totalTaxPrice)
                .setDiscountPrice(discountPrice)
                .setFeeAmount(feeAmount)
                .setOtherPrice(feeAmount)
                .setTotalPrice(totalPrice)
                .setAdjusted(true);
        purchaseInMapper.updateById(updateIn);
    }

}
