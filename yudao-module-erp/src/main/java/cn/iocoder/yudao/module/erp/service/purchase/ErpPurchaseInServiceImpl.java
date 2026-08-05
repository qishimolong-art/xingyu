package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

// TODO 芋艿：记录操作日�?

/**
 * ERP 采购入库 Service 实现�?
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseInServiceImpl implements ErpPurchaseInService {

    private static final int DRAFT_STATUS = 0;
    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_in";

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依�?
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockInBillService stockInBillService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    @Lazy // 延迟加载，避免循环依�?
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseIn(ErpPurchaseInSaveReqVO createReqVO) {
        // 1.1 校验采购订单已审核（如果填写�?orderId�?
        ErpPurchaseOrderDO purchaseOrder = null;
        if (createReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(createReqVO.getOrderId());
        } else {
            supplierService.validateSupplier(createReqVO.getSupplierId());
        }
        // 1.2 校验入库项的有效�?
        List<ErpPurchaseInItemDO> purchaseInItems = validatePurchaseInItems(createReqVO.getItems());
        // 1.3 生成入库单号，并校验唯一�?
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_IN_NO_PREFIX);
        if (purchaseInMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_IN_NO_EXISTS);
        }

        // 2.1 插入入库
        ErpPurchaseInDO purchaseIn = BeanUtils.toBean(createReqVO, ErpPurchaseInDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        purchaseIn.setInTime(createReqVO.getInTime() != null ? createReqVO.getInTime() : LocalDateTime.now());
        if (purchaseOrder != null) {
            purchaseIn.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                    .setDeptId(purchaseOrder.getDeptId());
        } else {
            purchaseIn.setSupplierId(createReqVO.getSupplierId()).setOrderNo(StrUtil.EMPTY);
        }
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseIn);
        // （当 orderId 为空时，supplierId �?createReqVO 传进来；若未传，则在主表 supplierId �?null，业务允许）
        calculateTotalPrice(purchaseIn, purchaseInItems);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseIn);
        purchaseInMapper.insert(purchaseIn);
        // 2.2 插入入库项
        purchaseInItems.forEach(o -> o.setInId(purchaseIn.getId()));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseInItems);
        purchaseInItemMapper.insertBatch(purchaseInItems);

        // 3. 仅在�?orderId 时更新采购订单的入库数量
        if (createReqVO.getOrderId() != null) {
            updatePurchaseOrderInCount(createReqVO.getOrderId());
        }
        operateLogService.recordCreate(ERP_PURCHASE_IN_TYPE, purchaseIn.getId(), purchaseIn.getNo());
        return purchaseIn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInDraft(ErpPurchaseInDraftCreateReqVO createReqVO) {
        validateOptionalDraftReferences(createReqVO);
        List<ErpPurchaseInItemDO> purchaseInItems = buildDraftPurchaseInItems(createReqVO.getItems());
        if (CollUtil.isEmpty(purchaseInItems)) {
            throw exception(PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_IN_NO_PREFIX);
        if (purchaseInMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_IN_NO_EXISTS);
        }
        ErpPurchaseInDO purchaseIn = BeanUtils.toBean(createReqVO, ErpPurchaseInDO.class,
                in -> in.setNo(no).setStatus(DRAFT_STATUS));
        purchaseIn.setInTime(createReqVO.getInTime() != null ? createReqVO.getInTime() : LocalDateTime.now());
        fillDraftSource(createReqVO, purchaseIn);
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseIn);
        calculateTotalPrice(purchaseIn, purchaseInItems);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseIn);
        purchaseInMapper.insert(purchaseIn);
        replacePurchaseInItems(purchaseIn.getId(), purchaseInItems);
        operateLogService.recordCreate(ERP_PURCHASE_IN_TYPE, purchaseIn.getId(), purchaseIn.getNo());
        return purchaseIn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseIn(ErpPurchaseInSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_UPDATE_FAIL_APPROVE, purchaseIn.getNo());
        }
        // 1.2 校验采购订单已审核（如果填写�?orderId�?
        ErpPurchaseOrderDO purchaseOrder = null;
        if (updateReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(updateReqVO.getOrderId());
        } else {
            supplierService.validateSupplier(updateReqVO.getSupplierId());
        }
        // 1.3 校验订单项的有效�?
        List<ErpPurchaseInItemDO> purchaseInItems = validatePurchaseInItems(updateReqVO.getItems());

        // 2.1 更新入库
        ErpPurchaseInDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInDO.class);
        updateObj.setStatus(null);
        updateObj.setInTime(updateReqVO.getInTime() != null ? updateReqVO.getInTime() : LocalDateTime.now());
        if (purchaseOrder != null) {
            updateObj.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                    .setDeptId(purchaseOrder.getDeptId());
        } else {
            updateObj.setSupplierId(updateReqVO.getSupplierId()).setOrderNo(StrUtil.EMPTY);
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseIn.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(updateObj);
        calculateTotalPrice(updateObj, purchaseInItems);
        purchaseInMapper.updateById(updateObj);
        // 2.2 更新入库�?
        updatePurchaseInItemList(updateReqVO.getId(), purchaseInItems);

        // 3.1 更新采购订单的入库数量（�?orderId 非空时）
        if (updateObj.getOrderId() != null) {
            updatePurchaseOrderInCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果采购订单编号变更了，需要更新“老”采购订单的入库数量
        if (ObjectUtil.notEqual(purchaseIn.getOrderId(), updateObj.getOrderId())) {
            if (purchaseIn.getOrderId() != null) {
                updatePurchaseOrderInCount(purchaseIn.getOrderId());
            }
        }
        operateLogService.recordUpdate(ERP_PURCHASE_IN_TYPE, updateReqVO.getId(), purchaseIn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInDraft(ErpPurchaseInDraftUpdateReqVO updateReqVO) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(updateReqVO.getId());
        if (!Integer.valueOf(DRAFT_STATUS).equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_UPDATE_FAIL_NOT_DRAFT, purchaseIn.getNo());
        }
        List<ErpPurchaseInItemDO> oldItems = purchaseInItemMapper.selectListByInId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, purchaseIn);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        validateOptionalDraftReferences(updateReqVO);
        List<ErpPurchaseInItemDO> purchaseInItems = buildDraftPurchaseInItems(updateReqVO.getItems());
        ErpPurchaseInDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInDO.class);
        updateObj.setStatus(null).setNo(null);
        updateObj.setInTime(updateReqVO.getInTime() != null ? updateReqVO.getInTime() : purchaseIn.getInTime());
        fillDraftSource(updateReqVO, updateObj);
        if (updateReqVO.getOrderId() == null && purchaseIn.getOrderId() != null) {
            updateObj.setOrderId(purchaseIn.getOrderId()).setOrderNo(purchaseIn.getOrderNo())
                    .setSupplierId(purchaseIn.getSupplierId());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseIn.getDeptId());
        }
        calculateTotalPrice(updateObj, purchaseInItems);
        purchaseInMapper.updateById(updateObj);
        replacePurchaseInItems(updateReqVO.getId(), purchaseInItems);
        operateLogService.recordUpdate(ERP_PURCHASE_IN_TYPE, updateReqVO.getId(), purchaseIn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPurchaseInDraft(ErpPurchaseInDraftUpdateReqVO updateReqVO) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(updateReqVO.getId());
        if (!Integer.valueOf(DRAFT_STATUS).equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_UPDATE_FAIL_NOT_DRAFT, purchaseIn.getNo());
        }
        updatePurchaseInDraft(updateReqVO);
        submitPurchaseIn(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchaseIn(Long id) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(id);
        if (!Integer.valueOf(DRAFT_STATUS).equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_SUBMIT_FAIL);
        }
        if (purchaseIn.getSupplierId() == null) {
            throw exception(PURCHASE_IN_SUBMIT_SUPPLIER_REQUIRED);
        }
        if (purchaseIn.getInTime() == null) {
            throw exception(PURCHASE_IN_SUBMIT_TIME_REQUIRED);
        }
        List<ErpPurchaseInItemDO> purchaseInItems = purchaseInItemMapper.selectListByInId(id);
        if (CollUtil.isEmpty(purchaseInItems)) {
            throw exception(PURCHASE_IN_SUBMIT_ITEMS_REQUIRED);
        }
        if (purchaseIn.getOrderId() != null) {
            purchaseOrderService.validatePurchaseOrder(purchaseIn.getOrderId());
        } else {
            supplierService.validateSupplier(purchaseIn.getSupplierId());
        }
        validatePurchaseInItems(BeanUtils.toBean(purchaseInItems, ErpPurchaseInSaveReqVO.Item.class));
        int updateCount = purchaseInMapper.updateByIdAndStatus(id, DRAFT_STATUS,
                new ErpPurchaseInDO().setStatus(ErpAuditStatus.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_IN_SUBMIT_FAIL);
        }
        if (purchaseIn.getOrderId() != null) {
            updatePurchaseOrderInCount(purchaseIn.getOrderId());
        }
        operateLogService.recordUpdate(ERP_PURCHASE_IN_TYPE, id, purchaseIn.getNo());
    }

    private void validateOptionalDraftReferences(ErpPurchaseInSaveReqVO reqVO) {
        if (reqVO.getOrderId() != null) {
            purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        } else if (reqVO.getSupplierId() != null) {
            supplierService.validateSupplier(reqVO.getSupplierId());
        }
    }

    private void fillDraftSource(ErpPurchaseInSaveReqVO reqVO, ErpPurchaseInDO target) {
        if (reqVO.getOrderId() == null) {
            target.setOrderNo(StrUtil.EMPTY);
            return;
        }
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        target.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                .setDeptId(purchaseOrder.getDeptId());
    }

    private List<ErpPurchaseInItemDO> buildDraftPurchaseInItems(List<ErpPurchaseInSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpPurchaseInSaveReqVO.Item> validItems = items.stream()
                .filter(item -> item.getProductId() != null && item.getWarehouseId() != null
                        && item.getCount() != null && item.getCount().compareTo(BigDecimal.ZERO) > 0
                        && item.getProductPrice() != null && item.getProductPrice().compareTo(BigDecimal.ZERO) >= 0)
                .collect(Collectors.toList());
        return validItems.isEmpty() ? Collections.emptyList() : validatePurchaseInItems(validItems, true);
    }

    private void replacePurchaseInItems(Long inId, List<ErpPurchaseInItemDO> items) {
        purchaseInItemMapper.deleteByInId(inId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setInId(inId));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        purchaseInItemMapper.insertBatch(items);
    }

    @Override
    public void updatePurchaseInRemark(ErpPurchaseUpdateRemarkReqVO updateReqVO) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(updateReqVO.getId());
        purchaseInMapper.updateById(new ErpPurchaseInDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PURCHASE_IN_TYPE, updateReqVO.getId(), purchaseIn.getNo());
    }

    private void calculateTotalPrice(ErpPurchaseInDO purchaseIn, List<ErpPurchaseInItemDO> purchaseInItems) {
        purchaseIn.setTotalCount(getSumValue(purchaseInItems, ErpPurchaseInItemDO::getCount,
                BigDecimal::add, BigDecimal.ZERO));
        purchaseIn.setTotalProductPrice(getSumValue(purchaseInItems, ErpPurchaseInItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseIn.setTotalTaxPrice(BigDecimal.ZERO);
        purchaseIn.setTotalPrice(purchaseIn.getTotalProductPrice());
        // 计算优惠价格
        if (purchaseIn.getDiscountPercent() == null) {
            purchaseIn.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(purchaseIn.getFeeAmount(), purchaseIn.getOtherPrice());
        purchaseIn.setFeeAmount(feeAmount);
        purchaseIn.setOtherPrice(feeAmount);
        purchaseIn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseIn.getTotalPrice(), purchaseIn.getDiscountPercent()));
        purchaseIn.setTotalPrice(purchaseIn.getTotalPrice().subtract(purchaseIn.getDiscountPrice()).add(feeAmount));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : (otherPrice != null ? otherPrice : BigDecimal.ZERO);
    }

    private void updatePurchaseOrderInCount(Long orderId) {
        Map<Long, BigDecimal> returnCountMap = getPurchaseOrderGeneratedInCountMap(orderId);
        // 2. 更新采购订单的入库数�?
        purchaseOrderService.updatePurchaseOrderInCount(orderId, returnCountMap);
    }

    private Map<Long, BigDecimal> getPurchaseOrderGeneratedInCountMap(Long orderId) {
        // 已生成的采购入库单都会占用采购订单可入库数量，审批状态不影响占用。
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectListByOrderId(orderId);
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyMap();
        }
        purchaseIns = purchaseIns.stream()
                .filter(purchaseIn -> !Integer.valueOf(DRAFT_STATUS).equals(purchaseIn.getStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyMap();
        }
        return purchaseInItemMapper.selectOrderItemCountSumMapByInIds(
                convertList(purchaseIns, ErpPurchaseInDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(PURCHASE_IN_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(id);
        // Validate status.
        if (!ErpAuditStatus.PROCESS.getStatus().equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_APPROVE_FAIL);
        }
        List<ErpPurchaseInItemDO> purchaseInItems = purchaseInItemMapper.selectListByInId(id);
        validatePurchaseInItemsForApproval(purchaseInItems);

        // Update status.
        int updateCount = purchaseInMapper.updateByIdAndStatus(id, purchaseIn.getStatus(),
                new ErpPurchaseInDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_IN_APPROVE_FAIL);
        }

        // 3. 变更库存
        warehouseService.validPurchaseWarehouseList(convertSet(purchaseInItems, ErpPurchaseInItemDO::getWarehouseId));
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(purchaseInItems, ErpPurchaseInItemDO::getWarehouseId));
        if (warehouseMap == null) {
            warehouseMap = Collections.emptyMap();
        }
        Map<Long, ErpWarehouseDO> finalWarehouseMap = warehouseMap;
        List<ErpPurchaseInItemDO> stockInBillItems = purchaseInItems.stream()
                .filter(item -> isStockBillEnabled(finalWarehouseMap, item.getWarehouseId()))
                .collect(Collectors.toList());
        List<ErpPurchaseInItemDO> directStockItems = purchaseInItems.stream()
                .filter(item -> !isStockBillEnabled(finalWarehouseMap, item.getWarehouseId()))
                .collect(Collectors.toList());
        directStockItems.forEach(purchaseInItem -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    purchaseInItem.getProductId(), purchaseInItem.getWarehouseId(), purchaseInItem.getBatchNo(), purchaseInItem.getCount(),
                    ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(), purchaseInItem.getInId(), purchaseInItem.getId(), purchaseIn.getNo(),
                    purchaseInItem.getProductPrice(), purchaseIn.getInTime()));
        });
        stockInBillService.createFromPurchaseIn(purchaseIn, stockInBillItems);

        // 4. 仅在审批通过时，回写每个产品的最近采购价 last_purchase_price
        purchaseInItems.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getGift()))
                .forEach(item -> productService.updateProductLastPurchasePrice(
                        item.getProductId(), item.getProductPrice()));

        // 5. 审批通过：自动生成采购凭证（仅在该月份已开账且启用采购凭证时触发）
        if (bookOpenService.isVoucherTypeEnabled(
                purchaseIn.getInTime().toLocalDate(),
                ErpVoucherTypeEnum.PURCHASE.getType())) {
            ErpSupplierDO supplier = supplierService.validateSupplier(purchaseIn.getSupplierId());
            String supplierName = supplier.getName();
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildPurchaseInItems(purchaseIn, supplierName);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType(),
                    purchaseIn.getId(),
                    purchaseIn.getNo(),
                    purchaseIn.getTotalPrice(),
                    purchaseIn.getInTime().toLocalDate(),
                    "采购入库 - " + supplierName,
                    voucherItems);
        }
        if (purchaseIn.getOrderId() != null) {
            updatePurchaseOrderInCount(purchaseIn.getOrderId());
        }
        operateLogService.recordStatus(ERP_PURCHASE_IN_TYPE, id, purchaseIn.getNo(), true);
    }

    private boolean isStockBillEnabled(Map<Long, ErpWarehouseDO> warehouseMap, Long warehouseId) {
        ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
        return warehouse != null && Boolean.TRUE.equals(warehouse.getStockBillEnabled());
    }

    @Override
    public void updatePurchaseInPaymentPrice(Long id, BigDecimal paymentPrice) {
        ErpPurchaseInDO purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn.getPaymentPrice().equals(paymentPrice)) {
            return;
        }
        BigDecimal settlementTotalPrice = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(
                purchaseIn, purchaseInItemMapper.selectListByInId(id));
        if (paymentPrice.compareTo(settlementTotalPrice) > 0) {
            throw exception(PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED, paymentPrice, settlementTotalPrice);
        }
        purchaseInMapper.updateById(new ErpPurchaseInDO().setId(id).setPaymentPrice(paymentPrice));
    }

    private List<ErpPurchaseInItemDO> validatePurchaseInItems(List<ErpPurchaseInSaveReqVO.Item> list) {
        return validatePurchaseInItems(list, false);
    }

    private List<ErpPurchaseInItemDO> validatePurchaseInItems(List<ErpPurchaseInSaveReqVO.Item> list,
                                                               boolean allowZeroNonGiftPrice) {
        // 0. 校验每项的入库数量和入库价格必须大于 0（赠品行单价允许 0�?
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseInSaveReqVO.Item item : list) {
                if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_IN_ITEM_COUNT_POSITIVE);
                }
                // 校验单价：单价为 null �?<0 直接报错�?0 仅允许赠品（orderItemId 关联的订单行是赠品由服务端预置为 0，这里只拦截负�?空值）
                if (!Boolean.TRUE.equals(item.getGift())
                        && (item.getProductPrice() == null
                        || item.getProductPrice().compareTo(BigDecimal.ZERO) < (allowZeroNonGiftPrice ? 0 : 1))) {
                    throw exception(PURCHASE_IN_ITEM_PRICE_POSITIVE);
                }
            }
        }
        Set<String> itemKeySet = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseInSaveReqVO.Item item : list) {
                String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
                String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|"
                        + (Boolean.TRUE.equals(item.getGift()) ? 1 : 0);
                if (!itemKeySet.add(itemKey)) {
                    throw exception(PURCHASE_IN_ITEM_DUPLICATE, productKey);
                }
            }
        }
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseInSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpPurchaseInSaveReqVO.Item::getProductId, ErpPurchaseInSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpPurchaseInSaveReqVO.Item::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        // 2. 转化�?ErpPurchaseInItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseInItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillDeptIdFromWarehouse(item, warehouseMap);

            // 包装数：优先�?VO 传来的；为空或非正数 �?用产品资�?packageQty；再为空 �?默认 1
            Integer packageQty = item.getPackageQty();
            if (packageQty == null || packageQty <= 0) {
                packageQty = product.getPackageQty() != null && product.getPackageQty() > 0
                        ? product.getPackageQty() : 1;
                item.setPackageQty(packageQty);
            }

            // 整件数有�?�?count = wholeQty × packageQty，覆�?VO 传的 count
            if (item.getWholeQty() != null) {
                item.setCount(new BigDecimal(item.getWholeQty()).multiply(new BigDecimal(packageQty)));
            }

            item.setGift(Boolean.TRUE.equals(item.getGift()));
            if (Boolean.TRUE.equals(item.getGift())) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
        }));
    }

    private void validatePurchaseInItemsForApproval(List<ErpPurchaseInItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (ErpPurchaseInItemDO item : items) {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_IN_ITEM_COUNT_POSITIVE);
            }
            if (Boolean.TRUE.equals(item.getGift())) {
                if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) != 0) {
                    throw exception(PURCHASE_IN_ITEM_PRICE_POSITIVE);
                }
            } else if (item.getProductPrice() == null
                    || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_IN_ITEM_PRICE_POSITIVE);
            }
        }
    }

    private void fillDeptIdFromWarehouse(ErpPurchaseInItemDO item, Map<Long, ErpWarehouseDO> warehouseMap) {
        if (item.getDeptId() == null && item.getWarehouseId() != null) {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private void updatePurchaseInItemList(Long id, List<ErpPurchaseInItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseInItemDO> oldList = purchaseInItemMapper.selectListByInId(id);
        List<List<ErpPurchaseInItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // Batch insert, update and delete items.
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setInId(id));
            purchaseDocumentDefaultService.fillCreateAuditDefaults(diffList.get(0));
            purchaseInItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseInItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseInItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseInItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseIn(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectByIds(ids);
        if (CollUtil.isEmpty(purchaseIns)) {
            return;
        }
        purchaseIns.forEach(purchaseIn -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
                throw exception(PURCHASE_IN_DELETE_FAIL_APPROVE, purchaseIn.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        purchaseIns.forEach(purchaseIn -> {
            // 2.1 删除订单
            purchaseInMapper.deleteById(purchaseIn.getId());
            // 2.2 删除订单�?
            purchaseInItemMapper.deleteByInId(purchaseIn.getId());

            // 2.3 更新采购订单的入库数�?
            if (purchaseIn.getOrderId() != null) {
                updatePurchaseOrderInCount(purchaseIn.getOrderId());
            }
            operateLogService.recordDelete(ERP_PURCHASE_IN_TYPE, purchaseIn.getId(), purchaseIn.getNo());
        });

    }

    private ErpPurchaseInDO validatePurchaseInExists(Long id) {
        ErpPurchaseInDO purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn == null) {
            throw exception(PURCHASE_IN_NOT_EXISTS);
        }
        return purchaseIn;
    }

    @Override
    public ErpPurchaseInDO getPurchaseIn(Long id) {
        return purchaseInMapper.selectById(id);
    }

    @Override
    public ErpPurchaseInDO validatePurchaseIn(Long id) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(id);
        if (ObjectUtil.notEqual(purchaseIn.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_IN_NOT_APPROVE);
        }
        return purchaseIn;
    }

    @Override
    public PageResult<ErpPurchaseInDO> getPurchaseInPage(ErpPurchaseInPageReqVO pageReqVO) {
        PageResult<ErpPurchaseInDO> pageResult = purchaseInMapper.selectPage(pageReqVO);
        Map<Long, BigDecimal> paymentPriceMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getId), ErpBizTypeEnum.PURCHASE_IN.getType());
        pageResult.getList().forEach(item -> item.setPaymentPrice(
                paymentPriceMap.getOrDefault(item.getId(), BigDecimal.ZERO)));
        return pageResult;
    }

    @Override
    public List<ErpPurchaseInDO> getPurchaseInList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return purchaseInMapper.selectByIds(ids);
    }

    // ==================== 采购入库�?====================

    @Override
    public List<ErpPurchaseInItemDO> getPurchaseInItemListByInId(Long inId) {
        return purchaseInItemMapper.selectListByInId(inId);
    }

    @Override
    public List<ErpPurchaseInItemDO> getPurchaseInItemListByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyList();
        }
        return purchaseInItemMapper.selectListByInIds(inIds);
    }

    @Override
    public Map<Long, BigDecimal> getApprovedReturnCountMapByInItemIds(Collection<Long> inItemIds) {
        if (CollUtil.isEmpty(inItemIds)) {
            return Collections.emptyMap();
        }
        return purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(inItemIds);
    }

    @Override
    public Map<Long, BigDecimal> getTransferOutCountMapByInItemIds(Collection<Long> inItemIds) {
        if (CollUtil.isEmpty(inItemIds)) {
            return Collections.emptyMap();
        }
        return stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(inItemIds);
    }

    @Override
    public List<ErpPurchaseReturnableItemRespVO> getReturnableItemsByInId(Long inId) {
        // 1. 校验入库单存在且已审�?
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(inId);

        // 2. 查入库项列表
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInId(inId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }

        // 3. 查所有入库项的累计已退数量
        Set<Long> itemIds = convertSet(items, ErpPurchaseInItemDO::getId);
        Map<Long, BigDecimal> returnedMap = getApprovedReturnCountMapByInItemIds(itemIds);

        // 4. 批量查产品信息（名称、编码）
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, ErpProductDO> productMap = convertMap(
                DataPermissionUtils.executeIgnore(() -> productService.validProductList(productIds)), ErpProductDO::getId);

        // 5. 组装结果：可退数量 = 入库数量 - 已退
        return items.stream().map(item -> {
            ErpPurchaseReturnableItemRespVO vo = new ErpPurchaseReturnableItemRespVO();
            vo.setSourceInId(inId);
            vo.setSourceInItemId(item.getId());
            vo.setSourceInNo(purchaseIn.getNo());
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setDeptId(item.getDeptId());
            vo.setProductPrice(item.getProductPrice());
            vo.setInCount(item.getCount());
            BigDecimal returned = returnedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            vo.setReturnedCount(returned);
            BigDecimal returnable = item.getCount().subtract(returned);
            // 可退不能为负
            if (returnable.compareTo(BigDecimal.ZERO) < 0) {
                returnable = BigDecimal.ZERO;
            }
            vo.setReturnableCount(returnable);

            vo.setPackageQty(item.getPackageQty());
            vo.setWholeQty(item.getWholeQty());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setDrawingNo(item.getDrawingNo());
            vo.setBatchNo(item.getBatchNo());
            vo.setBarCode(item.getBarCode());
            vo.setBrand(item.getBrand());
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBusinessEntity(item.getBusinessEntity());
            vo.setRemark(item.getRemark());
            ErpProductDO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductCode(product.getCode());
                vo.setBatchNoEnabled(product.getBatchNoEnabled());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpPurchaseInTransferOutableItemRespVO> getTransferOutableItemsByInId(Long inId) {
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(inId);
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInId(inId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Long> itemIds = convertSet(items, ErpPurchaseInItemDO::getId);
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, BigDecimal> movedMap = stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(itemIds);
        Map<Long, ErpProductDO> productMap = convertMap(
                DataPermissionUtils.executeIgnore(() -> productService.validProductList(productIds)),
                ErpProductDO::getId);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpPurchaseInItemDO::getWarehouseId)));

        return items.stream().map(item -> {
            BigDecimal movedCount = movedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal transferOutableCount = safeCount(item.getCount()).subtract(movedCount);
            if (transferOutableCount.compareTo(BigDecimal.ZERO) < 0) {
                transferOutableCount = BigDecimal.ZERO;
            }
            ErpPurchaseInTransferOutableItemRespVO vo = new ErpPurchaseInTransferOutableItemRespVO();
            vo.setSourceInId(purchaseIn.getId());
            vo.setSourceInItemId(item.getId());
            vo.setSourceInNo(purchaseIn.getNo());
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setFromWarehouseId(item.getWarehouseId());
            vo.setInCount(item.getCount());
            vo.setMovedCount(movedCount);
            vo.setTransferOutableCount(transferOutableCount);
            vo.setProductPrice(item.getProductPrice());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setBatchNo(item.getBatchNo());
            vo.setBarCode(item.getBarCode());
            vo.setBrand(item.getBrand());
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBusinessEntity(item.getBusinessEntity());
            vo.setRemark(item.getRemark());
            ErpProductDO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductCode(product.getCode());
                vo.setBatchNoEnabled(product.getBatchNoEnabled());
            }
            ErpProductRespVO productVO = productVOMap.get(item.getProductId());
            if (productVO != null) {
                vo.setProductUnitName(productVO.getUnitName());
            }
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setFromWarehouseName(warehouse.getName());
                vo.setFromWarehouseDeptId(warehouse.getDeptId());
                vo.setFromWarehouseDeptName(resolveDeptName(warehouse.getDeptId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpPurchaseInSaleCartableItemRespVO> getSaleCartableItemsByInId(Long inId) {
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(inId);
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInId(inId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Long> itemIds = convertSet(items, ErpPurchaseInItemDO::getId);
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, BigDecimal> convertedMap = saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(itemIds);
        Map<Long, ErpProductDO> productMap = convertMap(
                DataPermissionUtils.executeIgnore(() -> productService.validProductList(productIds)),
                ErpProductDO::getId);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpPurchaseInItemDO::getWarehouseId)));

        return items.stream().map(item -> {
            BigDecimal convertedCount = convertedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal saleCartableCount = safeCount(item.getCount()).subtract(convertedCount);
            if (saleCartableCount.compareTo(BigDecimal.ZERO) < 0) {
                saleCartableCount = BigDecimal.ZERO;
            }
            ErpPurchaseInSaleCartableItemRespVO vo = new ErpPurchaseInSaleCartableItemRespVO();
            vo.setSourceInId(purchaseIn.getId());
            vo.setSourceInItemId(item.getId());
            vo.setSourceInNo(purchaseIn.getNo());
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setInCount(item.getCount());
            vo.setConvertedCount(convertedCount);
            vo.setSaleCartableCount(saleCartableCount);
            vo.setPurchasePrice(item.getProductPrice());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setBatchNo(item.getBatchNo());
            vo.setBarCode(item.getBarCode());
            vo.setBrand(item.getBrand());
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBusinessEntity(item.getBusinessEntity());
            vo.setRemark(item.getRemark());
            ErpProductDO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductCode(product.getCode());
                vo.setBatchNoEnabled(product.getBatchNoEnabled());
                vo.setSalePrice(resolveDefaultSalePrice(product));
                vo.setStandard(product.getStandard());
            }
            ErpProductRespVO productVO = productVOMap.get(item.getProductId());
            if (productVO != null) {
                vo.setProductUnitName(productVO.getUnitName());
            }
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
                vo.setWarehouseDeptId(warehouse.getDeptId());
                vo.setWarehouseDeptName(resolveDeptName(warehouse.getDeptId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseInCreateTransferOutRespVO createTransferOutFromPurchaseIn(
            ErpPurchaseInCreateTransferOutReqVO reqVO) {
        validateStockTransferOutCreatePermission();
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(reqVO.getSourceInId());
        List<ErpPurchaseInItemDO> sourceItems = purchaseInItemMapper.selectListByInId(purchaseIn.getId());
        if (CollUtil.isEmpty(sourceItems)) {
            throw exception(PURCHASE_IN_TRANSFER_OUT_ITEMS_EMPTY);
        }
        Map<Long, ErpPurchaseInItemDO> sourceItemMap = convertMap(sourceItems, ErpPurchaseInItemDO::getId);
        Map<Long, BigDecimal> requestCountMap = buildTransferOutRequestCountMap(reqVO, sourceItemMap);
        Map<Long, BigDecimal> movedMap = stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(requestCountMap.keySet());
        validateTransferOutableCounts(requestCountMap, sourceItemMap, movedMap);

        Long defaultToWarehouseId = reqVO.getToWarehouseId();
        Map<Long, List<ErpStockMoveSaveReqVO.Item>> moveItemGroups = new LinkedHashMap<>();
        for (ErpPurchaseInCreateTransferOutReqVO.Item reqItem : reqVO.getItems()) {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(reqItem.getSourceInItemId());
            Long toWarehouseId = reqItem.getToWarehouseId() != null ? reqItem.getToWarehouseId() : defaultToWarehouseId;
            if (toWarehouseId == null) {
                throw exception(STOCK_MOVE_WAREHOUSE_REQUIRED);
            }
            if (Objects.equals(sourceItem.getWarehouseId(), toWarehouseId)) {
                throw exception(STOCK_MOVE_WAREHOUSE_SAME);
            }
            ErpStockMoveSaveReqVO.Item moveItem = new ErpStockMoveSaveReqVO.Item();
            moveItem.setFromWarehouseId(sourceItem.getWarehouseId());
            moveItem.setToWarehouseId(toWarehouseId);
            moveItem.setProductId(sourceItem.getProductId());
            moveItem.setProductPrice(sourceItem.getProductPrice());
            moveItem.setCount(reqItem.getCount());
            moveItem.setRemark(reqItem.getRemark());
            moveItem.setFromShelf(sourceItem.getWarehousePosition());
            moveItem.setBatchNo(sourceItem.getBatchNo());
            moveItem.setSourceInId(purchaseIn.getId());
            moveItem.setSourceInItemId(sourceItem.getId());
            moveItem.setSourceInNo(purchaseIn.getNo());
            moveItem.setSourceCount(sourceItem.getCount());
            moveItemGroups.computeIfAbsent(sourceItem.getWarehouseId(), key -> new ArrayList<>()).add(moveItem);
        }

        List<Long> moveIds = new ArrayList<>(moveItemGroups.size());
        moveItemGroups.values().forEach(moveItems -> {
            ErpStockMoveSaveReqVO moveReqVO = new ErpStockMoveSaveReqVO();
            moveReqVO.setDeptId(purchaseIn.getDeptId());
            moveReqVO.setToDeptId(reqVO.getToDeptId());
            moveReqVO.setMoveTime(reqVO.getMoveTime());
            moveReqVO.setSourceType(ErpBizTypeEnum.PURCHASE_IN.getType());
            moveReqVO.setSourceId(purchaseIn.getId());
            moveReqVO.setSourceNo(purchaseIn.getNo());
            moveReqVO.setRemark(StrUtil.blankToDefault(reqVO.getRemark(),
                    "由采购入库单 " + purchaseIn.getNo() + " 生成"));
            moveReqVO.setItems(moveItems);
            moveIds.add(stockMoveService.createStockMove(moveReqVO));
        });
        return new ErpPurchaseInCreateTransferOutRespVO(moveIds.get(0), moveIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseInCreateSaleCartRespVO createSaleCartFromPurchaseIn(ErpPurchaseInCreateSaleCartReqVO reqVO) {
        validateSaleCartCreatePermission();
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(reqVO.getSourceInId());
        List<ErpPurchaseInItemDO> sourceItems = purchaseInItemMapper.selectListByInId(purchaseIn.getId());
        if (CollUtil.isEmpty(sourceItems)) {
            throw exception(PURCHASE_IN_TRANSFER_OUT_ITEMS_EMPTY);
        }
        Map<Long, ErpPurchaseInItemDO> sourceItemMap = convertMap(sourceItems, ErpPurchaseInItemDO::getId);
        Map<Long, BigDecimal> requestCountMap = buildSaleCartRequestCountMap(reqVO, sourceItemMap);
        Map<Long, BigDecimal> convertedMap = saleConvertRecordMapper.selectPurchaseInToCartCountMapBySourceItemIds(requestCountMap.keySet());
        validateSaleCartableCounts(requestCountMap, sourceItemMap, convertedMap);

        ErpSaleCartSaveReqVO saleCartReqVO = buildSaleCartReqVO(reqVO, purchaseIn, sourceItemMap);
        ErpSaleCartSubmitRespVO submitRespVO = saleCartService.createAndSubmitSaleCartFromPurchaseIn(saleCartReqVO);
        List<ErpSaleCartItemDO> saleCartItems = saleCartService.getSaleCartItemListByCartId(submitRespVO.getId());
        saleConvertRecordMapper.insertBatch(buildPurchaseInToCartRecords(purchaseIn, reqVO, sourceItemMap, submitRespVO, saleCartItems));
        return new ErpPurchaseInCreateSaleCartRespVO(submitRespVO.getId(), submitRespVO.getNo(), submitRespVO.getStatus());
    }

    private void validateStockTransferOutCreatePermission() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        if (!permissionApi.hasAnyPermissions(loginUserId,
                "erp:stock-transfer-out:create", "erp:stock-move:create")) {
            throw exception(STOCK_MOVE_CREATE_PERMISSION_DENIED);
        }
    }

    private void validateSaleCartCreatePermission() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        if (!permissionApi.hasAnyPermissions(loginUserId, "erp:sale-cart:create")) {
            throw new ServiceException(STOCK_MOVE_CREATE_PERMISSION_DENIED.getCode(),
                    "Current user has no permission to create sale cart");
        }
    }

    private Map<Long, BigDecimal> buildTransferOutRequestCountMap(ErpPurchaseInCreateTransferOutReqVO reqVO,
                                                                  Map<Long, ErpPurchaseInItemDO> sourceItemMap) {
        Map<Long, BigDecimal> requestCountMap = new LinkedHashMap<>();
        for (ErpPurchaseInCreateTransferOutReqVO.Item item : reqVO.getItems()) {
            if (!sourceItemMap.containsKey(item.getSourceInItemId())) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_COUNT_POSITIVE);
            }
            requestCountMap.merge(item.getSourceInItemId(), item.getCount(), BigDecimal::add);
        }
        return requestCountMap;
    }

    private void validateTransferOutableCounts(Map<Long, BigDecimal> requestCountMap,
                                               Map<Long, ErpPurchaseInItemDO> sourceItemMap,
                                               Map<Long, BigDecimal> movedMap) {
        for (Map.Entry<Long, BigDecimal> entry : requestCountMap.entrySet()) {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(entry.getKey());
            BigDecimal movedCount = movedMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal availableCount = safeCount(sourceItem.getCount()).subtract(movedCount);
            if (availableCount.compareTo(BigDecimal.ZERO) < 0) {
                availableCount = BigDecimal.ZERO;
            }
            if (entry.getValue().compareTo(availableCount) > 0) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE,
                        entry.getKey(), entry.getValue(), availableCount);
            }
        }
    }

    private Map<Long, BigDecimal> buildSaleCartRequestCountMap(ErpPurchaseInCreateSaleCartReqVO reqVO,
                                                               Map<Long, ErpPurchaseInItemDO> sourceItemMap) {
        Map<Long, BigDecimal> requestCountMap = new LinkedHashMap<>();
        for (ErpPurchaseInCreateSaleCartReqVO.Item item : reqVO.getItems()) {
            if (!sourceItemMap.containsKey(item.getSourceInItemId())) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_COUNT_POSITIVE);
            }
            requestCountMap.merge(item.getSourceInItemId(), item.getCount(), BigDecimal::add);
        }
        return requestCountMap;
    }

    private void validateSaleCartableCounts(Map<Long, BigDecimal> requestCountMap,
                                            Map<Long, ErpPurchaseInItemDO> sourceItemMap,
                                            Map<Long, BigDecimal> convertedMap) {
        for (Map.Entry<Long, BigDecimal> entry : requestCountMap.entrySet()) {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(entry.getKey());
            BigDecimal convertedCount = convertedMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal availableCount = safeCount(sourceItem.getCount()).subtract(convertedCount);
            if (availableCount.compareTo(BigDecimal.ZERO) < 0) {
                availableCount = BigDecimal.ZERO;
            }
            if (entry.getValue().compareTo(availableCount) > 0) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE,
                        entry.getKey(), entry.getValue(), availableCount);
            }
        }
    }

    private ErpSaleCartSaveReqVO buildSaleCartReqVO(ErpPurchaseInCreateSaleCartReqVO reqVO,
                                                    ErpPurchaseInDO purchaseIn,
                                                    Map<Long, ErpPurchaseInItemDO> sourceItemMap) {
        ErpSaleCartSaveReqVO cartReqVO = new ErpSaleCartSaveReqVO();
        cartReqVO.setCustomerId(reqVO.getCustomerId());
        cartReqVO.setAccountId(reqVO.getAccountId());
        cartReqVO.setSaleUserId(reqVO.getSaleUserId());
        cartReqVO.setDeptId(reqVO.getDeptId());
        cartReqVO.setCartTime(reqVO.getCartTime());
        cartReqVO.setSettleMethod(reqVO.getSettleMethod());
        cartReqVO.setInvoiceType(reqVO.getInvoiceType());
        cartReqVO.setDeliveryMethod(reqVO.getDeliveryMethod());
        cartReqVO.setSourceType(ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType());
        cartReqVO.setSourceId(purchaseIn.getId());
        cartReqVO.setSourceNo(purchaseIn.getNo());
        cartReqVO.setRemark(StrUtil.blankToDefault(reqVO.getRemark(),
                "由采购入库单 " + purchaseIn.getNo() + " 生成"));
        cartReqVO.setOtherPrice(BigDecimal.ZERO);
        cartReqVO.setFeeAmount(BigDecimal.ZERO);
        cartReqVO.setItems(convertList(reqVO.getItems(), item -> {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(item.getSourceInItemId());
            ErpSaleCartSaveReqVO.Item cartItem = new ErpSaleCartSaveReqVO.Item();
            cartItem.setProductId(sourceItem.getProductId());
            cartItem.setWarehouseId(item.getWarehouseId());
            cartItem.setDeptId(item.getDeptId() != null ? item.getDeptId() : reqVO.getDeptId());
            cartItem.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO : item.getProductPrice());
            cartItem.setCount(item.getCount());
            cartItem.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            cartItem.setBatchNo(StrUtil.blankToDefault(item.getBatchNo(), sourceItem.getBatchNo()));
            cartItem.setWarehousePosition(sourceItem.getWarehousePosition());
            cartItem.setDrawingNo(sourceItem.getDrawingNo());
            cartItem.setBarCode(sourceItem.getBarCode());
            cartItem.setBrand(sourceItem.getBrand());
            cartItem.setVehicleModel(sourceItem.getVehicleModel());
            cartItem.setOriginPlace(sourceItem.getOriginPlace());
            cartItem.setRemark(item.getRemark());
            return cartItem;
        }));
        return cartReqVO;
    }

    private List<ErpSaleConvertRecordDO> buildPurchaseInToCartRecords(ErpPurchaseInDO purchaseIn,
                                                                      ErpPurchaseInCreateSaleCartReqVO reqVO,
                                                                      Map<Long, ErpPurchaseInItemDO> sourceItemMap,
                                                                      ErpSaleCartSubmitRespVO submitRespVO,
                                                                      List<ErpSaleCartItemDO> saleCartItems) {
        Map<String, List<ErpSaleCartItemDO>> targetItemMap = saleCartItems.stream()
                .collect(Collectors.groupingBy(this::buildSaleCartTargetItemKey, LinkedHashMap::new, Collectors.toList()));
        return convertList(reqVO.getItems(), reqItem -> {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(reqItem.getSourceInItemId());
            ErpSaleCartItemDO targetItem = pollFirst(targetItemMap.get(buildSaleCartTargetItemKey(reqItem, sourceItem)));
            return new ErpSaleConvertRecordDO()
                    .setConvertType(ErpSaleConvertTypeEnum.PURCHASE_IN_TO_CART.getType())
                    .setSourceType(ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType())
                    .setSourceId(purchaseIn.getId()).setSourceNo(purchaseIn.getNo()).setSourceItemId(sourceItem.getId())
                    .setTargetType(ErpSaleBizSourceTypeEnum.CART.getType())
                    .setTargetId(submitRespVO.getId()).setTargetNo(submitRespVO.getNo())
                    .setTargetItemId(targetItem != null ? targetItem.getId() : null)
                    .setProductId(sourceItem.getProductId()).setWarehouseId(reqItem.getWarehouseId()).setCount(reqItem.getCount());
        });
    }

    private String buildSaleCartTargetItemKey(ErpSaleCartItemDO item) {
        return item.getProductId() + "|" + item.getWarehouseId() + "|" + Boolean.TRUE.equals(item.getGiftFlag())
                + "|" + formatKeyCount(item.getCount()) + "|" + StrUtil.blankToDefault(item.getBatchNo(), "");
    }

    private String buildSaleCartTargetItemKey(ErpPurchaseInCreateSaleCartReqVO.Item reqItem, ErpPurchaseInItemDO sourceItem) {
        return sourceItem.getProductId() + "|" + reqItem.getWarehouseId() + "|" + Boolean.TRUE.equals(reqItem.getGiftFlag())
                + "|" + formatKeyCount(reqItem.getCount()) + "|" + StrUtil.blankToDefault(StrUtil.blankToDefault(reqItem.getBatchNo(), sourceItem.getBatchNo()), "");
    }

    private String formatKeyCount(BigDecimal count) {
        return count == null ? "0" : count.stripTrailingZeros().toPlainString();
    }

    private ErpSaleCartItemDO pollFirst(List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        return items.remove(0);
    }

    private BigDecimal resolveDefaultSalePrice(ErpProductDO product) {
        if (product.getSalePrice() != null) {
            return product.getSalePrice();
        }
        if (product.getRetailPrice() != null) {
            return product.getRetailPrice();
        }
        return product.getReferencePrice();
    }

    private BigDecimal safeCount(BigDecimal count) {
        return count != null ? count : BigDecimal.ZERO;
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO dept = deptApi.getDept(deptId);
        return dept == null ? null : dept.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInFromOrder(ErpPurchaseInFromOrderReqVO reqVO) {
        // 1. 校验采购订单已审�?
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        // 2. 查询订单子表
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderService.getPurchaseOrderItemListByOrderId(reqVO.getOrderId());
        Map<Long, ErpPurchaseOrderItemDO> orderItemMap = convertMap(orderItems, ErpPurchaseOrderItemDO::getId);
        // 3. 同一采购订单项可能拆到多个仓库入库，先按订单项汇总后再校验可入库数量
        Map<Long, BigDecimal> requestCountMap = new LinkedHashMap<>();
        for (ErpPurchaseInFromOrderReqVO.Item reqItem : reqVO.getItems()) {
            if (reqItem.getOrderItemId() == null) {
                throw exception(PURCHASE_ORDER_NOT_EXISTS);
            }
            if (reqItem.getCount() == null || reqItem.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_IN_ITEM_COUNT_POSITIVE);
            }
            requestCountMap.merge(reqItem.getOrderItemId(), reqItem.getCount(), BigDecimal::add);
        }
        Map<Long, BigDecimal> generatedInCountMap = getPurchaseOrderGeneratedInCountMap(reqVO.getOrderId());
        for (Map.Entry<Long, BigDecimal> entry : requestCountMap.entrySet()) {
            ErpPurchaseOrderItemDO orderItem = orderItemMap.get(entry.getKey());
            if (orderItem == null) {
                throw exception(PURCHASE_ORDER_NOT_EXISTS);
            }
            BigDecimal orderCount = orderItem.getCount() != null ? orderItem.getCount() : BigDecimal.ZERO;
            BigDecimal inCount = generatedInCountMap.getOrDefault(orderItem.getId(), BigDecimal.ZERO);
            BigDecimal inableCount = orderCount.subtract(inCount).max(BigDecimal.ZERO);
            BigDecimal requestCount = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            if (requestCount.compareTo(inableCount) > 0) {
                ErpProductDO product = productService.getProduct(orderItem.getProductId());
                String productName = product != null ? product.getName() : String.valueOf(orderItem.getProductId());
                throw exception(PURCHASE_ORDER_IN_EXCEED_INABLE, productName, inableCount, requestCount);
            }
        }
        // 4. 构造入库项
        List<ErpPurchaseInSaveReqVO.Item> inItems = new java.util.ArrayList<>();
        for (ErpPurchaseInFromOrderReqVO.Item reqItem : reqVO.getItems()) {
            ErpPurchaseOrderItemDO orderItem = orderItemMap.get(reqItem.getOrderItemId());
            if (orderItem == null) {
                throw exception(PURCHASE_ORDER_NOT_EXISTS);
            }
            // 构造入库项
            ErpPurchaseInSaveReqVO.Item inItem = new ErpPurchaseInSaveReqVO.Item();
            inItem.setOrderItemId(orderItem.getId());
            inItem.setWarehouseId(reqItem.getWarehouseId());
            inItem.setDeptId(reqItem.getDeptId() != null ? reqItem.getDeptId() : orderItem.getDeptId());
            inItem.setProductId(orderItem.getProductId());
            inItem.setProductUnitId(orderItem.getProductUnitId());
            // 赠品�?productPrice=0
            inItem.setProductPrice(Boolean.TRUE.equals(orderItem.getGift()) ? BigDecimal.ZERO : orderItem.getProductPrice());
            inItem.setGift(Boolean.TRUE.equals(orderItem.getGift()));
            inItem.setCount(reqItem.getCount());
            inItem.setBatchNo(orderItem.getBatchNo());

            inItem.setRemark(orderItem.getRemark());
            inItems.add(inItem);
        }
        // 4. 构�?ErpPurchaseInSaveReqVO
        ErpPurchaseInSaveReqVO saveReqVO = new ErpPurchaseInSaveReqVO();
        saveReqVO.setOrderId(reqVO.getOrderId());
        saveReqVO.setInTime(reqVO.getInTime() != null ? reqVO.getInTime() : java.time.LocalDateTime.now());
        saveReqVO.setAccountId(reqVO.getAccountId());
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(inItems);
        // 5. 创建入库单
        Long inId = createPurchaseIn(saveReqVO);
        return inId;
    }

    // ==================== 采购调价 专用查询 ====================

    @Override
    public List<ErpPurchaseInForAdjustRespVO> getApprovedPurchaseInsBySupplier(Long supplierId) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        // 1. 查该供应商下所有已审批的入库单
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(new LambdaQueryWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, supplierId)
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .orderByDesc(ErpPurchaseInDO::getId));
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyList();
        }

        // 2. 批量查入库项（只需要统计条数，�?inId 分组�?
        Set<Long> inIds = convertSet(purchaseIns, ErpPurchaseInDO::getId);
        List<ErpPurchaseInItemDO> itemList = purchaseInItemMapper.selectListByInIds(inIds);
        Map<Long, Long> itemCountMap = itemList.stream()
                .collect(Collectors.groupingBy(ErpPurchaseInItemDO::getInId, Collectors.counting()));

        // 3. 批量查供应商 + 审核人信�?
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(Collections.singleton(supplierId));
        Set<Long> updaterIds = purchaseIns.stream()
                .map(in -> parseLongSafely(in.getUpdater()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(updaterIds)
                ? adminUserApi.getUserMap(updaterIds) : Collections.emptyMap();

        // 4. 拼装 VO
        return purchaseIns.stream().map(in -> {
            ErpPurchaseInForAdjustRespVO vo = BeanUtils.toBean(in, ErpPurchaseInForAdjustRespVO.class);
            Long itemCount = itemCountMap.get(in.getId());
            vo.setItemCount(itemCount != null ? itemCount.intValue() : 0);
            ErpSupplierDO supplier = supplierMap.get(in.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getName());
            }
            // 近似：审核人 = updater，审批时�?= updateTime
            Long updaterId = parseLongSafely(in.getUpdater());
            if (updaterId != null) {
                vo.setAuditorId(updaterId);
                AdminUserRespDTO user = userMap.get(updaterId);
                if (user != null) {
                    vo.setAuditorName(user.getNickname());
                }
            }
            vo.setApproveTime(in.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpPurchaseInItemForAdjustRespVO> getApprovedPurchaseInItemsBySupplier(Long supplierId, Boolean excludeAdjusted) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        // 1. 查该供应商下所有已审批的入库单（用于拼 inNo / inTime�?
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(new LambdaQueryWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, supplierId)
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus()));
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyList();
        }
        Map<Long, ErpPurchaseInDO> inMap = convertMap(purchaseIns, ErpPurchaseInDO::getId);

        // 2. 批量查入库项
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInIds(inMap.keySet());
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        // 3. 过滤已调价行
        if (Boolean.TRUE.equals(excludeAdjusted)) {
            items = items.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getAdjusted()))
                    .collect(Collectors.toList());
            if (items.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // 4. 批量查产品（产品资料里有 code / name / unitName / vehicleModel / standard / featureCode / originPlace / brand / drawingNo�?
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));

        // 5. 批量查仓�?
        Set<Long> warehouseIds = convertSet(items, ErpPurchaseInItemDO::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);

        // 6. 拼装 VO
        return items.stream().map(item -> {
            ErpPurchaseInItemForAdjustRespVO vo = new ErpPurchaseInItemForAdjustRespVO();
            vo.setId(item.getId());
            vo.setInId(item.getInId());
            ErpPurchaseInDO in = inMap.get(item.getInId());
            if (in != null) {
                vo.setInNo(in.getNo());
                vo.setInTime(in.getInTime());
            }
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setProductPrice(item.getProductPrice());
            vo.setOriginalProductPrice(item.getOriginalProductPrice());
            vo.setCount(item.getCount());
            vo.setTotalPrice(item.getTotalPrice());
            vo.setDrawingNo(item.getDrawingNo());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setAdjusted(item.getAdjusted());
            // 产品资料字段（优先从产品资料补齐，item 自身也有一部分冗余字段�?
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitName(product.getUnitName());
                vo.setStandard(product.getStandard());
                vo.setFeatureCode(product.getFeatureCode());
                // 车型 / 品牌 / 产地：item 自身有冗余（随入库时快照），优先�?item �?
                vo.setVehicleModel(ObjectUtil.defaultIfNull(item.getVehicleModel(), product.getVehicleModel()));
                vo.setBrand(ObjectUtil.defaultIfNull(item.getBrand(), product.getBrand()));
                vo.setOriginPlace(ObjectUtil.defaultIfNull(item.getOriginPlace(), product.getOriginPlace()));
            } else {
                vo.setVehicleModel(item.getVehicleModel());
                vo.setBrand(item.getBrand());
                vo.setOriginPlace(item.getOriginPlace());
            }
            // 仓库
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 安全地把 creator / updater（字符串）转�?Long；失败返�?null
     */
    private static Long parseLongSafely(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public ErpPurchaseInImportRespVO importPurchaseInItems(List<ErpPurchaseInImportExcelVO> list) {
        ErpPurchaseInImportRespVO respVO = new ErpPurchaseInImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchaseInCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getPurchaseWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getName, warehouse -> warehouse, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseInImportExcelVO row = list.get(i);
            if (isEmptyPurchaseInRow(row)) {
                continue;
            }
            try {
                ErpProductDO product = getPurchaseInProduct(row, productMap);
                ErpWarehouseDO warehouse = getPurchaseInWarehouse(row, warehouseMap);
                ErpPurchaseInSaveReqVO.Item item = new ErpPurchaseInSaveReqVO.Item();
                item.setProductId(product.getId());
                item.setProductUnitId(product.getUnitId());
                item.setWarehouseId(warehouse.getId());
                item.setCount(requirePositiveCount(row.getCount(), "入库数量不能为空且必须大�?0"));
                item.setProductPrice(defaultPurchaseInPrice(row.getProductPrice(), product.getPurchasePrice()));
                item.setPackageQty(defaultPackageQty(product.getPackageQty()));
                item.setWholeQty(row.getWholeQty());
                item.setWarehousePosition(trimToNull(row.getWarehousePosition()));
                item.setBatchNo(trimToNull(row.getBatchNo()));
                item.setRemark(trimToNull(row.getRemark()));
                fillPurchaseInProductFields(item, product, productVOMap.get(product.getId()));
                respVO.getItems().add(item);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpPurchaseInImportRespVO.FailureItem(
                        i + 2, row.getProductCode(), ex.getMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseImportResultRespVO importPurchaseInOrderList(List<ErpPurchaseInOrderImportExcelVO> list) {
        ErpPurchaseImportResultRespVO respVO = new ErpPurchaseImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpSupplierDO> supplierMap = buildSupplierMap();
        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchaseInOrderProductCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getPurchaseWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getName, warehouse -> warehouse, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        List<PurchaseInOrderImportGroup> groups = new ArrayList<>();
        PurchaseInOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseInOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankPurchaseInOrderRow(row)) {
                continue;
            }
            boolean hasMain = hasPurchaseInOrderMainFields(row);
            boolean hasDetail = hasPurchaseInOrderDetailFields(row);
            if (hasMain) {
                ErpSupplierDO supplier = resolveSupplier(row.getSupplierName(), supplierMap);
                currentGroup = new PurchaseInOrderImportGroup(rowNo, row, supplier);
                groups.add(currentGroup);
                String orderNo = resolvePurchaseInOrderNo(rowNo, row);
                String supplierName = trimToNull(row.getSupplierName());
                if (supplierName == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier is required");
                } else if (supplier == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier not found: " + supplierName);
                } else if (CommonStatusEnum.isDisable(supplier.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier(" + supplier.getName() + ") is disabled");
                }
                validateImportDate(respVO, rowNo, orderNo, null, "入库时间", row.getInTime());
            } else if (hasDetail && currentGroup == null) {
                addImportFailure(respVO, rowNo, null, trimToNull(row.getProductCode()), "Missing purchase inbound header before item row");
                continue;
            }
            if (!hasDetail) {
                continue;
            }
            String orderNo = currentGroup == null ? null : resolvePurchaseInOrderNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            String productCode = trimToNull(row.getProductCode());
            boolean valid = true;
            ErpProductDO product = productMap.get(productCode);
            ErpWarehouseDO warehouse = warehouseMap.get(trimToNull(row.getWarehouseName()));
            if (productCode == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品编码不能为空");
                valid = false;
            } else if (product == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "Product not found");
                valid = false;
            }
            if (trimToNull(row.getWarehouseName()) == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "仓库名称不能为空");
                valid = false;
            } else if (warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "仓库不存在：" + row.getWarehouseName());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "入库数量必须大于 0");
                valid = false;
            }
            if (row.getProductPrice() != null && row.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "入库单价不能小于 0");
                valid = false;
            }
            if (valid) {
                currentGroup.getRows().add(new PurchaseInOrderImportRow(rowNo, row, product, warehouse));
            }
        }

        for (PurchaseInOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolvePurchaseInOrderNo(group.getRowNo(), group.getMainRow()), null,
                        "Purchase inbound requires at least one item");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }

        for (PurchaseInOrderImportGroup group : groups) {
            ErpPurchaseInSaveReqVO saveReqVO = buildPurchaseInOrderSaveReq(group, productVOMap);
            Long id = createPurchaseIn(saveReqVO);
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpPurchaseInSaveReqVO buildPurchaseInOrderSaveReq(PurchaseInOrderImportGroup group,
                                                               Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseInOrderImportExcelVO mainRow = group.getMainRow();
        ErpPurchaseInSaveReqVO saveReqVO = new ErpPurchaseInSaveReqVO();
        saveReqVO.setSupplierId(group.getSupplier().getId());
        saveReqVO.setInTime(parseImportDate(mainRow.getInTime(), LocalDateTime.now()));
        saveReqVO.setFactoryOrderNo(trimToNull(mainRow.getFactoryOrderNo()));
        saveReqVO.setRemark(trimToNull(mainRow.getRemark()));
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(convertList(group.getRows(), row -> buildPurchaseInOrderItem(row, productVOMap)));
        return saveReqVO;
    }

    private ErpPurchaseInSaveReqVO.Item buildPurchaseInOrderItem(PurchaseInOrderImportRow importRow,
                                                                 Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseInOrderImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpProductRespVO productVO = productVOMap.get(product.getId());
        ErpPurchaseInSaveReqVO.Item item = new ErpPurchaseInSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setProductUnitId(product.getUnitId());
        item.setWarehouseId(importRow.getWarehouse().getId());
        item.setCount(row.getItemCount());
        item.setProductPrice(defaultPurchaseInPrice(row.getProductPrice(), product.getPurchasePrice()));
        item.setPackageQty(defaultPackageQty(product.getPackageQty()));
        item.setWholeQty(row.getWholeQty());
        item.setWarehousePosition(trimToNull(row.getWarehousePosition()));
        item.setBatchNo(trimToNull(row.getBatchNo()));
        item.setRemark(trimToNull(row.getItemRemark()));
        fillPurchaseInProductFields(item, product, productVO);
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

    private static Set<String> extractPurchaseInOrderProductCodes(List<ErpPurchaseInOrderImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchaseInOrderImportExcelVO row : list) {
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

    private boolean isBlankPurchaseInOrderRow(ErpPurchaseInOrderImportExcelVO row) {
        return row == null || !hasPurchaseInOrderMainFields(row) && !hasPurchaseInOrderDetailFields(row);
    }

    private boolean hasPurchaseInOrderMainFields(ErpPurchaseInOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getNo()))
                || StrUtil.isNotBlank(trimToNull(row.getSupplierName()))
                || StrUtil.isNotBlank(trimToNull(row.getInTime()))
                || StrUtil.isNotBlank(trimToNull(row.getFactoryOrderNo()))
                || StrUtil.isNotBlank(trimToNull(row.getRemark()));
    }

    private boolean hasPurchaseInOrderDetailFields(ErpPurchaseInOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getProductCode()))
                || StrUtil.isNotBlank(trimToNull(row.getWarehouseName()))
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || row.getWholeQty() != null
                || StrUtil.isNotBlank(trimToNull(row.getWarehousePosition()))
                || StrUtil.isNotBlank(trimToNull(row.getBatchNo()))
                || StrUtil.isNotBlank(trimToNull(row.getItemRemark()));
    }

    private String resolvePurchaseInOrderNo(Integer rowNo, ErpPurchaseInOrderImportExcelVO row) {
        String no = trimToNull(row.getNo());
        if (no != null) {
            return no;
        }
        String supplierName = trimToNull(row.getSupplierName());
        if (supplierName != null) {
            return supplierName;
        }
        return "row " + rowNo;
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
            addImportFailure(respVO, rowNo, orderNo, productCode, label + "格式不正确，请使�?yyyy-MM-dd �?yyyy-MM-dd HH:mm:ss");
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

    private static Set<String> extractPurchaseInCodes(List<ErpPurchaseInImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchaseInImportExcelVO row : list) {
            String code = trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isEmptyPurchaseInRow(ErpPurchaseInImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getProductCode(), row.getWarehouseName(), row.getRemark())
                && row.getCount() == null && row.getProductPrice() == null
                && row.getWholeQty() == null && StrUtil.isBlank(row.getWarehousePosition())
                && StrUtil.isBlank(row.getBatchNo());
    }

    private ErpProductDO getPurchaseInProduct(ErpPurchaseInImportExcelVO row, Map<String, ErpProductDO> productMap) {
        String code = trimToNull(row.getProductCode());
        if (code == null) {
            throw new IllegalArgumentException("产品编码不能为空");
        }
        ErpProductDO product = productMap.get(code);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在：" + code);
        }
        return product;
    }

    private ErpWarehouseDO getPurchaseInWarehouse(ErpPurchaseInImportExcelVO row, Map<String, ErpWarehouseDO> warehouseMap) {
        String warehouseName = trimToNull(row.getWarehouseName());
        if (warehouseName == null) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        ErpWarehouseDO warehouse = warehouseMap.get(warehouseName);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在：" + warehouseName);
        }
        return warehouse;
    }

    private BigDecimal defaultPurchaseInPrice(BigDecimal importPrice, BigDecimal productPrice) {
        BigDecimal price = importPrice != null ? importPrice : productPrice;
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("入库单价不能小于 0");
        }
        return price;
    }

    private Integer defaultPackageQty(Integer packageQty) {
        return packageQty == null || packageQty <= 0 ? 1 : packageQty;
    }

    private BigDecimal requirePositiveCount(BigDecimal count, String message) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return count;
    }

    private void fillPurchaseInProductFields(ErpPurchaseInSaveReqVO.Item item, ErpProductDO product, ErpProductRespVO productVO) {
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        item.setBarCode(product.getBarCode());
        item.setBrand(product.getBrand());
        item.setVehicleModel(product.getVehicleModel());
        item.setOriginPlace(product.getOriginPlace());
        item.setDrawingNo(product.getDrawingNo());
        if (productVO != null) {
            item.setProductUnitName(productVO.getUnitName());
        }
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

    private static class PurchaseInOrderImportGroup {

        private final Integer rowNo;
        private final ErpPurchaseInOrderImportExcelVO mainRow;
        private final ErpSupplierDO supplier;
        private final List<PurchaseInOrderImportRow> rows = new ArrayList<>();

        private PurchaseInOrderImportGroup(Integer rowNo, ErpPurchaseInOrderImportExcelVO mainRow, ErpSupplierDO supplier) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.supplier = supplier;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseInOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpSupplierDO getSupplier() {
            return supplier;
        }

        public List<PurchaseInOrderImportRow> getRows() {
            return rows;
        }

    }

    private static class PurchaseInOrderImportRow {

        private final Integer rowNo;
        private final ErpPurchaseInOrderImportExcelVO row;
        private final ErpProductDO product;
        private final ErpWarehouseDO warehouse;

        private PurchaseInOrderImportRow(Integer rowNo, ErpPurchaseInOrderImportExcelVO row,
                                         ErpProductDO product, ErpWarehouseDO warehouse) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseInOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public ErpWarehouseDO getWarehouse() {
            return warehouse;
        }

    }

}
