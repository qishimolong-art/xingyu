package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCustomerImageMapper extends BaseMapperX<ErpCustomerImageDO> {

    default PageResult<ErpCustomerImageDO> selectPage(ErpCustomerImagePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerImageDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpCustomerImageDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(ErpCustomerImageDO::getImageType, ("__other__".equals(reqVO.getImageType()) || "__store__".equals(reqVO.getImageType())) ? null : reqVO.getImageType());
        if ("__other__".equals(reqVO.getImageType())) wrapper.and(query -> query.isNull(ErpCustomerImageDO::getImageType).or().notIn(ErpCustomerImageDO::getImageType, java.util.Arrays.asList("营业执照", "门店图片", "门头照片")));
        if ("__store__".equals(reqVO.getImageType())) wrapper.in(ErpCustomerImageDO::getImageType, java.util.Arrays.asList("门店图片", "门头照片"));
        wrapper.orderByAsc(ErpCustomerImageDO::getSort);
        wrapper.orderByDesc(ErpCustomerImageDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpCustomerImageDO::getImageType,
                ErpCustomerImageDO::getImageName,
                ErpCustomerImageDO::getRemark);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpCustomerImageDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerImageDO>()
                .eq(ErpCustomerImageDO::getCustomerId, customerId)
                .orderByAsc(ErpCustomerImageDO::getSort)
                .orderByDesc(ErpCustomerImageDO::getDefaulted)
                .orderByDesc(ErpCustomerImageDO::getId));
    }

}
