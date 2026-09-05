package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceReceiptStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;
import static cn.iocoder.yudao.module.erp.service.common.ErpFinanceAmountUtils.normalize;

// TODO 芋艿：记录操作日志

/**
 * ERP 收款单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpFinanceReceiptServiceImpl implements ErpFinanceReceiptService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receipt";

    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpCustomerDeptPermissionService customerDeptPermissionService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleReturnService saleReturnService;
    @Resource
    private ErpSalePriceAdjustService salePriceAdjustService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpFinanceAutoWriteOffService financeAutoWriteOffService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceReceipt(ErpFinanceReceiptSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO.getItems());
        // 1.1 校验订单项的有效性
        List<ErpFinanceReceiptItemDO> receiptItems = validateFinanceReceiptItems(
                createReqVO.getCustomerId(), createReqVO.getItems());
        // 1.2 校验客户
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (createReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(createReqVO.getFinanceUserId());
        }
        // 1.5 生成收款单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX);
        if (financeReceiptMapper.selectByNo(no) != null) {
            throw exception(FINANCE_RECEIPT_NO_EXISTS);
        }

        // 2.1 插入收款单
        ErpFinanceReceiptDO receipt = BeanUtils.toBean(createReqVO, ErpFinanceReceiptDO.class, in -> in
                .setNo(no).setStatus(ErpFinanceReceiptStatusEnum.PROCESS.getStatus()));
        permissionFieldFiller.fillCreateFields(receipt);
        validateReceiptCustomerDept(receipt, false);
        fillDefaultAmount(receipt);
        preparePendingItems(receipt, receiptItems);
        financeReceiptMapper.insert(receipt);
        // 2.2 插入收款单项
        if (CollUtil.isNotEmpty(receiptItems)) {
            receiptItems.forEach(o -> o.setReceiptId(receipt.getId()));
            financeReceiptItemMapper.insertBatch(receiptItems);
        }

        operateLogService.recordCreate(ERP_FINANCE_RECEIPT_TYPE, receipt.getId(), receipt.getNo());
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceReceiptDraft(ErpFinanceReceiptDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO.getItems());
        List<ErpFinanceReceiptItemDO> receiptItems = buildFinanceReceiptDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(receiptItems)) {
            throw exception(FINANCE_RECEIPT_DRAFT_ITEMS_REQUIRED);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX);
        if (financeReceiptMapper.selectByNo(no) != null) {
            throw exception(FINANCE_RECEIPT_NO_EXISTS);
        }
        ErpFinanceReceiptDO receipt = BeanUtils.toBean(createReqVO, ErpFinanceReceiptDO.class)
                .setId(null)
                .setNo(no)
                .setStatus(ErpFinanceReceiptStatusEnum.DRAFT.getStatus())
                .setReceiptTime(createReqVO.getReceiptTime() != null
                        ? createReqVO.getReceiptTime() : LocalDateTime.now());
        fillDraftAmounts(receipt, receiptItems);
        permissionFieldFiller.fillCreateFields(receipt);
        validateReceiptCustomerDept(receipt, true);
        financeReceiptMapper.insert(receipt);
        insertFinanceReceiptDraftItems(receipt.getId(), receiptItems);
        operateLogService.recordCreate(ERP_FINANCE_RECEIPT_TYPE, receipt.getId(), receipt.getNo());
        return receipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitFinanceReceipt(ErpFinanceReceiptSaveReqVO createReqVO) {
        return createFinanceReceipt(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceReceipt(ErpFinanceReceiptSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(updateReqVO.getId());
        if (ErpFinanceReceiptStatusEnum.DRAFT.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_DRAFT_UPDATE_FAIL, receipt.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_UPDATE_FAIL_APPROVE, receipt.getNo());
        }
        List<ErpFinanceReceiptItemDO> oldReceiptItems = financeReceiptItemMapper.selectListByReceiptId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, receipt);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldReceiptItems, ErpFinanceReceiptSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldReceiptItems);
        }
        // 1.2 校验客户
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (updateReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getFinanceUserId());
        }
        // 1.5 校验收款单项的有效性
        List<ErpFinanceReceiptItemDO> receiptItems = validateFinanceReceiptItems(
                updateReqVO.getCustomerId(), updateReqVO.getItems());

        // 2.1 更新收款单
        ErpFinanceReceiptDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceReceiptDO.class);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(receipt.getDeptId());
        }
        validateReceiptCustomerDept(updateObj, false);
        fillDefaultAmount(updateObj);
        preparePendingItems(updateObj, receiptItems);
        financeReceiptMapper.updateById(updateObj);
        // 2.2 更新收款单项
        updateFinanceReceiptItemList(updateReqVO.getId(), receiptItems);
        recordUpdate(receipt, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceReceiptDraft(ErpFinanceReceiptDraftSaveReqVO updateReqVO) {
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(updateReqVO.getId());
        if (!ErpFinanceReceiptStatusEnum.DRAFT.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_DRAFT_UPDATE_FAIL, receipt.getNo());
        }
        List<ErpFinanceReceiptItemDO> oldReceiptItems =
                financeReceiptItemMapper.selectListByReceiptId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, receipt);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldReceiptItems, ErpFinanceReceiptSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveHiddenItemFields(
                    FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldReceiptItems);
        }
        List<ErpFinanceReceiptItemDO> receiptItems = buildFinanceReceiptDraftItems(updateReqVO.getItems());
        ErpFinanceReceiptDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceReceiptDO.class)
                .setId(receipt.getId())
                .setNo(receipt.getNo())
                .setStatus(receipt.getStatus())
                .setReceiptTime(updateReqVO.getReceiptTime() != null
                        ? updateReqVO.getReceiptTime() : receipt.getReceiptTime());
        fillDraftAmounts(updateObj, receiptItems);
        validateReceiptCustomerDept(updateObj, true);
        if (financeReceiptMapper.updateByIdAndStatus(receipt.getId(),
                ErpFinanceReceiptStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(FINANCE_RECEIPT_DRAFT_UPDATE_FAIL, receipt.getNo());
        }
        financeReceiptItemMapper.deleteByReceiptId(receipt.getId());
        insertFinanceReceiptDraftItems(receipt.getId(), receiptItems);
        recordUpdate(receipt, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitFinanceReceipt(ErpFinanceReceiptSaveReqVO updateReqVO) {
        ErpFinanceReceiptDraftSaveReqVO draftReqVO =
                BeanUtils.toBean(updateReqVO, ErpFinanceReceiptDraftSaveReqVO.class);
        updateFinanceReceiptDraft(draftReqVO);
        submitFinanceReceipt(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFinanceReceipt(Long id) {
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectByIdForUpdate(id);
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        if (!ErpFinanceReceiptStatusEnum.DRAFT.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateFinanceReceiptDraftForSubmit(receipt);
        validateReceiptCustomerDept(receipt, false);
        List<ErpFinanceReceiptItemDO> receiptItems = validateFinanceReceiptItems(
                receipt.getCustomerId(),
                BeanUtils.toBean(financeReceiptItemMapper.selectListByReceiptId(id),
                        ErpFinanceReceiptSaveReqVO.Item.class));
        fillDefaultAmount(receipt);
        preparePendingItems(receipt, receiptItems);
        ErpFinanceReceiptDO statusUpdate = new ErpFinanceReceiptDO()
                .setStatus(ErpFinanceReceiptStatusEnum.PROCESS.getStatus())
                .setTotalPrice(receipt.getTotalPrice())
                .setDiscountPrice(receipt.getDiscountPrice())
                .setReceiptPrice(receipt.getReceiptPrice());
        if (financeReceiptMapper.updateByIdAndStatus(id,
                ErpFinanceReceiptStatusEnum.DRAFT.getStatus(), statusUpdate) == 0) {
            throw exception(FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        financeReceiptItemMapper.deleteByReceiptId(id);
        insertFinanceReceiptDraftItems(id, receiptItems);
        operateLogService.recordUpdate(ERP_FINANCE_RECEIPT_TYPE, id, receipt.getNo());
    }

    @Override
    public void updateFinanceReceiptRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(updateReqVO.getId());
        financeReceiptMapper.updateById(new ErpFinanceReceiptDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_FINANCE_RECEIPT_TYPE, receipt.getId(), receipt.getNo());
    }

    private void fillDefaultAmount(ErpFinanceReceiptDO receipt) {
        receipt.setDiscountPrice(normalize(getZeroIfNull(receipt.getDiscountPrice())));
        BigDecimal totalPrice = normalize(getZeroIfNull(receipt.getTotalPrice()));
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "合计收款不能为 0");
        }
        if (receipt.getDiscountPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "优惠金额不能小于 0");
        }
        BigDecimal receiptPrice = totalPrice.subtract(receipt.getDiscountPrice());
        if (receiptPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "实际收款不能为 0");
        }
        receipt.setTotalPrice(totalPrice).setReceiptPrice(normalize(receiptPrice));
    }

    private void validateReceiptCustomerDept(ErpFinanceReceiptDO receipt, boolean draft) {
        if (receipt == null || receipt.getDeptId() == null || (draft && receipt.getCustomerId() == null)) {
            return;
        }
        if (!customerDeptPermissionService.hasAvailableDept(
                receipt.getCustomerId(), receipt.getDeptId(), FIELD_PERMISSION_MODULE)) {
            throw exception(FINANCE_RECEIPT_CUSTOMER_DEPT_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFinanceReceipt(Long id) {
        // 1.1 校验存在
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectByIdForUpdate(id);
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        // 1.2 校验状态
        if (!ErpFinanceReceiptStatusEnum.PROCESS.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_APPROVE_FAIL);
        }

        List<ErpFinanceReceiptItemDO> pendingItems = financeReceiptItemMapper.selectListByReceiptId(id).stream()
                .filter(item -> item.getWriteOffStatus() == null
                        || ErpFinanceWriteOffStatusEnum.PENDING.getStatus().equals(item.getWriteOffStatus()))
                .collect(Collectors.toList());
        validateAndFillEffectiveItems(receipt, pendingItems);

        // 2. 更新状态并使初始核销明细生效
        int updateCount = financeReceiptMapper.updateByIdAndStatus(id, receipt.getStatus(),
                new ErpFinanceReceiptDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(FINANCE_RECEIPT_APPROVE_FAIL);
        }
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        pendingItems.forEach(item -> item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .setWriteOffTime(now).setWriteOffUserId(loginUserId));
        if (CollUtil.isNotEmpty(pendingItems)) {
            financeReceiptItemMapper.updateBatch(pendingItems);
            updateSalePrice(pendingItems);
        }
        financeAutoWriteOffService.autoWriteOffReceipt(id, loginUserId);
        operateLogService.recordStatus(ERP_FINANCE_RECEIPT_TYPE, id, receipt.getNo(), true);
    }

    private List<ErpFinanceReceiptItemDO> validateFinanceReceiptItems(
            Long customerId,
            List<ErpFinanceReceiptSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return convertList(list, o -> BeanUtils.toBean(o, ErpFinanceReceiptItemDO.class, item -> {
            if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_OUT.getType())) {
                ErpSaleOutDO saleOut = saleOutService.validateSaleOut(item.getBizId());
                if (!Objects.equals(saleOut.getCustomerId(), customerId)) {
                    throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "客户必须相同");
                }
                item.setTotalPrice(ErpOriginalSettlementAmountUtils.calculateSaleOut(saleOut,
                        saleOutService.getSaleOutItemListByOutIds(Collections.singleton(item.getBizId()))))
                        .setBizNo(saleOut.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_RETURN.getType())) {
                ErpSaleReturnDO saleReturn = saleReturnService.validateSaleReturn(item.getBizId());
                if (!Objects.equals(saleReturn.getCustomerId(), customerId)) {
                    throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "客户必须相同");
                }
                item.setTotalPrice(saleReturn.getTotalPrice().negate()).setBizNo(saleReturn.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType())) {
                ErpSalePriceAdjustDO salePriceAdjust = salePriceAdjustService.validateSalePriceAdjust(item.getBizId());
                if (!Objects.equals(salePriceAdjust.getCustomerId(), customerId)) {
                    throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "客户必须相同");
                }
                item.setTotalPrice(salePriceAdjust.getTotalAdjustPrice()).setBizNo(salePriceAdjust.getNo());
            } else {
                throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "业务类型不正确：" + item.getBizType());
            }
            item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus());
        }));
    }

    private List<ErpFinanceReceiptItemDO> buildFinanceReceiptDraftItems(
            List<ErpFinanceReceiptSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getBizType() != null
                        && item.getBizId() != null
                        && item.getReceiptPrice() != null)
                .filter(item -> ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_OUT.getType())
                        || ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_RETURN.getType())
                        || ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType()))
                .map(item -> BeanUtils.toBean(item, ErpFinanceReceiptItemDO.class)
                        .setId(null)
                        .setTotalPrice(normalize(getZeroIfNull(item.getTotalPrice())))
                        .setReceiptedPrice(normalize(getZeroIfNull(item.getReceiptedPrice())))
                        .setReceiptPrice(normalize(item.getReceiptPrice()))
                        .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus())
                        .setWriteOffTime(null)
                        .setWriteOffUserId(null)
                        .setReverseTime(null)
                        .setReverseUserId(null)
                        .setReverseReason(null))
                .collect(Collectors.toList());
    }

    private void fillDraftAmounts(ErpFinanceReceiptDO receipt, List<ErpFinanceReceiptItemDO> receiptItems) {
        BigDecimal totalPrice = receiptItems.stream()
                .map(ErpFinanceReceiptItemDO::getReceiptPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalPrice = normalize(totalPrice);
        BigDecimal discountPrice = normalize(getZeroIfNull(receipt.getDiscountPrice()));
        receipt.setTotalPrice(totalPrice)
                .setDiscountPrice(discountPrice)
                .setReceiptPrice(normalize(totalPrice.subtract(discountPrice)));
    }

    private void insertFinanceReceiptDraftItems(Long receiptId, List<ErpFinanceReceiptItemDO> receiptItems) {
        if (CollUtil.isEmpty(receiptItems)) {
            return;
        }
        receiptItems.forEach(item -> item.setId(null).setReceiptId(receiptId));
        financeReceiptItemMapper.insertBatch(receiptItems);
    }

    private void validateFinanceReceiptDraftForSubmit(ErpFinanceReceiptDO receipt) {
        if (receipt.getReceiptTime() == null) {
            throw exception(FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "收款时间不能为空");
        }
        if (receipt.getCustomerId() == null) {
            throw exception(FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "客户不能为空");
        }
        if (receipt.getAccountId() == null) {
            throw exception(FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "收款账户不能为空");
        }
        customerService.validateCustomer(receipt.getCustomerId());
        accountService.validateAccount(receipt.getAccountId());
        if (receipt.getFinanceUserId() != null) {
            adminUserApi.validateUser(receipt.getFinanceUserId());
        }
    }

    private void updateFinanceReceiptItemList(Long id, List<ErpFinanceReceiptItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpFinanceReceiptItemDO> oldList = financeReceiptItemMapper.selectListByReceiptId(id);
        List<List<ErpFinanceReceiptItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setReceiptId(id));
            financeReceiptItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            financeReceiptItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            financeReceiptItemMapper.deleteByIds(convertList(diffList.get(2), ErpFinanceReceiptItemDO::getId));
        }

    }

    private void updateSalePrice(List<ErpFinanceReceiptItemDO> receiptItems) {
        receiptItems.forEach(receiptItem -> {
            BigDecimal totalReceiptPrice = financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    receiptItem.getBizId(), receiptItem.getBizType());
            if (ErpBizTypeEnum.SALE_OUT.getType().equals(receiptItem.getBizType())) {
                saleOutService.updateSaleInReceiptPrice(receiptItem.getBizId(), totalReceiptPrice);
            } else if (ErpBizTypeEnum.SALE_RETURN.getType().equals(receiptItem.getBizType())) {
                saleReturnService.updateSaleReturnRefundPrice(receiptItem.getBizId(), totalReceiptPrice.negate());
            } else if (ErpBizTypeEnum.SALE_PRICE_ADJUST.getType().equals(receiptItem.getBizType())) {
                salePriceAdjustService.updateSalePriceAdjustReceiptPrice(receiptItem.getBizId(), totalReceiptPrice);
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + receiptItem.getBizType());
            }
        });
    }

    private void preparePendingItems(ErpFinanceReceiptDO receipt, List<ErpFinanceReceiptItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        validateAllocationItems(receipt, items);
        items.forEach(item -> item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus())
                .setWriteOffTime(null).setWriteOffUserId(null)
                .setReverseTime(null).setReverseUserId(null).setReverseReason(null));
    }

    private void validateAndFillEffectiveItems(ErpFinanceReceiptDO receipt,
                                               List<ErpFinanceReceiptItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        validateAllocationItems(receipt, items);
    }

    private void validateAllocationItems(ErpFinanceReceiptDO receipt, List<ErpFinanceReceiptItemDO> items) {
        Set<String> bizKeys = new HashSet<>();
        BigDecimal allocationAmount = BigDecimal.ZERO;
        for (ErpFinanceReceiptItemDO item : items) {
            String bizKey = item.getBizType() + ":" + item.getBizId();
            if (!bizKeys.add(bizKey)) {
                throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "同一业务单据不能重复选择");
            }
            ReceiptBizSnapshot biz = lockReceiptBiz(item.getBizType(), item.getBizId(), receipt);
            BigDecimal allocatedPrice = financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    item.getBizId(), item.getBizType());
            BigDecimal receiptPrice = normalize(item.getReceiptPrice());
            BigDecimal remainingPrice = normalize(biz.totalPrice.subtract(allocatedPrice));
            validateReceiptWriteOffAmount(receiptPrice, remainingPrice);
            item.setReceiptPrice(receiptPrice);
            item.setBizNo(biz.bizNo).setTotalPrice(biz.totalPrice).setReceiptedPrice(allocatedPrice);
            allocationAmount = normalize(allocationAmount.add(receiptPrice));
        }
        BigDecimal currentAllocatedPrice = receipt.getId() == null ? BigDecimal.ZERO
                : financeReceiptItemMapper.selectEffectivePriceSumMapByReceiptIds(
                        Collections.singleton(receipt.getId())).getOrDefault(receipt.getId(), BigDecimal.ZERO);
        validateReceiptAllocationLimit(receipt, currentAllocatedPrice.add(allocationAmount));
    }

    @Override
    public List<ErpFinanceReceiptWriteOffCandidateRespVO> getWriteOffCandidates(Long receiptId) {
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(receiptId);
        validateReceiptWriteOffStatus(receipt);
        List<ErpFinanceReceiptWriteOffCandidateRespVO> result = new java.util.ArrayList<>();

        LambdaQueryWrapperX<ErpSaleOutDO> outQuery = new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (receipt.getDeptId() == null) {
            outQuery.isNull(ErpSaleOutDO::getDeptId);
        } else {
            outQuery.eq(ErpSaleOutDO::getDeptId, receipt.getDeptId());
        }
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(outQuery);
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = CollUtil.isEmpty(saleOuts)
                ? Collections.emptyMap()
                : convertMultiMap(saleOutService.getSaleOutItemListByOutIds(
                        convertSet(saleOuts, ErpSaleOutDO::getId)), ErpSaleOutItemDO::getOutId);
        Map<Long, BigDecimal> outAllocated = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(saleOuts, ErpSaleOutDO::getId), ErpBizTypeEnum.SALE_OUT.getType());
        saleOuts.forEach(row -> addReceiptCandidate(result, ErpBizTypeEnum.SALE_OUT, row.getId(), row.getNo(),
                row.getOutTime(), ErpOriginalSettlementAmountUtils.calculateSaleOut(row,
                        saleOutItemMap.getOrDefault(row.getId(), Collections.emptyList())),
                outAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpSaleReturnDO> returnQuery = new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (receipt.getDeptId() == null) {
            returnQuery.isNull(ErpSaleReturnDO::getDeptId);
        } else {
            returnQuery.eq(ErpSaleReturnDO::getDeptId, receipt.getDeptId());
        }
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectList(returnQuery);
        Map<Long, BigDecimal> returnAllocated = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(saleReturns, ErpSaleReturnDO::getId), ErpBizTypeEnum.SALE_RETURN.getType());
        saleReturns.forEach(row -> addReceiptCandidate(result, ErpBizTypeEnum.SALE_RETURN, row.getId(), row.getNo(),
                row.getReturnTime(), getZeroIfNull(row.getTotalPrice()).negate(),
                returnAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpSalePriceAdjustDO> adjustQuery = new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, receipt.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (receipt.getDeptId() == null) {
            adjustQuery.isNull(ErpSalePriceAdjustDO::getDeptId);
        } else {
            adjustQuery.eq(ErpSalePriceAdjustDO::getDeptId, receipt.getDeptId());
        }
        List<ErpSalePriceAdjustDO> adjusts = salePriceAdjustMapper.selectList(adjustQuery);
        Map<Long, BigDecimal> adjustAllocated = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(adjusts, ErpSalePriceAdjustDO::getId), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
        adjusts.forEach(row -> addReceiptCandidate(result, ErpBizTypeEnum.SALE_PRICE_ADJUST, row.getId(), row.getNo(),
                row.getAdjustDate(), getZeroIfNull(row.getTotalAdjustPrice()),
                adjustAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        result.sort(Comparator.comparing(ErpFinanceReceiptWriteOffCandidateRespVO::getBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(ErpFinanceReceiptWriteOffCandidateRespVO::getBizNo,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private void addReceiptCandidate(List<ErpFinanceReceiptWriteOffCandidateRespVO> result, ErpBizTypeEnum bizType,
                                     Long bizId, String bizNo, LocalDateTime bizTime,
                                     BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal unallocatedPrice = totalPrice.subtract(allocatedPrice);
        if (unallocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        result.add(new ErpFinanceReceiptWriteOffCandidateRespVO()
                .setBizType(bizType.getType()).setBizTypeName(bizType.getName())
                .setBizId(bizId).setBizNo(bizNo).setBizTime(bizTime)
                .setTotalPrice(totalPrice).setAllocatedPrice(allocatedPrice)
                .setUnallocatedPrice(unallocatedPrice));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeOffFinanceReceipt(ErpFinanceReceiptWriteOffReqVO reqVO) {
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectByIdForUpdate(reqVO.getReceiptId());
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        validateReceiptWriteOffStatus(receipt);
        Set<String> bizKeys = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        List<ErpFinanceReceiptItemDO> items = reqVO.getItems().stream().map(reqItem -> {
            String bizKey = reqItem.getBizType() + ":" + reqItem.getBizId();
            if (!bizKeys.add(bizKey)) {
                throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "同一业务单据不能重复选择");
            }
            ReceiptBizSnapshot biz = lockReceiptBiz(reqItem.getBizType(), reqItem.getBizId(), receipt);
            BigDecimal allocatedPrice = financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    reqItem.getBizId(), reqItem.getBizType());
            BigDecimal writeOffAmount = normalize(reqItem.getWriteOffAmount());
            validateReceiptWriteOffAmount(writeOffAmount, normalize(biz.totalPrice.subtract(allocatedPrice)));
            return new ErpFinanceReceiptItemDO().setReceiptId(receipt.getId())
                    .setBizType(reqItem.getBizType()).setBizId(reqItem.getBizId()).setBizNo(biz.bizNo)
                    .setTotalPrice(biz.totalPrice).setReceiptedPrice(allocatedPrice)
                    .setReceiptPrice(writeOffAmount).setRemark(reqItem.getRemark())
                    .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    .setWriteOffTime(now).setWriteOffUserId(loginUserId);
        }).collect(Collectors.toList());
        BigDecimal currentAllocatedPrice = financeReceiptItemMapper.selectEffectivePriceSumMapByReceiptIds(
                Collections.singleton(receipt.getId())).getOrDefault(receipt.getId(), BigDecimal.ZERO);
        BigDecimal newAllocatedPrice = items.stream().map(ErpFinanceReceiptItemDO::getReceiptPrice)
                .reduce(currentAllocatedPrice, BigDecimal::add);
        validateReceiptAllocationLimit(receipt, newAllocatedPrice);
        financeReceiptItemMapper.insertBatch(items);
        updateSalePrice(items);
        operateLogService.record(ERP_FINANCE_RECEIPT_TYPE, ERP_UPDATE_SUB_TYPE, receipt.getId(),
                "收款单后续核销，单据编号：" + receipt.getNo() + "，本次核销："
                        + items.stream().map(ErpFinanceReceiptItemDO::getReceiptPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), receipt.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseFinanceReceiptWriteOff(ErpFinanceReceiptWriteOffReverseReqVO reqVO) {
        ErpFinanceReceiptItemDO item = financeReceiptItemMapper.selectByIdForUpdate(reqVO.getItemId());
        if (item == null || !ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus().equals(item.getWriteOffStatus())) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_ITEM_NOT_EFFECTIVE);
        }
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectByIdForUpdate(item.getReceiptId());
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        validateReceiptWriteOffStatus(receipt);
        lockReceiptBiz(item.getBizType(), item.getBizId(), receipt);
        item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus())
                .setReverseTime(LocalDateTime.now()).setReverseUserId(SecurityFrameworkUtils.getLoginUserId())
                .setReverseReason(reqVO.getReason());
        financeReceiptItemMapper.updateById(item);
        updateSalePrice(Collections.singletonList(item));
        operateLogService.record(ERP_FINANCE_RECEIPT_TYPE, ERP_UPDATE_SUB_TYPE, receipt.getId(),
                "撤销收款核销，单据编号：" + receipt.getNo() + "，核销明细：" + item.getId()
                        + "，原因：" + reqVO.getReason(), receipt.getNo());
    }

    private ReceiptBizSnapshot lockReceiptBiz(Integer bizType, Long bizId, ErpFinanceReceiptDO receipt) {
        ReceiptBizSnapshot result;
        if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_OUT.getType())) {
            ErpSaleOutDO row = saleOutMapper.selectOne(new LambdaQueryWrapperX<ErpSaleOutDO>()
                    .eq(ErpSaleOutDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), ErpOriginalSettlementAmountUtils.calculateSaleOut(row,
                    saleOutService.getSaleOutItemListByOutIds(Collections.singleton(bizId))));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_RETURN.getType())) {
            ErpSaleReturnDO row = saleReturnMapper.selectOne(new LambdaQueryWrapperX<ErpSaleReturnDO>()
                    .eq(ErpSaleReturnDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalPrice()).negate());
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.SALE_PRICE_ADJUST.getType())) {
            ErpSalePriceAdjustDO row = salePriceAdjustMapper.selectOne(new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                    .eq(ErpSalePriceAdjustDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new ReceiptBizSnapshot(row.getCustomerId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalAdjustPrice()));
        } else {
            throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "业务类型不正确：" + bizType);
        }
        if (result == null || !ErpAuditStatus.APPROVE.getStatus().equals(result.status)) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "业务单据不存在或未审核");
        }
        if (!Objects.equals(result.partyId, receipt.getCustomerId())) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "客户必须相同");
        }
        if (!Objects.equals(result.deptId, receipt.getDeptId())) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_BIZ_INVALID, "所属部门必须相同");
        }
        return result;
    }

    private void validateReceiptWriteOffStatus(ErpFinanceReceiptDO receipt) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_STATUS_INVALID);
        }
    }

    private void validateReceiptWriteOffAmount(BigDecimal amount, BigDecimal remainingPrice) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "核销金额不能为 0");
        }
        if (remainingPrice.compareTo(BigDecimal.ZERO) == 0
                || amount.signum() != remainingPrice.signum()
                || amount.abs().compareTo(remainingPrice.abs()) > 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "核销金额超过业务单据未核销金额或符号不一致");
        }
    }

    private void validateReceiptAllocationLimit(ErpFinanceReceiptDO receipt, BigDecimal allocatedPrice) {
        BigDecimal normalizedAllocatedPrice = normalize(allocatedPrice);
        BigDecimal totalPrice = normalize(getZeroIfNull(receipt.getTotalPrice()));
        if (normalizedAllocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0
                || normalizedAllocatedPrice.signum() != totalPrice.signum()
                || normalizedAllocatedPrice.abs().compareTo(totalPrice.abs()) > 0) {
            throw exception(FINANCE_RECEIPT_WRITEOFF_AMOUNT_EXCEED);
        }
    }

    private static final class ReceiptBizSnapshot {
        private final Long partyId;
        private final Long deptId;
        private final Integer status;
        private final String bizNo;
        private final BigDecimal totalPrice;

        private ReceiptBizSnapshot(Long partyId, Long deptId, Integer status, String bizNo, BigDecimal totalPrice) {
            this.partyId = partyId;
            this.deptId = deptId;
            this.status = status;
            this.bizNo = bizNo;
            this.totalPrice = totalPrice;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinanceReceipt(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpFinanceReceiptDO> receipts = financeReceiptMapper.selectByIds(ids);
        if (CollUtil.isEmpty(receipts)) {
            return;
        }
        receipts.forEach(receipt -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
                throw exception(FINANCE_RECEIPT_DELETE_FAIL_APPROVE, receipt.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        receipts.forEach(receipt -> {
            // 2.1 删除收款单
            financeReceiptMapper.deleteById(receipt.getId());
            // 2.2 删除收款单项
            List<ErpFinanceReceiptItemDO> receiptItems = financeReceiptItemMapper.selectListByReceiptId(receipt.getId());
            financeReceiptItemMapper.deleteByIds(convertSet(receiptItems, ErpFinanceReceiptItemDO::getId));

            operateLogService.recordDelete(ERP_FINANCE_RECEIPT_TYPE, receipt.getId(), receipt.getNo());
        });
    }

    private void recordUpdate(ErpFinanceReceiptDO oldReceipt, ErpFinanceReceiptDO newReceipt) {
        operateLogService.record(ERP_FINANCE_RECEIPT_TYPE, ERP_UPDATE_SUB_TYPE, newReceipt.getId(),
                "更新收款单，单据编号：" + oldReceipt.getNo()
                        + "，合计金额：" + oldReceipt.getTotalPrice() + " -> " + newReceipt.getTotalPrice()
                        + "，实际收款：" + oldReceipt.getReceiptPrice() + " -> " + newReceipt.getReceiptPrice(),
                oldReceipt.getNo());
    }

    private ErpFinanceReceiptDO validateFinanceReceiptExists(Long id) {
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectById(id);
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        return receipt;
    }

    @Override
    public ErpFinanceReceiptDO getFinanceReceipt(Long id) {
        return financeReceiptMapper.selectById(id);
    }

    @Override
    public List<ErpFinanceReceiptDO> getFinanceReceiptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return financeReceiptMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpFinanceReceiptDO> getFinanceReceiptPage(ErpFinanceReceiptPageReqVO pageReqVO) {
        return financeReceiptMapper.selectPage(pageReqVO);
    }

    // ==================== 收款单项 ====================

    @Override
    public List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptId(Long receiptId) {
        return financeReceiptItemMapper.selectListByReceiptId(receiptId);
    }

    @Override
    public PageResult<ErpFinanceReceiptItemDO> getFinanceReceiptItemPage(ErpFinanceReceiptItemPageReqVO pageReqVO) {
        validateFinanceReceiptExists(pageReqVO.getReceiptId());
        return financeReceiptItemMapper.selectPageByReceiptId(pageReqVO);
    }

    @Override
    public List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptIds(Collection<Long> receiptIds) {
        if (CollUtil.isEmpty(receiptIds)) {
            return Collections.emptyList();
        }
        return financeReceiptItemMapper.selectListByReceiptIds(receiptIds);
    }

    private BigDecimal getZeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

}
