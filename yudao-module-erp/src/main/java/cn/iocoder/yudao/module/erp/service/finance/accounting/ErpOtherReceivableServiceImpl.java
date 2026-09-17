package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpOtherReceivableServiceImpl implements ErpOtherReceivableService {

    @Resource
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Resource
    private ErpOtherReceivableItemMapper otherReceivableItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOtherReceivable(ErpOtherReceivableSaveReqVO createReqVO) {
        // 1. 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 2. 生成单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX);
        if (otherReceivableMapper.selectByNo(no) != null) {
            throw exception(OTHER_RECEIVABLE_NO_EXISTS);
        }

        // 3. 插入主表
        ErpOtherReceivableDO receivable = BeanUtils.toBean(createReqVO, ErpOtherReceivableDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        calculateTotalAmount(receivable, createReqVO.getItems());
        otherReceivableMapper.insert(receivable);

        // 4. 插入子表
        List<ErpOtherReceivableItemDO> items = convertList(createReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpOtherReceivableItemDO.class, item -> item.setReceivableId(receivable.getId())));
        otherReceivableItemMapper.insertBatch(items);
        return receivable.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherReceivable(ErpOtherReceivableSaveReqVO updateReqVO) {
        // 1. 校验存在 + 状态
        ErpOtherReceivableDO receivable = validateOtherReceivableExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(receivable.getStatus())) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE, receivable.getNo());
        }
        // 2. 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }

        // 3. 乐观锁更新主表（S12 修复：防止并发审核导致已审核单据被静默改写）
        ErpOtherReceivableDO updateObj = BeanUtils.toBean(updateReqVO, ErpOtherReceivableDO.class);
        calculateTotalAmount(updateObj, updateReqVO.getItems());
        int affected = otherReceivableMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED);
        }

        // 4. 更新子表
        updateOtherReceivableItemList(updateReqVO.getId(), convertList(updateReqVO.getItems(),
                o -> BeanUtils.toBean(o, ErpOtherReceivableItemDO.class, item -> item.setReceivableId(updateReqVO.getId()))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherReceivableStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpOtherReceivableDO receivable = validateOtherReceivableExists(id);
        // 1.2 校验状态
        if (receivable.getStatus().equals(status)) {
            throw exception(approve ? OTHER_RECEIVABLE_APPROVE_FAIL : OTHER_RECEIVABLE_PROCESS_FAIL);
        }

        // 1.3 反审：校验关联凭证；已审核拦截，未审核连带删除
        if (!approve) {
            List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.OTHER_RECEIVABLE.getType(), id);
            for (ErpVoucherDO v : vouchers) {
                if (Objects.equals(v.getAuditStatus(), ErpVoucherAuditStatusEnum.APPROVE.getStatus())) {
                    throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED, v.getVoucherNo());
                }
                voucherMapper.deleteById(v.getId());
                voucherItemMapper.delete(new LambdaQueryWrapper<ErpVoucherItemDO>()
                        .eq(ErpVoucherItemDO::getVoucherId, v.getId()));
            }
        }

        // 2. 更新状态
        int updateCount = otherReceivableMapper.updateByIdAndStatus(id, receivable.getStatus(),
                new ErpOtherReceivableDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? OTHER_RECEIVABLE_APPROVE_FAIL : OTHER_RECEIVABLE_PROCESS_FAIL);
        }

        // 3. 审批通过：自动生成凭证
        // 业务审核仅更新业务状态；凭证由财务统一预览生成。

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOtherReceivable(List<Long> ids) {
        List<ErpOtherReceivableDO> receivables = otherReceivableMapper.selectByIds(ids);
        if (CollUtil.isEmpty(receivables)) {
            return;
        }
        receivables.forEach(receivable -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(receivable.getStatus())) {
                throw exception(OTHER_RECEIVABLE_DELETE_FAIL_APPROVE, receivable.getNo());
            }
        });
        receivables.forEach(receivable -> {
            otherReceivableMapper.deleteById(receivable.getId());
            otherReceivableItemMapper.deleteByReceivableId(receivable.getId());
        });
    }

    @Override
    public ErpOtherReceivableDO getOtherReceivable(Long id) {
        return otherReceivableMapper.selectById(id);
    }

    @Override
    public PageResult<ErpOtherReceivableDO> getOtherReceivablePage(ErpOtherReceivablePageReqVO pageReqVO) {
        return otherReceivableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpOtherReceivableItemDO> getOtherReceivableItemListByReceivableId(Long receivableId) {
        return otherReceivableItemMapper.selectListByReceivableId(receivableId);
    }

    @Override
    public List<ErpOtherReceivableItemDO> getOtherReceivableItemListByReceivableIds(Collection<Long> receivableIds) {
        if (CollUtil.isEmpty(receivableIds)) {
            return Collections.emptyList();
        }
        return otherReceivableItemMapper.selectListByReceivableIds(receivableIds);
    }

    // ==================== 私有辅助 ====================

    private ErpOtherReceivableDO validateOtherReceivableExists(Long id) {
        ErpOtherReceivableDO receivable = otherReceivableMapper.selectById(id);
        if (receivable == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
        return receivable;
    }

    private void calculateTotalAmount(ErpOtherReceivableDO receivable, List<ErpOtherReceivableSaveReqVO.Item> items) {
        BigDecimal totalAmount = items.stream()
                .map(ErpOtherReceivableSaveReqVO.Item::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receivable.setTotalAmount(totalAmount);
        BigDecimal discountAmount = receivable.getDiscountAmount() == null ? BigDecimal.ZERO : receivable.getDiscountAmount();
        receivable.setActualAmount(totalAmount.subtract(discountAmount));
    }

    private void updateOtherReceivableItemList(Long receivableId, List<ErpOtherReceivableItemDO> newList) {
        List<ErpOtherReceivableItemDO> oldList = otherReceivableItemMapper.selectListByReceivableId(receivableId);
        List<List<ErpOtherReceivableItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setReceivableId(receivableId));
            otherReceivableItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            otherReceivableItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            otherReceivableItemMapper.deleteByIds(convertList(diffList.get(2), ErpOtherReceivableItemDO::getId));
        }
    }

}
