package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.finance.ErpMiscTransferOffsetConstants;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应收查看/导出的附加信息。不得由核销校验、抵销或主账户余额计算调用。
 */
@Service
public class ErpReceivableDetailDisplayService {
    private static final int BATCH_SIZE = 500;
    @Resource private ErpSaleOutMapper saleOutMapper;
    @Resource private ErpSaleReturnMapper saleReturnMapper;
    @Resource private ErpSalePriceAdjustMapper priceAdjustMapper;
    @Resource private ErpFinanceReceiptMapper receiptMapper;
    @Resource private ErpReceivableOtherMapper otherMapper;
    @Resource private ErpReceivableMiscMapper miscMapper;
    @Resource private ErpVoucherMapper voucherMapper;
    @Resource private ErpCustomerService customerService;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpSaleFieldPermissionMasker saleFieldMasker;
    @Resource private AdminUserApi userApi;
    @Resource private DeptApi deptApi;

    public List<ErpReceivableDetailRespVO> decorate(ErpReceivableDetailReqVO req,
            List<ErpReceivableDetailRespVO> accountingRows, BigDecimal opening, ErpFinanceVisibleScope accountScope) {
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>(accountingRows);
        if (hiddenFields("erp_finance_receivable_account").contains("remark")) {
            rows.stream().filter(row -> "核销".equals(row.getDocType())).forEach(row -> row.setRemark(null));
        }
        Map<ErpReceivableDetailRespVO, Long> auditors = new IdentityHashMap<>();
        fillSources(rows, "销售出库", "erp_sale_out", ids -> saleOutMapper.selectBatchIds(ids), auditors);
        fillSources(rows, "销售退货", "erp_sale_return", ids -> saleReturnMapper.selectBatchIds(ids), auditors);
        fillSources(rows, "销售调价", "erp_sale_price_adjust", ids -> priceAdjustMapper.selectBatchIds(ids), auditors);
        fillSources(rows, "收款单", "erp_finance_receipt", ids -> receiptMapper.selectBatchIds(ids), auditors);
        fillSources(rows, "应收调账", "erp_finance_receivable_other", ids -> otherMapper.selectBatchIds(ids), auditors);
        rows.addAll(buildMiscRows(req, accountScope));
        fillVouchers(rows);
        ErpCustomerDO customer = customerService.getCustomer(req.getCustomerId());
        rows.forEach(row -> row.setCustomerName(customer == null ? null : customer.getName()));
        for (List<Long> ids : batches(auditors.values())) {
            Map<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO> users = userApi.getUserMap(ids);
            auditors.forEach((row, id) -> {
                if (users.get(id) != null) row.setAuditorName(users.get(id).getNickname());
            });
        }
        // 原账务行已完成余额计算。插入独立展示行只携带当时余额，不改变原行。
        rows.sort(Comparator.comparing(ErpReceivableDetailRespVO::getDocDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpReceivableDetailRespVO::getDocType)
                .thenComparing(ErpReceivableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        BigDecimal balance = opening;
        for (ErpReceivableDetailRespVO row : rows) {
            if (Boolean.TRUE.equals(row.getDisplayOnly())) {
                row.setPrevBalance(balance);
                row.setBalance(balance);
            } else {
                balance = row.getBalance();
            }
        }
        return rows;
    }

    private void fillSources(List<ErpReceivableDetailRespVO> rows, String docType, String module,
            Function<Collection<Long>, List<?>> loader, Map<ErpReceivableDetailRespVO, Long> auditors) {
        List<ErpReceivableDetailRespVO> targets = rows.stream()
                .filter(row -> docType.equals(row.getDocType()) && row.getBizId() != null).collect(Collectors.toList());
        Map<Long, Object> sources = new HashMap<>();
        for (List<Long> ids : batches(targets.stream().map(ErpReceivableDetailRespVO::getBizId).collect(Collectors.toList()))) {
            // ID 来自已按客户及账户范围过滤的原账务行，不扩大来源行集合。
            for (Object source : DataPermissionUtils.executeIgnore(() -> loader.apply(ids))) {
                sources.put((Long) value(new BeanWrapperImpl(source), "id"), source);
            }
        }
        Set<String> financeHidden = module.startsWith("erp_sale_") ? Collections.emptySet()
                : hiddenFields(module);
        // 一次明细仅包含一个客户，同类单据复用字段权限，避免逐行解析客户价格等级。
        Set<String> saleHidden = module.startsWith("erp_sale_") && !sources.isEmpty()
                ? normalizeHidden(saleFieldMasker.getHiddenFieldSet(module, sources.values().iterator().next()))
                : Collections.emptySet();
        for (ErpReceivableDetailRespVO row : targets) {
            Object source = sources.get(row.getBizId());
            if (source == null) continue;
            BeanWrapper bean = new BeanWrapperImpl(source);
            Set<String> hidden = module.startsWith("erp_sale_") ? saleHidden : financeHidden;
            row.setDeliveryMethod((String) visible(bean, hidden, "deliveryMethod"));
            row.setSettleMethod((String) visible(bean, hidden, "settleMethod"));
            row.setApproveTime((LocalDateTime) visible(bean, hidden, "approveTime"));
            row.setBillAmount((BigDecimal) visible(bean, hidden, "billAmount"));
            row.setBillNo((String) visible(bean, hidden, "billNo"));
            row.setRemark((String) visible(bean, hidden, "remark"));
            row.setInternalNote((String) visible(bean, hidden, "internalNote"));
            row.setVoucherNo((String) visible(bean, hidden, "voucherNo"));
            Long auditor = (Long) visible(bean, hidden, "auditorId");
            if (auditor != null && !hidden.contains("auditorName")) auditors.put(row, auditor);
            if (module.startsWith("erp_sale_")) {
                String amountField = "销售退货".equals(docType) ? "refundPrice" : "receiptPrice";
                BigDecimal amount = hidden.contains(amountField) ? null : zero(row.getAllocatedAmount()).abs();
                row.setReceivedAmount(amount);
                row.setReceivedWriteOffAmount(amount);
            }
        }
    }

    private List<ErpReceivableDetailRespVO> buildMiscRows(ErpReceivableDetailReqVO req,
                                                        ErpFinanceVisibleScope accountScope) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || !permissionApi.hasAnyPermissions(userId, "erp:receivable-misc:query")) {
            return Collections.emptyList();
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, "erp_finance_receivable_misc");
        ErpFinanceVisibleScope miscScope = ErpFinanceVisibleScope.from(permission, userId);
        if (miscScope == null || !miscScope.hasAccess()) return Collections.emptyList();
        Set<String> hidden = hiddenFields("erp_finance_receivable_misc");
        List<ErpReceivableDetailRespVO> result = new ArrayList<>();
        Long afterId = null;
        while (true) {
            LambdaQueryWrapperX<ErpReceivableMiscDO> query = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                    .eq(ErpReceivableMiscDO::getCustomerId, req.getCustomerId())
                    .eq(ErpReceivableMiscDO::getStatus, 20)
                    .geIfPresent(ErpReceivableMiscDO::getBizTime, req.getStartTime())
                    .ltIfPresent(ErpReceivableMiscDO::getBizTime, req.getEndTime());
            query.and(q -> q.isNull(ErpReceivableMiscDO::getSourceType).or()
                            .ne(ErpReceivableMiscDO::getSourceType, ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE));
            if (Boolean.TRUE.equals(req.getDeptUnassigned())) query.isNull(ErpReceivableMiscDO::getDeptId);
            else query.eqIfPresent(ErpReceivableMiscDO::getDeptId, req.getDeptId());
            applyScope(query, accountScope, ErpReceivableMiscDO::getDeptId, ErpReceivableMiscDO::getHandlerId);
            applyScope(query, miscScope, ErpReceivableMiscDO::getDeptId, ErpReceivableMiscDO::getHandlerId);
            if (afterId != null) query.gt(ErpReceivableMiscDO::getId, afterId);
            query.orderByAsc(ErpReceivableMiscDO::getId).last("LIMIT 500");
            List<ErpReceivableMiscDO> originals = DataPermissionUtils.executeIgnore(() -> miscMapper.selectList(query));
            if (originals.isEmpty()) break;
            List<Long> ids = originals.stream().map(ErpReceivableMiscDO::getId).collect(Collectors.toList());
            Map<Long, BigDecimal> settled = miscMapper.selectSettlementAmountSumMapBySourceMiscIds(ids,
                    ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE);
            for (ErpReceivableMiscDO item : originals) {
                ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
                row.setDocType("其他应收");
                row.setBizType(24);
                row.setBizId(item.getId());
                row.setDocNo(item.getNo());
                row.setDocDate(item.getBizTime());
                row.setDeptId(item.getDeptId());
                row.setDisplayOnly(true);
                row.setIncreaseAmount(BigDecimal.ZERO);
                row.setOtherReceivableAmount(BigDecimal.ZERO);
                row.setReceiptAmount(BigDecimal.ZERO);
                row.setWriteOffAmount(BigDecimal.ZERO);
                row.setMiscReceivableAmount(hidden.contains("amount") ? null : item.getAmount());
                BigDecimal amount = hidden.contains("amount") || hidden.contains("settledAmount") ? null
                        : zero(settled.get(item.getId())).abs();
                row.setReceivedAmount(amount);
                row.setReceivedWriteOffAmount(amount);
                row.setAllocatedAmount(amount);
                row.setWriteOffBaseAmount(hidden.contains("amount") ? null : zero(item.getAmount()).abs());
                row.setRemark(hidden.contains("remark") ? null : item.getRemark());
                result.add(row);
            }
            afterId = originals.get(originals.size() - 1).getId();
            if (originals.size() < BATCH_SIZE) break;
        }
        for (List<Long> ids : batches(result.stream().map(ErpReceivableDetailRespVO::getDeptId).collect(Collectors.toList()))) {
            Map<Long, cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO> departments = deptApi.getDeptMap(ids);
            result.forEach(row -> {
                if (departments.get(row.getDeptId()) != null) row.setDeptName(departments.get(row.getDeptId()).getName());
            });
        }
        return result;
    }

