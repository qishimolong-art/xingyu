package cn.iocoder.yudao.module.erp.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
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
        LambdaQueryWrapperX<ErpBaseDataDO> wrapper = new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eqIfPresent(ErpBaseDataDO::getType, reqVO.getType())
                .likeIfPresent(ErpBaseDataDO::getName, reqVO.getName())
                .likeIfPresent(ErpBaseDataDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpBaseDataDO::getStatus, reqVO.getStatus());
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpBaseDataDO::getType, ErpBaseDataDO::getName, ErpBaseDataDO::getCode,
                ErpBaseDataDO::getRemark);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpBaseDataDO> wrapper, ErpBaseDataPageReqVO reqVO) {
        SFunction<ErpBaseDataDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByAsc(ErpBaseDataDO::getSort).orderByDesc(ErpBaseDataDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
        } else {
            wrapper.orderByAsc(ErpBaseDataDO::getSort).orderByDesc(ErpBaseDataDO::getId);
        }
    }

    static SFunction<ErpBaseDataDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpBaseDataDO::getId;
            case "name":
                return ErpBaseDataDO::getName;
            case "code":
                return ErpBaseDataDO::getCode;
            case "sort":
                return ErpBaseDataDO::getSort;
            case "status":
                return ErpBaseDataDO::getStatus;
            case "createTime":
                return ErpBaseDataDO::getCreateTime;
            default:
                return null;
        }
    }

    default List<ErpBaseDataDO> selectListByTypeAndStatus(String type, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eq(ErpBaseDataDO::getType, type)
                .eq(ErpBaseDataDO::getStatus, status)
                .orderByAsc(ErpBaseDataDO::getSort)
                .orderByAsc(ErpBaseDataDO::getId));
    }

    default ErpBaseDataDO selectByTypeAndName(String type, String name) {
        return selectOne(new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eq(ErpBaseDataDO::getType, type)
                .eq(ErpBaseDataDO::getName, name));
    }

    default ErpBaseDataDO selectByTypeAndCode(String type, String code) {
        return selectOne(new LambdaQueryWrapperX<ErpBaseDataDO>()
                .eq(ErpBaseDataDO::getType, type)
                .eq(ErpBaseDataDO::getCode, code));
    }

}
