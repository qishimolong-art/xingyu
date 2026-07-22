package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSaleConfigMapper extends BaseMapperX<ErpSaleConfigDO> {

    default ErpSaleConfigDO selectByTypeAndCode(String configType, String code) {
        return selectOne(ErpSaleConfigDO::getConfigType, configType, ErpSaleConfigDO::getCode, code);
    }

    default PageResult<ErpSaleConfigDO> selectPage(ErpSaleConfigPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleConfigDO> wrapper = new LambdaQueryWrapperX<ErpSaleConfigDO>()
                .eqIfPresent(ErpSaleConfigDO::getConfigType, reqVO.getConfigType())
                .likeIfPresent(ErpSaleConfigDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpSaleConfigDO::getName, reqVO.getName())
                .eqIfPresent(ErpSaleConfigDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpSaleConfigDO::getDeptId, reqVO.getDeptId());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpSaleConfigDO::getConfigType, ErpSaleConfigDO::getCode, ErpSaleConfigDO::getName,
                ErpSaleConfigDO::getConfigValue, ErpSaleConfigDO::getRemark);
        wrapper.orderByAsc(ErpSaleConfigDO::getConfigType)
                .orderByAsc(ErpSaleConfigDO::getSort)
                .orderByDesc(ErpSaleConfigDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpSaleConfigDO> selectListByTypeAndStatus(String configType, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpSaleConfigDO>()
                .eq(ErpSaleConfigDO::getConfigType, configType)
                .eqIfPresent(ErpSaleConfigDO::getStatus, status)
                .orderByAsc(ErpSaleConfigDO::getSort)
                .orderByDesc(ErpSaleConfigDO::getId));
    }

    default List<ErpSaleConfigDO> selectListByType(String configType) {
        return selectList(new LambdaQueryWrapperX<ErpSaleConfigDO>()
                .eq(ErpSaleConfigDO::getConfigType, configType)
                .orderByAsc(ErpSaleConfigDO::getSort)
                .orderByDesc(ErpSaleConfigDO::getId));
    }

    @Delete("<script>"
            + "DELETE FROM erp_sale_config "
            + "WHERE config_type = #{configType} "
            + "<if test='tenantId != null'>"
            + "AND tenant_id = #{tenantId} "
            + "</if>"
            + "AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach>"
            + "</script>")
    void deletePhysicalByTypeAndIds(@Param("configType") String configType, @Param("tenantId") Long tenantId,
                                    @Param("ids") Collection<Long> ids);

}
