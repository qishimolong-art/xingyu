package cn.iocoder.yudao.module.erp.service.autoorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpAutoOrderRuleDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpPurchaseSuggestionPageReqVO;

import java.util.List;

/**
 * ERP 自动订货 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpAutoOrderService {

    // ========== 订货规则 ==========
    Long createRule(ErpAutoOrderRuleDO rule);
    void updateRule(ErpAutoOrderRuleDO rule);
    void deleteRule(Long id);
    ErpAutoOrderRuleDO getRule(Long id);
    List<ErpAutoOrderRuleDO> getRuleList(Integer status);

    // ========== 采购建议单 ==========
    /**
     * 执行自动订货计算，生成采购建议单
     */
    Long generateSuggestion(Long warehouseId);

    /**
     * 确认采购建议单
     */
    void confirmSuggestion(Long id);

    /**
     * 将采购建议单转为采购订单
     */
    void generatePurchaseOrder(Long id);

    ErpPurchaseSuggestionDO getSuggestion(Long id);

    PageResult<ErpPurchaseSuggestionDO> getSuggestionPage(ErpPurchaseSuggestionPageReqVO pageReqVO);

    List<ErpPurchaseSuggestionItemDO> getSuggestionItemList(Long suggestionId);

}
