package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission.ErpSaleDirectForbiddenDeptRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleDirectForbiddenDeptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleDirectForbiddenDeptMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_DIRECT_DEPT_FORBIDDEN;

/**
 * ERP 销售直接开单部门权限 Service 实现
 */
@Service
@Validated
public class ErpSaleDirectDeptPermissionServiceImpl implements ErpSaleDirectDeptPermissionService {

    @Resource
    private ErpSaleDirectForbiddenDeptMapper saleDirectForbiddenDeptMapper;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<ErpSaleDirectForbiddenDeptRespVO> getForbiddenDeptList() {
        List<ErpSaleDirectForbiddenDeptDO> list = saleDirectForbiddenDeptMapper.selectListByCurrentTenant();
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, ErpSaleDirectForbiddenDeptDO::getDeptId));
        List<ErpSaleDirectForbiddenDeptRespVO> result = new ArrayList<>(list.size());
        for (ErpSaleDirectForbiddenDeptDO item : list) {
            ErpSaleDirectForbiddenDeptRespVO respVO = new ErpSaleDirectForbiddenDeptRespVO();
            respVO.setDeptId(item.getDeptId());
            DeptRespDTO dept = deptMap.get(item.getDeptId());
            respVO.setDeptName(dept != null ? dept.getName() : null);
            result.add(respVO);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateForbiddenDeptList(Collection<Long> deptIds) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        saleDirectForbiddenDeptMapper.deleteListByTenantId(tenantId);
        normalizeDeptIds(deptIds).forEach(deptId -> saleDirectForbiddenDeptMapper.insertIgnore(deptId, tenantId));
    }

    @Override
    public void validateSaleDocumentDeptAllowed(Long deptId) {
        if (deptId == null || saleDirectForbiddenDeptMapper.selectListByDeptIds(java.util.Collections.singleton(deptId)).isEmpty()) {
            return;
        }
        throw exception(SALE_DIRECT_DEPT_FORBIDDEN, getDeptName(deptId));
    }

    private Set<Long> normalizeDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return deptIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String getDeptName(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId);
        return dept != null && dept.getName() != null ? dept.getName() : String.valueOf(deptId);
    }

}
