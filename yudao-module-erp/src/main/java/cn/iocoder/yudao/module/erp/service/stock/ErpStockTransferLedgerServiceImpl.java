package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerTotalRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class ErpStockTransferLedgerServiceImpl implements ErpStockTransferLedgerService {

    private static final int TRANSFER_OUT = 10;
    private static final int TRANSFER_IN = 20;
    private static final int MAX_QUERY_DAYS = 366;
    private static final ErrorCode DATE_RANGE_INVALID = new ErrorCode(1_030_505_007,
            "调拨台账业务日期范围无效，且单次查询不能超过 366 天");

    private static final Map<String, String> EXCEPTION_REASONS = buildExceptionReasons();

    @Resource
    private ErpStockMoveMapper stockMoveMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @Override
    public PageResult<ErpStockTransferLedgerSummaryRespVO> getSummaryPage(
            ErpStockTransferLedgerPageReqVO reqVO) {
        List<ComputedGroup> groups = computeGroups(reqVO);
        Map<LocalDate, List<ComputedGroup>> dailyMap = groups.stream()
                .collect(Collectors.groupingBy(ComputedGroup::getBusinessDate));
        List<ErpStockTransferLedgerSummaryRespVO> list = dailyMap.entrySet().stream()
                .map(entry -> buildSummary(entry.getKey(), entry.getValue()))
                .sorted(summaryComparator(reqVO))
                .collect(Collectors.toList());
        return page(list, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    public ErpStockTransferLedgerTotalRespVO getTotal(ErpStockTransferLedgerPageReqVO reqVO) {
        List<ComputedGroup> groups = computeGroups(reqVO);
        BigDecimal outCount = sum(groups, ComputedGroup::getOutCount);
        BigDecimal inCount = sum(groups, ComputedGroup::getInCount);
        return new ErpStockTransferLedgerTotalRespVO()
                .setTransferOutCount(outCount)
                .setTransferInCount(inCount)
                .setDifferenceCount(outCount.subtract(inCount))
                .setAbnormalGroupCount(groups.stream().filter(ComputedGroup::isAbnormal).count());
    }

    @Override
    public PageResult<ErpStockTransferLedgerDetailRespVO> getDetailPage(
            ErpStockTransferLedgerDetailPageReqVO reqVO) {
        List<ErpStockTransferLedgerDetailRespVO> rows = computeGroups(reqVO).stream()
                .filter(group -> Objects.equals(group.getBusinessDate(), reqVO.getBusinessDate()))
                .flatMap(group -> group.getRows().stream())
                .sorted(detailComparator())
                .collect(Collectors.toList());
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    public List<ErpStockTransferLedgerDetailRespVO> getExportList(ErpStockTransferLedgerPageReqVO reqVO) {
        return computeGroups(reqVO).stream()
                .flatMap(group -> group.getRows().stream())
                .sorted(detailComparator())
                .collect(Collectors.toList());
    }

    @Override
    public List<DeptSimpleRespVO> getVisibleFromDeptSimpleList() {
        return dataPermissionDeptService.getEnabledDeptSimpleList(collectVisibleLedgerDeptIds(true));
    }

    @Override
    public List<DeptSimpleRespVO> getVisibleToDeptSimpleList() {
        return dataPermissionDeptService.getEnabledDeptSimpleList(collectVisibleLedgerDeptIds(false));
    }

    private List<Long> collectVisibleLedgerDeptIds(boolean fromDept) {
        ErpStockTransferOutPermissionScope outScope = stockMoveService.getTransferOutPermissionScope();
        ErpStockTransferOutPermissionScope inScope = stockMoveService.getTransferInPermissionScope();
        Set<Long> deptIds = new LinkedHashSet<>();
        DataPermissionUtils.executeIgnore(() -> {
            if (fromDept) {
                deptIds.addAll(stockMoveMapper.selectTransferOutVisibleFromDeptIdList(
                        outScope == null ? Collections.emptySet() : outScope.getDeptIds(),
                        outScope == null || outScope.isAll()));
                deptIds.addAll(stockMoveMapper.selectTransferInVisibleFromDeptIdList(
                        inScope == null ? Collections.emptySet() : inScope.getDeptIds(),
                        inScope == null || inScope.isAll()));
            } else {
                deptIds.addAll(stockMoveMapper.selectTransferOutVisibleToDeptIdList(
                        outScope == null ? Collections.emptySet() : outScope.getDeptIds(),
                        outScope == null || outScope.isAll()));
                deptIds.addAll(stockMoveMapper.selectTransferInVisibleToDeptIdList(
                        inScope == null ? Collections.emptySet() : inScope.getDeptIds(),
                        inScope == null || inScope.isAll()));
            }
            return null;
        });
        return new ArrayList<>(deptIds);
    }

    private List<ComputedGroup> computeGroups(ErpStockTransferLedgerPageReqVO reqVO) {
        validateDateRange(reqVO.getMoveTime());
        LocalDateTime start = reqVO.getMoveTime()[0];
        LocalDateTime end = reqVO.getMoveTime()[1];

        ErpStockMovePageReqVO dateQuery = new ErpStockMovePageReqVO();
        dateQuery.setMoveTime(new LocalDateTime[]{start, end});
        dateQuery.setTransferDirection(TRANSFER_OUT);
        List<ErpStockMoveDO> dateOutMoves = DataPermissionUtils.executeIgnore(() ->
                stockMoveMapper.selectTransferOutList(dateQuery, Collections.emptySet(), true));
        dateQuery.setTransferDirection(TRANSFER_IN);
        List<ErpStockMoveDO> dateInMoves = DataPermissionUtils.executeIgnore(() ->
                stockMoveMapper.selectTransferInList(dateQuery, Collections.emptySet(), true));

        Set<Long> seedIds = new LinkedHashSet<>();
        dateOutMoves.forEach(move -> {
            seedIds.add(move.getId());
            if (move.getRelatedMoveId() != null) seedIds.add(move.getRelatedMoveId());
        });
        dateInMoves.forEach(move -> {
            seedIds.add(move.getId());
            if (move.getRelatedMoveId() != null) seedIds.add(move.getRelatedMoveId());
        });
        List<ErpStockMoveDO> expandedMoves = DataPermissionUtils.executeIgnore(() ->
                stockMoveMapper.selectLedgerRelatedList(seedIds));
        Map<Long, ErpStockMoveDO> moveMap = expandedMoves.stream()
                .collect(Collectors.toMap(ErpStockMoveDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        dateOutMoves.forEach(move -> moveMap.put(move.getId(), move));
        dateInMoves.forEach(move -> moveMap.put(move.getId(), move));

        List<ErpStockMoveItemDO> allItems = moveMap.isEmpty() ? Collections.emptyList()
                : DataPermissionUtils.executeIgnore(() -> stockMoveItemMapper.selectListByMoveIds(moveMap.keySet()));
        Map<Long, List<ErpStockMoveItemDO>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(ErpStockMoveItemDO::getMoveId));

        ErpStockTransferOutPermissionScope outScope = stockMoveService.getTransferOutPermissionScope();
        ErpStockTransferOutPermissionScope inScope = stockMoveService.getTransferInPermissionScope();
        List<MoveGroup> groups = buildMoveGroups(moveMap.values());
        List<MoveGroup> visibleGroups = groups.stream()
                .filter(group -> isVisible(group, itemMap, outScope, inScope))
                .filter(group -> !group.getBusinessDate().isBefore(start.toLocalDate())
                        && !group.getBusinessDate().isAfter(end.toLocalDate()))
                .filter(group -> matchesDocumentFilters(group, reqVO))
                .collect(Collectors.toList());

        NameContext names = buildNameContext(allItems, visibleGroups);
        List<ComputedGroup> result = new ArrayList<>();
        for (MoveGroup group : visibleGroups) {
            ComputedGroup computed = computeGroup(group, itemMap, names, reqVO);
            if (computed != null && matchesResultFilters(computed, reqVO)) {
                result.add(computed);
            }
        }
        return result;
    }

    private List<MoveGroup> buildMoveGroups(Collection<ErpStockMoveDO> moves) {
        List<ErpStockMoveDO> outs = moves.stream().filter(this::isOut)
                .sorted(Comparator.comparing(ErpStockMoveDO::getId)).collect(Collectors.toList());
        List<ErpStockMoveDO> ins = moves.stream().filter(this::isIn)
                .sorted(Comparator.comparing(ErpStockMoveDO::getId)).collect(Collectors.toList());
        Set<Long> assignedInIds = new HashSet<>();
        List<MoveGroup> groups = new ArrayList<>();
        for (ErpStockMoveDO out : outs) {
            List<ErpStockMoveDO> relatedIns = ins.stream()
                    .filter(in -> !assignedInIds.contains(in.getId()))
                    .filter(in -> Objects.equals(in.getRelatedMoveId(), out.getId())
                            || Objects.equals(out.getRelatedMoveId(), in.getId()))
                    .collect(Collectors.toList());
            relatedIns.forEach(in -> assignedInIds.add(in.getId()));
            // 调拨入库只会在调拨出库审核通过时正式生成。历史脏数据中即使未审核出库
            // 残留了关联入库，也不能把该入库计入台账，否则会虚增入仓数量。
            List<ErpStockMoveDO> effectiveIns = ErpAuditStatus.APPROVE.getStatus().equals(out.getStatus())
                    ? relatedIns : Collections.emptyList();
            groups.add(new MoveGroup(out, effectiveIns));
        }
        ins.stream().filter(in -> !assignedInIds.contains(in.getId()))
                .forEach(in -> groups.add(new MoveGroup(null, Collections.singletonList(in))));
        return groups;
    }

    private boolean isVisible(MoveGroup group, Map<Long, List<ErpStockMoveItemDO>> itemMap,
                              ErpStockTransferOutPermissionScope outScope,
                              ErpStockTransferOutPermissionScope inScope) {
        if (outScope == null && inScope == null) return true;
        boolean outVisible = group.out != null && isSideVisible(group.out,
                itemMap.getOrDefault(group.out.getId(), Collections.emptyList()), outScope, true);
        boolean inVisible = group.ins.stream().anyMatch(in -> isSideVisible(in,
                itemMap.getOrDefault(in.getId(), Collections.emptyList()), inScope, false));
        return outVisible || inVisible;
    }

    private boolean isSideVisible(ErpStockMoveDO move, List<ErpStockMoveItemDO> items,
                                  ErpStockTransferOutPermissionScope scope, boolean out) {
        if (scope == null || scope.isAll()) return true;
        if (scope.getDeptIds().isEmpty()) return false;
        Long mainDeptId = out ? move.getFromDeptId() : move.getToDeptId();
        if (!scope.getDeptIds().contains(mainDeptId)) return false;
        return items.stream().allMatch(item -> {
            Long itemDeptId = out ? item.getFromDeptId() : item.getToDeptId();
            return itemDeptId != null && scope.getDeptIds().contains(itemDeptId);
        });
    }

    private ComputedGroup computeGroup(MoveGroup group, Map<Long, List<ErpStockMoveItemDO>> itemMap,
                                       NameContext names, ErpStockTransferLedgerPageReqVO reqVO) {
        List<ErpStockMoveItemDO> outItems = group.out == null ? Collections.emptyList()
                : itemMap.getOrDefault(group.out.getId(), Collections.emptyList());
        List<ErpStockMoveItemDO> inItems = group.ins.stream()
                .flatMap(in -> itemMap.getOrDefault(in.getId(), Collections.emptyList()).stream())
                .collect(Collectors.toList());
        Map<ItemKey, ItemAggregate> outMap = aggregate(outItems);
        Map<ItemKey, ItemAggregate> inMap = aggregate(inItems);
        Set<ItemKey> keys = new LinkedHashSet<>(outMap.keySet());
        keys.addAll(inMap.keySet());
        if (keys.isEmpty()) keys.add(ItemKey.empty());

        LinkedHashSet<String> groupErrors = getDocumentErrors(group);
        List<ErpStockTransferLedgerDetailRespVO> rows = new ArrayList<>();
        for (ItemKey key : keys) {
            if (!matchesItemFilters(key, reqVO)) continue;
            ItemAggregate out = outMap.get(key);
            ItemAggregate in = inMap.get(key);
            LinkedHashSet<String> errors = new LinkedHashSet<>(groupErrors);
            if (group.out != null && !group.ins.isEmpty()) {
                appendItemErrors(key, out, in, outMap.keySet(), inMap.keySet(), errors);
            }
            ErpStockTransferLedgerDetailRespVO row = buildDetail(group, key, out, in, names, errors);
            if (matchesKeyword(row, reqVO.getKeyword())) rows.add(row);
        }
        if (rows.isEmpty()) return null;

        LinkedHashSet<String> allErrors = rows.stream().flatMap(row -> row.getExceptionCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String status = allErrors.isEmpty() ? "NORMAL" : "ABNORMAL";
        rows.forEach(row -> row.setMatchStatus(status));
        BigDecimal outCount = rows.stream().map(ErpStockTransferLedgerDetailRespVO::getTransferOutCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inCount = rows.stream().map(ErpStockTransferLedgerDetailRespVO::getTransferInCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ComputedGroup(group.getBusinessDate(), group, rows, allErrors, status, outCount, inCount);
    }

    private LinkedHashSet<String> getDocumentErrors(MoveGroup group) {
        LinkedHashSet<String> errors = new LinkedHashSet<>();
        if (group.out == null) errors.add("ORPHAN_TRANSFER_IN");
        if (group.out != null && group.ins.isEmpty()) errors.add("MISSING_TRANSFER_IN");
        if (group.ins.size() > 1) errors.add("DUPLICATE_TRANSFER_IN");
        if (group.out != null && !group.ins.isEmpty()) {
            if (group.ins.size() != 1 || !Objects.equals(group.out.getRelatedMoveId(), group.ins.get(0).getId())
                    || group.ins.stream().anyMatch(in -> !Objects.equals(in.getRelatedMoveId(), group.out.getId()))) {
                errors.add("BROKEN_BIDIRECTIONAL_LINK");
            }
            if (group.ins.stream().anyMatch(in -> !Objects.equals(in.getStatus(), group.out.getStatus()))) {
                errors.add("STATUS_MISMATCH");
            }
            if (group.ins.stream().anyMatch(in -> in.getMoveTime() == null || group.out.getMoveTime() == null
                    || !Objects.equals(in.getMoveTime().toLocalDate(), group.out.getMoveTime().toLocalDate()))) {
                errors.add("DATE_MISMATCH");
            }
            if (group.ins.stream().anyMatch(in -> !Objects.equals(in.getFromDeptId(), group.out.getFromDeptId())
                    || !Objects.equals(in.getToDeptId(), group.out.getToDeptId()))) {
                errors.add("DEPT_MISMATCH");
            }
        }
        return errors;
    }

    private void appendItemErrors(ItemKey key, ItemAggregate out, ItemAggregate in,
                                  Set<ItemKey> outKeys, Set<ItemKey> inKeys, Set<String> errors) {
        if (out == null && in != null) {
            errors.add(hasSameProductBatch(key, outKeys) ? "WAREHOUSE_MISMATCH"
                    : hasSameProductWarehouses(key, outKeys) ? "BATCH_MISMATCH" : "ITEM_MISSING_OUT");
        } else if (out != null && in == null) {
            errors.add(hasSameProductBatch(key, inKeys) ? "WAREHOUSE_MISMATCH"
                    : hasSameProductWarehouses(key, inKeys) ? "BATCH_MISMATCH" : "ITEM_MISSING_IN");
        } else if (out != null && in != null && out.count.compareTo(in.count) != 0) {
            errors.add("COUNT_MISMATCH");
        }
    }

    private ErpStockTransferLedgerDetailRespVO buildDetail(MoveGroup group, ItemKey key,
                                                            ItemAggregate out, ItemAggregate in,
                                                            NameContext names, Set<String> errors) {
        ErpStockMoveDO firstIn = group.ins.isEmpty() ? null : group.ins.get(0);
        ErpStockMoveDO base = group.out != null ? group.out : firstIn;
        BigDecimal outCount = out == null ? BigDecimal.ZERO : out.count;
        BigDecimal inCount = in == null ? BigDecimal.ZERO : in.count;
        ItemAggregate snapshot = resolveAggregate(out, in);
        String reason = errors.stream().map(EXCEPTION_REASONS::get).filter(Objects::nonNull)
                .collect(Collectors.joining("；"));
        ErpProductRespVO product = names.products.get(key.productId);
        return new ErpStockTransferLedgerDetailRespVO()
                .setRowKey((group.out == null ? "IN-" + firstIn.getId() : "OUT-" + group.out.getId())
                        + "-" + key.asText())
                .setGroupKey(group.out == null ? -firstIn.getId() : group.out.getId())
                .setBusinessDate(group.getBusinessDate())
                .setTransferOutId(group.out == null ? null : group.out.getId())
                .setTransferOutNo(group.out == null ? null : group.out.getNo())
                .setTransferInId(firstIn == null ? null : firstIn.getId())
                .setTransferInNo(group.ins.stream().map(ErpStockMoveDO::getNo)
                        .filter(StringUtils::hasText).collect(Collectors.joining(", ")))
                .setSourceNo(base == null ? null : base.getSourceNo())
                .setFromDeptId(base == null ? null : base.getFromDeptId())
                .setFromDeptName(getDeptName(names, base == null ? null : base.getFromDeptId()))
                .setToDeptId(base == null ? null : base.getToDeptId())
                .setToDeptName(getDeptName(names, base == null ? null : base.getToDeptId()))
                .setProductId(key.productId)
                .setProductCode(product == null ? null : product.getCode())
                .setProductName(product == null ? null : product.getName())
                .setFromWarehouseId(key.fromWarehouseId)
                .setFromWarehouseName(getWarehouseName(names, key.fromWarehouseId))
                .setToWarehouseId(key.toWarehouseId)
                .setToWarehouseName(getWarehouseName(names, key.toWarehouseId))
                .setBatchNo(key.batchNo)
                .setProductUnitName(product == null ? null : product.getUnitName())
                .setPackageQty(snapshot.packageQty)
                .setWeight(snapshot.weight)
                .setTotalWeight(snapshot.weight == null ? null : snapshot.weight.multiply(outCount))
                .setTransferOutCount(outCount)
                .setTransferInCount(inCount)
                .setDifferenceCount(outCount.subtract(inCount))
                .setTransferOutStatus(group.out == null ? null : group.out.getStatus())
                .setTransferInStatus(firstIn == null ? null : firstIn.getStatus())
                .setExceptionCodes(new ArrayList<>(errors))
                .setExceptionReason(StringUtils.hasText(reason) ? reason : null);
    }

    private NameContext buildNameContext(List<ErpStockMoveItemDO> items, List<MoveGroup> groups) {
        Set<Long> productIds = items.stream().map(ErpStockMoveItemDO::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = items.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getFromWarehouseId(), item.getToWarehouseId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> deptIds = groups.stream().flatMap(group -> {
                    ErpStockMoveDO base = group.out != null ? group.out
                            : group.ins.isEmpty() ? null : group.ins.get(0);
                    return base == null ? java.util.stream.Stream.<Long>empty()
                            : java.util.stream.Stream.of(base.getFromDeptId(), base.getToDeptId());
                }).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpProductRespVO> products = productIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, DeptRespDTO> depts = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return new NameContext(products, warehouses, depts);
    }

    private boolean matchesDocumentFilters(MoveGroup group, ErpStockTransferLedgerPageReqVO req) {
        ErpStockMoveDO out = group.out;
        if (StringUtils.hasText(req.getTransferOutNo())
                && (out == null || !contains(out.getNo(), req.getTransferOutNo()))) return false;
        if (StringUtils.hasText(req.getTransferInNo())
                && group.ins.stream().noneMatch(in -> contains(in.getNo(), req.getTransferInNo()))) return false;
        if (StringUtils.hasText(req.getSourceNo())) {
            boolean match = out != null && contains(out.getSourceNo(), req.getSourceNo())
                    || group.ins.stream().anyMatch(in -> contains(in.getSourceNo(), req.getSourceNo()));
            if (!match) return false;
        }
        if (req.getFromDeptId() != null && !Objects.equals(group.getFromDeptId(), req.getFromDeptId())) return false;
        if (req.getToDeptId() != null && !Objects.equals(group.getToDeptId(), req.getToDeptId())) return false;
        if (req.getStatus() != null) {
            boolean match = out != null && Objects.equals(out.getStatus(), req.getStatus())
                    || group.ins.stream().anyMatch(in -> Objects.equals(in.getStatus(), req.getStatus()));
            if (!match) return false;
        }
        return true;
    }

    private boolean matchesItemFilters(ItemKey key, ErpStockTransferLedgerPageReqVO req) {
        return (req.getProductId() == null || Objects.equals(key.productId, req.getProductId()))
                && (req.getFromWarehouseId() == null || Objects.equals(key.fromWarehouseId, req.getFromWarehouseId()))
                && (req.getToWarehouseId() == null || Objects.equals(key.toWarehouseId, req.getToWarehouseId()));
    }

    private boolean matchesResultFilters(ComputedGroup group, ErpStockTransferLedgerPageReqVO req) {
        if (StringUtils.hasText(req.getMatchStatus())
                && !group.status.equalsIgnoreCase(req.getMatchStatus().trim())) return false;
        return !StringUtils.hasText(req.getExceptionCode())
                || group.errors.contains(req.getExceptionCode().trim().toUpperCase(Locale.ROOT));
    }

    private boolean matchesKeyword(ErpStockTransferLedgerDetailRespVO row, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return java.util.stream.Stream.of(row.getTransferOutNo(), row.getTransferInNo(), row.getSourceNo(),
                        row.getProductCode(), row.getProductName(), row.getFromWarehouseName(),
                        row.getToWarehouseName(), row.getFromDeptName(), row.getToDeptName())
                .filter(Objects::nonNull).map(text -> text.toLowerCase(Locale.ROOT))
                .anyMatch(text -> text.contains(value));
    }

    private ErpStockTransferLedgerSummaryRespVO buildSummary(LocalDate date, List<ComputedGroup> groups) {
        Set<Long> outIds = groups.stream().map(group -> group.group.out)
                .filter(Objects::nonNull).map(ErpStockMoveDO::getId).collect(Collectors.toSet());
        Set<Long> inIds = groups.stream().flatMap(group -> group.group.ins.stream())
                .map(ErpStockMoveDO::getId).collect(Collectors.toSet());
        BigDecimal outCount = sum(groups, ComputedGroup::getOutCount);
        BigDecimal inCount = sum(groups, ComputedGroup::getInCount);
        long abnormal = groups.stream().filter(ComputedGroup::isAbnormal).count();
        return new ErpStockTransferLedgerSummaryRespVO()
                .setBusinessDate(date)
                .setTransferOutDocumentCount((long) outIds.size())
                .setTransferOutCount(outCount)
                .setTransferInDocumentCount((long) inIds.size())
                .setTransferInCount(inCount)
                .setDifferenceCount(outCount.subtract(inCount))
                .setPendingGroupCount(0L)
                .setCompletedGroupCount(groups.size() - abnormal)
                .setAbnormalGroupCount(abnormal)
                .setLedgerStatus(abnormal > 0 ? "ABNORMAL" : "NORMAL");
    }

    private void validateDateRange(LocalDateTime[] range) {
        if (range == null || range.length != 2 || range[0] == null || range[1] == null
                || range[0].isAfter(range[1])
                || Duration.between(range[0], range[1]).toDays() >= MAX_QUERY_DAYS) {
            throw exception(DATE_RANGE_INVALID);
        }
    }

    private Map<ItemKey, ItemAggregate> aggregate(List<ErpStockMoveItemDO> items) {
        Map<ItemKey, ItemAggregate> result = new LinkedHashMap<>();
        for (ErpStockMoveItemDO item : items) {
            ItemKey key = new ItemKey(item.getProductId(), item.getFromWarehouseId(), item.getToWarehouseId(),
                    item.getBatchNo() == null ? "" : item.getBatchNo().trim());
            result.computeIfAbsent(key, ignored -> new ItemAggregate()).add(item);
        }
        return result;
    }

    private boolean hasSameProductBatch(ItemKey key, Set<ItemKey> others) {
        return others.stream().anyMatch(other -> Objects.equals(other.productId, key.productId)
                && Objects.equals(other.batchNo, key.batchNo));
    }

    private boolean hasSameProductWarehouses(ItemKey key, Set<ItemKey> others) {
        return others.stream().anyMatch(other -> Objects.equals(other.productId, key.productId)
                && Objects.equals(other.fromWarehouseId, key.fromWarehouseId)
                && Objects.equals(other.toWarehouseId, key.toWarehouseId));
    }

    private boolean isOut(ErpStockMoveDO move) {
        return move.getTransferDirection() == null || Objects.equals(move.getTransferDirection(), TRANSFER_OUT);
    }

    private boolean isIn(ErpStockMoveDO move) {
        return Objects.equals(move.getTransferDirection(), TRANSFER_IN);
    }

    private boolean contains(String source, String target) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(target.trim().toLowerCase(Locale.ROOT));
    }

    private String getDeptName(NameContext names, Long id) {
        DeptRespDTO dept = names.depts.get(id);
        return dept == null ? null : dept.getName();
    }

    private String getWarehouseName(NameContext names, Long id) {
        ErpWarehouseDO warehouse = names.warehouses.get(id);
        return warehouse == null ? null : warehouse.getName();
    }

    private ItemAggregate resolveAggregate(ItemAggregate out, ItemAggregate in) {
        return out != null ? out : in != null ? in : new ItemAggregate();
    }

    private BigDecimal sum(List<ComputedGroup> groups, Function<ComputedGroup, BigDecimal> getter) {
        return groups.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Comparator<ErpStockTransferLedgerSummaryRespVO> summaryComparator(
            ErpStockTransferLedgerPageReqVO reqVO) {
        Comparator<ErpStockTransferLedgerSummaryRespVO> comparator =
                Comparator.comparing(ErpStockTransferLedgerSummaryRespVO::getBusinessDate);
        return "asc".equalsIgnoreCase(reqVO.getOrderDirection()) ? comparator : comparator.reversed();
    }

    private Comparator<ErpStockTransferLedgerDetailRespVO> detailComparator() {
        return Comparator.comparingInt((ErpStockTransferLedgerDetailRespVO row) ->
                        "ABNORMAL".equals(row.getMatchStatus()) ? 0 : 1)
                .thenComparing(ErpStockTransferLedgerDetailRespVO::getBusinessDate, Comparator.reverseOrder())
                .thenComparing(ErpStockTransferLedgerDetailRespVO::getGroupKey,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ErpStockTransferLedgerDetailRespVO::getRowKey);
    }

    private <T> PageResult<T> page(List<T> list, Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = Math.min((safePageNo - 1) * safePageSize, list.size());
        int to = Math.min(from + safePageSize, list.size());
        return new PageResult<>(new ArrayList<>(list.subList(from, to)), (long) list.size());
    }

    private static Map<String, String> buildExceptionReasons() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("MISSING_TRANSFER_IN", "缺少调拨入库");
        map.put("ORPHAN_TRANSFER_IN", "孤立调拨入库");
        map.put("DUPLICATE_TRANSFER_IN", "重复调拨入库");
        map.put("BROKEN_BIDIRECTIONAL_LINK", "关联关系异常");
        map.put("STATUS_MISMATCH", "状态不一致");
        map.put("DATE_MISMATCH", "调拨日期不一致");
        map.put("DEPT_MISMATCH", "部门不一致");
        map.put("ITEM_MISSING_OUT", "缺少出库明细");
        map.put("ITEM_MISSING_IN", "缺少入库明细");
        map.put("WAREHOUSE_MISMATCH", "仓库不一致");
        map.put("BATCH_MISMATCH", "批次不一致");
        map.put("COUNT_MISMATCH", "数量不一致");
        return Collections.unmodifiableMap(map);
    }

    private static final class MoveGroup {
        private final ErpStockMoveDO out;
        private final List<ErpStockMoveDO> ins;

        private MoveGroup(ErpStockMoveDO out, List<ErpStockMoveDO> ins) {
            this.out = out;
            this.ins = ins;
        }

        private LocalDate getBusinessDate() {
            ErpStockMoveDO base = out != null ? out : ins.get(0);
            return base.getMoveTime().toLocalDate();
        }

        private Long getFromDeptId() {
            ErpStockMoveDO base = out != null ? out : ins.get(0);
            return base.getFromDeptId();
        }

        private Long getToDeptId() {
            ErpStockMoveDO base = out != null ? out : ins.get(0);
            return base.getToDeptId();
        }
    }

    private static final class ItemKey {
        private final Long productId;
        private final Long fromWarehouseId;
        private final Long toWarehouseId;
        private final String batchNo;

        private ItemKey(Long productId, Long fromWarehouseId, Long toWarehouseId, String batchNo) {
            this.productId = productId;
            this.fromWarehouseId = fromWarehouseId;
            this.toWarehouseId = toWarehouseId;
            this.batchNo = batchNo;
        }

        private static ItemKey empty() {
            return new ItemKey(null, null, null, "");
        }

        private String asText() {
            return String.valueOf(productId) + '-' + fromWarehouseId + '-' + toWarehouseId + '-' + batchNo;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof ItemKey)) return false;
            ItemKey key = (ItemKey) object;
            return Objects.equals(productId, key.productId)
                    && Objects.equals(fromWarehouseId, key.fromWarehouseId)
                    && Objects.equals(toWarehouseId, key.toWarehouseId)
                    && Objects.equals(batchNo, key.batchNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, fromWarehouseId, toWarehouseId, batchNo);
        }
    }

    private static final class ItemAggregate {
        private BigDecimal count = BigDecimal.ZERO;
        private Integer packageQty;
        private BigDecimal weight;

        private void add(ErpStockMoveItemDO item) {
            BigDecimal value = item == null ? null : item.getCount();
            count = count.add(value == null ? BigDecimal.ZERO : value);
            if (item != null && packageQty == null) {
                packageQty = item.getPackageQty();
            }
            if (item != null && weight == null) {
                weight = item.getWeight();
            }
        }
    }

    private static final class NameContext {
        private final Map<Long, ErpProductRespVO> products;
        private final Map<Long, ErpWarehouseDO> warehouses;
        private final Map<Long, DeptRespDTO> depts;

        private NameContext(Map<Long, ErpProductRespVO> products, Map<Long, ErpWarehouseDO> warehouses,
                            Map<Long, DeptRespDTO> depts) {
            this.products = products;
            this.warehouses = warehouses;
            this.depts = depts;
        }
    }

    private static final class ComputedGroup {
        private final LocalDate businessDate;
        private final MoveGroup group;
        private final List<ErpStockTransferLedgerDetailRespVO> rows;
        private final Set<String> errors;
        private final String status;
        private final BigDecimal outCount;
        private final BigDecimal inCount;

        private ComputedGroup(LocalDate businessDate, MoveGroup group,
                              List<ErpStockTransferLedgerDetailRespVO> rows, Set<String> errors,
                              String status, BigDecimal outCount, BigDecimal inCount) {
            this.businessDate = businessDate;
            this.group = group;
            this.rows = rows;
            this.errors = errors;
            this.status = status;
            this.outCount = outCount;
            this.inCount = inCount;
        }

        private LocalDate getBusinessDate() { return businessDate; }
        private List<ErpStockTransferLedgerDetailRespVO> getRows() { return rows; }
        private BigDecimal getOutCount() { return outCount; }
        private BigDecimal getInCount() { return inCount; }
        private boolean isAbnormal() { return !errors.isEmpty(); }
    }

}
