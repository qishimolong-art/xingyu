package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptCreditSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDeptCreditDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDeptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerDeptCreditMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.base.ErpArchiveMergeService;
import cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerCreditStatusBO;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_BLOCKED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_VALUE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DEPT_CREDIT_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DEPT_CREDIT_DUPLICATE_DEPT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_DISABLE_FAIL_RECEIVABLE_NOT_CLEAR;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ARCHIVE_MERGE_SAME_ID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MERGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_SALE_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_IMPORT_SUB_TYPE;
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
    private static final String CUSTOMER_DEPT_DISTRIBUTE_PERMISSION = "erp:customer:dept-distribute";
    private static final String IMPORT_DEPT_NAME_SPLIT_REGEX = "[,，、;；]";

    @Resource
    private ErpCustomerMapper customerMapper;
    @Resource
    private ErpCustomerDeptMapper customerDeptMapper;
    @Resource
    private ErpCustomerDeptCreditMapper customerDeptCreditMapper;
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
    private ErpArchiveMergeService archiveMergeService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(ErpCustomerSaveReqVO createReqVO) {
        Collection<Long> customerDeptIds = createReqVO.getDeptIds();
        clearHiddenFields(createReqVO);
        if (isFieldHidden("allowMultiDept")) {
            createReqVO.setAllowMultiDept(false);
        }
        if (isFieldHidden("deptIds")) {
            customerDeptIds = Collections.emptyList();
        }
        validateCreditConfig(createReqVO);
        // 插入
        ErpCustomerDO customer = BeanUtils.toBean(createReqVO, ErpCustomerDO.class);
        customer.setName(trimToNull(customer.getName()));
        validateCustomerNameUnique(null, customer.getName());
        normalizeCreditConfig(customer);
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
        if (customer.getDeptId() == null) {
            customer.setDeptId(getLoginUserDeptId());
        }
        if (customer.getAllowMultiDept() == null) {
            customer.setAllowMultiDept(false);
        }
        customerMapper.insert(customer);
        syncCustomerDeptList(customer.getId(), buildCustomerDeptIds(customer.getDeptId(), customerDeptIds,
                customer.getAllowMultiDept()));
        recordCreate(customer);
        // 返回
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(ErpCustomerSaveReqVO updateReqVO) {
        // 校验存在
        ErpCustomerDO existing = validateCustomerExists(updateReqVO.getId());
        Collection<Long> customerDeptIds = updateReqVO.getDeptIds();
        boolean allowMultiDeptHidden = isFieldHidden("allowMultiDept");
        boolean deptIdHidden = isFieldHidden("deptId");
        boolean deptIdsHidden = isFieldHidden("deptIds");
        preserveHiddenFields(updateReqVO, existing);
        validateCreditConfig(updateReqVO);
        // 更新
        ErpCustomerDO updateObj = BeanUtils.toBean(updateReqVO, ErpCustomerDO.class);
        updateObj.setName(trimToNull(updateObj.getName()));
        validateCustomerNameUnique(existing.getId(), updateObj.getName());
        normalizeCreditConfig(updateObj);
        updateObj.setCode(existing.getCode());
        if (allowMultiDeptHidden) {
            updateObj.setAllowMultiDept(null);
        }
        if (deptIdHidden) {
            updateObj.setDeptId(null);
        } else {
            updateObj.setDeptId(resolvePrimaryDeptId(updateObj.getDeptId(), customerDeptIds, existing.getDeptId()));
        }
        customerMapper.updateById(updateObj);
        Boolean effectiveAllowMultiDept = updateObj.getAllowMultiDept() != null
                ? updateObj.getAllowMultiDept() : existing.getAllowMultiDept();
        Long effectiveDeptId = deptIdHidden ? existing.getDeptId() : updateObj.getDeptId();
        boolean allowMultiDeptChanged = !allowMultiDeptHidden && updateReqVO.getAllowMultiDept() != null;
        if (allowMultiDeptChanged && Boolean.FALSE.equals(effectiveAllowMultiDept)) {
            customerDeptMapper.deleteByCustomerId(updateReqVO.getId());
        } else if (!deptIdsHidden && (customerDeptIds != null || allowMultiDeptChanged)) {
            syncCustomerDeptList(updateReqVO.getId(), buildCustomerDeptIds(effectiveDeptId, customerDeptIds,
                    effectiveAllowMultiDept));
        }
        recordUpdate(existing, customerMapper.selectById(updateReqVO.getId()));
    }

    @Override
    public ErpCustomerDeptDistributionRespVO getCustomerDeptDistribution(Long id) {
        // 查看分配部门信息时，使用无可见范围限制的查询——调用此接口的用户已通过权限校验，
        // 不应再受数据权限过滤的影响（例如部门可见范围会导致跨部门分配信息丢失）
        ErpCustomerDO customer = DataPermissionUtils.executeIgnore(() -> customerMapper.selectById(id));
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        ErpCustomerDeptDistributionRespVO respVO =
                BeanUtils.toBean(customer, ErpCustomerDeptDistributionRespVO.class);
        List<Long> deptIds = getCustomerDeptMap(Collections.singleton(id)).getOrDefault(id, Collections.emptyList());
        Set<Long> allDeptIds = new LinkedHashSet<>();
        if (customer.getDeptId() != null) {
            allDeptIds.add(customer.getDeptId());
        }
        allDeptIds.addAll(deptIds);
        Map<Long, String> deptNameMap = buildDeptNameMap(allDeptIds);
        respVO.setDeptName(deptNameMap.get(customer.getDeptId()));
        respVO.setDeptIds(deptIds);
        respVO.setDeptNames(buildDeptNames(deptIds, deptNameMap));
        respVO.setDeptNameMap(deptNameMap);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerDeptDistribution(ErpCustomerDeptDistributionSaveReqVO reqVO) {
        ErpCustomerDO existing = validateCustomerExists(reqVO.getId());
        if (CollUtil.isNotEmpty(reqVO.getDeptIds())) {
            deptApi.validateDeptList(reqVO.getDeptIds());
        }
        if (reqVO.getDeptId() != null) {
            deptApi.validateDeptList(Collections.singleton(reqVO.getDeptId()));
        }
        ErpCustomerDO updateObj = new ErpCustomerDO();
        updateObj.setId(reqVO.getId());
        updateObj.setDeptId(resolvePrimaryDeptId(reqVO.getDeptId(), reqVO.getDeptIds(), existing.getDeptId()));
        updateObj.setAllowMultiDept(Boolean.TRUE.equals(reqVO.getAllowMultiDept()));
        customerMapper.updateById(updateObj);
        if (Boolean.TRUE.equals(updateObj.getAllowMultiDept())) {
            syncCustomerDeptList(reqVO.getId(), buildCustomerDeptIds(updateObj.getDeptId(), reqVO.getDeptIds(), true));
        } else {
            customerDeptMapper.deleteByCustomerId(reqVO.getId());
        }
        recordUpdate(existing, DataPermissionUtils.executeIgnore(() -> customerMapper.selectById(reqVO.getId())));
    }

    @Override
    public ErpCustomerDeptCreditRespVO getCustomerDeptCredit(Long id) {
        ErpCustomerDO customer = DataPermissionUtils.executeIgnore(() -> customerMapper.selectById(id));
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        List<Long> saleDeptIds = getCustomerSaleDeptIds(customer);
        Map<Long, String> deptNameMap = buildDeptNameMap(saleDeptIds);
        List<ErpCustomerDeptCreditDO> credits = customerDeptCreditMapper.selectListByCustomerId(id);
        Map<Long, ErpCustomerDeptCreditDO> creditMap = (credits == null ? Collections.<ErpCustomerDeptCreditDO>emptyList() : credits).stream()
                .filter(credit -> credit.getDeptId() != null)
                .collect(Collectors.toMap(ErpCustomerDeptCreditDO::getDeptId, credit -> credit,
                        (first, second) -> first, LinkedHashMap::new));
        List<ErpCustomerDeptCreditRespVO.Item> items = saleDeptIds.stream()
                .map(deptId -> buildDeptCreditItem(deptId, deptNameMap.get(deptId), creditMap.get(deptId)))
                .collect(Collectors.toList());

        ErpCustomerDeptCreditRespVO respVO = BeanUtils.toBean(customer, ErpCustomerDeptCreditRespVO.class);
        respVO.setDeptIds(saleDeptIds);
        respVO.setDeptNameMap(deptNameMap);
        respVO.setItems(items);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerDeptCredit(ErpCustomerDeptCreditSaveReqVO reqVO) {
        ErpCustomerDO customer = validateCustomerExists(reqVO.getId());
        List<ErpCustomerDeptCreditSaveReqVO.Item> items = reqVO.getItems() == null
                ? Collections.emptyList() : reqVO.getItems();
        validateDeptCreditItems(customer, items);

        customerDeptCreditMapper.deleteByCustomerId(reqVO.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<ErpCustomerDeptCreditDO> creditList = items.stream()
                .filter(item -> item != null && item.getDeptId() != null)
                .map(item -> buildDeptCreditDO(reqVO.getId(), item))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(creditList)) {
            return;
        }
        customerDeptCreditMapper.insertBatch(creditList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        // 校验存在
        ErpCustomerDO customer = validateCustomerExists(id);
        baseArchiveReferenceService.validateCustomerNotReferenced(id);
        // 删除
        customerMapper.deleteById(id);
        customerDeptMapper.deleteByCustomerId(id);
        customerDeptCreditMapper.deleteByCustomerId(id);
        recordDelete(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeCustomer(Long sourceId, Long keepId) {
        if (Objects.equals(sourceId, keepId)) {
            throw exception(ARCHIVE_MERGE_SAME_ID);
        }
        ErpCustomerDO source = validateCustomerExists(sourceId);
        ErpCustomerDO keep = validateCustomerExists(keepId);
        if (Boolean.TRUE.equals(source.getMergedFlag())) {
            throw exception(CUSTOMER_MERGED, source.getName());
        }
        if (Boolean.TRUE.equals(keep.getMergedFlag())) {
            throw exception(CUSTOMER_MERGED, keep.getName());
        }
        String operatorId = String.valueOf(getLoginUserId());
        archiveMergeService.mergeCustomerReferences(sourceId, keepId, operatorId);
        customerMapper.update(null, new LambdaUpdateWrapper<ErpCustomerDO>()
                .eq(ErpCustomerDO::getId, sourceId)
                .ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE)
                .set(ErpCustomerDO::getMergedFlag, true)
                .set(ErpCustomerDO::getMergedTargetId, keepId)
                .set(ErpCustomerDO::getMergedBy, getLoginUserId())
                .set(ErpCustomerDO::getMergedTime, LocalDateTime.now()));
        recordUpdate(source, customerMapper.selectById(sourceId));
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
        ErpCustomerDO customer = DataPermissionUtils.executeIgnore(() -> customerMapper.selectById(id));
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    @Override
    public ErpCustomerDO getCustomer(Long id) {
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectById(id);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisibleById(id, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public ErpCustomerDO validateCustomer(Long id) {
        ErpCustomerDO customer = getCustomer(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(customer.getStatus())) {
            throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
        }
        return customer;
    }

    @Override
    public ErpCustomerDO validateCustomerForSale(Long id) {
        ErpCustomerDO customer = validateCustomer(id);
        validateCustomerCredit(customer);
        return customer;
    }

    @Override
    public ErpCustomerDO validateCustomerForSale(Long id, Long saleDeptId) {
        ErpCustomerDO customer = validateCustomer(id);
        validateCustomerCredit(customer, saleDeptId);
        return customer;
    }

    @Override
    public ErpCustomerDO validateCustomerForGeneratedSale(Long id, Long saleDeptId) {
        ErpCustomerDO customer = validateCustomerExists(id);
        if (CommonStatusEnum.isDisable(customer.getStatus())) {
            throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
        }
        if (saleDeptId != null && !getCustomerSaleDeptIds(customer).contains(saleDeptId)) {
            throw exception(CUSTOMER_SALE_DEPT_NOT_ALLOWED);
        }
        validateCustomerCredit(customer, saleDeptId);
        return customer;
    }

    private void validateCustomerCredit(ErpCustomerDO customer) {
        ErpCustomerCreditStatusBO creditStatus = buildCustomerCreditStatus(customer);
        if (Boolean.TRUE.equals(creditStatus.getBlocked())) {
            throw exception(CUSTOMER_CREDIT_BLOCKED, customer.getName(), creditStatus.getBlockedReason());
        }
    }

    private void validateCustomerCredit(ErpCustomerDO customer, Long saleDeptId) {
        if (saleDeptId == null) {
            validateCustomerCredit(customer);
            return;
        }
        ErpCustomerDeptCreditDO deptCredit =
                customerDeptCreditMapper.selectByCustomerIdAndDeptId(customer.getId(), saleDeptId);
        if (deptCredit == null) {
            validateCustomerCredit(customer);
            return;
        }
        if (!Boolean.TRUE.equals(deptCredit.getCreditEnabled())) {
            return;
        }
        ErpCustomerCreditStatusBO creditStatus = buildCustomerCreditStatus(customer, deptCredit, saleDeptId);
        if (Boolean.TRUE.equals(creditStatus.getBlocked())) {
            throw exception(CUSTOMER_CREDIT_BLOCKED, customer.getName(), creditStatus.getBlockedReason());
        }
    }

    @Override
    public List<Long> getCustomerSaleDeptIds(Long customerId) {
        ErpCustomerDO customer = validateCustomer(customerId);
        return getCustomerSaleDeptIds(customer);
    }

    @Override
    public List<Long> getCustomerSaleDeptIdsIgnoreDataPermission(Long customerId) {
        ErpCustomerDO customer = validateCustomerExists(customerId);
        if (CommonStatusEnum.isDisable(customer.getStatus())) {
            throw exception(CUSTOMER_NOT_ENABLE, customer.getName());
        }
        return getCustomerSaleDeptIds(customer);
    }

    private List<Long> getCustomerSaleDeptIds(ErpCustomerDO customer) {
        Set<Long> deptIds = new LinkedHashSet<>();
        if (customer.getDeptId() != null) {
            deptIds.add(customer.getDeptId());
        }
        if (Boolean.TRUE.equals(customer.getAllowMultiDept())) {
            List<ErpCustomerDeptDO> customerDepts = DataPermissionUtils.executeIgnore(
                    () -> customerDeptMapper.selectListByCustomerId(customer.getId()));
            deptIds.addAll(customerDepts.stream().map(ErpCustomerDeptDO::getDeptId).collect(Collectors.toList()));
        }
        return deptIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void validateCustomerSaleDept(Long customerId, Long deptId) {
        if (deptId == null) {
            return;
        }
        if (!getCustomerSaleDeptIds(customerId).contains(deptId)) {
            throw exception(CUSTOMER_SALE_DEPT_NOT_ALLOWED);
        }
    }

    @Override
    public ErpCustomerCreditStatusBO getCustomerCreditStatus(Long customerId) {
        if (customerId == null) {
            return null;
        }
        ErpCustomerDO customer = customerMapper.selectById(customerId);
        return customer == null ? null : buildCustomerCreditStatus(customer);
    }

    @Override
    public Map<Long, ErpCustomerCreditStatusBO> getCustomerCreditStatusMap(Collection<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ErpCustomerDO> customers = customerMapper.selectByIds(customerIds);
        Map<Long, ErpCustomerCreditStatusBO> result = new LinkedHashMap<>();
        for (ErpCustomerDO customer : customers) {
            result.put(customer.getId(), buildCustomerCreditStatus(customer));
        }
        return result;
    }

    @Override
    public List<ErpCustomerDO> getCustomerList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectByIds(ids);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisibleListByIds(ids, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public PageResult<ErpCustomerDO> getCustomerPage(ErpCustomerPageReqVO pageReqVO) {
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectPage(pageReqVO);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisiblePage(pageReqVO, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public PageResult<ErpCustomerDO> getCustomerPageByStatus(ErpCustomerPageReqVO pageReqVO, Integer status) {
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectPageByStatus(pageReqVO, status);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisiblePageByStatus(pageReqVO, status, scope.getDeptIds(),
                        scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByStatus(Integer status) {
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectListByStatus(status);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisibleListByStatus(status, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public List<ErpCustomerDO> getCustomerListByNameLike(String name) {
        if (name == null || name.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        CustomerVisibleScope scope = getCustomerVisibleScope();
        if (scope == null) {
            return customerMapper.selectListByNameLike(name);
        }
        return DataPermissionUtils.executeIgnore(() ->
                customerMapper.selectVisibleListByNameLike(name, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpCustomerImportRespVO importCustomerList(List<ErpCustomerImportExcelVO> list) {
        ErpCustomerImportRespVO respVO = new ErpCustomerImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpCustomerDO> existedMap = buildExistedCustomerMap(list);
        ImportDeptContext deptContext = buildImportDeptContext(list);
        for (int i = 0; i < list.size(); i++) {
            ErpCustomerImportExcelVO importVO = list.get(i);
            if (importVO == null || !StringUtils.hasText(trimToNull(importVO.getName()))) {
                continue;
            }
            Integer rowNo = i + 2;
            String code = trimToNull(importVO.getCode());
            try {
                ImportDeptAssignment deptAssignment = resolveImportDeptAssignment(importVO, deptContext);
                ErpCustomerDO existed = StringUtils.hasText(code) ? existedMap.get(code) : null;
                if (existed != null) {
                    updateCustomerFromImport(importVO, existed, deptAssignment);
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                } else {
                    Long customerId = createCustomerFromImport(importVO, deptAssignment);
                    if (StringUtils.hasText(code)) {
                        existedMap.put(code, new ErpCustomerDO().setId(customerId).setCode(code));
                    }
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                }
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpCustomerImportRespVO.FailureItem(
                        rowNo, code, getImportFailureReason(ex)));
            }
        }
        respVO.setSuccessCount(respVO.getCreateCount() + respVO.getUpdateCount());
        respVO.setFailureCount(respVO.getFailureDetails().size());
        if (operateLogService != null) {
            operateLogService.record(ERP_CUSTOMER_TYPE, ERP_IMPORT_SUB_TYPE, 0L,
                    "导入客户信息，新增：" + respVO.getCreateCount()
                            + "，更新：" + respVO.getUpdateCount()
                            + "，失败：" + respVO.getFailureCount(),
                    "客户导入");
        }
        return respVO;
    }

    private Map<String, ErpCustomerDO> buildExistedCustomerMap(List<ErpCustomerImportExcelVO> list) {
        List<String> codes = list.stream()
                .filter(Objects::nonNull)
                .map(ErpCustomerImportExcelVO::getCode)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(codes)) {
            return new HashMap<>();
        }
        List<ErpCustomerDO> customers = customerMapper.selectListByCodes(codes);
        if (CollUtil.isEmpty(customers)) {
            return new HashMap<>();
        }
        return customers.stream()
                .filter(item -> StringUtils.hasText(item.getCode()))
                .collect(Collectors.toMap(ErpCustomerDO::getCode, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Long createCustomerFromImport(ErpCustomerImportExcelVO importVO, ImportDeptAssignment deptAssignment) {
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        reqVO.setCode(trimToNull(importVO.getCode()));
        reqVO.setName(trimToNull(importVO.getName()));
        reqVO.setContact(trimToNull(importVO.getContact()));
        reqVO.setMobile(trimToNull(importVO.getMobile()));
        reqVO.setTelephone(trimToNull(importVO.getTelephone()));
        reqVO.setAreaId(importVO.getAreaId());
        reqVO.setDetailAddress(trimToNull(importVO.getDetailAddress()));
        reqVO.setTaxNo(trimToNull(importVO.getTaxNo()));
        reqVO.setBankName(trimToNull(importVO.getBankName()));
        reqVO.setBankAccount(trimToNull(importVO.getBankAccount()));
        reqVO.setRemark(trimToNull(importVO.getRemark()));
        reqVO.setSort(importVO.getSort());
        reqVO.setStatus(importVO.getStatus() == null ? CommonStatusEnum.ENABLE.getStatus() : importVO.getStatus());
        fillCustomerImportMnemonicCodes(reqVO);
        if (deptAssignment.hasDeptImport()) {
            reqVO.setDeptId(deptAssignment.getDeptId());
            reqVO.setDeptIds(deptAssignment.getDeptIds());
            reqVO.setAllowMultiDept(CollUtil.isNotEmpty(deptAssignment.getDeptIds()));
        }
        return createCustomer(reqVO);
    }

    private void updateCustomerFromImport(ErpCustomerImportExcelVO importVO, ErpCustomerDO existing,
                                          ImportDeptAssignment deptAssignment) {
        ErpCustomerDO updateObj = new ErpCustomerDO();
        updateObj.setId(existing.getId());
        updateObj.setCode(existing.getCode());
        updateObj.setName(trimToNull(importVO.getName()));
        validateCustomerNameUnique(existing.getId(), updateObj.getName());
        setIfHasText(updateObj::setContact, importVO.getContact());
        setIfHasText(updateObj::setMobile, importVO.getMobile());
        setIfHasText(updateObj::setTelephone, importVO.getTelephone());
        if (importVO.getAreaId() != null) {
            updateObj.setAreaId(importVO.getAreaId());
        }
        setIfHasText(updateObj::setDetailAddress, importVO.getDetailAddress());
        setIfHasText(updateObj::setTaxNo, importVO.getTaxNo());
        setIfHasText(updateObj::setBankName, importVO.getBankName());
        setIfHasText(updateObj::setBankAccount, importVO.getBankAccount());
        setIfHasText(updateObj::setRemark, importVO.getRemark());
        if (importVO.getSort() != null) {
            updateObj.setSort(importVO.getSort());
        }
        if (importVO.getStatus() != null) {
            updateObj.setStatus(importVO.getStatus());
        }
        if (deptAssignment.hasDeptImport()) {
            updateObj.setDeptId(resolvePrimaryDeptId(deptAssignment.getDeptId(), deptAssignment.getDeptIds(),
                    existing.getDeptId()));
            updateObj.setAllowMultiDept(CollUtil.isNotEmpty(deptAssignment.getDeptIds()));
        }
        fillCustomerImportMnemonicCodes(updateObj, existing);
        clearHiddenFields(updateObj);
        customerMapper.updateById(updateObj);
        if (deptAssignment.hasDeptImport()) {
            Long effectiveDeptId = updateObj.getDeptId() != null ? updateObj.getDeptId() : existing.getDeptId();
            if (Boolean.TRUE.equals(updateObj.getAllowMultiDept())) {
                syncCustomerDeptList(existing.getId(), buildCustomerDeptIds(effectiveDeptId,
                        deptAssignment.getDeptIds(), true));
            } else {
                customerDeptMapper.deleteByCustomerId(existing.getId());
            }
        }
        ErpCustomerDO newest = customerMapper.selectById(existing.getId());
        recordUpdate(existing, newest != null ? newest : updateObj);
    }

    private ImportDeptContext buildImportDeptContext(List<ErpCustomerImportExcelVO> list) {
        if (list.stream().filter(Objects::nonNull).noneMatch(this::hasImportDeptText)) {
            return ImportDeptContext.empty();
        }
        Long loginUserId = getLoginUserId();
        if (permissionApi == null
                || !permissionApi.hasAnyPermissions(loginUserId, CUSTOMER_DEPT_DISTRIBUTE_PERMISSION)) {
            return ImportDeptContext.permissionDenied();
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, FIELD_PERMISSION_MODULE);
        Set<Long> allowedDeptIds = Boolean.TRUE.equals(permission != null ? permission.getAll() : null)
                ? null : (permission != null && CollUtil.isNotEmpty(permission.getDeptIds())
                        ? permission.getDeptIds() : Collections.emptySet());
        List<DeptRespDTO> depts = deptApi.getDeptListByStatus(null);
        if (depts == null) {
            depts = Collections.emptyList();
        }
        Map<Long, DeptRespDTO> deptMap = depts.stream()
                .filter(dept -> dept != null && dept.getId() != null)
                .collect(Collectors.toMap(DeptRespDTO::getId, dept -> dept, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<DeptRespDTO>> nameMap = new LinkedHashMap<>();
        Map<String, List<DeptRespDTO>> fullNameMap = new LinkedHashMap<>();
        for (DeptRespDTO dept : deptMap.values()) {
            addDeptCandidate(nameMap, dept.getName(), dept);
            addDeptCandidate(fullNameMap, buildDeptFullName(dept.getId(), deptMap), dept);
        }
        return new ImportDeptContext(nameMap, fullNameMap, allowedDeptIds, false);
    }

    private boolean hasImportDeptText(ErpCustomerImportExcelVO importVO) {
        return StringUtils.hasText(trimToNull(importVO.getDeptName()))
                || StringUtils.hasText(trimToNull(importVO.getDeptNames()));
    }

    private ImportDeptAssignment resolveImportDeptAssignment(ErpCustomerImportExcelVO importVO,
                                                             ImportDeptContext context) {
        if (!hasImportDeptText(importVO)) {
            return ImportDeptAssignment.none();
        }
        if (isFieldHidden("deptId") || isFieldHidden("deptIds") || isFieldHidden("allowMultiDept")) {
            throw new IllegalStateException("无客户部门字段权限，不能通过导入设置部门分配");
        }
        if (context.isPermissionDenied()) {
            throw new IllegalStateException("无客户分配部门权限，不能通过导入设置部门分配");
        }
        Long deptId = resolveImportDeptId(importVO.getDeptName(), context);
        List<Long> deptIds = resolveImportDeptIds(importVO.getDeptNames(), context);
        if (deptId == null && CollUtil.isNotEmpty(deptIds)) {
            deptId = deptIds.get(0);
        }
        return new ImportDeptAssignment(true, deptId, buildCustomerDeptIds(deptId, deptIds,
                CollUtil.isNotEmpty(deptIds)));
    }

    private Long resolveImportDeptId(String deptName, ImportDeptContext context) {
        String name = trimToNull(deptName);
        if (name == null) {
            return null;
        }
        return resolveImportDept(name, context).getId();
    }

    private List<Long> resolveImportDeptIds(String deptNames, ImportDeptContext context) {
        String names = trimToNull(deptNames);
        if (names == null) {
            return Collections.emptyList();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        for (String item : names.split(IMPORT_DEPT_NAME_SPLIT_REGEX)) {
            String name = trimToNull(item);
            if (name == null) {
                continue;
            }
            deptIds.add(resolveImportDept(name, context).getId());
        }
        return new ArrayList<>(deptIds);
    }

    private DeptRespDTO resolveImportDept(String inputName, ImportDeptContext context) {
        String normalizedName = normalizeDeptName(inputName);
        boolean fullNameInput = inputName.contains("/") || inputName.contains("\\");
        List<DeptRespDTO> candidates = fullNameInput
                ? context.getFullNameMap().getOrDefault(normalizedName, Collections.emptyList())
                : context.getNameMap().getOrDefault(normalizedName, Collections.emptyList());
        if (CollUtil.isEmpty(candidates)) {
            throw new IllegalArgumentException("部门不存在：" + inputName);
        }
        List<DeptRespDTO> enabledCandidates = candidates.stream()
                .filter(dept -> CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(enabledCandidates)) {
            throw new IllegalArgumentException("部门未启用：" + inputName);
        }
        if (enabledCandidates.size() > 1) {
            throw new IllegalArgumentException("部门名称重复：" + inputName + "，请填写完整路径");
        }
        DeptRespDTO dept = enabledCandidates.get(0);
        if (context.getAllowedDeptIds() != null && !context.getAllowedDeptIds().contains(dept.getId())) {
            throw new IllegalArgumentException("部门超出当前用户可操作范围：" + inputName);
        }
        return dept;
    }

    private void addDeptCandidate(Map<String, List<DeptRespDTO>> map, String name, DeptRespDTO dept) {
        String normalizedName = normalizeDeptName(name);
        if (!StringUtils.hasText(normalizedName)) {
            return;
        }
        map.computeIfAbsent(normalizedName, key -> new ArrayList<>()).add(dept);
    }

    private String normalizeDeptName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            return null;
        }
        return name.replace('／', '/')
                .replace('\\', '/')
                .replaceAll("\\s*/\\s*", "/")
                .toLowerCase();
    }

    private void setIfHasText(java.util.function.Consumer<String> setter, String value) {
        String trim = trimToNull(value);
        if (trim != null) {
            setter.accept(trim);
        }
    }

    private String getImportFailureReason(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
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
        boolean allowMultiDeptHidden = isFieldHidden("allowMultiDept");
        boolean deptIdHidden = isFieldHidden("deptId");
        boolean deptIdsHidden = isFieldHidden("deptIds");
        if (reqVO.getSaleUserId() != null && !isFieldHidden("saleUserId")) {
            wrapper.set(ErpCustomerDO::getSaleUserId, reqVO.getSaleUserId());
            hasUpdate = true;
        }
        if (reqVO.getDeveloperUserId() != null && !isFieldHidden("developerUserId")) {
            wrapper.set(ErpCustomerDO::getDeveloperUserId, reqVO.getDeveloperUserId());
            hasUpdate = true;
        }
        Long batchPrimaryDeptId = resolvePrimaryDeptId(reqVO.getDeptId(), reqVO.getDeptIds(), null);
        if (batchPrimaryDeptId != null && !deptIdHidden) {
            wrapper.set(ErpCustomerDO::getDeptId, batchPrimaryDeptId);
            hasUpdate = true;
        }
        if (reqVO.getAllowMultiDept() != null && !allowMultiDeptHidden) {
            wrapper.set(ErpCustomerDO::getAllowMultiDept, reqVO.getAllowMultiDept());
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
        boolean deptIdsChanged = !deptIdsHidden && reqVO.getDeptIds() != null;
        boolean allowMultiDeptChanged = !allowMultiDeptHidden && reqVO.getAllowMultiDept() != null;
        if (!hasUpdate && !deptIdsChanged && !allowMultiDeptChanged) {
            return;
        }
        List<ErpCustomerDO> customers = customerMapper.selectByIds(reqVO.getIds());
        Map<Long, ErpCustomerDO> customerMap = customers.stream()
                .collect(Collectors.toMap(ErpCustomerDO::getId, customer -> customer, (first, second) -> first));
        if (hasUpdate) {
            customerMapper.update(null, wrapper);
        }
        if (deptIdsChanged || allowMultiDeptChanged) {
            for (Long customerId : reqVO.getIds()) {
                ErpCustomerDO customer = customerMap.get(customerId);
                if (customer == null) {
                    continue;
                }
                Long effectiveDeptId = !deptIdHidden && batchPrimaryDeptId != null ? batchPrimaryDeptId : customer.getDeptId();
                Boolean effectiveAllowMultiDept = allowMultiDeptChanged ? reqVO.getAllowMultiDept() : customer.getAllowMultiDept();
                if (Boolean.FALSE.equals(effectiveAllowMultiDept)) {
                    customerDeptMapper.deleteByCustomerId(customerId);
                    continue;
                }
                syncCustomerDeptList(customerId, buildCustomerDeptIds(effectiveDeptId, reqVO.getDeptIds(),
                        effectiveAllowMultiDept));
            }
        }
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
            recordUpdate(customer, customerMapper.selectById(id));
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
            recordUpdate(customer, customerMapper.selectById(id));
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

    private void validateCreditConfig(ErpCustomerSaveReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (reqVO.getCreditLimit() != null && reqVO.getCreditLimit().compareTo(BigDecimal.ZERO) < 0
                || reqVO.getCreditTermDays() != null && reqVO.getCreditTermDays() < 0) {
            throw exception(CUSTOMER_CREDIT_VALUE_INVALID);
        }
        if (Boolean.TRUE.equals(reqVO.getCreditEnabled())
                && reqVO.getCreditLimit() == null && reqVO.getCreditTermDays() == null) {
            throw exception(CUSTOMER_CREDIT_CONFIG_REQUIRED);
        }
    }

    private void validateDeptCreditItems(ErpCustomerDO customer, List<ErpCustomerDeptCreditSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> allowedDeptIds = new LinkedHashSet<>(getCustomerSaleDeptIds(customer));
        Set<Long> deptIds = new LinkedHashSet<>();
        for (ErpCustomerDeptCreditSaveReqVO.Item item : items) {
            if (item == null || item.getDeptId() == null) {
                continue;
            }
            if (!deptIds.add(item.getDeptId())) {
                throw exception(CUSTOMER_DEPT_CREDIT_DUPLICATE_DEPT);
            }
            if (!allowedDeptIds.contains(item.getDeptId())) {
                throw exception(CUSTOMER_DEPT_CREDIT_DEPT_NOT_ALLOWED);
            }
            validateDeptCreditConfig(item);
        }
        if (CollUtil.isNotEmpty(deptIds)) {
            deptApi.validateDeptList(deptIds);
        }
    }

    private void validateDeptCreditConfig(ErpCustomerDeptCreditSaveReqVO.Item item) {
        if (item.getCreditLimit() != null && item.getCreditLimit().compareTo(BigDecimal.ZERO) < 0
                || item.getCreditTermDays() != null && item.getCreditTermDays() < 0) {
            throw exception(CUSTOMER_CREDIT_VALUE_INVALID);
        }
        if (Boolean.TRUE.equals(item.getCreditEnabled())
                && item.getCreditLimit() == null && item.getCreditTermDays() == null) {
            throw exception(CUSTOMER_CREDIT_CONFIG_REQUIRED);
        }
    }

    private ErpCustomerDeptCreditDO buildDeptCreditDO(Long customerId, ErpCustomerDeptCreditSaveReqVO.Item item) {
        ErpCustomerDeptCreditDO credit = BeanUtils.toBean(item, ErpCustomerDeptCreditDO.class);
        credit.setCustomerId(customerId);
        normalizeDeptCreditConfig(credit);
        return credit;
    }

    private ErpCustomerDeptCreditRespVO.Item buildDeptCreditItem(Long deptId, String deptName,
                                                                 ErpCustomerDeptCreditDO credit) {
        ErpCustomerDeptCreditRespVO.Item item = credit == null
                ? new ErpCustomerDeptCreditRespVO.Item()
                : BeanUtils.toBean(credit, ErpCustomerDeptCreditRespVO.Item.class);
        item.setDeptId(deptId);
        item.setDeptName(deptName);
        if (item.getCreditEnabled() == null) {
            item.setCreditEnabled(false);
        }
        return item;
    }

    private void normalizeDeptCreditConfig(ErpCustomerDeptCreditDO credit) {
        if (!Boolean.TRUE.equals(credit.getCreditEnabled())) {
            credit.setCreditEnabled(false);
            credit.setCreditLimit(null);
            credit.setCreditTermDays(null);
        }
    }

    private void normalizeCreditConfig(ErpCustomerDO customer) {
        if (customer == null) {
            return;
        }
        if (!Boolean.TRUE.equals(customer.getCreditEnabled())) {
            customer.setCreditEnabled(false);
            customer.setCreditLimit(null);
            customer.setCreditTermDays(null);
        }
    }

    private ErpCustomerCreditStatusBO buildCustomerCreditStatus(ErpCustomerDO customer) {
        return buildCustomerCreditStatus(customer, customer.getCreditEnabled(), customer.getCreditLimit(),
                customer.getCreditTermDays(), null);
    }

    private ErpCustomerCreditStatusBO buildCustomerCreditStatus(ErpCustomerDO customer,
                                                               ErpCustomerDeptCreditDO deptCredit,
                                                               Long saleDeptId) {
        return buildCustomerCreditStatus(customer, deptCredit.getCreditEnabled(), deptCredit.getCreditLimit(),
                deptCredit.getCreditTermDays(), saleDeptId);
    }

    private ErpCustomerCreditStatusBO buildCustomerCreditStatus(ErpCustomerDO customer, Boolean creditEnabledFlag,
                                                               BigDecimal creditLimit, Integer creditTermDays,
                                                               Long saleDeptId) {
        BigDecimal balance = getReceivableBalance(customer.getId(), saleDeptId);
        LocalDate earliestUnpaidDate = balance.compareTo(BigDecimal.ZERO) > 0
                ? findEarliestUnpaidDate(customer.getId(), saleDeptId, balance) : null;
        Integer debtDays = earliestUnpaidDate == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(earliestUnpaidDate, LocalDate.now()));

        boolean creditEnabled = Boolean.TRUE.equals(creditEnabledFlag);
        boolean amountExceeded = creditEnabled && creditLimit != null
                && balance.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(creditLimit) >= 0;
        boolean termExceeded = creditEnabled && creditTermDays != null
                && debtDays != null && debtDays > creditTermDays;
        String blockedReason = buildCreditBlockedReason(creditLimit, creditTermDays, balance, debtDays,
                amountExceeded, termExceeded);

        return new ErpCustomerCreditStatusBO()
                .setCustomerId(customer.getId())
                .setCreditEnabled(creditEnabledFlag)
                .setCreditLimit(creditLimit)
                .setCreditTermDays(creditTermDays)
                .setReceivableBalance(balance)
                .setEarliestUnpaidDate(earliestUnpaidDate)
                .setDebtDays(debtDays)
                .setAmountExceeded(amountExceeded)
                .setTermExceeded(termExceeded)
                .setBlocked(amountExceeded || termExceeded)
                .setBlockedReason(blockedReason);
    }

    private BigDecimal getReceivableBalance(Long customerId) {
        return getReceivableBalance(customerId, null);
    }

    private BigDecimal getReceivableBalance(Long customerId, Long saleDeptId) {
        ErpReceivableAccountDO account = saleDeptId == null
                ? receivableAccountMapper.selectByCustomerId(customerId)
                : receivableAccountMapper.selectByCustomerIdAndDeptId(customerId, saleDeptId);
        return account == null || account.getReceivableBalance() == null
                ? BigDecimal.ZERO : account.getReceivableBalance();
    }

    private String buildCreditBlockedReason(BigDecimal creditLimit, Integer creditTermDays,
                                            BigDecimal balance, Integer debtDays,
                                            boolean amountExceeded, boolean termExceeded) {
        List<String> reasons = new ArrayList<>();
        if (amountExceeded) {
            reasons.add("欠款金额 " + balance + " 已达到授信金额 " + creditLimit);
        }
        if (termExceeded) {
            reasons.add("欠款天数 " + debtDays + " 天已超过授信期限 " + creditTermDays + " 天");
        }
        return String.join("；", reasons);
    }

    private LocalDate findEarliestUnpaidDate(Long customerId, Long saleDeptId, BigDecimal currentBalance) {
        List<ReceivableTimelineRow> rows = buildReceivableTimelineRows(customerId, saleDeptId);
        BigDecimal remaining = currentBalance;
        LocalDate earliest = null;
        for (int i = rows.size() - 1; i >= 0 && remaining.compareTo(BigDecimal.ZERO) > 0; i--) {
            ReceivableTimelineRow row = rows.get(i);
            if (row.amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            remaining = remaining.subtract(row.amount);
            earliest = row.docDate == null ? null : row.docDate.toLocalDate();
        }
        return earliest;
    }

    private List<ReceivableTimelineRow> buildReceivableTimelineRows(Long customerId, Long saleDeptId) {
        List<ReceivableTimelineRow> rows = new ArrayList<>();
        saleOutMapper.selectList(new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, customerId)
                .eqIfPresent(ErpSaleOutDO::getDeptId, saleDeptId)
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus()))
                .forEach(item -> rows.add(new ReceivableTimelineRow(item.getOutTime(), nullToZero(item.getTotalPrice()))));
        saleReturnMapper.selectList(new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, customerId)
                .eqIfPresent(ErpSaleReturnDO::getDeptId, saleDeptId)
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus()))
                .forEach(item -> rows.add(new ReceivableTimelineRow(item.getReturnTime(), negateAmount(item.getTotalPrice()))));
        salePriceAdjustMapper.selectList(new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, customerId)
                .eqIfPresent(ErpSalePriceAdjustDO::getDeptId, saleDeptId)
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus()))
                .forEach(item -> rows.add(new ReceivableTimelineRow(item.getAdjustDate(), nullToZero(item.getTotalAdjustPrice()))));
        financeReceiptMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getCustomerId, customerId)
                .eqIfPresent(ErpFinanceReceiptDO::getDeptId, saleDeptId)
                .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus()))
                .forEach(item -> rows.add(new ReceivableTimelineRow(item.getReceiptTime(), negateAmount(item.getTotalPrice()))));
        receivableWriteOffMapper.selectList(new LambdaQueryWrapperX<ErpReceivableWriteOffDO>()
                .eq(ErpReceivableWriteOffDO::getCustomerId, customerId)
                .eqIfPresent(ErpReceivableWriteOffDO::getDeptId, saleDeptId))
                .forEach(item -> rows.add(new ReceivableTimelineRow(item.getWriteOffTime(), negateAmount(item.getWriteOffAmount()))));
        receivableOtherMapper.selectList(new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, customerId)
                .eqIfPresent(ErpReceivableOtherDO::getDeptId, saleDeptId)
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus()))
                .forEach(item -> rows.add(new ReceivableTimelineRow(
                        item.getBizTime() == null ? null : item.getBizTime().atStartOfDay(),
                        nullToZero(item.getReceivableAmount()))));
        rows.sort(Comparator.comparing(ReceivableTimelineRow::getDocDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private BigDecimal nullToZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return nullToZero(amount).negate();
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

    @Override
    public Map<Long, List<Long>> getCustomerDeptMap(Collection<Long> customerIds) {
        if (CollUtil.isEmpty(customerIds)) {
            return Collections.emptyMap();
        }
        return customerDeptMapper.selectListByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(ErpCustomerDeptDO::getCustomerId, Collectors.mapping(
                        ErpCustomerDeptDO::getDeptId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().distinct().collect(Collectors.toList())))));
    }

    private Long resolvePrimaryDeptId(Long deptId, Collection<Long> deptIds, Long fallbackDeptId) {
        if (deptId != null) {
            return deptId;
        }
        if (CollUtil.isNotEmpty(deptIds)) {
            return deptIds.stream().filter(Objects::nonNull).findFirst().orElse(fallbackDeptId);
        }
        return fallbackDeptId;
    }

    private List<Long> buildCustomerDeptIds(Long deptId, Collection<Long> deptIds, Boolean allowMultiDept) {
        if (!Boolean.TRUE.equals(allowMultiDept)) {
            return Collections.emptyList();
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (deptId != null) {
            ids.add(deptId);
        }
        if (CollUtil.isNotEmpty(deptIds)) {
            deptIds.stream().filter(Objects::nonNull).forEach(ids::add);
        }
        return new ArrayList<>(ids);
    }

    private void syncCustomerDeptList(Long customerId, Collection<Long> deptIds) {
        customerDeptMapper.deleteByCustomerId(customerId);
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        customerDeptMapper.insertBatch(deptIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(deptId -> {
                    ErpCustomerDeptDO dept = new ErpCustomerDeptDO();
                    dept.setCustomerId(customerId);
                    dept.setDeptId(deptId);
                    return dept;
                })
                .collect(Collectors.toList()));
    }

    private String buildDeptNames(Collection<Long> deptIds, Map<Long, String> deptNameMap) {
        if (CollUtil.isEmpty(deptIds)) {
            return null;
        }
        return deptIds.stream()
                .map(deptNameMap::get)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    private Map<Long, String> buildDeptNameMap(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        Map<Long, DeptRespDTO> deptMap = loadDeptWithParents(deptIds);
        Map<Long, String> result = new LinkedHashMap<>();
        deptIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(deptId -> {
                    String deptName = buildDeptFullName(deptId, deptMap);
                    if (StringUtils.hasText(deptName)) {
                        result.put(deptId, deptName);
                    }
                });
        return result;
    }

    private Map<Long, DeptRespDTO> loadDeptWithParents(Collection<Long> deptIds) {
        Map<Long, DeptRespDTO> result = new LinkedHashMap<>();
        Set<Long> pendingIds = deptIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        while (CollUtil.isNotEmpty(pendingIds)) {
            Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(pendingIds);
            pendingIds = new LinkedHashSet<>();
            if (CollUtil.isEmpty(deptMap)) {
                break;
            }
            for (DeptRespDTO dept : deptMap.values()) {
                if (dept == null || dept.getId() == null || result.containsKey(dept.getId())) {
                    continue;
                }
                result.put(dept.getId(), dept);
                Long parentId = dept.getParentId();
                if (parentId != null && parentId > 0 && !result.containsKey(parentId)) {
                    pendingIds.add(parentId);
                }
            }
        }
        return result;
    }

    private String buildDeptFullName(Long deptId, Map<Long, DeptRespDTO> deptMap) {
        DeptRespDTO dept = deptMap.get(deptId);
        if (dept == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Set<Long> visitedIds = new LinkedHashSet<>();
        Long currentId = deptId;
        while (currentId != null && visitedIds.add(currentId)) {
            DeptRespDTO current = deptMap.get(currentId);
            if (current == null) {
                break;
            }
            if (StringUtils.hasText(current.getName())) {
                names.add(0, current.getName());
            }
            Long parentId = current.getParentId();
            if (parentId == null || parentId <= 0) {
                break;
            }
            currentId = parentId;
        }
        return CollUtil.isEmpty(names) ? null : String.join(" / ", names);
    }

    private CustomerVisibleScope getCustomerVisibleScope() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_customer");
        if (permission == null) {
            return null;
        }
        Long selfUserId = Boolean.TRUE.equals(permission.getSelf()) ? loginUserId : null;
        return new CustomerVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(), selfUserId);
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

    private void validateCustomerNameUnique(Long id, String name) {
        String normalizedName = trimToNull(name);
        if (!StringUtils.hasText(normalizedName)) {
            return;
        }
        ErpCustomerDO customer = customerMapper.selectByNameExcludeId(normalizedName, id);
        if (customer != null) {
            throw exception(CUSTOMER_NAME_DUPLICATE, normalizedName);
        }
    }

    private String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void fillCustomerImportMnemonicCodes(ErpCustomerSaveReqVO reqVO) {
        String name = trimToNull(reqVO.getName());
        if (!StringUtils.hasText(name)) {
            return;
        }
        if (!StringUtils.hasText(reqVO.getPinyinCode())) {
            reqVO.setPinyinCode(ErpMnemonicCodeUtils.buildPinyinCode(name));
        }
        if (!StringUtils.hasText(reqVO.getWubiCode())) {
            reqVO.setWubiCode(ErpMnemonicCodeUtils.buildWubiCode(name));
        }
    }

    private void fillCustomerImportMnemonicCodes(ErpCustomerDO updateObj, ErpCustomerDO existing) {
        String name = trimToNull(updateObj.getName());
        if (!StringUtils.hasText(name)) {
            return;
        }
        if (!StringUtils.hasText(existing.getPinyinCode())) {
            updateObj.setPinyinCode(ErpMnemonicCodeUtils.buildPinyinCode(name));
        }
        if (!StringUtils.hasText(existing.getWubiCode())) {
            updateObj.setWubiCode(ErpMnemonicCodeUtils.buildWubiCode(name));
        }
    }

    private void recordCreate(ErpCustomerDO customer) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_CUSTOMER_TYPE, customer.getId(), customer, customer.getCode());
        }
    }

    private void recordUpdate(ErpCustomerDO oldCustomer, ErpCustomerDO newCustomer) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_CUSTOMER_TYPE, newCustomer.getId(), oldCustomer, newCustomer,
                    newCustomer.getCode());
        }
    }

    private void recordDelete(ErpCustomerDO customer) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_CUSTOMER_TYPE, customer.getId(), customer, customer.getCode());
        }
    }

    private static final class ImportDeptContext {

        private final Map<String, List<DeptRespDTO>> nameMap;
        private final Map<String, List<DeptRespDTO>> fullNameMap;
        private final Set<Long> allowedDeptIds;
        private final boolean permissionDenied;

        private ImportDeptContext(Map<String, List<DeptRespDTO>> nameMap,
                                  Map<String, List<DeptRespDTO>> fullNameMap,
                                  Set<Long> allowedDeptIds,
                                  boolean permissionDenied) {
            this.nameMap = nameMap;
            this.fullNameMap = fullNameMap;
            this.allowedDeptIds = allowedDeptIds;
            this.permissionDenied = permissionDenied;
        }

        private static ImportDeptContext empty() {
            return new ImportDeptContext(Collections.emptyMap(), Collections.emptyMap(), null, false);
        }

        private static ImportDeptContext permissionDenied() {
            return new ImportDeptContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), true);
        }

        private Map<String, List<DeptRespDTO>> getNameMap() {
            return nameMap;
        }

        private Map<String, List<DeptRespDTO>> getFullNameMap() {
            return fullNameMap;
        }

        private Set<Long> getAllowedDeptIds() {
            return allowedDeptIds;
        }

        private boolean isPermissionDenied() {
            return permissionDenied;
        }

    }

    private static final class ImportDeptAssignment {

        private final boolean deptImport;
        private final Long deptId;
        private final List<Long> deptIds;

        private ImportDeptAssignment(boolean deptImport, Long deptId, List<Long> deptIds) {
            this.deptImport = deptImport;
            this.deptId = deptId;
            this.deptIds = deptIds == null ? Collections.emptyList() : deptIds;
        }

        private static ImportDeptAssignment none() {
            return new ImportDeptAssignment(false, null, Collections.emptyList());
        }

        private boolean hasDeptImport() {
            return deptImport;
        }

        private Long getDeptId() {
            return deptId;
        }

        private List<Long> getDeptIds() {
            return deptIds;
        }

    }

    private static final class ReceivableTimelineRow {

        private final LocalDateTime docDate;
        private final BigDecimal amount;

        private ReceivableTimelineRow(LocalDateTime docDate, BigDecimal amount) {
            this.docDate = docDate;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
        }

        private LocalDateTime getDocDate() {
            return docDate;
        }

    }

    private static class CustomerVisibleScope {

        private final boolean all;
        private final Collection<Long> deptIds;
        private final Long selfUserId;

        private CustomerVisibleScope(boolean all, Collection<Long> deptIds, Long selfUserId) {
            this.all = all;
            this.deptIds = deptIds == null ? Collections.emptyList() : deptIds;
            this.selfUserId = selfUserId;
        }

        public boolean isAll() {
            return all;
        }

        public Collection<Long> getDeptIds() {
            return deptIds;
        }

        public Long getSelfUserId() {
            return selfUserId;
        }
    }

}
