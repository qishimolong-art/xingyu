package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 会计科目 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpAccountingSubjectMapper extends BaseMapperX<ErpAccountingSubjectDO> {

    default ErpAccountingSubjectDO selectBySubjectCode(String subjectCode) {
        return selectOne(ErpAccountingSubjectDO::getSubjectCode, subjectCode);
    }

    default List<ErpAccountingSubjectDO> selectListByCategory(Integer subjectCategory) {
        return selectList(new LambdaQueryWrapperX<ErpAccountingSubjectDO>()
                .eq(ErpAccountingSubjectDO::getSubjectCategory, subjectCategory)
                .orderByAsc(ErpAccountingSubjectDO::getSubjectCode));
    }

    default List<ErpAccountingSubjectDO> selectListByParentCode(String parentCode) {
        return selectList(ErpAccountingSubjectDO::getParentCode, parentCode);
    }

    default List<ErpAccountingSubjectDO> selectListByIsLeaf(Boolean isLeaf) {
        return selectList(new LambdaQueryWrapperX<ErpAccountingSubjectDO>()
                .eqIfPresent(ErpAccountingSubjectDO::getIsLeaf, isLeaf)
                .orderByAsc(ErpAccountingSubjectDO::getSubjectCode));
    }

    default List<ErpAccountingSubjectDO> selectListBySubjectCodes(Collection<String> subjectCodes) {
        return selectList(ErpAccountingSubjectDO::getSubjectCode, subjectCodes);
    }

    default List<ErpAccountingSubjectDO> selectListAllOrderByCode() {
        return selectList(new LambdaQueryWrapperX<ErpAccountingSubjectDO>()
                .orderByAsc(ErpAccountingSubjectDO::getSubjectCode));
    }

    default PageResult<ErpAccountingSubjectDO> selectPage(ErpAccountingSubjectPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpAccountingSubjectDO> wrapper = new LambdaQueryWrapperX<ErpAccountingSubjectDO>()
                .likeIfPresent(ErpAccountingSubjectDO::getSubjectCode, reqVO.getSubjectCode())
                .likeIfPresent(ErpAccountingSubjectDO::getSubjectName, reqVO.getSubjectName())
                .eqIfPresent(ErpAccountingSubjectDO::getSubjectCategory, reqVO.getSubjectCategory())
                .eqIfPresent(ErpAccountingSubjectDO::getParentCode, reqVO.getParentCode())
                .eqIfPresent(ErpAccountingSubjectDO::getSubjectLevel, reqVO.getSubjectLevel())
                .eqIfPresent(ErpAccountingSubjectDO::getIsLeaf, reqVO.getIsLeaf())
                .eqIfPresent(ErpAccountingSubjectDO::getEnable, reqVO.getEnable());
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpAccountingSubjectDO::getSubjectCode, ErpAccountingSubjectDO::getSubjectName,
                ErpAccountingSubjectDO::getShortName, ErpAccountingSubjectDO::getParentCode,
                ErpAccountingSubjectDO::getRemark);
        wrapper.orderByAsc(ErpAccountingSubjectDO::getSubjectCode);
        return selectPage(reqVO, wrapper);
    }

}
