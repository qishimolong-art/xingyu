package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;

import java.util.Collection;
import java.util.List;

/**
 * ERP 会计科目辅助核算 Service 接口
 *
 * @author Claude
 */
public interface ErpSubjectAuxiliaryService {

    /**
     * 保存某科目的辅助核算类型列表（先删后插）
     *
     * @param subjectId     科目编号
     * @param subjectCode   科目编码（冗余，方便排查）
     * @param auxiliaryTypes 辅助核算类型集合（参见 ErpAuxiliaryTypeEnum）
     */
    void saveSubjectAuxiliary(Long subjectId, String subjectCode, Collection<String> auxiliaryTypes);

    /**
     * 删除指定科目下的所有辅助核算关联
     *
     * @param subjectId 科目编号
     */
    void deleteBySubjectId(Long subjectId);

    /**
     * 查询某科目挂的辅助核算列表
     *
     * @param subjectId 科目编号
     * @return 关联列表
     */
    List<ErpSubjectAuxiliaryDO> getListBySubjectId(Long subjectId);

    /**
     * 批量查询多个科目挂的辅助核算列表
     *
     * @param subjectIds 科目编号集合
     * @return 关联列表
     */
    List<ErpSubjectAuxiliaryDO> getListBySubjectIds(Collection<Long> subjectIds);

}
