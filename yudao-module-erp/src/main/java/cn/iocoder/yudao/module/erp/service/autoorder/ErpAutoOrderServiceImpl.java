package cn.iocoder.yudao.module.erp.service.autoorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpAutoOrderRuleDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.autoorder.ErpPurchaseSuggestionItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpAutoOrderRuleMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpPurchaseSuggestionItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpPurchaseSuggestionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.autoorder.ErpPurchaseSuggestionPageReqVO;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 自动订货 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpAutoOrderServiceImpl implements ErpAutoOrderService {

    @Resource
    private ErpAutoOrderRuleMapper autoOrderRuleMapper;
    @Resource
    private ErpPurchaseSuggestionMapper suggestionMapper;
    @Resource
    private ErpPurchaseSuggestionItemMapper suggestionItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;

    // ========== 订货规则 ==========

    @Override
    public Long createRule(ErpAutoOrderRuleDO rule) {
        autoOrderRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(ErpAutoOrderRuleDO rule) {
        validateRuleExists(rule.getId());
        autoOrderRuleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        validateRuleExists(id);
        autoOrderRuleMapper.deleteById(id);
    }

    @Override
    public ErpAutoOrderRuleDO getRule(Long id) {
        return autoOrderRuleMapper.selectById(id);
    }

    @Override
    public List<ErpAutoOrderRuleDO> getRuleList(Integer status) {
        return autoOrderRuleMapper.selectListByStatus(status);
    }

    private void validateRuleExists(Long id) {
        if (autoOrderRuleMapper.selectById(id) == null) {
            throw exception(AUTO_ORDER_RULE_NOT_EXISTS);
        }
    }

    // ========== 采购建议单 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateSuggestion(Long warehouseId) {
        // 1. 获取所有启用的规则
        List<ErpAutoOrderRuleDO> rules = autoOrderRuleMapper.selectListByStatus(0);

        // 2. 生成建议单
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_SUGGESTION_NO_PREFIX);
        ErpPurchaseSuggestionDO suggestionDO = ErpPurchaseSuggestionDO.builder()
                .no(no)
                .status(10) // 待确认
                .warehouseId(warehouseId)
                .suggestTime(LocalDateTime.now())
                .build();

        List<ErpPurchaseSuggestionItemDO> items = new ArrayList<>();
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;

        // 3. 遍历规则，计算建议采购量
        // TODO: 实际实现需要注入 ErpStockService 查询库存，注入销售出库统计查询日均销量
        // 此处为框架代码，具体计算逻辑：
        // 日均销量 = 过去 calcDays 天销售出库总量 / calcDays
        // 触发条件 = (当前库存 - 占用库存) < minStock
        // 建议采购量 = maxStock - (当前库存 - 占用库存) + (日均销量 * leadDays)

        for (ErpAutoOrderRuleDO rule : rules) {
            // 过滤仓库
            if (warehouseId != null && rule.getWarehouseId() != null
                    && !warehouseId.equals(rule.getWarehouseId())) {
                continue;
            }
            // 实际计算逻辑预留
            // BigDecimal currentStock = ...;
            // BigDecimal lockStock = ...;
            // BigDecimal availableStock = currentStock.subtract(lockStock);
            // if (availableStock.compareTo(rule.getMinStock()) < 0) {
            //     BigDecimal avgDailySale = ...;
            //     BigDecimal suggestCount = rule.getMaxStock()
            //         .subtract(availableStock)
            //         .add(avgDailySale.multiply(BigDecimal.valueOf(rule.getLeadDays())));
            //     // 创建建议项
            // }
        }

        suggestionDO.setTotalCount(totalCount);
        suggestionDO.setTotalPrice(totalPrice);
        suggestionMapper.insert(suggestionDO);

        // 4. 插入建议明细
        for (ErpPurchaseSuggestionItemDO item : items) {
            item.setSuggestionId(suggestionDO.getId());
        }
        if (!items.isEmpty()) {
            suggestionItemMapper.insertBatch(items);
        }

        return suggestionDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSuggestion(Long id) {
        ErpPurchaseSuggestionDO suggestion = validateSuggestionExists(id);
        if (!Integer.valueOf(10).equals(suggestion.getStatus())) {
            throw exception(PURCHASE_SUGGESTION_CONFIRM_FAIL);
        }
        ErpPurchaseSuggestionDO updateDO = new ErpPurchaseSuggestionDO();
        updateDO.setId(id);
        updateDO.setStatus(20); // 已确认
        suggestionMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePurchaseOrder(Long id) {
        ErpPurchaseSuggestionDO suggestion = validateSuggestionExists(id);
        if (!Integer.valueOf(20).equals(suggestion.getStatus())) {
            throw exception(PURCHASE_SUGGESTION_GENERATE_FAIL);
        }

        // TODO: 调用 ErpPurchaseOrderService.createPurchaseOrder() 生成采购订单
        // List<ErpPurchaseSuggestionItemDO> items = suggestionItemMapper.selectListBySuggestionId(id);
        // 将建议项转换为采购订单项，按供应商分组生成多个采购订单

        // 更新状态
        ErpPurchaseSuggestionDO updateDO = new ErpPurchaseSuggestionDO();
        updateDO.setId(id);
        updateDO.setStatus(30); // 已生成采购单
        // updateDO.setPurchaseOrderId(purchaseOrderId);
        suggestionMapper.updateById(updateDO);
    }

    @Override
    public ErpPurchaseSuggestionDO getSuggestion(Long id) {
        return suggestionMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchaseSuggestionDO> getSuggestionPage(ErpPurchaseSuggestionPageReqVO pageReqVO) {
        return suggestionMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseSuggestionItemDO> getSuggestionItemList(Long suggestionId) {
        return suggestionItemMapper.selectListBySuggestionId(suggestionId);
    }

    private ErpPurchaseSuggestionDO validateSuggestionExists(Long id) {
        ErpPurchaseSuggestionDO suggestion = suggestionMapper.selectById(id);
        if (suggestion == null) {
            throw exception(PURCHASE_SUGGESTION_NOT_EXISTS);
        }
        return suggestion;
    }

}
