package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpWarehouseMoveService {

    Long createWarehouseMove(@Valid ErpWarehouseMoveSaveReqVO createReqVO);

    Long createWarehouseMoveDraft(ErpWarehouseMoveDraftCreateReqVO createReqVO);

    void updateWarehouseMove(@Valid ErpWarehouseMoveSaveReqVO updateReqVO);

    void updateWarehouseMoveDraft(ErpWarehouseMoveDraftUpdateReqVO updateReqVO);

    void updateAndSubmitWarehouseMoveDraft(@Valid ErpWarehouseMoveSaveReqVO updateReqVO);

    void submitWarehouseMove(Long id);

    void updateWarehouseMoveRemark(@Valid ErpStockUpdateRemarkReqVO updateReqVO);

    void updateWarehouseMoveStatus(Long id, Integer status);

    void deleteWarehouseMove(List<Long> ids);

    ErpWarehouseMoveDO getWarehouseMove(Long id);

    PageResult<ErpWarehouseMoveDO> getWarehouseMovePage(ErpWarehouseMovePageReqVO pageReqVO);

    ErpWarehouseMoveSummaryRespVO getWarehouseMoveSummary(ErpWarehouseMovePageReqVO pageReqVO);

    List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId);

    PageResult<ErpWarehouseMoveItemDO> getWarehouseMoveItemPage(ErpWarehouseMoveItemPageReqVO pageReqVO);

    List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveIds(Collection<Long> moveIds);

}
