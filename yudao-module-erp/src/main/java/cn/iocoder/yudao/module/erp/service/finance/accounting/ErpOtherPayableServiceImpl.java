package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.getSumValue;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpOtherPayableServiceImpl implements ErpOtherPayableService {

    private static final String NO_PREFIX = "QTYF";

    @Resource
    private ErpOtherPayableMapper otherPayableMapper;
    @Resource
    private ErpOtherPayableItemMapper otherPayableItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;
    @Resource
    private ErpAccountService accountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOtherPayable(ErpOtherPayableSaveReqVO createReqVO) {
        // 0. S11 修复：校验结算账户存在
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1. 校验明细
        List<ErpOtherPayableItemDO> items = validateAndConvertItems(createReqVO.getItems());

        // 2. 生成单号
        String no = noRedisDAO.generate(NO_PREFIX);
        if (otherPayableMapper.selectByNo(no) != null) {
            throw exception(OTHER_PAYABLE_NO_EXISTS);
        }

        // 3. 插入主表
        ErpOtherPayableDO payable = BeanUtils.toBean(createReqVO, ErpOtherPayableDO.class);
        payable.setNo(no);
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        calculateTotalAmount(payable, items);
        otherPayableMapper.insert(payable);

        // 4. 插入子表
        items.forEach(item -> item.setPayableId(payable.getId()));
        otherPayableItemMapper.insertBatch(items);
        return payable.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherPayable(ErpOtherPayableSaveReqVO updateReqVO) {
        // 0. S11 修复：校验结算账户存在
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1. 校验存在 + 状态
        ErpOtherPayableDO payable = validateOtherPayableExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(payable.getStatus())) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_APPROVE, payable.getNo());
        }

        // 2. 校验明细
        List<ErpOtherPayableItemDO> items = validateAndConvertItems(updateReqVO.getItems());

        // 3. 乐观锁更新主表（S12 修复：防止并发审核导致已审核单据被静默改写）
        ErpOtherPayableDO updateObj = BeanUtils.toBean(updateReqVO, ErpOtherPayableDO.class);
        calculateTotalAmount(updateObj, items);
        int affected = otherPayableMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_STATUS_CHANGED);
        }

        // 4. 删旧插新子表
        otherPayableItemMapper.deleteByPayableId(payable.getId());
        items.forEach(item -> item.setPayableId(payable.getId()));
        otherPayableItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherPayableStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1. 校验存在
        ErpOtherPayableDO payable = validateOtherPayableExists(id);
        // 2. 校验状态
        if (payable.getStatus().equals(status)) {
            throw exception(approve ? OTHER_PAYABLE_APPROVE_FAIL : OTHER_PAYABLE_PROCESS_FAIL);
        }

        // 3. 反审核时处理凭证
        if (!approve) {
            List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType(), id);
            if (CollUtil.isNotEmpty(vouchers)) {
                for (ErpVoucherDO voucher : vouchers) {
                    if (ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(voucher.getAuditStatus())) {
                        throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED, voucher.getVoucherNo());
                    }
                    // 删除未审核的凭证
                    voucherItemMapper.deleteByVoucherId(voucher.getId());
                    voucherMapper.deleteById(voucher.getId());
                }
            }
        }

        // 4. 乐观锁更新状态
        int updateCount = otherPayableMapper.updateByIdAndStatus(id, payable.getStatus(),
                new ErpOtherPayableDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? OTHER_PAYABLE_APPROVE_FAIL : OTHER_PAYABLE_PROCESS_FAIL);
        }

        // 5. 审核通过时生成凭证
        if (approve) {
            // 由财务在凭证生成页统一处理。
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOtherPayable(Long id) {
        // 1. 校验存在 + 状态
        ErpOtherPayableDO payable = validateOtherPayableExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(payable.getStatus())) {
            throw exception(OTHER_PAYABLE_DELETE_FAIL_APPROVE, payable.getNo());
        }
        // 2. 删除主表 + 子表
        otherPayableMapper.deleteById(id);
        otherPayableItemMapper.deleteByPayableId(id);
    }

    @Override
    public ErpOtherPayableDO getOtherPayable(Long id) {
        return otherPayableMapper.selectById(id);
    }

    @Override
    public PageResult<ErpOtherPayableDO> getOtherPayablePage(ErpOtherPayablePageReqVO pageReqVO) {
        return otherPayableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpOtherPayableItemDO> getOtherPayableItemListByPayableId(Long payableId) {
        return otherPayableItemMapper.selectListByPayableId(payableId);
    }

    @Override
    public List<ErpOtherPayableItemDO> getOtherPayableItemListByPayableIds(Collection<Long> payableIds) {
        if (CollUtil.isEmpty(payableIds)) {
            return Collections.emptyList();
        }
        return otherPayableItemMapper.selectListByPayableIds(payableIds);
    }

    // ==================== 私有辅助 ====================

    private ErpOtherPayableDO validateOtherPayableExists(Long id) {
        ErpOtherPayableDO payable = otherPayableMapper.selectById(id);
        if (payable == null) {
            throw exception(OTHER_PAYABLE_NOT_EXISTS);
        }
        return payable;
    }

    private List<ErpOtherPayableItemDO> validateAndConvertItems(List<ErpOtherPayableSaveReqVO.Item> reqItems) {
        return convertList(reqItems, item -> BeanUtils.toBean(item, ErpOtherPayableItemDO.class));
    }

    private void calculateTotalAmount(ErpOtherPayableDO payable, List<ErpOtherPayableItemDO> items) {
        BigDecimal totalAmount = getSumValue(items, ErpOtherPayableItemDO::getAmount, BigDecimal::add, BigDecimal.ZERO);
        payable.setTotalAmount(totalAmount);
        BigDecimal discountAmount = payable.getDiscountAmount() != null ? payable.getDiscountAmount() : BigDecimal.ZERO;
        payable.setDiscountAmount(discountAmount);
        payable.setActualAmount(totalAmount.subtract(discountAmount));
    }

}
