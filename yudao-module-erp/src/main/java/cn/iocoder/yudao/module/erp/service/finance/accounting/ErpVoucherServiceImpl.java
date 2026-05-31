package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpAccountingSubjectMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceTypeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 凭证 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpVoucherServiceImpl implements ErpVoucherService {

    private static final int VOUCHER_NO_GENERATE_MAX_RETRY = 10;

    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;
    @Resource
    private ErpAccountingSubjectMapper subjectMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucher(ErpVoucherSaveReqVO createReqVO) {
        // 1. 校验分录 + 借贷平衡 + 末级科目
        List<ErpVoucherItemDO> items = validateVoucherItems(createReqVO.getItems());

        // 2. 生成凭证编号（S3 修复：用业务凭证日期决定月份，避免跨月凭证号穿越）
        String voucherWord = ObjectUtil.defaultIfNull(createReqVO.getVoucherWord(), ErpNoRedisDAO.VOUCHER_WORD_DEFAULT);
        YearMonth voucherYM = YearMonth.from(
                createReqVO.getVoucherDate() != null ? createReqVO.getVoucherDate() : LocalDate.now());
        String voucherNo = generateAvailableVoucherNo(voucherWord, voucherYM);

        // 3. 组装并插入主表
        ErpVoucherDO voucher = BeanUtils.toBean(createReqVO, ErpVoucherDO.class);
        voucher.setVoucherWord(voucherWord).setVoucherNo(voucherNo);
        fillUserNicknames(voucher, createReqVO);
        fillPeriodAndAggregate(voucher, items);
        voucher.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        voucher.setSourceType(ErpVoucherSourceTypeEnum.MANUAL.getType());
        voucher.setGenerateBusinessDoc(false);
        fillMakerUser(voucher);
        voucherMapper.insert(voucher);

        // 4. 插入分录子表
        items.forEach(item -> item.setVoucherId(voucher.getId()));
        voucherItemMapper.insertBatch(items);
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucher(ErpVoucherSaveReqVO updateReqVO) {
        // 1. 校验存在 + 状态
        if (updateReqVO.getId() == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        ErpVoucherDO voucher = validateVoucherExists(updateReqVO.getId());
        if (!ErpVoucherAuditStatusEnum.PROCESS.getStatus().equals(voucher.getAuditStatus())) {
            throw new ServiceException(VOUCHER_AUDIT_FAIL.getCode(), "已审核的凭证不可修改");
        }
        // 2. 校验分录 + 借贷平衡
        List<ErpVoucherItemDO> items = validateVoucherItems(updateReqVO.getItems());

        // 3. 更新主表
        ErpVoucherDO updateObj = BeanUtils.toBean(updateReqVO, ErpVoucherDO.class);
        // 保留原 voucherWord/voucherNo/auditStatus/sourceType 等不变
        updateObj.setVoucherWord(voucher.getVoucherWord());
        updateObj.setVoucherNo(voucher.getVoucherNo());
        updateObj.setAuditStatus(voucher.getAuditStatus());
        updateObj.setSourceType(voucher.getSourceType());
        updateObj.setSourceBizType(voucher.getSourceBizType());
        updateObj.setSourceBizId(voucher.getSourceBizId());
        updateObj.setSourceBizNo(voucher.getSourceBizNo());
        updateObj.setSourceBizAmount(voucher.getSourceBizAmount());
        updateObj.setGenerateBusinessDoc(voucher.getGenerateBusinessDoc());
        updateObj.setMakerUserId(voucher.getMakerUserId());
        updateObj.setMakerUserName(voucher.getMakerUserName());
        fillUserNicknames(updateObj, updateReqVO);
        fillPeriodAndAggregate(updateObj, items);
        voucherMapper.updateById(updateObj);

        // 4. 删除旧分录 + 插入新分录
        voucherItemMapper.deleteByVoucherId(voucher.getId());
        items.forEach(item -> item.setVoucherId(voucher.getId()));
        voucherItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVoucher(Long id) {
        // 1. 校验存在 + 状态
        ErpVoucherDO voucher = validateVoucherExists(id);
        if (!ErpVoucherAuditStatusEnum.PROCESS.getStatus().equals(voucher.getAuditStatus())) {
            throw new ServiceException(VOUCHER_AUDIT_FAIL.getCode(), "已审核的凭证不可删除");
        }
        // 2. 删除主表 + 子表
        voucherMapper.deleteById(id);
        voucherItemMapper.deleteByVoucherId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVoucher(Long id) {
        // 1. 校验存在
        ErpVoucherDO voucher = validateVoucherExists(id);
        if (!ErpVoucherAuditStatusEnum.PROCESS.getStatus().equals(voucher.getAuditStatus())) {
            throw exception(VOUCHER_AUDIT_FAIL);
        }
        // 2. 借贷平衡再校验一次（防止被绕过）
        List<ErpVoucherItemDO> items = voucherItemMapper.selectListByVoucherId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(VOUCHER_ITEM_EMPTY);
        }
        validateDebitCreditBalance(items);

        // 3. 乐观锁更新审核状态
        Long auditorUserId = SecurityFrameworkUtils.getLoginUserId();
        String auditorUserName = getNicknameSafe(auditorUserId);
        ErpVoucherDO updateObj = new ErpVoucherDO()
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus())
                .setAuditTime(LocalDateTime.now())
                .setAuditorUserId(auditorUserId)
                .setAuditorUserName(auditorUserName);
        int rows = voucherMapper.updateByIdAndAuditStatus(id,
                ErpVoucherAuditStatusEnum.PROCESS.getStatus(), updateObj);
        if (rows == 0) {
            throw exception(VOUCHER_AUDIT_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processVoucher(Long id) {
        // 1. 校验存在 + 状态
        ErpVoucherDO voucher = validateVoucherExists(id);
        if (!ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(voucher.getAuditStatus())) {
            throw exception(VOUCHER_PROCESS_FAIL);
        }
        // 2. 乐观锁回退到未审核（与项目其他模块一致）
        ErpVoucherDO updateObj = new ErpVoucherDO()
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus())
                .setAuditTime(null)
                .setAuditorUserId(null)
                .setAuditorUserName(null);
        int rows = voucherMapper.updateByIdAndAuditStatus(id,
                ErpVoucherAuditStatusEnum.APPROVE.getStatus(), updateObj);
        if (rows == 0) {
            throw exception(VOUCHER_PROCESS_FAIL);
        }
    }

    @Override
    public ErpVoucherDO getVoucher(Long id) {
        return voucherMapper.selectById(id);
    }

    @Override
    public PageResult<ErpVoucherDO> getVoucherPage(ErpVoucherPageReqVO pageReqVO) {
        return voucherMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpVoucherDO> getVoucherList(List<Long> ids) {
        return voucherMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpVoucherItemDO> getVoucherItemListByVoucherId(Long voucherId) {
        return voucherItemMapper.selectListByVoucherId(voucherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucherFromBiz(Integer sourceBizType, Long sourceBizId, String sourceBizNo,
                                     BigDecimal sourceBizAmount, LocalDate voucherDate, String summary,
                                     List<ErpVoucherItemDO> items) {
        replaceUnapprovedVoucherFromBiz(sourceBizType, sourceBizId, sourceBizNo);

        // 1. 校验分录非空 + 借贷平衡
        if (CollUtil.isEmpty(items)) {
            throw exception(VOUCHER_ITEM_EMPTY);
        }
        // 标准化金额（null → ZERO）+ 行号补齐
        int autoLineNo = 1;
        for (ErpVoucherItemDO item : items) {
            item.setDebitAmount(ObjectUtil.defaultIfNull(item.getDebitAmount(), BigDecimal.ZERO));
            item.setCreditAmount(ObjectUtil.defaultIfNull(item.getCreditAmount(), BigDecimal.ZERO));
            if (item.getLineNo() == null) {
                item.setLineNo(autoLineNo);
            }
            autoLineNo++;
        }
        validateDebitCreditBalance(items);

        // 2. 生成凭证编号（S3 修复：用业务凭证日期决定月份，避免跨月凭证号穿越）
        String voucherWord = ErpNoRedisDAO.VOUCHER_WORD_DEFAULT;
        YearMonth voucherYM = YearMonth.from(voucherDate != null ? voucherDate : LocalDate.now());
        String voucherNo = generateAvailableVoucherNo(voucherWord, voucherYM);

        // 3. 组装并插入主表
        ErpVoucherDO voucher = new ErpVoucherDO()
                .setVoucherWord(voucherWord)
                .setVoucherNo(voucherNo)
                .setVoucherDate(voucherDate)
                .setSummary(summary)
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus())
                .setSourceType(ErpVoucherSourceTypeEnum.AUTO.getType())
                .setSourceBizType(sourceBizType)
                .setSourceBizId(sourceBizId)
                .setSourceBizNo(sourceBizNo)
                .setSourceBizAmount(sourceBizAmount)
                .setGenerateBusinessDoc(true);
        fillPeriodAndAggregate(voucher, items);
        fillMakerUser(voucher);
        voucherMapper.insert(voucher);

        // 4. 插入分录子表
        items.forEach(item -> item.setVoucherId(voucher.getId()));
        voucherItemMapper.insertBatch(items);
        return voucher.getId();
    }

    private void replaceUnapprovedVoucherFromBiz(Integer sourceBizType, Long sourceBizId, String sourceBizNo) {
        if (sourceBizType == null || sourceBizId == null) {
            return;
        }
        List<ErpVoucherDO> oldVouchers = voucherMapper.selectListByBiz(sourceBizType, sourceBizId);
        if (CollUtil.isEmpty(oldVouchers)) {
            return;
        }
        for (ErpVoucherDO oldVoucher : oldVouchers) {
            if (ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(oldVoucher.getAuditStatus())) {
                throw exception(VOUCHER_BIZ_APPROVED_EXISTS,
                        ObjectUtil.defaultIfNull(sourceBizNo, oldVoucher.getSourceBizNo()));
            }
        }
        for (ErpVoucherDO oldVoucher : oldVouchers) {
            voucherMapper.deleteById(oldVoucher.getId());
            voucherItemMapper.deleteByVoucherId(oldVoucher.getId());
        }
    }

    // ==================== 私有辅助 ====================

    private String generateAvailableVoucherNo(String voucherWord, YearMonth voucherYM) {
        for (int i = 0; i < VOUCHER_NO_GENERATE_MAX_RETRY; i++) {
            String voucherNo = noRedisDAO.generateMonthly(voucherWord, voucherYM);
            if (voucherMapper.selectByVoucherNo(voucherNo) == null) {
                return voucherNo;
            }
        }
        throw exception(VOUCHER_NO_EXISTS);
    }

    private ErpVoucherDO validateVoucherExists(Long id) {
        if (id == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        ErpVoucherDO voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
        return voucher;
    }

    /**
     * 校验分录列表：非空 + 同行借贷不能并存且不能全为零 + 借贷平衡 + 末级科目；并冗余科目编码/名称、行号
     */
    private List<ErpVoucherItemDO> validateVoucherItems(List<cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherItemSaveReqVO> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            throw exception(VOUCHER_ITEM_EMPTY);
        }
        // 1. 转 DO
        List<ErpVoucherItemDO> items = new ArrayList<>(reqItems.size());
        int autoLineNo = 1;
        for (cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherItemSaveReqVO reqItem : reqItems) {
            ErpVoucherItemDO item = BeanUtils.toBean(reqItem, ErpVoucherItemDO.class);
            BigDecimal debit = ObjectUtil.defaultIfNull(item.getDebitAmount(), BigDecimal.ZERO);
            BigDecimal credit = ObjectUtil.defaultIfNull(item.getCreditAmount(), BigDecimal.ZERO);
            // 2. 同行借/贷不可同时 > 0；且至少一非 0
            boolean debitPositive = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean creditPositive = credit.compareTo(BigDecimal.ZERO) > 0;
            if (debitPositive && creditPositive) {
                throw new ServiceException(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(),
                        "分录第 " + autoLineNo + " 行借贷不能同时录入");
            }
            if (!debitPositive && !creditPositive) {
                throw new ServiceException(VOUCHER_DEBIT_CREDIT_NOT_BALANCE.getCode(),
                        "分录第 " + autoLineNo + " 行借方或贷方至少需要一个有金额");
            }
            item.setDebitAmount(debit);
            item.setCreditAmount(credit);
            // 行号：reqItem 没传则自增
            item.setLineNo(ObjectUtil.defaultIfNull(reqItem.getLineNo(), autoLineNo));
            autoLineNo++;
            items.add(item);
        }

        // 3. 借贷平衡
        validateDebitCreditBalance(items);

        // 4. 校验科目末级 + 冗余科目编码/名称
        Set<Long> subjectIds = convertSet(items, ErpVoucherItemDO::getSubjectId);
        List<ErpAccountingSubjectDO> subjects = subjectMapper.selectByIds(new HashSet<>(subjectIds));
        if (subjects.size() != subjectIds.size()) {
            throw exception(ACCOUNTING_SUBJECT_NOT_EXISTS);
        }
        Map<Long, ErpAccountingSubjectDO> subjectMap = convertSubjectMap(subjects);
        for (ErpVoucherItemDO item : items) {
            ErpAccountingSubjectDO subject = subjectMap.get(item.getSubjectId());
            if (subject == null) {
                throw exception(ACCOUNTING_SUBJECT_NOT_EXISTS);
            }
            if (Boolean.FALSE.equals(subject.getIsLeaf())) {
                throw exception(ACCOUNTING_SUBJECT_NOT_LEAF);
            }
            item.setSubjectCode(subject.getSubjectCode());
            item.setSubjectName(subject.getSubjectName());
        }
        return items;
    }

    private Map<Long, ErpAccountingSubjectDO> convertSubjectMap(List<ErpAccountingSubjectDO> subjects) {
        return cn.iocoder.yudao.framework.common.util.collection.CollectionUtils
                .convertMap(subjects, ErpAccountingSubjectDO::getId);
    }

    /**
     * 借贷平衡校验：sum(debit) == sum(credit)（用 BigDecimal#compareTo）
     */
    private void validateDebitCreditBalance(List<ErpVoucherItemDO> items) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpVoucherItemDO item : items) {
            totalDebit = totalDebit.add(ObjectUtil.defaultIfNull(item.getDebitAmount(), BigDecimal.ZERO));
            totalCredit = totalCredit.add(ObjectUtil.defaultIfNull(item.getCreditAmount(), BigDecimal.ZERO));
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw exception(VOUCHER_DEBIT_CREDIT_NOT_BALANCE,
                    totalDebit.toPlainString(), totalCredit.toPlainString());
        }
    }

    /**
     * 推断归属年月、聚合借贷合计、补充摘要
     */
    private void fillPeriodAndAggregate(ErpVoucherDO voucher, List<ErpVoucherItemDO> items) {
        LocalDate voucherDate = voucher.getVoucherDate();
        if (voucher.getPeriodYear() == null && voucherDate != null) {
            voucher.setPeriodYear(voucherDate.getYear());
        }
        if (voucher.getPeriodMonth() == null && voucherDate != null) {
            voucher.setPeriodMonth(voucherDate.getMonthValue());
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (ErpVoucherItemDO item : items) {
            totalDebit = totalDebit.add(ObjectUtil.defaultIfNull(item.getDebitAmount(), BigDecimal.ZERO));
            totalCredit = totalCredit.add(ObjectUtil.defaultIfNull(item.getCreditAmount(), BigDecimal.ZERO));
        }
        voucher.setTotalDebit(totalDebit).setTotalCredit(totalCredit);
        if (CollUtil.isEmpty(items) ? false : (voucher.getSummary() == null || voucher.getSummary().isEmpty())) {
            voucher.setSummary(items.get(0).getSummary());
        }
        if (voucher.getAttachmentCount() == null) {
            voucher.setAttachmentCount(0);
        }
    }

    private void fillMakerUser(ErpVoucherDO voucher) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        voucher.setMakerUserId(userId);
        voucher.setMakerUserName(getNicknameSafe(userId));
    }

    /**
     * 把前端传来的 bookkeeperUserId / cashierUserId / supervisorUserId 翻译成 nickname 写入 DO 的字符串字段
     */
    private void fillUserNicknames(ErpVoucherDO voucher, ErpVoucherSaveReqVO reqVO) {
        if (reqVO.getBookkeeperUserId() != null) {
            voucher.setBookkeeper(getNicknameSafe(reqVO.getBookkeeperUserId()));
        }
        if (reqVO.getCashierUserId() != null) {
            voucher.setCashier(getNicknameSafe(reqVO.getCashierUserId()));
        }
        if (reqVO.getSupervisorUserId() != null) {
            voucher.setSupervisor(getNicknameSafe(reqVO.getSupervisorUserId()));
        }
    }

    private String getNicknameSafe(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            return user != null ? user.getNickname() : null;
        } catch (Exception e) {
            return null;
        }
    }

}
