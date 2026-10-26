package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemMatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockLockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SALE_CART_TYPE;

/**
 * ERP 销售手推车 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ErpSaleCartServiceImpl implements ErpSaleCartService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_cart";
    private static final String FIRST_APPROVE_CONFIG_TYPE = "SALE_CART_FIRST_APPROVE";
    private static final String FIRST_APPROVE_CONFIG_CODE = "GLOBAL";
    private static final String FIRST_APPROVE_DEPT_CONFIG_TYPE = "SALE_CART_FIRST_APPROVE_DEPT";
    private static final String FIRST_APPROVE_DEPT_CODE_PREFIX = "DEPT_";
    private static final int TRANSFER_DIRECTION_OUT = 10;
    private static final String FREIGHT_TYPE_CUSTOMER_ADVANCE = "代客户付";
    private static final String FREIGHT_TYPE_SELF_PAY = "我方自付";
    private static final Long MALL_DEFAULT_ADMIN_USER_ID = 121L;

    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleCartTransferLinkService saleCartTransferLinkService;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Resource
    private ErpSaleConfigMapper saleConfigMapper;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Resource
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Resource
    private ErpSaleQuoteProgressService saleQuoteProgressService;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpReceivableOtherService receivableOtherService;
    @Resource
    private ErpPayableExpenseService payableExpenseService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockLockService stockLockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;
    @Resource
    private ErpSalePriceLevelPricePicker priceLevelPricePicker;
    @Resource
    private ErpSalePriceLevelPermissionValidator priceLevelPermissionValidator;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleCart(ErpSaleCartSaveReqVO createReqVO) {
        ErpSaleDecimalUtils.requireScale(createReqVO.getDiscountPercent(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getFeeAmount(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getOtherPrice(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getTaxRate(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getTotalFreight(), "开单时间，由系统自动生成");
        if (createReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < createReqVO.getItems().size(); decimalRow++) {
                ErpSaleCartSaveReqVO.Item decimalItem = createReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行是否赠品");
            }
        }
        CreatedSaleCart created = createSaleCart(createReqVO, ErpSaleCartStatusEnum.PROCESS.getStatus());
        recordCreate(created.cart.getId(), created.cart.getNo());
        return created.cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleCartDraftFromSource(ErpSaleCartSaveReqVO createReqVO) {
        ErpSaleDecimalUtils.requireScale(createReqVO.getDiscountPercent(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getFeeAmount(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getOtherPrice(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getTaxRate(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(createReqVO.getTotalFreight(), "开单时间，由系统自动生成");
        if (createReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < createReqVO.getItems().size(); decimalRow++) {
                ErpSaleCartSaveReqVO.Item decimalItem = createReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行是否赠品");
            }
        }
        CreatedSaleCart created = createSaleCart(createReqVO, ErpSaleCartStatusEnum.PROCESS.getStatus(), true);
        recordCreate(created.cart.getId(), created.cart.getNo());
        return created.cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleCartSubmitRespVO createAndSubmitSaleCart(ErpSaleCartSaveReqVO createReqVO) {
        CreatedSaleCart created = createSaleCart(createReqVO, null);
        return submitCreatedSaleCart(created);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleCartSubmitRespVO createAndSubmitSaleCartFromPurchaseIn(ErpSaleCartSaveReqVO createReqVO) {
        CreatedSaleCart created = createSaleCart(createReqVO, null, true);
        return submitCreatedSaleCart(created);
    }

    private ErpSaleCartSubmitRespVO submitCreatedSaleCart(CreatedSaleCart created) {
        Integer targetStatus = created.cart.getStatus();
        ErpSaleCartSubmitRespVO result = buildSubmitResult(created.cart, created.items);
        if (CollUtil.isNotEmpty(result.getShortageItems())) {
            throw buildStockShortageException(result.getShortageItems());
        }
        result.setId(created.cart.getId());
        result.setStatus(targetStatus);
        recordCreate(created.cart.getId(), created.cart.getNo());
        record(created.cart.getId(), "提交", "提交销售手推车，单据编号：" + created.cart.getNo(), created.cart.getNo());
        return result;
    }

    private CreatedSaleCart createSaleCart(ErpSaleCartSaveReqVO createReqVO, Integer status) {
        return createSaleCart(createReqVO, status, false);
    }

    private CreatedSaleCart createSaleCart(ErpSaleCartSaveReqVO createReqVO, Integer status, boolean preserveSource) {
        boolean draft = ErpSaleCartStatusEnum.PROCESS.getStatus().equals(status);
        Integer sourceType = createReqVO.getSourceType();
        Long sourceId = createReqVO.getSourceId();
        String sourceNo = createReqVO.getSourceNo();
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO, createReqVO.getItems());
        Long saleDeptId = resolveSaleDeptId(createReqVO.getDeptId());
        if (createReqVO.getCustomerId() != null) {
            validateCustomerForCreate(createReqVO.getCustomerId(), saleDeptId, preserveSource);
        }
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_CART_NO_PREFIX);
        if (saleCartMapper.selectByNo(no) != null) {
            throw exception(SALE_CART_NO_EXISTS);
        }

        ErpSaleCartDO cart = BeanUtils.toBean(createReqVO, ErpSaleCartDO.class,
                in -> in.setNo(no).setCartTime(LocalDateTime.now()));
        if (preserveSource) {
            cart.setSourceType(sourceType);
            cart.setSourceId(sourceId);
            cart.setSourceNo(sourceNo);
        } else {
            // 管理后台新增不能伪造商城、报价等结构化来源，来源只允许由可信内部转换入口写入。
            cart.setSourceType(null);
            cart.setSourceId(null);
            cart.setSourceNo(null);
        }
        saleDocumentDefaultService.fillCreateDefaults(cart);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(cart.getDeptId());
        cart.setStatus(status != null ? status : ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus());
        List<ErpSaleCartSaveReqVO.Item> itemReqs = mergeCreateItemRequests(createReqVO.getItems());
        if (draft) {
            itemReqs = filterDraftItems(itemReqs);
            if (CollUtil.isEmpty(itemReqs)) {
                throw exception(SALE_CART_DRAFT_ITEMS_REQUIRED);
            }
        } else if (CollUtil.isEmpty(itemReqs)) {
            throw exception(SALE_CART_ITEMS_EMPTY);
        }
        List<ErpSaleCartItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : (draft
                ? validateSaleCartDraftItems(itemReqs, cart.getDeptId())
                : validateSaleCartItems(itemReqs, cart.getDeptId()));
        calculateTotalPrice(cart, items);
        if (!draft) {
            validateFreightFinanceFields(cart);
        }
        if (isMallOrderSource(preserveSource, sourceType)) {
            fillMallDefaultAuditFields(cart, items);
        }
        saleCartTransferLinkService.insertCart(cart);
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setId(null).setCartId(cart.getId()));
            saleCartItemMapper.insertBatch(items);
        }
        return new CreatedSaleCart(cart, items);
    }

    private boolean isMallOrderSource(boolean preserveSource, Integer sourceType) {
        return preserveSource && ErpSaleBizSourceTypeEnum.MALL_ORDER.getType().equals(sourceType);
    }

    private void fillMallDefaultAuditFields(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        String mallDefaultAdminUserId = MALL_DEFAULT_ADMIN_USER_ID.toString();
        cart.setSaleUserId(MALL_DEFAULT_ADMIN_USER_ID);
        cart.setCreator(mallDefaultAdminUserId);
        cart.setUpdater(mallDefaultAdminUserId);
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> {
                item.setCreator(mallDefaultAdminUserId);
                item.setUpdater(mallDefaultAdminUserId);
            });
        }
    }

    private void validateCustomerForCreate(Long customerId, Long saleDeptId, boolean preserveSource) {
        if (preserveSource) {
            customerService.validateCustomerForGeneratedSale(customerId, saleDeptId);
            return;
        }
        customerService.validateCustomerForSale(customerId, saleDeptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO) {
        ErpSaleDecimalUtils.requireScale(updateReqVO.getDiscountPercent(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getFeeAmount(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getOtherPrice(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getTaxRate(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getTotalFreight(), "开单时间，由系统自动生成");
        if (updateReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < updateReqVO.getItems().size(); decimalRow++) {
                ErpSaleCartSaveReqVO.Item decimalItem = updateReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行是否赠品");
            }
        }
        updateSaleCart(updateReqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(updateReqVO.getId());
        saleCartMapper.updateById(new ErpSaleCartDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartDraft(ErpSaleCartSaveReqVO updateReqVO) {
        ErpSaleDecimalUtils.requireScale(updateReqVO.getDiscountPercent(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getFeeAmount(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getOtherPrice(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getTaxRate(), "开单时间，由系统自动生成");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getTotalFreight(), "开单时间，由系统自动生成");
        if (updateReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < updateReqVO.getItems().size(); decimalRow++) {
                ErpSaleCartSaveReqVO.Item decimalItem = updateReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行明细操作类型：insert 新增，update 修改，delete 删除");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行是否赠品");
            }
        }
        updateSaleCart(updateReqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSaleCartItems(ErpSaleCartItemBatchUpdateReqVO updateReqVO) {
        boolean updateWarehouse = updateReqVO.getWarehouseId() != null;
        boolean updateDept = updateReqVO.getDeptId() != null;
        boolean updatePrice = updateReqVO.getPriceLevel() != null;
        if (!updateWarehouse && !updateDept && !updatePrice) {
            throw exception(SALE_CART_ITEM_BATCH_UPDATE_EMPTY);
        }
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE,
                updateWarehouse, updateDept, updatePrice,
                SALE_CART_ITEM_BATCH_UPDATE_FIELD_DENIED);
        ErpSaleCartDO cart = validateSaleCartForUpdate(updateReqVO.getCartId());
        if (!isEditableBeforeBillingConfirm(cart)) {
            throw exception(SALE_CART_UPDATE_FAIL_NOT_PROCESS, cart.getNo());
        }
        if (hasUnapprovedCartTransfer(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId())
                || hasApprovedCartTransfer(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId())) {
            throw exception(SALE_CART_ITEM_BATCH_UPDATE_TRANSFER_EXISTS, cart.getNo());
        }

        List<ErpSaleCartItemDO> cartItems = currentCartItems(updateReqVO.getCartId());
        Map<Long, ErpSaleCartItemDO> itemMap = convertMap(cartItems, ErpSaleCartItemDO::getId);
        List<ErpSaleCartItemDO> selectedItems = new ArrayList<>();
        for (Long itemId : new LinkedHashSet<>(updateReqVO.getItemIds())) {
            ErpSaleCartItemDO item = itemMap.get(itemId);
            if (item == null) {
                throw exception(SALE_CART_ITEM_BATCH_UPDATE_NOT_EXISTS, itemId);
            }
            selectedItems.add(item);
        }
        if (updateWarehouse && isQuoteSourceCart(cart)) {
            Set<Long> linkedItemIds = saleConvertRecordMapper
                    .selectQuoteToCartListByCartIds(Collections.singleton(cart.getId())).stream()
                    .map(ErpSaleConvertRecordDO::getTargetItemId).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            selectedItems.stream().filter(item -> linkedItemIds.contains(item.getId())).findFirst()
                    .ifPresent(item -> {
                        throw exception(SALE_CART_QUOTE_SOURCE_IDENTITY_MODIFY_FAIL, item.getId());
                    });
        }

        ErpWarehouseDO targetWarehouse = null;
        Long targetDeptId = null;
        if (updateWarehouse || updateDept) {
            targetWarehouse = batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId());
            targetDeptId = batchUpdateSupport.resolveTargetDeptId(targetWarehouse, updateReqVO.getDeptId(),
                    FIELD_PERMISSION_MODULE, SALE_CART_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            for (ErpSaleCartItemDO item : selectedItems) {
                Long finalWarehouseId = targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
                batchUpdateSupport.validateWarehouseDept(finalWarehouseId, targetDeptId, FIELD_PERMISSION_MODULE,
                        SALE_CART_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
        }

        Map<Long, BigDecimal> productPriceMap = updatePrice
                ? pickBatchProductPriceMap(convertSet(selectedItems, ErpSaleCartItemDO::getProductId), updateReqVO,
                cart.getDeptId())
                : Collections.emptyMap();
        Set<String> touchedKeys = selectedItems.stream().map(this::stockKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ErpWarehouseDO finalTargetWarehouse = targetWarehouse;
        Long finalTargetDeptId = targetDeptId;
        selectedItems.forEach(item -> {
            if (finalTargetWarehouse != null) {
                item.setWarehouseId(finalTargetWarehouse.getId());
                item.setBatchNo(null);
                ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), finalTargetWarehouse.getId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
            }
            if (updateDept || finalTargetWarehouse != null) {
                item.setDeptId(finalTargetDeptId);
            }
            if (updatePrice) {
                BigDecimal productPrice = Boolean.TRUE.equals(item.getGiftFlag())
                        ? BigDecimal.ZERO : productPriceMap.getOrDefault(item.getProductId(), BigDecimal.ZERO);
                item.setProductPrice(productPrice);
                item.setTotalPrice(ErpSaleDecimalUtils.multiply(productPrice, item.getCount()));
                item.setTaxPercent(null);
                item.setTaxPrice(BigDecimal.ZERO);
            }
        });
        selectedItems.stream().map(this::stockKey).forEach(touchedKeys::add);
        Set<Long> selectedItemIds = selectedItems.stream().map(ErpSaleCartItemDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        CartItemMergeOutcome mergeOutcome = mergeTouchedCartItems(cartItems, touchedKeys,
                updatePrice ? selectedItemIds : Collections.emptySet());
        saleConvertRecordMapper.redirectTargetItemIds(cart.getId(), mergeOutcome.redirects);
        validateSourceMinimumCount(cart, mergeOutcome.items);
        if (CollUtil.isNotEmpty(mergeOutcome.redirects)) {
            saleCartItemMapper.deleteByIds(mergeOutcome.redirects.keySet());
        }
        Set<Long> updateIds = new LinkedHashSet<>(selectedItemIds);
        updateIds.addAll(mergeOutcome.redirects.values());
        List<ErpSaleCartItemDO> updateItems = mergeOutcome.items.stream()
                .filter(item -> updateIds.contains(item.getId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateItems)) {
            saleCartItemMapper.updateBatch(updateItems);
        }
        ErpSaleCartDO totalUpdate = new ErpSaleCartDO()
                .setId(updateReqVO.getCartId())
                .setDiscountPercent(cart.getDiscountPercent())
                .setFeeAmount(cart.getFeeAmount())
                .setOtherPrice(cart.getOtherPrice());
        calculateTotalPrice(totalUpdate, mergeOutcome.items);
        saleCartMapper.updateById(totalUpdate);
        if (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())) {
            validateStockEnoughRealtime(mergeOutcome.items);
        }
        recordUpdate(updateReqVO.getCartId(), cart.getNo());
    }

    private Map<Long, BigDecimal> pickBatchProductPriceMap(Set<Long> productIds,
                                                           ErpSaleCartItemBatchUpdateReqVO updateReqVO,
                                                           Long businessDeptId) {
        priceLevelPermissionValidator.validateSelectablePriceLevel(updateReqVO.getPriceLevel(), businessDeptId);
        return priceLevelPricePicker.pickProductPriceMap(productIds, updateReqVO.getPriceLevel());
    }

    private void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO, boolean draft) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(updateReqVO.getId());
        if (draft
                ? !ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())
                : !isEditableBeforeBillingConfirm(cart)) {
            throw exception(SALE_CART_UPDATE_FAIL_NOT_PROCESS, cart.getNo());
        }
        preserveHiddenFields(updateReqVO, cart);
        List<ErpSaleCartSaveReqVO.Item> requestItems = updateReqVO.getItems();
        List<ErpSaleCartSaveReqVO.Item> itemReqs = requestItems;
        List<ErpSaleCartItemDO> oldItems = saleCartItemMapper.selectListByCartIdForUpdate(updateReqVO.getId());
        preserveHiddenItemFields(updateReqVO, itemReqs, oldItems);
        boolean incrementalItems = ErpSaleItemOperationHelper.useIncrementalItems(itemReqs,
                ErpSaleCartSaveReqVO.Item::getOperation, SALE_CART_ITEM_OPERATION_INVALID);
        ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleCartSaveReqVO.Item> itemChangeSet = null;
        if (incrementalItems) {
            itemChangeSet = ErpSaleItemOperationHelper.buildRequestChangeSet(itemReqs, oldItems,
                    ErpSaleCartSaveReqVO.Item.class, ErpSaleCartSaveReqVO.Item::getId,
                    ErpSaleCartSaveReqVO.Item::setId, ErpSaleCartSaveReqVO.Item::getOperation,
                    ErpSaleCartItemDO::getId, SALE_CART_ITEM_OPERATION_INVALID,
                    SALE_CART_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        Set<String> touchedStockKeys = collectTouchedStockKeys(requestItems, oldItems, incrementalItems);
        ItemMergeOutcome mergeOutcome = mergeTouchedItemRequests(itemReqs, touchedStockKeys, oldItems, requestItems);
        itemReqs = new ArrayList<>(mergeOutcome.items);
        if (incrementalItems) {
            itemChangeSet.getFinalItems().clear();
            itemChangeSet.getFinalItems().addAll(itemReqs);
            itemChangeSet.getDeleteIds().addAll(mergeOutcome.redirects.keySet());
            itemChangeSet.getUpdateIds().addAll(mergeOutcome.updateIds);
        }
        ErpSaleCartDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleCartDO.class);
        updateObj.setCartTime(LocalDateTime.now());
        // 来源是建单时的不可变追溯信息，编辑请求不得覆盖或清空，否则会破坏网上订单分栏。
        updateObj.setSourceType(cart.getSourceType());
        updateObj.setSourceId(cart.getSourceId());
        updateObj.setSourceNo(cart.getSourceNo());
        Long saleDeptId = updateObj.getDeptId() != null ? updateObj.getDeptId() : cart.getDeptId();
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleDeptId);
        updateObj.setNo(cart.getNo());
        updateObj.setStatus(cart.getStatus());
        updateObj.setDeptId(saleDeptId);
        if (!draft || updateReqVO.getCustomerId() != null) {
            customerService.validateCustomerForSale(updateReqVO.getCustomerId(), saleDeptId);
        }
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        if (draft && CollUtil.isNotEmpty(itemReqs)) {
            itemReqs = filterDraftItems(itemReqs);
        }
        if (!draft && CollUtil.isEmpty(itemReqs)) {
            throw exception(SALE_CART_ITEMS_EMPTY);
        }
        List<ErpSaleCartItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : (draft
                ? validateSaleCartDraftItems(itemReqs, saleDeptId)
                : validateSaleCartItems(itemReqs, saleDeptId));
        saleConvertRecordMapper.redirectTargetItemIds(updateReqVO.getId(), mergeOutcome.redirects);
        List<ErpSaleConvertRecordDO> quoteSourceRecords = validateQuoteSourceCartItems(cart, oldItems, items);
        validateSourceMinimumCount(cart, items);
        calculateTotalPrice(updateObj, items);
        if (!draft) {
            validateFreightFinanceFields(updateObj);
        }
        saleCartMapper.updateById(updateObj);
        if (incrementalItems) {
            applySaleCartItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            applyFullItemUpdate(updateReqVO.getId(), oldItems, items);
        }
        syncQuoteSourceConvertRecords(quoteSourceRecords);
        if (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())) {
            validateStockEnoughRealtime(items);
        }
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    private void applySaleCartItemChangeSet(Long cartId,
            ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleCartSaveReqVO.Item> changeSet,
            List<ErpSaleCartItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            saleCartItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpSaleCartItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setId(null).setCartId(cartId));
            saleCartItemMapper.insertBatch(insertList);
        }
        List<ErpSaleCartItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setCartId(cartId));
            saleCartItemMapper.updateBatch(updateList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartBasic(ErpSaleCartUpdateBasicReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(updateReqVO.getId());
        if (!ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_BASIC_FAIL_STATUS, cart.getNo());
        }
        customerService.validateCustomerForSale(updateReqVO.getCustomerId(), cart.getDeptId());
        BigDecimal feeAmount = updateReqVO.getFeeAmount() != null ? updateReqVO.getFeeAmount() : BigDecimal.ZERO;
        saleCartMapper.updateById(new ErpSaleCartDO()
                .setId(updateReqVO.getId())
                .setCustomerId(updateReqVO.getCustomerId())
                .setFeeAmount(feeAmount)
                .setOtherPrice(feeAmount)
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleCartSubmitRespVO submitSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(cart.getDeptId());
        List<ErpSaleCartItemDO> items = currentCartItems(id);
        normalizeItemDeptIdByCartDept(cart, items);
        validateSaleCartForBillingConfirm(cart, items);
        ErpSaleCartSubmitRespVO result = buildSubmitResult(cart, items);
        if (CollUtil.isNotEmpty(result.getShortageItems())) {
            throw buildStockShortageException(result.getShortageItems());
        }
        Integer targetStatus = ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus();
        updateStatus(id, ErpSaleCartStatusEnum.PROCESS.getStatus(), targetStatus,
                SALE_CART_SUBMIT_FAIL);
        result.setStatus(targetStatus);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleCartSubmitRespVO confirmSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(cart.getDeptId());
        if (!ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_CONFIRM_FAIL);
        }
        List<ErpSaleCartItemDO> items = currentCartItems(id);
        normalizeItemDeptIdByCartDept(cart, items);
        validateSaleCartForBillingConfirm(cart, items);
        ErpSaleCartSubmitRespVO result = buildSubmitResult(cart, items);
        if (CollUtil.isNotEmpty(result.getShortageItems())) {
            throw buildStockShortageException(result.getShortageItems());
        }
        Integer targetStatus = getBillingConfirmTargetStatus(cart.getDeptId());
        Long confirmUserId = SecurityFrameworkUtils.getLoginUserId();
        LocalDateTime confirmTime = LocalDateTime.now();
        int updateCount = saleCartMapper.updateByIdAndStatus(id,
                ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus(),
                new ErpSaleCartDO().setStatus(targetStatus)
                        .setBillingConfirmUserId(confirmUserId)
                        .setBillingConfirmTime(confirmTime));
        if (updateCount == 0) {
            throw exception(SALE_CART_CONFIRM_FAIL);
        }
        cart.setStatus(targetStatus);
        cart.setBillingConfirmUserId(confirmUserId);
        cart.setBillingConfirmTime(confirmTime);
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(targetStatus)) {
            lockSaleCartStock(cart, items);
            createTransferOutDraft(cart, items);
        }
        result.setStatus(targetStatus);
        record(id, "开单确认", "确认销售手推车，单据编号：" + cart.getNo(), cart.getNo());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCrossDeptTransferOutDraftByCartId(Long cartId) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(cartId);
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_FIRST_APPROVE_FAIL);
        }
        List<ErpSaleCartItemDO> items = currentCartItems(cartId);
        normalizeItemDeptIdByCartDept(cart, items);
        createTransferOutDraft(cart, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void firstApproveSaleCart(Long id) {
        FirstApproveConfig config = getFirstApproveConfigObject();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw exception(SALE_CART_FIRST_APPROVE_DISABLED);
        }
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(cart.getDeptId());
        if (!isFirstApproveRequiredForDept(config, cart.getDeptId())) {
            throw exception(SALE_CART_FIRST_APPROVE_DEPT_UNAUTHORIZED);
        }
        List<ErpSaleCartItemDO> items = currentCartItems(id);
        normalizeItemDeptIdByCartDept(cart, items);
        // 新流程的待初审单据尚未进入库存占用口径；历史无确认记录的状态 20 仍按旧口径兼容。
        validateStockEnoughRealtime(items, cart.getBillingConfirmTime() == null);
        lockSaleCartStock(cart, items);
        createTransferOutDraft(cart, items);
        updateStatus(id, ErpSaleCartStatusEnum.SUBMITTED.getStatus(), ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(),
                SALE_CART_FIRST_APPROVE_FAIL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> finalApproveSaleCart(Long id) {
        return doFinalApproveSaleCart(id, SecurityFrameworkUtils.getLoginUserId(), false);
    }

    private List<Long> doFinalApproveSaleCart(Long id, Long finalApproveUserId,
                                              boolean fulfillmentAutoApprove) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(cart.getDeptId());
        ErpSalePickDeliverySummaryRespVO fulfillment = salePickDeliveryService.getSummaryBySaleCartId(id);
        if (fulfillment != null) {
            if (!fulfillmentAutoApprove) {
                throw exception(SALE_CART_FULFILLMENT_MANUAL_FINAL_FORBIDDEN);
            }
            if (!ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(fulfillment.getDeliveryStatus())) {
                throw exception(SALE_CART_FULFILLMENT_NOT_COMPLETED);
            }
        }
        Integer oldStatus = resolveFinalApproveOldStatus(cart);
        int claimCount = saleCartMapper.updateByIdAndStatus(id, oldStatus,
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus()));
        if (claimCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        List<ErpSaleCartItemDO> items = currentCartItems(id);
        normalizeItemDeptIdByCartDept(cart, items);
        // 终审时实时校验库存，不依赖缓存的 stockCount。
        // The cart has already been claimed as FINAL_APPROVE, so it is no longer included in occupied stock.
        List<ErpSaleCartItemDO> finalItems = resolveFinalSaleItems(cart, items);
        validateStockEnoughRealtime(finalItems, false, cart.getDeptId());
        validateFreightFinanceFields(cart);
        Set<Long> warehouseIds = convertSet(finalItems, ErpSaleCartItemDO::getWarehouseId);
        warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, cart.getDeptId(),
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION);
        Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(cart, finalItems),
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), cart.getNo(), false);
        LocalDateTime finalApproveTime = LocalDateTime.now();
        createFreightFinanceDocuments(cart, finalApproveTime);
        // 普通销售审核已通过 deductStock 释放本手推车锁；此处不再重复释放或扣实物。
        List<Long> saleOutIds = new ArrayList<>();
        saleOutIds.add(saleOutId);
        int updateCount = saleCartMapper.updateByIdAndStatus(id, ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus())
                        .setFinalAuditTime(finalApproveTime)
                        .setFinalAuditUserId(finalApproveUserId));
        if (updateCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        if (saleQuoteProgressService != null) {
            saleQuoteProgressService.refreshByCartIds(Collections.singleton(id));
        }
        recordStatus(id, cart.getNo(), true);
        return saleOutIds;
    }

    private void createFreightFinanceDocuments(ErpSaleCartDO cart, LocalDateTime approveTime) {
        if (!FREIGHT_TYPE_CUSTOMER_ADVANCE.equals(cart.getFreightType())
                && !FREIGHT_TYPE_SELF_PAY.equals(cart.getFreightType())) {
            return;
        }
        validateFreightFinanceFields(cart);
        BigDecimal financeAmount = resolveFreightFinanceAmount(cart);
        ErpSaleCartFreightDraftCreateReqBO createReqBO = new ErpSaleCartFreightDraftCreateReqBO()
                .setCartId(cart.getId())
                .setCartNo(cart.getNo())
                .setBizTime(cart.getCartTime().toLocalDate())
                .setApproveTime(approveTime)
                .setCustomerId(cart.getCustomerId())
                .setSettleMethod(cart.getSettleMethod())
                .setAccountId(cart.getAccountId())
                .setDeptId(cart.getDeptId())
                .setHandlerId(cart.getSaleUserId())
                .setParty(cart.getLogisticsCompany())
                .setAmount(financeAmount);
        if (FREIGHT_TYPE_CUSTOMER_ADVANCE.equals(cart.getFreightType())) {
            receivableOtherService.createFromSaleCartFreight(createReqBO);
            payableExpenseService.createDraftFromSaleCartFreight(createReqBO);
        } else {
            payableExpenseService.createFromSaleCartFreight(createReqBO);
        }
    }

    private void validateFreightFinanceFields(ErpSaleCartDO cart) {
        if (!FREIGHT_TYPE_CUSTOMER_ADVANCE.equals(cart.getFreightType())
                && !FREIGHT_TYPE_SELF_PAY.equals(cart.getFreightType())) {
            return;
        }
        BigDecimal financeAmount = resolveFreightFinanceAmount(cart);
        if (financeAmount == null || financeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(SALE_CART_FREIGHT_AMOUNT_INVALID);
        }
        if (FREIGHT_TYPE_SELF_PAY.equals(cart.getFreightType())
                && (!StringUtils.hasText(cart.getSettleMethod())
                || cart.getAccountId() == null || cart.getSaleUserId() == null)) {
            throw exception(SALE_CART_SELF_PAY_INFO_REQUIRED);
        }
    }

    private BigDecimal resolveFreightFinanceAmount(ErpSaleCartDO cart) {
        BigDecimal feeAmount = resolveFeeAmount(cart.getFeeAmount(), cart.getOtherPrice());
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            return feeAmount;
        }
        return cart.getTotalFreight();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelFirstApproveSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        doCancelFirstApproveSaleCart(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSaleCartByTransferOutId(Long transferOutId) {
        ErpStockMoveDO observed = stockMoveService.getStockMove(transferOutId);
        if (observed == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(observed.getSourceType())) {
            throw exception(STOCK_MOVE_UNLOCK_NOT_CART_SOURCE);
        }
        if (observed.getSourceId() == null) {
            throw exception(STOCK_MOVE_UNLOCK_SOURCE_ID_MISSING);
        }
        if (observed.getTransferDirection() != null
                && !Integer.valueOf(TRANSFER_DIRECTION_OUT).equals(observed.getTransferDirection())) {
            throw exception(STOCK_MOVE_UNLOCK_NOT_TRANSFER_OUT);
        }
        if (!ErpAuditStatus.PROCESS.getStatus().equals(observed.getStatus())) {
            throw exception(STOCK_MOVE_UNLOCK_APPROVED);
        }
        ErpStockMoveOperationPermission observedPermission = stockMoveService.getUnlockCartPermission(
                observed, stockMoveService.getStockMoveItemListByMoveId(transferOutId));
        if (!Boolean.TRUE.equals(observedPermission.getAllowed())) {
            throw exception(STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);
        }
        // 拣货/送货提交先锁履约单，解锁沿用同一顺序，避免与送货完成自动审批形成反向锁等待。
        salePickDeliveryService.rollbackUnstartedSaleCartFulfillment(observed.getSourceId());
        // 履约单回退后，再按调拨审批顺序锁手推车、调拨和库存，并重新核对来源。
        ErpSaleCartDO cart = validateSaleCartForUpdate(observed.getSourceId());
        ErpStockMoveDO transferOut = stockMoveService.getStockMoveForUpdate(transferOutId);
        if (transferOut == null || !Objects.equals(observed.getSourceType(), transferOut.getSourceType())
                || !Objects.equals(observed.getSourceId(), transferOut.getSourceId())) {
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409,
                    "调拨来源已变化，请刷新后重试");
        }
        if (transferOut.getTransferDirection() != null
                && !Integer.valueOf(TRANSFER_DIRECTION_OUT).equals(transferOut.getTransferDirection())) {
            throw exception(STOCK_MOVE_UNLOCK_NOT_TRANSFER_OUT);
        }
        if (!ErpAuditStatus.PROCESS.getStatus().equals(transferOut.getStatus())) {
            throw exception(STOCK_MOVE_UNLOCK_APPROVED);
        }
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(transferOut.getSourceType())) {
            throw exception(STOCK_MOVE_UNLOCK_NOT_CART_SOURCE);
        }
        if (transferOut.getSourceId() == null) {
            throw exception(STOCK_MOVE_UNLOCK_SOURCE_ID_MISSING);
        }

        List<ErpStockMoveItemDO> transferOutItems = stockMoveService.getStockMoveItemsForUpdate(transferOutId);
        ErpStockMoveOperationPermission transferOutPermission = stockMoveService.getUnlockCartPermission(
                transferOut, transferOutItems);
        if (!Boolean.TRUE.equals(transferOutPermission.getAllowed())) {
            throw exception(STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);
        }

        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UNLOCK_STATUS_INVALID);
        }
        List<ErpStockMoveDO> relatedTransferOuts = new ArrayList<>(stockMoveService.getCartTransferOutListForUpdate(cart.getId()));
        if (relatedTransferOuts.stream().noneMatch(move -> Objects.equals(move.getId(), transferOutId))) {
            relatedTransferOuts.add(transferOut);
        }
        if (relatedTransferOuts.stream()
                .anyMatch(move -> ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus()))) {
            throw exception(STOCK_MOVE_UNLOCK_SOURCE_APPROVED_EXISTS);
        }
        boolean permissionDenied = relatedTransferOuts.stream()
                .filter(move -> !Objects.equals(move.getId(), transferOutId))
                .anyMatch(move -> {
            List<ErpStockMoveItemDO> moveItems = stockMoveService.getStockMoveItemsForUpdate(move.getId());
            ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(move, moveItems);
            return !Boolean.TRUE.equals(permission.getAllowed());
        });
        if (permissionDenied) {
            throw exception(STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);
        }
        doUnlockSaleCart(cart,
                "从调拨出库单解锁手推车，调拨出库单：" + transferOut.getNo()
                        + "/" + transferOut.getId() + "，销售手推车：" + cart.getNo() + "/" + cart.getId());
    }

    /**
     * 解锁手推车并回退到待开单确认，确保重新确认后重新生成调拨、拣货和送货任务。
     */
    private void doUnlockSaleCart(ErpSaleCartDO cart, String content) {
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UNLOCK_STATUS_INVALID);
        }
        Long cartId = cart.getId();
        validateNoFulfillmentOrder(cartId);
        stockMoveService.deleteUnapprovedTransferOutBySource(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        int updateCount = saleCartMapper.rollbackToPendingBillingConfirmForUnlock(cartId);
        if (updateCount != 1) {
            throw exception(SALE_CART_STATUS_CHANGED);
        }
        record(cartId, "解锁手推车", content, cart.getNo());
    }

    /**
     * 执行销售手推车撤销初审。
     */
    private void doCancelFirstApproveSaleCart(ErpSaleCartDO cart) {
        doCancelFirstApproveSaleCart(cart, "撤销初审",
                "撤销销售手推车初审，单据编号：" + cart.getNo());
    }

    private void doCancelFirstApproveSaleCart(ErpSaleCartDO cart, String action, String content) {
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_CANCEL_FIRST_APPROVE_FAIL);
        }
        Long cartId = cart.getId();
        validateNoFulfillmentOrder(cartId);
        stockMoveService.deleteUnapprovedTransferOutBySource(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cartId);
        int updateCount = saleCartMapper.cancelFirstApprove(cartId);
        if (updateCount != 1) {
            throw exception(SALE_CART_STATUS_CHANGED);
        }
        record(cartId, action, content, cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoFinalApproveAfterTransferOut(Long id) {
        DataPermissionUtils.executeIgnore(() ->
                doAutoFinalApproveAfterTransferOut(id, SecurityFrameworkUtils.getLoginUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoFinalApproveAfterTransferOut(Long id, Long finalApproveUserId) {
        DataPermissionUtils.executeIgnore(() -> doAutoFinalApproveAfterTransferOut(id, finalApproveUserId));
    }

    private void doAutoFinalApproveAfterTransferOut(Long id, Long finalApproveUserId) {
        // 串行化同一手推车的多张调拨出库审批，确保后到事务能看到先到事务已提交的审批状态。
        ErpSaleCartDO cart = saleCartMapper.selectByIdForUpdate(id);
        if (cart == null) {
            log.warn("调拨出库审批后自动终审未找到销售手推车，可能单据不存在、已删除或不属于当前租户，saleCartId={}", id);
            return;
        }
        if (ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            log.info("调拨出库审批后无需重复终审，销售手推车已生成销售单，saleCartId={}, cartNo={}", id, cart.getNo());
            return;
        }
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            log.warn("调拨出库审批后暂不自动终审，销售手推车状态不是初审，saleCartId={}, cartNo={}, status={}",
                    id, cart.getNo(), cart.getStatus());
            return;
        }
        Integer sourceType = ErpSaleBizSourceTypeEnum.CART.getType();
        if (hasUnapprovedCartTransfer(sourceType, id)) {
            log.info("调拨出库审批后暂不自动终审，销售手推车仍有未审核调拨出库单，saleCartId={}, cartNo={}",
                    id, cart.getNo());
            return;
        }
        if (!hasApprovedCartTransfer(sourceType, id)) {
            log.warn("调拨出库审批后暂不自动终审，销售手推车没有已审核调拨出库单，saleCartId={}, cartNo={}",
                    id, cart.getNo());
            return;
        }
        doFinalApproveSaleCart(id, finalApproveUserId, true);
        log.info("调拨出库审批后销售手推车自动终审成功，saleCartId={}, cartNo={}, finalApproveUserId={}",
                id, cart.getNo(), finalApproveUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoFinalApproveAfterDelivery(Long id, Long finalApproveUserId) {
        DataPermissionUtils.executeIgnore(() -> doAutoFinalApproveAfterDelivery(id, finalApproveUserId));
    }

    private void doAutoFinalApproveAfterDelivery(Long id, Long finalApproveUserId) {
        ErpSaleCartDO cart = saleCartMapper.selectByIdForUpdate(id);
        if (cart == null) {
            log.warn("送货完成后自动终审未找到销售手推车，saleCartId={}", id);
            return;
        }
        if (ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            return;
        }
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            log.warn("送货完成后暂不自动终审，销售手推车状态不是待终审，saleCartId={}, status={}",
                    id, cart.getStatus());
            return;
        }
        ErpSalePickDeliverySummaryRespVO fulfillment = salePickDeliveryService.getSummaryBySaleCartId(id);
        if (fulfillment == null || !ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(
                fulfillment.getDeliveryStatus())) {
            throw exception(SALE_CART_FULFILLMENT_NOT_COMPLETED);
        }
        doFinalApproveSaleCart(id, finalApproveUserId, true);
        log.info("送货完成后销售手推车自动终审成功，saleCartId={}, cartNo={}, finalApproveUserId={}",
                id, cart.getNo(), finalApproveUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(id);
        if (!ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_REJECT_FAIL);
        }
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            validateNoFulfillmentOrder(id);
            stockMoveService.deleteUnapprovedTransferOutBySource(ErpSaleBizSourceTypeEnum.CART.getType(), id);
            stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), id);
        }
        int updateCount = saleCartMapper.rejectToProcess(id, cart.getStatus());
        if (updateCount == 0) {
            throw exception(SALE_CART_REJECT_FAIL);
        }
        record(id, "驳回", "驳回销售手推车，单据编号：" + cart.getNo(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToQuote(ErpSaleCartConvertQuoteReqVO reqVO) {
        ErpSaleCartDO cart = validateSaleCartForUpdate(reqVO.getCartId());
        if (!isEditableBeforeBillingConfirm(cart)) {
            throw exception(SALE_CART_CONVERT_QUOTE_FAIL);
        }

        // 1. 查询所有子表行（整单转换）
        List<ErpSaleCartItemDO> allItems = currentCartItems(cart.getId());
        if (allItems.isEmpty()) {
            throw exception(SALE_CART_CONVERT_QUOTE_ITEMS_EMPTY);
        }

        // 2. 生成报价订单
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_QUOTE_NO_PREFIX);
        if (saleQuoteMapper.selectByNo(no) != null) {
            throw exception(SALE_QUOTE_NO_EXISTS);
        }
        ErpSaleQuoteDO quote = BeanUtils.toBean(cart, ErpSaleQuoteDO.class, in -> in
                .setId(null).setNo(no).setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setQuoteTime(cart.getCartTime())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cart.getId()).setSourceNo(cart.getNo()));
        List<ErpSaleQuoteItemDO> quoteItems = convertList(allItems, cartItem ->
                BeanUtils.toBean(cartItem, ErpSaleQuoteItemDO.class, item ->
                        item.setId(null).setQuoteId(null).setConvertedCount(BigDecimal.ZERO)));
        calculateQuoteTotalPrice(quote, quoteItems);
        saleDocumentDefaultService.fillCreateDefaults(quote);
        saleQuoteMapper.insert(quote);
        quoteItems.forEach(item -> item.setQuoteId(quote.getId()));
        saleQuoteItemMapper.insertBatch(quoteItems);

        // 3. 记录转换关系，先记录，再删除原手推车。
        saleConvertRecordMapper.insertBatch(buildCartToQuoteRecords(cart, allItems, quote, quoteItems));

        // 4. 删除原手推车主表 + 子表
        record(cart.getId(), "to quote",
                "sale cart converted to quote, no=" + cart.getNo() + ", target=" + quote.getNo(), cart.getNo());
        saleCartItemMapper.deleteByCartId(cart.getId());
        saleCartMapper.deleteById(cart.getId());
        saleConvertRecordMapper.deleteByTarget(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId());
        if (isQuoteSourceCart(cart) && saleQuoteProgressService != null) {
            saleQuoteProgressService.refreshQuoteProgress(cart.getSourceId());
        }

        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleCart(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> orderedIds = ids.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        List<ErpSaleCartDO> carts = orderedIds.stream().map(this::validateSaleCartForUpdate).collect(Collectors.toList());
        Set<Long> quoteIds = saleConvertRecordMapper.selectQuoteToCartListByCartIds(orderedIds).stream()
                .map(ErpSaleConvertRecordDO::getSourceId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        carts.forEach(cart -> {
            if (!isEditableBeforeBillingConfirm(cart)) {
                throw exception(SALE_CART_DELETE_FAIL_NOT_DRAFT, cart.getNo());
            }
        });
        carts.forEach(cart -> stockMoveService.deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId()));
        carts.forEach(cart -> stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId()));
        saleCartMapper.deleteBatchIds(orderedIds);
        orderedIds.forEach(saleCartItemMapper::deleteByCartId);
        orderedIds.forEach(id -> saleConvertRecordMapper.deleteByTarget(ErpSaleBizSourceTypeEnum.CART.getType(), id));
        if (saleQuoteProgressService != null) {
            quoteIds.forEach(saleQuoteProgressService::refreshQuoteProgress);
        }
        carts.forEach(cart -> recordDelete(cart.getId(), cart.getNo()));
    }

    private boolean isQuoteSourceCart(ErpSaleCartDO cart) {
        return cart != null && ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(cart.getSourceType())
                && cart.getSourceId() != null;
    }

    /**
     * 新建单据没有已保存身份，所有相同库存行都可直接归并。
     */
    private List<ErpSaleCartSaveReqVO.Item> mergeCreateItemRequests(List<ErpSaleCartSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return items;
        }
        Set<String> allKeys = items.stream().filter(Objects::nonNull).map(this::stockKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return mergeTouchedItemRequests(items, allKeys, Collections.emptyList(), items).items;
    }

    private Set<String> collectTouchedStockKeys(List<ErpSaleCartSaveReqVO.Item> requestItems,
                                                List<ErpSaleCartItemDO> oldItems,
                                                boolean incrementalItems) {
        if (CollUtil.isEmpty(requestItems)) {
            return Collections.emptySet();
        }
        Map<Long, ErpSaleCartItemDO> oldItemMap = convertMap(oldItems, ErpSaleCartItemDO::getId);
        Set<String> result = new LinkedHashSet<>();
        for (ErpSaleCartSaveReqVO.Item requestItem : requestItems) {
            if (requestItem == null) {
                continue;
            }
            String operation = requestItem.getOperation() == null ? "" : requestItem.getOperation().trim();
            if (incrementalItems && ErpSaleItemOperationHelper.OPERATION_DELETE.equalsIgnoreCase(operation)) {
                continue;
            }
            ErpSaleCartItemDO oldItem = requestItem.getId() == null ? null : oldItemMap.get(requestItem.getId());
            if (incrementalItems || oldItem == null || itemContentChanged(requestItem, oldItem)) {
                result.add(stockKey(requestItem));
                if (oldItem != null) {
                    result.add(stockKey(oldItem));
                }
            }
        }
        if (!incrementalItems && CollUtil.isNotEmpty(oldItems)) {
            Set<Long> retainedIds = requestItems.stream().filter(Objects::nonNull)
                    .map(ErpSaleCartSaveReqVO.Item::getId).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            oldItems.stream().filter(item -> item.getId() != null && !retainedIds.contains(item.getId()))
                    .map(this::stockKey).forEach(result::add);
        }
        return result;
    }

    private boolean itemContentChanged(ErpSaleCartSaveReqVO.Item requestItem, ErpSaleCartItemDO oldItem) {
        return !Objects.equals(stockKey(requestItem), stockKey(oldItem))
                || !sameDecimal(requestItem.getCount(), oldItem.getCount())
                || !sameDecimal(requestItem.getProductPrice(), oldItem.getProductPrice())
                || !Objects.equals(requestItem.getDeptId(), oldItem.getDeptId());
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private ItemMergeOutcome mergeTouchedItemRequests(List<ErpSaleCartSaveReqVO.Item> items,
                                                       Set<String> touchedStockKeys,
                                                       List<ErpSaleCartItemDO> oldItems,
                                                       List<ErpSaleCartSaveReqVO.Item> currentRequests) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(touchedStockKeys)) {
            return new ItemMergeOutcome(items == null ? Collections.emptyList() : items,
                    Collections.emptyMap(), Collections.emptySet());
        }
        Map<String, BigDecimal> currentPriceMap = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(currentRequests)) {
            Map<Long, ErpSaleCartItemDO> oldItemMap = convertMap(oldItems, ErpSaleCartItemDO::getId);
            boolean incrementalRequest = currentRequests.stream().filter(Objects::nonNull)
                    .anyMatch(item -> StringUtils.hasText(item.getOperation()));
            currentRequests.stream().filter(Objects::nonNull)
                    .filter(item -> !ErpSaleItemOperationHelper.OPERATION_DELETE.equalsIgnoreCase(item.getOperation()))
                    .filter(item -> !incrementalRequest
                            ? item.getId() == null || !oldItemMap.containsKey(item.getId())
                                    || itemContentChanged(item, oldItemMap.get(item.getId()))
                            : ErpSaleItemOperationHelper.OPERATION_INSERT.equalsIgnoreCase(item.getOperation())
                                    || ErpSaleItemOperationHelper.OPERATION_UPDATE.equalsIgnoreCase(item.getOperation()))
                    .filter(item -> item.getProductPrice() != null)
                    .forEach(item -> currentPriceMap.put(stockKey(item), item.getProductPrice()));
        }
        Map<String, BigDecimal> newestStoredPriceMap = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(oldItems)) {
            oldItems.stream().filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ErpSaleCartItemDO::getId,
                            Comparator.nullsFirst(Long::compareTo)))
                    .filter(item -> item.getProductPrice() != null)
                    .forEach(item -> newestStoredPriceMap.put(stockKey(item), item.getProductPrice()));
        }
        Map<String, List<ErpSaleCartSaveReqVO.Item>> groupMap = items.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::stockKey, LinkedHashMap::new, Collectors.toList()));
        List<ErpSaleCartSaveReqVO.Item> mergedItems = new ArrayList<>();
        Map<Long, Long> redirects = new LinkedHashMap<>();
        Set<Long> updateIds = new LinkedHashSet<>();
        groupMap.forEach((key, group) -> {
            if (!touchedStockKeys.contains(key) || group.size() == 1) {
                mergedItems.addAll(group);
                return;
            }
            ErpSaleCartSaveReqVO.Item survivor = group.stream().filter(item -> item.getId() != null)
                    .min(Comparator.comparing(ErpSaleCartSaveReqVO.Item::getId))
                    .orElse(group.get(0));
            ErpSaleCartSaveReqVO.Item merged = BeanUtils.toBean(survivor, ErpSaleCartSaveReqVO.Item.class);
            BigDecimal totalCount = group.stream().map(ErpSaleCartSaveReqVO.Item::getCount)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            merged.setCount(totalCount);
            BigDecimal finalPrice = currentPriceMap.containsKey(key)
                    ? currentPriceMap.get(key) : newestStoredPriceMap.get(key);
            if (finalPrice != null) {
                merged.setProductPrice(finalPrice);
            }
            merged.setOperation(null);
            if (merged.getId() != null) {
                updateIds.add(merged.getId());
                group.stream().map(ErpSaleCartSaveReqVO.Item::getId).filter(Objects::nonNull)
                        .filter(id -> !Objects.equals(id, merged.getId()))
                        .forEach(id -> redirects.put(id, merged.getId()));
            }
            mergedItems.add(merged);
        });
        return new ItemMergeOutcome(mergedItems, redirects, updateIds);
    }

    private String stockKey(ErpSaleCartSaveReqVO.Item item) {
        return stockKey(item.getProductId(), item.getWarehouseId(), item.getBatchNo(), item.getGiftFlag());
    }

    private String stockKey(ErpSaleCartItemDO item) {
        return stockKey(item.getProductId(), item.getWarehouseId(), item.getBatchNo(), item.getGiftFlag());
    }

    private String stockKey(Long productId, Long warehouseId, String batchNo, Boolean giftFlag) {
        return productId + "|" + warehouseId + "|" + normalizeBatchNo(batchNo) + "|"
                + (Boolean.TRUE.equals(giftFlag) ? 1 : 0);
    }

    private static final class ItemMergeOutcome {

        private final List<ErpSaleCartSaveReqVO.Item> items;
        private final Map<Long, Long> redirects;
        private final Set<Long> updateIds;

        private ItemMergeOutcome(List<ErpSaleCartSaveReqVO.Item> items, Map<Long, Long> redirects,
                                 Set<Long> updateIds) {
            this.items = items;
            this.redirects = redirects;
            this.updateIds = updateIds;
        }
    }

    private CartItemMergeOutcome mergeTouchedCartItems(List<ErpSaleCartItemDO> items,
                                                        Set<String> touchedKeys,
                                                        Set<Long> currentPriceItemIds) {
        Map<String, List<ErpSaleCartItemDO>> groupMap = items.stream().collect(Collectors.groupingBy(
                this::stockKey, LinkedHashMap::new, Collectors.toList()));
        List<ErpSaleCartItemDO> result = new ArrayList<>();
        Map<Long, Long> redirects = new LinkedHashMap<>();
        groupMap.forEach((key, group) -> {
            group.sort(Comparator.comparing(ErpSaleCartItemDO::getId, Comparator.nullsLast(Long::compareTo)));
            if (!touchedKeys.contains(key) || group.size() == 1) {
                result.addAll(group);
                return;
            }
            ErpSaleCartItemDO survivor = group.get(0);
            ErpSaleCartItemDO latest = group.get(group.size() - 1);
            BigDecimal totalCount = group.stream().map(ErpSaleCartItemDO::getCount)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            survivor.setCount(totalCount);
            ErpSaleCartItemDO currentPriceItem = group.stream()
                    .filter(item -> currentPriceItemIds.contains(item.getId())).reduce((left, right) -> right)
                    .orElse(null);
            BigDecimal finalPrice = currentPriceItem != null ? currentPriceItem.getProductPrice() : latest.getProductPrice();
            if (finalPrice != null) {
                survivor.setProductPrice(finalPrice);
            }
            survivor.setTotalPrice(ErpSaleDecimalUtils.multiply(survivor.getProductPrice(), totalCount));
            survivor.setTaxPrice(BigDecimal.ZERO);
            group.stream().skip(1).map(ErpSaleCartItemDO::getId).filter(Objects::nonNull)
                    .forEach(id -> redirects.put(id, survivor.getId()));
            result.add(survivor);
        });
        return new CartItemMergeOutcome(result, redirects);
    }

    private static final class CartItemMergeOutcome {

        private final List<ErpSaleCartItemDO> items;
        private final Map<Long, Long> redirects;

        private CartItemMergeOutcome(List<ErpSaleCartItemDO> items, Map<Long, Long> redirects) {
            this.items = items;
            this.redirects = redirects;
        }
    }

    private List<ErpSaleConvertRecordDO> validateQuoteSourceCartItems(ErpSaleCartDO cart,
                                                                      List<ErpSaleCartItemDO> oldItems,
                                                                      List<ErpSaleCartItemDO> finalItems) {
        if (!isQuoteSourceCart(cart)) {
            return Collections.emptyList();
        }
        List<ErpSaleConvertRecordDO> records = saleConvertRecordMapper
                .selectQuoteToCartListByCartIds(Collections.singleton(cart.getId()));
        if (CollUtil.isEmpty(records)) {
            return records;
        }
        Map<Long, ErpSaleCartItemDO> oldItemMap = convertMap(oldItems, ErpSaleCartItemDO::getId);
        Map<Long, ErpSaleCartItemDO> finalItemMap = finalItems.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ErpSaleCartItemDO::getId, item -> item));
        finalItemMap.keySet().stream().filter(id -> !oldItemMap.containsKey(id)).findFirst()
                .ifPresent(id -> {
                    throw exception(SALE_CART_ITEM_UPDATE_NOT_EXISTS);
                });
        // 与报价分批转手推车保持相同的“主表 -> 明细”加锁顺序，避免并发调整数量时锁顺序反转。
        saleQuoteMapper.selectByIdForUpdate(cart.getSourceId());
        Map<Long, ErpSaleQuoteItemDO> quoteItemMap = convertMap(
                saleQuoteItemMapper.selectListByQuoteIdForUpdate(cart.getSourceId()), ErpSaleQuoteItemDO::getId);
        List<ErpSaleConvertRecordDO> allQuoteRecords = saleConvertRecordMapper
                .selectQuoteToCartListByQuoteIds(Collections.singleton(cart.getSourceId()));
        Set<Long> activeCartIds = allQuoteRecords.stream().map(ErpSaleConvertRecordDO::getTargetId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(activeCartIds)) {
            activeCartIds = saleCartMapper.selectByIds(activeCartIds).stream()
                    .map(ErpSaleCartDO::getId).collect(Collectors.toSet());
        }
        Set<Long> finalActiveCartIds = activeCartIds;
        allQuoteRecords = allQuoteRecords.stream()
                .filter(record -> finalActiveCartIds.contains(record.getTargetId()))
                .collect(Collectors.toList());
        Map<Long, BigDecimal> totalBySourceItem = allQuoteRecords.stream()
                .collect(Collectors.groupingBy(ErpSaleConvertRecordDO::getSourceItemId,
                        Collectors.reducing(BigDecimal.ZERO,
                                record -> record.getCount() != null ? record.getCount() : BigDecimal.ZERO,
                                BigDecimal::add)));
        for (ErpSaleConvertRecordDO record : records) {
            ErpSaleCartItemDO oldItem = oldItemMap.get(record.getTargetItemId());
            ErpSaleCartItemDO finalItem = finalItemMap.get(record.getTargetItemId());
            if (oldItem == null) {
                throw exception(SALE_CART_ITEM_UPDATE_NOT_EXISTS);
            }
            if (finalItem != null && !sameQuoteSourceIdentity(oldItem, finalItem)) {
                throw exception(SALE_CART_QUOTE_SOURCE_IDENTITY_MODIFY_FAIL, oldItem.getId());
            }
            ErpSaleQuoteItemDO quoteItem = quoteItemMap.get(record.getSourceItemId());
            if (quoteItem == null) {
                throw exception(SALE_QUOTE_ITEM_NOT_EXISTS, record.getSourceItemId());
            }
            BigDecimal converted = totalBySourceItem.getOrDefault(record.getSourceItemId(), BigDecimal.ZERO);
            if (converted.compareTo(quoteItem.getCount()) > 0) {
                throw exception(SALE_CART_QUOTE_SOURCE_COUNT_EXCEED,
                        quoteItem.getId(), converted, quoteItem.getCount());
            }
        }
        return records;
    }

    private void validateSourceMinimumCount(ErpSaleCartDO cart, List<ErpSaleCartItemDO> finalItems) {
        List<ErpSaleConvertRecordDO> records = saleConvertRecordMapper
                .selectSourceToCartListByCartIds(Collections.singleton(cart.getId()));
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Map<Long, ErpSaleCartItemDO> finalItemMap = finalItems.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ErpSaleCartItemDO::getId, item -> item));
        Map<Long, BigDecimal> minimumCountMap = records.stream().filter(record -> record.getTargetItemId() != null)
                .collect(Collectors.groupingBy(ErpSaleConvertRecordDO::getTargetItemId,
                        Collectors.reducing(BigDecimal.ZERO,
                                record -> record.getCount() != null ? record.getCount() : BigDecimal.ZERO,
                                BigDecimal::add)));
        minimumCountMap.forEach((itemId, minimumCount) -> {
            ErpSaleCartItemDO finalItem = finalItemMap.get(itemId);
            if (finalItem == null || finalItem.getCount() == null
                    || finalItem.getCount().compareTo(minimumCount) < 0) {
                throw exception(SALE_CART_SOURCE_COUNT_BELOW_CONVERTED, itemId, minimumCount);
            }
        });
    }

    private boolean sameQuoteSourceIdentity(ErpSaleCartItemDO oldItem, ErpSaleCartItemDO finalItem) {
        return Objects.equals(oldItem.getProductId(), finalItem.getProductId())
                && Objects.equals(oldItem.getProductUnitId(), finalItem.getProductUnitId())
                && Objects.equals(oldItem.getWarehouseId(), finalItem.getWarehouseId())
                && Objects.equals(normalizeBatchNo(oldItem.getBatchNo()), normalizeBatchNo(finalItem.getBatchNo()))
                && Objects.equals(Boolean.TRUE.equals(oldItem.getGiftFlag()), Boolean.TRUE.equals(finalItem.getGiftFlag()));
    }

    private String normalizeBatchNo(String batchNo) {
        return batchNo == null ? "" : batchNo.trim();
    }

    private void applyFullItemUpdate(Long cartId, List<ErpSaleCartItemDO> oldItems,
                                     List<ErpSaleCartItemDO> finalItems) {
        Map<Long, ErpSaleCartItemDO> oldItemMap = convertMap(oldItems, ErpSaleCartItemDO::getId);
        Set<Long> finalIds = finalItems.stream().map(ErpSaleCartItemDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> deleteIds = oldItems.stream().map(ErpSaleCartItemDO::getId)
                .filter(id -> !finalIds.contains(id)).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deleteIds)) {
            saleCartItemMapper.deleteByIds(deleteIds);
        }
        List<ErpSaleCartItemDO> insertItems = finalItems.stream().filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertItems)) {
            insertItems.forEach(item -> item.setCartId(cartId));
            saleCartItemMapper.insertBatch(insertItems);
        }
        List<ErpSaleCartItemDO> updateItems = finalItems.stream().filter(item -> item.getId() != null)
                .filter(item -> !samePersistedItem(item, oldItemMap.get(item.getId())))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateItems)) {
            updateItems.forEach(item -> item.setCartId(cartId));
            saleCartItemMapper.updateBatch(updateItems);
        }
    }

    private boolean samePersistedItem(ErpSaleCartItemDO current, ErpSaleCartItemDO old) {
        if (old == null) {
            return false;
        }
        return Objects.equals(stockKey(current), stockKey(old))
                && Objects.equals(current.getDeptId(), old.getDeptId())
                && sameDecimal(current.getCount(), old.getCount())
                && sameDecimal(current.getProductPrice(), old.getProductPrice())
                && Objects.equals(current.getWarehousePosition(), old.getWarehousePosition())
                && Objects.equals(current.getDrawingNo(), old.getDrawingNo())
                && Objects.equals(current.getBarCode(), old.getBarCode())
                && Objects.equals(current.getBrand(), old.getBrand())
                && Objects.equals(current.getVehicleModel(), old.getVehicleModel())
                && Objects.equals(current.getOriginPlace(), old.getOriginPlace())
                && Objects.equals(current.getStandard(), old.getStandard())
                && Objects.equals(current.getRemark(), old.getRemark());
    }

    private void syncQuoteSourceConvertRecords(List<ErpSaleConvertRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Set<Long> quoteIds = new LinkedHashSet<>();
        records.forEach(record -> quoteIds.add(record.getSourceId()));
        if (saleQuoteProgressService != null) {
            quoteIds.forEach(saleQuoteProgressService::refreshQuoteProgress);
        }
    }

    private boolean isEditableBeforeBillingConfirm(ErpSaleCartDO cart) {
        if (cart == null) {
            return false;
        }
        return ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())
                || ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus().equals(cart.getStatus())
                || (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                    && cart.getBillingConfirmTime() == null);
    }

    private void updateStatus(Long id, Integer oldStatus, Integer newStatus, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        validateSaleCartForUpdate(id);
        boolean isFirstApproveAction = ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(oldStatus)
                && ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus);
        int updateCount = saleCartMapper.updateByIdAndStatus(id, oldStatus,
                new ErpSaleCartDO().setStatus(newStatus)
                        .setFirstAuditTime(isFirstApproveAction ? LocalDateTime.now() : null)
                        .setFirstAuditUserId(isFirstApproveAction ? SecurityFrameworkUtils.getLoginUserId() : null));
        if (updateCount == 0) {
            throw exception(errorCode);
        }
        ErpSaleCartDO cart = saleCartMapper.selectById(id);
        if (cart != null) {
            if (isFirstApproveAction) {
                recordStatus(id, cart.getNo(), true);
            } else if (ErpSaleCartStatusEnum.PENDING_BILLING_CONFIRM.getStatus().equals(newStatus)
                    || ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(newStatus)
                    || ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus)) {
                record(id, "提交", "提交销售手推车，单据编号：" + cart.getNo(), cart.getNo());
            }
        }
    }

    private void validateSaleCartForBillingConfirm(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(SALE_CART_ITEMS_EMPTY);
        }
        for (ErpSaleCartItemDO item : items) {
            if (item == null || item.getProductId() == null || item.getWarehouseId() == null) {
                throw exception(SALE_CART_ITEMS_EMPTY);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_CART_ITEM_COUNT_POSITIVE);
            }
            if (!Boolean.TRUE.equals(item.getGiftFlag())
                    && (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                throw exception(SALE_CART_ITEM_PRICE_POSITIVE);
            }
        }
        Set<Long> productIds = convertSet(items, ErpSaleCartItemDO::getProductId);
        List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(productIds));
        productBatchNoValidator.validateBatchNoAllowed(items, convertMap(products, ErpProductDO::getId),
                ErpSaleCartItemDO::getProductId, ErpSaleCartItemDO::getBatchNo);
        if (cart.getSourceType() == null) {
            customerService.validateCustomerForSale(cart.getCustomerId(), cart.getDeptId());
        } else {
            customerService.validateCustomerForGeneratedSale(cart.getCustomerId(), cart.getDeptId());
        }
        if (cart.getAccountId() != null) {
            accountService.validateAccount(cart.getAccountId());
        }
        if (cart.getSaleUserId() != null) {
            adminUserApi.validateUser(cart.getSaleUserId());
        }
        validateFreightFinanceFields(cart);
    }

    private ErpSaleOutSaveReqVO buildSaleOutReqVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setCustomerId(cart.getCustomerId());
        reqVO.setAccountId(cart.getAccountId());
        reqVO.setSaleUserId(cart.getSaleUserId());
        reqVO.setDeptId(cart.getDeptId());
        reqVO.setOutTime(cart.getCartTime());
        reqVO.setDiscountPercent(cart.getDiscountPercent());
        reqVO.setFeeAmount(cart.getFeeAmount());
        reqVO.setOtherPrice(cart.getOtherPrice());
        reqVO.setDeliveryMethod(cart.getDeliveryMethod());
        reqVO.setVin(cart.getVin());
        reqVO.setRemark(cart.getRemark());
        reqVO.setItems(convertList(items, item -> {
            ErpSaleOutSaveReqVO.Item outItem = new ErpSaleOutSaveReqVO.Item();
            outItem.setWarehouseId(item.getWarehouseId());
            outItem.setDeptId(item.getDeptId());
            outItem.setSourceWarehouseId(item.getSourceWarehouseId());
            outItem.setSourceDeptId(item.getSourceDeptId());
            outItem.setProductId(item.getProductId());
            outItem.setProductUnitId(item.getProductUnitId());
            outItem.setUnitWeight(item.getWeight());
            outItem.setPackageQty(item.getPackageQty());
            outItem.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO : item.getProductPrice());
            outItem.setCount(item.getCount());

            outItem.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            outItem.setBatchNo(item.getBatchNo());
            outItem.setRemark(item.getRemark());
            return outItem;
        }));
        return reqVO;
    }

    private Long resolveSaleDeptId(Long deptId) {
        return deptId != null ? deptId : SecurityFrameworkUtils.getLoginUserDeptId();
    }

    private List<ErpSaleCartItemDO> validateSaleCartItems(List<ErpSaleCartSaveReqVO.Item> list, Long saleDeptId) {
        validateSaleCartItemRequiredFields(list);
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleCartSaveReqVO.Item::getProductId, ErpSaleCartSaveReqVO.Item::getBatchNo);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleSelectableWarehouseListForDept(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId), saleDeptId,
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION), ErpWarehouseDO::getId);
        Map<String, ErpStockDO> stockMap = getStockMapIgnoreDataPermission(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId),
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId));
        // 校验价格
        list.forEach(item -> {
            if (!Boolean.TRUE.equals(item.getGiftFlag())
                    && (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                throw exception(SALE_CART_ITEM_PRICE_POSITIVE);
            }
        });
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            fillDeptIdFromSaleDept(item, saleDeptId, warehouseMap);
            validateSaleDeptWarehousePermission(item, saleDeptId);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(ErpSaleDecimalUtils.multiply(item.getProductPrice(), item.getCount()));
            ErpStockDO stock = stockMap.get(buildProductWarehouseMapKey(item.getProductId(), item.getWarehouseId()));
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
        }));
    }

    private List<ErpSaleCartItemDO> validateSaleCartDraftItems(List<ErpSaleCartSaveReqVO.Item> list, Long saleDeptId) {
        validateSaleCartItemRequiredFields(list);
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleCartSaveReqVO.Item::getProductId, ErpSaleCartSaveReqVO.Item::getBatchNo);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleSelectableWarehouseListForDept(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId), saleDeptId,
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION), ErpWarehouseDO::getId);
        Map<String, ErpStockDO> stockMap = getStockMapIgnoreDataPermission(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId),
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId));
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            fillDeptIdFromSaleDept(item, saleDeptId, warehouseMap);
            validateSaleDeptWarehousePermission(item, saleDeptId);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())
                    || item.getProductPrice() == null
                    || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(ErpSaleDecimalUtils.multiply(item.getProductPrice(), item.getCount()));
            ErpStockDO stock = stockMap.get(buildProductWarehouseMapKey(item.getProductId(), item.getWarehouseId()));
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
        }));
    }

    private void validateSaleCartItemRequiredFields(List<ErpSaleCartSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            throw exception(SALE_CART_ITEMS_EMPTY);
        }
        for (ErpSaleCartSaveReqVO.Item item : list) {
            if (item == null || item.getProductId() == null || item.getWarehouseId() == null) {
                throw exception(SALE_CART_ITEMS_EMPTY);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_CART_ITEM_COUNT_POSITIVE);
            }
        }
    }

    private List<ErpSaleCartSaveReqVO.Item> filterDraftItems(List<ErpSaleCartSaveReqVO.Item> itemReqs) {
        if (CollUtil.isEmpty(itemReqs)) {
            return Collections.emptyList();
        }
        return itemReqs.stream()
                .filter(item -> item != null && item.getProductId() != null
                        && item.getWarehouseId() != null && item.getCount() != null
                        && item.getCount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private void fillDeptIdFromSaleDept(ErpSaleCartItemDO item, Long saleDeptId, Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpWarehouseDO warehouse = item.getWarehouseId() == null ? null : warehouseMap.get(item.getWarehouseId());
        if (warehouse != null && !warehouseService.isWarehouseSaleAllowedForDept(warehouse.getId(), saleDeptId)) {
            item.setDeptId(warehouse.getDeptId());
            return;
        }
        if (item.getDeptId() == null || (saleDeptId != null && warehouse != null
                && Objects.equals(item.getDeptId(), warehouse.getDeptId())
                && !Objects.equals(saleDeptId, warehouse.getDeptId()))) {
            item.setDeptId(saleDeptId);
        }
    }

    private void fillProductWeightAndPackage(ErpSaleCartItemDO item, ErpProductDO product) {
        if (product == null) {
            return;
        }
        if (item.getWeight() == null) {
            item.setWeight(product.getWeight());
        }
        if (item.getPackageQty() == null) {
            item.setPackageQty(product.getPackageQty());
        }
    }

    private void normalizeItemDeptIdByCartDept(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (cart == null || cart.getDeptId() == null || CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleSelectableWarehouseListForDept(
                convertSet(items, ErpSaleCartItemDO::getWarehouseId), cart.getDeptId(),
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION), ErpWarehouseDO::getId);
        items.forEach(item -> {
            fillDeptIdFromSaleDept(item, cart.getDeptId(), warehouseMap);
            validateSaleDeptWarehousePermission(item, cart.getDeptId());
        });
    }

    private void validateSaleDeptWarehousePermission(ErpSaleCartItemDO item, Long saleDeptId) {
        if (item.getDeptId() == null) {
            throw exception(SALE_WAREHOUSE_DEPT_REQUIRED);
        }
        warehouseService.validateWarehouseSaleSelectableForDept(item.getWarehouseId(),
                saleDeptId != null ? saleDeptId : item.getDeptId(),
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION);
    }

    private void createTransferOutDraft(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ErpWarehouseDO> sourceWarehouseMap = getCartSourceWarehouseMap(cart, items);
        List<ErpSaleCartItemDO> transferItems = getCrossDeptItems(cart, items, sourceWarehouseMap);
        if (CollUtil.isEmpty(transferItems)) {
            deleteRedundantUnapprovedTransferOut(cart);
            salePickDeliveryService.generateForSaleCart(cart.getId());
            return;
        }
        Long directWarehouseId = warehouseService.resolveDirectWarehouseId(cart.getDeptId());
        Map<Long, List<ErpSaleCartItemDO>> deptItemMap = transferItems.stream().collect(Collectors.groupingBy(
                item -> sourceWarehouseMap.get(item.getWarehouseId()).getDeptId(),
                LinkedHashMap::new, Collectors.toList()));
        LocalDateTime moveTime = LocalDateTime.now();
        List<ErpStockMoveSaveReqVO> reqVOs = new ArrayList<>(deptItemMap.size());
        deptItemMap.forEach((fromDeptId, deptItems) -> reqVOs.add(buildTransferOutDraftReqVO(
                cart, deptItems, fromDeptId, directWarehouseId, moveTime)));
        stockMoveService.syncTransferOutDraftsBySource(reqVOs);
        salePickDeliveryService.generateForSaleCart(cart.getId());
    }

    private void validateNoFulfillmentOrder(Long cartId) {
        if (salePickDeliveryService.getSummaryBySaleCartId(cartId) != null) {
            throw exception(SALE_CART_FULFILLMENT_ROLLBACK_FORBIDDEN);
        }
    }

    private ErpStockMoveSaveReqVO buildTransferOutDraftReqVO(ErpSaleCartDO cart,
                                                              List<ErpSaleCartItemDO> transferItems,
                                                              Long fromDeptId, Long directWarehouseId,
                                                              LocalDateTime moveTime) {
        ErpStockMoveSaveReqVO reqVO = new ErpStockMoveSaveReqVO();
        reqVO.setDeptId(cart.getDeptId());
        reqVO.setFromDeptId(fromDeptId);
        reqVO.setToDeptId(cart.getDeptId());
        reqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        reqVO.setMoveTime(moveTime);
        reqVO.setSourceType(ErpSaleBizSourceTypeEnum.CART.getType());
        reqVO.setSourceId(cart.getId());
        reqVO.setSourceNo(cart.getNo());
        reqVO.setRemark("销售手推车初审自动生成调拨出仓单，调入仓库为销售部门对应的直发仓");
        Map<String, ErpSaleCartItemDO> mergedItems = new LinkedHashMap<>();
        transferItems.forEach(item -> {
            String key = item.getProductId() + "-" + item.getWarehouseId();
            ErpSaleCartItemDO merged = mergedItems.get(key);
            if (merged == null) {
                mergedItems.put(key, BeanUtils.toBean(item, ErpSaleCartItemDO.class));
                return;
            }
            merged.setCount((merged.getCount() == null ? BigDecimal.ZERO : merged.getCount())
                    .add(item.getCount() == null ? BigDecimal.ZERO : item.getCount()));
        });
        reqVO.setItems(convertList(mergedItems.values(), item -> {
            ErpStockMoveSaveReqVO.Item moveItem = new ErpStockMoveSaveReqVO.Item();
            moveItem.setFromWarehouseId(item.getWarehouseId());
            moveItem.setToWarehouseId(directWarehouseId);
            moveItem.setFromDeptId(fromDeptId);
            moveItem.setToDeptId(cart.getDeptId());
            moveItem.setProductId(item.getProductId());
            moveItem.setProductPrice(item.getProductPrice());
            moveItem.setCount(item.getCount());
            moveItem.setRemark(item.getRemark());
            return moveItem;
        }));
        return reqVO;
    }

    private void lockSaleCartStock(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        warehouseService.validSaleWarehouseList(convertSet(items, ErpSaleCartItemDO::getWarehouseId));
        List<ErpSaleCartItemDO> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing(ErpSaleCartItemDO::getProductId)
                .thenComparing(ErpSaleCartItemDO::getWarehouseId).thenComparing(ErpSaleCartItemDO::getId));
        ordered.forEach(item -> {
            stockLockService.lockStock(item.getProductId(), item.getWarehouseId(), item.getCount(),
                    ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), item.getId(), cart.getNo());
        });
    }

    private List<ErpSaleCartItemDO> resolveFinalSaleItems(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return items;
        }
        Map<Long, ErpWarehouseDO> sourceWarehouseMap = getCartSourceWarehouseMap(cart, items);
        List<ErpSaleCartItemDO> crossDeptItems = getCrossDeptItems(cart, items, sourceWarehouseMap);
        if (CollUtil.isEmpty(crossDeptItems)) {
            deleteRedundantUnapprovedTransferOut(cart);
            return items;
        }
        Integer sourceType = ErpSaleBizSourceTypeEnum.CART.getType();
        if (hasUnapprovedCartTransfer(sourceType, cart.getId())) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
        List<ErpStockMoveDO> approvedTransfers = stockMoveService.getCartTransferOutListForUpdate(cart.getId())
                .stream()
                .filter(move -> ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(approvedTransfers)) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
        List<ErpStockMoveItemDO> moveItems = approvedTransfers.stream()
                .flatMap(move -> stockMoveService.getStockMoveItemsForUpdate(move.getId()).stream())
                .collect(Collectors.toList());
        Map<String, BigDecimal> expectedCountMap = aggregateCartItemCounts(crossDeptItems);
        Map<String, BigDecimal> actualCountMap = aggregateMoveItemCounts(moveItems);
        Map<String, Long> targetWarehouseMap = resolveMoveTargetWarehouseMap(moveItems);
        if (!countMapEquals(expectedCountMap, actualCountMap)
                || !targetWarehouseMap.keySet().equals(expectedCountMap.keySet())) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
        validateTransferTargetWarehouses(cart, targetWarehouseMap.values());
        return convertList(items, item -> {
            ErpWarehouseDO sourceWarehouse = sourceWarehouseMap.get(item.getWarehouseId());
            if (!isCrossDeptWarehouse(cart.getDeptId(), sourceWarehouse)) {
                return item;
            }
            Long targetWarehouseId = targetWarehouseMap.get(buildTransferItemKey(
                    item.getProductId(), item.getWarehouseId()));
            if (targetWarehouseId == null) {
                throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
            }
            return BeanUtils.toBean(item, ErpSaleCartItemDO.class,
                    finalItem -> finalItem.setWarehouseId(targetWarehouseId)
                            .setDeptId(cart.getDeptId())
                            .setSourceWarehouseId(item.getWarehouseId())
                            .setSourceDeptId(sourceWarehouse.getDeptId()));
        });
    }

    private void deleteRedundantUnapprovedTransferOut(ErpSaleCartDO cart) {
        Integer sourceType = ErpSaleBizSourceTypeEnum.CART.getType();
        if (!hasUnapprovedCartTransfer(sourceType, cart.getId())
                || hasApprovedCartTransfer(sourceType, cart.getId())) {
            return;
        }
        stockMoveService.deleteUnapprovedTransferOutBySource(sourceType, cart.getId());
    }

    private void validateTransferTargetWarehouses(ErpSaleCartDO cart, Collection<Long> targetWarehouseIds) {
        Set<Long> distinctTargetWarehouseIds = targetWarehouseIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ErpWarehouseDO> targetWarehouseMap = convertMap(
                warehouseService.validSaleWarehouseListForDept(distinctTargetWarehouseIds, cart.getDeptId()),
                ErpWarehouseDO::getId);
        boolean invalidTarget = targetWarehouseMap.size() != distinctTargetWarehouseIds.size()
                || targetWarehouseMap.values().stream()
                .anyMatch(warehouse -> !Objects.equals(cart.getDeptId(), warehouse.getDeptId()));
        if (invalidTarget) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
    }

    private Map<Long, ErpWarehouseDO> getCartSourceWarehouseMap(ErpSaleCartDO cart,
                                                                List<ErpSaleCartItemDO> items) {
        return convertMap(warehouseService.validSaleSelectableWarehouseListForDept(
                convertSet(items, ErpSaleCartItemDO::getWarehouseId), cart.getDeptId(),
                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION), ErpWarehouseDO::getId);
    }

    private List<ErpSaleCartItemDO> getCrossDeptItems(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items,
                                                      Map<Long, ErpWarehouseDO> sourceWarehouseMap) {
        return items.stream()
                .filter(item -> isCrossDeptWarehouse(cart.getDeptId(), sourceWarehouseMap.get(item.getWarehouseId())))
                .collect(Collectors.toList());
    }

    private boolean isCrossDeptWarehouse(Long saleDeptId, ErpWarehouseDO warehouse) {
        return saleDeptId != null && warehouse != null && warehouse.getDeptId() != null
                && !Objects.equals(saleDeptId, warehouse.getDeptId());
    }

    private Map<String, BigDecimal> aggregateCartItemCounts(List<ErpSaleCartItemDO> items) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        items.forEach(item -> result.merge(buildTransferItemKey(item.getProductId(), item.getWarehouseId()),
                item.getCount(), BigDecimal::add));
        return result;
    }

    private Map<String, BigDecimal> aggregateMoveItemCounts(List<ErpStockMoveItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        items.forEach(item -> result.merge(buildTransferItemKey(item.getProductId(), item.getFromWarehouseId()),
                item.getCount(), BigDecimal::add));
        return result;
    }

    private Map<String, Long> resolveMoveTargetWarehouseMap(List<ErpStockMoveItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        items.forEach(item -> {
            String key = buildTransferItemKey(item.getProductId(), item.getFromWarehouseId());
            Long existingTarget = result.putIfAbsent(key, item.getToWarehouseId());
            if (item.getToWarehouseId() == null
                    || (existingTarget != null && !Objects.equals(existingTarget, item.getToWarehouseId()))) {
                throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
            }
        });
        return result;
    }

    private boolean countMapEquals(Map<String, BigDecimal> expected, Map<String, BigDecimal> actual) {
        if (!expected.keySet().equals(actual.keySet())) {
            return false;
        }
        return expected.entrySet().stream().allMatch(entry -> actual.get(entry.getKey()) != null
                && entry.getValue() != null && entry.getValue().compareTo(actual.get(entry.getKey())) == 0);
    }

    private String buildTransferItemKey(Long productId, Long warehouseId) {
        return productId + "-" + warehouseId;
    }

    private void validateStockEnoughRealtime(List<ErpSaleCartItemDO> items) {
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = calculateStockShortages(items, true);
        if (CollUtil.isNotEmpty(shortages)) {
            throw buildStockShortageException(shortages);
        }
    }

    private void validateStockEnoughRealtime(List<ErpSaleCartItemDO> items, boolean occupiedIncludesCurrentCart) {
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = calculateStockShortages(items,
                occupiedIncludesCurrentCart);
        if (CollUtil.isNotEmpty(shortages)) {
            throw buildStockShortageException(shortages);
        }
    }

    private void validateStockEnoughRealtime(List<ErpSaleCartItemDO> items, boolean occupiedIncludesCurrentCart,
                                             Long saleDeptId) {
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = calculateStockShortages(items,
                occupiedIncludesCurrentCart, saleDeptId);
        if (CollUtil.isNotEmpty(shortages)) {
            throw buildStockShortageException(shortages);
        }
    }

    private ErpSaleCartSubmitRespVO buildSubmitResult(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        ErpSaleCartSubmitRespVO result = new ErpSaleCartSubmitRespVO();
        result.setId(cart.getId());
        result.setNo(cart.getNo());
        result.setStatus(cart.getStatus());
        boolean occupiedIncludesCurrentCart = ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())
                || (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                    && cart.getBillingConfirmTime() == null);
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = calculateStockShortages(items, occupiedIncludesCurrentCart);
        result.setShortageItems(shortages);
        result.setStockInsufficient(CollUtil.isNotEmpty(shortages));
        return result;
    }

    private ServiceException buildStockShortageException(List<ErpSaleCartSubmitRespVO.ShortageItem> shortages) {
        StringBuilder message = new StringBuilder("库存不足，无法提交：");
        int displayCount = Math.min(shortages.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            ErpSaleCartSubmitRespVO.ShortageItem shortage = shortages.get(i);
            if (i > 0) {
                message.append("；");
            }
            message.append(defaultIfBlank(shortage.getProductName(), shortage.getProductCode(), shortage.getProductId()))
                    .append(" / ").append(defaultIfBlank(shortage.getWarehouseName(), null, shortage.getWarehouseId()))
                    .append("，需要=").append(formatCount(shortage.getRequiredCount()))
                    .append("，库存=").append(formatCount(shortage.getStockCount()))
                    .append("，缺少=").append(formatCount(shortage.getShortageCount()));
        }
        if (shortages.size() > displayCount) {
            message.append("；共 ").append(shortages.size()).append(" 条库存不足明细");
        }
        return new ServiceException(STOCK_COUNT_NEGATIVE2.getCode(), message.toString());
    }

    private String defaultIfBlank(String first, String second, Long id) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        if (second != null && !second.isEmpty()) {
            return second;
        }
        return String.valueOf(id);
    }

    private String formatCount(BigDecimal count) {
        return count != null ? count.stripTrailingZeros().toPlainString() : "0";
    }

    private List<ErpSaleCartSubmitRespVO.ShortageItem> calculateStockShortages(
            List<ErpSaleCartItemDO> items, boolean occupiedIncludesCurrentCart) {
        return calculateStockShortages(items, occupiedIncludesCurrentCart, null);
    }

    private List<ErpSaleCartSubmitRespVO.ShortageItem> calculateStockShortages(
            List<ErpSaleCartItemDO> items, boolean occupiedIncludesCurrentCart, Long saleDeptId) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Set<Long> warehouseIds = convertSet(items, ErpSaleCartItemDO::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                saleDeptId != null
                        ? warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, saleDeptId,
                                ErpWarehouseService.SALE_CART_ALL_PRODUCT_PERMISSION)
                        : warehouseService.validSaleWarehouseList(warehouseIds),
                ErpWarehouseDO::getId);
        Map<ProductWarehouseKey, BigDecimal> requiredCountMap = new LinkedHashMap<>();
        items.forEach(item -> {
            ProductWarehouseKey key = new ProductWarehouseKey(item.getProductId(), item.getWarehouseId());
            requiredCountMap.merge(key, item.getCount() != null ? item.getCount() : BigDecimal.ZERO, BigDecimal::add);
        });
        if (requiredCountMap.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = requiredCountMap.keySet().stream().map(ProductWarehouseKey::getProductId).collect(Collectors.toSet());
        Map<String, BigDecimal> occupiedCountMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getOccupiedCountMap(productIds, warehouseIds));
        Map<String, ErpStockDO> stockMap = getStockMapIgnoreDataPermission(productIds, warehouseIds);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = new ArrayList<>();
        requiredCountMap.forEach((key, requiredCount) -> {
            ErpStockDO stock = stockMap.get(buildProductWarehouseMapKey(key.getProductId(), key.getWarehouseId()));
            BigDecimal stockCount = stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal occupiedCount = occupiedCountMap.getOrDefault(
                    key.getProductId() + "_" + key.getWarehouseId(), BigDecimal.ZERO);
            // SUBMITTED/FIRST_APPROVE carts are part of occupiedCount. Exclude the current cart before
            // comparing its requested quantity, otherwise the current quantity is deducted twice.
            BigDecimal existingOccupiedCount = occupiedIncludesCurrentCart
                    ? occupiedCount.subtract(requiredCount).max(BigDecimal.ZERO) : occupiedCount;
            BigDecimal availableCount = stockCount.subtract(existingOccupiedCount);
            if (availableCount.compareTo(requiredCount) >= 0) {
                return;
            }
            ErpProductRespVO product = productMap.get(key.getProductId());
            ErpWarehouseDO warehouse = warehouseMap.get(key.getWarehouseId());
            ErpSaleCartSubmitRespVO.ShortageItem shortage = new ErpSaleCartSubmitRespVO.ShortageItem();
            shortage.setProductId(key.getProductId());
            shortage.setProductCode(product != null ? product.getCode() : null);
            shortage.setProductName(product != null ? product.getName() : null);
            shortage.setWarehouseId(key.getWarehouseId());
            shortage.setWarehouseName(warehouse != null ? warehouse.getName() : null);
            shortage.setRequiredCount(requiredCount);
            shortage.setStockCount(availableCount);
            shortage.setShortageCount(requiredCount.subtract(availableCount));
            shortages.add(shortage);
        });
        return shortages;
    }

    private ErpStockDO getStockIgnoreDataPermission(Long productId, Long warehouseId) {
        if (stockService == null) {
            return null;
        }
        return DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
    }

    private Map<String, ErpStockDO> getStockMapIgnoreDataPermission(Collection<Long> productIds,
                                                                    Collection<Long> warehouseIds) {
        if (stockService == null) {
            return Collections.emptyMap();
        }
        Map<String, ErpStockDO> stockMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getStockMap(productIds, warehouseIds));
        return stockMap != null ? stockMap : Collections.emptyMap();
    }

    private String buildProductWarehouseMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

    private void calculateTotalPrice(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        cart.setTotalCount(getSumValue(items, ErpSaleCartItemDO::getCount, BigDecimal::add));
        cart.setTotalProductPrice(getSumValue(items, ErpSaleCartItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        cart.setTotalTaxPrice(BigDecimal.ZERO);
        cart.setTotalPrice(cart.getTotalProductPrice());
        if (cart.getDiscountPercent() == null) {
            cart.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(cart.getFeeAmount(), cart.getOtherPrice());
        cart.setFeeAmount(feeAmount);
        cart.setOtherPrice(feeAmount);
        cart.setDiscountPrice(ErpSaleDecimalUtils.multiplyPercent(cart.getTotalPrice(), cart.getDiscountPercent()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(cart.getDiscountPrice()).add(feeAmount));
    }

    private void calculateQuoteTotalPrice(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        quote.setTotalCount(getSumValue(items, ErpSaleQuoteItemDO::getCount, BigDecimal::add));
        quote.setTotalProductPrice(getSumValue(items, ErpSaleQuoteItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        quote.setTotalTaxPrice(BigDecimal.ZERO);
        quote.setTotalPrice(quote.getTotalProductPrice());
        if (quote.getDiscountPercent() == null) {
            quote.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(quote.getFeeAmount(), quote.getOtherPrice());
        quote.setFeeAmount(feeAmount);
        quote.setOtherPrice(feeAmount);
        quote.setDiscountPrice(ErpSaleDecimalUtils.multiplyPercent(quote.getTotalPrice(), quote.getDiscountPercent()));
        quote.setTotalPrice(quote.getTotalPrice().subtract(quote.getDiscountPrice()).add(feeAmount));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : (otherPrice != null ? otherPrice : BigDecimal.ZERO);
    }

    private List<ErpSaleConvertRecordDO> buildCartToQuoteRecords(ErpSaleCartDO cart, List<ErpSaleCartItemDO> cartItems,
                                                                 ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> quoteItems) {
        return convertList(cartItems, cartItem -> {
            ErpSaleQuoteItemDO targetItem = quoteItems.stream()
                    .filter(item -> cartItem.getProductId().equals(item.getProductId())
                            && cartItem.getWarehouseId().equals(item.getWarehouseId())
                            && Boolean.TRUE.equals(cartItem.getGiftFlag()) == Boolean.TRUE.equals(item.getGiftFlag())
                            && cartItem.getCount().compareTo(item.getCount()) == 0)
                    .findFirst().orElse(null);
            return new ErpSaleConvertRecordDO()
                    .setConvertType(ErpSaleConvertTypeEnum.CART_TO_QUOTE.getType())
                    .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                    .setSourceId(cart.getId()).setSourceNo(cart.getNo()).setSourceItemId(cartItem.getId())
                    .setTargetType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                    .setTargetId(quote.getId()).setTargetNo(quote.getNo()).setTargetItemId(targetItem != null ? targetItem.getId() : null)
                    .setProductId(cartItem.getProductId()).setWarehouseId(cartItem.getWarehouseId()).setCount(cartItem.getCount());
        });
    }

    private boolean hasUnapprovedCartTransfer(Integer sourceType, Long cartId) {
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(sourceType)) {
            throw new IllegalArgumentException("必须使用手推车来源");
        }
        return stockMoveService.getCartTransferOutListForUpdate(cartId).stream()
                .anyMatch(move -> !ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus()));
    }

    private boolean hasApprovedCartTransfer(Integer sourceType, Long cartId) {
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(sourceType)) {
            throw new IllegalArgumentException("必须使用手推车来源");
        }
        return stockMoveService.getCartTransferOutListForUpdate(cartId).stream()
                .anyMatch(move -> ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus()));
    }

    private ErpSaleCartDO validateSaleCartExists(Long id) {
        ErpSaleCartDO cart = saleCartMapper.selectById(id);
        if (cart == null) {
            throw exception(SALE_CART_NOT_EXISTS);
        }
        return cart;
    }

    private ErpSaleCartDO validateSaleCartForUpdate(Long id) {
        ErpSaleCartDO cart = saleCartMapper.selectByIdForUpdate(id);
        if (cart == null) {
            throw exception(SALE_CART_NOT_EXISTS);
        }
        return cart;
    }

    private List<ErpSaleCartItemDO> currentCartItems(Long id) {
        List<ErpSaleCartItemDO> items = new ArrayList<>(saleCartItemMapper.selectListByCartIdForUpdate(id));
        items.sort(Comparator.comparing(ErpSaleCartItemDO::getId,
                Comparator.nullsLast(Long::compareTo)));
        return items;
    }

    @Override
    public ErpSaleCartDO getSaleCart(Long id) {
        return saleCartMapper.selectById(id);
    }

    @Override
    public ErpSaleCartDO getLatestOnlineOrder() {
        return saleCartMapper.selectLatestOnlineOrder();
    }

    @Override
    public PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO) {
        return saleCartMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpSaleCartSummaryRespVO getSaleCartSummary(ErpSaleCartPageReqVO pageReqVO) {
        return saleCartMapper.selectSummary(pageReqVO);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId) {
        return saleCartItemMapper.selectListByCartId(cartId);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId, String orderField,
                                                               String orderDirection) {
        return saleCartItemMapper.selectListByCartId(cartId, orderField, orderDirection);
    }

    @Override
    public PageResult<ErpSaleCartItemDO> getSaleCartItemPage(ErpSaleCartItemPageReqVO pageReqVO) {
        validateSaleCartExists(pageReqVO.getCartId());
        if (CollUtil.isNotEmpty(pageReqVO.getExcludedItemIds())) {
            pageReqVO.setExcludedItemIds(pageReqVO.getExcludedItemIds().stream()
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList()));
        }
        return saleCartItemMapper.selectPageByCartId(pageReqVO);
    }

    @Override
    public ErpSaleCartItemMatchRespVO getSaleCartItemMatches(ErpSaleCartItemMatchReqVO reqVO) {
        validateSaleCartExists(reqVO.getCartId());
        LinkedHashMap<String, ErpSaleCartItemMatchReqVO.Key> requestedKeyMap = new LinkedHashMap<>();
        reqVO.getKeys().forEach(key -> requestedKeyMap.put(stockKey(key.getProductId(), key.getWarehouseId(),
                key.getBatchNo(), key.getGiftFlag()), key));
        List<ErpSaleCartItemDO> matchedItems = saleCartItemMapper.selectListByCartIdAndStockKeys(
                reqVO.getCartId(), requestedKeyMap.values());
        Map<String, List<ErpSaleCartItemDO>> itemGroupMap = matchedItems.stream()
                .collect(Collectors.groupingBy(this::stockKey, LinkedHashMap::new, Collectors.toList()));
        ErpSaleCartItemMatchRespVO result = new ErpSaleCartItemMatchRespVO();
        List<ErpSaleCartItemMatchRespVO.Match> matches = new ArrayList<>();
        requestedKeyMap.forEach((keyValue, requestKey) -> {
            List<ErpSaleCartItemDO> groupItems = itemGroupMap.getOrDefault(keyValue, Collections.emptyList());
            if (CollUtil.isEmpty(groupItems)) {
                return;
            }
            groupItems.sort(Comparator.comparing(ErpSaleCartItemDO::getId,
                    Comparator.nullsLast(Long::compareTo)));
            Long survivorId = groupItems.get(0).getId();
            long rank = saleCartItemMapper.selectDefaultRankByCartIdAndItemId(reqVO.getCartId(), survivorId);
            ErpSaleCartItemMatchRespVO.Match match = new ErpSaleCartItemMatchRespVO.Match();
            match.setProductId(requestKey.getProductId());
            match.setWarehouseId(requestKey.getWarehouseId());
            match.setBatchNo(normalizeBatchNo(requestKey.getBatchNo()));
            match.setGiftFlag(Boolean.TRUE.equals(requestKey.getGiftFlag()));
            match.setSurvivorId(survivorId);
            match.setTargetPageNo((int) Math.max(1L, (rank + reqVO.getPageSize() - 1L) / reqVO.getPageSize()));
            match.setItems(BeanUtils.toBean(groupItems, ErpSaleCartRespVO.Item.class));
            matches.add(match);
        });
        result.setMatches(matches);
        return result;
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds) {
        if (CollUtil.isEmpty(cartIds)) {
            return Collections.emptyList();
        }
        return saleCartItemMapper.selectListByCartIds(cartIds);
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    public PageResult<DeptSimpleRespVO> getWarehouseAvailableDeptSimplePage(Long warehouseId, PageParam pageParam) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimplePage(warehouseId, FIELD_PERMISSION_MODULE, pageParam);
    }

    @Override
    public ErpSaleCartImportRespVO parseImportData(List<ErpSaleCartImportExcelVO> list) {
        ErpSaleCartImportRespVO respVO = new ErpSaleCartImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleCartImportExcelVO::getProductCode, ErpSaleCartImportExcelVO::getProductName, ErpSaleCartImportExcelVO::getFactoryCode, productMapper);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productResolver.getResolvedProducts(), ErpProductDO::getId)));
        Map<String, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.getCurrentUserVisibleSaleWarehouseList(), ErpWarehouseDO::getName);
        for (int i = 0; i < list.size(); i++) {
            ErpSaleCartImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            String decimalError = ErpSaleDecimalUtils.importScaleError(row);
            if (decimalError != null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), decimalError));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            if (productResult.isFailure()) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), productResult.getErrorMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productResult.getProduct();
            ErpWarehouseDO warehouse = warehouseMap.get(row.getWarehouseName());
            if (warehouse == null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "所属仓库不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpSaleCartRespVO.Item item = new ErpSaleCartRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(warehouse.getId());
            item.setWarehouseName(warehouse.getName());
            item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
            item.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag())
                    ? BigDecimal.ZERO : (row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice()));
            item.setCount(row.getCount());
            item.setBrand(product.getBrand());
            item.setVehicleModel(product.getVehicleModel());
            item.setStandard(product.getStandard());
            item.setRemark(row.getRemark());
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleImportResultRespVO importSaleCartOrderList(List<ErpSaleCartOrderImportExcelVO> list) {
        ErpSaleImportResultRespVO respVO = new ErpSaleImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Map<String, ErpCustomerDO> customerMap = buildSaleCartImportCustomerMap();
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleCartOrderImportExcelVO::getProductCode, ErpSaleCartOrderImportExcelVO::getProductName,
                ErpSaleCartOrderImportExcelVO::getFactoryCode, productMapper);
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getCurrentUserVisibleSaleWarehouseList().stream()
                .collect(Collectors.toMap(item -> normalizeSaleCartImportKey(item.getName()), item -> item, (a, b) -> a));

        List<SaleCartOrderImportGroup> groups = new ArrayList<>();
        SaleCartOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpSaleCartOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            String decimalError = ErpSaleDecimalUtils.importScaleError(row);
            if (decimalError != null) {
                respVO.getFailureDetails().add(new ErpSaleImportResultRespVO.FailureItem(rowNo, null, row.getProductCode(), decimalError));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (isBlankSaleCartOrderRow(row)) {
                continue;
            }
            if (hasSaleCartOrderMainFields(row)) {
                ErpCustomerDO customer = customerMap.get(normalizeSaleCartImportKey(row.getCustomerName()));
                currentGroup = new SaleCartOrderImportGroup(rowNo, row, customer);
                groups.add(currentGroup);
                String orderNo = resolveSaleCartImportNo(rowNo, row);
                if (trimSaleCartImportText(row.getCustomerName()) == null) {
                    addSaleCartImportFailure(respVO, rowNo, orderNo, null, "客户名称不能为空");
                } else if (customer == null) {
                    addSaleCartImportFailure(respVO, rowNo, orderNo, null, "客户不存在：" + row.getCustomerName());
                } else if (CommonStatusEnum.isDisable(customer.getStatus())) {
                    addSaleCartImportFailure(respVO, rowNo, orderNo, null, "客户未启用：" + customer.getName());
                }
            } else if (hasSaleCartOrderDetailFields(row) && currentGroup == null) {
                addSaleCartImportFailure(respVO, rowNo, null,
                        ErpImportProductResolver.getIdentifier(row.getProductCode(), row.getProductName(), row.getFactoryCode()),
                        "明细行前缺少销售手推车主表信息");
                continue;
            }
            if (!hasSaleCartOrderDetailFields(row)) {
                continue;
            }
            String orderNo = currentGroup == null ? null
                    : resolveSaleCartImportNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            String productIdentifier = productResult.getIdentifier();
            ErpProductDO product = productResult.getProduct();
            ErpWarehouseDO warehouse = trimSaleCartImportText(row.getWarehouseName()) == null ? null
                    : warehouseMap.get(normalizeSaleCartImportKey(row.getWarehouseName()));
            boolean valid = true;
            if (productResult.isFailure()) {
                addSaleCartImportFailure(respVO, rowNo, orderNo, productIdentifier, productResult.getErrorMessage());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addSaleCartImportFailure(respVO, rowNo, orderNo, productIdentifier, "数量必须大于 0");
                valid = false;
            }
            if (row.getProductPrice() != null && row.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                addSaleCartImportFailure(respVO, rowNo, orderNo, productIdentifier, "单价不能小于 0");
                valid = false;
            }
            if (trimSaleCartImportText(row.getWarehouseName()) == null) {
                addSaleCartImportFailure(respVO, rowNo, orderNo, productIdentifier, "所属仓库不能为空");
                valid = false;
            } else if (warehouse == null) {
                addSaleCartImportFailure(respVO, rowNo, orderNo, productIdentifier,
                        "仓库不存在或无销售权限：" + row.getWarehouseName());
                valid = false;
            }
            if (valid && currentGroup != null) {
                currentGroup.getRows().add(new SaleCartOrderImportRow(row, product, warehouse));
            }
        }
        for (SaleCartOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addSaleCartImportFailure(respVO, group.getRowNo(),
                        resolveSaleCartImportNo(group.getRowNo(), group.getMainRow()), null,
                        "销售手推车至少需要一条明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }
        for (SaleCartOrderImportGroup group : groups) {
            Long id = createSaleCart(buildSaleCartImportSaveReq(group));
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpSaleCartSaveReqVO buildSaleCartImportSaveReq(SaleCartOrderImportGroup group) {
        ErpSaleCartSaveReqVO saveReqVO = new ErpSaleCartSaveReqVO();
        saveReqVO.setCustomerId(group.getCustomer().getId());
        saveReqVO.setRemark(trimSaleCartImportText(group.getMainRow().getRemark()));
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setFeeAmount(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(convertList(group.getRows(), this::buildSaleCartImportItem));
        return saveReqVO;
    }

    private ErpSaleCartSaveReqVO.Item buildSaleCartImportItem(SaleCartOrderImportRow importRow) {
        ErpSaleCartOrderImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpSaleCartSaveReqVO.Item item = new ErpSaleCartSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setWarehouseId(importRow.getWarehouse().getId());
        item.setCount(row.getItemCount());
        item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
        item.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO
                : (row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice()));
        item.setRemark(trimSaleCartImportText(row.getItemRemark()));
        return item;
    }

    private Map<String, ErpCustomerDO> buildSaleCartImportCustomerMap() {
        Map<String, ErpCustomerDO> map = new LinkedHashMap<>();
        cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO pageReqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        customerService.getCustomerPage(pageReqVO).getList().forEach(customer -> {
            putSaleCartImportCustomer(map, customer.getCode(), customer);
            putSaleCartImportCustomer(map, customer.getName(), customer);
            putSaleCartImportCustomer(map, customer.getShortName(), customer);
        });
        return map;
    }

    private void putSaleCartImportCustomer(Map<String, ErpCustomerDO> map, String key, ErpCustomerDO customer) {
        String normalized = normalizeSaleCartImportKey(key);
        if (normalized != null) {
            map.putIfAbsent(normalized, customer);
        }
    }

    private boolean isBlankSaleCartOrderRow(ErpSaleCartOrderImportExcelVO row) {
        return row == null || !hasSaleCartOrderMainFields(row) && !hasSaleCartOrderDetailFields(row);
    }

    private boolean hasSaleCartOrderMainFields(ErpSaleCartOrderImportExcelVO row) {
        return trimSaleCartImportText(row.getCustomerName()) != null
                || trimSaleCartImportText(row.getRemark()) != null;
    }

    private boolean hasSaleCartOrderDetailFields(ErpSaleCartOrderImportExcelVO row) {
        return trimSaleCartImportText(row.getProductCode()) != null
                || trimSaleCartImportText(row.getProductName()) != null
                || trimSaleCartImportText(row.getFactoryCode()) != null
                || trimSaleCartImportText(row.getWarehouseName()) != null
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || row.getGiftFlag() != null
                || trimSaleCartImportText(row.getItemRemark()) != null;
    }

    private String resolveSaleCartImportNo(Integer rowNo, ErpSaleCartOrderImportExcelVO row) {
        String customerName = trimSaleCartImportText(row.getCustomerName());
        return customerName != null ? customerName : "row " + rowNo;
    }

    private void addSaleCartImportFailure(ErpSaleImportResultRespVO respVO, Integer rowNo, String orderNo,
                                          String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpSaleImportResultRespVO.FailureItem(rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    private String normalizeSaleCartImportKey(String value) {
        String normalized = trimSaleCartImportText(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String trimSaleCartImportText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public ErpSaleCartFirstApproveConfigRespVO getFirstApproveConfig() {
        FirstApproveConfig config = getFirstApproveConfigObject();
        ErpSaleCartFirstApproveConfigRespVO respVO = BeanUtils.toBean(config, ErpSaleCartFirstApproveConfigRespVO.class);
        respVO.setDeptIds(getFirstApproveDeptIds());
        // 兼容旧版前端字段：初审配置部门只限定单据所属部门，不再限定操作用户所属部门。
        respVO.setCurrentUserAllowed(true);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFirstApproveConfig(ErpSaleCartFirstApproveConfigSaveReqVO reqVO) {
        List<Long> deptIds = CollUtil.emptyIfNull(reqVO.getDeptIds()).stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (Boolean.TRUE.equals(reqVO.getEnabled()) && Boolean.TRUE.equals(reqVO.getDeptAuthEnabled())
                && CollUtil.isEmpty(deptIds)) {
            throw exception(SALE_CART_FIRST_APPROVE_DEPT_EMPTY);
        }
        if (CollUtil.isNotEmpty(deptIds)) {
            deptApi.validateDeptList(deptIds);
        }
        FirstApproveConfig config = BeanUtils.toBean(reqVO, FirstApproveConfig.class);
        upsertFirstApproveGlobalConfig(config);
        replaceFirstApproveDeptConfigs(deptIds);
    }

    private static class SaleCartOrderImportGroup {
        private final Integer rowNo;
        private final ErpSaleCartOrderImportExcelVO mainRow;
        private final ErpCustomerDO customer;
        private final List<SaleCartOrderImportRow> rows = new ArrayList<>();

        private SaleCartOrderImportGroup(Integer rowNo, ErpSaleCartOrderImportExcelVO mainRow,
                                         ErpCustomerDO customer) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.customer = customer;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpSaleCartOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpCustomerDO getCustomer() {
            return customer;
        }

        public List<SaleCartOrderImportRow> getRows() {
            return rows;
        }
    }

    private static class SaleCartOrderImportRow {
        private final ErpSaleCartOrderImportExcelVO row;
        private final ErpProductDO product;
        private final ErpWarehouseDO warehouse;

        private SaleCartOrderImportRow(ErpSaleCartOrderImportExcelVO row, ErpProductDO product,
                                       ErpWarehouseDO warehouse) {
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
        }

        public ErpSaleCartOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public ErpWarehouseDO getWarehouse() {
            return warehouse;
        }
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
            operateLogService.recordCreate(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_CART_TYPE, id, no, approve);
        }
    }

    private void record(Long id, String subType, String action, String no) {
        if (operateLogService != null) {
            operateLogService.record(ERP_SALE_CART_TYPE, subType, id, action, no);
        }
    }

    private Integer getBillingConfirmTargetStatus(Long deptId) {
        return isFirstApproveRequiredForDept(deptId)
                ? ErpSaleCartStatusEnum.SUBMITTED.getStatus()
                : ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus();
    }

    private Integer resolveFinalApproveOldStatus(ErpSaleCartDO cart) {
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            return ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus();
        }
        if (!isFirstApproveRequiredForDept(cart.getDeptId())
                && ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())) {
            return ErpSaleCartStatusEnum.SUBMITTED.getStatus();
        }
        throw exception(SALE_CART_FINAL_APPROVE_FAIL);
    }

    private FirstApproveConfig getFirstApproveConfigObject() {
        ErpSaleConfigDO configDO = saleConfigMapper.selectByTypeAndCode(FIRST_APPROVE_CONFIG_TYPE, FIRST_APPROVE_CONFIG_CODE);
        if (configDO == null || configDO.getConfigValue() == null || configDO.getConfigValue().isEmpty()) {
            return FirstApproveConfig.defaultConfig();
        }
        FirstApproveConfig config = JsonUtils.parseObject(configDO.getConfigValue(), FirstApproveConfig.class);
        return config != null ? config.normalize() : FirstApproveConfig.defaultConfig();
    }

    private List<Long> getFirstApproveDeptIds() {
        return saleConfigMapper.selectListByType(FIRST_APPROVE_DEPT_CONFIG_TYPE).stream()
                .map(ErpSaleConfigDO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void upsertFirstApproveGlobalConfig(FirstApproveConfig config) {
        ErpSaleConfigDO existing = saleConfigMapper.selectByTypeAndCode(FIRST_APPROVE_CONFIG_TYPE, FIRST_APPROVE_CONFIG_CODE);
        ErpSaleConfigDO configDO = new ErpSaleConfigDO()
                .setConfigType(FIRST_APPROVE_CONFIG_TYPE)
                .setCode(FIRST_APPROVE_CONFIG_CODE)
                .setName("销售手推车初审设置")
                .setConfigValue(JsonUtils.toJsonString(config.normalize()))
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setSort(0)
                .setRemark("控制销售手推车是否需要初审及初审部门授权");
        if (existing == null) {
            saleConfigMapper.insert(configDO);
            return;
        }
        configDO.setId(existing.getId());
        saleConfigMapper.updateById(configDO);
    }

    private void replaceFirstApproveDeptConfigs(List<Long> deptIds) {
        List<ErpSaleConfigDO> existingList = saleConfigMapper.selectListByType(FIRST_APPROVE_DEPT_CONFIG_TYPE);
        Set<Long> deptIdSet = new LinkedHashSet<>(deptIds);
        List<Long> deleteIds = existingList.stream()
                .filter(config -> config.getDeptId() == null || !deptIdSet.contains(config.getDeptId()))
                .map(ErpSaleConfigDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deleteIds)) {
            saleConfigMapper.deletePhysicalByTypeAndIds(FIRST_APPROVE_DEPT_CONFIG_TYPE,
                    TenantContextHolder.getTenantId(), deleteIds);
        }
        if (CollUtil.isEmpty(deptIdSet)) {
            return;
        }
        List<ErpSaleConfigDO> configs = new ArrayList<>();
        Map<Long, ErpSaleConfigDO> existingMap = existingList.stream()
                .filter(config -> config.getDeptId() != null)
                .collect(Collectors.toMap(ErpSaleConfigDO::getDeptId, config -> config, (first, second) -> first));
        int sort = 0;
        for (Long deptId : deptIdSet) {
            ErpSaleConfigDO config = new ErpSaleConfigDO()
                    .setConfigType(FIRST_APPROVE_DEPT_CONFIG_TYPE)
                    .setCode(FIRST_APPROVE_DEPT_CODE_PREFIX + deptId)
                    .setName("销售手推车初审授权部门")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setDeptId(deptId)
                    .setSort(sort++);
            ErpSaleConfigDO existing = existingMap.get(deptId);
            if (existing == null) {
                configs.add(config);
                continue;
            }
            config.setId(existing.getId());
            saleConfigMapper.updateById(config);
        }
        if (CollUtil.isNotEmpty(configs)) {
            saleConfigMapper.insertBatch(configs);
        }
    }

    @Override
    public boolean isFirstApproveRequiredForDept(Long deptId) {
        return isFirstApproveRequiredForDept(getFirstApproveConfigObject(), deptId);
    }

    @Override
    public Map<Long, Boolean> getFirstApproveRequiredMap(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        FirstApproveConfig config = getFirstApproveConfigObject();
        Set<Long> allowedDeptIds = Boolean.TRUE.equals(config.getEnabled())
                && Boolean.TRUE.equals(config.getDeptAuthEnabled())
                ? getFirstApproveDeptIdSet(config) : Collections.emptySet();
        Map<Long, Boolean> result = new LinkedHashMap<>();
        deptIds.forEach(deptId -> result.putIfAbsent(deptId,
                isFirstApproveRequiredForDept(config, deptId, allowedDeptIds)));
        return result;
    }

    private boolean isFirstApproveRequiredForDept(FirstApproveConfig config, Long deptId) {
        Set<Long> allowedDeptIds = Boolean.TRUE.equals(config.getEnabled())
                && Boolean.TRUE.equals(config.getDeptAuthEnabled())
                ? getFirstApproveDeptIdSet(config) : Collections.emptySet();
        return isFirstApproveRequiredForDept(config, deptId, allowedDeptIds);
    }

    private boolean isFirstApproveRequiredForDept(FirstApproveConfig config, Long deptId,
                                                   Set<Long> allowedDeptIds) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return false;
        }
        if (!Boolean.TRUE.equals(config.getDeptAuthEnabled())) {
            return true;
        }
        if (deptId == null) {
            return false;
        }
        return allowedDeptIds.contains(deptId);
    }

    private Set<Long> getFirstApproveDeptIdSet(FirstApproveConfig config) {
        List<Long> deptIds = getFirstApproveDeptIds();
        Set<Long> allowedDeptIds = new HashSet<>(deptIds);
        if (Boolean.TRUE.equals(config.getIncludeChildDept())) {
            deptIds.forEach(deptId -> {
                List<DeptRespDTO> childDeptList = deptApi.getChildDeptList(deptId);
                CollUtil.addAll(allowedDeptIds, convertList(childDeptList, DeptRespDTO::getId));
            });
        }
        return allowedDeptIds;
    }

    @lombok.Data
    private static final class FirstApproveConfig {

        private Boolean enabled;
        private Boolean deptAuthEnabled;
        private Boolean includeChildDept;

        private static FirstApproveConfig defaultConfig() {
            FirstApproveConfig config = new FirstApproveConfig();
            config.setEnabled(true);
            config.setDeptAuthEnabled(false);
            config.setIncludeChildDept(true);
            return config;
        }

        private FirstApproveConfig normalize() {
            this.enabled = this.enabled != null ? this.enabled : true;
            this.deptAuthEnabled = this.deptAuthEnabled != null ? this.deptAuthEnabled : false;
            this.includeChildDept = this.includeChildDept != null ? this.includeChildDept : true;
            return this;
        }

    }

    private static final class ProductWarehouseKey {

        private final Long productId;
        private final Long warehouseId;

        private ProductWarehouseKey(Long productId, Long warehouseId) {
            this.productId = productId;
            this.warehouseId = warehouseId;
        }

        private Long getProductId() {
            return productId;
        }

        private Long getWarehouseId() {
            return warehouseId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProductWarehouseKey)) {
                return false;
            }
            ProductWarehouseKey that = (ProductWarehouseKey) o;
            return Objects.equals(productId, that.productId) && Objects.equals(warehouseId, that.warehouseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, warehouseId);
        }

    }

    private static final class CreatedSaleCart {

        private final ErpSaleCartDO cart;
        private final List<ErpSaleCartItemDO> items;

        private CreatedSaleCart(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
            this.cart = cart;
            this.items = items;
        }

    }

}
