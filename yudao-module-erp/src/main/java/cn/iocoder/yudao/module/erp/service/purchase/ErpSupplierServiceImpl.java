package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDeptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DISABLE_FAIL_PAYABLE_NOT_CLEAR;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SUPPLIER_TYPE;

/**
 * ERP 渚涘簲鍟?Service 瀹炵幇绫?
 *
 * @author 鑺嬮亾婧愮爜
 */
@Service
@Validated
public class ErpSupplierServiceImpl implements ErpSupplierService {

    private static final String FIELD_PERMISSION_MODULE = "erp_supplier";
    private static final List<String> SUPPLIER_CATEGORY_OPTIONS = Arrays.asList("供应商", "既是客户又是供应商");

    @Resource
    private ErpSupplierMapper supplierMapper;
    @Resource
    private ErpSupplierDeptMapper supplierDeptMapper;
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpPayableAccountMapper payableAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplier(ErpSupplierSaveReqVO createReqVO) {
        ErpSupplierDO supplier = BeanUtils.toBean(createReqVO, ErpSupplierDO.class);
        Collection<Long> supplierDeptIds = createReqVO.getDeptIds();
        if (isFieldHidden("allowMultiDept")) {
            supplier.setAllowMultiDept(false);
        }
        if (isFieldHidden("deptIds")) {
            supplierDeptIds = Collections.emptyList();
        }
        // 鍏滃簳榛樿鍊?
        if (supplier.getSort() == null) {
            supplier.setSort(0);
        }
        if (supplier.getStatus() == null) {
            supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (supplier.getDeptId() == null) {
            supplier.setDeptId(getLoginUserDeptId());
        }
        applySupplierCreateDefaults(supplier);
        // 鑷姩鐢熸垚缂栫爜
        supplier.setCode(normalizeCode(supplier.getCode()));
        if (!StringUtils.hasText(supplier.getCode())) {
            supplier.setCode(generateSupplierCode());
        }
        validateSupplierCodeUnique(null, supplier.getCode());
        supplierMapper.insert(supplier);
        syncSupplierDeptList(supplier.getId(), buildSupplierDeptIds(supplier.getDeptId(), supplierDeptIds,
                supplier.getAllowMultiDept()));
        operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier.getName());
        return supplier.getId();
    }

