package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

/**
 * ERP 产品单位 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpProductUnitServiceImpl implements ErpProductUnitService {

    private static final String FIELD_PERMISSION_MODULE = "erp_product_unit";

    @Resource
    private ErpProductUnitMapper productUnitMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpProductService productService;

    @Override
    public Long createProductUnit(ErpProductUnitSaveReqVO createReqVO) {
        applyProductUnitSaveFieldPermissions(createReqVO, null);
        ValidationUtils.validate(createReqVO);
        // 1. 校验名字唯一
        validateProductUnitNameUnique(null, createReqVO.getName());
        // 2. 插入
        ErpProductUnitDO unit = BeanUtils.toBean(createReqVO, ErpProductUnitDO.class);
        if (unit.getDeptId() == null) {
            unit.setDeptId(getLoginUserDeptId());
        }
        productUnitMapper.insert(unit);
        operateLogService.recordCreate(ERP_PRODUCT_UNIT_TYPE, unit.getId(), unit, unit.getName());
        return unit.getId();
    }

    @Override
    public void updateProductUnit(ErpProductUnitSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpProductUnitDO existing = validateProductUnitExists(updateReqVO.getId());
        applyProductUnitSaveFieldPermissions(updateReqVO, existing);
        ValidationUtils.validate(updateReqVO);
        // 1.2 校验名字唯一
        validateProductUnitNameUnique(updateReqVO.getId(), updateReqVO.getName());
        // 2. 更新
        ErpProductUnitDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductUnitDO.class);
        updateObj.setDeptId(existing.getDeptId());
        productUnitMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_PRODUCT_UNIT_TYPE, updateReqVO.getId(), existing,
                productUnitMapper.selectById(updateReqVO.getId()), updateObj.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateProductUnit(ErpProductUnitBatchUpdateReqVO updateReqVO) {
        if (CollUtil.isEmpty(updateReqVO.getIds())) {
            return;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        Set<String> hiddenFieldSet = CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "status")) {
            throw new IllegalArgumentException("无权批量修改单位状态");
        }
        for (Long id : updateReqVO.getIds()) {
            ErpProductUnitDO existing = validateProductUnitExists(id);
            ErpProductUnitDO updateObj = new ErpProductUnitDO();
            updateObj.setId(id);
            updateObj.setStatus(updateReqVO.getStatus());
            productUnitMapper.updateById(updateObj);
            operateLogService.recordUpdate(ERP_PRODUCT_UNIT_TYPE, id, existing,
                    productUnitMapper.selectById(id), existing.getName());
        }
    }

    @VisibleForTesting
    void validateProductUnitNameUnique(Long id, String name) {
        ErpProductUnitDO unit = productUnitMapper.selectByName(name);
        if (unit == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的字典类型
        if (id == null) {
            throw exception(PRODUCT_UNIT_NAME_DUPLICATE);
        }
        if (!unit.getId().equals(id)) {
            throw exception(PRODUCT_UNIT_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteProductUnit(Long id) {
        // 1.1 校验存在
        ErpProductUnitDO unit = validateProductUnitExists(id);
        // 1.2 校验产品是否使用
        if (productService.getProductCountByUnitId(id) > 0) {
            throw exception(PRODUCT_UNIT_EXITS_PRODUCT);
        }
        // 2. 删除
        productUnitMapper.deleteById(id);
        operateLogService.recordDelete(ERP_PRODUCT_UNIT_TYPE, id, unit, unit.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductUnitList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteProductUnit(id);
        }
    }

    private ErpProductUnitDO validateProductUnitExists(Long id) {
        ErpProductUnitDO unit = productUnitMapper.selectById(id);
        if (unit == null) {
            throw exception(PRODUCT_UNIT_NOT_EXISTS);
        }
        return unit;
    }

    @Override
    public ErpProductUnitDO getProductUnit(Long id) {
        return applyProductUnitFieldPermissions(productUnitMapper.selectById(id));
    }

    @Override
    public PageResult<ErpProductUnitDO> getProductUnitPage(ErpProductUnitPageReqVO pageReqVO) {
        PageResult<ErpProductUnitDO> pageResult = productUnitMapper.selectPage(pageReqVO);
        List<ErpProductUnitDO> list = pageResult.getList().stream()
                .map(this::applyProductUnitFieldPermissions)
                .collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<ErpProductUnitDO> getProductUnitListByStatus(Integer status) {
        return productUnitMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpProductUnitDO> getProductUnitListByStatusForCurrentUser(Integer status) {
        return productUnitMapper.selectListByStatus(status).stream()
                .map(this::applyProductUnitFieldPermissions)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpProductUnitDO> getProductUnitList(Collection<Long> ids) {
         return productUnitMapper.selectByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpProductUnitImportRespVO importProductUnitList(List<ErpProductUnitImportExcelVO> list) {
        ErpProductUnitImportRespVO result = new ErpProductUnitImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return result;
        }
        Map<String, ErpProductUnitDO> unitMap = buildUnitNameMap();
        for (int i = 0; i < list.size(); i++) {
            ErpProductUnitImportExcelVO row = list.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            int rowNo = i + 2;
            String name = trimToNull(row.getName());
            try {
                ErpProductUnitDO existing = unitMap.get(name);
                ErpProductUnitSaveReqVO saveReqVO = new ErpProductUnitSaveReqVO();
                saveReqVO.setId(existing == null ? null : existing.getId());
                saveReqVO.setName(name);
                saveReqVO.setStatus(row.getStatus() == null ? CommonStatusEnum.ENABLE.getStatus() : row.getStatus());
                if (existing == null) {
                    createProductUnit(saveReqVO);
                    result.setCreateCount(result.getCreateCount() + 1);
                } else {
                    updateProductUnit(saveReqVO);
                    result.setUpdateCount(result.getUpdateCount() + 1);
                }
                unitMap = buildUnitNameMap();
            } catch (Exception ex) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailureDetails().add(new ErpProductUnitImportRespVO.FailureItem(
                        rowNo, name, getImportFailureReason(ex)));
            }
        }
        result.setSuccessCount(result.getCreateCount() + result.getUpdateCount());
        result.setFailureCount(result.getFailureDetails().size());
        return result;
    }

    @Override
    public List<ErpProductUnitImportExcelVO> parseCsvImport(Reader reader) {
        CsvReadConfig config = CsvReadConfig.defaultConfig().setContainsHeader(true);
        CsvReader csvReader = CsvUtil.getReader(config);
        CsvData csvData = csvReader.read(reader);
        List<ErpProductUnitImportExcelVO> result = new ArrayList<>();
        for (CsvRow row : csvData.getRows()) {
            ErpProductUnitImportExcelVO item = new ErpProductUnitImportExcelVO();
            item.setName(trimToNull(readCsvValue(row, "单位名称", "单位")));
            item.setStatus(parseInteger(readCsvValue(row, "状态")));
            result.add(item);
        }
        return result;
    }

    private Map<String, ErpProductUnitDO> buildUnitNameMap() {
        List<ErpProductUnitDO> units = productUnitMapper.selectListAll();
        if (CollUtil.isEmpty(units)) {
            return new HashMap<>();
        }
        return units.stream()
                .filter(unit -> StrUtil.isNotBlank(trimToNull(unit.getName())))
                .collect(Collectors.toMap(unit -> trimToNull(unit.getName()), unit -> unit, (a, b) -> a, LinkedHashMap::new));
    }

    private boolean isEmptyRow(ErpProductUnitImportExcelVO row) {
        return row == null
                || (!StrUtil.isNotBlank(trimToNull(row.getName())) && row.getStatus() == null);
    }

    private String getImportFailureReason(Exception ex) {
        if (ex instanceof ConstraintViolationException) {
            return ex.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private String readCsvValue(CsvRow row, String header) {
        return row.getByName(header);
    }

    private String readCsvValue(CsvRow row, String header, String fallbackHeader) {
        String value = readCsvValue(row, header);
        return value != null ? value : readCsvValue(row, fallbackHeader);
    }

    private String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    private Integer parseInteger(String value) {
        String trim = trimToNull(value);
        if (trim == null) {
            return null;
        }
        return Integer.valueOf(trim);
    }

    private ErpProductUnitDO applyProductUnitFieldPermissions(ErpProductUnitDO unit) {
        if (unit == null) {
            return null;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return unit;
        }
        ErpProductUnitDO result = BeanUtils.toBean(unit, ErpProductUnitDO.class);
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "name")) {
            result.setName(null);
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            result.setStatus(null);
        }
        return result;
    }

    private void applyProductUnitSaveFieldPermissions(ErpProductUnitSaveReqVO reqVO, ErpProductUnitDO existing) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "name")) {
            reqVO.setName(existing == null ? null : existing.getName());
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            reqVO.setStatus(existing == null ? null : existing.getStatus());
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

}
