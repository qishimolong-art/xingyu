package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpSubjectAuxiliaryMapper;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ERP 会计科目辅助核算 Service 实现
 *
 * @author Claude
 */
@Service
@Validated
public class ErpSubjectAuxiliaryServiceImpl implements ErpSubjectAuxiliaryService {

    @Resource
    private ErpSubjectAuxiliaryMapper subjectAuxiliaryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSubjectAuxiliary(Long subjectId, String subjectCode, Collection<String> auxiliaryTypes) {
        if (subjectId == null) {
            return;
        }
        // 先删除旧数据
        subjectAuxiliaryMapper.deleteBySubjectId(subjectId);
        if (auxiliaryTypes == null || auxiliaryTypes.isEmpty()) {
            return;
        }
        // 校验有效类型 + 去重，保持原顺序
        Set<String> validTypes = new LinkedHashSet<>();
        Set<String> allowedTypes = new java.util.HashSet<>(Arrays.asList(ErpAuxiliaryTypeEnum.ARRAYS));
        for (String type : auxiliaryTypes) {
            if (type != null && allowedTypes.contains(type)) {
                validTypes.add(type);
            }
        }
        if (validTypes.isEmpty()) {
            return;
        }
        List<ErpSubjectAuxiliaryDO> list = new ArrayList<>(validTypes.size());
        int sort = 0;
        for (String type : validTypes) {
            ErpSubjectAuxiliaryDO entry = ErpSubjectAuxiliaryDO.builder()
                    .subjectId(subjectId)
                    .subjectCode(subjectCode)
                    .auxiliaryType(type)
                    .sort(sort++)
                    .build();
            list.add(entry);
        }
        subjectAuxiliaryMapper.insertBatch(list);
    }

    @Override
    public void deleteBySubjectId(Long subjectId) {
        if (subjectId == null) {
            return;
        }
        subjectAuxiliaryMapper.deleteBySubjectId(subjectId);
    }

    @Override
    public List<ErpSubjectAuxiliaryDO> getListBySubjectId(Long subjectId) {
        if (subjectId == null) {
            return Collections.emptyList();
        }
        return subjectAuxiliaryMapper.selectListBySubjectId(subjectId);
    }

    @Override
    public List<ErpSubjectAuxiliaryDO> getListBySubjectIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return subjectAuxiliaryMapper.selectListBySubjectIds(new ArrayList<>(subjectIds));
    }

}
