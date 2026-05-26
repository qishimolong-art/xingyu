package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计科目辅助核算关联 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpSubjectAuxiliaryMapper extends BaseMapperX<ErpSubjectAuxiliaryDO> {

    default List<ErpSubjectAuxiliaryDO> selectListBySubjectId(Long subjectId) {
        return selectList(ErpSubjectAuxiliaryDO::getSubjectId, subjectId);
    }

    default List<ErpSubjectAuxiliaryDO> selectListBySubjectIds(List<Long> subjectIds) {
        return selectList(ErpSubjectAuxiliaryDO::getSubjectId, subjectIds);
    }

    default int deleteBySubjectId(Long subjectId) {
        return delete(ErpSubjectAuxiliaryDO::getSubjectId, subjectId);
    }

}
