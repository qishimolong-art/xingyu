package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableReportMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpPayableReportServiceImpl implements ErpPayableReportService {

    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String REPORT_MODULE = "erp_finance_payable_report";

    @Resource
    private ErpPayableReportMapper payableReportMapper;
    @Resource
    private ErpPayableMiscMapper payableMiscMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public PageResult<ErpPayableReportRespVO> getPayableReportPage(ErpPayableReportPageReqVO reqVO) {
        ErpFinanceVisibleScope supplierScope = getVisibleScope(SUPPLIER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(supplierScope) || !hasAccess(documentScope)) {
            return PageResult.empty();
        }
        return DataPermissionUtils.executeIgnore(() -> payableReportMapper.selectPage(reqVO,
                supplierScope.getDeptIds(), selfUserIdText(supplierScope), supplierScope.isAll(),
                documentScope.getDeptIds(), documentScope.getSelfUserId(), documentScope.isAll()));
    }

    @Override
    public List<ErpPayableReportDetailRespVO> getPayableReportDetailList(ErpPayableReportDetailReqVO reqVO) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        ErpFinanceVisibleScope supplierScope = getVisibleScope(SUPPLIER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(supplierScope) || !hasAccess(documentScope) || !canSeeSupplier(reqVO.getSupplierId(), supplierScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildDetailList(reqVO, documentScope));
    }

    private List<ErpPayableReportDetailRespVO> buildDetailList(ErpPayableReportDetailReqVO reqVO,
                                                               ErpFinanceVisibleScope scope) {
        BigDecimal balance = getInitialBalance(reqVO, scope);
        List<ErpPayableMiscDO> miscRows = selectOtherList(reqVO, scope, false);
        List<ErpPayableReportDetailRespVO> rows = miscRows.stream()
                .map(item -> buildRow(item, BigDecimal.ZERO))
                .collect(Collectors.toList());
        for (ErpPayableReportDetailRespVO row : rows) {
            row.setPrevBalance(balance);
            balance = balance.add(getChangeAmount(row));
            row.setBalance(balance);
        }
        return rows;
    }

    private BigDecimal getInitialBalance(ErpPayableReportDetailReqVO reqVO, ErpFinanceVisibleScope scope) {
        if (reqVO.getStartDate() == null) {
            return BigDecimal.ZERO;
        }
        ErpPayableReportDetailReqVO copy = new ErpPayableReportDetailReqVO();
        copy.setSupplierId(reqVO.getSupplierId());
        copy.setBizTime(new java.time.LocalDateTime[]{null, reqVO.getStartDate().atStartOfDay()});
        List<ErpPayableMiscDO> rows = selectOtherList(copy, scope, true);
        return rows.stream()
                .map(item -> amount(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ErpPayableMiscDO> selectOtherList(ErpPayableReportDetailReqVO reqVO,
                                                    ErpFinanceVisibleScope scope,
                                                    boolean beforeStartDate) {
        LambdaQueryWrapperX<ErpPayableMiscDO> query = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (beforeStartDate) {
            query.lt(ErpPayableMiscDO::getBizTime, reqVO.getEndTime());
        } else {
            query.geIfPresent(ErpPayableMiscDO::getBizTime, reqVO.getStartTime())
                    .leIfPresent(ErpPayableMiscDO::getBizTime, reqVO.getEndTime());
        }
        applyScope(query, scope);
        return payableMiscMapper.selectList(query.orderByAsc(ErpPayableMiscDO::getBizTime)
                .orderByAsc(ErpPayableMiscDO::getNo)
                .orderByAsc(ErpPayableMiscDO::getId));
    }

    private ErpPayableReportDetailRespVO buildRow(ErpPayableMiscDO item, BigDecimal paidAmount) {
        BigDecimal payableAmount = amount(item.getAmount());
        ErpPayableReportDetailRespVO row = new ErpPayableReportDetailRespVO();
        row.setDocType("其他应付");
        row.setBizType(ErpBizTypeEnum.PAYABLE_MISC.getType());
        row.setBizId(item.getId());
        row.setDocDate(item.getBizTime());
        row.setDocNo(item.getNo());
        row.setIncreaseAmount(payableAmount);
        row.setPaymentAmount(amount(paidAmount));
        row.setWriteOffAmount(BigDecimal.ZERO);
        row.setAllocatedAmount(amount(paidAmount).abs());
        row.setWriteOffBaseAmount(payableAmount);
        row.setRemark(item.getRemark());
        row.setFileUrl(item.getFileUrl());
        return row;
    }

    private BigDecimal getChangeAmount(ErpPayableReportDetailRespVO row) {
        return amount(row.getIncreaseAmount()).subtract(amount(row.getPaymentAmount()));
    }

    private boolean canSeeSupplier(Long supplierId, ErpFinanceVisibleScope supplierScope) {
        ErpPayableReportPageReqVO reqVO = new ErpPayableReportPageReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setShowZeroBalance(true);
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return !payableReportMapper.selectList(reqVO, supplierScope.getDeptIds(), selfUserIdText(supplierScope),
                supplierScope.isAll(), Collections.emptyList(), null, true).isEmpty();
    }

    private ErpFinanceVisibleScope getVisibleScope(String module) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        DeptDataPermissionRespDTO permission = loginUserId == null ? null
                : permissionApi.getDeptDataPermission(loginUserId, module);
        return ErpFinanceVisibleScope.from(permission, loginUserId);
    }

    private void applyScope(LambdaQueryWrapperX<ErpPayableMiscDO> query, ErpFinanceVisibleScope scope) {
        if (scope.isAll()) {
            return;
        }
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(wrapper -> wrapper.in(ErpPayableMiscDO::getDeptId, scope.getDeptIds())
                    .or().eq(ErpPayableMiscDO::getHandlerId, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(ErpPayableMiscDO::getDeptId, scope.getDeptIds());
        } else {
            query.eq(ErpPayableMiscDO::getHandlerId, scope.getSelfUserId());
        }
    }

    private boolean hasAccess(ErpFinanceVisibleScope scope) {
        return scope != null && scope.hasAccess();
    }

    private String selfUserIdText(ErpFinanceVisibleScope scope) {
        return scope.getSelfUserId() == null ? null : String.valueOf(scope.getSelfUserId());
    }

    private BigDecimal amount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
