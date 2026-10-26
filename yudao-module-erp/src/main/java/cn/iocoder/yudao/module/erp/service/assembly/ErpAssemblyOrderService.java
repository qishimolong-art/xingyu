package cn.iocoder.yudao.module.erp.service.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpAssemblyOrderService {

    Long createDraft(@Valid ErpAssemblyOrderSaveReqVO reqVO);

    void updateDraft(@Valid ErpAssemblyOrderSaveReqVO reqVO);

    void confirm(Long id);

    void reverseConfirm(Long id);

    void start(Long id);

    void cancelStart(Long id);

    void complete(Long id);

    void reverseAssembly(Long id);

    void deleteDraft(Long id);

    ErpAssemblyOrderDO getOrder(Long id);

    PageResult<ErpAssemblyOrderDO> getOrderPage(ErpAssemblyOrderPageReqVO reqVO);

    List<ErpAssemblyOrderItemDO> getOrderItemList(Long orderId);

    List<ErpAssemblyOrderItemDO> getOrderItemList(Collection<Long> orderIds);

    PageResult<ErpAssemblyOrderItemDO> getOrderItemPage(ErpAssemblyOrderItemPageReqVO reqVO);

}
