package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/** 供应商列表与导出共用的响应组装，避免异步导出口径偏离列表。 */
@Service
public class ErpSupplierQueryService {

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    public List<ErpSupplierRespVO> buildSupplierRespList(List<ErpSupplierDO> list) {
        List<ErpSupplierRespVO> response = BeanUtils.toBean(list, ErpSupplierRespVO.class);
        fillSupplierExtra(response);
        return response;
    }

    public void fillSupplierExtra(List<ErpSupplierRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, List<Long>> supplierDeptMap = supplierService.getSupplierDeptMap(
                convertSet(list, ErpSupplierRespVO::getId));
        Set<Long> deptIds = new HashSet<>(convertSet(list, ErpSupplierRespVO::getDeptId));
        supplierDeptMap.values().forEach(ids -> {
            if (ids != null) {
                deptIds.addAll(ids);
            }
        });
        Set<Long> userIds = new HashSet<>();
        list.forEach(supplier -> {
            addUserId(userIds, supplier.getCreator());
            addUserId(userIds, supplier.getPurchaser());
            addUserId(userIds, supplier.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        list.forEach(supplier -> {
            Long creatorId = parseUserId(supplier.getCreator());
            AdminUserRespDTO creator = creatorId == null ? null : userMap.get(creatorId);
            if (supplier.getCreateDeptId() == null && creator != null) {
                supplier.setCreateDeptId(creator.getDeptId());
            }
            if (supplier.getCreateDeptId() != null) {
                deptIds.add(supplier.getCreateDeptId());
            }
        });
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = DataPermissionUtils.executeIgnore(() -> deptApi.getDeptMap(deptIds));
        list.forEach(supplier -> {
            List<Long> currentDeptIds = supplierDeptMap.getOrDefault(supplier.getId(), Collections.emptyList());
            supplier.setDeptIds(currentDeptIds);
            supplier.setDeptNames(currentDeptIds.stream()
                    .map(deptMap::get)
                    .filter(dept -> dept != null && dept.getName() != null)
                    .map(DeptRespDTO::getName)
                    .collect(Collectors.joining("、")));
            MapUtils.findAndThen(deptMap, supplier.getDeptId(), dept -> supplier.setDeptName(dept.getName()));
            MapUtils.findAndThen(deptMap, supplier.getCreateDeptId(), dept -> supplier.setCreateDeptName(dept.getName()));
            Long creatorId = parseUserId(supplier.getCreator());
            if (creatorId != null) {
                MapUtils.findAndThen(userMap, creatorId, user -> supplier.setCreatorName(user.getNickname()));
            }
            Long purchaserId = parseUserId(supplier.getPurchaser());
            if (purchaserId != null) {
                MapUtils.findAndThen(userMap, purchaserId, user -> supplier.setPurchaserName(user.getNickname()));
            }
            Long updaterId = parseUserId(supplier.getUpdater());
            if (updaterId != null) {
                MapUtils.findAndThen(userMap, updaterId, user -> supplier.setUpdaterName(user.getNickname()));
            }
        });
    }

    private void addUserId(Set<Long> userIds, String userId) {
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
