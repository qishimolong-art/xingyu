package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
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
 * ERP 预收账款单 Service 实现类
 */
@Service
@Validated
public class ErpPreReceivableServiceImpl implements ErpPreReceivableService {

    @Resource
    private ErpPreReceivableMapper preReceivableMapper;
    @Resource
    private ErpPreReceivableItemMapper preReceivableItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpAccountService accountService;
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
    public Long createPreReceivable(ErpPreReceivableSaveReqVO createReqVO) {
        // 1.1 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.2 生成单号
        String no = noRedisDAO.generate("YSZK");
        if (preReceivableMapper.selectByNo(no) != null) {
            throw exception(PRE_RECEIVABLE_NO_EXISTS);
        }

        // 2.1 插入主表
        ErpPreReceivableDO preReceivable = BeanUtils.toBean(createReqVO, ErpPreReceivableDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        calculateTotalAmount(preReceivable, createReqVO.getItems());
        preReceivableMapper.insert(preReceivable);

        // 2.2 插入子表
        List<ErpPreReceivableItemDO> items = convertList(createReqVO.getItems(),
                item -> BeanUtils.toBean(item, ErpPreReceivableItemDO.class,
                        o -> o.setPreReceivableId(preReceivable.getId())));
        preReceivableItemMapper.insertBatch(items);
        return preReceivable.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreReceivable(ErpPreReceivableSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPreReceivableDO preReceivable = validatePreReceivableExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(preReceivable.getStatus())) {
            throw exception(PRE_RECEIVABLE_UPDATE_FAIL_APPROVE, preReceivable.getNo());
        }
        // 1.2 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }

        // 2.1 乐观锁更新主表（S12 修复：防止并发审核导致已审核单据被静默改写）
        ErpPreReceivableDO updateObj = BeanUtils.toBean(updateReqVO, ErpPreReceivableDO.class);
        calculateTotalAmount(updateObj, updateReqVO.getItems());
        int affected = preReceivableMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(PRE_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED);
        }

        // 2.2 更新子表（先删后插）
        preReceivableItemMapper.delete(new LambdaQueryWrapper<ErpPreReceivableItemDO>()
                .eq(ErpPreReceivableItemDO::getPreReceivableId, updateReqVO.getId()));
        List<ErpPreReceivableItemDO> items = convertList(updateReqVO.getItems(),
                item -> BeanUtils.toBean(item, ErpPreReceivableItemDO.class,
                        o -> o.setPreReceivableId(updateReqVO.getId())));
        preReceivableItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePreReceivable(List<Long> ids) {
        List<ErpPreReceivableDO> list = preReceivableMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(preReceivable -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(preReceivable.getStatus())) {
                throw exception(PRE_RECEIVABLE_DELETE_FAIL_APPROVE, preReceivable.getNo());
            }
        });
        // 删除主表 + 子表
        preReceivableMapper.deleteByIds(ids);
        ids.forEach(id -> preReceivableItemMapper.delete(new LambdaQueryWrapper<ErpPreReceivableItemDO>()
                .eq(ErpPreReceivableItemDO::getPreReceivableId, id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreReceivableStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPreReceivableDO preReceivable = validatePreReceivableExists(id);
        // 1.2 校验状态
        if (preReceivable.getStatus().equals(status)) {
            throw exception(approve ? PRE_RECEIVABLE_APPROVE_FAIL : PRE_RECEIVABLE_PROCESS_FAIL);
        }

        // 1.3 反审：校验关联凭证
        if (!approve) {
            List<ErpVoucherDO> vouchers = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.PRE_RECEIVABLE.getType(), id);
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
        int updateCount = preReceivableMapper.updateByIdAndStatus(id, preReceivable.getStatus(),
                new ErpPreReceivableDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PRE_RECEIVABLE_APPROVE_FAIL : PRE_RECEIVABLE_PROCESS_FAIL);
        }

        // 3. 审批通过：自动生成凭证
        // 业务审核仅更新业务状态；凭证由财务统一预览生成。

    }

    @Override
    public ErpPreReceivableDO getPreReceivable(Long id) {
        return preReceivableMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPreReceivableDO> getPreReceivablePage(ErpPreReceivablePageReqVO pageReqVO) {
        return preReceivableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPreReceivableItemDO> getPreReceivableItemListByPreReceivableId(Long preReceivableId) {
        return preReceivableItemMapper.selectListByPreReceivableId(preReceivableId);
    }

    @Override
    public List<ErpPreReceivableItemDO> getPreReceivableItemListByPreReceivableIds(Collection<Long> preReceivableIds) {
        if (CollUtil.isEmpty(preReceivableIds)) {
            return Collections.emptyList();
        }
        return preReceivableItemMapper.selectListByPreReceivableIds(preReceivableIds);
    }

    // ==================== 私有方法 ====================

    private ErpPreReceivableDO validatePreReceivableExists(Long id) {
        ErpPreReceivableDO preReceivable = preReceivableMapper.selectById(id);
        if (preReceivable == null) {
            throw exception(PRE_RECEIVABLE_NOT_EXISTS);
        }
        return preReceivable;
    }

    private void calculateTotalAmount(ErpPreReceivableDO preReceivable, List<ErpPreReceivableSaveReqVO.Item> items) {
        BigDecimal totalAmount = getSumValue(items, ErpPreReceivableSaveReqVO.Item::getAmount, BigDecimal::add, BigDecimal.ZERO);
        preReceivable.setTotalAmount(totalAmount);
        BigDecimal discountAmount = preReceivable.getDiscountAmount() != null ? preReceivable.getDiscountAmount() : BigDecimal.ZERO;
        preReceivable.setActualAmount(totalAmount.subtract(discountAmount));
    }

}
