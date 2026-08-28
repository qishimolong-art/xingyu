package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDeptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseBranchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseSaleDeptPermissionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpUserWarehousePermissionDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpUserWarehousePermissionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseSaleDeptPermissionMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_CODE_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DIRECT_MULTIPLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DIRECT_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DISABLE_FAIL_STOCK_NOT_ZERO;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_PURCHASE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_SALE_DISABLE_FAIL_STOCK_NOT_ZERO;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_SALE_DEPT_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_SALE_NOT_ENABLE;

/**
 * ERP 濞寸姵鎸哥花?Service 閻庡湱鍋熼獮鍥╃尵? *
 * @author 闁煎搫顑夋禍鎯р攦閹邦喚鍨?
 */
@Service
@Validated
public class ErpWarehouseServiceImpl implements ErpWarehouseService {

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";
    private static final String DIRECT_WAREHOUSE_NAME = "直发仓";
    private static final String DIRECT_WAREHOUSE_AUTO_CREATE_REMARK =
            "系统自动创建：销售手推车跨部门调拨专用直发仓";
    private static final String DIRECT_WAREHOUSE_CREATE_LOCK_KEY_PREFIX = "erp:warehouse:direct:create:";
    private static final String WAREHOUSE_LOG_TYPE = "仓库信息";
    private static final String WAREHOUSE_LOG_CREATE = "新增";
    private static final String WAREHOUSE_LOG_UPDATE = "修改";
    private static final String WAREHOUSE_LOG_DELETE = "删除";
    private static final String LOG_EMPTY_VALUE = "空";

    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpProductDeptMapper productDeptMapper;

