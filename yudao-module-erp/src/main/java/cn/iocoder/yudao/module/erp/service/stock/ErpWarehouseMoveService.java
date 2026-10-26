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
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpAssemblyWarehouseMoveCreateReqBO;

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

    void confirmWarehouseMove(Long id);

    void cancelConfirmWarehouseMove(Long id);

    void autoApproveAfterPick(Long id, Long pickUserId);

    /** 为跨仓套件组装创建一张已完成、已过账的系统移货单。 */
    ErpWarehouseMoveDO createAndApproveAssemblyMove(ErpAssemblyWarehouseMoveCreateReqBO createReqBO);

    /** 作废跨仓套件组装产生的系统移货单，并反冲对应库存。 */
    void voidAssemblyMove(Long id, String reason);

    void updateWarehouseMoveRemark(@Valid ErpStockUpdateRemarkReqVO updateReqVO);

    void updateWarehouseMoveStatus(Long id, Integer status);

    void deleteWarehouseMove(List<Long> ids);

    ErpWarehouseMoveDO getWarehouseMove(Long id);

    List<ErpWarehouseMoveDO> getWarehouseMoveList(Collection<Long> ids);

    PageResult<ErpWarehouseMoveDO> getWarehouseMovePage(ErpWarehouseMovePageReqVO pageReqVO);

    ErpWarehouseMoveSummaryRespVO getWarehouseMoveSummary(ErpWarehouseMovePageReqVO pageReqVO);

    List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId);

    List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId, String orderField,
                                                                  String orderDirection);

    PageResult<ErpWarehouseMoveItemDO> getWarehouseMoveItemPage(ErpWarehouseMoveItemPageReqVO pageReqVO);

    List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveIds(Collection<Long> moveIds);

}
