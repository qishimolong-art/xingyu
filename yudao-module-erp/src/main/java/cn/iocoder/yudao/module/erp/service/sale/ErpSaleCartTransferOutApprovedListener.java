package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.service.stock.ErpSaleCartTransferOutApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 调拨出库审批事务内检查并触发销售手推车自动终审。 */
@Component
@RequiredArgsConstructor
public class ErpSaleCartTransferOutApprovedListener {

    private final ErpSaleCartService saleCartService;

    /**
     * 与调拨出库审批共用事务：最后一张调拨出库单触发终审失败时，审批同步回滚并返回错误，
     * 避免调拨单已全部审核、手推车却永久停留在初审状态。
     */
    @EventListener
    public void onTransferOutApproved(ErpSaleCartTransferOutApprovedEvent event) {
        saleCartService.autoFinalApproveAfterTransferOut(event.getSaleCartId(), event.getApproveUserId());
    }

}
