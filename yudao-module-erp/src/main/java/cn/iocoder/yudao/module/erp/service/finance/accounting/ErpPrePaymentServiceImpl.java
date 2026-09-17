package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
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
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpPrePaymentServiceImpl implements ErpPrePaymentService {

    @Resource
    private ErpPrePaymentMapper prePaymentMapper;
    @Resource
    private ErpPrePaymentItemMapper prePaymentItemMapper;
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

    private static final String NO_PREFIX = "YFKD";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrePayment(ErpPrePaymentSaveReqVO createReqVO) {
        // 0. S11 修复：校验结算账户存在
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1. 生成单号
        String no = noRedisDAO.generate(NO_PREFIX);
        if (prePaymentMapper.selectByNo(no) != null) {
            throw exception(PRE_PAYMENT_NO_EXISTS);
        }

        // 2. 插入主表
        ErpPrePaymentDO prePayment = BeanUtils.toBean(createReqVO, ErpPrePaymentDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        List<ErpPrePaymentItemDO> items = convertList(createReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpPrePaymentItemDO.class));
        calculateTotalAmount(prePayment, items);
        prePaymentMapper.insert(prePayment);

        // 3. 插入子表
        items.forEach(item -> item.setPrePaymentId(prePayment.getId()));
        prePaymentItemMapper.insertBatch(items);
        return prePayment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrePayment(ErpPrePaymentSaveReqVO updateReqVO) {
        // 0. S11 修复：校验结算账户存在
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1. 校验存在 + 状态
        ErpPrePaymentDO prePayment = validatePrePaymentExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(prePayment.getStatus())) {
            throw exception(PRE_PAYMENT_UPDATE_FAIL_APPROVE, prePayment.getNo());
        }

        // 2. 乐观锁更新主表（S12 修复：防止并发审核导致已审核单据被静默改写）
        ErpPrePaymentDO updateObj = BeanUtils.toBean(updateReqVO, ErpPrePaymentDO.class);
        List<ErpPrePaymentItemDO> items = convertList(updateReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpPrePaymentItemDO.class));
        calculateTotalAmount(updateObj, items);
        int affected = prePaymentMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(PRE_PAYMENT_UPDATE_FAIL_STATUS_CHANGED);
        }

        // 3. 更新子表（先删后插）
        prePaymentItemMapper.deleteByPrePaymentId(prePayment.getId());
        items.forEach(item -> item.setPrePaymentId(prePayment.getId()));
        prePaymentItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrePaymentStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1. 校验存在
        ErpPrePaymentDO prePayment = validatePrePaymentExists(id);
        if (prePayment.getStatus().equals(status)) {
            throw exception(approve ? PRE_PAYMENT_APPROVE_FAIL : PRE_PAYMENT_PROCESS_FAIL);
        }

        // 2. 反审核：检查关联凭证
        if (!approve) {
            List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType(), id);
            if (CollUtil.isNotEmpty(vouchers)) {
                for (ErpVoucherDO voucher : vouchers) {
                    if (ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(voucher.getAuditStatus())) {
                        throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED, voucher.getVoucherNo());
                    }
                    // 未审核的凭证直接删除
                    voucherItemMapper.deleteByVoucherId(voucher.getId());
                    voucherMapper.deleteById(voucher.getId());
                }
            }
        }

        // 3. 更新状态
        int updateCount = prePaymentMapper.updateByIdAndStatus(id, prePayment.getStatus(),
                new ErpPrePaymentDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PRE_PAYMENT_APPROVE_FAIL : PRE_PAYMENT_PROCESS_FAIL);
        }

        // 财务在凭证生成页统一处理。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrePayment(Long id) {
        // 1. 校验存在 + 状态
        ErpPrePaymentDO prePayment = validatePrePaymentExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(prePayment.getStatus())) {
            throw exception(PRE_PAYMENT_DELETE_FAIL_APPROVE, prePayment.getNo());
        }

        // 2. 删除主表 + 子表
        prePaymentMapper.deleteById(id);
        prePaymentItemMapper.deleteByPrePaymentId(id);
    }

    @Override
    public ErpPrePaymentDO getPrePayment(Long id) {
        return prePaymentMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPrePaymentDO> getPrePaymentPage(ErpPrePaymentPageReqVO pageReqVO) {
        return prePaymentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPrePaymentItemDO> getPrePaymentItemListByPrePaymentId(Long prePaymentId) {
        return prePaymentItemMapper.selectListByPrePaymentId(prePaymentId);
    }

    // ==================== 私有辅助 ====================

    private ErpPrePaymentDO validatePrePaymentExists(Long id) {
        ErpPrePaymentDO prePayment = prePaymentMapper.selectById(id);
        if (prePayment == null) {
            throw exception(PRE_PAYMENT_NOT_EXISTS);
        }
        return prePayment;
    }

    private void calculateTotalAmount(ErpPrePaymentDO prePayment, List<ErpPrePaymentItemDO> items) {
        BigDecimal totalAmount = getSumValue(items, ErpPrePaymentItemDO::getAmount, BigDecimal::add, BigDecimal.ZERO);
        prePayment.setTotalAmount(totalAmount);
        BigDecimal discountAmount = prePayment.getDiscountAmount() != null ? prePayment.getDiscountAmount() : BigDecimal.ZERO;
        prePayment.setDiscountAmount(discountAmount);
        prePayment.setActualAmount(totalAmount.subtract(discountAmount));
    }

}
