package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierDeptDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportRespVO;
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
import cn.iocoder.yudao.module.erp.service.base.ErpArchiveMergeService;
import cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DISABLE_FAIL_PAYABLE_NOT_CLEAR;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ARCHIVE_MERGE_SAME_ID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_MERGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_IMPORT_SUB_TYPE;
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
    private static final Pattern IMPORT_DEPT_SEPARATOR = Pattern.compile("[、,，;；\\r\\n]+");

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
    private ErpArchiveMergeService archiveMergeService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpPayableAccountMapper payableAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplier(ErpSupplierSaveReqVO createReqVO) {
        ErpSupplierDO supplier = BeanUtils.toBean(createReqVO, ErpSupplierDO.class);
        supplier.setName(trimToNull(supplier.getName()));
        validateSupplierNameUnique(null, supplier.getName());
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
        Long loginUserDeptId = getLoginUserDeptId();
        if (supplier.getDeptId() == null) {
            supplier.setDeptId(loginUserDeptId);
        }
        supplier.setCreateDeptId(loginUserDeptId != null ? loginUserDeptId : supplier.getDeptId());
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
        operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier, supplier.getCode());
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
        updateObj.setName(trimToNull(updateObj.getName()));
        validateSupplierNameUnique(supplier.getId(), updateObj.getName());
        updateObj.setCode(supplier.getCode());
        updateObj.setCreateDeptId(supplier.getCreateDeptId());
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
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, updateReqVO.getId(), supplier,
                supplierMapper.selectById(updateReqVO.getId()), supplier.getCode());
    }

    @Override
    public ErpSupplierDeptDistributionRespVO getSupplierDeptDistribution(Long id) {
        // 查看分配部门信息时，使用无可见范围限制的查询——调用此接口的用户已通过权限校验，
        // 不应再受数据权限过滤的影响（例如部门可见范围会导致跨部门分配信息丢失）
        ErpSupplierDO supplier = DataPermissionUtils.executeIgnore(() -> supplierMapper.selectById(id));
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        ErpSupplierDeptDistributionRespVO respVO =
                BeanUtils.toBean(supplier, ErpSupplierDeptDistributionRespVO.class);
        List<Long> deptIds = getSupplierDeptMap(Collections.singleton(id)).getOrDefault(id, Collections.emptyList());
        Set<Long> allDeptIds = new LinkedHashSet<>();
        if (supplier.getDeptId() != null) {
            allDeptIds.add(supplier.getDeptId());
        }
        allDeptIds.addAll(deptIds);
        Map<Long, String> deptNameMap = buildDeptNameMap(allDeptIds);
        respVO.setDeptName(deptNameMap.get(supplier.getDeptId()));
        respVO.setDeptIds(deptIds);
        respVO.setDeptNames(buildDeptNames(deptIds, deptNameMap));
        respVO.setDeptNameMap(deptNameMap);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierDeptDistribution(ErpSupplierDeptDistributionSaveReqVO reqVO) {
        ErpSupplierDO existing = validateSupplierExists(reqVO.getId());
        if (CollUtil.isNotEmpty(reqVO.getDeptIds())) {
            deptApi.validateDeptList(reqVO.getDeptIds());
        }
        if (reqVO.getDeptId() != null) {
            deptApi.validateDeptList(Collections.singleton(reqVO.getDeptId()));
        }
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(reqVO.getId());
        updateObj.setDeptId(resolvePrimaryDeptId(reqVO.getDeptId(), reqVO.getDeptIds(), existing.getDeptId()));
        updateObj.setAllowMultiDept(Boolean.TRUE.equals(reqVO.getAllowMultiDept()));
        supplierMapper.updateById(updateObj);
        if (Boolean.TRUE.equals(updateObj.getAllowMultiDept())) {
            syncSupplierDeptList(reqVO.getId(), buildSupplierDeptIds(updateObj.getDeptId(), reqVO.getDeptIds(), true));
        } else {
            supplierDeptMapper.deleteBySupplierId(reqVO.getId());
        }
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, reqVO.getId(), existing,
                supplierMapper.selectById(reqVO.getId()), existing.getCode());
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
        List<ErpSupplierDO> suppliers = supplierMapper.selectByIds(reqVO.getIds());
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
        for (Long supplierId : reqVO.getIds()) {
            ErpSupplierDO supplier = supplierMap.get(supplierId);
            if (supplier == null) {
                continue;
            }
            operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, supplierId, supplier,
                    supplierMapper.selectById(supplierId), supplier.getCode());
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
        operateLogService.recordDelete(ERP_SUPPLIER_TYPE, id, supplier, supplier.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeSupplier(Long sourceId, Long keepId) {
        if (Objects.equals(sourceId, keepId)) {
            throw exception(ARCHIVE_MERGE_SAME_ID);
        }
        ErpSupplierDO source = validateSupplierExists(sourceId);
        ErpSupplierDO keep = validateSupplierExists(keepId);
        if (Boolean.TRUE.equals(source.getMergedFlag())) {
            throw exception(SUPPLIER_MERGED, source.getName());
        }
        if (Boolean.TRUE.equals(keep.getMergedFlag())) {
            throw exception(SUPPLIER_MERGED, keep.getName());
        }
        String operatorId = String.valueOf(getLoginUserId());
        archiveMergeService.mergeSupplierReferences(sourceId, keepId, operatorId);
        supplierMapper.update(null, new LambdaUpdateWrapper<ErpSupplierDO>()
                .eq(ErpSupplierDO::getId, sourceId)
                .ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE)
                .set(ErpSupplierDO::getMergedFlag, true)
                .set(ErpSupplierDO::getMergedTargetId, keepId)
                .set(ErpSupplierDO::getMergedBy, getLoginUserId())
                .set(ErpSupplierDO::getMergedTime, LocalDateTime.now()));
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, sourceId, source,
                supplierMapper.selectById(sourceId), source.getCode());
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
        ErpSupplierDO supplier = DataPermissionUtils.executeIgnore(() -> supplierMapper.selectById(id));
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
    public PageResult<ErpSupplierDO> getSupplierPageByStatus(ErpSupplierPageReqVO pageReqVO, Integer status) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null) {
            return supplierMapper.selectPageByStatus(pageReqVO, status);
        }
        return DataPermissionUtils.executeIgnore(() ->
                supplierMapper.selectVisiblePageByStatus(pageReqVO, status, scope.getDeptIds(),
                        scope.getSelfUserId(), scope.isAll()));
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
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier, supplierMapper.selectById(id), supplier.getCode());
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
            operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier, supplierMapper.selectById(id), supplier.getCode());
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
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier, supplierMapper.selectById(id), supplier.getCode());
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
    public ErpSupplierImportRespVO importSupplierList(List<ErpSupplierImportExcelVO> list) {
        ErpSupplierImportRespVO respVO = new ErpSupplierImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        List<String> codes = listImportCodes(list);
        List<ErpSupplierDO> existedSuppliers = CollUtil.isEmpty(codes)
                ? Collections.emptyList() : supplierMapper.selectListByCodes(codes);
        Map<String, ErpSupplierDO> existedMap = buildExistedSupplierMap(existedSuppliers);
        Map<String, Integer> duplicateCodeCountMap = buildDuplicateCodeCountMap(existedSuppliers);
        DeptResolveContext deptResolveContext = buildImportDeptResolveContext(list);
        for (int i = 0; i < list.size(); i++) {
            ErpSupplierImportExcelVO importVO = list.get(i);
            if (importVO == null || !StringUtils.hasText(trimToNull(importVO.getName()))) {
                continue;
            }
            Integer rowNo = i + 2;
            String code = trimToNull(importVO.getCode());
            try {
                if (StringUtils.hasText(code) && duplicateCodeCountMap.getOrDefault(code, 0) > 1) {
                    throw new IllegalStateException("供应商编码(" + code + ")存在多条供应商资料，请先清理重复编码后再导入");
                }
                ErpSupplierDO existed = StringUtils.hasText(code) ? existedMap.get(code) : null;
                if (existed != null) {
                    updateSupplierFromImport(importVO, existed, deptResolveContext);
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                } else {
                    ErpSupplierDO created = createSupplierFromImport(importVO, deptResolveContext);
                    existedMap.put(created.getCode(), created);
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                }
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpSupplierImportRespVO.FailureItem(
                        rowNo, code, getImportFailureReason(ex)));
            }
        }
        respVO.setSuccessCount(respVO.getCreateCount() + respVO.getUpdateCount());
        respVO.setFailureCount(respVO.getFailureDetails().size());
        operateLogService.record(ERP_SUPPLIER_TYPE, ERP_IMPORT_SUB_TYPE, 0L,
                "导入供应商信息，新增：" + respVO.getCreateCount()
                        + "，更新：" + respVO.getUpdateCount()
                        + "，失败：" + respVO.getFailureCount(),
                "供应商导入");
        return respVO;
    }

    private List<String> listImportCodes(List<ErpSupplierImportExcelVO> list) {
        return list.stream()
                .filter(Objects::nonNull)
                .map(ErpSupplierImportExcelVO::getCode)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, ErpSupplierDO> buildExistedSupplierMap(List<ErpSupplierDO> suppliers) {
        if (CollUtil.isEmpty(suppliers)) {
            return new HashMap<>();
        }
        return suppliers.stream()
                .filter(item -> StringUtils.hasText(item.getCode()))
                .collect(Collectors.toMap(ErpSupplierDO::getCode, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Integer> buildDuplicateCodeCountMap(List<ErpSupplierDO> suppliers) {
        if (CollUtil.isEmpty(suppliers)) {
            return new HashMap<>();
        }
        Map<String, Integer> countMap = new HashMap<>();
        suppliers.stream()
                .map(ErpSupplierDO::getCode)
                .filter(StringUtils::hasText)
                .forEach(code -> countMap.put(code, countMap.getOrDefault(code, 0) + 1));
        return countMap;
    }

    private DeptResolveContext buildImportDeptResolveContext(List<ErpSupplierImportExcelVO> list) {
        Set<String> deptNames = collectImportDeptNames(list);
        if (CollUtil.isEmpty(deptNames)) {
            return new DeptResolveContext();
        }
        List<DeptRespDTO> enabledDeptList = DataPermissionUtils.executeIgnore(
                () -> deptApi.getDeptListByStatus(CommonStatusEnum.ENABLE.getStatus()));
        if (CollUtil.isEmpty(enabledDeptList)) {
            return new DeptResolveContext();
        }
        Map<Long, DeptRespDTO> deptMap = loadDeptWithParents(enabledDeptList.stream()
                .map(DeptRespDTO::getId)
                .collect(Collectors.toList()));
        DeptResolveContext context = new DeptResolveContext();
        enabledDeptList.stream()
                .filter(dept -> dept != null && dept.getId() != null)
                .forEach(dept -> {
                    addDeptResolveItem(context.simpleNameMap, dept.getName(), dept.getId());
                    addDeptResolveItem(context.fullNameMap, buildDeptFullName(dept.getId(), deptMap), dept.getId());
                });
        return context;
    }

    private Set<String> collectImportDeptNames(List<ErpSupplierImportExcelVO> list) {
        Set<String> names = new LinkedHashSet<>();
        list.stream()
                .filter(Objects::nonNull)
                .forEach(item -> {
                    addImportDeptName(names, item.getDeptName());
                    splitImportDeptNames(item.getDeptNames()).forEach(names::add);
                });
        return names;
    }

    private void addImportDeptName(Set<String> names, String name) {
        String key = normalizeImportDeptKey(name);
        if (StringUtils.hasText(key)) {
            names.add(key);
        }
    }

    private void addDeptResolveItem(Map<String, List<Long>> map, String name, Long deptId) {
        String key = normalizeImportDeptKey(name);
        if (!StringUtils.hasText(key) || deptId == null) {
            return;
        }
        List<Long> ids = map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!ids.contains(deptId)) {
            ids.add(deptId);
        }
    }

    private SupplierImportDeptAssignment resolveImportDeptAssignment(ErpSupplierImportExcelVO importVO,
                                                                    DeptResolveContext deptResolveContext) {
        SupplierImportDeptAssignment assignment = new SupplierImportDeptAssignment();
        String deptName = trimToNull(importVO.getDeptName());
        if (StringUtils.hasText(deptName)) {
            assignment.deptNamePresent = true;
            assignment.deptId = resolveImportDeptName(deptName, deptResolveContext);
        }
        List<String> deptNames = splitImportDeptNames(importVO.getDeptNames());
        if (CollUtil.isNotEmpty(deptNames)) {
            assignment.deptNamesPresent = true;
            Set<Long> deptIds = new LinkedHashSet<>();
            for (String item : deptNames) {
                deptIds.add(resolveImportDeptName(item, deptResolveContext));
            }
            assignment.deptIds = new ArrayList<>(deptIds);
        }
        return assignment;
    }

    private Long resolveImportDeptName(String deptName, DeptResolveContext context) {
        String key = normalizeImportDeptKey(deptName);
        if (!StringUtils.hasText(key)) {
            return null;
        }
        boolean fullPath = key.contains("/");
        List<Long> ids = fullPath ? context.fullNameMap.get(key) : context.simpleNameMap.get(key);
        if (CollUtil.isEmpty(ids)) {
            throw new IllegalStateException("部门(" + deptName + ")不存在或已停用");
        }
        if (ids.size() > 1) {
            if (fullPath) {
                throw new IllegalStateException("部门完整路径(" + deptName + ")存在多个，请联系管理员清理部门");
            }
            throw new IllegalStateException("部门名称(" + deptName + ")存在多个，请填写完整路径，例如：总公司 / 采购部");
        }
        return ids.get(0);
    }

    private List<String> splitImportDeptNames(String deptNames) {
        String text = trimToNull(deptNames);
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        return Arrays.stream(IMPORT_DEPT_SEPARATOR.split(text))
                .map(this::normalizeImportDeptKey)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeImportDeptKey(String value) {
        String text = trimToNull(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.replace('／', '/').replace('\\', '/');
        if (normalized.contains("/")) {
            return Arrays.stream(normalized.split("/"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(" / "));
        }
        return normalized.replaceAll("\\s+", " ");
    }

    private ErpSupplierDO createSupplierFromImport(ErpSupplierImportExcelVO importVO,
                                                  DeptResolveContext deptResolveContext) {
        SupplierImportDeptAssignment deptAssignment = resolveImportDeptAssignment(importVO, deptResolveContext);
        ErpSupplierDO supplier = BeanUtils.toBean(importVO, ErpSupplierDO.class);
        supplier.setName(trimToNull(supplier.getName()));
        validateSupplierNameUnique(null, supplier.getName());
        supplier.setCode(normalizeCode(supplier.getCode()));
        if (deptAssignment.hasDeptName()) {
            supplier.setDeptId(deptAssignment.getDeptId());
        }
        if (deptAssignment.hasDeptNames()) {
            supplier.setAllowMultiDept(true);
        }
        if (!StringUtils.hasText(supplier.getCode())) {
            supplier.setCode(generateSupplierCode());
        }
        if (supplier.getStatus() == null) {
            supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (supplier.getSort() == null) {
            supplier.setSort(0);
        }
        fillSupplierImportMnemonicCodes(supplier);
        purchaseDocumentDefaultService.fillCreateDefaults(supplier);
        Long loginUserDeptId = getLoginUserDeptId();
        if (supplier.getDeptId() == null) {
            supplier.setDeptId(deptAssignment.firstDeptId(loginUserDeptId));
        }
        supplier.setCreateDeptId(loginUserDeptId != null ? loginUserDeptId : supplier.getDeptId());
        applySupplierCreateDefaults(supplier);
        validateSupplierCodeUnique(null, supplier.getCode());
        supplierMapper.insert(supplier);
        syncSupplierDeptList(supplier.getId(), buildSupplierDeptIds(supplier.getDeptId(),
                deptAssignment.hasDeptNames() ? deptAssignment.getDeptIds() : null, supplier.getAllowMultiDept()));
        operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier, supplier.getCode());
        return supplier;
    }

    private void updateSupplierFromImport(ErpSupplierImportExcelVO importVO, ErpSupplierDO existing,
                                          DeptResolveContext deptResolveContext) {
        SupplierImportDeptAssignment deptAssignment = resolveImportDeptAssignment(importVO, deptResolveContext);
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(existing.getId());
        updateObj.setCode(existing.getCode());
        updateObj.setName(trimToNull(importVO.getName()));
        validateSupplierNameUnique(existing.getId(), updateObj.getName());
        setIfHasText(updateObj::setShortName, importVO.getShortName());
        if (deptAssignment.hasDeptName()) {
            updateObj.setDeptId(deptAssignment.getDeptId());
        }
        if (deptAssignment.hasDeptNames()) {
            updateObj.setAllowMultiDept(true);
        } else if (importVO.getAllowMultiDept() != null) {
            updateObj.setAllowMultiDept(importVO.getAllowMultiDept());
        }
        setIfHasText(updateObj::setContact, importVO.getContact());
        setIfHasText(updateObj::setMobile, importVO.getMobile());
        setIfHasText(updateObj::setTelephone, importVO.getTelephone());
        setIfHasText(updateObj::setEmail, importVO.getEmail());
        setIfHasText(updateObj::setRegion, importVO.getRegion());
        setIfHasText(updateObj::setCategory, importVO.getCategory());
        validateSupplierCategory(updateObj.getCategory());
        setIfHasText(updateObj::setPurchaser, importVO.getPurchaser());
        setIfHasText(updateObj::setSettleMethod, importVO.getSettleMethod());
        setIfHasText(updateObj::setTransportMethod, importVO.getTransportMethod());
        setIfHasText(updateObj::setFreightType, importVO.getFreightType());
        setIfHasText(updateObj::setLogisticsCompany, importVO.getLogisticsCompany());
        setIfHasText(updateObj::setInvoiceType, importVO.getInvoiceType());
        setIfHasText(updateObj::setTaxpayerId, importVO.getTaxpayerId());
        setIfHasText(updateObj::setInvoiceBank, importVO.getInvoiceBank());
        setIfHasText(updateObj::setInvoiceBankAccount, importVO.getInvoiceBankAccount());
        setIfHasText(updateObj::setInvoiceAddress, importVO.getInvoiceAddress());
        setIfHasText(updateObj::setInvoicePhone, importVO.getInvoicePhone());
        setIfHasText(updateObj::setInvoiceCompany, importVO.getInvoiceCompany());
        setIfHasText(updateObj::setAccount, importVO.getAccount());
        setIfHasText(updateObj::setBankName, importVO.getBankName());
        setIfHasText(updateObj::setBankAccount, importVO.getBankAccount());
        setIfHasText(updateObj::setBankAddress, importVO.getBankAddress());
        setIfHasText(updateObj::setTaxNo, importVO.getTaxNo());
        if (importVO.getTaxPercent() != null) {
            updateObj.setTaxPercent(importVO.getTaxPercent());
        }
        setIfHasText(updateObj::setFinancePhone, importVO.getFinancePhone());
        setIfHasText(updateObj::setMemberCode, importVO.getMemberCode());
        setIfHasText(updateObj::setLegalPerson, importVO.getLegalPerson());
        setIfHasText(updateObj::setCreditCode, importVO.getCreditCode());
        setIfHasText(updateObj::setRemark, importVO.getRemark());
        if (importVO.getSort() != null) {
            updateObj.setSort(importVO.getSort());
        }
        if (importVO.getStatus() != null) {
            updateObj.setStatus(importVO.getStatus());
        }
        fillSupplierImportMnemonicCodes(updateObj, existing);
        supplierMapper.updateById(updateObj);
        if (deptAssignment.hasDeptName() || deptAssignment.hasDeptNames() || importVO.getAllowMultiDept() != null) {
            Boolean effectiveAllowMultiDept = updateObj.getAllowMultiDept() != null
                    ? updateObj.getAllowMultiDept() : existing.getAllowMultiDept();
            Long effectiveDeptId = updateObj.getDeptId() != null ? updateObj.getDeptId() : existing.getDeptId();
            List<Long> effectiveDeptIds = deptAssignment.hasDeptNames()
                    ? deptAssignment.getDeptIds() : getExistingSupplierDeptIds(existing.getId());
            syncSupplierDeptList(existing.getId(), buildSupplierDeptIds(effectiveDeptId, effectiveDeptIds,
                    effectiveAllowMultiDept));
        }
        ErpSupplierDO newest = supplierMapper.selectById(existing.getId());
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, existing.getId(), existing,
                newest != null ? newest : updateObj, existing.getCode());
    }

    private List<Long> getExistingSupplierDeptIds(Long supplierId) {
        List<ErpSupplierDeptDO> deptList = supplierDeptMapper.selectListBySupplierId(supplierId);
        if (CollUtil.isEmpty(deptList)) {
            return Collections.emptyList();
        }
        return deptList.stream()
                .map(ErpSupplierDeptDO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
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
    public Map<Long, List<Long>> getSupplierDeptMap(Collection<Long> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return Collections.emptyMap();
        }
        return supplierDeptMapper.selectListBySupplierIds(supplierIds).stream()
                .collect(Collectors.groupingBy(ErpSupplierDeptDO::getSupplierId, Collectors.mapping(
                        ErpSupplierDeptDO::getDeptId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().distinct().collect(Collectors.toList())))));
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

    private void validateSupplierNameUnique(Long id, String name) {
        String normalizedName = trimToNull(name);
        if (!StringUtils.hasText(normalizedName)) {
            return;
        }
        ErpSupplierDO supplier = supplierMapper.selectByNameExcludeId(normalizedName, id);
        if (supplier != null) {
            throw exception(SUPPLIER_NAME_DUPLICATE, normalizedName);
        }
    }

    private String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void fillSupplierImportMnemonicCodes(ErpSupplierDO supplier) {
        String name = trimToNull(supplier.getName());
        if (!StringUtils.hasText(name)) {
            return;
        }
        if (!StringUtils.hasText(supplier.getPinyinCode())) {
            supplier.setPinyinCode(ErpMnemonicCodeUtils.buildPinyinCode(name));
        }
        if (!StringUtils.hasText(supplier.getWubiCode())) {
            supplier.setWubiCode(ErpMnemonicCodeUtils.buildWubiCode(name));
        }
    }

    private void fillSupplierImportMnemonicCodes(ErpSupplierDO updateObj, ErpSupplierDO existing) {
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

    private static class DeptResolveContext {

        private final Map<String, List<Long>> simpleNameMap = new HashMap<>();
        private final Map<String, List<Long>> fullNameMap = new HashMap<>();

    }

    private static class SupplierImportDeptAssignment {

        private boolean deptNamePresent;
        private Long deptId;
        private boolean deptNamesPresent;
        private List<Long> deptIds = Collections.emptyList();

        private boolean hasDeptName() {
            return deptNamePresent;
        }

        private Long getDeptId() {
            return deptId;
        }

        private boolean hasDeptNames() {
            return deptNamesPresent;
        }

        private List<Long> getDeptIds() {
            return deptIds;
        }

        private Long firstDeptId(Long fallbackDeptId) {
            if (deptId != null) {
                return deptId;
            }
            if (CollUtil.isNotEmpty(deptIds)) {
                return deptIds.get(0);
            }
            return fallbackDeptId;
        }

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
