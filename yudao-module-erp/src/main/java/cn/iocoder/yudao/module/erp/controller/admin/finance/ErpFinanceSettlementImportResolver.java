package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDateTime;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.trimToNull;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.zeroIfNull;

/**
 * Resolves user-facing settlement import fields into internal ids.
 */
@Component
public class ErpFinanceSettlementImportResolver {

    private static final String PAYMENT_FIELD_PERMISSION_MODULE = "erp_finance_payment";
    private static final String RECEIPT_FIELD_PERMISSION_MODULE = "erp_finance_receipt";

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;

    public PaymentImportContext buildPaymentContext() {
        return new PaymentImportContext(buildSupplierNameMap(), buildAccountNameMap(),
                buildDeptNameMap(PAYMENT_FIELD_PERMISSION_MODULE));
    }

    public ReceiptImportContext buildReceiptContext() {
        return new ReceiptImportContext(buildCustomerNameMap(), buildAccountNameMap(),
                buildDeptNameMap(RECEIPT_FIELD_PERMISSION_MODULE));
    }

    public ErpFinancePaymentSaveReqVO buildPaymentSaveReqVO(ErpFinancePaymentImportExcelVO row,
                                                            PaymentImportContext context) {
        ErpFinancePaymentSaveReqVO reqVO = new ErpFinancePaymentSaveReqVO();
        reqVO.setPaymentTime(parseDateTime(row.getPaymentTime(), LocalDateTime.now()));
        reqVO.setFinanceUserId(resolveFinanceUserId(row.getFinanceUserName(), row.getFinanceUserId()));
        reqVO.setDeptId(resolveOptionalName("所属部门", row.getDeptName(), row.getDeptId(), context.deptNameMap));
        reqVO.setSupplierId(resolveRequiredName("供应商", row.getSupplierName(), row.getSupplierId(),
                context.supplierNameMap));
        reqVO.setAccountId(resolveRequiredName("付款账户", row.getAccountName(), row.getAccountId(),
                context.accountNameMap));
        reqVO.setDiscountPrice(zeroIfNull(row.getDiscountPrice()));
        reqVO.setTotalPrice(row.getTotalPrice());
        reqVO.setPaymentPrice(row.getPaymentPrice());
        reqVO.setRemark(row.getRemark());

        if (!hasPaymentBizFields(row)) {
            reqVO.setItems(Collections.emptyList());
            return reqVO;
        }
        validatePaymentBizFields(row);
        Integer bizType = resolvePaymentBizType(row.getBizType());
        Long bizId = resolvePaymentBizId(bizType, row.getBizNo(), row.getBizId());
        ErpFinancePaymentSaveReqVO.Item item = new ErpFinancePaymentSaveReqVO.Item();
        item.setBizType(bizType);
        item.setBizId(bizId);
        item.setPaidPrice(zeroIfNull(row.getPaidPrice()));
        item.setPaymentPrice(row.getItemPaymentPrice());
        item.setRemark(row.getItemRemark());
        reqVO.setItems(Collections.singletonList(item));
        return reqVO;
    }

    public ErpFinanceReceiptSaveReqVO buildReceiptSaveReqVO(ErpFinanceReceiptImportExcelVO row,
                                                            ReceiptImportContext context) {
        ErpFinanceReceiptSaveReqVO reqVO = new ErpFinanceReceiptSaveReqVO();
        reqVO.setReceiptTime(parseDateTime(row.getReceiptTime(), LocalDateTime.now()));
        reqVO.setFinanceUserId(resolveFinanceUserId(row.getFinanceUserName(), row.getFinanceUserId()));
        reqVO.setDeptId(resolveOptionalName("所属部门", row.getDeptName(), row.getDeptId(), context.deptNameMap));
        reqVO.setCustomerId(resolveRequiredName("客户", row.getCustomerName(), row.getCustomerId(),
                context.customerNameMap));
        reqVO.setAccountId(resolveRequiredName("收款账户", row.getAccountName(), row.getAccountId(),
                context.accountNameMap));
        reqVO.setDiscountPrice(zeroIfNull(row.getDiscountPrice()));
        reqVO.setTotalPrice(row.getTotalPrice());
        reqVO.setReceiptPrice(row.getReceiptPrice());
        reqVO.setRemark(row.getRemark());

        if (!hasReceiptBizFields(row)) {
            reqVO.setItems(Collections.emptyList());
            return reqVO;
        }
        validateReceiptBizFields(row);
        Integer bizType = resolveReceiptBizType(row.getBizType());
        Long bizId = resolveReceiptBizId(bizType, row.getBizNo(), row.getBizId());
        ErpFinanceReceiptSaveReqVO.Item item = new ErpFinanceReceiptSaveReqVO.Item();
        item.setBizType(bizType);
        item.setBizId(bizId);
        item.setReceiptedPrice(zeroIfNull(row.getReceiptedPrice()));
        item.setReceiptPrice(row.getItemReceiptPrice());
        item.setRemark(row.getItemRemark());
        reqVO.setItems(Collections.singletonList(item));
        return reqVO;
    }