    @Resource
    private ErpWarehouseBranchMapper warehouseBranchMapper;
    @Resource
    private ErpUserWarehousePermissionMapper userWarehousePermissionMapper;
    @Resource
    private ErpWarehouseSaleDeptPermissionMapper warehouseSaleDeptPermissionMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(ErpWarehouseSaveReqVO createReqVO) {
        // 闁圭粯甯掗崣鍡樼閹惧磭姘?
        ErpWarehouseDO warehouse = BeanUtils.toBean(createReqVO, ErpWarehouseDO.class);
        if (warehouse.getSort() == null) {
            warehouse.setSort(0L);
        }
        fillWarehouseEnabledDefaults(warehouse);
        normalizeWarehouseCode(warehouse);
        if (StrUtil.isBlank(warehouse.getWarehouseCode())) {
            warehouse.setWarehouseCode(generateWarehouseCode());
        }
        validateWarehouseCodeUnique(null, warehouse.getWarehouseCode());
        warehouseMapper.insert(warehouse);
        // 闁圭粯甯掗崣鍡涘礆閸℃鏆楅柛蹇撶枃娴?
        createWarehouseBranches(warehouse.getId(), createReqVO.getBranchTenantIds());
        recordWarehouseCreateLog(warehouse);
        // 閺夆晜鏌ㄥú?
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(ErpWarehouseSaveReqVO updateReqVO) {
        ErpWarehouseDO warehouse = validateWarehouseExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, warehouse);

        ErpWarehouseDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseDO.class);
        updateObj.setId(warehouse.getId());
        updateObj.setAddress(warehouse.getAddress());
        updateObj.setPrincipal(warehouse.getPrincipal());
        updateObj.setWarehousePrice(warehouse.getWarehousePrice());
        updateObj.setTruckagePrice(warehouse.getTruckagePrice());
        updateObj.setDefaultStatus(warehouse.getDefaultStatus());
        updateObj.setStorageCenterId(warehouse.getStorageCenterId());
        updateObj.setEcommerceEnabled(warehouse.getEcommerceEnabled());
        updateObj.setSaleBillControl(warehouse.getSaleBillControl());
        updateObj.setZeroStockHide(warehouse.getZeroStockHide());
        updateObj.setGoodsToBranch(warehouse.getGoodsToBranch());
        updateObj.setDept(warehouse.getDept());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(warehouse.getDeptId());
        }
        updateObj.setWarehouseLocation(warehouse.getWarehouseLocation());
        updateObj.setOutPacking(warehouse.getOutPacking());
        updateObj.setAutoOrder(warehouse.getAutoOrder());
        updateObj.setMaxPickCount(warehouse.getMaxPickCount());
        updateObj.setWarehouseCode(warehouse.getWarehouseCode());
        updateObj.setStockGroupType(warehouse.getStockGroupType());
        updateObj.setCreditControl(warehouse.getCreditControl());
        updateObj.setRegionId(warehouse.getRegionId());
        if (Boolean.FALSE.equals(updateObj.getSaleEnabled())) {
            validateWarehouseSaleDisableStockClear(Collections.singleton(warehouse.getId()));
        }
        warehouseMapper.updateById(updateObj);
        if (!Objects.equals(updateObj.getDeptId(), warehouse.getDeptId())) {
            stockMapper.updateDeptIdByWarehouseId(warehouse.getId(), updateObj.getDeptId());
            syncProductOpenDeptByWarehouseIds(Collections.singleton(warehouse.getId()));
        }

        if (updateReqVO.getBranchTenantIds() != null) {
            warehouseBranchMapper.deleteByWarehouseId(updateReqVO.getId());
            createWarehouseBranches(updateReqVO.getId(), updateReqVO.getBranchTenantIds());
        }
        recordWarehouseUpdateLog(warehouse, warehouseMapper.selectById(updateObj.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateWarehouse(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        if (!hasBatchUpdateFields(updateReqVO)) {
            return;
        }
        if (CommonStatusEnum.isDisable(updateReqVO.getStatus())) {
            validateWarehouseStockClear(updateReqVO.getIds());
        }
        if (Boolean.FALSE.equals(updateReqVO.getSaleEnabled())) {
            validateWarehouseSaleDisableStockClear(updateReqVO.getIds());
        }
        for (Long id : updateReqVO.getIds()) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            ErpWarehouseDO updateObj = buildBatchUpdateObj(updateReqVO);
            updateObj.setId(id);
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateObj, warehouse);
            warehouseMapper.updateById(updateObj);
            if (updateReqVO.getDeptId() != null && !Objects.equals(updateReqVO.getDeptId(), warehouse.getDeptId())) {
                stockMapper.updateDeptIdByWarehouseId(id, updateReqVO.getDeptId());
                syncProductOpenDeptByWarehouseIds(Collections.singleton(id));
            }
            recordWarehouseUpdateLog(warehouse, warehouseMapper.selectById(id));
        }
    }

    private boolean hasBatchUpdateFields(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        return updateReqVO.getDeptId() != null
                || updateReqVO.getWarehouseType() != null
                || updateReqVO.getStatus() != null
                || updateReqVO.getSaleEnabled() != null
                || updateReqVO.getPurchaseEnabled() != null
                || updateReqVO.getStockBillEnabled() != null
                || updateReqVO.getScanControl() != null
                || updateReqVO.getSplitOrder() != null
                || updateReqVO.getSort() != null
                || updateReqVO.getRemark() != null;
    }

    private ErpWarehouseDO buildBatchUpdateObj(ErpWarehouseBatchUpdateReqVO updateReqVO) {
        ErpWarehouseDO updateObj = new ErpWarehouseDO();
        if (updateReqVO.getDeptId() != null) {
            updateObj.setDeptId(updateReqVO.getDeptId());
        }
        if (updateReqVO.getWarehouseType() != null) {
            updateObj.setWarehouseType(updateReqVO.getWarehouseType());
        }
        if (updateReqVO.getStatus() != null) {
            updateObj.setStatus(updateReqVO.getStatus());
            if (CommonStatusEnum.isDisable(updateReqVO.getStatus())) {
                updateObj.setDisabledBy(getLoginUserId());
                updateObj.setDisabledTime(LocalDateTime.now());
            }
        }
        if (updateReqVO.getSaleEnabled() != null) {
            updateObj.setSaleEnabled(updateReqVO.getSaleEnabled());
        }
        if (updateReqVO.getPurchaseEnabled() != null) {
            updateObj.setPurchaseEnabled(updateReqVO.getPurchaseEnabled());
        }
        if (updateReqVO.getStockBillEnabled() != null) {
            updateObj.setStockBillEnabled(updateReqVO.getStockBillEnabled());
        }
        if (updateReqVO.getScanControl() != null) {
            updateObj.setScanControl(updateReqVO.getScanControl());
        }
        if (updateReqVO.getSplitOrder() != null) {
            updateObj.setSplitOrder(updateReqVO.getSplitOrder());
        }
        if (updateReqVO.getSort() != null) {
            updateObj.setSort(updateReqVO.getSort());
        }
        if (updateReqVO.getRemark() != null) {
            updateObj.setRemark(updateReqVO.getRemark());
        }
        return updateObj;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableWarehouse(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        validateWarehouseStockClear(distinctIds);
        for (Long id : distinctIds) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            ErpWarehouseDO updateObj = new ErpWarehouseDO();
            updateObj.setId(id);
            updateObj.setStatus(CommonStatusEnum.DISABLE.getStatus());
            updateObj.setDisabledBy(getLoginUserId());
            updateObj.setDisabledTime(LocalDateTime.now());
            warehouseMapper.updateById(updateObj);
            recordWarehouseUpdateLog(warehouse, warehouseMapper.selectById(id));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreWarehouse(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long id : distinctIds) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            if (!CommonStatusEnum.isDisable(warehouse.getStatus())) {
                throw exception(WAREHOUSE_NOT_ENABLE, warehouse.getName());
            }
            warehouseMapper.update(null, new LambdaUpdateWrapper<ErpWarehouseDO>()
                    .eq(ErpWarehouseDO::getId, id)
                    .set(ErpWarehouseDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                    .set(ErpWarehouseDO::getDisabledBy, null)
                    .set(ErpWarehouseDO::getDisabledTime, null));
            recordWarehouseUpdateLog(warehouse, warehouseMapper.selectById(id));
        }
    }

    private void recordWarehouseCreateLog(ErpWarehouseDO warehouse) {
        if (warehouse == null) {
            return;
        }
        operateLogService.record(WAREHOUSE_LOG_TYPE, WAREHOUSE_LOG_CREATE, warehouse.getId(),
                buildWarehouseIdentity(warehouse), warehouse.getWarehouseCode());
    }

    private void recordWarehouseUpdateLog(ErpWarehouseDO before, ErpWarehouseDO after) {
        ErpWarehouseDO target = after != null ? after : before;
        if (target == null) {
            return;
        }
        operateLogService.record(WAREHOUSE_LOG_TYPE, WAREHOUSE_LOG_UPDATE, target.getId(),
                buildWarehouseIdentity(target) + "\n变更明细：\n" + buildWarehouseChangeLines(before, after),
                target.getWarehouseCode());
    }

    private void recordWarehouseDeleteLog(ErpWarehouseDO warehouse) {
        if (warehouse == null) {
            return;
        }
        operateLogService.record(WAREHOUSE_LOG_TYPE, WAREHOUSE_LOG_DELETE, warehouse.getId(),
                buildWarehouseIdentity(warehouse), warehouse.getWarehouseCode());
    }

    private String buildWarehouseIdentity(ErpWarehouseDO warehouse) {
        return "数据库编号：" + formatWarehouseValue(warehouse.getId())
                + "\n名称：" + formatWarehouseValue(warehouse.getName())
                + "\n编码：" + formatWarehouseValue(warehouse.getWarehouseCode());
    }

    private String buildWarehouseChangeLines(ErpWarehouseDO before, ErpWarehouseDO after) {
        if (before == null || after == null) {
            return "无字段变化。";
        }
        List<String> changes = new ArrayList<>();
        addWarehouseChange(changes, "名称", before.getName(), after.getName());
        addWarehouseChange(changes, "所属部门", before.getDeptId(), after.getDeptId(), this::formatDeptValue);
        addWarehouseChange(changes, "仓库地址", before.getAddress(), after.getAddress());
        addWarehouseChange(changes, "排序", before.getSort(), after.getSort());
        addWarehouseChange(changes, "备注", before.getRemark(), after.getRemark());
        addWarehouseChange(changes, "负责人", before.getPrincipal(), after.getPrincipal());
        addWarehouseChange(changes, "仓储费", before.getWarehousePrice(), after.getWarehousePrice());
        addWarehouseChange(changes, "搬运费", before.getTruckagePrice(), after.getTruckagePrice());
        addWarehouseChange(changes, "状态", before.getStatus(), after.getStatus(), this::formatStatusValue);
        addWarehouseChange(changes, "默认仓库", before.getDefaultStatus(), after.getDefaultStatus());
        addWarehouseChange(changes, "仓库类型", before.getWarehouseType(), after.getWarehouseType(), this::formatWarehouseTypeValue);
        addWarehouseChange(changes, "仓储中心", before.getStorageCenterId(), after.getStorageCenterId(), this::formatWarehouseRefValue);
        addWarehouseChange(changes, "仓储对应仓库", before.getStorageWarehouseId(), after.getStorageWarehouseId(), this::formatWarehouseRefValue);
        addWarehouseChange(changes, "销售启用", before.getSaleEnabled(), after.getSaleEnabled(), this::formatEnabledValue);
        addWarehouseChange(changes, "采购启用", before.getPurchaseEnabled(), after.getPurchaseEnabled(), this::formatEnabledValue);
        addWarehouseChange(changes, "入出仓单", before.getStockBillEnabled(), after.getStockBillEnabled(), this::formatStockBillValue);
        addWarehouseChange(changes, "允许电商销售", before.getEcommerceEnabled(), after.getEcommerceEnabled());
        addWarehouseChange(changes, "扫码管控", before.getScanControl(), after.getScanControl());
        addWarehouseChange(changes, "销售开单管控", before.getSaleBillControl(), after.getSaleBillControl());
        addWarehouseChange(changes, "销售0库存不显示", before.getZeroStockHide(), after.getZeroStockHide());
        addWarehouseChange(changes, "货到分店", before.getGoodsToBranch(), after.getGoodsToBranch());
        addWarehouseChange(changes, "部门", before.getDept(), after.getDept());
        addWarehouseChange(changes, "仓库地点", before.getWarehouseLocation(), after.getWarehouseLocation());
        addWarehouseChange(changes, "出仓打包装箱", before.getOutPacking(), after.getOutPacking());
        addWarehouseChange(changes, "自动订货", before.getAutoOrder(), after.getAutoOrder());
        addWarehouseChange(changes, "允许同时拣货单数", before.getMaxPickCount(), after.getMaxPickCount());
        addWarehouseChange(changes, "编码", before.getWarehouseCode(), after.getWarehouseCode());
        addWarehouseChange(changes, "是否拆单", before.getSplitOrder(), after.getSplitOrder());
        addWarehouseChange(changes, "出入仓分组", before.getStockGroupType(), after.getStockGroupType(), this::formatStockGroupTypeValue);
        addWarehouseChange(changes, "额度管控", before.getCreditControl(), after.getCreditControl());
        addWarehouseChange(changes, "区域", before.getRegionId(), after.getRegionId());
        if (CollUtil.isEmpty(changes)) {
            return "无字段变化。";
        }
        return String.join("\n", changes);
    }

    private void addWarehouseChange(List<String> changes, String fieldName, Object before, Object after) {
        addWarehouseChange(changes, fieldName, before, after, this::formatWarehouseValue);
    }

    private void addWarehouseChange(List<String> changes, String fieldName, Object before, Object after,
                                    java.util.function.Function<Object, String> formatter) {
        if (isSameWarehouseValue(before, after)) {
            return;
        }
        changes.add(fieldName + "：" + formatter.apply(before) + "->" + formatter.apply(after) + "；");
        if (changes.size() > 1) {
            int previousIndex = changes.size() - 2;
            String previous = changes.get(previousIndex);
            if (previous.endsWith("。")) {
                changes.set(previousIndex, previous.substring(0, previous.length() - 1) + "；");
            }
        }
        int lastIndex = changes.size() - 1;
        String last = changes.get(lastIndex);
        changes.set(lastIndex, last.substring(0, last.length() - 1) + "。");
    }

    private boolean isSameWarehouseValue(Object before, Object after) {
        if (before instanceof BigDecimal && after instanceof BigDecimal) {
            return ((BigDecimal) before).compareTo((BigDecimal) after) == 0;
        }
        return Objects.equals(before, after);
    }

    private String formatWarehouseValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        if (value instanceof String) {
            return StrUtil.blankToDefault((String) value, LOG_EMPTY_VALUE);
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "是" : "否";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        return String.valueOf(value);
    }

    private String formatDeptValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        Long deptId = (Long) value;
        try {
            DeptRespDTO dept = deptApi.getDept(deptId);
            if (dept != null && StrUtil.isNotBlank(dept.getName())) {
                return dept.getName();
            }
        } catch (Throwable ignored) {
            // 日志展示兜底为 ID，避免部门名称查询影响主流程。
        }
        return String.valueOf(deptId);
    }

    private String formatWarehouseRefValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        Long warehouseId = (Long) value;
        try {
            ErpWarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
            if (warehouse != null && StrUtil.isNotBlank(warehouse.getName())) {
                return warehouse.getName();
            }
        } catch (Throwable ignored) {
            // 日志展示兜底为 ID，避免关联仓库查询影响主流程。
        }
        return String.valueOf(warehouseId);
    }

    private String formatStatusValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        return CommonStatusEnum.isEnable((Integer) value) ? "启用" : "停用";
    }

    private String formatEnabledValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        return Boolean.TRUE.equals(value) ? "启用" : "停用";
    }

    private String formatStockBillValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        return Boolean.TRUE.equals(value) ? "生成" : "不生成";
    }

    private String formatWarehouseTypeValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        Integer warehouseType = (Integer) value;
        if (Objects.equals(warehouseType, 1)) {
            return "物理仓";
        }
        if (Objects.equals(warehouseType, 2)) {
            return "虚拟仓";
        }
        return String.valueOf(warehouseType);
    }

    private String formatStockGroupTypeValue(Object value) {
        if (value == null) {
            return LOG_EMPTY_VALUE;
        }
        Integer stockGroupType = (Integer) value;
        if (Objects.equals(stockGroupType, 1)) {
            return "全部";
        }
        if (Objects.equals(stockGroupType, 2)) {
            return "入仓单";
        }
        if (Objects.equals(stockGroupType, 3)) {
            return "出仓单";
        }
        if (Objects.equals(stockGroupType, 4)) {
            return "全部不分组";
        }
        return String.valueOf(stockGroupType);
    }

    private void validateWarehouseStockClear(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            Long nonZeroCount = stockMapper.selectNonZeroCountByWarehouseId(id);
            if (nonZeroCount != null && nonZeroCount > 0) {
                throw exception(WAREHOUSE_DISABLE_FAIL_STOCK_NOT_ZERO, warehouse.getName());
            }
        }
    }

    private void validateWarehouseSaleDisableStockClear(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpWarehouseDO warehouse = validateWarehouseExists(id);
            Long nonZeroCount = stockMapper.selectNonZeroCountByWarehouseId(id);
            if (nonZeroCount != null && nonZeroCount > 0) {
                throw exception(WAREHOUSE_SALE_DISABLE_FAIL_STOCK_NOT_ZERO, warehouse.getName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseDefaultStatus(Long id, Boolean defaultStatus) {
        // 1. 闁哄稄绻濋悰娆戔偓娑櫭﹢?
        ErpWarehouseDO before = validateWarehouseExists(id);

        // 2.1 濠碘€冲€归悘澶婎嚕閳ь剟宕ラ銈囩闁告帗鐟╁〒鍓佹啺娴ｇ褰犻梻鍌ゅ幗婢у秹寮垫径濠傚緭閻庣懓鍟板▓鎴烆渶濡鍚?
        if (defaultStatus) {
            ErpWarehouseDO warehouse = warehouseMapper.selectByDefaultStatus();
            if (warehouse != null) {
                warehouseMapper.updateById(new ErpWarehouseDO().setId(warehouse.getId()).setDefaultStatus(false));
            }
        }
        // 2.2 闁哄洤鐡ㄩ弻濠勨偓鐢垫嚀缁ㄦ煡鎯冮崟顖滃笡閻犱降鍊楁慨鎼佸箑?
        warehouseMapper.updateById(new ErpWarehouseDO().setId(id).setDefaultStatus(defaultStatus));
        recordWarehouseUpdateLog(before, warehouseMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long id) {
        // 闁哄稄绻濋悰娆戔偓娑櫭﹢?
        ErpWarehouseDO warehouse = validateWarehouseExists(id);
        baseArchiveReferenceService.validateWarehouseNotReferenced(id);
        // 闁告帞濞€濞呭孩绂掗幘宕囨皑
        warehouseMapper.deleteById(id);
        // 闁告帞濞€濞呭酣宕氶崱妤冩殫闁稿繐鐤囨禒?
        warehouseBranchMapper.deleteByWarehouseId(id);
        userWarehousePermissionMapper.deleteListByWarehouseIds(Collections.singleton(id),
                TenantContextHolder.getRequiredTenantId());
        recordWarehouseDeleteLog(warehouse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteWarehouse(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpWarehouseImportRespVO importWarehouseList(List<ErpWarehouseImportExcelVO> list) {
        ErpWarehouseImportRespVO respVO = new ErpWarehouseImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Map<String, List<DeptRespDTO>> deptNameMap = buildDeptNameMap(list);
        for (int i = 0; i < list.size(); i++) {
            ErpWarehouseImportExcelVO row = list.get(i);
            Integer rowNo = i + 2;
            try {
                resolveImportDeptId(row, deptNameMap);
                boolean create = findImportWarehouse(row) == null;
                importWarehouse(row);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
                if (create) {
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                } else {
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                }
            } catch (Exception ex) {
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                respVO.getFailureDetails().add(new ErpWarehouseImportRespVO.FailureItem(
                        rowNo, row == null ? null : row.getWarehouseCode(), ex.getMessage()));
            }
        }
        return respVO;
    }

    private Map<String, List<DeptRespDTO>> buildDeptNameMap(List<ErpWarehouseImportExcelVO> list) {
        Set<String> deptNames = list.stream()
                .filter(row -> row != null && StrUtil.isNotBlank(row.getDeptName()))
                .map(row -> row.getDeptName().trim())
                .collect(Collectors.toCollection(HashSet::new));
        if (CollUtil.isEmpty(deptNames)) {
            return Collections.emptyMap();
        }
        return deptNames.stream().collect(Collectors.toMap(deptName -> deptName,
                deptName -> deptApi.getDeptListByName(deptName)));
    }

    private void resolveImportDeptId(ErpWarehouseImportExcelVO row, Map<String, List<DeptRespDTO>> deptNameMap) {
        if (row == null || StrUtil.isBlank(row.getDeptName())) {
            return;
        }
        String deptName = row.getDeptName().trim();
        List<DeptRespDTO> matchedDepts = deptNameMap.get(deptName);
        if (CollUtil.isEmpty(matchedDepts)) {
            throw new IllegalArgumentException("所属部门不存在：" + deptName);
        }
        if (matchedDepts.size() > 1) {
            throw new IllegalArgumentException("所属部门名称重复，请使用唯一部门名称：" + deptName);
        }
        row.setDeptName(deptName);
        row.setDeptId(matchedDepts.get(0).getId());
    }

    private void importWarehouse(ErpWarehouseImportExcelVO row) {
        if (row == null || StrUtil.isBlank(row.getName())) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        ErpWarehouseDO existing = findImportWarehouse(row);
        ErpWarehouseDO importObj = BeanUtils.toBean(row, ErpWarehouseDO.class);
        if (existing == null) {
            if (importObj.getStatus() == null) {
                importObj.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (importObj.getSort() == null) {
                importObj.setSort(0L);
            }
            fillWarehouseEnabledDefaults(importObj);
            normalizeWarehouseCode(importObj);
            if (StrUtil.isBlank(importObj.getWarehouseCode())) {
                importObj.setWarehouseCode(generateWarehouseCode());
            }
            validateWarehouseCodeUnique(null, importObj.getWarehouseCode());
            warehouseMapper.insert(importObj);
            recordWarehouseCreateLog(importObj);
            return;
        }
        importObj.setId(existing.getId());
        importObj.setDefaultStatus(existing.getDefaultStatus());
        importObj.setWarehouseCode(existing.getWarehouseCode());
        if (Boolean.FALSE.equals(importObj.getSaleEnabled())) {
            validateWarehouseSaleDisableStockClear(Collections.singleton(existing.getId()));
        }
        warehouseMapper.updateById(importObj);
        if (importObj.getDeptId() != null && !Objects.equals(importObj.getDeptId(), existing.getDeptId())) {
            stockMapper.updateDeptIdByWarehouseId(existing.getId(), importObj.getDeptId());
        }
        recordWarehouseUpdateLog(existing, warehouseMapper.selectById(existing.getId()));
    }

    private ErpWarehouseDO findImportWarehouse(ErpWarehouseImportExcelVO row) {
        if (row == null) {
            return null;
        }
        if (StrUtil.isNotBlank(row.getWarehouseCode())) {
            ErpWarehouseDO warehouse = warehouseMapper.selectByWarehouseCode(row.getWarehouseCode());
            if (warehouse != null) {
                return warehouse;
            }
        }
        return StrUtil.isBlank(row.getName()) ? null : warehouseMapper.selectByName(row.getName());
    }

    private void normalizeWarehouseCode(ErpWarehouseDO warehouse) {
        if (warehouse != null && StrUtil.isNotBlank(warehouse.getWarehouseCode())) {
            warehouse.setWarehouseCode(warehouse.getWarehouseCode().trim());
        }
    }

    private String generateWarehouseCode() {
        for (int i = 0; i < 5; i++) {
            String warehouseCode = noRedisDAO.generatePlain(ErpNoRedisDAO.WAREHOUSE_CODE_PREFIX);
            if (StrUtil.isNotBlank(warehouseCode) && warehouseMapper.selectByWarehouseCode(warehouseCode) == null) {
                return warehouseCode;
            }
        }
        throw new IllegalStateException("自动生成仓库编码失败，请稍后重试");
    }

    private void validateWarehouseCodeUnique(Long id, String warehouseCode) {
        if (StrUtil.isBlank(warehouseCode)) {
            return;
        }
        ErpWarehouseDO existing = warehouseMapper.selectByWarehouseCode(warehouseCode);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(WAREHOUSE_CODE_EXISTS, warehouseCode);
        }
    }

    private void fillWarehouseEnabledDefaults(ErpWarehouseDO warehouse) {
        if (warehouse.getSaleEnabled() == null) {
            warehouse.setSaleEnabled(true);
        }
        if (warehouse.getPurchaseEnabled() == null) {
            warehouse.setPurchaseEnabled(true);
        }
    }

    private ErpWarehouseDO validateWarehouseExists(Long id) {
        ErpWarehouseDO warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        return warehouse;
    }

    @Override
    public ErpWarehouseDO getWarehouse(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public ErpWarehouseDO getCurrentUserVisibleWarehouse(Long id) {
        if (id == null) {
            return null;
        }
        if (hasCurrentUserAllWarehousePermission()) {
            return warehouseMapper.selectById(id);
        }
        Set<Long> visibleWarehouseIds = getCurrentUserArchiveVisibleWarehouseIds(null);
        if (!visibleWarehouseIds.contains(id)) {
            return null;
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectById(id));
    }

    @Override
    public Set<Long> getCurrentUserSaleDistributedVisibleWarehouseIds() {
        if (hasCurrentUserAllWarehousePermission()) {
            return Collections.emptySet();
        }
        Set<Long> baseVisibleWarehouseIds = getCurrentUserVisibleWarehouseIds(null);
        Set<Long> saleDistributedWarehouseIds = getCurrentUserSaleDistributedWarehouseIds();
        saleDistributedWarehouseIds.removeAll(baseVisibleWarehouseIds);
        return saleDistributedWarehouseIds;
    }

    @Override
    public List<ErpWarehouseDO> validWarehouseList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpWarehouseDO> list = warehouseMapper.selectByIds(ids);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(list, ErpWarehouseDO::getId);
        for (Long id : ids) {
            ErpWarehouseDO warehouse = warehouseMap.get(id);
            if (warehouseMap.get(id) == null) {
                throw exception(WAREHOUSE_NOT_EXISTS);
            }
            if (CommonStatusEnum.isDisable(warehouse.getStatus())) {
                throw exception(WAREHOUSE_NOT_ENABLE, warehouse.getName());
            }
        }
        return list;
    }

    @Override
    public List<ErpWarehouseDO> validPurchaseWarehouseList(Collection<Long> ids) {
        List<ErpWarehouseDO> list = validWarehouseList(ids);
        validateCurrentUserWarehousePermission(ids);
        for (ErpWarehouseDO warehouse : list) {
            if (!isPurchaseEnabled(warehouse)) {
                throw exception(WAREHOUSE_PURCHASE_NOT_ENABLE, warehouse.getName());
            }
        }
        return list;
    }

    @Override
    public List<ErpWarehouseDO> validSaleWarehouseList(Collection<Long> ids) {
        List<ErpWarehouseDO> list = DataPermissionUtils.executeIgnore(() -> validWarehouseList(ids));
        validateCurrentUserSaleWarehousePermission(ids);
        for (ErpWarehouseDO warehouse : list) {
            if (!isSaleEnabled(warehouse)) {
                throw exception(WAREHOUSE_SALE_NOT_ENABLE, warehouse.getName());
            }
        }
        return list;
    }

    @Override
    public List<ErpWarehouseDO> validSaleWarehouseListForDept(Collection<Long> ids, Long deptId) {
        List<ErpWarehouseDO> list = DataPermissionUtils.executeIgnore(() -> validWarehouseList(ids));
        for (ErpWarehouseDO warehouse : list) {
            validateWarehouseSaleAllowedForDept(warehouse, deptId);
        }
        return list;
    }

    @Override
    public List<ErpWarehouseDO> validSaleSelectableWarehouseListForDept(Collection<Long> ids, Long deptId) {
        if (deptId == null) {
            return validSaleWarehouseList(ids);
        }
        List<ErpWarehouseDO> list = DataPermissionUtils.executeIgnore(() -> validWarehouseList(ids));
        for (ErpWarehouseDO warehouse : list) {
            validateWarehouseSaleSelectableForDept(warehouse, deptId);
        }
        return list;
    }

    private void validateCurrentUserSaleWarehousePermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds) || hasCurrentUserAllWarehousePermission()) {
            return;
        }
        Set<Long> allowedWarehouseIds = getCurrentUserVisibleSaleWarehouseList().stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> deniedWarehouseIds = warehouseIds.stream()
                .filter(Objects::nonNull)
                .filter(warehouseId -> !allowedWarehouseIds.contains(warehouseId))
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deniedWarehouseIds)) {
            throw new IllegalArgumentException("No warehouse sale permission: " + deniedWarehouseIds);
        }
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseListByStatus(Integer status) {
        return warehouseMapper.selectListByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resolveDirectWarehouseId(Long deptId) {
        if (deptId == null) {
            throw exception(WAREHOUSE_DIRECT_NOT_CONFIGURED);
        }
        List<ErpWarehouseDO> directWarehouses = getEnabledDirectWarehouses(deptId);
        if (directWarehouses.size() == 1) {
            return directWarehouses.get(0).getId();
        }
        if (directWarehouses.size() > 1) {
            throw exception(WAREHOUSE_DIRECT_MULTIPLE);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock(DIRECT_WAREHOUSE_CREATE_LOCK_KEY_PREFIX + tenantId + ":" + deptId);
        lock.lock();
        boolean unlockAfterTransaction = false;
        try {
            directWarehouses = getEnabledDirectWarehouses(deptId);
            if (directWarehouses.size() == 1) {
                return directWarehouses.get(0).getId();
            }
            if (directWarehouses.size() > 1) {
                throw exception(WAREHOUSE_DIRECT_MULTIPLE);
            }
            ErpWarehouseSaveReqVO createReqVO = new ErpWarehouseSaveReqVO();
            createReqVO.setName(DIRECT_WAREHOUSE_NAME);
            createReqVO.setDeptId(deptId);
            createReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            createReqVO.setSaleEnabled(true);
            createReqVO.setStockBillEnabled(false);
            createReqVO.setRemark(DIRECT_WAREHOUSE_AUTO_CREATE_REMARK);
            Long warehouseId = createWarehouse(createReqVO);
            unlockAfterTransaction = registerUnlockAfterTransaction(lock);
            return warehouseId;
        } finally {
            if (!unlockAfterTransaction && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private List<ErpWarehouseDO> getEnabledDirectWarehouses(Long deptId) {
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectListByNameAndDeptIdAndStatus(
                DIRECT_WAREHOUSE_NAME, deptId, CommonStatusEnum.ENABLE.getStatus()));
    }

    private boolean registerUnlockAfterTransaction(RLock lock) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        });
        return true;
    }

    @Override
    public List<ErpWarehouseDO> getPurchaseWarehouseListByStatus(Integer status) {
        return getWarehouseListByStatus(status).stream()
                .filter(this::isPurchaseEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpWarehouseDO> getSaleWarehouseListByStatus(Integer status) {
        return getWarehouseListByStatus(status).stream()
                .filter(this::isSaleEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpWarehouseDO> getAssignableWarehouseList() {
        return DataPermissionUtils.executeIgnore(() ->
                warehouseMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus()));
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserAuthorizedWarehouseList() {
        if (hasCurrentUserAllWarehousePermission()) {
            return getAssignableWarehouseList();
        }
        Set<Long> visibleWarehouseIds = getCurrentUserVisibleWarehouseIds(CommonStatusEnum.ENABLE.getStatus());
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectListByStatusAndIds(
                CommonStatusEnum.ENABLE.getStatus(), visibleWarehouseIds));
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserAuthorizedPurchaseWarehouseList() {
        return getCurrentUserAuthorizedWarehouseList().stream()
                .filter(this::isPurchaseEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserAuthorizedSaleWarehouseList() {
        return getCurrentUserAuthorizedWarehouseList().stream()
                .filter(this::isSaleEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserVisibleSaleWarehouseList() {
        List<ErpWarehouseDO> list = new ArrayList<>(getCurrentUserAuthorizedSaleWarehouseList());
        Set<Long> warehouseIds = list.stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> distributedWarehouseIds = new LinkedHashSet<>(getCurrentUserSaleDistributedVisibleWarehouseIds());
        distributedWarehouseIds.removeAll(warehouseIds);
        if (CollUtil.isEmpty(distributedWarehouseIds)) {
            return list;
        }
        List<ErpWarehouseDO> distributedWarehouses = DataPermissionUtils.executeIgnore(() ->
                warehouseMapper.selectListByStatusAndIds(CommonStatusEnum.ENABLE.getStatus(), distributedWarehouseIds));
        distributedWarehouses.stream()
                .filter(this::isSaleEnabled)
                .filter(warehouse -> warehouse.getId() != null && warehouseIds.add(warehouse.getId()))
                .forEach(list::add);
        return list;
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserSaleSelectableWarehouseListByDept(Long deptId) {
        if (deptId == null) {
            return getCurrentUserVisibleSaleWarehouseList();
        }
        List<ErpWarehouseDO> list = new ArrayList<>(getSaleWarehouseListByDeptId(deptId));
        Set<Long> warehouseIds = list.stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        getCurrentUserDirectAuthorizedSaleWarehouseList().stream()
                .filter(warehouse -> warehouse.getId() != null && warehouseIds.add(warehouse.getId()))
                .forEach(list::add);
        return list;
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserStockVisibleWarehouseList() {
        Set<Long> warehouseIds = getCurrentUserProductStockPermissionScope().getVisibleWarehouseIds();
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectListByStatusAndIds(
                CommonStatusEnum.ENABLE.getStatus(), warehouseIds));
    }

    @Override
    public ErpProductStockPermissionScope getCurrentUserProductStockPermissionScope() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return ErpProductStockPermissionScope.empty(null);
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_stock");
        if (permission == null) {
            return ErpProductStockPermissionScope.empty(loginUserId);
        }
        List<ErpWarehouseDO> warehouses = DataPermissionUtils.executeIgnore(() ->
                warehouseMapper.selectListByStatusIfPresent(CommonStatusEnum.ENABLE.getStatus()));
        if (Boolean.TRUE.equals(permission.getAll())) {
            Set<Long> allWarehouseIds = warehouses.stream()
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return new ErpProductStockPermissionScope(true, allWarehouseIds,
                    Collections.emptySet(), loginUserId);
        }

        Set<Long> departmentWarehouseIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(permission.getDeptIds())) {
            warehouses.stream()
                    .filter(warehouse -> warehouse.getDeptId() != null
                            && permission.getDeptIds().contains(warehouse.getDeptId()))
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(departmentWarehouseIds::add);
        }
        Set<Long> selfWarehouseIds = new LinkedHashSet<>();
        Long loginDeptId = getLoginUserDeptId();
        if (Boolean.TRUE.equals(permission.getSelf()) && loginDeptId != null) {
            warehouses.stream()
                    .filter(warehouse -> Objects.equals(loginDeptId, warehouse.getDeptId()))
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(selfWarehouseIds::add);
        }
        return new ErpProductStockPermissionScope(false, departmentWarehouseIds, selfWarehouseIds, loginUserId);
    }

    @Override
    public List<ErpWarehouseDO> getCurrentUserStockMoveFromWarehouseList() {
        List<ErpWarehouseDO> list = new ArrayList<>(getCurrentUserAuthorizedWarehouseList());
        Set<Long> warehouseIds = list.stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> distributedWarehouseIds = new LinkedHashSet<>(getCurrentUserSaleDistributedVisibleWarehouseIds());
        distributedWarehouseIds.removeAll(warehouseIds);
        if (CollUtil.isEmpty(distributedWarehouseIds)) {
            return list;
        }
        DataPermissionUtils.executeIgnore(() ->
                warehouseMapper.selectListByStatusAndIds(CommonStatusEnum.ENABLE.getStatus(), distributedWarehouseIds))
                .stream()
                .filter(warehouse -> warehouse.getId() != null && warehouseIds.add(warehouse.getId()))
                .forEach(list::add);
        return list;
    }

    @Override
    public List<ErpWarehouseDO> getSaleWarehouseListByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        Set<Long> distributedWarehouseIds = warehouseSaleDeptPermissionMapper.selectListByDeptId(deptId).stream()
                .map(ErpWarehouseSaleDeptPermissionDO::getWarehouseId)
                .collect(Collectors.toSet());
        List<ErpWarehouseDO> saleWarehouses = DataPermissionUtils.executeIgnore(() ->
                warehouseMapper.selectListByStatusAndDeptIdOrIds(CommonStatusEnum.ENABLE.getStatus(),
                        deptId, distributedWarehouseIds));
        return saleWarehouses.stream()
                .filter(this::isSaleEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> getWarehouseSaleDeptIds(Long warehouseId) {
        if (warehouseId == null) {
            return Collections.emptySet();
        }
        DataPermissionUtils.executeIgnore(() -> {
            validateWarehouseExists(warehouseId);
        });
        return warehouseSaleDeptPermissionMapper.selectListByWarehouseId(warehouseId).stream()
                .map(ErpWarehouseSaleDeptPermissionDO::getDeptId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseSaleDeptPermissions(Long warehouseId, Collection<Long> deptIds) {
        if (warehouseId == null) {
            return;
        }
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> validateWarehouseExists(warehouseId));
        Set<Long> deptIdSet = expandDeptIds(deptIds);
        deptIdSet.removeIf(deptId -> deptId == null || Objects.equals(deptId, warehouse.getDeptId()));
        if (CollUtil.isNotEmpty(deptIdSet)) {
            deptApi.validateDeptList(deptIdSet);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        warehouseSaleDeptPermissionMapper.deleteListByWarehouseId(warehouseId, tenantId);
        if (CollUtil.isEmpty(deptIdSet)) {
            return;
        }
        deptIdSet.forEach(deptId -> warehouseSaleDeptPermissionMapper.insertIgnore(warehouseId, deptId, tenantId));
    }

    @Override
    public void validateWarehouseSaleAllowedForDept(Long warehouseId, Long deptId) {
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> validateWarehouseExists(warehouseId));
        validateWarehouseSaleAllowedForDept(warehouse, deptId);
    }

    @Override
    public void validateWarehouseSaleSelectableForDept(Long warehouseId, Long deptId) {
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> validateWarehouseExists(warehouseId));
        validateWarehouseSaleSelectableForDept(warehouse, deptId);
    }

    @Override
    public boolean isWarehouseSaleAllowedForDept(Long warehouseId, Long deptId) {
        if (warehouseId == null || deptId == null) {
            return false;
        }
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectById(warehouseId));
        return warehouse != null && isWarehouseSaleAllowedForDept(warehouse, deptId);
    }

    private void validateWarehouseSaleAllowedForDept(ErpWarehouseDO warehouse, Long deptId) {
        if (CommonStatusEnum.isDisable(warehouse.getStatus())) {
            throw exception(WAREHOUSE_NOT_ENABLE, warehouse.getName());
        }
        if (!isSaleEnabled(warehouse)) {
            throw exception(WAREHOUSE_SALE_NOT_ENABLE, warehouse.getName());
        }
        if (isWarehouseSaleAllowedForDept(warehouse, deptId)) {
            return;
        }
        throw exception(WAREHOUSE_SALE_DEPT_PERMISSION_DENIED, warehouse.getName());
    }

    private void validateWarehouseSaleSelectableForDept(ErpWarehouseDO warehouse, Long deptId) {
        if (CommonStatusEnum.isDisable(warehouse.getStatus())) {
            throw exception(WAREHOUSE_NOT_ENABLE, warehouse.getName());
        }
        if (!isSaleEnabled(warehouse)) {
            throw exception(WAREHOUSE_SALE_NOT_ENABLE, warehouse.getName());
        }
        if (isWarehouseSaleAllowedForDept(warehouse, deptId) || isCurrentUserDirectAuthorizedWarehouse(warehouse.getId())) {
            return;
        }
        throw exception(WAREHOUSE_SALE_DEPT_PERMISSION_DENIED, warehouse.getName());
    }

    private boolean isWarehouseSaleAllowedForDept(ErpWarehouseDO warehouse, Long deptId) {
        if (warehouse == null || deptId == null) {
            return false;
        }
        if (Objects.equals(warehouse.getDeptId(), deptId)) {
            return true;
        }
        return warehouse.getId() != null
                && warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(warehouse.getId(), deptId) > 0;
    }

    private List<ErpWarehouseDO> getCurrentUserDirectAuthorizedSaleWarehouseList() {
        Set<Long> warehouseIds = getCurrentUserAuthorizedWarehouseIds();
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectListByStatusAndIds(
                        CommonStatusEnum.ENABLE.getStatus(), warehouseIds))
                .stream()
                .filter(this::isSaleEnabled)
                .collect(Collectors.toList());
    }

    private boolean isCurrentUserDirectAuthorizedWarehouse(Long warehouseId) {
        return warehouseId != null && getCurrentUserAuthorizedWarehouseIds().contains(warehouseId);
    }

    private Set<Long> expandDeptIds(Collection<Long> deptIds) {
        Set<Long> result = (deptIds == null ? Collections.<Long>emptyList() : deptIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(result)) {
            return result;
        }
        Set<Long> selectedIds = new LinkedHashSet<>(result);
        selectedIds.forEach(deptId -> {
            List<DeptRespDTO> childDeptList = deptApi.getChildDeptList(deptId);
            if (CollUtil.isNotEmpty(childDeptList)) {
                result.addAll(convertList(childDeptList, DeptRespDTO::getId));
            }
        });
        return result;
    }

    private boolean isPurchaseEnabled(ErpWarehouseDO warehouse) {
        return warehouse != null && !Boolean.FALSE.equals(warehouse.getPurchaseEnabled());
    }

    private boolean isSaleEnabled(ErpWarehouseDO warehouse) {
        return warehouse != null && !Boolean.FALSE.equals(warehouse.getSaleEnabled());
    }

    @Override
    public List<Long> getUserWarehouseIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return convertList(userWarehousePermissionMapper.selectListByUserId(userId),
                ErpUserWarehousePermissionDO::getWarehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserWarehousePermissions(Long userId, Collection<Long> warehouseIds) {
        if (userId == null) {
            return;
        }
        Set<Long> warehouseIdSet = (warehouseIds == null ? Collections.<Long>emptyList() : warehouseIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        DataPermissionUtils.executeIgnore(() -> {
            validWarehouseList(warehouseIdSet);
        });
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        userWarehousePermissionMapper.deleteListByUserId(userId, tenantId);
        if (CollUtil.isEmpty(warehouseIdSet)) {
            return;
        }
        warehouseIdSet.forEach(warehouseId -> userWarehousePermissionMapper.insertIgnore(userId, warehouseId, tenantId));
    }

    @Override
    public List<Long> getWarehouseUserIds(Long warehouseId) {
        if (warehouseId == null) {
            return Collections.emptyList();
        }
        DataPermissionUtils.executeIgnore(() -> {
            validateWarehouseExists(warehouseId);
        });
        return convertList(userWarehousePermissionMapper.selectListByWarehouseId(warehouseId),
                ErpUserWarehousePermissionDO::getUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseUserPermissions(Long warehouseId, Collection<Long> userIds) {
        if (warehouseId == null) {
            return;
        }
        DataPermissionUtils.executeIgnore(() -> {
            validateWarehouseExists(warehouseId);
        });
        Set<Long> userIdSet = (userIds == null ? Collections.<Long>emptyList() : userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        userWarehousePermissionMapper.deleteListByWarehouseId(warehouseId, tenantId);
        if (CollUtil.isEmpty(userIdSet)) {
            return;
        }
        userIdSet.forEach(userId -> userWarehousePermissionMapper.insertIgnore(userId, warehouseId, tenantId));
    }

    @Override
    public void validateCurrentUserWarehousePermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds) || hasCurrentUserAllWarehousePermission()) {
            return;
        }
        Set<Long> allowedWarehouseIds = getCurrentUserVisibleWarehouseIds(CommonStatusEnum.ENABLE.getStatus());
        List<Long> deniedWarehouseIds = warehouseIds.stream()
                .filter(Objects::nonNull)
                .filter(warehouseId -> !allowedWarehouseIds.contains(warehouseId))
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deniedWarehouseIds)) {
            throw new IllegalArgumentException("No warehouse permission: " + deniedWarehouseIds);
        }
    }

    @Override
    public void validateCurrentUserStockWarehousePermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return;
        }
        ErpProductStockPermissionScope scope = getCurrentUserProductStockPermissionScope();
        List<Long> deniedWarehouseIds = warehouseIds.stream()
                .filter(Objects::nonNull)
                .filter(warehouseId -> !scope.canAccessWarehouse(warehouseId))
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deniedWarehouseIds)) {
            throw new IllegalArgumentException("No product stock warehouse permission: " + deniedWarehouseIds);
        }
    }

    @Override
    public void validateCurrentUserStockPermission(Collection<ErpStockDO> stocks) {
        if (CollUtil.isEmpty(stocks)) {
            return;
        }
        ErpProductStockPermissionScope scope = getCurrentUserProductStockPermissionScope();
        List<Long> deniedStockIds = stocks.stream()
                .filter(Objects::nonNull)
                .filter(stock -> !scope.canAccessStock(stock))
                .map(ErpStockDO::getId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deniedStockIds)) {
            throw new IllegalArgumentException("Product stock does not exist or is not accessible");
        }
    }

    @Override
    public void validateCurrentUserStockMoveFromWarehousePermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds) || hasCurrentUserAllWarehousePermission()) {
            return;
        }
        Set<Long> allowedWarehouseIds = getCurrentUserStockMoveFromWarehouseList().stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> deniedWarehouseIds = warehouseIds.stream()
                .filter(Objects::nonNull)
                .filter(warehouseId -> !allowedWarehouseIds.contains(warehouseId))
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deniedWarehouseIds)) {
            throw new IllegalArgumentException("No stock move from warehouse permission: " + deniedWarehouseIds);
        }
    }

    @Override
    public boolean hasCurrentUserAllWarehousePermission() {
        Long loginUserId = getLoginUserId();
        return loginUserId != null && permissionApi.hasAnyRoles(loginUserId, RoleCodeEnum.SUPER_ADMIN.getCode());
    }

    @Override
    public Set<Long> getCurrentUserAuthorizedWarehouseIds() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(getUserWarehouseIds(loginUserId));
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectByIds(ids);
    }

    @Override
    public List<ErpWarehouseDO> getWarehouseListByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectListByDeptId(deptId);
    }

    @Override
    public PageResult<ErpWarehouseDO> getWarehousePage(ErpWarehousePageReqVO pageReqVO) {
        if (hasCurrentUserAllWarehousePermission()) {
            return warehouseMapper.selectPage(pageReqVO);
        }
        Set<Long> visibleWarehouseIds = getCurrentUserArchiveVisibleWarehouseIds(null);
        return DataPermissionUtils.executeIgnore(() -> warehouseMapper.selectPageByIds(pageReqVO, visibleWarehouseIds));
    }

    @Override
    public List<Long> getWarehouseBranchTenantIds(Long warehouseId) {
        List<ErpWarehouseBranchDO> branches = warehouseBranchMapper.selectListByWarehouseId(warehouseId);
        return convertList(branches, ErpWarehouseBranchDO::getBranchTenantId);
    }

    /**
     * 闁归潧缍婇崳娲礆濞戞绱﹀ù鐘虫尭缁ㄩ亶宕氶崱妤冩殫闁稿繐鐤囨禒?
     */
    private void createWarehouseBranches(Long warehouseId, List<Long> branchTenantIds) {
        if (CollUtil.isEmpty(branchTenantIds)) {
            return;
        }
        List<ErpWarehouseBranchDO> branches = convertList(branchTenantIds, tenantId ->
                new ErpWarehouseBranchDO().setWarehouseId(warehouseId).setBranchTenantId(tenantId));
        warehouseBranchMapper.insertBatch(branches);
    }

    private Set<Long> getCurrentUserVisibleWarehouseIds(Integer status) {
        if (getLoginUserId() == null) {
            return Collections.emptySet();
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(), "erp_warehouse");
        if (permission != null && Boolean.TRUE.equals(permission.getAll())) {
            return warehouseMapper.selectListByStatusIfPresent(status).stream()
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        Set<Long> visibleWarehouseIds = new LinkedHashSet<>();
        if (permission != null && CollUtil.isNotEmpty(permission.getDeptIds())) {
            warehouseMapper.selectListByStatusIfPresent(status).stream()
                    .filter(warehouse -> warehouse.getDeptId() != null
                            && permission.getDeptIds().contains(warehouse.getDeptId()))
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(visibleWarehouseIds::add);
        }
        if (permission != null && Boolean.TRUE.equals(permission.getSelf())) {
            String loginUserId = String.valueOf(getLoginUserId());
            warehouseMapper.selectListByStatusIfPresent(status).stream()
                    .filter(warehouse -> Objects.equals(warehouse.getCreator(), loginUserId))
                    .map(ErpWarehouseDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(visibleWarehouseIds::add);
        }
        visibleWarehouseIds.addAll(getCurrentUserAuthorizedWarehouseIds());
        return visibleWarehouseIds;
    }

    private Set<Long> getCurrentUserArchiveVisibleWarehouseIds(Integer status) {
        Set<Long> visibleWarehouseIds = getCurrentUserVisibleWarehouseIds(status);
        visibleWarehouseIds.addAll(getCurrentUserSaleDistributedWarehouseIds());
        if (status == null || CollUtil.isEmpty(visibleWarehouseIds)) {
            return visibleWarehouseIds;
        }
        return warehouseMapper.selectListByStatusAndIds(status, visibleWarehouseIds).stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> getCurrentUserSaleDistributedWarehouseIds() {
        Set<Long> deptIds = getCurrentUserArchiveDeptIds();
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptySet();
        }
        return warehouseSaleDeptPermissionMapper.selectListByDeptIds(deptIds).stream()
                .map(ErpWarehouseSaleDeptPermissionDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> getCurrentUserArchiveDeptIds() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return Collections.emptySet();
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_warehouse");
        Set<Long> deptIds = new LinkedHashSet<>();
        if (permission != null && Boolean.TRUE.equals(permission.getAll())) {
            warehouseMapper.selectListByStatusIfPresent(null).stream()
                    .map(ErpWarehouseDO::getDeptId)
                    .filter(Objects::nonNull)
                    .forEach(deptIds::add);
        } else if (permission != null && CollUtil.isNotEmpty(permission.getDeptIds())) {
            deptIds.addAll(permission.getDeptIds());
        }
        Long loginDeptId = getLoginUserDeptId();
        if (loginDeptId != null) {
            deptIds.add(loginDeptId);
        }
        return deptIds;
    }

    private void syncProductOpenDeptByWarehouseIds(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return;
        }
        List<ErpStockDO> affectedStocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectList(new LambdaQueryWrapper<ErpStockDO>()
                        .in(ErpStockDO::getWarehouseId, warehouseIds)));
        Set<Long> productIds = affectedStocks.stream()
                .map(ErpStockDO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        List<ErpStockDO> productStocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectList(new LambdaQueryWrapper<ErpStockDO>()
                        .in(ErpStockDO::getProductId, productIds)));
        Set<Long> allWarehouseIds = productStocks.stream()
                .map(ErpStockDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ErpWarehouseDO> warehouseMap = allWarehouseIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                convertMap(warehouseMapper.selectByIds(allWarehouseIds), ErpWarehouseDO::getId));
        Map<Long, List<Long>> deptIdsByProduct = productStocks.stream()
                .filter(stock -> stock.getProductId() != null && stock.getWarehouseId() != null)
                .collect(Collectors.groupingBy(ErpStockDO::getProductId,
                        Collectors.mapping(stock -> {
                            ErpWarehouseDO warehouse = warehouseMap.get(stock.getWarehouseId());
                            return warehouse == null ? null : warehouse.getDeptId();
                        }, Collectors.toList())));
        for (Long productId : productIds) {
            List<Long> deptIds = deptIdsByProduct.getOrDefault(productId, Collections.emptyList()).stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            productMapper.update(null, new LambdaUpdateWrapper<ErpProductDO>()
                    .eq(ErpProductDO::getId, productId)
                    .set(ErpProductDO::getDeptId, CollUtil.isEmpty(deptIds) ? null : deptIds.get(0)));
            productDeptMapper.deleteByProductId(productId);
            if (CollUtil.isEmpty(deptIds)) {
                continue;
            }
            productDeptMapper.insertBatch(deptIds.stream()
                    .map(deptId -> {
                        ErpProductDeptDO dept = new ErpProductDeptDO();
                        dept.setProductId(productId);
                        dept.setDeptId(deptId);
                        return dept;
                    })
                    .collect(Collectors.toList()));
        }
    }

}

