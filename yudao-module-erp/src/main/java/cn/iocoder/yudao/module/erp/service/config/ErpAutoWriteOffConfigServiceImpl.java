package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpAutoWriteOffDeptConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpAutoWriteOffDeptConfigMapper;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpAutoWriteOffConfigServiceImpl implements ErpAutoWriteOffConfigService {

    @Resource
    private ErpAutoWriteOffDeptConfigMapper autoWriteOffDeptConfigMapper;
    @Resource
    private DeptApi deptApi;

    @Override
    public ErpAutoWriteOffConfigRespVO getConfig() {
        List<Long> disabledDeptIds = getDisabledDeptIds();
        ErpAutoWriteOffConfigRespVO result = new ErpAutoWriteOffConfigRespVO();
        result.setDisabledDeptIds(disabledDeptIds);
        result.setEffectiveDisabledDeptIds(new ArrayList<>(expandDeptIds(disabledDeptIds)));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ErpAutoWriteOffConfigUpdateReqVO reqVO) {
        List<Long> deptIds = CollUtil.emptyIfNull(reqVO.getDisabledDeptIds()).stream()
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deptIds)) {
            deptApi.validateDeptList(deptIds);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String operator = String.valueOf(loginUserId != null ? loginUserId : 0L);
        autoWriteOffDeptConfigMapper.softDeleteAll(operator, tenantId);
        deptIds.forEach(deptId -> autoWriteOffDeptConfigMapper.restoreOrInsert(deptId, operator, tenantId));
    }

    @Override
    public boolean isAutoWriteOffDisabled(Long deptId) {
        if (deptId == null) {
            return true;
        }
        return expandDeptIds(getDisabledDeptIds()).contains(deptId);
    }

    private List<Long> getDisabledDeptIds() {
        return autoWriteOffDeptConfigMapper.selectActiveList().stream()
                .map(ErpAutoWriteOffDeptConfigDO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Set<Long> expandDeptIds(Collection<Long> deptIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (CollUtil.isEmpty(deptIds)) {
            return result;
        }
        deptIds.stream().filter(Objects::nonNull).sorted().forEach(deptId -> {
            result.add(deptId);
            List<DeptRespDTO> children = deptApi.getChildDeptList(deptId);
            if (CollUtil.isNotEmpty(children)) {
                children.stream().map(DeptRespDTO::getId).filter(Objects::nonNull)
                        .sorted().forEach(result::add);
            }
        });
        return result;
    }

}
