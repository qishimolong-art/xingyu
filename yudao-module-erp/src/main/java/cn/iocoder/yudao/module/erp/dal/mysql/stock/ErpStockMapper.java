package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferDirectionEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ERP 产品库存 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMapper extends BaseMapperX<ErpStockDO> {

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO) {
        return selectPage(reqVO, (Collection<Long>) null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter) {
        return selectPage(reqVO, productIdFilter, null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter,
                                             Collection<Long> warehouseIdFilter) {
        return selectPage(reqVO, productIdFilter, warehouseIdFilter, null, null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter,
                                             Collection<Long> warehouseIdFilter,
                                             Collection<Long> keywordProductIdFilter,
                                             Collection<Long> keywordWarehouseIdFilter) {
        return selectPage(reqVO, productIdFilter, warehouseIdFilter, keywordProductIdFilter,
                keywordWarehouseIdFilter, null, null, null, null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter,
                                             Collection<Long> warehouseIdFilter,
                                             Collection<Long> keywordProductIdFilter,
                                             Collection<Long> keywordWarehouseIdFilter,
                                             Map<Long, Set<Long>> batchKeywordStockKeyMap) {
        return selectPage(reqVO, productIdFilter, warehouseIdFilter, keywordProductIdFilter,
                keywordWarehouseIdFilter, batchKeywordStockKeyMap, null, null, null);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter,
                                             Collection<Long> warehouseIdFilter,
                                             Collection<Long> keywordProductIdFilter,
                                             Collection<Long> keywordWarehouseIdFilter,
                                             Collection<Long> departmentWarehouseIds,
                                             Collection<Long> selfWarehouseIds,
                                             String selfCreator) {
        return selectPage(reqVO, productIdFilter, warehouseIdFilter, keywordProductIdFilter,
                keywordWarehouseIdFilter, null, departmentWarehouseIds, selfWarehouseIds, selfCreator);
    }

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO, Collection<Long> productIdFilter,
                                             Collection<Long> warehouseIdFilter,
                                             Collection<Long> keywordProductIdFilter,
                                             Collection<Long> keywordWarehouseIdFilter,
                                             Map<Long, Set<Long>> batchKeywordStockKeyMap,
                                             Collection<Long> departmentWarehouseIds,
                                             Collection<Long> selfWarehouseIds,
                                             String selfCreator) {
        QueryWrapperX<ErpStockDO> wrapper = buildStockQueryWrapper(reqVO, productIdFilter, warehouseIdFilter,
                keywordProductIdFilter, keywordWarehouseIdFilter, batchKeywordStockKeyMap,
                departmentWarehouseIds, selfWarehouseIds, selfCreator);
        if (wrapper == null) {
            return PageResult.empty(0L);
        }
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpStockDO> selectPageOrderByAvailableCount(ErpStockPageReqVO reqVO,
                                                                   Collection<Long> productIdFilter,
                                                                   Collection<Long> warehouseIdFilter,
                                                                   Collection<Long> keywordProductIdFilter,
                                                                   Collection<Long> keywordWarehouseIdFilter,
                                                                   Map<Long, Set<Long>> batchKeywordStockKeyMap,
                                                                   Collection<Long> departmentWarehouseIds,
                                                                   Collection<Long> selfWarehouseIds,
                                                                   String selfCreator) {
        QueryWrapperX<ErpStockDO> wrapper = buildStockQueryWrapper(reqVO, productIdFilter, warehouseIdFilter,
                keywordProductIdFilter, keywordWarehouseIdFilter, batchKeywordStockKeyMap,
                departmentWarehouseIds, selfWarehouseIds, selfCreator);
        if (wrapper == null) {
            return PageResult.empty(0L);
        }
        String orderDirection = normalizeOrderDirection(reqVO.getOrderDirection());
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            List<ErpStockDO> list = selectListOrderByAvailableCount(wrapper, orderDirection);
            return new PageResult<>(list, (long) list.size());
        }
        IPage<ErpStockDO> page = MyBatisUtils.buildPage(reqVO);
        IPage<ErpStockDO> pageResult = selectPageOrderByAvailableCount(page, wrapper, orderDirection);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal());
    }

    @SelectProvider(type = ErpStockMapper.class, method = "availableCountSortedSql")
    IPage<ErpStockDO> selectPageOrderByAvailableCount(IPage<ErpStockDO> page,
                                                      @Param("ew") QueryWrapperX<ErpStockDO> wrapper,
                                                      @Param("orderDirection") String orderDirection);

    @SelectProvider(type = ErpStockMapper.class, method = "availableCountSortedSql")
    List<ErpStockDO> selectListOrderByAvailableCount(@Param("ew") QueryWrapperX<ErpStockDO> wrapper,
                                                     @Param("orderDirection") String orderDirection);

    default QueryWrapperX<ErpStockDO> buildStockQueryWrapper(ErpStockPageReqVO reqVO,
                                                            Collection<Long> productIdFilter,
                                                            Collection<Long> warehouseIdFilter,
                                                            Collection<Long> keywordProductIdFilter,
                                                            Collection<Long> keywordWarehouseIdFilter,
                                                            Map<Long, Set<Long>> batchKeywordStockKeyMap,
                                                            Collection<Long> departmentWarehouseIds,
                                                            Collection<Long> selfWarehouseIds,
                                                            String selfCreator) {
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<ErpStockDO>()
                .eqIfPresent("product_id", reqVO.getProductId())
                .eqIfPresent("warehouse_id", reqVO.getWarehouseId())
                .likeIfPresent("shelf", fuzzyKeyword(reqVO.getShelf()))
                .geIfPresent("count", reqVO.getCountMin())
                .leIfPresent("count", reqVO.getCountMax());
        if (productIdFilter != null) {
            if (productIdFilter.isEmpty()) {
                return null;
            }
            wrapper.in("product_id", productIdFilter);
        }
        if (warehouseIdFilter != null) {
            if (warehouseIdFilter.isEmpty()) {
                return null;
            }
            wrapper.in("warehouse_id", warehouseIdFilter);
        }
        if (keywordProductIdFilter != null || keywordWarehouseIdFilter != null) {
            if (!appendKeywordCondition(wrapper, keywordProductIdFilter, keywordWarehouseIdFilter,
                    batchKeywordStockKeyMap)) {
                return null;
            }
        }
        if (selfCreator != null) {
            if (isEmpty(departmentWarehouseIds) && isEmpty(selfWarehouseIds)) {
                return null;
            }
            wrapper.and(permission -> {
                boolean hasDepartmentScope = !isEmpty(departmentWarehouseIds);
                if (hasDepartmentScope) {
                    permission.in("warehouse_id", departmentWarehouseIds);
                }
                if (!isEmpty(selfWarehouseIds)) {
                    if (hasDepartmentScope) {
                        permission.or();
                    }
                    permission.nested(self -> self.in("warehouse_id", selfWarehouseIds)
                            .eq("creator", selfCreator));
                }
            });
        }
        // 库存数筛选
        if (reqVO.getCountFilter() != null) {
            if (reqVO.getCountFilter() == 1) {
                wrapper.gt("count", BigDecimal.ZERO);
            } else if (reqVO.getCountFilter() == 2) {
                wrapper.eq("count", BigDecimal.ZERO);
            }
        }
        if (Boolean.TRUE.equals(reqVO.getPositiveCountOnly())) {
            wrapper.gt("count", BigDecimal.ZERO);
        }
        if (Boolean.TRUE.equals(reqVO.getShelfEmptyOnly())) {
            wrapper.and(w -> w.isNull("shelf").or().eq("shelf", ""));
        }
        if (Boolean.TRUE.equals(reqVO.getShelfDuplicateOnly())) {
            wrapper.isNotNull("shelf")
                    .ne("shelf", "")
                    .exists("SELECT 1 FROM erp_stock s2 WHERE s2.deleted = b'0' "
                            + "AND s2.warehouse_id = erp_stock.warehouse_id "
                            + "AND s2.shelf = erp_stock.shelf "
                            + "AND s2.id <> erp_stock.id");
        }
        return wrapper;
    }

    default ErpStockSummaryRespVO selectSummary(ErpStockPageReqVO reqVO,
                                                Collection<Long> productIdFilter,
                                                Collection<Long> warehouseIdFilter,
                                                Collection<Long> keywordProductIdFilter,
                                                Collection<Long> keywordWarehouseIdFilter,
                                                Map<Long, Set<Long>> batchKeywordStockKeyMap,
                                                Collection<Long> departmentWarehouseIds,
                                                Collection<Long> selfWarehouseIds,
                                                String selfCreator) {
        QueryWrapperX<ErpStockDO> wrapper = buildStockQueryWrapper(reqVO, productIdFilter, warehouseIdFilter,
                keywordProductIdFilter, keywordWarehouseIdFilter, batchKeywordStockKeyMap,
                departmentWarehouseIds, selfWarehouseIds, selfCreator);
        if (wrapper == null) {
            return new ErpStockSummaryRespVO();
        }
        String currentPriceExpression = reqVO.getPriceSystemId() != null
                ? currentPriceExpression(reqVO.getPriceSystemId()) : productField("backup_price1");
        wrapper.select(
                "COUNT(*) AS total_rows",
                "COALESCE(SUM(COALESCE(count, 0)), 0) AS total_stock_count",
                "COALESCE(SUM(COALESCE(cost_amount, 0)), 0) AS total_cost_amount",
                "COALESCE(SUM(COALESCE(count, 0) * COALESCE(" + currentPriceExpression
                        + ", 0)), 0) AS total_current_price_amount",
                "COALESCE(SUM(COALESCE(" + pendingInCountExpression()
                        + ", 0)), 0) AS total_pending_in_count",
                "COALESCE(SUM(COALESCE(" + occupiedCountExpression()
                        + ", 0)), 0) AS total_occupied_count",
                "COALESCE(SUM(COALESCE(" + inTransitCountExpression()
                        + ", 0)), 0) AS total_in_transit_count",
                "COALESCE(SUM(COALESCE(count, 0) * COALESCE(" + productField("weight")
                        + ", 0)), 0) AS total_weight");
        List<Map<String, Object>> rows = selectMaps(wrapper);
        if (CollUtil.isEmpty(rows)) {
            return new ErpStockSummaryRespVO();
        }
        return buildSummary(rows.get(0));
    }

    static ErpStockSummaryRespVO buildSummary(Map<String, Object> row) {
        return new ErpStockSummaryRespVO()
                .setTotalRows(getLong(row, "total_rows"))
                .setTotalStockCount(getBigDecimal(row, "total_stock_count"))
                .setTotalCostAmount(getBigDecimal(row, "total_cost_amount"))
                .setTotalCurrentPriceAmount(getBigDecimal(row, "total_current_price_amount"))
                .setTotalPendingInCount(getBigDecimal(row, "total_pending_in_count"))
                .setTotalOccupiedCount(getBigDecimal(row, "total_occupied_count"))
                .setTotalInTransitCount(getBigDecimal(row, "total_in_transit_count"))
                .setTotalWeight(getBigDecimal(row, "total_weight"));
    }

    static Long getLong(Map<String, Object> row, String key) {
        if (row == null) {
            return 0L;
        }
        Object value = row.get(key);
        return value == null ? 0L : Long.valueOf(value.toString());
    }

    static BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        if (row == null) {
            return BigDecimal.ZERO;
        }
        Object value = row.get(key);
        return value == null ? BigDecimal.ZERO
                : value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    /**
     * 关键词命中产品、仓库，或在批次展开模式下命中库存流水中的批次号。
     */
    static boolean appendKeywordCondition(QueryWrapperX<ErpStockDO> wrapper,
                                          Collection<Long> keywordProductIdFilter,
                                          Collection<Long> keywordWarehouseIdFilter,
                                          Map<Long, Set<Long>> batchKeywordStockKeyMap) {
        boolean hasProductKeyword = !isEmpty(keywordProductIdFilter);
        boolean hasWarehouseKeyword = !isEmpty(keywordWarehouseIdFilter);
        boolean hasBatchKeyword = batchKeywordStockKeyMap != null && !batchKeywordStockKeyMap.isEmpty();
        if (!hasProductKeyword && !hasWarehouseKeyword && !hasBatchKeyword) {
            return false;
        }
        wrapper.and(w -> {
            boolean hasCondition = false;
            if (hasProductKeyword) {
                w.in("product_id", keywordProductIdFilter);
                hasCondition = true;
            }
            if (hasWarehouseKeyword) {
                if (hasCondition) {
                    w.or();
                }
                w.in("warehouse_id", keywordWarehouseIdFilter);
                hasCondition = true;
            }
            if (hasBatchKeyword) {
                if (hasCondition) {
                    w.or();
                }
                w.nested(batch -> {
                    boolean hasBatchPair = false;
                    for (Map.Entry<Long, Set<Long>> entry : batchKeywordStockKeyMap.entrySet()) {
                        if (entry.getKey() == null || isEmpty(entry.getValue())) {
                            continue;
                        }
                        if (hasBatchPair) {
                            batch.or();
                        }
                        batch.nested(pair -> pair.eq("product_id", entry.getKey())
                                .in("warehouse_id", entry.getValue()));
                        hasBatchPair = true;
                    }
                });
            }
        });
        return true;
    }

    static void orderByIfPresent(QueryWrapperX<ErpStockDO> wrapper, ErpStockPageReqVO reqVO) {
        String orderExpression = getOrderExpression(reqVO.getOrderField(), reqVO.getPriceSystemId());
        if (orderExpression == null) {
            defaultOrderBy(wrapper);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderExpression).orderByDesc("id");
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderExpression).orderByDesc("id");
            return;
        }
        defaultOrderBy(wrapper);
    }

    static boolean isAvailableCountSort(ErpStockPageReqVO reqVO) {
        return reqVO != null
                && reqVO.getOrderField() != null
                && "availableCount".equals(reqVO.getOrderField().trim())
                && isSupportedOrderDirection(reqVO.getOrderDirection());
    }

    static boolean isSupportedOrderDirection(String orderDirection) {
        return "asc".equalsIgnoreCase(orderDirection) || "desc".equalsIgnoreCase(orderDirection);
    }

    static String normalizeOrderDirection(String orderDirection) {
        return "asc".equalsIgnoreCase(orderDirection) ? "ASC" : "DESC";
    }

    static String availableCountSortedSql() {
        String candidateStockSql = candidateStockSql();
        return "<script>"
                + "SELECT erp_stock.* "
                + "FROM " + candidateStockSql + " erp_stock "
                + "LEFT JOIN ("
                + "  SELECT occupied_source.product_id, occupied_source.warehouse_id, "
                + "         SUM(occupied_source.occupied_count_delta) AS occupied_count "
                + "  FROM ("
                + saleCartOccupiedSql(candidateStockSql)
                + "    UNION ALL "
                + pendingAuditOccupiedSql(candidateStockSql, "erp_sale_out_items", "soi",
                "erp_sale_out", "so", "soi.out_id", "soi.warehouse_id",
                "COALESCE(soi.count, 0)")
                + "    UNION ALL "
                + pendingAuditOccupiedSql(candidateStockSql, "erp_purchase_return_items", "pri",
                "erp_purchase_return", "pr", "pri.return_id", "pri.warehouse_id",
                "COALESCE(pri.count, 0)")
                + "    UNION ALL "
                + pendingAuditOccupiedSql(candidateStockSql, "erp_stock_out_item", "soi2",
                "erp_stock_out", "so2", "soi2.out_id", "soi2.warehouse_id",
                "COALESCE(soi2.count, 0)")
                + "    UNION ALL "
                + stockTransferOutOccupiedSql(candidateStockSql)
                + "    UNION ALL "
                + warehouseMoveOccupiedSql(candidateStockSql)
                + "    UNION ALL "
                + stockCheckLessOccupiedSql(candidateStockSql)
                + "    UNION ALL "
                + stockOutBillOccupiedSql(candidateStockSql)
                + "  ) occupied_source "
                + "  GROUP BY occupied_source.product_id, occupied_source.warehouse_id"
                + ") occupied ON occupied.product_id = erp_stock.product_id "
                + "AND occupied.warehouse_id = erp_stock.warehouse_id "
                + "ORDER BY (COALESCE(erp_stock.count, 0) - COALESCE(occupied.occupied_count, 0)) "
                + "${orderDirection}, erp_stock.id DESC"
                + "</script>";
    }

    static String candidateStockSql() {
        return "(SELECT * FROM erp_stock "
                + "WHERE deleted = 0 "
                + "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != \"\"'>"
                + " AND ${ew.sqlSegment}"
                + "</if>"
                + ")";
    }

    static String saleCartOccupiedSql(String candidateStockSql) {
        return "    SELECT sci.product_id, sci.warehouse_id, SUM(COALESCE(sci.count, 0)) AS occupied_count_delta "
                + "    FROM erp_sale_cart_items sci "
                + "    INNER JOIN erp_sale_cart sc ON sc.id = sci.cart_id "
                + "    AND sc.deleted = 0 "
                + "    AND sc.status IN (" + ErpSaleCartStatusEnum.SUBMITTED.getStatus() + ","
                + ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus() + ") "
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = sci.product_id "
                + "    AND candidate_stock.warehouse_id = sci.warehouse_id "
                + "    WHERE sci.deleted = 0 "
                + "    AND NOT EXISTS (SELECT 1 FROM erp_sale_out so WHERE so.deleted = 0 AND so.tenant_id=sc.tenant_id AND so.source_type=30 AND so.source_id = sc.id) "
                + "    GROUP BY sci.product_id, sci.warehouse_id ";
    }

    static String pendingAuditOccupiedSql(String candidateStockSql, String itemTable, String itemAlias,
                                          String mainTable, String mainAlias, String itemForeignKey,
                                          String warehouseColumn, String countExpression) {
        return "    SELECT " + itemAlias + ".product_id, " + warehouseColumn
                + " AS warehouse_id, SUM(" + countExpression + ") AS occupied_count_delta "
                + "    FROM " + itemTable + " " + itemAlias + " "
                + "    INNER JOIN " + mainTable + " " + mainAlias
                + " ON " + mainAlias + ".id = " + itemForeignKey
                + " AND " + mainAlias + ".deleted = 0"
                + " AND " + mainAlias + ".status = " + ErpAuditStatus.PROCESS.getStatus()
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = " + itemAlias + ".product_id "
                + "    AND candidate_stock.warehouse_id = " + warehouseColumn + " "
                + "    WHERE " + itemAlias + ".deleted = 0 "
                + "    GROUP BY " + itemAlias + ".product_id, " + warehouseColumn + " ";
    }

    static String stockTransferOutOccupiedSql(String candidateStockSql) {
        return "    SELECT smi.product_id, smi.from_warehouse_id AS warehouse_id, "
                + "SUM(COALESCE(smi.count, 0)) AS occupied_count_delta "
                + "    FROM erp_stock_move_item smi "
                + "    INNER JOIN erp_stock_move sm ON sm.id = smi.move_id "
                + "    AND sm.deleted = 0 "
                + "    AND sm.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sm.transfer_direction = " + ErpStockTransferDirectionEnum.TRANSFER_OUT.getDirection() + " "
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = smi.product_id "
                + "    AND candidate_stock.warehouse_id = smi.from_warehouse_id "
                + "    WHERE smi.deleted = 0 "
                + "    GROUP BY smi.product_id, smi.from_warehouse_id ";
    }

    static String warehouseMoveOccupiedSql(String candidateStockSql) {
        return "    SELECT wmi.product_id, wmi.from_warehouse_id AS warehouse_id, "
                + "SUM(COALESCE(wmi.count, 0)) AS occupied_count_delta "
                + "    FROM erp_warehouse_move_item wmi "
                + "    INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id "
                + "    AND wm.deleted = 0 "
                + "    AND wm.status = " + ErpAuditStatus.PROCESS.getStatus() + " "
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = wmi.product_id "
                + "    AND candidate_stock.warehouse_id = wmi.from_warehouse_id "
                + "    WHERE wmi.deleted = 0 "
                + "    GROUP BY wmi.product_id, wmi.from_warehouse_id ";
    }

    static String stockCheckLessOccupiedSql(String candidateStockSql) {
        return "    SELECT sci2.product_id, sci2.warehouse_id, "
                + "SUM(ABS(COALESCE(sci2.count, 0))) AS occupied_count_delta "
                + "    FROM erp_stock_check_item sci2 "
                + "    INNER JOIN erp_stock_check sc2 ON sc2.id = sci2.check_id "
                + "    AND sc2.deleted = 0 "
                + "    AND sc2.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sc2.check_type = " + ErpStockCheckTypeEnum.COUNT.getType() + " "
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = sci2.product_id "
                + "    AND candidate_stock.warehouse_id = sci2.warehouse_id "
                + "    WHERE sci2.deleted = 0 AND sci2.count &lt; 0 "
                + "    GROUP BY sci2.product_id, sci2.warehouse_id ";
    }

    static String stockOutBillOccupiedSql(String candidateStockSql) {
        return "    SELECT sobi.product_id, sobi.warehouse_id, "
                + "SUM(GREATEST(COALESCE(sobi.count, 0) - COALESCE(sobi.picked_count, 0), 0)) AS occupied_count_delta "
                + "    FROM erp_stock_out_bill_item sobi "
                + "    INNER JOIN erp_stock_out_bill sob ON sob.id = sobi.bill_id "
                + "    AND sob.deleted = 0 AND sob.status IN (10,20) "
                + "    INNER JOIN " + candidateStockSql + " candidate_stock "
                + "    ON candidate_stock.product_id = sobi.product_id "
                + "    AND candidate_stock.warehouse_id = sobi.warehouse_id "
                + "    WHERE sobi.deleted = 0 "
                + "    GROUP BY sobi.product_id, sobi.warehouse_id ";
    }

    static String getOrderExpression(String orderField, Long priceSystemId) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "productId":
                return "erp_stock.product_id";
            case "warehouseId":
                return "erp_stock.warehouse_id";
            case "deptId":
                return "erp_stock.dept_id";
            case "warehouseName":
                return "(SELECT w.name FROM erp_warehouse w WHERE w.id = erp_stock.warehouse_id AND w.deleted = b'0')";
            case "deptName":
                return "(SELECT d.name FROM system_dept d WHERE d.id = erp_stock.dept_id AND d.deleted = b'0')";
            case "shelf":
                return "erp_stock.shelf";
            case "count":
                return "erp_stock.count";
            case "purchasePrice":
                return "COALESCE(erp_stock.purchase_price, " + productField("last_purchase_price") + ")";
            case "lastPurchasePrice":
                return productField("last_purchase_price");
            case "salePrice":
                return productField("sale_price");
            case "referencePrice":
                return productField("reference_price");
            case "retailPrice":
                return productField("retail_price");
            case "occupiedCount":
                return "COALESCE(" + occupiedCountExpression() + ", 0)";
            case "availableCount":
                return availableCountExpression();
            case "pendingInCount":
                return "COALESCE(" + pendingInCountExpression() + ", 0)";
            case "inTransitCount":
                return "COALESCE(" + inTransitCountExpression() + ", 0)";
            case "costPrice":
                return "erp_stock.cost_price";
            case "costAmount":
                return "erp_stock.cost_amount";
            case "productName":
                return productField("name");
            case "productCode":
                return productField("code");
            case "drawingNo":
                return productField("drawing_no");
            case "standard":
                return productField("standard");
            case "featureCode":
                return productField("feature_code");
            case "vehicleModel":
                return productField("vehicle_model");
            case "brand":
                return productField("brand");
            case "originPlace":
                return productField("origin_place");
            case "factoryCode":
                return productField("factory_code");
            case "stockMin":
                return productField("stock_min");
            case "stockMax":
                return productField("stock_max");
            case "stockStandard":
                return productField("stock_standard");
            case "unitName":
                return productUnitNameExpression();
            case "categoryName":
                return productCategoryNameExpression();
            case "currentPrice":
                return currentPriceExpression(priceSystemId);
            case "currentPriceAmount":
                return "COALESCE(" + currentPriceExpression(priceSystemId) + ", 0) * COALESCE(erp_stock.count, 0)";
            default:
                return null;
        }
    }

    static String productField(String column) {
        return "(SELECT p." + column + " FROM erp_product p "
                + "WHERE p.id = erp_stock.product_id AND p.deleted = b'0')";
    }

    static String productUnitNameExpression() {
        return "(SELECT u.name FROM erp_product p "
                + "LEFT JOIN erp_product_unit u ON u.id = p.unit_id AND u.deleted = b'0' "
                + "WHERE p.id = erp_stock.product_id AND p.deleted = b'0')";
    }

    static String productCategoryNameExpression() {
        return "(SELECT c.name FROM erp_product p "
                + "LEFT JOIN erp_product_category c ON c.id = p.category_id AND c.deleted = b'0' "
                + "WHERE p.id = erp_stock.product_id AND p.deleted = b'0')";
    }

    static String occupiedCountExpression() {
        String cart = "(SELECT SUM(COALESCE(sci.count, 0)) "
                + "FROM erp_sale_cart_items sci "
                + "INNER JOIN erp_sale_cart sc ON sc.id = sci.cart_id "
                + "AND sc.deleted = b'0' "
                + "AND sc.status IN (" + ErpSaleCartStatusEnum.SUBMITTED.getStatus()
                + "," + ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus() + ") "
                + "WHERE sci.deleted = b'0' "
                + "AND sci.product_id = erp_stock.product_id "
                + "AND sci.warehouse_id = erp_stock.warehouse_id "
                + "AND NOT EXISTS (SELECT 1 FROM erp_sale_out so WHERE so.deleted = b'0' "
                + "AND so.tenant_id=sc.tenant_id AND so.source_type=30 AND so.source_id = sc.id))";
        String saleOut = pendingAuditSum("erp_sale_out_items", "soi", "erp_sale_out", "so",
                "soi.out_id", "soi.warehouse_id", "COALESCE(soi.count, 0)");
        String purchaseReturn = pendingAuditSum("erp_purchase_return_items", "pri", "erp_purchase_return", "pr",
                "pri.return_id", "pri.warehouse_id", "COALESCE(pri.count, 0)");
        String stockOut = pendingAuditSum("erp_stock_out_item", "soi2", "erp_stock_out", "so2",
                "soi2.out_id", "soi2.warehouse_id", "COALESCE(soi2.count, 0)");
        String transferOut = "(SELECT SUM(COALESCE(smi.count, 0)) FROM erp_stock_move_item smi "
                + "INNER JOIN erp_stock_move sm ON sm.id = smi.move_id AND sm.deleted = b'0' "
                + "AND sm.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sm.transfer_direction = " + ErpStockTransferDirectionEnum.TRANSFER_OUT.getDirection() + " "
                + "WHERE smi.deleted = b'0' AND smi.product_id = erp_stock.product_id "
                + "AND smi.from_warehouse_id = erp_stock.warehouse_id)";
        String warehouseMove = "(SELECT SUM(COALESCE(wmi.count, 0)) FROM erp_warehouse_move_item wmi "
                + "INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id AND wm.deleted = b'0' "
                + "AND wm.status = " + ErpAuditStatus.PROCESS.getStatus() + " "
                + "WHERE wmi.deleted = b'0' AND wmi.product_id = erp_stock.product_id "
                + "AND wmi.from_warehouse_id = erp_stock.warehouse_id)";
        String checkLess = "(SELECT SUM(ABS(COALESCE(sci2.count, 0))) FROM erp_stock_check_item sci2 "
                + "INNER JOIN erp_stock_check sc2 ON sc2.id = sci2.check_id AND sc2.deleted = b'0' "
                + "AND sc2.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sc2.check_type = " + ErpStockCheckTypeEnum.COUNT.getType() + " "
                + "WHERE sci2.deleted = b'0' AND sci2.count < 0 "
                + "AND sci2.product_id = erp_stock.product_id AND sci2.warehouse_id = erp_stock.warehouse_id)";
        String outBill = "(SELECT SUM(GREATEST(COALESCE(sobi.count, 0) - COALESCE(sobi.picked_count, 0), 0)) "
                + "FROM erp_stock_out_bill_item sobi INNER JOIN erp_stock_out_bill sob "
                + "ON sob.id = sobi.bill_id AND sob.deleted = b'0' AND sob.status IN (10,20) "
                + "WHERE sobi.deleted = b'0' AND sobi.product_id = erp_stock.product_id "
                + "AND sobi.warehouse_id = erp_stock.warehouse_id)";
        return addNullableCounts(cart, saleOut, purchaseReturn, stockOut, transferOut, warehouseMove, checkLess, outBill);
    }

    static String availableCountExpression() {
        return "(COALESCE(erp_stock.count, 0) - COALESCE(" + occupiedCountExpression() + ", 0))";
    }

    static String pendingInCountExpression() {
        String purchaseIn = pendingAuditSum("erp_purchase_in_items", "pii", "erp_purchase_in", "pi",
                "pii.in_id", "pii.warehouse_id", "COALESCE(pii.count, 0)");
        String saleReturn = pendingAuditSum("erp_sale_return_items", "sri", "erp_sale_return", "sr",
                "sri.return_id", "sri.warehouse_id", "COALESCE(sri.count, 0)");
        String stockIn = pendingAuditSum("erp_stock_in_item", "sii", "erp_stock_in", "si",
                "sii.in_id", "sii.warehouse_id", "COALESCE(sii.count, 0)");
        String transferIn = "(SELECT SUM(COALESCE(smi.count, 0)) FROM erp_stock_move_item smi "
                + "INNER JOIN erp_stock_move sm ON sm.id = smi.move_id AND sm.deleted = b'0' "
                + "AND sm.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sm.transfer_direction = " + ErpStockTransferDirectionEnum.TRANSFER_IN.getDirection() + " "
                + "WHERE smi.deleted = b'0' AND smi.product_id = erp_stock.product_id "
                + "AND smi.to_warehouse_id = erp_stock.warehouse_id)";
        String warehouseMove = "(SELECT SUM(COALESCE(wmi.count, 0)) FROM erp_warehouse_move_item wmi "
                + "INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id AND wm.deleted = b'0' "
                + "AND wm.status = " + ErpAuditStatus.PROCESS.getStatus() + " "
                + "WHERE wmi.deleted = b'0' AND wmi.product_id = erp_stock.product_id "
                + "AND wmi.to_warehouse_id = erp_stock.warehouse_id)";
        String checkMore = "(SELECT SUM(COALESCE(sci.count, 0)) FROM erp_stock_check_item sci "
                + "INNER JOIN erp_stock_check sc ON sc.id = sci.check_id AND sc.deleted = b'0' "
                + "AND sc.status = " + ErpAuditStatus.PROCESS.getStatus()
                + " AND sc.check_type = " + ErpStockCheckTypeEnum.COUNT.getType() + " "
                + "WHERE sci.deleted = b'0' AND sci.count > 0 "
                + "AND sci.product_id = erp_stock.product_id AND sci.warehouse_id = erp_stock.warehouse_id)";
        String inBill = "(SELECT SUM(GREATEST(COALESCE(sibi.count, 0) - COALESCE(sibi.picked_count, 0), 0)) "
                + "FROM erp_stock_in_bill_item sibi INNER JOIN erp_stock_in_bill sib "
                + "ON sib.id = sibi.bill_id AND sib.deleted = b'0' AND sib.status IN (10,20) "
                + "WHERE sibi.deleted = b'0' AND sibi.product_id = erp_stock.product_id "
                + "AND sibi.warehouse_id = erp_stock.warehouse_id)";
        return addNullableCounts(purchaseIn, saleReturn, stockIn, transferIn, warehouseMove, checkMore, inBill);
    }

    static String pendingAuditSum(String itemTable, String itemAlias, String mainTable, String mainAlias,
                                  String itemForeignKey, String warehouseColumn, String countExpression) {
        return "(SELECT SUM(" + countExpression + ") FROM " + itemTable + " " + itemAlias
                + " INNER JOIN " + mainTable + " " + mainAlias + " ON " + mainAlias + ".id = " + itemForeignKey
                + " AND " + mainAlias + ".deleted = b'0' AND " + mainAlias + ".status = "
                + ErpAuditStatus.PROCESS.getStatus() + " WHERE " + itemAlias + ".deleted = b'0' AND "
                + itemAlias + ".product_id = erp_stock.product_id AND " + warehouseColumn
                + " = erp_stock.warehouse_id)";
    }

    static String addNullableCounts(String... expressions) {
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < expressions.length; i++) {
            if (i > 0) {
                result.append(" + ");
            }
            result.append("COALESCE(").append(expressions[i]).append(", 0)");
        }
        return result.append(")").toString();
    }

    default Map<String, BigDecimal> selectPendingInCountMap(Collection<Long> productIds,
                                                            Collection<Long> warehouseIds) {
        return selectStockChangeCountMap(productIds, warehouseIds, pendingInCountExpression(), "pending_count");
    }

    default Map<String, BigDecimal> selectOccupiedCountMap(Collection<Long> productIds,
                                                           Collection<Long> warehouseIds) {
        return selectStockChangeCountMap(productIds, warehouseIds, occupiedCountExpression(), "occupied_count");
    }

    default Map<String, BigDecimal> selectStockChangeCountMap(Collection<Long> productIds,
                                                              Collection<Long> warehouseIds,
                                                              String expression, String alias) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)) {
            return result;
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("product_id", "warehouse_id", expression + " AS " + alias)
                .in("product_id", productIds)
                .in("warehouse_id", warehouseIds));
        for (Map<String, Object> row : rows) {
            Long productId = MapUtil.getLong(row, "product_id");
            Long warehouseId = MapUtil.getLong(row, "warehouse_id");
            Object value = row.get(alias);
            if (productId == null || warehouseId == null || value == null) {
                continue;
            }
            result.put(productId + "_" + warehouseId, value instanceof BigDecimal
                    ? (BigDecimal) value : new BigDecimal(value.toString()));
        }
        return result;
    }

    static String inTransitCountExpression() {
        return "(SELECT SUM(GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0)) "
                + "FROM erp_purchase_order_items poi "
                + "INNER JOIN erp_purchase_order po ON po.id = poi.order_id "
                + "AND po.deleted = b'0' "
                + "AND po.status = " + ErpAuditStatus.APPROVE.getStatus() + " "
                + "WHERE poi.deleted = b'0' "
                + "AND poi.product_id = erp_stock.product_id "
                + "AND poi.warehouse_id = erp_stock.warehouse_id)";
    }

    static String currentPriceExpression(Long priceSystemId) {
        if (priceSystemId == null) {
            return "NULL";
        }
        return "(SELECT pps.price FROM erp_product_price_system pps "
                + "WHERE pps.deleted = b'0' "
                + "AND pps.product_id = erp_stock.product_id "
                + "AND pps.price_system_id = " + priceSystemId
                + " LIMIT 1)";
    }

    static void defaultOrderBy(QueryWrapperX<ErpStockDO> wrapper) {
        wrapper.orderByAsc("warehouse_id")
                .orderByDesc("id");
    }

    static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    static String fuzzyKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    default ErpStockDO selectByProductIdAndWarehouseIdForUpdate(Long productId, Long warehouseId) {
        return selectOne(new LambdaQueryWrapperX<ErpStockDO>().eq(ErpStockDO::getProductId, productId)
                .eq(ErpStockDO::getWarehouseId, warehouseId).last("FOR UPDATE"));
    }

    default ErpStockDO selectByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return selectOne(ErpStockDO::getProductId, productId,
                ErpStockDO::getWarehouseId, warehouseId);
    }

    default List<ErpStockDO> selectListByProductIdsAndWarehouseIds(Collection<Long> productIds,
                                                                   Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockDO>()
                .in(ErpStockDO::getProductId, productIds)
                .in(ErpStockDO::getWarehouseId, warehouseIds));
    }

    default List<ErpStockDO> selectListByProductId(Long productId) {
        return selectList(ErpStockDO::getProductId, productId);
    }

    default List<ErpStockDO> selectListByProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockDO>()
                .in(ErpStockDO::getProductId, productIds));
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockDO::getWarehouseId, warehouseId);
    }

    default Long selectNonZeroCountByWarehouseId(Long warehouseId) {
        return selectCount(new LambdaQueryWrapperX<ErpStockDO>()
                .eq(ErpStockDO::getWarehouseId, warehouseId)
                .ne(ErpStockDO::getCount, BigDecimal.ZERO));
    }

    default int updateDeptIdByWarehouseId(Long warehouseId, Long deptId) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getWarehouseId, warehouseId)
                .set(ErpStockDO::getDeptId, deptId));
    }

    default int updateShelfByIds(Collection<Long> ids, String shelf) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .in(ErpStockDO::getId, ids)
                .set(ErpStockDO::getShelf, shelf));
    }

    default int updateShelfById(Long id, String shelf) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getShelf, shelf));
    }

    default int updatePurchasePriceById(Long id, BigDecimal purchasePrice) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getPurchasePrice, purchasePrice));
    }

    default int updateOccupiedCountById(Long id, BigDecimal occupiedCount) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getOccupiedCount, occupiedCount));
    }

    /** 原子增加库存锁定数，仅在剩余可用库存充足时成功。 */
    default int tryIncreaseLockCount(Long id, BigDecimal count) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .apply("count - COALESCE(lock_count, 0) >= {0}", count)
                .setSql("lock_count = COALESCE(lock_count, 0) + " + count.toPlainString()));
    }

    /** 原子减少库存锁定数，禁止减成负数。 */
    default int tryDecreaseLockCount(Long id, BigDecimal count) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .apply("COALESCE(lock_count, 0) >= {0}", count)
                .setSql("lock_count = COALESCE(lock_count, 0) - " + count.toPlainString()));
    }

    default int updatePendingInCountById(Long id, BigDecimal pendingInCount) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getPendingInCount, pendingInCount));
    }

    default int updateInTransitCountById(Long id, BigDecimal inTransitCount) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getInTransitCount, inTransitCount));
    }

    default int updateCostById(Long id, BigDecimal costPrice, BigDecimal costAmount) {
        return update(null, new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .set(ErpStockDO::getCostPrice, costPrice)
                .set(ErpStockDO::getCostAmount, costAmount));
    }

    default int updateCountIncrement(Long id, BigDecimal count, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id);
        if (count.compareTo(BigDecimal.ZERO) > 0) {
            updateWrapper.setSql("count = count + " + count);
        } else if (count.compareTo(BigDecimal.ZERO) < 0) {
            if (!negativeEnable) {
                updateWrapper.ge(ErpStockDO::getCount, count.abs());
            }
            updateWrapper.setSql("count = count - " + count.abs());
        }
        return update(null, updateWrapper);
    }

    /**
     * 原子更新库存的 count 与 costPrice、costAmount
     * 采用乐观锁：仅当 count == expectedOldCount 时才更新，避免并发写成本时覆盖
     *
     * @return 更新行数（0 表示并发冲突，需上层重试）
     */
    default int updateCountAndCost(Long id, BigDecimal expectedOldCount, BigDecimal newCount,
                                   BigDecimal newCostPrice, BigDecimal newCostAmount, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .eq(ErpStockDO::getCount, expectedOldCount)
                .set(ErpStockDO::getCount, newCount)
                .set(ErpStockDO::getCostPrice, newCostPrice)
                .set(ErpStockDO::getCostAmount, newCostAmount);
        if (!negativeEnable && newCount.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        return update(null, updateWrapper);
    }

    /**
     * 仅更新库存的 costAmount / costPrice（数量不变），采购调价专用。
     *
     * <p>以 count + costAmount 双条件作为乐观锁；并发修改将导致返回 0，上层需重试。</p>
     *
     * @param id                 库存主键
     * @param expectedCount      期望的当前库存数量（版本校验）
     * @param expectedCostAmount 期望的当前成本金额（版本校验）
     * @param newCostAmount      新成本金额
     * @param newCostPrice       新成本均价
     * @return 更新行数
     */
    default int updateCostAmountAndPrice(Long id, BigDecimal expectedCount, BigDecimal expectedCostAmount,
                                         BigDecimal newCostAmount, BigDecimal newCostPrice) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .eq(ErpStockDO::getCount, expectedCount)
                .eq(ErpStockDO::getCostAmount, expectedCostAmount)
                .set(ErpStockDO::getCostAmount, newCostAmount)
                .set(ErpStockDO::getCostPrice, newCostPrice);
        return update(null, updateWrapper);
    }

    /**
     * 仅更新库存成本价和成本金额，数量不变。
     */
    default int updateCostPriceAndAmount(Long id, BigDecimal expectedCount, BigDecimal expectedCostPrice,
                                         BigDecimal newCostPrice, BigDecimal newCostAmount) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id)
                .eq(ErpStockDO::getCount, expectedCount)
                .eq(ErpStockDO::getCostPrice, expectedCostPrice)
                .set(ErpStockDO::getCostPrice, newCostPrice)
                .set(ErpStockDO::getCostAmount, newCostAmount);
        return update(null, updateWrapper);
    }

    default BigDecimal selectSumByProductId(Long productId) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sum_count")
                .eq("product_id", productId));
        // 获得数量
        if (CollUtil.isEmpty(result)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(MapUtil.getDouble(result.get(0), "sum_count", 0D));
    }

    default BigDecimal selectSumByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sum_count")
                .eq("product_id", productId)
                .eq("warehouse_id", warehouseId));
        if (CollUtil.isEmpty(result)) {
            return BigDecimal.ZERO;
        }
        return getBigDecimal(result.get(0), "sum_count");
    }

    /**
     * 按 product_id 批量聚合库存数量
     *
     * @param productIds 产品编号集合
     * @return Map&lt;productId, sum(count)&gt;
     */
    default Map<Long, BigDecimal> selectSumMapByProductIds(Collection<Long> productIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (CollUtil.isEmpty(productIds)) {
            return map;
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("product_id AS product_id, SUM(count) AS sum_count")
                .in("product_id", productIds)
                .groupBy("product_id"));
        if (CollUtil.isEmpty(rows)) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Long productId = MapUtil.getLong(row, "product_id");
            if (productId == null) {
                continue;
            }
            map.put(productId, BigDecimal.valueOf(MapUtil.getDouble(row, "sum_count", 0D)));
        }
        return map;
    }

}