    private String generateSupplierCode() {
        String maxCode = supplierMapper.selectMaxCode();
        int nextNum = 1;
        if (maxCode != null && maxCode.startsWith("GYS")) {
            try {
                nextNum = Integer.parseInt(maxCode.substring(3)) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("GYS%06d", nextNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplier(ErpSupplierSaveReqVO updateReqVO) {
        // 鏍￠獙瀛樺湪
        ErpSupplierDO supplier = validateSupplierExists(updateReqVO.getId());
        // 鏇存柊
        ErpSupplierDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierDO.class);
        updateObj.setCode(supplier.getCode());
        validateSupplierCategory(updateObj.getCategory());
        boolean allowMultiDeptHidden = isFieldHidden("allowMultiDept");
        boolean deptIdHidden = isFieldHidden("deptId");
        boolean deptIdsHidden = isFieldHidden("deptIds");
        if (allowMultiDeptHidden) {
            updateObj.setAllowMultiDept(null);
        }
        if (deptIdHidden) {
            updateObj.setDeptId(null);
        } else {
            updateObj.setDeptId(resolvePrimaryDeptId(updateObj.getDeptId(), updateReqVO.getDeptIds(), supplier.getDeptId()));
        }
        supplierMapper.updateById(updateObj);
        Boolean effectiveAllowMultiDept = updateObj.getAllowMultiDept() != null
                ? updateObj.getAllowMultiDept() : supplier.getAllowMultiDept();
        Long effectiveDeptId = deptIdHidden ? supplier.getDeptId() : updateObj.getDeptId();
        boolean allowMultiDeptChanged = !allowMultiDeptHidden && updateReqVO.getAllowMultiDept() != null;
        if (allowMultiDeptChanged && Boolean.FALSE.equals(effectiveAllowMultiDept)) {
            supplierDeptMapper.deleteBySupplierId(updateReqVO.getId());
        } else if (!deptIdsHidden && (updateReqVO.getDeptIds() != null || allowMultiDeptChanged)) {
            syncSupplierDeptList(updateReqVO.getId(), buildSupplierDeptIds(effectiveDeptId, updateReqVO.getDeptIds(),
                    effectiveAllowMultiDept));
        }
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, updateReqVO.getId(),
                StringUtils.hasText(updateObj.getName()) ? updateObj.getName() : supplier.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSupplier(ErpSupplierBatchUpdateReqVO reqVO) {
        if (CommonStatusEnum.isDisable(reqVO.getStatus())) {
            validateSupplierPayableClear(reqVO.getIds());
        }
        LambdaUpdateWrapper<ErpSupplierDO> wrapper = new LambdaUpdateWrapper<ErpSupplierDO>()
                .in(ErpSupplierDO::getId, reqVO.getIds());
        boolean allowMultiDeptHidden = isFieldHidden("allowMultiDept");
        boolean deptIdHidden = isFieldHidden("deptId");
        boolean deptIdsHidden = isFieldHidden("deptIds");
        boolean hasUpdate = false;
        if (reqVO.getStatus() != null && !isFieldHidden("status")) {
            wrapper.set(ErpSupplierDO::getStatus, reqVO.getStatus());
            if (CommonStatusEnum.isDisable(reqVO.getStatus())) {
                wrapper.set(ErpSupplierDO::getDisabledBy, getLoginUserId());
                wrapper.set(ErpSupplierDO::getDisabledTime, LocalDateTime.now());
            }
            hasUpdate = true;
        }
        Long batchPrimaryDeptId = resolvePrimaryDeptId(reqVO.getDeptId(), reqVO.getDeptIds(), null);
        if (batchPrimaryDeptId != null && !deptIdHidden) {
            wrapper.set(ErpSupplierDO::getDeptId, batchPrimaryDeptId);
            hasUpdate = true;
        }
        if (reqVO.getAllowMultiDept() != null && !allowMultiDeptHidden) {
            wrapper.set(ErpSupplierDO::getAllowMultiDept, reqVO.getAllowMultiDept());
            hasUpdate = true;
        }
        if (reqVO.getRegion() != null && !isFieldHidden("region")) {
            wrapper.set(ErpSupplierDO::getRegion, reqVO.getRegion());
            hasUpdate = true;
        }
        if (reqVO.getSupplierType() != null && !isFieldHidden("supplierType")) {
            wrapper.set(ErpSupplierDO::getSupplierType, reqVO.getSupplierType());
            hasUpdate = true;
        }
        if (reqVO.getPurchaser() != null && !isFieldHidden("purchaser")) {
            wrapper.set(ErpSupplierDO::getPurchaser, reqVO.getPurchaser());
            hasUpdate = true;
        }
        if (reqVO.getSettleMethod() != null && !isFieldHidden("settleMethod")) {
            wrapper.set(ErpSupplierDO::getSettleMethod, reqVO.getSettleMethod());
            hasUpdate = true;
        }
        if (reqVO.getRemark() != null && !isFieldHidden("remark")) {
            wrapper.set(ErpSupplierDO::getRemark, reqVO.getRemark());
            hasUpdate = true;
        }
        boolean deptIdsChanged = !deptIdsHidden && reqVO.getDeptIds() != null;
        boolean allowMultiDeptChanged = !allowMultiDeptHidden && reqVO.getAllowMultiDept() != null;
        if (!hasUpdate && !deptIdsChanged && !allowMultiDeptChanged) {
            return;
        }
        List<ErpSupplierDO> suppliers = (deptIdsChanged || allowMultiDeptChanged)
                ? supplierMapper.selectByIds(reqVO.getIds()) : Collections.emptyList();
        Map<Long, ErpSupplierDO> supplierMap = suppliers.stream()
                .collect(Collectors.toMap(ErpSupplierDO::getId, supplier -> supplier, (first, second) -> first));
        if (hasUpdate) {
            supplierMapper.update(null, wrapper);
        }
        if (deptIdsChanged || allowMultiDeptChanged) {
            for (Long supplierId : reqVO.getIds()) {
                ErpSupplierDO supplier = supplierMap.get(supplierId);
                if (supplier == null) {
                    continue;
                }
                Long effectiveDeptId = !deptIdHidden && batchPrimaryDeptId != null ? batchPrimaryDeptId : supplier.getDeptId();
                Boolean effectiveAllowMultiDept = allowMultiDeptChanged ? reqVO.getAllowMultiDept() : supplier.getAllowMultiDept();
                if (Boolean.FALSE.equals(effectiveAllowMultiDept)) {
                    supplierDeptMapper.deleteBySupplierId(supplierId);
                    continue;
                }
                syncSupplierDeptList(supplierId, buildSupplierDeptIds(effectiveDeptId, reqVO.getDeptIds(),
                        effectiveAllowMultiDept));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableSupplier(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        validateSupplierPayableClear(distinctIds);
        for (Long id : distinctIds) {
            disableSupplier(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        // 鏍￠獙瀛樺湪
        ErpSupplierDO supplier = validateSupplierExists(id);
        baseArchiveReferenceService.validateSupplierNotReferenced(id);
        // 鍒犻櫎
        supplierMapper.deleteById(id);
        supplierDeptMapper.deleteBySupplierId(id);
        operateLogService.recordDelete(ERP_SUPPLIER_TYPE, id, supplier.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteSupplier(id);
        }
    }

    private void validateSupplierNotReferenced(Long supplierId) {
        if (purchaseOrderMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("閲囪喘璁㈠崟", purchaseOrderMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseInMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("purchase in", purchaseInMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseReturnMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("閲囪喘閫€璐у崟", purchaseReturnMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseInvoiceMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("閲囪喘绁ㄦ嵁", purchaseInvoiceMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchasePriceAdjustMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("purchase price adjust", purchasePriceAdjustMapper.selectFirstNoBySupplierId(supplierId)));
        }
    }

    private String buildReferencedMessage(String bizName, String no) {
        return StringUtils.hasText(no) ? bizName + " " + no : bizName;
    }

    private ErpSupplierDO validateSupplierExists(Long id) {
        ErpSupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        return supplier;
    }

    @Override
    public ErpSupplierDO getSupplier(Long id) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectById(id);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisibleById(id, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public ErpSupplierDO validateSupplier(Long id) {
        ErpSupplierDO supplier = getSupplier(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            throw exception(SUPPLIER_NOT_ENABLE, supplier.getName());
        }
        return supplier;
    }

    @Override
    public List<ErpSupplierDO> getSupplierList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectByIds(ids);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisibleListByIds(ids, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public PageResult<ErpSupplierDO> getSupplierPage(ErpSupplierPageReqVO pageReqVO) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectPage(pageReqVO);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisiblePage(pageReqVO, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public void updateSupplierStatus(Long id, Integer status) {
        if (CommonStatusEnum.isDisable(status)) {
            disableSupplier(id);
            return;
        }
        ErpSupplierDO supplier = validateSupplierExists(id);
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            restoreSupplier(Collections.singletonList(id));
            return;
        }
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        supplierMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreSupplier(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long id : distinctIds) {
            ErpSupplierDO supplier = validateSupplierExists(id);
            if (!CommonStatusEnum.isDisable(supplier.getStatus())) {
                throw exception(SUPPLIER_NOT_ENABLE, supplier.getName());
            }
            supplierMapper.update(null, new LambdaUpdateWrapper<ErpSupplierDO>()
                    .eq(ErpSupplierDO::getId, id)
                    .set(ErpSupplierDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                    .set(ErpSupplierDO::getDisabledBy, null)
                    .set(ErpSupplierDO::getDisabledTime, null));
            operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier.getName());
        }
    }

    private void disableSupplier(Long id) {
        ErpSupplierDO supplier = validateSupplierExists(id);
        validateSupplierPayableClear(supplier);
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(id);
        updateObj.setStatus(CommonStatusEnum.DISABLE.getStatus());
        updateObj.setDisabledBy(getLoginUserId());
        updateObj.setDisabledTime(LocalDateTime.now());
        supplierMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier.getName());
    }

    @Override
    public List<ErpSupplierDO> getSupplierListByStatus(Integer status) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectListByStatus(status);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisibleListByStatus(status, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public List<ErpSupplierDO> getSupplierListByNameLike(String name) {
        if (name == null || name.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectListByNameLike(name);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisibleListByNameLike(name, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importSupplierList(List<ErpSupplierImportExcelVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ErpSupplierImportExcelVO importVO : list) {
            if (importVO == null || !StringUtils.hasText(importVO.getName())) {
                continue;
            }
            ErpSupplierDO supplier = BeanUtils.toBean(importVO, ErpSupplierDO.class);
            if (!StringUtils.hasText(supplier.getCode())) {
                supplier.setCode(generateSupplierCode());
            }
            if (supplier.getStatus() == null) {
                supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (supplier.getSort() == null) {
                supplier.setSort(0);
            }
            purchaseDocumentDefaultService.fillCreateDefaults(supplier);
            applySupplierCreateDefaults(supplier);
            supplierMapper.insert(supplier);
            syncSupplierDeptList(supplier.getId(), buildSupplierDeptIds(supplier.getDeptId(), null,
                    supplier.getAllowMultiDept()));
            operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier.getName());
        }
    }

    @Override
    public Map<Long, List<Long>> getSupplierDeptMap(Collection<Long> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return Collections.emptyMap();
        }
        return supplierDeptMapper.selectListBySupplierIds(supplierIds).stream()
                .collect(Collectors.groupingBy(ErpSupplierDeptDO::getSupplierId, Collectors.mapping(
                        ErpSupplierDeptDO::getDeptId, Collectors.toList())));
    }

    private boolean isFieldHidden(String fieldName) {
        return fieldPermissionMasker != null && fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, fieldName);
    }

    private void validateSupplierPayableClear(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            validateSupplierPayableClear(validateSupplierExists(id));
        }
    }

    private void validateSupplierPayableClear(ErpSupplierDO supplier) {
        ErpPayableAccountDO account = payableAccountMapper.selectBySupplierId(supplier.getId());
        BigDecimal balance = account == null || account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw exception(SUPPLIER_DISABLE_FAIL_PAYABLE_NOT_CLEAR, supplier.getName(), balance);
        }
    }

    private void applySupplierCreateDefaults(ErpSupplierDO supplier) {
        if (supplier.getAllowMultiDept() == null) {
            supplier.setAllowMultiDept(false);
        }
        if (!StringUtils.hasText(supplier.getCategory())) {
            supplier.setCategory("供应商");
            return;
        }
        validateSupplierCategory(supplier.getCategory());
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

    private List<Long> buildSupplierDeptIds(Long deptId, Collection<Long> deptIds, Boolean allowMultiDept) {
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
        return ids.stream().collect(Collectors.toList());
    }

    private void syncSupplierDeptList(Long supplierId, Collection<Long> deptIds) {
        supplierDeptMapper.deleteBySupplierId(supplierId);
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        supplierDeptMapper.insertBatch(deptIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(deptId -> {
                    ErpSupplierDeptDO dept = new ErpSupplierDeptDO();
                    dept.setSupplierId(supplierId);
                    dept.setDeptId(deptId);
                    return dept;
                })
                .collect(Collectors.toList()));
    }

    private void validateSupplierCategory(String category) {
        if (StringUtils.hasText(category) && !SUPPLIER_CATEGORY_OPTIONS.contains(category)) {
            throw exception(SUPPLIER_CATEGORY_INVALID);
        }
    }

    private void validateSupplierCodeUnique(Long id, String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        ErpSupplierDO supplier = supplierMapper.selectByCodeExcludeId(code, id);
        if (supplier != null) {
            throw exception(SUPPLIER_CODE_DUPLICATE, code);
        }
    }

    private String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private SupplierVisibleScope getSupplierVisibleScope() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_supplier");
        if (permission == null) {
            return null;
        }
        Long selfUserId = Boolean.TRUE.equals(permission.getSelf()) ? loginUserId : null;
        return new SupplierVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(), selfUserId);
    }

    private static class SupplierVisibleScope {

        private final boolean all;
        private final Collection<Long> deptIds;
        private final Long selfUserId;

        private SupplierVisibleScope(boolean all, Collection<Long> deptIds, Long selfUserId) {
            this.all = all;
            this.deptIds = deptIds == null ? java.util.Collections.emptyList() : deptIds;
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
