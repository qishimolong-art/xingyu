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
import org.apache.ibatis.annotations.Mapper;
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

    default PageResult<ErpProductDO> selectPage(ErpProductPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpProductDO>()
                .likeIfPresent(ErpProductDO::getName, fuzzyKeyword(reqVO.getName()))
                .likeIfPresent(ErpProductDO::getCode, fuzzyKeyword(reqVO.getCode()))
                .likeIfPresent(ErpProductDO::getVehicleModel, fuzzyKeyword(reqVO.getVehicleModel()))
                .likeIfPresent(ErpProductDO::getFactoryCode, fuzzyKeyword(reqVO.getFactoryCode()))
                .eqIfPresent(ErpProductDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(ErpProductDO::getDefaultWarehouseId, reqVO.getWarehouseId())
                .betweenIfPresent(ErpProductDO::getCreateTime, reqVO.getCreateTime())
                // 默认过滤掉已合并的配件
                .ne(ErpProductDO::getMergedFlag, Boolean.TRUE)
                .orderByDesc(ErpProductDO::getId));
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

    default List<ErpProductDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .eq(ErpProductDO::getStatus, status)
                .ne(ErpProductDO::getMergedFlag, Boolean.TRUE));
    }

    default ErpProductDO selectByCode(String code) {
        return selectOne(ErpProductDO::getCode, code);
    }

    default ErpProductDO selectByCodeExcludeId(String code, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<ErpProductDO>()
                .eq(ErpProductDO::getCode, code)
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
        LambdaQueryWrapper<ErpProductDO> w = new LambdaQueryWrapper<>();
        w.select(ErpProductDO::getId);
        if (reqVO.getProductCode() != null && !reqVO.getProductCode().isEmpty()) {
            w.like(ErpProductDO::getCode, reqVO.getProductCode());
        }
        if (reqVO.getProductName() != null && !reqVO.getProductName().isEmpty()) {
            w.like(ErpProductDO::getName, reqVO.getProductName());
        }
        if (reqVO.getDrawingNo() != null && !reqVO.getDrawingNo().isEmpty()) {
            w.like(ErpProductDO::getDrawingNo, reqVO.getDrawingNo());
        }
        if (reqVO.getVehicleModel() != null && !reqVO.getVehicleModel().isEmpty()) {
            w.like(ErpProductDO::getVehicleModel, reqVO.getVehicleModel());
        }
        if (reqVO.getOriginPlace() != null && !reqVO.getOriginPlace().isEmpty()) {
            w.like(ErpProductDO::getOriginPlace, reqVO.getOriginPlace());
        }
        if (reqVO.getBrand() != null && !reqVO.getBrand().isEmpty()) {
            w.like(ErpProductDO::getBrand, reqVO.getBrand());
        }
        if (reqVO.getShelf() != null && !reqVO.getShelf().isEmpty()) {
            w.like(ErpProductDO::getShelf, reqVO.getShelf());
        }
        if (reqVO.getFeatureCode() != null && !reqVO.getFeatureCode().isEmpty()) {
            w.like(ErpProductDO::getFeatureCode, reqVO.getFeatureCode());
        }
        if (reqVO.getStandard() != null && !reqVO.getStandard().isEmpty()) {
            w.like(ErpProductDO::getStandard, reqVO.getStandard());
        }
        if (reqVO.getFactoryCode() != null && !reqVO.getFactoryCode().isEmpty()) {
            w.like(ErpProductDO::getFactoryCode, reqVO.getFactoryCode());
        }
        if (reqVO.getBarCode() != null && !reqVO.getBarCode().isEmpty()) {
            w.like(ErpProductDO::getBarCode, reqVO.getBarCode());
        }
        if (reqVO.getOeNumber() != null && !reqVO.getOeNumber().isEmpty()) {
            w.like(ErpProductDO::getOeNumber, reqVO.getOeNumber());
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

}
