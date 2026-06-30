package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERP 仓库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpWarehouseMapper extends BaseMapperX<ErpWarehouseDO> {

    default PageResult<ErpWarehouseDO> selectPage(ErpWarehousePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpWarehouseDO> wrapper = new LambdaQueryWrapperX<ErpWarehouseDO>()
                .likeIfPresent(ErpWarehouseDO::getName, reqVO.getName())
                .eqIfPresent(ErpWarehouseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpWarehouseDO::getWarehouseType, reqVO.getWarehouseType())
                .likeIfPresent(ErpWarehouseDO::getWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(ErpWarehouseDO::getDeptId, reqVO.getDeptId());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpWarehouseDO> selectPageByIds(ErpWarehousePageReqVO reqVO, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpWarehouseDO> wrapper = new LambdaQueryWrapperX<ErpWarehouseDO>()
                .likeIfPresent(ErpWarehouseDO::getName, reqVO.getName())
                .eqIfPresent(ErpWarehouseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpWarehouseDO::getWarehouseType, reqVO.getWarehouseType())
                .likeIfPresent(ErpWarehouseDO::getWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(ErpWarehouseDO::getDeptId, reqVO.getDeptId())
                .in(ErpWarehouseDO::getId, ids);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpWarehouseDO> wrapper, ErpWarehousePageReqVO reqVO) {
        SFunction<ErpWarehouseDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpWarehouseDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
            return;
        }
        wrapper.orderByDesc(ErpWarehouseDO::getId);
    }

    static SFunction<ErpWarehouseDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "name":
                return ErpWarehouseDO::getName;
            case "warehouseCode":
                return ErpWarehouseDO::getWarehouseCode;
            case "deptId":
                return ErpWarehouseDO::getDeptId;
            case "warehouseType":
                return ErpWarehouseDO::getWarehouseType;
            case "status":
                return ErpWarehouseDO::getStatus;
            case "saleEnabled":
                return ErpWarehouseDO::getSaleEnabled;
            case "purchaseEnabled":
                return ErpWarehouseDO::getPurchaseEnabled;
            case "stockBillEnabled":
                return ErpWarehouseDO::getStockBillEnabled;
            case "scanControl":
                return ErpWarehouseDO::getScanControl;
            case "splitOrder":
                return ErpWarehouseDO::getSplitOrder;
            case "sort":
                return ErpWarehouseDO::getSort;
            case "remark":
                return ErpWarehouseDO::getRemark;
            default:
                return null;
        }
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

    default List<ErpWarehouseDO> selectListByStatusIfPresent(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpWarehouseDO>()
                .eqIfPresent(ErpWarehouseDO::getStatus, status));
    }

    default List<ErpWarehouseDO> selectListByStatusAndIds(Integer status, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpWarehouseDO>()
                .eqIfPresent(ErpWarehouseDO::getStatus, status)
                .in(ErpWarehouseDO::getId, ids));
    }

    default List<ErpWarehouseDO> selectListByDeptId(Long deptId) {
        return selectList(ErpWarehouseDO::getDeptId, deptId);
    }

}