    private boolean hasPaymentBizFields(ErpFinancePaymentImportExcelVO row) {
        return trimToNull(row.getBizType()) != null || trimToNull(row.getBizNo()) != null
                || row.getBizId() != null || row.getPaidPrice() != null
                || row.getItemPaymentPrice() != null || trimToNull(row.getItemRemark()) != null;
    }

    private boolean hasReceiptBizFields(ErpFinanceReceiptImportExcelVO row) {
        return trimToNull(row.getBizType()) != null || trimToNull(row.getBizNo()) != null
                || row.getBizId() != null || row.getReceiptedPrice() != null
                || row.getItemReceiptPrice() != null || trimToNull(row.getItemRemark()) != null;
    }

    private void validatePaymentBizFields(ErpFinancePaymentImportExcelVO row) {
        if (trimToNull(row.getBizType()) == null || (trimToNull(row.getBizNo()) == null && row.getBizId() == null)
                || row.getItemPaymentPrice() == null) {
            throw new IllegalArgumentException("业务类型、业务单号/业务ID、本次付款需同时填写，或全部留空后续核销");
        }
    }

    private void validateReceiptBizFields(ErpFinanceReceiptImportExcelVO row) {
        if (trimToNull(row.getBizType()) == null || (trimToNull(row.getBizNo()) == null && row.getBizId() == null)
                || row.getItemReceiptPrice() == null) {
            throw new IllegalArgumentException("业务类型、业务单号/业务ID、本次收款需同时填写，或全部留空后续核销");
        }
    }

    private Map<String, List<Long>> buildSupplierNameMap() {
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        Map<String, List<Long>> map = new HashMap<>();
        supplierService.getSupplierPage(pageReqVO).getList()
                .forEach(supplier -> putName(map, supplier.getName(), supplier.getId()));
        return map;
    }

    private Map<String, List<Long>> buildCustomerNameMap() {
        ErpCustomerPageReqVO pageReqVO = new ErpCustomerPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        Map<String, List<Long>> map = new HashMap<>();
        customerService.getCustomerPage(pageReqVO).getList()
                .forEach(customer -> putName(map, customer.getName(), customer.getId()));
        return map;
    }

    private Map<String, List<Long>> buildAccountNameMap() {
        ErpAccountPageReqVO pageReqVO = new ErpAccountPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        Map<String, List<Long>> map = new HashMap<>();
        accountService.getAccountPage(pageReqVO).getList()
                .forEach(account -> putName(map, account.getName(), account.getId()));
        return map;
    }

    private Map<String, List<Long>> buildDeptNameMap(String module) {
        Map<String, List<Long>> map = new HashMap<>();
        dataPermissionDeptService.getDeptSimpleList(module)
                .forEach(dept -> putName(map, dept.getName(), dept.getId()));
        return map;
    }

