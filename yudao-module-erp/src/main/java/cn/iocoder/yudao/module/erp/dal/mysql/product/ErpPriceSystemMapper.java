package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpPriceSystemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPriceSystemMapper extends BaseMapperX<ErpPriceSystemDO> {

    default ErpPriceSystemDO selectByCode(String code) {
        return selectOne(ErpPriceSystemDO::getCode, code);
    }

    default PageResult<ErpPriceSystemDO> selectPage(String name, Integer status, Integer pageNo, Integer pageSize) {
        return selectPage(
                new cn.iocoder.yudao.framework.common.pojo.PageParam().setPageNo(pageNo).setPageSize(pageSize),
                new LambdaQueryWrapperX<ErpPriceSystemDO>()
                        .likeIfPresent(ErpPriceSystemDO::getName, name)
                        .eqIfPresent(ErpPriceSystemDO::getStatus, status)
                        .orderByAsc(ErpPriceSystemDO::getSort));
    }
}
