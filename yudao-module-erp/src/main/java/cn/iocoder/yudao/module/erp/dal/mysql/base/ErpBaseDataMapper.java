package cn.iocoder.yudao.module.erp.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 基础数据 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpBaseDataMapper extends BaseMapperX<ErpBaseDataDO> {

    default PageResult<ErpBaseDataDO> selectPage(ErpBaseDataPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eqIfPresent(ErpBaseDataDO::getType, reqVO.getType())
                .likeIfPresent(ErpBaseDataDO::getName, reqVO.getName())
                .eqIfPresent(ErpBaseDataDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpBaseDataDO::getSort)
                .orderByDesc(ErpBaseDataDO::getId));
    }

    default List<ErpBaseDataDO> selectListByTypeAndStatus(String type, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eq(ErpBaseDataDO::getType, type)
                .eq(ErpBaseDataDO::getStatus, status)
                .orderByAsc(ErpBaseDataDO::getSort));
    }

    default ErpBaseDataDO selectByTypeAndName(String type, String name) {
        return selectOne(new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eq(ErpBaseDataDO::getType, type)
                .eq(ErpBaseDataDO::getName, name));
    }

}
