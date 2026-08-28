package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableReportMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpReceivableReportServiceImpl implements ErpReceivableReportService {

    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final String REPORT_MODULE = "erp_finance_receivable_report";

    @Resource
    private ErpReceivableReportMapper receivableReportMapper;
    @Resource
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public PageResult<ErpReceivableReportRespVO> getReceivableReportPage(ErpReceivableReportPageReqVO reqVO) {
        ErpFinanceVisibleScope customerScope = getVisibleScope(CUSTOMER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(customerScope) || !hasAccess(documentScope)) {
            return PageResult.empty();
        }
        return DataPermissionUtils.executeIgnore(() -> receivableReportMapper.selectPage(reqVO,
                customerScope.getDeptIds(), selfUserIdText(customerScope), customerScope.isAll(),
                documentScope.getDeptIds(), documentScope.getSelfUserId(), documentScope.isAll()));
    }

    @Override
    public List<ErpReceivableReportDetailRespVO> getReceivableReportDetailList(ErpReceivableReportDetailReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ErpFinanceVisibleScope customerScope = getVisibleScope(CUSTOMER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(customerScope) || !hasAccess(documentScope) || !canSeeCustomer(reqVO.getCustomerId(), customerScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildDetailList(reqVO, documentScope));
    }

    private List<ErpReceivableReportDetailRespVO> buildDetailList(ErpReceivableReportDetailReqVO reqVO,
                                                                  ErpFinanceVisibleScope scope) {
        BigDecimal balance = getInitialBalance(reqVO, scope);
        List<ErpReceivableReportDetailRespVO> rows = selectOtherList(reqVO, scope, false).stream()
                .map(this::buildRow)
                .collect(Collectors.toList());
        for (ErpReceivableReportDetailRespVO row : rows) {
            row.setPrevBalance(balance);
            balance = balance.add(getChangeAmount(row));
            row.setBalance(balance);
        }
        return rows;
    }

    private BigDecimal getInitialBalance(ErpReceivableReportDetailReqVO reqVO, ErpFinanceVisibleScope scope) {
        if (reqVO.getStartDate() == null) {
            return BigDecimal.ZERO;
        }
        ErpReceivableReportDetailReqVO copy = new ErpReceivableReportDetailReqVO();
        copy.setCustomerId(reqVO.getCustomerId());
        copy.setBizTime(new java.time.LocalDateTime[]{null, reqVO.getStartDate().atStartOfDay()});
        return selectOtherList(copy, scope, true).stream()
                .map(item -> amount(item.getReceivableAmount()).subtract(amount(item.getSettledAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ErpReceivableOtherDO> selectOtherList(ErpReceivableReportDetailReqVO reqVO,
                                                       ErpFinanceVisibleScope scope,
                                                       boolean beforeStartDate) {
        LambdaQueryWrapperX<ErpReceivableOtherDO> query = new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (beforeStartDate) {
            query.lt(ErpReceivableOtherDO::getBizTime, reqVO.getEndDate());
        } else {
            query.geIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getStartDate())
                    .leIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getEndDate());
        }
        applyScope(query, scope);
        return receivableOtherMapper.selectList(query.orderByAsc(ErpReceivableOtherDO::getBizTime)
                .orderByAsc(ErpReceivableOtherDO::getNo)
                .orderByAsc(ErpReceivableOtherDO::getId));
    }

    private ErpReceivableReportDetailRespVO buildRow(ErpReceivableOtherDO item) {
        BigDecimal receivableAmount = amount(item.getReceivableAmount());
        BigDecimal settledAmount = amount(item.getSettledAmount());
        ErpReceivableReportDetailRespVO row = new ErpReceivableReportDetailRespVO();
        row.setDocType("其他应收");
        row.setBizId(item.getId());
        row.setDocDate(item.getBizTime() == null ? null : item.getBizTime().atStartOfDay());
        row.setDocNo(item.getNo());
        row.setIncreaseAmount(receivableAmount);
        row.setReceiptAmount(settledAmount);
        row.setWriteOffAmount(BigDecimal.ZERO);
        row.setAllocatedAmount(settledAmount);
        row.setWriteOffBaseAmount(receivableAmount);
        return row;
    }

    private BigDecimal getChangeAmount(ErpReceivableReportDetailRespVO row) {
        return amount(row.getIncreaseAmount()).subtract(amount(row.getReceiptAmount()));
    }

    private boolean canSeeCustomer(Long customerId, ErpFinanceVisibleScope customerScope) {
        ErpReceivableReportPageReqVO reqVO = new ErpReceivableReportPageReqVO();
        reqVO.setCustomerId(customerId);
        reqVO.setShowZeroBalance(true);
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return !receivableReportMapper.selectList(reqVO, customerScope.getDeptIds(), selfUserIdText(customerScope),
                customerScope.isAll(), Collections.emptyList(), null, true).isEmpty();
    }

    private ErpFinanceVisibleScope getVisibleScope(String module) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        DeptDataPermissionRespDTO permission = loginUserId == null ? null
                : permissionApi.getDeptDataPermission(loginUserId, module);
        return ErpFinanceVisibleScope.from(permission, loginUserId);
    }

    private void applyScope(LambdaQueryWrapperX<ErpReceivableOtherDO> query, ErpFinanceVisibleScope scope) {
        if (scope.isAll()) {
            return;
        }
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(wrapper -> wrapper.in(ErpReceivableOtherDO::getDeptId, scope.getDeptIds())
                    .or().eq(ErpReceivableOtherDO::getHandlerId, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(ErpReceivableOtherDO::getDeptId, scope.getDeptIds());
        } else {
            query.eq(ErpReceivableOtherDO::getHandlerId, scope.getSelfUserId());
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
