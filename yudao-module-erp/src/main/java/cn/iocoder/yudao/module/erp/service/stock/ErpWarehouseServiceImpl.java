package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseBranchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WAREHOUSE_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;

/**
 * ERP 濞寸姵鎸哥花?Service 閻庡湱鍋熼獮鍥╃尵? *
 * @author 闁煎搫顑夋禍鎯р攦閹邦喚鍨?
 */
@Service
@Validated
public class ErpWarehouseServiceImpl implements ErpWarehouseService {

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";

    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Resource
    private ErpWarehouseBranchMapper warehouseBranchMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(ErpWarehouseSaveReqVO createReqVO) {
        // 闁圭粯甯掗崣鍡樼閹惧磭姘?
        ErpWarehouseDO warehouse = BeanUtils.toBean(createReqVO, ErpWarehouseDO.class);
        if (warehouse.getSort() == null) {
            warehouse.setSort(0L);
        }
        warehouseMapper.insert(warehouse);
        // 闁圭粯甯掗崣鍡涘礆閸℃鏆楅柛蹇撶枃娴?
        createWarehouseBranches(warehouse.getId(), createReqVO.getBranchTenantIds());
        operateLogService.recordCreate(ERP_WAREHOUSE_TYPE, warehouse.getId(), warehouse.getName());
        // 閺夆晜鏌ㄥú?
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(ErpWarehouseSaveReqVO updateReqVO) {
        ErpWarehouseDO warehouse = validateWarehouseExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, warehouse);

        ErpWarehouseDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseDO.class);
        updateObj.setId(warehouse.getId());
        updateObj.setAddress(warehouse.getAddress());
        updateObj.setPrincipal(warehouse.getPrincipal());
        updateObj.setWarehousePrice(warehouse.getWarehousePrice());
        updateObj.setTruckagePrice(warehouse.getTruckagePrice());
        updateObj.setDefaultStatus(warehouse.getDefaultStatus());
        updateObj.setStorageCenterId(warehouse.getStorageCenterId());
        updateObj.setEcommerceEnabled(warehouse.getEcommerceEnabled());
        updateObj.setSaleBillControl(warehouse.getSaleBillControl());
        updateObj.setZeroStockHide(warehouse.getZeroStockHide());
        updateObj.setGoodsToBranch(warehouse.getGoodsToBranch());
        updateObj.setDept(warehouse.getDept());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(warehouse.getDeptId());
        }
        updateObj.setWarehouseLocation(warehouse.getWarehouseLocation());
        updateObj.setOutPacking(warehouse.getOutPacking());
        updateObj.setAutoOrder(warehouse.getAutoOrder());
        updateObj.setMaxPickCount(warehouse.getMaxPickCount());
        updateObj.setStockGroupType(warehouse.getStockGroupType());
        updateObj.setCreditControl(warehouse.getCreditControl());
        updateObj.setRegionId(warehouse.getRegionId());
        warehouseMapper.updateById(updateObj);
        if (!Objects.equals(updateObj.getDeptId(), warehouse.getDeptId())) {
            stockMapper.updateDeptIdByWarehouseId(warehouse.getId(), updateObj.getDeptId());
        }

        if (updateReqVO.getBranchTenantIds() != null) {
            warehouseBranchMapper.deleteByWarehouseId(updateReqVO.getId());
            createWarehouseBranches(updateReqVO.getId(), updateReqVO.getBranchTenantIds());
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_TYPE, updateObj.getId(), updateObj.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateWarehouse(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        if (!hasBatchUpdateFields(updateReqVO)) {
            return;
        }
        for (Long id : updateReqVO.getIds()) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            ErpWarehouseDO updateObj = buildBatchUpdateObj(updateReqVO);
            updateObj.setId(id);
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateObj, warehouse);
            warehouseMapper.updateById(updateObj);
            if (updateReqVO.getDeptId() != null && !Objects.equals(updateReqVO.getDeptId(), warehouse.getDeptId())) {
                stockMapper.updateDeptIdByWarehouseId(id, updateReqVO.getDeptId());
            }
            operateLogService.recordUpdate(ERP_WAREHOUSE_TYPE, id, warehouse.getName());
        }
    }

    private boolean hasBatchUpdateFields(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        return updateReqVO.getDeptId() != null
                || updateReqVO.getWarehouseType() != null
                || updateReqVO.getStatus() != null
                || updateReqVO.getSaleEnabled() != null
                || updateReqVO.getPurchaseEnabled() != null
                || updateReqVO.getStockBillEnabled() != null
                || updateReqVO.getScanControl() != null
                || updateReqVO.getSplitOrder() != null
                || updateReqVO.getSort() != null
                || updateReqVO.getRemark() != null;
    }

    private ErpWarehouseDO buildBatchUpdateObj(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        ErpWarehouseDO updateObj = new ErpWarehouseDO();
        if (updateReqVO.getDeptId() != null) {
            updateObj.setDeptId(updateReqVO.getDeptId());
        }
        if (updateReqVO.getWarehouseType() != null) {
            updateObj.setWarehouseType(updateReqVO.getWarehouseType());
        }
        if (updateReqVO.getStatus() != null) {
            updateObj.setStatus(updateReqVO.getStatus());
        }
        if (updateReqVO.getSaleEnabled() != null) {
            updateObj.setSaleEnabled(updateReqVO.getSaleEnabled());
        }
        if (updateReqVO.getPurchaseEnabled() != null) {
            updateObj.setPurchaseEnabled(updateReqVO.getPurchaseEnabled());
        }
        if (updateReqVO.getStockBillEnabled() != null) {
            updateObj.setStockBillEnabled(updateReqVO.getStockBillEnabled());
        }
        if (updateReqVO.getScanControl() != null) {
            updateObj.setScanControl(updateReqVO.getScanControl());
        }
        if (updateReqVO.getSplitOrder() != null) {
            updateObj.setSplitOrder(updateReqVO.getSplitOrder());
        }
        if (updateReqVO.getSort() != null) {
            updateObj.setSort(updateReqVO.getSort());
        }
        if (updateReqVO.getRemark() != null) {
            updateObj.setRemark(updateReqVO.getRemark());
        }
        return updateObj;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseDefaultStatus(Long id, Boolean defaultStatus) {
        // 1. 闁哄稄绻濋悰娆戔偓娑櫭﹢?
        validateWarehouseExists(id);

        // 2.1 濠碘€冲€归悘澶婎嚕閳ь剟宕ラ銈囩闁告帗鐟╁〒鍓佹啺娴ｇ褰犻梻鍌ゅ幗婢у秹寮垫径濠傚緭閻庣懓鍟板▓鎴烆渶濡鍚?
        if (defaultStatus) {
            ErpWarehouseDO warehouse = warehouseMapper.selectByDefaultStatus();
            if (warehouse != null) {
                warehouseMapper.updateById(new ErpWarehouseDO().setId(warehouse.getId()).setDefaultStatus(false));
            }
        }
        // 2.2 闁哄洤鐡ㄩ弻濠勨偓鐢垫嚀缁ㄦ煡鎯冮崟顖滃笡閻犱降鍊楁慨鎼佸箑?
        warehouseMapper.updateById(new ErpWarehouseDO().setId(id).setDefaultStatus(defaultStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long id) {
        // 闁哄稄绻濋悰娆戔偓娑櫭﹢?
        ErpWarehouseDO warehouse = validateWarehouseExists(id);
        baseArchiveReferenceService.validateWarehouseNotReferenced(id);
        // 闁告帞濞€濞呭孩绂掗幘宕囨皑
        warehouseMapper.deleteById(id);
        // 闁告帞濞€濞呭酣宕氶崱妤冩殫闁稿繐鐤囨禒?
        warehouseBranchMapper.deleteByWarehouseId(id);
        operateLogService.record(ERP_WAREHOUSE_TYPE, ERP_DELETE_SUB_TYPE, id,
                "delete warehouse: " + warehouse.getName(), warehouse.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteWarehouse(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpWarehouseImportRespVO importWarehouseList(List<ErpWarehouseImportExcelVO> list) {
        ErpWarehouseImportRespVO respVO = new ErpWarehouseImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Map<String, List<DeptRespDTO>> deptNameMap = buildDeptNameMap(list);
        for (int i = 0; i < list.size(); i++) {
            ErpWarehouseImportExcelVO row = list.get(i);
            Integer rowNo = i + 2;
            try {
                resolveImportDeptId(row, deptNameMap);
                boolean create = findImportWarehouse(row) == null;
                importWarehouse(row);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
                if (create) {
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                } else {
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                }
            } catch (Exception ex) {
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                respVO.getFailureDetails().add(new ErpWarehouseImportRespVO.FailureItem(
                        rowNo, row == null ? null : row.getWarehouseCode(), ex.getMessage()));
            }
        }
        return respVO;
    }

    private Map<String, List<DeptRespDTO>> buildDeptNameMap(List<ErpWarehouseImportExcelVO> list) {
        Set<String> deptNames = list.stream()
                .filter(row -> row != null && StrUtil.isNotBlank(row.getDeptName()))
                .map(row -> row.getDeptName().trim())
                .collect(Collectors.toCollection(HashSet::new));
        if (CollUtil.isEmpty(deptNames)) {
            return Collections.emptyMap();
        }
        return deptNames.stream().collect(Collectors.toMap(deptName -> deptName,
                deptName -> deptApi.getDeptListByName(deptName)));
    }

    private void resolveImportDeptId(ErpWarehouseImportExcelVO row, Map<String, List<DeptRespDTO>> deptNameMap) {
        if (row == null || StrUtil.isBlank(row.getDeptName())) {
            return;
        }
        String deptName = row.getDeptName().trim();
        List<DeptRespDTO> matchedDepts = deptNameMap.get(deptName);
        if (CollUtil.isEmpty(matchedDepts)) {
            throw new IllegalArgumentException("所属部门不存在：" + deptName);
        }
        if (matchedDepts.size() > 1) {
            throw new IllegalArgumentException("所属部门名称重复，请使用唯一部门名称：" + deptName);
        }
        row.setDeptName(deptName);
        row.setDeptId(matchedDepts.get(0).getId());
    }

    private void importWarehouse(ErpWarehouseImportExcelVO row) {
        if (row == null || StrUtil.isBlank(row.getName())) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        ErpWarehouseDO existing = findImportWarehouse(row);
        ErpWarehouseDO importObj = BeanUtils.toBean(row, ErpWarehouseDO.class);
        if (existing == null) {
            if (importObj.getStatus() == null) {
                importObj.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (importObj.getSort() == null) {
                importObj.setSort(0L);
            }
            warehouseMapper.insert(importObj);
            operateLogService.recordCreate(ERP_WAREHOUSE_TYPE, importObj.getId(), importObj.getName());
            return;
        }
        importObj.setId(existing.getId());
        importObj.setDefaultStatus(existing.getDefaultStatus());
        warehouseMapper.updateById(importObj);
        if (importObj.getDeptId() != null && !Objects.equals(importObj.getDeptId(), existing.getDeptId())) {
            stockMapper.updateDeptIdByWarehouseId(existing.getId(), importObj.getDeptId());
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_TYPE, existing.getId(), importObj.getName());
    }

    private ErpWarehouseDO findImportWarehouse(ErpWarehouseImportExcelVO row) {
        if (row == null) {
            return null;
        }
        if (StrUtil.isNotBlank(row.getWarehouseCode())) {
            ErpWarehouseDO warehouse = warehouseMapper.selectByWarehouseCode(row.getWarehouseCode());
            if (warehouse != null) {
                return warehouse;
            }
        }
        return StrUtil.isBlank(row.getName()) ? null : warehouseMapper.selectByName(row.getName());
    }

    private ErpWarehouseDO validateWarehouseExists(Long id) {
        ErpWarehouseDO warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        return warehouse;
    }

    @Override
    public ErpWarehouseDO getWarehouse(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public List<ErpWarehouseDO> validWarehouseList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpWarehouseDO> list = warehouseMapper.selectByIds(ids);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(list, ErpWarehouseDO::getId);
        for (Long id : ids) {
            ErpWarehouseDO warehouse = warehouseMap.get(id);
            if (warehouseMap.get(id) == null) {
                throw exception(WAREHOUSE_NOT_EXISTS);
            }
            if (CommonStatusEnum.isDisable(warehouse.getStatus())) {
                throw exception(WAREHOUSE_NOT_ENABLE, warehouse.getName());
            }
        }
        return list;
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseListByStatus(Integer status) {
        return warehouseMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectByIds(ids);
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseListByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectListByDeptId(deptId);
    }

    @Override
    public PageResult<ErpWarehouseDO> getWarehousePage(ErpWarehousePageReqVO pageReqVO) {
        return warehouseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<Long> getWarehouseBranchTenantIds(Long warehouseId) {
        List<ErpWarehouseBranchDO> branches = warehouseBranchMapper.selectListByWarehouseId(warehouseId);
        return convertList(branches, ErpWarehouseBranchDO::getBranchTenantId);
    }

    /**
     * 闁归潧缍婇崳娲礆濞戞绱﹀ù鐘虫尭缁ㄩ亶宕氶崱妤冩殫闁稿繐鐤囨禒?
     */
    private void createWarehouseBranches(Long warehouseId, List<Long> branchTenantIds) {
        if (CollUtil.isEmpty(branchTenantIds)) {
            return;
        }
        List<ErpWarehouseBranchDO> branches = convertList(branchTenantIds, tenantId ->
                new ErpWarehouseBranchDO().setWarehouseId(warehouseId).setBranchTenantId(tenantId));
        warehouseBranchMapper.insertBatch(branches);
    }

}

