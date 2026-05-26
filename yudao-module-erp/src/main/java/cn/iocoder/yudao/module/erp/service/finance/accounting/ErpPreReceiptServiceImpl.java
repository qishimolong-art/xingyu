package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 预收款单 Service 实现类
 */
@Service
@Validated
public class ErpPreReceiptServiceImpl implements ErpPreReceiptService {

    @Resource
    private ErpPreReceiptMapper preReceiptMapper;
    @Resource
    private ErpPreReceiptItemMapper preReceiptItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpAccountService accountService;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPreReceipt(ErpPreReceiptSaveReqVO createReqVO) {
        // 1.1 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.2 生成单号
        String no = noRedisDAO.generate("YSKD");
        if (preReceiptMapper.selectByNo(no) != null) {
            throw exception(PRE_RECEIPT_NO_EXISTS);
        }

        // 2.1 插入主表
        ErpPreReceiptDO preReceipt = BeanUtils.toBean(createReqVO, ErpPreReceiptDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        calculateTotalAmount(preReceipt, createReqVO.getItems());
        preReceiptMapper.insert(preReceipt);

        // 2.2 插入子表
        List<ErpPreReceiptItemDO> items = convertList(createReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpPreReceiptItemDO.class));
        items.forEach(item -> item.setPreReceiptId(preReceipt.getId()));
        preReceiptItemMapper.insertBatch(items);

        return preReceipt.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreReceipt(ErpPreReceiptSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPreReceiptDO preReceipt = validatePreReceiptExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(preReceipt.getStatus())) {
            throw exception(PRE_RECEIPT_UPDATE_FAIL_APPROVE, preReceipt.getNo());
        }
        // 1.2 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }

        // 2.1 乐观锁更新主表（S12 修复：防止并发审核导致已审核单据被静默改写）
        ErpPreReceiptDO updateObj = BeanUtils.toBean(updateReqVO, ErpPreReceiptDO.class);
        calculateTotalAmount(updateObj, updateReqVO.getItems());
        int affected = preReceiptMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(PRE_RECEIPT_UPDATE_FAIL_STATUS_CHANGED);
        }

        // 2.2 更新子表
        List<ErpPreReceiptItemDO> newItems = convertList(updateReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpPreReceiptItemDO.class));
        updatePreReceiptItemList(updateReqVO.getId(), newItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreReceiptStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPreReceiptDO preReceipt = validatePreReceiptExists(id);
        // 1.2 校验状态
        if (preReceipt.getStatus().equals(status)) {
            throw exception(approve ? PRE_RECEIPT_APPROVE_FAIL : PRE_RECEIPT_PROCESS_FAIL);
        }

        // 1.3 反审：校验关联凭证
        if (!approve) {
            List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.PRE_RECEIPT.getType(), id);
            for (ErpVoucherDO v : vouchers) {
                if (Objects.equals(v.getAuditStatus(), ErpVoucherAuditStatusEnum.APPROVE.getStatus())) {
                    throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED);
                }
                voucherMapper.deleteById(v.getId());
                voucherItemMapper.delete(new LambdaQueryWrapper<ErpVoucherItemDO>()
                        .eq(ErpVoucherItemDO::getVoucherId, v.getId()));
            }
        }

        // 2. 更新状态
        int updateCount = preReceiptMapper.updateByIdAndStatus(id, preReceipt.getStatus(),
                new ErpPreReceiptDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PRE_RECEIPT_APPROVE_FAIL : PRE_RECEIPT_PROCESS_FAIL);
        }

        // 3. 审批通过：自动生成预收款凭证
        if (approve && preReceipt.getBizTime() != null
                && bookOpenService.isVoucherTypeEnabled(preReceipt.getBizTime().toLocalDate(),
                ErpVoucherTypeEnum.PRE_RECEIPT.getType())) {
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildPreReceiptItems(preReceipt);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.PRE_RECEIPT.getType(),
                    preReceipt.getId(),
                    preReceipt.getNo(),
                    preReceipt.getActualAmount(),
                    preReceipt.getBizTime().toLocalDate(),
                    "预收款 - " + preReceipt.getPartyName(),
                    voucherItems);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePreReceipt(List<Long> ids) {
        List<ErpPreReceiptDO> preReceipts = preReceiptMapper.selectByIds(ids);
        if (CollUtil.isEmpty(preReceipts)) {
            return;
        }
        preReceipts.forEach(preReceipt -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(preReceipt.getStatus())) {
                throw exception(PRE_RECEIPT_DELETE_FAIL_APPROVE, preReceipt.getNo());
            }
        });

        preReceipts.forEach(preReceipt -> {
            preReceiptMapper.deleteById(preReceipt.getId());
            List<ErpPreReceiptItemDO> items = preReceiptItemMapper.selectListByPreReceiptId(preReceipt.getId());
            if (CollUtil.isNotEmpty(items)) {
                preReceiptItemMapper.deleteByIds(convertSet(items, ErpPreReceiptItemDO::getId));
            }
        });
    }

    @Override
    public ErpPreReceiptDO getPreReceipt(Long id) {
        return preReceiptMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPreReceiptDO> getPreReceiptPage(ErpPreReceiptPageReqVO pageReqVO) {
        return preReceiptMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPreReceiptItemDO> getPreReceiptItemListByPreReceiptId(Long preReceiptId) {
        return preReceiptItemMapper.selectListByPreReceiptId(preReceiptId);
    }

    @Override
    public List<ErpPreReceiptItemDO> getPreReceiptItemListByPreReceiptIds(Collection<Long> preReceiptIds) {
        if (CollUtil.isEmpty(preReceiptIds)) {
            return Collections.emptyList();
        }
        return preReceiptItemMapper.selectListByPreReceiptIds(preReceiptIds);
    }

    // ==================== 私有方法 ====================

    private void calculateTotalAmount(ErpPreReceiptDO preReceipt, List<ErpPreReceiptSaveReqVO.Item> items) {
        BigDecimal totalAmount = getSumValue(items, ErpPreReceiptSaveReqVO.Item::getAmount, BigDecimal::add, BigDecimal.ZERO);
        preReceipt.setTotalAmount(totalAmount);
        BigDecimal discountAmount = preReceipt.getDiscountAmount() != null ? preReceipt.getDiscountAmount() : BigDecimal.ZERO;
        preReceipt.setActualAmount(totalAmount.subtract(discountAmount));
    }

    private ErpPreReceiptDO validatePreReceiptExists(Long id) {
        ErpPreReceiptDO preReceipt = preReceiptMapper.selectById(id);
        if (preReceipt == null) {
            throw exception(PRE_RECEIPT_NOT_EXISTS);
        }
        return preReceipt;
    }

    private void updatePreReceiptItemList(Long preReceiptId, List<ErpPreReceiptItemDO> newList) {
        List<ErpPreReceiptItemDO> oldList = preReceiptItemMapper.selectListByPreReceiptId(preReceiptId);
        List<List<ErpPreReceiptItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setPreReceiptId(preReceiptId));
            preReceiptItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            preReceiptItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            preReceiptItemMapper.deleteByIds(convertList(diffList.get(2), ErpPreReceiptItemDO::getId));
        }
    }

}
