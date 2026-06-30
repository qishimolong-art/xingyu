package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 仓库 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpWarehouseService {

    /**
     * 创建仓库
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWarehouse(@Valid ErpWarehouseSaveReqVO createReqVO);

    /**
     * 更新ERP 仓库
     *
     * @param updateReqVO 更新信息
     */
    void updateWarehouse(@Valid ErpWarehouseSaveReqVO updateReqVO);

    /**
     * 批量修改仓库
     *
     * @param updateReqVO 批量修改信息
     */
    void batchUpdateWarehouse(@Valid ErpWarehouseBatchUpdateReqVO updateReqVO);

    /**
     * 批量停用仓库
     *
     * @param ids 仓库编号列表
     */
    void batchDisableWarehouse(List<Long> ids);

    /**
     * 还原停用仓库
     *
     * @param ids 仓库编号列表
     */
    void restoreWarehouse(List<Long> ids);

    /**
     * 更新仓库默认状态
     *
     * @param id     编号
     * @param defaultStatus 默认状态
     */
    void updateWarehouseDefaultStatus(Long id, Boolean defaultStatus);

    /**
     * 删除仓库
     *
     * @param id 编号
     */
    void deleteWarehouse(Long id);

    /**
     * 批量删除仓库
     *
     * @param ids 编号数组
     */
    void deleteWarehouseList(List<Long> ids);

    /**
     * 导入仓库列表
     *
     * @param list 导入数据
     * @return 导入结果
     */
    ErpWarehouseImportRespVO importWarehouseList(List<ErpWarehouseImportExcelVO> list);

    /**
     * 获得仓库
     *
     * @param id 编号
     * @return 仓库
     */
    ErpWarehouseDO getWarehouse(Long id);

    /**
     * Gets warehouse visible to current login user.
     *
     * @param id warehouse id
     * @return warehouse, or null if not visible
     */
    ErpWarehouseDO getCurrentUserVisibleWarehouse(Long id);

    /**
     * 校验仓库列表的有效性
     *
     * @param ids 编号数组
     * @return 仓库列表
     */
    List<ErpWarehouseDO> validWarehouseList(Collection<Long> ids);

    /**
     * Validates purchase warehouses.
     *
     * @param ids warehouse ids
     * @return warehouse list
     */
    List<ErpWarehouseDO> validPurchaseWarehouseList(Collection<Long> ids);

    /**
     * Validates sale warehouses.
     *
     * @param ids warehouse ids
     * @return warehouse list
     */
    List<ErpWarehouseDO> validSaleWarehouseList(Collection<Long> ids);

    /**
     * 获得指定状态的仓库列表
     *
     * @param status 状态
     * @return 仓库列表
     */
    List<ErpWarehouseDO> getWarehouseListByStatus(Integer status);

    /**
     * Gets purchase enabled warehouses by status.
     *
     * @param status status
     * @return warehouse list
     */
    List<ErpWarehouseDO> getPurchaseWarehouseListByStatus(Integer status);

    /**
     * Gets sale enabled warehouses by status.
     *
     * @param status status
     * @return warehouse list
     */
    List<ErpWarehouseDO> getSaleWarehouseListByStatus(Integer status);

    /**
     * Gets enabled warehouses assignable by permission configuration.
     *
     * @return warehouse list
     */
    List<ErpWarehouseDO> getAssignableWarehouseList();

    /**
     * Gets enabled warehouse list authorized to current login user.
     *
     * @return warehouse list
     */
    List<ErpWarehouseDO> getCurrentUserAuthorizedWarehouseList();

    /**
     * Gets enabled purchase warehouse list authorized to current login user.
     *
     * @return warehouse list
     */
    List<ErpWarehouseDO> getCurrentUserAuthorizedPurchaseWarehouseList();

    /**
     * Gets enabled sale warehouse list authorized to current login user.
     *
     * @return warehouse list
     */
    List<ErpWarehouseDO> getCurrentUserAuthorizedSaleWarehouseList();

    /**
     * Gets authorized warehouse ids by user id.
     *
     * @param userId user id
     * @return warehouse ids
     */
    List<Long> getUserWarehouseIds(Long userId);

    /**
     * Replaces user warehouse permissions.
     *
     * @param userId user id
     * @param warehouseIds warehouse ids
     */
    void updateUserWarehousePermissions(Long userId, Collection<Long> warehouseIds);

    /**
     * Gets authorized user ids by warehouse id.
     *
     * @param warehouseId warehouse id
     * @return user ids
     */
    List<Long> getWarehouseUserIds(Long warehouseId);

    /**
     * Replaces warehouse user permissions.
     *
     * @param warehouseId warehouse id
     * @param userIds user ids
     */
    void updateWarehouseUserPermissions(Long warehouseId, Collection<Long> userIds);

    /**
     * Validates current login user can access all warehouses.
     *
     * @param warehouseIds warehouse ids
     */
    void validateCurrentUserWarehousePermission(Collection<Long> warehouseIds);

    /**
     * Whether current login user has all warehouse access.
     *
     * @return true if all warehouse access
     */
    boolean hasCurrentUserAllWarehousePermission();

    /**
     * Gets authorized warehouse ids for current user.
     *
     * @return warehouse ids
     */
    Set<Long> getCurrentUserAuthorizedWarehouseIds();

    /**
     * 获得仓库列表
     *
     * @param ids 编号数组
     * @return 仓库列表
     */
    List<ErpWarehouseDO> getWarehouseList(Collection<Long> ids);

    /**
     * 鑾峰緱閮ㄩ棬涓嬬殑浠撳簱鍒楄〃
     *
     * @param deptId 閮ㄩ棬缂栧彿
     * @return 浠撳簱鍒楄〃
     */
    List<ErpWarehouseDO> getWarehouseListByDeptId(Long deptId);

    /**
     * 获得仓库 Map
     *
     * @param ids 编号数组
     * @return 仓库 Map
     */
    default Map<Long, ErpWarehouseDO> getWarehouseMap(Collection<Long> ids) {
        return convertMap(getWarehouseList(ids), ErpWarehouseDO::getId);
    }

    /**
     * 获得仓库分页
     *
     * @param pageReqVO 分页查询
     * @return 仓库分页
     */
    PageResult<ErpWarehouseDO> getWarehousePage(ErpWarehousePageReqVO pageReqVO);

    /**
     * 获得仓库关联的分店租户ID列表
     *
     * @param warehouseId 仓库编号
     * @return 分店租户ID列表
     */
    List<Long> getWarehouseBranchTenantIds(Long warehouseId);

}
