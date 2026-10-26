package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/** 仓库列表和导出共用的响应组装服务。 */
@Service
public class ErpWarehouseQueryService {

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    public List<ErpWarehouseRespVO> buildWarehouseVOList(List<ErpWarehouseDO> list) {
        return buildWarehouseVOList(list, false);
    }

    public List<ErpWarehouseRespVO> buildWarehouseVOList(List<ErpWarehouseDO> list,
                                                         boolean markReadonlyBySaleDistribution) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, ErpWarehouseDO::getDeptId));
        Set<Long> storageWarehouseIds = list.stream()
                .map(ErpWarehouseDO::getStorageWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ErpWarehouseDO> storageWarehouseMap = convertMap(
                warehouseService.getWarehouseList(storageWarehouseIds), ErpWarehouseDO::getId);
        Set<Long> userIds = new HashSet<>();
        list.forEach(warehouse -> {
            addUserId(userIds, warehouse.getCreator());
            addUserId(userIds, warehouse.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> readonlyWarehouseIds = markReadonlyBySaleDistribution
                ? warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds() : Collections.emptySet();
        return BeanUtils.toBean(list, ErpWarehouseRespVO.class, vo -> {
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            MapUtils.findAndThen(storageWarehouseMap, vo.getStorageWarehouseId(),
                    warehouse -> vo.setStorageWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
            vo.setReadonlyBySaleDistribution(readonlyWarehouseIds.contains(vo.getId()));
        });
    }

    private void fillUserNames(ErpWarehouseRespVO warehouse, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(warehouse.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> warehouse.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(warehouse.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> warehouse.setUpdaterName(user.getNickname()));
        }
    }

    private void addUserId(Collection<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
