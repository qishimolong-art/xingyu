package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpPriceSystemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPriceSystemMapper extends BaseMapperX<ErpPriceSystemDO> {

    default ErpPriceSystemDO selectByCode(String code) {
        return selectOne(ErpPriceSystemDO::getCode, code);
    }

    default PageResult<ErpPriceSystemDO> selectPage(ErpPriceSystemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPriceSystemDO> wrapper = new LambdaQueryWrapperX<ErpPriceSystemDO>()
                .likeIfPresent(ErpPriceSystemDO::getName, reqVO.getName())
                .eqIfPresent(ErpPriceSystemDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPriceSystemDO::getCode, reqVO.getCode());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpPriceSystemDO::getCode, ErpPriceSystemDO::getName, ErpPriceSystemDO::getRemark);
        wrapper.orderByAsc(ErpPriceSystemDO::getSort);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpPriceSystemDO> selectPage(String name, Integer status, Integer pageNo, Integer pageSize) {
        cn.iocoder.yudao.framework.common.pojo.PageParam pageParam =
                new cn.iocoder.yudao.framework.common.pojo.PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);
        return selectPage(pageParam, new LambdaQueryWrapperX<ErpPriceSystemDO>()
                .likeIfPresent(ErpPriceSystemDO::getName, name)
                .eqIfPresent(ErpPriceSystemDO::getStatus, status)
                .orderByAsc(ErpPriceSystemDO::getSort));
    }
}