    private Long resolveFinanceUserId(String name, Long fallbackId) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            return fallbackId;
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserListByNickname(normalizedName);
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("财务人员不存在：" + normalizedName);
        }
        if (users.size() > 1) {
            throw new IllegalArgumentException("财务人员名称重复：" + normalizedName);
        }
        return users.get(0).getId();
    }

    private Long resolveRequiredName(String label, String name, Long fallbackId, Map<String, List<Long>> nameMap) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            if (fallbackId == null) {
                throw new IllegalArgumentException(label + "不能为空");
            }
            return fallbackId;
        }
        return resolveUniqueId(label, normalizedName, nameMap);
    }

    private Long resolveOptionalName(String label, String name, Long fallbackId, Map<String, List<Long>> nameMap) {
        String normalizedName = trimToNull(name);
        return normalizedName == null ? fallbackId : resolveUniqueId(label, normalizedName, nameMap);
    }

    private Long resolveUniqueId(String label, String name, Map<String, List<Long>> nameMap) {
        List<Long> ids = nameMap.get(name);
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException(label + "不存在：" + name);
        }
        if (ids.size() > 1) {
            throw new IllegalArgumentException(label + "名称重复：" + name);
        }
        return ids.get(0);
    }

    private Integer resolvePaymentBizType(String value) {
        return resolveBizType(value, "付款单业务类型",
                ErpBizTypeEnum.PURCHASE_IN, ErpBizTypeEnum.PURCHASE_RETURN, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST);
    }

    private Integer resolveReceiptBizType(String value) {
        return resolveBizType(value, "收款单业务类型",
                ErpBizTypeEnum.SALE_OUT, ErpBizTypeEnum.SALE_RETURN, ErpBizTypeEnum.SALE_PRICE_ADJUST);
    }

    private Integer resolveBizType(String value, String label, ErpBizTypeEnum... allowedTypes) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        for (ErpBizTypeEnum allowedType : allowedTypes) {
            if (StrUtil.equals(normalizedValue, allowedType.getName())
                    || StrUtil.equals(normalizedValue, String.valueOf(allowedType.getType()))) {
                return allowedType.getType();
            }
        }
        throw new IllegalArgumentException(label + "不支持：" + normalizedValue);
    }

    private Long resolvePaymentBizId(Integer bizType, String bizNo, Long fallbackId) {
        String normalizedNo = trimToNull(bizNo);
        if (normalizedNo == null) {
            if (fallbackId == null) {
                throw new IllegalArgumentException("业务单号不能为空");
            }
            return fallbackId;
        }
        if (ErpBizTypeEnum.PURCHASE_IN.getType().equals(bizType)) {
            ErpPurchaseInDO row = purchaseInMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        if (ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(bizType)) {
            ErpPurchaseReturnDO row = purchaseReturnMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        if (ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType().equals(bizType)) {
            ErpPurchasePriceAdjustDO row = purchasePriceAdjustMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        throw new IllegalArgumentException("付款单业务类型不支持：" + bizType);
    }

    private Long resolveReceiptBizId(Integer bizType, String bizNo, Long fallbackId) {
        String normalizedNo = trimToNull(bizNo);
        if (normalizedNo == null) {
            if (fallbackId == null) {
                throw new IllegalArgumentException("业务单号不能为空");
            }
            return fallbackId;
        }
        if (ErpBizTypeEnum.SALE_OUT.getType().equals(bizType)) {
            ErpSaleOutDO row = saleOutMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        if (ErpBizTypeEnum.SALE_RETURN.getType().equals(bizType)) {
            ErpSaleReturnDO row = saleReturnMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        if (ErpBizTypeEnum.SALE_PRICE_ADJUST.getType().equals(bizType)) {
            ErpSalePriceAdjustDO row = salePriceAdjustMapper.selectByNo(normalizedNo);
            return getBizId(row == null ? null : row.getId(), normalizedNo);
        }
        throw new IllegalArgumentException("收款单业务类型不支持：" + bizType);
    }

    private Long getBizId(Long bizId, String bizNo) {
        if (bizId == null) {
            throw new IllegalArgumentException("业务单号不存在：" + bizNo);
        }
        return bizId;
    }

    private void putName(Map<String, List<Long>> map, String name, Long id) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null || id == null) {
            return;
        }
        map.computeIfAbsent(normalizedName, key -> new ArrayList<>()).add(id);
    }

    public static class PaymentImportContext {

        private final Map<String, List<Long>> supplierNameMap;
        private final Map<String, List<Long>> accountNameMap;
        private final Map<String, List<Long>> deptNameMap;

        private PaymentImportContext(Map<String, List<Long>> supplierNameMap,
                                     Map<String, List<Long>> accountNameMap,
                                     Map<String, List<Long>> deptNameMap) {
            this.supplierNameMap = supplierNameMap;
            this.accountNameMap = accountNameMap;
            this.deptNameMap = deptNameMap;
        }
    }

    public static class ReceiptImportContext {

        private final Map<String, List<Long>> customerNameMap;
        private final Map<String, List<Long>> accountNameMap;
        private final Map<String, List<Long>> deptNameMap;

        private ReceiptImportContext(Map<String, List<Long>> customerNameMap,
                                     Map<String, List<Long>> accountNameMap,
                                     Map<String, List<Long>> deptNameMap) {
            this.customerNameMap = customerNameMap;
            this.accountNameMap = accountNameMap;
            this.deptNameMap = deptNameMap;
        }
    }
}
