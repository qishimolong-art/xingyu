package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutUpdateExpressFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockLockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 销售出库 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpSaleOutServiceImpl implements ErpSaleOutService {
    @Resource
    private cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService tradeSnapshotService;

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_out";
    private static final String EXPRESS_FILE_DIRECTORY = "erp/sale-out/express";
    private static final long EXPRESS_FILE_MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> EXPRESS_FILE_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png"));
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockLockService stockLockService;
    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;
    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Resource
    private FileApi fileApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOut(ErpSaleOutSaveReqVO createReqVO) {
        // 1.1 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(createReqVO.getOrderId());
        createReqVO.setCustomerId(saleOrder.getCustomerId());
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO, createReqVO.getItems());
        clearItemSourceSnapshots(createReqVO.getItems());
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleOrder.getDeptId());
        customerService.validateCustomerForSale(saleOrder.getCustomerId(), saleOrder.getDeptId());
        // 1.2 校验出库项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(createReqVO.getItems(), createReqVO.getOrderId());
        // 1.3 校验结算账户
        accountService.validateAccount(createReqVO.getAccountId());
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成出库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_OUT_NO_PREFIX);
        if (saleOutMapper.selectByNo(no) != null) {
            throw exception(SALE_OUT_NO_EXISTS);
        }

        // 2.1 插入出库
        ErpSaleOutDO saleOut = BeanUtils.toBean(createReqVO, ErpSaleOutDO.class);
        saleOut.setNo(no);
        saleOut.setStatus(ErpAuditStatus.PROCESS.getStatus());
        saleOut.setOrderNo(saleOrder.getNo());
        saleOut.setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(saleOut, saleOutItems);
        saleDocumentDefaultService.fillCreateDefaults(saleOut);
        saleOutMapper.insert(saleOut);
        // 2.2 Insert sale out items.
        saleOutItems.forEach(o -> o.setOutId(saleOut.getId()));
        saleOutItemMapper.insertBatch(saleOutItems);

        // 3. 更新销售订单的出库数量
        updateSaleOrderOutCount(createReqVO.getOrderId());
        recordCreate(saleOut.getId(), saleOut.getNo());
        return saleOut.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGeneratedSaleOut(ErpSaleOutSaveReqVO createReqVO, Integer sourceType, Long sourceId, String sourceNo) {
        return createGeneratedSaleOut(createReqVO, sourceType, sourceId, sourceNo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGeneratedSaleOut(ErpSaleOutSaveReqVO createReqVO, Integer sourceType, Long sourceId, String sourceNo,
                                       Boolean deferStockOutBill) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO, createReqVO.getItems());
        // 1. Validate base data. The new sale flow does not depend on old sale orders.
        Long saleDeptId = resolveSaleDeptId(createReqVO);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleDeptId);
        customerService.validateCustomerForGeneratedSale(createReqVO.getCustomerId(), saleDeptId);
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(
                createReqVO.getItems(), null, saleDeptId);
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_OUT_NO_PREFIX);
        if (saleOutMapper.selectByNo(no) != null) {
            throw exception(SALE_OUT_NO_EXISTS);
        }

        // 2. 插入销售单，并保留来源单据用于追溯
        ErpSaleOutDO saleOut = BeanUtils.toBean(createReqVO, ErpSaleOutDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(sourceType).setSourceId(sourceId).setSourceNo(sourceNo)
                .setOrderId(null).setOrderNo(null));
        calculateTotalPrice(saleOut, saleOutItems);
        saleDocumentDefaultService.fillCreateDefaults(saleOut);
        saleOutMapper.insert(saleOut);
        Long saleOutId = saleOut.getId();
        if (saleOutId == null) {
            ErpSaleOutDO inserted = DataPermissionUtils.executeIgnore(
                    () -> saleOutMapper.selectBySourceTypeAndSourceId(sourceType, sourceId));
            if (inserted == null) {
                throw exception(SALE_OUT_NO_EXISTS);
            }
            saleOutId = inserted.getId();
            saleOut.setId(saleOutId).setNo(inserted.getNo());
        }
        saleOutItems.forEach(o -> o.setOutId(saleOut.getId()).setOrderItemId(null));
        saleOutItemMapper.insertBatch(saleOutItems);

        // 3. 自动审核，复用现有销售出库扣库存流水
        Long generatedSaleOutId = saleOutId;
        // 销售领货已取消。旧五参调用仍兼容接收，但不得用 defer 或仓库历史开关跳过真实扣库。
        DataPermissionUtils.executeIgnore(
                () -> updateSaleOutStatus(generatedSaleOutId, ErpAuditStatus.APPROVE.getStatus(), false));
        recordCreate(generatedSaleOutId, saleOut.getNo());
        return generatedSaleOutId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOut(ErpSaleOutSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpSaleOutDO saleOut = validateSaleOutForUpdate(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
            throw exception(SALE_OUT_UPDATE_FAIL_APPROVE, saleOut.getNo());
        }
        preserveHiddenFields(updateReqVO, saleOut);
        List<ErpSaleOutItemDO> existingItems = saleOutItemMapper.selectListByOutIdForUpdate(updateReqVO.getId());
        preserveHiddenItemFields(updateReqVO, updateReqVO.getItems(), existingItems);
        boolean incrementalItems = ErpSaleItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpSaleOutSaveReqVO.Item::getOperation, SALE_OUT_ITEM_OPERATION_INVALID);
        ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleOutSaveReqVO.Item> itemChangeSet = null;
        List<ErpSaleOutSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpSaleItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), existingItems,
                    ErpSaleOutSaveReqVO.Item.class, ErpSaleOutSaveReqVO.Item::getId,
                    ErpSaleOutSaveReqVO.Item::setId, ErpSaleOutSaveReqVO.Item::getOperation,
                    ErpSaleOutItemDO::getId, SALE_OUT_ITEM_OPERATION_INVALID,
                    SALE_OUT_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        preserveItemSourceSnapshots(itemReqs, existingItems);
        // 1.2 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(updateReqVO.getOrderId());
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleOrder.getDeptId());
        customerService.validateCustomerForSale(saleOrder.getCustomerId(), saleOrder.getDeptId());
        // 1.3 校验结算账户
        accountService.validateAccount(updateReqVO.getAccountId());
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(itemReqs, updateReqVO.getOrderId());

        // 2.1 更新出库
        ErpSaleOutDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOutDO.class)
                .setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(updateObj, saleOutItems);
        saleOutMapper.updateById(updateObj);
        // 2.2 更新出库项
        if (incrementalItems) {
            applySaleOutItemChangeSet(updateReqVO.getId(), itemChangeSet, saleOutItems);
        } else {
            updateSaleOutItemList(updateReqVO.getId(), saleOutItems);
        }

        // 3.1 更新销售订单的出库数量
        if (updateObj.getOrderId() != null) {
            updateSaleOrderOutCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果销售订单编号变更了，需要更新“老”销售订单的出库数量
        if (ObjectUtil.notEqual(saleOut.getOrderId(), updateObj.getOrderId())) {
            if (saleOut.getOrderId() != null) {
                updateSaleOrderOutCount(saleOut.getOrderId());
            }
        }
        recordUpdate(updateReqVO.getId(), saleOut.getNo());
    }

    @Override
    public void updateSaleOutRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleOutDO saleOut = validateSaleOutExists(updateReqVO.getId());
        saleOutMapper.updateById(new ErpSaleOutDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), saleOut.getNo());
    }

    @Override
    public void updateSaleOutExpressFile(ErpSaleOutUpdateExpressFileReqVO updateReqVO) {
        ErpSaleOutDO saleOut = validateSaleOutExists(updateReqVO.getId());
        saleOutMapper.updateById(new ErpSaleOutDO()
                .setId(saleOut.getId())
                .setExpressFileUrl(updateReqVO.getExpressFileUrl()));
        recordUpdate(saleOut.getId(), saleOut.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadSaleOutExpressFile(Long id, byte[] content, String fileName) {
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        String extension = validateExpressFile(content, fileName);
        String contentType = "png".equals(extension) ? "image/png" : "image/jpeg";
        String fileUrl = fileApi.createFile(content, fileName,
                EXPRESS_FILE_DIRECTORY + "/" + id, contentType);
        saleOutMapper.updateById(new ErpSaleOutDO()
                .setId(saleOut.getId())
                .setExpressFileUrl(fileUrl));
        recordUpdate(saleOut.getId(), saleOut.getNo());
        return fileUrl;
    }

    private String validateExpressFile(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw exception(SALE_OUT_EXPRESS_FILE_EMPTY);
        }
        if (content.length > EXPRESS_FILE_MAX_SIZE) {
            throw exception(SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED);
        }
        String extension = FileUtil.extName(fileName);
        extension = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        if (!EXPRESS_FILE_EXTENSIONS.contains(extension)
                || !isExpressFileSignatureValid(content, extension)) {
            throw exception(SALE_OUT_EXPRESS_FILE_TYPE_INVALID);
        }
        return extension;
    }

    private boolean isExpressFileSignatureValid(byte[] content, String extension) {
        if ("png".equals(extension)) {
            if (content.length < PNG_SIGNATURE.length) {
                return false;
            }
            for (int i = 0; i < PNG_SIGNATURE.length; i++) {
                if (content[i] != PNG_SIGNATURE[i]) {
                    return false;
                }
            }
            return true;
        }
        return content.length >= 4
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[content.length - 2] == (byte) 0xFF
                && content[content.length - 1] == (byte) 0xD9;
    }

    private void calculateTotalPrice(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems) {
        saleOut.setTotalCount(getSumValue(saleOutItems, ErpSaleOutItemDO::getCount, BigDecimal::add));
        saleOut.setTotalProductPrice(getSumValue(saleOutItems, ErpSaleOutItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOut.setTotalTaxPrice(BigDecimal.ZERO);
        saleOut.setTotalPrice(saleOut.getTotalProductPrice());
        // 计算优惠价格
        if (saleOut.getDiscountPercent() == null) {
            saleOut.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(saleOut.getFeeAmount(), saleOut.getOtherPrice(), saleOut.getExtraFee());
        saleOut.setFeeAmount(feeAmount);
        saleOut.setOtherPrice(feeAmount);
        saleOut.setExtraFee(feeAmount);
        saleOut.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOut.getTotalPrice(), saleOut.getDiscountPercent()));
        saleOut.setTotalPrice(saleOut.getTotalPrice().subtract(saleOut.getDiscountPrice()).add(feeAmount));
        saleOut.setTotalWeight(getSumValue(saleOutItems, ErpSaleOutItemDO::getTotalWeight, BigDecimal::add, BigDecimal.ZERO));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice, BigDecimal extraFee) {
        if (feeAmount != null) {
            return feeAmount;
        }
        if (otherPrice != null) {
            return otherPrice;
        }
        return extraFee != null ? extraFee : BigDecimal.ZERO;
    }

    private void updateSaleOrderOutCount(Long orderId) {
        // 1.1 查询销售订单对应的销售出库单列表
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectListByOrderId(orderId);
        // 1.2 查询对应的销售订单项的出库数量
        Map<Long, BigDecimal> returnCountMap = saleOutItemMapper.selectOrderItemCountSumMapByOutIds(
                convertList(saleOuts, ErpSaleOutDO::getId));
        // 2. 更新销售订单的出库数量
        saleOrderService.updateSaleOrderOutCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOutStatus(Long id, Integer status) {
        updateSaleOutStatus(id, status, true);
    }

    private void updateSaleOutStatus(Long id, Integer status, boolean validateCurrentUserWarehousePermission) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(SALE_OUT_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpSaleOutDO saleOut = validateSaleOutForUpdate(id);
        // Validate status.
        if (!ErpAuditStatus.PROCESS.getStatus().equals(saleOut.getStatus())) {
            throw exception(SALE_OUT_APPROVE_FAIL);
        }
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleOut.getDeptId());

        // Update status.
        int updateCount = saleOutMapper.updateByIdAndStatus(id, saleOut.getStatus(),
                new ErpSaleOutDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_OUT_APPROVE_FAIL);
        }

        // 3. 变更库存
        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByOutIdForUpdate(id);
        List<Long> warehouseIds = convertList(saleOutItems, ErpSaleOutItemDO::getWarehouseId);
        if (validateCurrentUserWarehousePermission) {
            warehouseService.validSaleWarehouseList(warehouseIds);
        } else {
            warehouseService.validSaleSelectableWarehouseListForDept(
                    warehouseIds, resolveSaleDeptId(saleOut, saleOutItems),
                    ErpWarehouseService.SALE_OUT_ALL_PRODUCT_PERMISSION);
        }
        Integer bizType = ErpStockRecordBizTypeEnum.SALE_OUT.getType();

        ErpSaleOutDO approvedSource = saleOutMapper.selectByIdForUpdate(id);
        Map<Long, cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService.PreparedTradeContext> tradeContexts =
                tradeSnapshotService.prepareSaleOut(approvedSource, saleOutItems);
        // 同一父单下的多个库存维度采用稳定顺序；source锁先于库存锁。
        saleOutItems.sort(java.util.Comparator.comparing(ErpSaleOutItemDO::getProductId)
                .thenComparing(ErpSaleOutItemDO::getWarehouseId).thenComparing(ErpSaleOutItemDO::getId));

        // 凭证成本在生成页读取已完成业务的库存流水。
        // 3.2 Deduct stock.
        saleOutItems.forEach(saleOutItem -> {
            BigDecimal count = saleOutItem.getCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    saleOutItem.getProductId(), saleOutItem.getWarehouseId(), saleOutItem.getBatchNo(), count,
                    bizType, saleOutItem.getOutId(), saleOutItem.getId(), saleOut.getNo(),
                    saleOutItem.getProductPrice(), saleOut.getOutTime())
                    .setAccountingDeptId(saleOut.getDeptId()).setSourcePriceBasis("INCLUSIVE_UNCONFIRMED")
                    .setTradeContext(tradeContexts.get(saleOutItem.getId())));
        });
        if (ErpSaleBizSourceTypeEnum.CART.getType().equals(saleOut.getSourceType())
                && saleOut.getSourceId() != null) {
            stockLockService.deductStock(ErpSaleBizSourceTypeEnum.CART.getType(), saleOut.getSourceId());
        }

        // 4. 审批通过且已开账：生成销售凭证
        // 业务审核仅更新业务状态；凭证由财务统一预览生成。

        salePickDeliveryService.generateForSaleOut(id);
        recordStatus(id, saleOut.getNo(), true);
    }

    @Override
    public void updateSaleInReceiptPrice(Long id, BigDecimal receiptPrice) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        if (saleOut.getReceiptPrice().equals(receiptPrice)) {
            return;
        }
        BigDecimal settlementTotalPrice = ErpOriginalSettlementAmountUtils.calculateSaleOut(
                saleOut, saleOutItemMapper.selectListByOutId(id));
        if (receiptPrice.compareTo(settlementTotalPrice) > 0) {
            throw exception(SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED, receiptPrice, settlementTotalPrice);
        }
        saleOutMapper.updateById(new ErpSaleOutDO().setId(id).setReceiptPrice(receiptPrice));
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list) {
        return validateSaleOutItems(list, null);
    }

    private ErpStockDO getStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list, Long orderId) {
        return validateSaleOutItems(list, orderId, null);
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list, Long orderId,
                                                        Long saleDeptId) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleOutSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleOutSaveReqVO.Item::getProductId, ErpSaleOutSaveReqVO.Item::getBatchNo);
        List<Long> warehouseIds = convertList(list, ErpSaleOutSaveReqVO.Item::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                saleDeptId != null
                        ? warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, saleDeptId,
                                ErpWarehouseService.SALE_OUT_ALL_PRODUCT_PERMISSION)
                        : warehouseService.validSaleWarehouseList(warehouseIds),
                ErpWarehouseDO::getId);
        Map<Long, ErpSaleOrderItemDO> orderItemMap = buildOrderItemMap(orderId);
        // 2. 转化成 ErpSaleOutItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleOutItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillDeptIdFromWarehouse(item, warehouseMap, saleDeptId);
            validateSaleDeptWarehousePermission(item, saleDeptId);
            fillBatchNoFromOrderItem(item, orderItemMap);
            fillWeightAndPackageFromOrderItem(item, orderItemMap);
            fillProductWeightAndPackage(item, product);
            item.setGiftFlag(resolveGiftFlag(o.getGiftFlag(), item.getOrderItemId(), orderItemMap));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
                item.setProductPrice(BigDecimal.ZERO);
                item.setTotalPrice(BigDecimal.ZERO);
                item.setTaxPercent(null);
                item.setTaxPrice(BigDecimal.ZERO);
                item.setTotalWeight(MoneyUtils.priceMultiply(item.getUnitWeight(), item.getCount()));
                return;
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            item.setTotalWeight(MoneyUtils.priceMultiply(item.getUnitWeight(), item.getCount()));
        }));
    }

    private void fillProductWeightAndPackage(ErpSaleOutItemDO item, ErpProductDO product) {
        if (product == null) {
            return;
        }
        if (item.getUnitWeight() == null) {
            item.setUnitWeight(product.getWeight());
        }
        if (item.getPackageQty() == null) {
            item.setPackageQty(product.getPackageQty());
        }
    }

    private Long resolveSaleDeptId(ErpSaleOutSaveReqVO reqVO) {
        if (reqVO.getDeptId() != null) {
            return reqVO.getDeptId();
        }
        if (CollUtil.isEmpty(reqVO.getItems())) {
            return null;
        }
        return reqVO.getItems().stream()
                .map(ErpSaleOutSaveReqVO.Item::getDeptId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Long resolveSaleDeptId(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        if (saleOut.getDeptId() != null) {
            return saleOut.getDeptId();
        }
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        return items.stream()
                .map(ErpSaleOutItemDO::getDeptId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void fillDeptIdFromWarehouse(ErpSaleOutItemDO item, Map<Long, ErpWarehouseDO> warehouseMap, Long saleDeptId) {
        if (item.getWarehouseId() == null) {
            return;
        }
        ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
        if (warehouse != null && !warehouseService.isWarehouseSaleAllowedForDept(warehouse.getId(), saleDeptId)) {
            item.setDeptId(warehouse.getDeptId());
            return;
        }
        if (item.getDeptId() == null) {
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private void validateSaleDeptWarehousePermission(ErpSaleOutItemDO item, Long saleDeptId) {
        if (item.getDeptId() == null) {
            throw exception(SALE_WAREHOUSE_DEPT_REQUIRED);
        }
        warehouseService.validateWarehouseSaleSelectableForDept(item.getWarehouseId(),
                saleDeptId != null ? saleDeptId : item.getDeptId(),
                ErpWarehouseService.SALE_OUT_ALL_PRODUCT_PERMISSION);
    }

    private Map<Long, ErpSaleOrderItemDO> buildOrderItemMap(Long orderId) {
        if (orderId == null) {
            return Collections.emptyMap();
        }
        List<ErpSaleOrderItemDO> orderItems = saleOrderService.getSaleOrderItemListByOrderId(orderId);
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptyMap();
        }
        return orderItems.stream().collect(Collectors.toMap(ErpSaleOrderItemDO::getId,
                item -> item, (oldValue, newValue) -> oldValue));
    }

    private void fillBatchNoFromOrderItem(ErpSaleOutItemDO item, Map<Long, ErpSaleOrderItemDO> orderItemMap) {
        if (item.getBatchNo() != null || item.getOrderItemId() == null) {
            return;
        }
        ErpSaleOrderItemDO orderItem = orderItemMap.get(item.getOrderItemId());
        if (orderItem != null) {
            item.setBatchNo(orderItem.getBatchNo());
        }
    }

    private void fillWeightAndPackageFromOrderItem(ErpSaleOutItemDO item, Map<Long, ErpSaleOrderItemDO> orderItemMap) {
        if (item.getOrderItemId() == null) {
            return;
        }
        ErpSaleOrderItemDO orderItem = orderItemMap.get(item.getOrderItemId());
        if (orderItem == null) {
            return;
        }
        if (item.getUnitWeight() == null) {
            item.setUnitWeight(orderItem.getWeight());
        }
        if (item.getPackageQty() == null) {
            item.setPackageQty(orderItem.getPackageQty());
        }
    }

    private Boolean resolveGiftFlag(Boolean reqGiftFlag, Long orderItemId, Map<Long, ErpSaleOrderItemDO> orderItemMap) {
        if (reqGiftFlag != null) {
            return Boolean.TRUE.equals(reqGiftFlag);
        }
        ErpSaleOrderItemDO orderItem = orderItemId == null ? null : orderItemMap.get(orderItemId);
        if (orderItem != null) {
            return Boolean.TRUE.equals(orderItem.getGiftFlag());
        }
        return Boolean.FALSE;
    }

    /** 普通新增不允许客户端伪造跨部门来源，来源快照仅由单据转换链路写入。 */
    private void clearItemSourceSnapshots(List<ErpSaleOutSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setSourceWarehouseId(null).setSourceDeptId(null));
    }

    /** 编辑时来源快照始终以数据库原值为准，避免请求篡改或丢失。 */
    private void preserveItemSourceSnapshots(List<ErpSaleOutSaveReqVO.Item> items,
                                             List<ErpSaleOutItemDO> existingItems) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ErpSaleOutItemDO> existingItemMap = CollUtil.isEmpty(existingItems)
                ? Collections.emptyMap() : convertMap(existingItems, ErpSaleOutItemDO::getId);
        items.forEach(item -> {
            ErpSaleOutItemDO existingItem = existingItemMap.get(item.getId());
            item.setSourceWarehouseId(existingItem == null ? null : existingItem.getSourceWarehouseId());
            item.setSourceDeptId(existingItem == null ? null : existingItem.getSourceDeptId());
        });
    }

    private void updateSaleOutItemList(Long id, List<ErpSaleOutItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOutItemDO> oldList = saleOutItemMapper.selectListByOutId(id);
        List<List<ErpSaleOutItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOutId(id));
            saleOutItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleOutItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOutItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOutItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOut(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOutDO> saleOuts = new java.util.ArrayList<>();
        if (ids != null) {
            ids.stream().filter(java.util.Objects::nonNull).distinct().sorted().forEach(id -> {
                ErpSaleOutDO source = saleOutMapper.selectByIdForUpdate(id);
                if (source != null) saleOuts.add(source);
            });
        }
        if (CollUtil.isEmpty(saleOuts)) {
            return;
        }
        saleOuts.forEach(saleOut -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
                throw exception(SALE_OUT_DELETE_FAIL_APPROVE, saleOut.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOuts.forEach(saleOut -> {
            // 2.1 删除订单
            saleOutMapper.deleteById(saleOut.getId());
            // 2.2 删除订单项
            saleOutItemMapper.deleteByOutId(saleOut.getId());

            // 2.3 更新销售订单的出库数量
            if (saleOut.getOrderId() != null) {
                updateSaleOrderOutCount(saleOut.getOrderId());
            }
            recordDelete(saleOut.getId(), saleOut.getNo());
        });

    }

    private ErpSaleOutDO validateSaleOutExists(Long id) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw exception(SALE_OUT_NOT_EXISTS);
        }
        return saleOut;
    }

    private ErpSaleOutDO validateSaleOutForUpdate(Long id) {
        ErpSaleOutDO source = saleOutMapper.selectByIdForUpdate(id);
        if (source == null) throw exception(SALE_OUT_NOT_EXISTS);
        return source;
    }

    @Override
    public ErpSaleOutDO getSaleOut(Long id) {
        return saleOutMapper.selectById(id);
    }

    @Override
    public ErpSaleOutDO validateSaleOut(Long id) {
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        if (ObjectUtil.notEqual(saleOut.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_OUT_NOT_APPROVE);
        }
        return saleOut;
    }

    @Override
    public PageResult<ErpSaleOutDO> getSaleOutPage(ErpSaleOutPageReqVO pageReqVO) {
        PageResult<ErpSaleOutDO> pageResult = saleOutMapper.selectPage(pageReqVO);
        Set<Long> saleOutIds = convertSet(pageResult.getList(), ErpSaleOutDO::getId);
        Map<Long, BigDecimal> receiptPriceMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                saleOutIds, ErpBizTypeEnum.SALE_OUT.getType());
        pageResult.getList().forEach(item -> item.setReceiptPrice(
                receiptPriceMap.getOrDefault(item.getId(), BigDecimal.ZERO)));
        if (Boolean.TRUE.equals(pageReqVO.getReceiptEnable()) && CollUtil.isNotEmpty(saleOutIds)) {
            Map<Long, List<ErpSaleOutItemDO>> itemMap = convertMultiMap(
                    saleOutItemMapper.selectListByOutIds(saleOutIds), ErpSaleOutItemDO::getOutId);
            pageResult.getList().forEach(item -> item.setTotalPrice(
                    ErpOriginalSettlementAmountUtils.calculateSaleOut(
                            item, itemMap.getOrDefault(item.getId(), Collections.emptyList()))));
        }
        return pageResult;
    }

    // ==================== 销售出库项 ====================

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutId(Long outId) {
        return saleOutItemMapper.selectListByOutId(outId);
    }

    @Override
    public PageResult<ErpSaleOutItemDO> getSaleOutItemPage(ErpSaleOutItemPageReqVO pageReqVO) {
        validateSaleOutExists(pageReqVO.getOutId());
        return saleOutItemMapper.selectPageByOutId(pageReqVO);
    }

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyList();
        }
        return saleOutItemMapper.selectListByOutIds(outIds);
    }

    @Override
    public List<ErpSaleReturnableItemRespVO> getReturnableItemsByOutId(Long outId) {
        ErpSaleOutDO saleOut = validateSaleOut(outId);
        List<ErpSaleOutItemDO> items = saleOutItemMapper.selectListByOutId(outId);
        return buildSaleReturnableItemVOList(saleOut, items);
    }

    private void applySaleOutItemChangeSet(Long outId,
            ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleOutSaveReqVO.Item> changeSet,
            List<ErpSaleOutItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            saleOutItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpSaleOutItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setOutId(outId));
            saleOutItemMapper.insertBatch(insertList);
        }
        List<ErpSaleOutItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setOutId(outId));
            saleOutItemMapper.updateBatch(updateList);
        }
    }

    @Override
    public PageResult<ErpSaleReturnableItemRespVO> getReturnableItemPage(ErpSaleOutReturnableItemPageReqVO pageReqVO) {
        ErpSaleOutDO saleOut = validateSaleOut(pageReqVO.getOutId());
        PageResult<ErpSaleOutItemDO> pageResult = saleOutItemMapper.selectReturnableItemPage(pageReqVO);
        return new PageResult<>(buildSaleReturnableItemVOList(saleOut, pageResult.getList()), pageResult.getTotal());
    }

    private List<ErpSaleReturnableItemRespVO> buildSaleReturnableItemVOList(
            ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Map<Long, BigDecimal> returnedMap;
        if (saleReturnMapper == null) {
            returnedMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(
                    convertList(items, ErpSaleOutItemDO::getId));
        } else {
            List<ErpSaleReturnDO> approvedReturns = saleReturnMapper.selectListBySourceOutId(saleOut.getId());
            approvedReturns.removeIf(saleReturn -> !ErpAuditStatus.APPROVE.getStatus().equals(saleReturn.getStatus()));
            returnedMap = CollUtil.isEmpty(approvedReturns) ? Collections.emptyMap()
                    : saleReturnItemMapper.selectSourceOutItemCountSumMapByReturnIds(
                            convertList(approvedReturns, ErpSaleReturnDO::getId));
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(
                () -> productService.getProductVOMap(convertSet(items, ErpSaleOutItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(
                () -> warehouseService.getWarehouseMap(convertSet(items, ErpSaleOutItemDO::getWarehouseId)));
        List<Long> deptIds = convertList(items, ErpSaleOutItemDO::getDeptId);
        deptIds.addAll(convertList(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return items.stream().map(item -> {
            ErpSaleReturnableItemRespVO vo = new ErpSaleReturnableItemRespVO();
            vo.setSourceOutId(saleOut.getId());
            vo.setSourceOutItemId(item.getId());
            vo.setSourceOutNo(saleOut.getNo());
            vo.setCustomerId(saleOut.getCustomerId());
            vo.setProductId(item.getProductId());
            MapUtils.findAndThen(productMap, item.getProductId(), product -> {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductBarCode(product.getBarCode());
                vo.setProductUnitName(product.getUnitName());
            });
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWeight(item.getUnitWeight());
            vo.setPackageQty(item.getPackageQty());
            vo.setWarehouseId(item.getWarehouseId());
            MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                vo.setWarehouseName(warehouse.getName());
                vo.setWarehouseDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                        dept -> vo.setWarehouseDeptName(dept.getName()));
            });
            vo.setDeptId(item.getDeptId());
            vo.setBatchNo(item.getBatchNo());
            MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            vo.setProductPrice(item.getProductPrice());
            vo.setTaxPercent(item.getTaxPercent());
            vo.setOutCount(item.getCount());
            BigDecimal returned = returnedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            vo.setReturnedCount(returned);
            BigDecimal returnable = item.getCount().subtract(returned);
            vo.setReturnableCount(returnable.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : returnable);

            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void clearHiddenItemFields(Object context, List<?> items) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context, items);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private void preserveHiddenItemFields(Object context, List<?> items, List<?> existingItems) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context, items, existingItems);
        }
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_SALE_OUT_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_OUT_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_OUT_TYPE, id, no, approve);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_OUT_TYPE, id, no);
        }
    }

}
