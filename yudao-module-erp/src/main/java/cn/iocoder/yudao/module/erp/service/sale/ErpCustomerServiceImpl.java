package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DISABLE_FAIL_RECEIVABLE_NOT_CLEAR;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_CUSTOMER_TYPE;

/**
 * ERP 客户 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCustomerServiceImpl implements ErpCustomerService {

    private static final String FIELD_PERMISSION_MODULE = "erp_customer";

    @Resource
    private ErpCustomerMapper customerMapper;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpReceivableWriteOffMapper receivableWriteOffMapper;
    @Resource
    private ErpReceivableAccountMapper receivableAccountMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    public Long createCustomer(ErpCustomerSaveReqVO createReqVO) {
        clearHiddenFields(createReqVO);
        // 插入
        ErpCustomerDO customer = BeanUtils.toBean(createReqVO, ErpCustomerDO.class);
        // 自动生成编码
        customer.setCode(normalizeCode(customer.getCode()));
        if (!StringUtils.hasText(customer.getCode())) {
            customer.setCode(noRedisDAO.generate(ErpNoRedisDAO.CUSTOMER_NO_PREFIX));
        }
        validateCustomerCodeUnique(null, customer.getCode());
        if (!StringUtils.hasText(customer.getMemberCode())) {
            customer.setMemberCode(noRedisDAO.generate(ErpNoRedisDAO.MEMBER_NO_PREFIX));
        }
        if (!StringUtils.hasText(customer.getPlatformCode())) {
            customer.setPlatformCode(noRedisDAO.generate(ErpNoRedisDAO.PLATFORM_NO_PREFIX));
        }
        // sort 默认值
        if (customer.getSort() == null) {
            customer.setSort(0);
        }
        saleDocumentDefaultService.fillCreateDefaults(customer);
        customerMapper.insert(customer);
        recordCreate(customer.getId(), customer.getCode());
        // 返回
        return customer.getId();
    }

    @Override
    public void updateCustomer(ErpCustomerSaveReqVO updateReqVO) {
        // 校验存在
        ErpCustomerDO existing = validateCustomerExists(updateReqVO.getId());
        preserveHiddenFields(updateReqVO, existing);
        // 更新
        ErpCustomerDO updateObj = BeanUtils.toBean(updateReqVO, ErpCustomerDO.class);
        updateObj.setCode(existing.getCode());
        customerMapper.updateById(updateObj);
        recordUpdate(updateReqVO.getId(), existing.getCode());
    }

    @Override
    public void deleteCustomer(Long id) {
        // 校验存在
        ErpCustomerDO customer = validateCustomerExists(id);
        baseArchiveReferenceService.validateCustomerNotReferenced(id);
        // 删除
        customerMapper.deleteById(id);
        recordDelete(id, customer.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerList(List<Long> ids) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteCustomer(id);
        }
    }

    private void validateCustomerNotReferenced(Long customerId) {
        if (saleQuoteMapper != null && saleQuoteMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "报价订单");
        }
        if (saleCartMapper != null && saleCartMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "销售手推车");
        }
        if (saleOrderMapper != null && saleOrderMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "销售订单");
        }
        if (saleOutMapper != null && saleOutMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "销售出库单");
        }
        if (saleReturnMapper != null && saleReturnMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "销售退货单");
        }
        if (salePriceAdjustMapper != null && salePriceAdjustMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "销售调价单");
        }
        if (financeReceiptMapper != null && financeReceiptMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "收款单");
        }
        if (receivableOtherMapper != null && receivableOtherMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "其他应收");
        }
        if (receivableWriteOffMapper != null && receivableWriteOffMapper.selectCountByCustomerId(customerId) > 0) {
            throw exception(CUSTOMER_DELETE_FAIL_REFERENCED, "应收核销");
        }
    }

    private ErpCustomerDO validateCustomerExists(Long id) {
        ErpCustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    @Override
    public ErpCustomerDO getCustomer(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public ErpCustomerDO validateCustomer(Long id) {
        ErpCustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(customer.getStatus())) {
            throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
        }
        return customer;
    }

    @Override
    public List<ErpCustomerDO> getCustomerList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return customerMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpCustomerDO> getCustomerPage(ErpCustomerPageReqVO pageReqVO) {
        return customerMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByStatus(Integer status) {
        return customerMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByNameLike(String name) {
        if (name == null || name.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return customerMapper.selectListByNameLike(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importCustomerList(List<ErpCustomerImportExcelVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ErpCustomerImportExcelVO importVO : list) {
            if (importVO == null || !StringUtils.hasText(importVO.getName())) {
                continue;
            }
            ErpCustomerDO customer = BeanUtils.toBean(importVO, ErpCustomerDO.class);
            if (customer.getStatus() == null) {
                customer.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (customer.getSort() == null) {
                customer.setSort(0);
            }
            clearHiddenFields(customer);
            saleDocumentDefaultService.fillCreateDefaults(customer);
            customerMapper.insert(customer);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateCustomer(ErpCustomerBatchUpdateReqVO reqVO) {
        if (CommonStatusEnum.isDisable(reqVO.getStatus())) {
            validateCustomerReceivableClear(reqVO.getIds());
        }
        LambdaUpdateWrapper<ErpCustomerDO> wrapper = new LambdaUpdateWrapper<ErpCustomerDO>()
                .in(ErpCustomerDO::getId, reqVO.getIds());
        boolean hasUpdate = false;
        if (reqVO.getSaleUserId() != null && !isFieldHidden("saleUserId")) {
            wrapper.set(ErpCustomerDO::getSaleUserId, reqVO.getSaleUserId());
            hasUpdate = true;
        }
        if (reqVO.getDeveloperUserId() != null && !isFieldHidden("developerUserId")) {
            wrapper.set(ErpCustomerDO::getDeveloperUserId, reqVO.getDeveloperUserId());
            hasUpdate = true;
        }
        if (reqVO.getDeptId() != null && !isFieldHidden("deptId")) {
            wrapper.set(ErpCustomerDO::getDeptId, reqVO.getDeptId());
            hasUpdate = true;
        }
        if (reqVO.getStatus() != null && !isFieldHidden("status")) {
            wrapper.set(ErpCustomerDO::getStatus, reqVO.getStatus());
            if (CommonStatusEnum.isDisable(reqVO.getStatus())) {
                wrapper.set(ErpCustomerDO::getDisabledBy, getLoginUserId());
                wrapper.set(ErpCustomerDO::getDisabledTime, LocalDateTime.now());
            }
            hasUpdate = true;
        }
        if (reqVO.getPriceLevel() != null && !isFieldHidden("priceLevel")) {
            wrapper.set(ErpCustomerDO::getPriceLevel, reqVO.getPriceLevel());
            hasUpdate = true;
        }
        if (reqVO.getRouteId() != null && !isFieldHidden("routeId")) {
            wrapper.set(ErpCustomerDO::getRouteId, reqVO.getRouteId());
            hasUpdate = true;
        }
        if (reqVO.getFreightExplainId() != null && !isFieldHidden("freightExplainId")) {
            wrapper.set(ErpCustomerDO::getFreightExplainId, reqVO.getFreightExplainId());
            hasUpdate = true;
        }
        if (reqVO.getRemark() != null && !isFieldHidden("remark")) {
            wrapper.set(ErpCustomerDO::getRemark, reqVO.getRemark());
            hasUpdate = true;
        }
        if (!hasUpdate) {
            return;
        }
        customerMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableCustomer(List<Long> ids) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        validateCustomerReceivableClear(distinctIds);
        for (Long id : distinctIds) {
            ErpCustomerDO customer = validateCustomerExists(id);
            ErpCustomerDO updateObj = new ErpCustomerDO();
            updateObj.setId(id);
            updateObj.setStatus(CommonStatusEnum.DISABLE.getStatus());
            updateObj.setDisabledBy(getLoginUserId());
            updateObj.setDisabledTime(LocalDateTime.now());
            customerMapper.updateById(updateObj);
            recordUpdate(id, customer.getCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreCustomer(List<Long> ids) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long id : distinctIds) {
            ErpCustomerDO customer = validateCustomerExists(id);
            if (!CommonStatusEnum.isDisable(customer.getStatus())) {
                throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
            }
            customerMapper.update(null, new LambdaUpdateWrapper<ErpCustomerDO>()
                    .eq(ErpCustomerDO::getId, id)
                    .set(ErpCustomerDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                    .set(ErpCustomerDO::getDisabledBy, null)
                    .set(ErpCustomerDO::getDisabledTime, null));
            recordUpdate(id, customer.getCode());
        }
    }

    private void validateCustomerReceivableClear(Collection<Long> ids) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            validateCustomerReceivableClear(validateCustomerExists(id));
        }
    }

    private void validateCustomerReceivableClear(ErpCustomerDO customer) {
        ErpReceivableAccountDO account = receivableAccountMapper.selectByCustomerId(customer.getId());
        BigDecimal balance = account == null || account.getReceivableBalance() == null
                ? BigDecimal.ZERO : account.getReceivableBalance();
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw exception(CUSTOMER_DISABLE_FAIL_RECEIVABLE_NOT_CLEAR, customer.getName(), balance);
        }
    }

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private boolean isFieldHidden(String fieldName) {
        return fieldPermissionMasker != null && fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, fieldName);
    }

    private void validateCustomerCodeUnique(Long id, String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        ErpCustomerDO customer = customerMapper.selectByCodeExcludeId(code, id);
        if (customer != null) {
            throw exception(CUSTOMER_CODE_DUPLICATE, code);
        }
    }

    private String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_CUSTOMER_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_CUSTOMER_TYPE, id, no);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_CUSTOMER_TYPE, id, no);
        }
    }

}
