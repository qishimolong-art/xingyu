package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountTransactionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountTransactionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Service
@Validated
public class ErpFinanceBillService {

    public static final String BILL_TYPE_RECEIPT = "receipt";
    public static final String BILL_TYPE_PAYMENT = "payment";

    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    public PageResult<ErpFinanceBillRespVO> getFinanceBillPage(ErpFinanceBillPageReqVO reqVO) {
        List<ErpFinanceBillRespVO> rows = buildBillRows(reqVO);
        rows.sort(Comparator.comparing(ErpFinanceBillRespVO::getBillTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ErpFinanceBillRespVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    public PageResult<ErpAccountTransactionRespVO> getAccountTransactionPage(ErpAccountTransactionPageReqVO reqVO) {
        ErpFinanceBillPageReqVO billReqVO = new ErpFinanceBillPageReqVO();
        billReqVO.setPageNo(1);
        billReqVO.setPageSize(Integer.MAX_VALUE);
        billReqVO.setAccountId(reqVO.getAccountId());
        billReqVO.setBillType(reqVO.getBillType());
        billReqVO.setBillTime(reqVO.getTransactionTime());
        List<ErpAccountTransactionRespVO> rows = convertList(buildBillRows(billReqVO), bill -> {
            ErpAccountTransactionRespVO row = new ErpAccountTransactionRespVO();
            row.setNo(bill.getNo());
            row.setBillType(bill.getBillType());
            row.setBillTypeName(bill.getBillTypeName());
            row.setAmount(bill.getActualPrice());
            row.setTransactionTime(bill.getBillTime());
            row.setBizNo(getBizNo(bill));
            row.setRemark(bill.getRemark());
            return row;
        });
        rows.sort(Comparator.comparing(ErpAccountTransactionRespVO::getTransactionTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ErpAccountTransactionRespVO::getNo, Comparator.nullsLast(Comparator.reverseOrder())));
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    private List<ErpFinanceBillRespVO> buildBillRows(ErpFinanceBillPageReqVO reqVO) {
        List<ErpFinanceReceiptDO> receipts = BILL_TYPE_PAYMENT.equals(reqVO.getBillType())
                ? new ArrayList<>()
                : selectReceiptList(reqVO);
        List<ErpFinancePaymentDO> payments = BILL_TYPE_RECEIPT.equals(reqVO.getBillType())
                ? new ArrayList<>()
                : selectPaymentList(reqVO);

        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(receipts, ErpFinanceReceiptDO::getCustomerId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(payments, ErpFinancePaymentDO::getSupplierId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(Stream.concat(
                receipts.stream().map(ErpFinanceReceiptDO::getAccountId),
                payments.stream().map(ErpFinancePaymentDO::getAccountId))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(Stream.concat(
                receipts.stream().map(ErpFinanceReceiptDO::getDeptId),
                payments.stream().map(ErpFinancePaymentDO::getDeptId))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(Stream.concat(
                receipts.stream().flatMap(item -> Stream.of(NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()), item.getFinanceUserId())),
                payments.stream().flatMap(item -> Stream.of(NumberUtils.parseLong(item.getCreator()),
                        NumberUtils.parseLong(item.getUpdater()), item.getFinanceUserId())))
                .filter(Objects::nonNull).collect(Collectors.toList()));

        List<ErpFinanceBillRespVO> rows = new ArrayList<>(receipts.size() + payments.size());
        receipts.forEach(receipt -> rows.add(buildReceiptRow(receipt, customerMap, accountMap, deptMap, userMap)));
        payments.forEach(payment -> rows.add(buildPaymentRow(payment, supplierMap, accountMap, deptMap, userMap)));
        return rows;
    }

    private List<ErpFinanceReceiptDO> selectReceiptList(ErpFinanceBillPageReqVO reqVO) {
        Set<Long> idsByBizNo = selectReceiptIdsByBizNo(reqVO.getBizNo());
        if (reqVO.getBizNo() != null && CollUtil.isEmpty(idsByBizNo)) {
            return new ArrayList<>();
        }
        return financeReceiptMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .likeIfPresent(ErpFinanceReceiptDO::getNo, reqVO.getNo())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getEndTime())
                .eqIfPresent(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpFinanceReceiptDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinanceReceiptDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpFinanceReceiptDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpFinanceReceiptDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinanceReceiptDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpFinanceReceiptDO::getId, idsByBizNo));
    }

    private List<ErpFinancePaymentDO> selectPaymentList(ErpFinanceBillPageReqVO reqVO) {
        Set<Long> idsByBizNo = selectPaymentIdsByBizNo(reqVO.getBizNo());
        if (reqVO.getBizNo() != null && CollUtil.isEmpty(idsByBizNo)) {
            return new ArrayList<>();
        }
        return financePaymentMapper.selectList(new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .likeIfPresent(ErpFinancePaymentDO::getNo, reqVO.getNo())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime())
                .eqIfPresent(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpFinancePaymentDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinancePaymentDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpFinancePaymentDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpFinancePaymentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinancePaymentDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpFinancePaymentDO::getId, idsByBizNo));
    }

    private Set<Long> selectReceiptIdsByBizNo(String bizNo) {
        if (StrUtil.isBlank(bizNo)) {
            return null;
        }
        return convertSet(financeReceiptItemMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptItemDO>()
                .like(ErpFinanceReceiptItemDO::getBizNo, bizNo)), ErpFinanceReceiptItemDO::getReceiptId);
    }

    private Set<Long> selectPaymentIdsByBizNo(String bizNo) {
        if (StrUtil.isBlank(bizNo)) {
            return null;
        }
        return convertSet(financePaymentItemMapper.selectList(new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .like(ErpFinancePaymentItemDO::getBizNo, bizNo)), ErpFinancePaymentItemDO::getPaymentId);
    }

    private String getBizNo(ErpFinanceBillRespVO bill) {
        if (BILL_TYPE_RECEIPT.equals(bill.getBillType())) {
            return financeReceiptItemMapper.selectListByReceiptId(bill.getId()).stream()
                    .map(ErpFinanceReceiptItemDO::getBizNo)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
        }
        return financePaymentItemMapper.selectListByPaymentId(bill.getId()).stream()
                .map(ErpFinancePaymentItemDO::getBizNo)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private ErpFinanceBillRespVO buildReceiptRow(ErpFinanceReceiptDO receipt, Map<Long, ErpCustomerDO> customerMap,
                                                 Map<Long, ErpAccountDO> accountMap, Map<Long, DeptRespDTO> deptMap,
                                                 Map<Long, AdminUserRespDTO> userMap) {
        ErpFinanceBillRespVO row = new ErpFinanceBillRespVO();
        row.setId(receipt.getId());
        row.setNo(receipt.getNo());
        row.setBillType(BILL_TYPE_RECEIPT);
        row.setBillTypeName("收款");
        row.setStatus(receipt.getStatus());
        row.setBillTime(receipt.getReceiptTime());
        row.setFinanceUserId(receipt.getFinanceUserId());
        row.setDeptId(receipt.getDeptId());
        row.setCustomerId(receipt.getCustomerId());
        row.setAccountId(receipt.getAccountId());
        row.setTotalPrice(receipt.getTotalPrice());
        row.setDiscountPrice(receipt.getDiscountPrice());
        row.setActualPrice(receipt.getReceiptPrice());
        row.setRemark(receipt.getRemark());
        row.setCreator(receipt.getCreator());
        row.setCreateTime(receipt.getCreateTime());
        row.setUpdater(receipt.getUpdater());
        row.setUpdateTime(receipt.getUpdateTime());
        setNames(row, customerMap.get(receipt.getCustomerId()), null, accountMap.get(receipt.getAccountId()),
                deptMap.get(receipt.getDeptId()), userMap);
        return row;
    }

    private ErpFinanceBillRespVO buildPaymentRow(ErpFinancePaymentDO payment, Map<Long, ErpSupplierDO> supplierMap,
                                                 Map<Long, ErpAccountDO> accountMap, Map<Long, DeptRespDTO> deptMap,
                                                 Map<Long, AdminUserRespDTO> userMap) {
        ErpFinanceBillRespVO row = new ErpFinanceBillRespVO();
        row.setId(payment.getId());
        row.setNo(payment.getNo());
        row.setBillType(BILL_TYPE_PAYMENT);
        row.setBillTypeName("付款");
        row.setStatus(payment.getStatus());
        row.setBillTime(payment.getPaymentTime());
        row.setFinanceUserId(payment.getFinanceUserId());
        row.setDeptId(payment.getDeptId());
        row.setSupplierId(payment.getSupplierId());
        row.setAccountId(payment.getAccountId());
        row.setTotalPrice(payment.getTotalPrice());
        row.setDiscountPrice(payment.getDiscountPrice());
        row.setActualPrice(payment.getPaymentPrice());
        row.setRemark(payment.getRemark());
        row.setCreator(payment.getCreator());
        row.setCreateTime(payment.getCreateTime());
        row.setUpdater(payment.getUpdater());
        row.setUpdateTime(payment.getUpdateTime());
        setNames(row, null, supplierMap.get(payment.getSupplierId()), accountMap.get(payment.getAccountId()),
                deptMap.get(payment.getDeptId()), userMap);
        return row;
    }

    private void setNames(ErpFinanceBillRespVO row, ErpCustomerDO customer, ErpSupplierDO supplier,
                          ErpAccountDO account, DeptRespDTO dept, Map<Long, AdminUserRespDTO> userMap) {
        if (customer != null) {
            row.setCustomerName(customer.getName());
        }
        if (supplier != null) {
            row.setSupplierName(supplier.getName());
        }
        if (account != null) {
            row.setAccountName(account.getName());
        }
        if (dept != null) {
            row.setDeptName(dept.getName());
        }
        AdminUserRespDTO financeUser = userMap.get(row.getFinanceUserId());
        if (financeUser != null) {
            row.setFinanceUserName(financeUser.getNickname());
        }
        AdminUserRespDTO creator = userMap.get(NumberUtils.parseLong(row.getCreator()));
        if (creator != null) {
            row.setCreatorName(creator.getNickname());
        }
        AdminUserRespDTO updater = userMap.get(NumberUtils.parseLong(row.getUpdater()));
        if (updater != null) {
            row.setUpdaterName(updater.getNickname());
        }
    }

    private <T> PageResult<T> page(List<T> rows, int pageNo, int pageSize) {
        if (CollUtil.isEmpty(rows)) {
            return PageResult.empty(0L);
        }
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        if (fromIndex >= rows.size()) {
            return PageResult.empty((long) rows.size());
        }
        int toIndex = Math.min(rows.size(), fromIndex + pageSize);
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

}
