package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ERP 产品库存明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockRecordMapper extends BaseMapperX<ErpStockRecordDO> {

    /**
     * 五期新方法：支持按产品维度预过滤 productIds
     * 注意：不能重载父类 selectPage(PageParam, Wrapper) 的签名，改用独立方法名。
     */
    default PageResult<ErpStockRecordDO> selectPageWithProductFilter(ErpStockRecordPageReqVO reqVO,
                                                                     Collection<Long> productIdFilter) {
        QueryWrapperX<ErpStockRecordDO> wrapper = new QueryWrapperX<ErpStockRecordDO>()
                .eqIfPresent("product_id", reqVO.getProductId())
                .eqIfPresent("warehouse_id", reqVO.getWarehouseId())
                .eqIfPresent("biz_type", reqVO.getBizType())
                .eqIfPresent("biz_id", reqVO.getBizId())
                .eqIfPresent("biz_item_id", reqVO.getBizItemId())
                .likeIfPresent("biz_no", reqVO.getBizNo())
                .betweenIfPresent("create_time", reqVO.getCreateTime())
                .betweenIfPresent("biz_date", reqVO.getBizDate());
        appendBatchNoFilter(wrapper, reqVO);
        if (reqVO.getBizTypes() != null && !reqVO.getBizTypes().isEmpty()) {
            wrapper.in("biz_type", reqVO.getBizTypes());
        }
        if (productIdFilter != null) {
            if (productIdFilter.isEmpty()) {
                return PageResult.empty(0L);
            }
            wrapper.in("product_id", productIdFilter);
        }
        appendKeyword(wrapper, reqVO.getKeyword());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void appendBatchNoFilter(QueryWrapperX<ErpStockRecordDO> wrapper, ErpStockRecordPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getUnassignedBatch())) {
            wrapper.and(w -> w.isNull("batch_no").or().apply("TRIM(batch_no) = ''"));
            return;
        }
        if (StringUtils.hasText(reqVO.getBatchNo())) {
            wrapper.apply("TRIM(batch_no) = {0}", reqVO.getBatchNo().trim());
        }
    }

    /** 查询批次流水的完整时间序列，用于计算不受分页和页面筛选影响的批次结存。 */
    default List<ErpStockRecordDO> selectBatchRunningBalanceRecords(Long productId, Long warehouseId,
                                                                    String batchNo, Boolean unassignedBatch) {
        QueryWrapperX<ErpStockRecordDO> wrapper = new QueryWrapperX<ErpStockRecordDO>()
                .eq("product_id", productId)
                .eq("warehouse_id", warehouseId);
        if (Boolean.TRUE.equals(unassignedBatch)) {
            wrapper.and(w -> w.isNull("batch_no").or().apply("TRIM(batch_no) = ''"));
        } else if (StringUtils.hasText(batchNo)) {
            wrapper.apply("TRIM(batch_no) = {0}", batchNo.trim());
        }
        wrapper.orderByAsc("create_time").orderByAsc("id");
        return selectList(wrapper);
    }

    static void appendKeyword(QueryWrapperX<ErpStockRecordDO> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String value = keyword.trim().replaceAll("\\s+", "%");
        wrapper.and(w -> w.like("biz_no", value)
                .or().apply("EXISTS (SELECT 1 FROM system_dept d WHERE d.id = dept_id "
                        + "AND d.deleted = b'0' AND d.name LIKE {0})", "%" + value + "%")
                .or().apply("EXISTS (SELECT 1 FROM erp_product p "
                        + "WHERE p.id = product_id AND p.deleted = b'0' "
                        + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.pinyin_code LIKE {0} "
                        + "OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} OR p.vehicle_model LIKE {0} "
                        + "OR p.factory_code LIKE {0} OR p.standard LIKE {0} OR p.brand LIKE {0} "
                        + "OR p.drawing_no LIKE {0}))", "%" + value + "%")
                .or().apply("DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}", "%" + value + "%"));
    }

    /**
     * 一次扫描批次流水，返回命中批次号的产品、仓库组合，避免库存分页对流水表执行相关子查询。
     */
    default Map<Long, Set<Long>> selectStockKeyMapByBatchNoKeyword(
            String keyword, Collection<Long> warehouseIdFilter) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyMap();
        }
        if (warehouseIdFilter != null && warehouseIdFilter.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<ErpStockRecordDO> wrapper =
                buildBatchNoKeywordStockKeyWrapper(keyword, warehouseIdFilter);
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        for (ErpStockRecordDO record : selectList(wrapper)) {
            if (record.getProductId() == null || record.getWarehouseId() == null) {
                continue;
            }
            result.computeIfAbsent(record.getProductId(), key -> new LinkedHashSet<>())
                    .add(record.getWarehouseId());
        }
        return result;
    }

    static QueryWrapper<ErpStockRecordDO> buildBatchNoKeywordStockKeyWrapper(
            String keyword, Collection<Long> warehouseIdFilter) {
        String value = keyword.trim().replaceAll("\\s+", "%");
        QueryWrapper<ErpStockRecordDO> wrapper = new QueryWrapper<ErpStockRecordDO>()
                .select("product_id", "warehouse_id")
                .isNotNull("batch_no")
                .ne("batch_no", "")
                .apply("TRIM(batch_no) LIKE {0}", "%" + value + "%")
                .groupBy("product_id", "warehouse_id");
        if (warehouseIdFilter != null) {
            wrapper.in("warehouse_id", warehouseIdFilter);
        }
        return wrapper;
    }

    static void orderByIfPresent(QueryWrapperX<ErpStockRecordDO> wrapper, ErpStockRecordPageReqVO reqVO) {
        String orderExpression = getOrderExpression(reqVO.getOrderField());
        if (orderExpression == null || !("asc".equalsIgnoreCase(reqVO.getOrderDirection())
                || "desc".equalsIgnoreCase(reqVO.getOrderDirection()))) {
            wrapper.orderByDesc("id");
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderExpression);
        } else {
            wrapper.orderByDesc(orderExpression);
        }
        wrapper.orderByDesc("id");
    }

    static String getOrderExpression(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "bizDate":
                return "erp_stock_record.biz_date";
            case "bizType":
                return "erp_stock_record.biz_type";
            case "bizNo":
                return "erp_stock_record.biz_no";
            case "productCode":
                return productField("code");
            case "productName":
                return productField("name");
            case "warehouseName":
                return warehouseNameExpression();
            case "inCount":
                return positiveValue("count");
            case "inUnitPrice":
                return positiveValue("unit_price");
            case "inAmount":
                return positiveValue("total_price");
            case "outCount":
                return negativeAbsoluteValue("count");
            case "outUnitPrice":
                return negativeValue("unit_price");
            case "outAmount":
                return negativeAbsoluteValue("total_price");
            case "totalCount":
                return "erp_stock_record.total_count";
            case "costPrice":
                return "erp_stock_record.cost_price";
            case "costAmount":
                return "erp_stock_record.cost_amount";
            default:
                return null;
        }
    }

    static String productField(String column) {
        return "(SELECT p." + column + " FROM erp_product p WHERE p.id = erp_stock_record.product_id "
                + "AND p.deleted = b'0')";
    }

    static String warehouseNameExpression() {
        return "(SELECT w.name FROM erp_warehouse w WHERE w.id = erp_stock_record.warehouse_id "
                + "AND w.deleted = b'0')";
    }

    static String positiveValue(String column) {
        return "CASE WHEN erp_stock_record.count > 0 THEN erp_stock_record." + column + " END";
    }

    static String negativeValue(String column) {
        return "CASE WHEN erp_stock_record.count < 0 THEN erp_stock_record." + column + " END";
    }

    static String negativeAbsoluteValue(String column) {
        return "CASE WHEN erp_stock_record.count < 0 THEN ABS(erp_stock_record." + column + ") END";
    }

    /** 原签名：保留向后兼容，委托到新方法 */
    default PageResult<ErpStockRecordDO> selectPage(ErpStockRecordPageReqVO reqVO) {
        return selectPageWithProductFilter(reqVO, null);
    }

    default java.util.List<ErpStockRecordDO> selectListByBiz(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<ErpStockRecordDO>()
                .eq(ErpStockRecordDO::getBizType, bizType)
                .eq(ErpStockRecordDO::getBizId, bizId));
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockRecordDO::getWarehouseId, warehouseId);
    }

    default Long selectCountByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return selectCount(new LambdaQueryWrapperX<ErpStockRecordDO>()
                .eq(ErpStockRecordDO::getProductId, productId)
                .eq(ErpStockRecordDO::getWarehouseId, warehouseId));
    }

    /**
     * 按产品、仓库、批次号汇总库存流水余额。未指定批次统一返回空批次号。
     */
    default List<Map<String, Object>> selectBatchBalanceList(Collection<ErpStockDO> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<Long> productIds = stocks.stream().map(ErpStockDO::getProductId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Collection<Long> warehouseIds = stocks.stream().map(ErpStockDO::getWarehouseId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (productIds.isEmpty() || warehouseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectMaps(new QueryWrapper<ErpStockRecordDO>()
                .select("product_id", "warehouse_id", "NULLIF(TRIM(batch_no), '') AS batch_no",
                        "SUM(count) AS available_count",
                        "MIN(CASE WHEN count > 0 THEN COALESCE(biz_date, create_time) END) AS first_in_time")
                .in("product_id", productIds)
                .in("warehouse_id", warehouseIds)
                .groupBy("product_id", "warehouse_id", "NULLIF(TRIM(batch_no), '')"));
    }

}
