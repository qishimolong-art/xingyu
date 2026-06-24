package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 仓库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpWarehouseMapper extends BaseMapperX<ErpWarehouseDO> {

    default PageResult<ErpWarehouseDO> selectPage(ErpWarehousePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpWarehouseDO>()
                .likeIfPresent(ErpWarehouseDO::getName, reqVO.getName())
                .eqIfPresent(ErpWarehouseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpWarehouseDO::getWarehouseType, reqVO.getWarehouseType())
                .likeIfPresent(ErpWarehouseDO::getWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(ErpWarehouseDO::getDeptId, reqVO.getDeptId())
                .orderByDesc(ErpWarehouseDO::getId));
    }

    default ErpWarehouseDO selectByDefaultStatus() {
        return selectOne(ErpWarehouseDO::getDefaultStatus, true);
    }

    default ErpWarehouseDO selectByWarehouseCode(String warehouseCode) {
        return selectOne(ErpWarehouseDO::getWarehouseCode, warehouseCode);
    }

    default ErpWarehouseDO selectByName(String name) {
        return selectOne(ErpWarehouseDO::getName, name);
    }

    default List<ErpWarehouseDO> selectListByStatus(Integer status) {
        return selectList(ErpWarehouseDO::getStatus, status);
    }

    default List<ErpWarehouseDO> selectListByDeptId(Long deptId) {
        return selectList(ErpWarehouseDO::getDeptId, deptId);
    }

}