    private void fillVouchers(List<ErpReceivableDetailRespVO> rows) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || !permissionApi.hasAnyPermissions(userId, "erp:voucher:query")
                || hiddenFields("erp_voucher").contains("voucherNo")) return;
        Map<String, Integer> types = new LinkedHashMap<>();
        types.put("销售出库", 2);
        types.put("销售退货", 3);
        types.put("收款单", 6);
        types.forEach((docType, sourceType) -> {
            String module = "销售出库".equals(docType) ? "erp_sale_out"
                    : "销售退货".equals(docType) ? "erp_sale_return" : "erp_finance_receipt";
            if (hiddenFields(module).contains("voucherNo")) return;
            List<ErpReceivableDetailRespVO> targets = rows.stream().filter(row -> docType.equals(row.getDocType())
                    && row.getBizId() != null && (row.getVoucherNo() == null || row.getVoucherNo().trim().isEmpty()))
                    .collect(Collectors.toList());
            Map<Long, Set<String>> numbers = new HashMap<>();
            for (List<Long> ids : batches(targets.stream().map(ErpReceivableDetailRespVO::getBizId).collect(Collectors.toList()))) {
                // 保持凭证自身的数据权限；不按业务单号猜测关联。
                List<ErpVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                        .eq(ErpVoucherDO::getSourceBizType, sourceType).in(ErpVoucherDO::getSourceBizId, ids)
                        .orderByAsc(ErpVoucherDO::getId));
                for (ErpVoucherDO voucher : vouchers) {
                    if (voucher.getVoucherNo() != null && !voucher.getVoucherNo().trim().isEmpty())
                        numbers.computeIfAbsent(voucher.getSourceBizId(), key -> new LinkedHashSet<>()).add(voucher.getVoucherNo());
                }
            }
            targets.forEach(row -> {
                if (numbers.containsKey(row.getBizId())) row.setVoucherNo(String.join("、", numbers.get(row.getBizId())));
            });
        });
    }

    private static Object value(BeanWrapper bean, String field) {
        return bean.isReadableProperty(field) ? bean.getPropertyValue(field) : null;
    }

    private Set<String> hiddenFields(String module) {
        return normalizeHidden(permissionApi.getCurrentUserHiddenFields(module));
    }

    private static Set<String> normalizeHidden(Collection<String> fields) {
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private static Object visible(BeanWrapper bean, Set<String> hidden, String field) {
        return hidden.contains(field) ? null : value(bean, field);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static List<List<Long>> batches(Collection<Long> values) {
        List<Long> ids = values.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<List<Long>> result = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += BATCH_SIZE)
            result.add(ids.subList(start, Math.min(start + BATCH_SIZE, ids.size())));
        return result;
    }

    private static <T> void applyScope(LambdaQueryWrapperX<T> query, ErpFinanceVisibleScope scope,
                                      SFunction<T, ?> dept, SFunction<T, ?> handler) {
        if (scope.isAll()) return;
        query.and(q -> {
            if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null)
                q.in(dept, scope.getDeptIds()).or().eq(handler, scope.getSelfUserId());
            else if (!scope.getDeptIds().isEmpty()) q.in(dept, scope.getDeptIds());
            else q.eq(handler, scope.getSelfUserId());
        });
    }
}
