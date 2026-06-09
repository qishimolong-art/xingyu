package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSaleConfigMapper extends BaseMapperX<ErpSaleConfigDO> {

    default ErpSaleConfigDO selectByTypeAndCode(String configType, String code) {
        return selectOne(ErpSaleConfigDO::getConfigType, configType, ErpSaleConfigDO::getCode, code);
    }

    default PageResult<ErpSaleConfigDO> selectPage(ErpSaleConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSaleConfigDO>()
                .eqIfPresent(ErpSaleConfigDO::getConfigType, reqVO.getConfigType())
                .likeIfPresent(ErpSaleConfigDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpSaleConfigDO::getName, reqVO.getName())
                .eqIfPresent(ErpSaleConfigDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpSaleConfigDO::getDeptId, reqVO.getDeptId())
                .orderByAsc(ErpSaleConfigDO::getConfigType)
                .orderByAsc(ErpSaleConfigDO::getSort)
                .orderByDesc(ErpSaleConfigDO::getId));
    }

    default List<ErpSaleConfigDO> selectListByTypeAndStatus(String configType, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpSaleConfigDO>()
                .eq(ErpSaleConfigDO::getConfigType, configType)
                .eqIfPresent(ErpSaleConfigDO::getStatus, status)
                .orderByAsc(ErpSaleConfigDO::getSort)
                .orderByDesc(ErpSaleConfigDO::getId));
    }

}
