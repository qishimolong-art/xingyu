package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseBranchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;

/**
 * ERP 仓库 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpWarehouseServiceImpl implements ErpWarehouseService {

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";

    @Resource
    private ErpWarehouseMapper warehouseMapper;

    @Resource
    private ErpWarehouseBranchMapper warehouseBranchMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(ErpWarehouseSaveReqVO createReqVO) {
        // 插入仓库
        ErpWarehouseDO warehouse = BeanUtils.toBean(createReqVO, ErpWarehouseDO.class);
        warehouseMapper.insert(warehouse);
        // 插入分店关联
        createWarehouseBranches(warehouse.getId(), createReqVO.getBranchTenantIds());
        // 返回
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(ErpWarehouseSaveReqVO updateReqVO) {
        // 校验存在
        ErpWarehouseDO warehouse = validateWarehouseExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, warehouse);
        // 更新仓库：只覆盖本次表单提交的字段，避免把历史扩展字段清空
        ErpWarehouseDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseDO.class);
        updateObj.setId(warehouse.getId());
        updateObj.setAddress(warehouse.getAddress());
        updateObj.setPrincipal(warehouse.getPrincipal());
        updateObj.setWarehousePrice(warehouse.getWarehousePrice());
        updateObj.setTruckagePrice(warehouse.getTruckagePrice());
        updateObj.setDefaultStatus(warehouse.getDefaultStatus());
        updateObj.setStorageCenterId(warehouse.getStorageCenterId());
        updateObj.setStorageWarehouseId(warehouse.getStorageWarehouseId());
        updateObj.setEcommerceEnabled(warehouse.getEcommerceEnabled());
        updateObj.setSaleBillControl(warehouse.getSaleBillControl());
        updateObj.setZeroStockHide(warehouse.getZeroStockHide());
        updateObj.setGoodsToBranch(warehouse.getGoodsToBranch());
        updateObj.setDept(warehouse.getDept());
        updateObj.setWarehouseLocation(warehouse.getWarehouseLocation());
        updateObj.setOutPacking(warehouse.getOutPacking());
        updateObj.setAutoOrder(warehouse.getAutoOrder());
        updateObj.setMaxPickCount(warehouse.getMaxPickCount());
        updateObj.setStockGroupType(warehouse.getStockGroupType());
        updateObj.setCreditControl(warehouse.getCreditControl());
        updateObj.setRegionId(warehouse.getRegionId());
        warehouseMapper.updateById(updateObj);
        // 精简表单不再提交分店时，保留原有分店关联
        if (updateReqVO.getBranchTenantIds() != null) {
            warehouseBranchMapper.deleteByWarehouseId(updateReqVO.getId());
            createWarehouseBranches(updateReqVO.getId(), updateReqVO.getBranchTenantIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseDefaultStatus(Long id, Boolean defaultStatus) {
        // 1. 校验存在
        validateWarehouseExists(id);

        // 2.1 如果开启，则需要关闭所有其它的默认
        if (defaultStatus) {
            ErpWarehouseDO warehouse = warehouseMapper.selectByDefaultStatus();
            if (warehouse != null) {
                warehouseMapper.updateById(new ErpWarehouseDO().setId(warehouse.getId()).setDefaultStatus(false));
            }
        }
        // 2.2 更新对应的默认状态
        warehouseMapper.updateById(new ErpWarehouseDO().setId(id).setDefaultStatus(defaultStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long id) {
        // 校验存在
        validateWarehouseExists(id);
        // 删除仓库
        warehouseMapper.deleteById(id);
        // 删除分店关联
        warehouseBranchMapper.deleteByWarehouseId(id);
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
    public PageResult<ErpWarehouseDO> getWarehousePage(ErpWarehousePageReqVO pageReqVO) {
        return warehouseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<Long> getWarehouseBranchTenantIds(Long warehouseId) {
        List<ErpWarehouseBranchDO> branches = warehouseBranchMapper.selectListByWarehouseId(warehouseId);
        return convertList(branches, ErpWarehouseBranchDO::getBranchTenantId);
    }

    /**
     * 批量创建仓库分店关联
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
