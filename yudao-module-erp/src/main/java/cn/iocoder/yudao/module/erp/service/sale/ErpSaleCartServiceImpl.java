package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
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
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
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
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
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
 * ERP 销售手推车 Service 实现�?
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

    @Resource
    private ErpSaleCartMapper saleCartMapper;
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
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
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
        CreatedSaleCart created = createSaleCart(createReqVO, ErpSaleCartStatusEnum.PROCESS.getStatus());
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
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(targetStatus)) {
            lockSaleCartStock(created.cart, created.items);
            createTransferOutDraft(created.cart, created.items);
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
        clearHiddenItemFields(createReqVO.getItems());
        if (createReqVO.getCustomerId() != null) {
            customerService.validateCustomerForSale(createReqVO.getCustomerId());
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
        }
        saleDocumentDefaultService.fillCreateDefaults(cart);
        cart.setStatus(status != null ? status : getSubmitTargetStatus(cart.getDeptId()));
        List<ErpSaleCartSaveReqVO.Item> itemReqs = createReqVO.getItems();
        if (draft) {
            itemReqs = filterDraftItems(itemReqs);
            if (CollUtil.isEmpty(itemReqs)) {
                throw exception(SALE_CART_DRAFT_ITEMS_REQUIRED);
            }
        }
        List<ErpSaleCartItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : (draft
                ? validateSaleCartDraftItems(itemReqs, cart.getDeptId())
                : validateSaleCartItems(itemReqs, cart.getDeptId()));
        calculateTotalPrice(cart, items);
        if (!draft) {
            validateFreightFinanceFields(cart);
        }
        saleCartMapper.insert(cart);
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setCartId(cart.getId()));
            saleCartItemMapper.insertBatch(items);
        }
        return new CreatedSaleCart(cart, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO) {
        updateSaleCart(updateReqVO, false);
    }

    @Override
    public void updateSaleCartRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        saleCartMapper.updateById(new ErpSaleCartDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartDraft(ErpSaleCartSaveReqVO updateReqVO) {
        updateSaleCart(updateReqVO, true);
    }

    private void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO, boolean draft) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        if (draft
                ? !ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())
                : !isBeforeFirstApprove(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_FAIL_NOT_PROCESS, cart.getNo());
        }
        preserveHiddenFields(updateReqVO, cart);
        List<ErpSaleCartSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        preserveHiddenItemFields(itemReqs, saleCartItemMapper.selectListByCartId(updateReqVO.getId()));
        if (!draft || updateReqVO.getCustomerId() != null) {
            customerService.validateCustomerForSale(updateReqVO.getCustomerId());
        }
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        ErpSaleCartDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleCartDO.class);
        updateObj.setCartTime(LocalDateTime.now());
        Long saleDeptId = updateObj.getDeptId() != null ? updateObj.getDeptId() : cart.getDeptId();
        updateObj.setNo(cart.getNo());
        updateObj.setStatus(cart.getStatus());
        updateObj.setDeptId(saleDeptId);
        if (draft && CollUtil.isNotEmpty(itemReqs)) {
            itemReqs = filterDraftItems(itemReqs);
        }
        List<ErpSaleCartItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : (draft
                ? validateSaleCartDraftItems(itemReqs, saleDeptId)
                : validateSaleCartItems(itemReqs, saleDeptId));
        calculateTotalPrice(updateObj, items);
        if (!draft) {
            validateFreightFinanceFields(updateObj);
        }
        saleCartMapper.updateById(updateObj);
        saleCartItemMapper.deleteByCartId(updateReqVO.getId());
        items.forEach(item -> {
            item.setId(null);
            item.setCartId(updateReqVO.getId());
        });
        if (CollUtil.isNotEmpty(items)) {
            saleCartItemMapper.insertBatch(items);
        }
        if (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())) {
            validateStockEnoughRealtime(items);
        }
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartBasic(ErpSaleCartUpdateBasicReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        if (!ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_BASIC_FAIL_STATUS, cart.getNo());
        }
        customerService.validateCustomerForSale(updateReqVO.getCustomerId());
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
        ErpSaleCartDO cart = validateSaleCartExists(id);
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(id);
        normalizeItemDeptIdByCartDept(cart, items);
        ErpSaleCartSubmitRespVO result = buildSubmitResult(cart, items);
        if (CollUtil.isNotEmpty(result.getShortageItems())) {
            throw buildStockShortageException(result.getShortageItems());
        }
        Integer targetStatus = getSubmitTargetStatus(cart.getDeptId());
        updateStatus(id, ErpSaleCartStatusEnum.PROCESS.getStatus(), targetStatus,
                SALE_CART_SUBMIT_FAIL);
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(targetStatus)) {
            lockSaleCartStock(cart, items);
            createTransferOutDraft(cart, items);
        }
        result.setStatus(targetStatus);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCrossDeptTransferOutDraftByCartId(Long cartId) {
        ErpSaleCartDO cart = validateSaleCartExists(cartId);
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_FIRST_APPROVE_FAIL);
        }
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(cartId);
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
        ErpSaleCartDO cart = validateSaleCartExists(id);
        if (!isFirstApproveRequiredForDept(config, cart.getDeptId())) {
            throw exception(SALE_CART_FIRST_APPROVE_DEPT_UNAUTHORIZED);
        }
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(id);
        normalizeItemDeptIdByCartDept(cart, items);
        validateStockEnoughRealtime(items);
        lockSaleCartStock(cart, items);
        createTransferOutDraft(cart, items);
        updateStatus(id, ErpSaleCartStatusEnum.SUBMITTED.getStatus(), ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(),
                SALE_CART_FIRST_APPROVE_FAIL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> finalApproveSaleCart(Long id) {
        return doFinalApproveSaleCart(id, SecurityFrameworkUtils.getLoginUserId());
    }

    private List<Long> doFinalApproveSaleCart(Long id, Long finalApproveUserId) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        Integer oldStatus = resolveFinalApproveOldStatus(cart);
        int claimCount = saleCartMapper.updateByIdAndStatus(id, oldStatus,
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus()));
        if (claimCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(id);
        normalizeItemDeptIdByCartDept(cart, items);
        // 终审时实时校验库存，不依赖缓存的 stockCount。
        // The cart has already been claimed as FINAL_APPROVE, so it is no longer included in occupied stock.
        List<ErpSaleCartItemDO> finalItems = resolveFinalSaleItems(cart, items);
        validateStockEnoughRealtime(finalItems, false, cart.getDeptId());
        validateFreightFinanceFields(cart);
        Set<Long> warehouseIds = convertSet(finalItems, ErpSaleCartItemDO::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.validSaleWarehouseListForDept(warehouseIds, cart.getDeptId()), ErpWarehouseDO::getId);
        boolean stockBillEnabled = warehouseMap.values().stream()
                .anyMatch(warehouse -> Boolean.TRUE.equals(warehouse.getStockBillEnabled()));
        Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(cart, finalItems),
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), cart.getNo(), stockBillEnabled);
        createFreightFinanceDraft(cart);
        if (stockBillEnabled) {
            // 出库凭证已接管待出库占用，释放手推车库存锁，避免重复占用。
            stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId());
        }
        List<Long> saleOutIds = new ArrayList<>();
        saleOutIds.add(saleOutId);
        /*
         * 原逻辑：按仓库拆分生成销售单。业务目前要求一张手推车只生成一张销售单，后续如果恢复拆单可启用该逻辑。
        Map<Long, List<ErpSaleCartItemDO>> itemsByWarehouse = items.stream()
                .collect(Collectors.groupingBy(ErpSaleCartItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.validSaleWarehouseList(itemsByWarehouse.keySet()), ErpWarehouseDO::getId);
        List<Long> saleOutIds = new ArrayList<>();
        for (Map.Entry<Long, List<ErpSaleCartItemDO>> entry : itemsByWarehouse.entrySet()) {
            ErpWarehouseDO warehouse = warehouseMap.get(entry.getKey());
            boolean stockBillEnabled = warehouse != null && Boolean.TRUE.equals(warehouse.getStockBillEnabled());
            Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(cart, entry.getValue()),
                    ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), cart.getNo(), stockBillEnabled);
            saleOutIds.add(saleOutId);
        }
         */
        int updateCount = saleCartMapper.updateByIdAndStatus(id, ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus())
                        .setFinalAuditTime(LocalDateTime.now())
                        .setFinalAuditUserId(finalApproveUserId));
        if (updateCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        recordStatus(id, cart.getNo(), true);
        return saleOutIds;
    }

    private void createFreightFinanceDraft(ErpSaleCartDO cart) {
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
                .setCustomerId(cart.getCustomerId())
                .setSettleMethod(cart.getSettleMethod())
                .setAccountId(cart.getAccountId())
                .setDeptId(cart.getDeptId())
                .setHandlerId(cart.getSaleUserId())
                .setParty(cart.getLogisticsCompany())
                .setAmount(financeAmount);
        if (FREIGHT_TYPE_CUSTOMER_ADVANCE.equals(cart.getFreightType())) {
            receivableOtherService.createFromSaleCartFreight(createReqBO);
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
        ErpSaleCartDO cart = validateSaleCartExists(id);
        doCancelFirstApproveSaleCart(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSaleCartByTransferOutId(Long transferOutId) {
        ErpStockMoveDO transferOut = stockMoveService.getStockMoveForUpdate(transferOutId);
        if (transferOut == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
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

        List<ErpStockMoveItemDO> transferOutItems = stockMoveService.getStockMoveItemListByMoveId(transferOutId);
        ErpStockMoveOperationPermission transferOutPermission = stockMoveService.getUnlockCartPermission(
                transferOut, transferOutItems);
        if (!Boolean.TRUE.equals(transferOutPermission.getAllowed())) {
            throw exception(STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);
        }

        ErpSaleCartDO cart = validateSaleCartExists(transferOut.getSourceId());
        if (!ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UNLOCK_STATUS_INVALID);
        }
        List<ErpStockMoveDO> relatedTransferOuts = new ArrayList<>(stockMoveService.getTransferOutListBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId()));
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
            List<ErpStockMoveItemDO> moveItems = stockMoveService.getStockMoveItemListByMoveId(move.getId());
            ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(move, moveItems);
            return !Boolean.TRUE.equals(permission.getAllowed());
        });
        if (permissionDenied) {
            throw exception(STOCK_MOVE_UNLOCK_CROSS_DEPT_DENIED);
        }
        doCancelFirstApproveSaleCart(cart, "解锁手推车",
                "从调拨出库单解锁手推车，调拨出库单：" + transferOut.getNo()
                        + "/" + transferOut.getId() + "，销售手推车：" + cart.getNo() + "/" + cart.getId());
    }

    /**
     * 统一执行销售手推车撤销初审，供销售手推车入口及后续调拨出库解锁入口复用。
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
        if (stockMoveService.hasUnapprovedTransferOutBySource(sourceType, id)) {
            log.info("调拨出库审批后暂不自动终审，销售手推车仍有未审核调拨出库单，saleCartId={}, cartNo={}",
                    id, cart.getNo());
            return;
        }
        if (!stockMoveService.hasApprovedTransferOutBySource(sourceType, id)) {
            log.warn("调拨出库审批后暂不自动终审，销售手推车没有已审核调拨出库单，saleCartId={}, cartNo={}",
                    id, cart.getNo());
            return;
        }
        doFinalApproveSaleCart(id, finalApproveUserId);
        log.info("调拨出库审批后销售手推车自动终审成功，saleCartId={}, cartNo={}, finalApproveUserId={}",
                id, cart.getNo(), finalApproveUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        if (!ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_REJECT_FAIL);
        }
        if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            stockMoveService.deleteUnapprovedTransferOutBySource(ErpSaleBizSourceTypeEnum.CART.getType(), id);
            stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), id);
        }
        int updateCount = saleCartMapper.updateByIdAndStatus(id, cart.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_CART_REJECT_FAIL);
        }
        record(id, "驳回", "驳回销售手推车，单据编号：" + cart.getNo(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToQuote(ErpSaleCartConvertQuoteReqVO reqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(reqVO.getCartId());
        if (!isBeforeFirstApprove(cart.getStatus())) {
            throw exception(SALE_CART_CONVERT_QUOTE_FAIL);
        }

        // 1. 查询所有子表行（整单转换）
        List<ErpSaleCartItemDO> allItems = saleCartItemMapper.selectListByCartId(cart.getId());
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

        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleCart(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<ErpSaleCartDO> carts = saleCartMapper.selectBatchIds(ids);
        carts.forEach(cart -> {
            if (!isBeforeFirstApprove(cart.getStatus())) {
                throw exception(SALE_CART_DELETE_FAIL_NOT_DRAFT, cart.getNo());
            }
        });
        carts.forEach(cart -> stockMoveService.deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId()));
        carts.forEach(cart -> stockLockService.unlockStock(ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId()));
        saleCartMapper.deleteBatchIds(ids);
        ids.forEach(saleCartItemMapper::deleteByCartId);
        ids.forEach(id -> saleConvertRecordMapper.deleteByTarget(ErpSaleBizSourceTypeEnum.CART.getType(), id));
        carts.forEach(cart -> recordDelete(cart.getId(), cart.getNo()));
    }

    private boolean isBeforeFirstApprove(Integer status) {
        return ErpSaleCartStatusEnum.PROCESS.getStatus().equals(status)
                || ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(status);
    }

    private void updateStatus(Long id, Integer oldStatus, Integer newStatus, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        validateSaleCartExists(id);
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
            } else if (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(newStatus)
                    || ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus)) {
                record(id, "提交", "提交销售手推车，单据编号：" + cart.getNo(), cart.getNo());
            }
        }
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
            outItem.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO : item.getProductPrice());
            outItem.setCount(item.getCount());

            outItem.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            outItem.setBatchNo(item.getBatchNo());
            outItem.setRemark(item.getRemark());
            return outItem;
        }));
        return reqVO;
    }

    private List<ErpSaleCartItemDO> validateSaleCartItems(List<ErpSaleCartSaveReqVO.Item> list, Long saleDeptId) {
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleCartSaveReqVO.Item::getProductId, ErpSaleCartSaveReqVO.Item::getBatchNo);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseList(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId)), ErpWarehouseDO::getId);
        // 校验数量和价格
        list.forEach(item -> {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_CART_ITEM_COUNT_POSITIVE);
            }
            if (!Boolean.TRUE.equals(item.getGiftFlag())
                    && (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                throw exception(SALE_CART_ITEM_PRICE_POSITIVE);
            }
        });
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            fillDeptIdFromSaleDept(item, saleDeptId, warehouseMap);
            validateSaleDeptWarehousePermission(item);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
        }));
    }

    private List<ErpSaleCartItemDO> validateSaleCartDraftItems(List<ErpSaleCartSaveReqVO.Item> list, Long saleDeptId) {
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleCartSaveReqVO.Item::getProductId, ErpSaleCartSaveReqVO.Item::getBatchNo);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseList(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getWarehouseId)), ErpWarehouseDO::getId);
        list.forEach(item -> {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_CART_ITEM_COUNT_POSITIVE);
            }
        });
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            item.setId(null);
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            fillDeptIdFromSaleDept(item, saleDeptId, warehouseMap);
            validateSaleDeptWarehousePermission(item);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())
                    || item.getProductPrice() == null
                    || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
        }));
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
        if (item.getDeptId() == null || (saleDeptId != null && warehouse != null
                && Objects.equals(item.getDeptId(), warehouse.getDeptId())
                && !Objects.equals(saleDeptId, warehouse.getDeptId()))) {
            item.setDeptId(saleDeptId);
        }
    }

    private void normalizeItemDeptIdByCartDept(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (cart == null || cart.getDeptId() == null || CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseListForDept(
                convertSet(items, ErpSaleCartItemDO::getWarehouseId), cart.getDeptId()), ErpWarehouseDO::getId);
        items.forEach(item -> {
            fillDeptIdFromSaleDept(item, cart.getDeptId(), warehouseMap);
            validateSaleDeptWarehousePermission(item);
        });
    }

    private void validateSaleDeptWarehousePermission(ErpSaleCartItemDO item) {
        if (item.getDeptId() == null) {
            throw exception(SALE_WAREHOUSE_DEPT_REQUIRED);
        }
        warehouseService.validateWarehouseSaleAllowedForDept(item.getWarehouseId(), item.getDeptId());
    }

    private void createTransferOutDraft(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ErpWarehouseDO> sourceWarehouseMap = getCartSourceWarehouseMap(cart, items);
        List<ErpSaleCartItemDO> transferItems = getCrossDeptItems(cart, items, sourceWarehouseMap);
        if (CollUtil.isEmpty(transferItems)) {
            deleteRedundantUnapprovedTransferOut(cart);
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
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseList(
                convertSet(items, ErpSaleCartItemDO::getWarehouseId)), ErpWarehouseDO::getId);
        items.forEach(item -> {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null && Boolean.TRUE.equals(warehouse.getStockBillEnabled())) {
                return;
            }
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
        if (stockMoveService.hasUnapprovedTransferOutBySource(sourceType, cart.getId())) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
        List<ErpStockMoveDO> approvedTransfers = stockMoveService.getTransferOutListBySource(sourceType, cart.getId())
                .stream()
                .filter(move -> ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(approvedTransfers)) {
            throw exception(SALE_WAREHOUSE_TRANSFER_NOT_APPROVED);
        }
        List<ErpStockMoveItemDO> moveItems = approvedTransfers.stream()
                .flatMap(move -> stockMoveService.getStockMoveItemListByMoveId(move.getId()).stream())
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
        if (!stockMoveService.hasUnapprovedTransferOutBySource(sourceType, cart.getId())
                || stockMoveService.hasApprovedTransferOutBySource(sourceType, cart.getId())) {
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
        return convertMap(warehouseService.validSaleWarehouseListForDept(
                convertSet(items, ErpSaleCartItemDO::getWarehouseId), cart.getDeptId()), ErpWarehouseDO::getId);
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
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = calculateStockShortages(items, true);
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
                        ? warehouseService.validSaleWarehouseListForDept(warehouseIds, saleDeptId)
                        : warehouseService.validSaleWarehouseList(warehouseIds),
                ErpWarehouseDO::getId);
        Map<ProductWarehouseKey, BigDecimal> requiredCountMap = new LinkedHashMap<>();
        items.forEach(item -> {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null && Boolean.TRUE.equals(warehouse.getStockBillEnabled())) {
                return;
            }
            ProductWarehouseKey key = new ProductWarehouseKey(item.getProductId(), item.getWarehouseId());
            requiredCountMap.merge(key, item.getCount() != null ? item.getCount() : BigDecimal.ZERO, BigDecimal::add);
        });
        if (requiredCountMap.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = requiredCountMap.keySet().stream().map(ProductWarehouseKey::getProductId).collect(Collectors.toSet());
        Map<String, BigDecimal> occupiedCountMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getOccupiedCountMap(productIds, warehouseIds));
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        List<ErpSaleCartSubmitRespVO.ShortageItem> shortages = new ArrayList<>();
        requiredCountMap.forEach((key, requiredCount) -> {
            ErpStockDO stock = getStockIgnoreDataPermission(key.getProductId(), key.getWarehouseId());
            BigDecimal stockCount = stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal occupiedCount = occupiedCountMap.getOrDefault(
                    key.getProductId() + "_" + key.getWarehouseId(), BigDecimal.ZERO);
            // PROCESS/SUBMITTED/FIRST_APPROVE carts are part of occupiedCount. Exclude the current cart before
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
        cart.setDiscountPrice(MoneyUtils.priceMultiplyPercent(cart.getTotalPrice(), cart.getDiscountPercent()));
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
        quote.setDiscountPrice(MoneyUtils.priceMultiplyPercent(quote.getTotalPrice(), quote.getDiscountPercent()));
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

    private ErpSaleCartDO validateSaleCartExists(Long id) {
        ErpSaleCartDO cart = saleCartMapper.selectById(id);
        if (cart == null) {
            throw exception(SALE_CART_NOT_EXISTS);
        }
        return cart;
    }

    @Override
    public ErpSaleCartDO getSaleCart(Long id) {
        return saleCartMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO) {
        return saleCartMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId) {
        return saleCartItemMapper.selectListByCartId(cartId);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds) {
        if (CollUtil.isEmpty(cartIds)) {
            return Collections.emptyList();
        }
        return saleCartItemMapper.selectListByCartIds(cartIds);
    }

    @Override
    public ErpSaleCartImportRespVO parseImportData(List<ErpSaleCartImportExcelVO> list) {
        ErpSaleCartImportRespVO respVO = new ErpSaleCartImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (row.getProductCode() != null && !row.getProductCode().isEmpty()) {
                productCodes.add(row.getProductCode());
            }
        });
        Map<String, ErpProductDO> productMap = convertMap(
                DataPermissionUtils.executeIgnore(() -> productMapper.selectListByCodes(productCodes)), ErpProductDO::getCode);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productMap.values(), ErpProductDO::getId)));
        Map<String, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.getCurrentUserVisibleSaleWarehouseList(), ErpWarehouseDO::getName);
        for (int i = 0; i < list.size(); i++) {
            ErpSaleCartImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (row.getProductCode() == null || row.getProductCode().isEmpty()) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, null, "Product code is required"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "Product not found"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(row.getWarehouseName());
            if (warehouse == null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "Warehouse not found"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "数量必须大于 0"));
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
            item.setBrand(row.getBrand());
            item.setVehicleModel(row.getVehicleModel());
            item.setStandard(row.getStandard());
            item.setRemark(row.getRemark());
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
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

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void clearHiddenItemFields(List<?> items) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, items);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private void preserveHiddenItemFields(List<?> items, List<?> existingItems) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, items, existingItems);
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

    private Integer getSubmitTargetStatus(Long deptId) {
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
