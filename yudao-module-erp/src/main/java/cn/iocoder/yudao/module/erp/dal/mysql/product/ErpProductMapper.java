package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERP 产品 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProductMapper extends BaseMapperX<ErpProductDO> {

    String KEYWORD_FIELD_CODE = "code";
    String KEYWORD_FIELD_NAME = "name";
    String KEYWORD_FIELD_PINYIN_CODE = "pinyinCode";
    String KEYWORD_FIELD_WUBI_CODE = "wubiCode";
    String KEYWORD_FIELD_BAR_CODE = "barCode";
    String KEYWORD_FIELD_VEHICLE_MODEL = "vehicleModel";
    String KEYWORD_FIELD_FACTORY_CODE = "factoryCode";
    String KEYWORD_FIELD_STANDARD = "standard";
    String KEYWORD_FIELD_REMARK = "remark";
    String KEYWORD_FIELD_BRAND = "brand";
    String KEYWORD_FIELD_OE_NUMBER = "oeNumber";
    String KEYWORD_FIELD_ORIGIN_PLACE = "originPlace";
    String KEYWORD_FIELD_FEATURE_CODE = "featureCode";
    String KEYWORD_FIELD_DRAWING_NO = "drawingNo";
    String KEYWORD_FIELD_SHELF = "shelf";
    String KEYWORD_FIELD_CATEGORY_NAME = "categoryName";
    String KEYWORD_FIELD_UNIT_NAME = "unitName";
    String KEYWORD_FIELD_DEFAULT_WAREHOUSE_NAME = "defaultWarehouseName";
    String KEYWORD_FIELD_DEPT_NAME = "deptName";
    String KEYWORD_FIELD_UNIVERSAL = "universal";
    String KEYWORD_FIELD_CREATE_TIME = "createTime";
    String EXPLICIT_KEYWORD_DELIMITER_REGEX = "[/\\\\,，;；+|]+";
    String SPACE_KEYWORD_DELIMITER_REGEX = "[\\s\\u3000]+";
    List<String> STOCK_PRODUCT_KEYWORD_FIELDS = java.util.Arrays.asList(
            KEYWORD_FIELD_CODE,
            KEYWORD_FIELD_NAME,
            KEYWORD_FIELD_PINYIN_CODE,
            KEYWORD_FIELD_WUBI_CODE,
            KEYWORD_FIELD_BAR_CODE,
            KEYWORD_FIELD_VEHICLE_MODEL,
            KEYWORD_FIELD_FACTORY_CODE,
            KEYWORD_FIELD_STANDARD,
            KEYWORD_FIELD_REMARK,
            KEYWORD_FIELD_BRAND,
            KEYWORD_FIELD_OE_NUMBER,
            KEYWORD_FIELD_ORIGIN_PLACE,
            KEYWORD_FIELD_FEATURE_CODE,
            KEYWORD_FIELD_DRAWING_NO,
            KEYWORD_FIELD_SHELF,
            KEYWORD_FIELD_UNIT_NAME);
    List<String> STOCK_KEYWORD_FIELDS = java.util.Arrays.asList(
            KEYWORD_FIELD_CODE,
            KEYWORD_FIELD_NAME,
            KEYWORD_FIELD_PINYIN_CODE,
            KEYWORD_FIELD_WUBI_CODE,
            KEYWORD_FIELD_BAR_CODE,
            KEYWORD_FIELD_VEHICLE_MODEL,
            KEYWORD_FIELD_FACTORY_CODE,
            KEYWORD_FIELD_STANDARD,
            KEYWORD_FIELD_REMARK,
            KEYWORD_FIELD_BRAND,
            KEYWORD_FIELD_OE_NUMBER,
            KEYWORD_FIELD_ORIGIN_PLACE,
            KEYWORD_FIELD_FEATURE_CODE,
            KEYWORD_FIELD_DRAWING_NO,
            KEYWORD_FIELD_SHELF,
            KEYWORD_FIELD_UNIT_NAME,
            KEYWORD_FIELD_DEPT_NAME,
            KEYWORD_FIELD_CREATE_TIME);

    @Select("<script>" +
            "SELECT id" +
            "<foreach collection='columns' item='column'>, `${column}`</foreach>" +
            " FROM erp_product WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectCustomFieldMaps(@Param("ids") Collection<Long> ids,
                                                    @Param("columns") Collection<String> columns);

    @Update("<script>" +
            "UPDATE erp_product SET " +
            "<foreach collection='values' item='value' index='column' separator=','>`${column}` = #{value}</foreach>" +
            " WHERE id = #{id}" +
            "</script>")
    int updateCustomFields(@Param("id") Long id, @Param("values") Map<String, Object> values);

    default PageResult<ErpProductDO> selectPage(ErpProductPageReqVO reqVO) {
        return selectPage(reqVO, java.util.Arrays.asList(
                KEYWORD_FIELD_CODE,
                KEYWORD_FIELD_NAME,
                KEYWORD_FIELD_PINYIN_CODE,
                KEYWORD_FIELD_WUBI_CODE,
                KEYWORD_FIELD_BAR_CODE,
                KEYWORD_FIELD_VEHICLE_MODEL,
                KEYWORD_FIELD_FACTORY_CODE,
                KEYWORD_FIELD_STANDARD,
                KEYWORD_FIELD_REMARK,
                KEYWORD_FIELD_BRAND,
                KEYWORD_FIELD_OE_NUMBER,
                KEYWORD_FIELD_ORIGIN_PLACE,
                KEYWORD_FIELD_FEATURE_CODE,
                KEYWORD_FIELD_DRAWING_NO,
                KEYWORD_FIELD_SHELF,
                KEYWORD_FIELD_CATEGORY_NAME,
                KEYWORD_FIELD_UNIT_NAME,
                KEYWORD_FIELD_DEFAULT_WAREHOUSE_NAME,
                KEYWORD_FIELD_DEPT_NAME,
                KEYWORD_FIELD_UNIVERSAL,
                KEYWORD_FIELD_CREATE_TIME), Collections.emptyList());
    }

    default PageResult<ErpProductDO> selectPage(ErpProductPageReqVO reqVO,
                                                Collection<String> keywordFields,
                                                Collection<String> customKeywordColumns) {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<ErpProductDO>();
        wrapper
                .likeIfPresent(ErpProductDO::getName, fuzzyKeyword(reqVO.getName()))
                .likeIfPresent(ErpProductDO::getCode, fuzzyKeyword(reqVO.getCode()))
                .likeIfPresent(ErpProductDO::getBrand, fuzzyKeyword(reqVO.getBrand()))
                .likeIfPresent(ErpProductDO::getVehicleModel, fuzzyKeyword(reqVO.getVehicleModel()))
                .likeIfPresent(ErpProductDO::getFactoryCode, fuzzyKeyword(reqVO.getFactoryCode()))
                .eqIfPresent(ErpProductDO::getCategoryId, reqVO.getCategoryId())
                .inIfPresent(ErpProductDO::getCategoryId, reqVO.getCategoryIds())
                .eqIfPresent(ErpProductDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpProductDO::getDefaultWarehouseId, reqVO.getWarehouseId())
                .betweenIfPresent(ErpProductDO::getCreateTime, reqVO.getCreateTime());
        appendKeywordCondition(wrapper, reqVO.getKeyword(), keywordFields, customKeywordColumns);
        if (reqVO.getDeptId() != null) {
            wrapper.and(w -> w.eq(ErpProductDO::getDeptId, reqVO.getDeptId())
                    .or()
                    .exists("SELECT 1 FROM erp_product_dept epd "
                            + "WHERE epd.product_id = erp_product.id "
                            + "AND epd.deleted = b'0' "
                            + "AND epd.dept_id = " + reqVO.getDeptId())
                    .or()
                    .exists("SELECT 1 FROM erp_stock s "
                            + "JOIN erp_warehouse w2 ON w2.id = s.warehouse_id AND w2.deleted = b'0' "
                            + "WHERE s.product_id = erp_product.id "
                            + "AND s.deleted = b'0' "
                            + "AND w2.dept_id = " + reqVO.getDeptId()));
        }
        applyVisibleScope(wrapper, reqVO);
        // 默认过滤掉已合并的配件
        // Filter merged products by default.
        wrapper.ne(ErpProductDO::getMergedFlag, Boolean.TRUE);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void appendKeywordCondition(LambdaQueryWrapperX<ErpProductDO> wrapper,
                                       String keyword,
                                       Collection<String> keywordFields,
                                       Collection<String> customKeywordColumns) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        KeywordSearch keywordSearch = parseKeywordSearch(keyword);
        if (keywordSearch.explicitDelimited()) {
            appendKeywordTokensAndCondition(wrapper, keywordSearch.tokens(), keywordFields, customKeywordColumns);
            return;
        }
        if (keywordSearch.spaceDelimited()) {
            wrapper.and(w -> {
                appendKeywordMatchGroup(w, fuzzyKeyword(keyword), keywordFields, customKeywordColumns);
                w.or(or -> appendKeywordTokensAndCondition(or, keywordSearch.tokens(), keywordFields, customKeywordColumns));
            });
            return;
        }
        wrapper.and(w -> appendKeywordMatchGroup(w, fuzzyKeyword(keyword), keywordFields, customKeywordColumns));
    }

    static KeywordSearch parseKeywordSearch(String keyword) {
        String rawKeyword = keyword.trim();
        List<String> explicitTokens = splitKeywordTokens(rawKeyword, EXPLICIT_KEYWORD_DELIMITER_REGEX);
        if (containsExplicitDelimiter(rawKeyword) && !explicitTokens.isEmpty()) {
            return new KeywordSearch(explicitTokens, true, false);
        }
        List<String> spaceTokens = splitKeywordTokens(rawKeyword, SPACE_KEYWORD_DELIMITER_REGEX);
        if (spaceTokens.size() > 1) {
            return new KeywordSearch(spaceTokens, false, true);
        }
        return new KeywordSearch(Collections.emptyList(), false, false);
    }

    static List<String> splitKeywordTokens(String keyword, String delimiterRegex) {
        return java.util.Arrays.stream(keyword.split(delimiterRegex))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    static boolean containsExplicitDelimiter(String keyword) {
        return keyword.indexOf('/') >= 0
                || keyword.indexOf('\\') >= 0
                || keyword.indexOf(',') >= 0
                || keyword.indexOf('，') >= 0
                || keyword.indexOf(';') >= 0
                || keyword.indexOf('；') >= 0
                || keyword.indexOf('+') >= 0
                || keyword.indexOf('|') >= 0;
    }

    static void appendKeywordTokensAndCondition(LambdaQueryWrapper<ErpProductDO> wrapper,
                                                List<String> tokens,
                                                Collection<String> keywordFields,
                                                Collection<String> customKeywordColumns) {
        tokens.forEach(token -> wrapper.and(w -> appendKeywordMatchGroup(w, fuzzyKeyword(token),
                keywordFields, customKeywordColumns)));
    }

    static void appendKeywordMatchGroup(LambdaQueryWrapper<ErpProductDO> wrapper,
                                        String keyword,
                                        Collection<String> keywordFields,
                                        Collection<String> customKeywordColumns) {
        boolean hasCondition = false;
        if (keywordFields.contains(KEYWORD_FIELD_CODE)) {
            wrapper.like(ErpProductDO::getCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_NAME)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getName, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_PINYIN_CODE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getPinyinCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_WUBI_CODE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getWubiCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_BAR_CODE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getBarCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_VEHICLE_MODEL)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getVehicleModel, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_FACTORY_CODE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getFactoryCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_STANDARD)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getStandard, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_REMARK)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getRemark, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_BRAND)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getBrand, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_OE_NUMBER)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getOeNumber, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_ORIGIN_PLACE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getOriginPlace, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_FEATURE_CODE)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getFeatureCode, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_DRAWING_NO)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getDrawingNo, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_SHELF)) {
            appendOr(wrapper, hasCondition).like(ErpProductDO::getShelf, keyword);
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_CREATE_TIME)) {
            appendOr(wrapper, hasCondition).apply("DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}", "%" + keyword + "%");
            hasCondition = true;
        }
        for (String column : customKeywordColumns) {
            if (!StringUtils.hasText(column)) {
                continue;
            }
            appendOr(wrapper, hasCondition).apply(column + " LIKE {0}", "%" + keyword + "%");
            hasCondition = true;
        }
        hasCondition = appendKeywordExists(wrapper, hasCondition, keyword, keywordFields);
        if (!hasCondition) {
            wrapper.apply("1 = 0");
        }
    }

    class KeywordSearch {

        private final List<String> tokens;
        private final boolean explicitDelimited;
        private final boolean spaceDelimited;

        KeywordSearch(List<String> tokens, boolean explicitDelimited, boolean spaceDelimited) {
            this.tokens = tokens;
            this.explicitDelimited = explicitDelimited;
            this.spaceDelimited = spaceDelimited;
        }

        List<String> tokens() {
            return tokens;
        }

        boolean explicitDelimited() {
            return explicitDelimited;
        }

        boolean spaceDelimited() {
            return spaceDelimited;
        }

    }

    static LambdaQueryWrapper<ErpProductDO> appendOr(LambdaQueryWrapper<ErpProductDO> wrapper,
                                                    boolean hasCondition) {
        return hasCondition ? wrapper.or() : wrapper;
    }

    static boolean appendKeywordExists(LambdaQueryWrapper<ErpProductDO> wrapper,
                                       boolean hasCondition,
                                       String keyword,
                                       Collection<String> keywordFields) {
        if (keywordFields.contains(KEYWORD_FIELD_CATEGORY_NAME)) {
            appendOr(wrapper, hasCondition).exists("SELECT 1 FROM erp_product_category c "
                    + "WHERE c.id = erp_product.category_id "
                    + "AND c.deleted = b'0' "
                    + "AND c.name LIKE {0}", "%" + keyword + "%");
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_UNIT_NAME)) {
            appendOr(wrapper, hasCondition).exists("SELECT 1 FROM erp_product_unit u "
                    + "WHERE u.id = erp_product.unit_id "
                    + "AND u.deleted = b'0' "
                    + "AND u.name LIKE {0}", "%" + keyword + "%");
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_DEFAULT_WAREHOUSE_NAME)) {
            appendOr(wrapper, hasCondition).exists("SELECT 1 FROM erp_warehouse wh "
                    + "WHERE wh.id = erp_product.default_warehouse_id "
                    + "AND wh.deleted = b'0' "
                    + "AND wh.name LIKE {0}", "%" + keyword + "%");
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_DEPT_NAME)) {
            appendOr(wrapper, hasCondition).exists("SELECT 1 FROM system_dept d "
                    + "WHERE d.deleted = b'0' "
                    + "AND d.name LIKE {0} "
                    + "AND (d.id = erp_product.dept_id "
                    + "OR EXISTS (SELECT 1 FROM erp_product_dept epd "
                    + "WHERE epd.product_id = erp_product.id "
                    + "AND epd.deleted = b'0' "
                    + "AND epd.dept_id = d.id))", "%" + keyword + "%");
            hasCondition = true;
        }
        if (keywordFields.contains(KEYWORD_FIELD_UNIVERSAL)) {
            appendOr(wrapper, hasCondition).exists("SELECT 1 FROM erp_product_universal epu "
                    + "WHERE epu.product_id = erp_product.id "
                    + "AND epu.deleted = b'0' "
                    + "AND (epu.universal_code LIKE {0} "
                    + "OR epu.universal_name LIKE {1} "
                    + "OR epu.universal_vehicle LIKE {2})",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            hasCondition = true;
        }
        return hasCondition;
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpProductDO> wrapper, ErpProductPageReqVO reqVO) {
        SFunction<ErpProductDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpProductDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByDesc(ErpProductDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByDesc(ErpProductDO::getId);
            return;
        }
        wrapper.orderByDesc(ErpProductDO::getId);
    }

    static SFunction<ErpProductDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "code":
                return ErpProductDO::getCode;
            case "name":
                return ErpProductDO::getName;
            case "pinyinCode":
                return ErpProductDO::getPinyinCode;
            case "wubiCode":
                return ErpProductDO::getWubiCode;
            case "vehicleModel":
                return ErpProductDO::getVehicleModel;
            case "standard":
                return ErpProductDO::getStandard;
            case "barCode":
                return ErpProductDO::getBarCode;
            case "factoryCode":
                return ErpProductDO::getFactoryCode;
            case "lastPurchasePrice":
                return ErpProductDO::getLastPurchasePrice;
            case "salePrice":
                return ErpProductDO::getSalePrice;
            case "brand":
                return ErpProductDO::getBrand;
            case "originPlace":
                return ErpProductDO::getOriginPlace;
            case "drawingNo":
                return ErpProductDO::getDrawingNo;
            case "shelf":
                return ErpProductDO::getShelf;
            case "retailPrice":
                return ErpProductDO::getRetailPrice;
            case "referencePrice":
                return ErpProductDO::getReferencePrice;
            case "sharePrice":
                return ErpProductDO::getSharePrice;
            case "status":
                return ErpProductDO::getStatus;
            case "createTime":
                return ErpProductDO::getCreateTime;
            case "updateTime":
                return ErpProductDO::getUpdateTime;
            default:
                return null;
        }
    }

    static String fuzzyKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(ErpProductDO::getCategoryId, categoryId);
    }

    default Long selectCountByUnitId(Long unitId) {
        return selectCount(ErpProductDO::getUnitId, unitId);
    }

    default Long selectCountByBrand(String brand) {
        return selectCount(ErpProductDO::getBrand, brand);
    }

    default Long selectCountByDefaultWarehouseId(Long warehouseId) {
        return selectCount(ErpProductDO::getDefaultWarehouseId, warehouseId);
    }

    default List<ErpProductDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .eq(ErpProductDO::getStatus, status)
                .ne(ErpProductDO::getMergedFlag, Boolean.TRUE));
    }

    default List<ErpProductDO> selectVisibleListByStatus(Integer status, ErpProductPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpProductDO::getStatus, status);
        wrapper.ne(ErpProductDO::getMergedFlag, Boolean.TRUE);
        wrapper.orderByDesc(ErpProductDO::getId);
        applyVisibleScope(wrapper, reqVO);
        return selectList(wrapper);
    }

    default List<ErpProductDO> selectVisibleSimpleListByStatus(Integer status, ErpProductPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.select(ErpProductDO::getId, ErpProductDO::getCode, ErpProductDO::getName,
                ErpProductDO::getPinyinCode, ErpProductDO::getWubiCode,
                ErpProductDO::getBarCode, ErpProductDO::getCategoryId, ErpProductDO::getUnitId,
                ErpProductDO::getPurchasePrice, ErpProductDO::getSalePrice, ErpProductDO::getMinPrice,
                ErpProductDO::getSharePrice);
        wrapper.eq(ErpProductDO::getStatus, status);
        wrapper.ne(ErpProductDO::getMergedFlag, Boolean.TRUE);
        wrapper.orderByDesc(ErpProductDO::getId);
        applyVisibleScope(wrapper, reqVO);
        return selectList(wrapper);
    }

    default ErpProductDO selectVisibleById(Long id, ErpProductPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpProductDO::getId, id);
        applyVisibleScope(wrapper, reqVO);
        return selectOne(wrapper);
    }

    default List<ErpProductDO> selectVisibleListByIds(Collection<Long> ids, ErpProductPageReqVO reqVO) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(ErpProductDO::getId, ids);
        applyVisibleScope(wrapper, reqVO);
        return selectList(wrapper);
    }

    static void applyVisibleScope(LambdaQueryWrapper<ErpProductDO> wrapper, ErpProductPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getVisibleAll())) {
            return;
        }
        Collection<Long> deptIds = reqVO.getVisibleDeptIds();
        Collection<Long> warehouseIds = reqVO.getVisibleWarehouseIds();
        Long selfUserId = reqVO.getVisibleSelfUserId();
        boolean hasDeptScope = CollUtil.isNotEmpty(deptIds);
        boolean hasWarehouseScope = CollUtil.isNotEmpty(warehouseIds);
        boolean hasSelfScope = selfUserId != null;
        if (!hasDeptScope && !hasWarehouseScope && !hasSelfScope) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(scope -> {
            boolean hasCondition = false;
            if (hasDeptScope) {
                scope.in(ErpProductDO::getDeptId, deptIds)
                        .or()
                        .exists("SELECT 1 FROM erp_product_dept epd "
                                + "WHERE epd.product_id = erp_product.id "
                                + "AND epd.deleted = b'0' "
                                + "AND epd.dept_id IN (" + CollUtil.join(deptIds, ",") + ")")
                        .or()
                        .exists("SELECT 1 FROM erp_stock s "
                                + "JOIN erp_warehouse w ON w.id = s.warehouse_id AND w.deleted = b'0' "
                                + "WHERE s.product_id = erp_product.id "
                                + "AND s.deleted = b'0' "
                                + "AND w.dept_id IN (" + CollUtil.join(deptIds, ",") + ")");
                hasCondition = true;
            }
            if (hasWarehouseScope) {
                appendOr(scope, hasCondition).exists("SELECT 1 FROM erp_stock s "
                        + "WHERE s.product_id = erp_product.id "
                        + "AND s.deleted = b'0' "
                        + "AND s.warehouse_id IN (" + CollUtil.join(warehouseIds, ",") + ")");
                hasCondition = true;
            }
            if (hasSelfScope) {
                appendOr(scope, hasCondition).eq(ErpProductDO::getCreator, String.valueOf(selfUserId));
            }
        });
    }

    default ErpProductDO selectByCode(String code) {
        return selectOne(ErpProductDO::getCode, code);
    }

    default ErpProductDO selectByCodeExcludeId(String code, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<ErpProductDO>()
                .eq(ErpProductDO::getCode, code)
                .neIfPresent(ErpProductDO::getId, excludeId));
    }

    default ErpProductDO selectByNameExcludeId(String name, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<ErpProductDO>()
                .eq(ErpProductDO::getName, name)
                .neIfPresent(ErpProductDO::getId, excludeId));
    }

    default List<String> selectCodesByPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }
        QueryWrapper<ErpProductDO> wrapper = new QueryWrapper<ErpProductDO>()
                .select("code")
                .likeRight("code", prefix);
        return selectList(wrapper).stream()
                .map(ErpProductDO::getCode)
                .collect(Collectors.toList());
    }

    default List<ErpProductDO> selectListByCodes(Collection<String> codes) {
        if (CollUtil.isEmpty(codes)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .in(ErpProductDO::getCode, codes));
    }

    default List<ErpProductDO> selectListByNames(Collection<String> names) {
        if (CollUtil.isEmpty(names)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .in(ErpProductDO::getName, names));
    }

    default List<ErpProductDO> selectListByFactoryCodes(Collection<String> factoryCodes) {
        if (CollUtil.isEmpty(factoryCodes)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .in(ErpProductDO::getFactoryCode, factoryCodes));
    }

    /**
     * 批量更新 shelf
     */
    default int updateShelfByIds(Collection<Long> ids, String shelf) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        LambdaUpdateWrapper<ErpProductDO> wrapper = new LambdaUpdateWrapper<ErpProductDO>()
                .in(ErpProductDO::getId, ids)
                .set(ErpProductDO::getShelf, shelf);
        return update(null, wrapper);
    }

    /**
     * 查询 shelf 出现次数 > 1 的 shelf 值列表
     */
    default List<String> selectDuplicateShelfValues() {
        QueryWrapper<ErpProductDO> wrapper = new QueryWrapper<ErpProductDO>()
                .select("shelf")
                .ne("shelf", "")
                .isNotNull("shelf")
                .groupBy("shelf")
                .having("COUNT(*) > 1");
        List<Map<String, Object>> result = selectMaps(wrapper);
        return result.stream()
                .map(m -> (String) m.get("shelf"))
                .collect(Collectors.toList());
    }

    /**
     * 查询 shelf 为 null 或空字符串的产品 ID
     */
    default List<Long> selectIdsByEmptyShelf() {
        QueryWrapper<ErpProductDO> wrapper = new QueryWrapper<ErpProductDO>()
                .select("id")
                .and(q -> q.isNull("shelf").or().eq("shelf", ""));
        List<Map<String, Object>> result = selectMaps(wrapper);
        return result.stream()
                .map(m -> (Long) m.get("id"))
                .collect(Collectors.toList());
    }

    /**
     * 根据 shelf 值查产品 ID（配合 selectDuplicateShelfValues 使用）
     */
    default List<Long> selectIdsByShelfIn(Collection<String> shelfs) {
        if (CollUtil.isEmpty(shelfs)) {
            return Collections.emptyList();
        }
        QueryWrapper<ErpProductDO> wrapper = new QueryWrapper<ErpProductDO>()
                .select("id")
                .in("shelf", shelfs);
        List<Map<String, Object>> result = selectMaps(wrapper);
        return result.stream()
                .map(m -> (Long) m.get("id"))
                .collect(Collectors.toList());
    }

    /**
     * 按产品维度的过滤条件查询 productIds（用于库存分页预过滤）。
     * 所有参数都是 AND，null/空不参与过滤。
     *
     * @param reqVO 库存分页请求
     * @param shelfDuplicateIds 货架位重复的产品 ID 集合（shelfDuplicateOnly=true 时必传）
     * @param shelfEmptyIds 货架位为空的产品 ID 集合（shelfEmptyOnly=true 时必传）
     */
    default List<Long> selectIdsByComplexQuery(ErpStockPageReqVO reqVO,
                                               Collection<Long> shelfDuplicateIds,
                                               Collection<Long> shelfEmptyIds) {
        return selectIdsByComplexQuery(reqVO, shelfDuplicateIds, shelfEmptyIds, true);
    }

    default List<Long> selectIdsByComplexQueryWithoutKeyword(ErpStockPageReqVO reqVO,
                                                             Collection<Long> shelfDuplicateIds,
                                                             Collection<Long> shelfEmptyIds) {
        return selectIdsByComplexQuery(reqVO, shelfDuplicateIds, shelfEmptyIds, false);
    }

    default List<Long> selectIdsByKeyword(ErpStockPageReqVO reqVO) {
        LambdaQueryWrapper<ErpProductDO> w = new LambdaQueryWrapper<>();
        w.select(ErpProductDO::getId);
        appendStockKeywordCondition(w, reqVO.getKeyword());
        List<Map<String, Object>> rows = selectMaps(w);
        return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
    }

    default List<Long> selectIdsByComplexQuery(ErpStockPageReqVO reqVO,
                                               Collection<Long> shelfDuplicateIds,
                                               Collection<Long> shelfEmptyIds,
                                               boolean includeKeyword) {
        LambdaQueryWrapper<ErpProductDO> w = new LambdaQueryWrapper<>();
        w.select(ErpProductDO::getId);
        if (StringUtils.hasText(reqVO.getProductCode())) {
            w.like(ErpProductDO::getCode, fuzzyKeyword(reqVO.getProductCode()));
        }
        if (StringUtils.hasText(reqVO.getProductName())) {
            w.like(ErpProductDO::getName, fuzzyKeyword(reqVO.getProductName()));
        }
        if (StringUtils.hasText(reqVO.getDrawingNo())) {
            w.like(ErpProductDO::getDrawingNo, fuzzyKeyword(reqVO.getDrawingNo()));
        }
        if (StringUtils.hasText(reqVO.getVehicleModel())) {
            w.like(ErpProductDO::getVehicleModel, fuzzyKeyword(reqVO.getVehicleModel()));
        }
        if (StringUtils.hasText(reqVO.getOriginPlace())) {
            w.like(ErpProductDO::getOriginPlace, fuzzyKeyword(reqVO.getOriginPlace()));
        }
        if (StringUtils.hasText(reqVO.getBrand())) {
            w.like(ErpProductDO::getBrand, fuzzyKeyword(reqVO.getBrand()));
        }
        if (StringUtils.hasText(reqVO.getShelf())) {
            w.like(ErpProductDO::getShelf, fuzzyKeyword(reqVO.getShelf()));
        }
        if (StringUtils.hasText(reqVO.getFeatureCode())) {
            w.like(ErpProductDO::getFeatureCode, fuzzyKeyword(reqVO.getFeatureCode()));
        }
        if (StringUtils.hasText(reqVO.getStandard())) {
            w.like(ErpProductDO::getStandard, fuzzyKeyword(reqVO.getStandard()));
        }
        if (StringUtils.hasText(reqVO.getFactoryCode())) {
            w.like(ErpProductDO::getFactoryCode, fuzzyKeyword(reqVO.getFactoryCode()));
        }
        if (StringUtils.hasText(reqVO.getBarCode())) {
            w.like(ErpProductDO::getBarCode, fuzzyKeyword(reqVO.getBarCode()));
        }
        if (StringUtils.hasText(reqVO.getOeNumber())) {
            w.like(ErpProductDO::getOeNumber, fuzzyKeyword(reqVO.getOeNumber()));
        }
        appendStockKeywordCondition(w, reqVO.getProductKeyword());
        if (includeKeyword) {
            appendStockKeywordCondition(w, reqVO.getKeyword());
        }
        if (reqVO.getCategoryId() != null) {
            w.eq(ErpProductDO::getCategoryId, reqVO.getCategoryId());
        }
        if (reqVO.getProductStatus() != null) {
            w.eq(ErpProductDO::getStatus, reqVO.getProductStatus());
        }
        if (reqVO.getStockMaxMin() != null) w.ge(ErpProductDO::getStockMax, reqVO.getStockMaxMin());
        if (reqVO.getStockMaxMax() != null) w.le(ErpProductDO::getStockMax, reqVO.getStockMaxMax());
        if (reqVO.getStockMinMin() != null) w.ge(ErpProductDO::getStockMin, reqVO.getStockMinMin());
        if (reqVO.getStockMinMax() != null) w.le(ErpProductDO::getStockMin, reqVO.getStockMinMax());
        if (reqVO.getStockStandardMin() != null) w.ge(ErpProductDO::getStockStandard, reqVO.getStockStandardMin());
        if (reqVO.getStockStandardMax() != null) w.le(ErpProductDO::getStockStandard, reqVO.getStockStandardMax());
        // 特殊：货架位重复/空置 → 预查出的 ids 做交集
        // Intersect with precomputed shelf duplicate IDs.
        if (Boolean.TRUE.equals(reqVO.getShelfDuplicateOnly())) {
            if (shelfDuplicateIds == null || shelfDuplicateIds.isEmpty()) {
                return Collections.emptyList();
            }
            w.in(ErpProductDO::getId, shelfDuplicateIds);
        }
        if (Boolean.TRUE.equals(reqVO.getShelfEmptyOnly())) {
            if (shelfEmptyIds == null || shelfEmptyIds.isEmpty()) {
                return Collections.emptyList();
            }
            w.in(ErpProductDO::getId, shelfEmptyIds);
        }
        List<Map<String, Object>> rows = selectMaps(w);
        return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
    }

    static void appendStockKeywordCondition(LambdaQueryWrapper<ErpProductDO> w, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        KeywordSearch keywordSearch = parseKeywordSearch(keyword);
        if (keywordSearch.explicitDelimited()) {
            appendStockProductKeywordTokensAndCondition(w, keywordSearch.tokens());
            return;
        }
        if (keywordSearch.spaceDelimited()) {
            w.and(q -> {
                appendStockKeywordMatchGroup(q, fuzzyKeyword(keyword));
                q.or(or -> appendStockProductKeywordTokensAndCondition(or, keywordSearch.tokens()));
            });
            return;
        }
        w.and(q -> appendStockKeywordMatchGroup(q, fuzzyKeyword(keyword)));
    }

    static void appendStockKeywordMatchGroup(LambdaQueryWrapper<ErpProductDO> wrapper, String keyword) {
        appendKeywordMatchGroup(wrapper, keyword, STOCK_KEYWORD_FIELDS, Collections.emptyList());
    }

    static void appendStockProductKeywordTokensAndCondition(LambdaQueryWrapper<ErpProductDO> wrapper,
                                                           List<String> tokens) {
        appendKeywordTokensAndCondition(wrapper, tokens, STOCK_PRODUCT_KEYWORD_FIELDS, Collections.emptyList());
    }

}
