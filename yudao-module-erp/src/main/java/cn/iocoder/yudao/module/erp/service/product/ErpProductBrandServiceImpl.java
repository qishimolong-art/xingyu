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
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductBrandMapper;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_EXITS_PRODUCT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NOT_ENABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_CREATE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PRODUCT_BRAND_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_UPDATE_SUB_TYPE;

/**
 * ERP 配件品牌 Service 实现类
 */
@Service
@Validated
public class ErpProductBrandServiceImpl implements ErpProductBrandService {

    private static final String FIELD_PERMISSION_MODULE = "erp_product_brand";

    @Resource
    private ErpProductBrandMapper erpProductBrandMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Resource
    @Lazy
    private ErpProductService productService;

    @Override
    public Long createProductBrand(ErpProductBrandSaveReqVO createReqVO) {
        applyProductBrandSaveFieldPermissions(createReqVO, null);
        normalizeSaveReqVO(createReqVO);
        ValidationUtils.validate(createReqVO);
        validateProductBrandNameUnique(null, createReqVO.getName());
        ErpProductBrandDO brand = BeanUtils.toBean(createReqVO, ErpProductBrandDO.class);
        if (brand.getStatus() == null) {
            brand.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (brand.getSort() == null) {
            brand.setSort(0);
        }
        erpProductBrandMapper.insert(brand);
        operateLogService.recordCreate(ERP_PRODUCT_BRAND_TYPE, brand.getId(), brand, brand.getName());
        return brand.getId();
    }

    @Override
    public void updateProductBrand(ErpProductBrandSaveReqVO updateReqVO) {
        ErpProductBrandDO existing = validateProductBrandExists(updateReqVO.getId());
        applyProductBrandSaveFieldPermissions(updateReqVO, existing);
        normalizeSaveReqVO(updateReqVO);
        ValidationUtils.validate(updateReqVO);
        validateProductBrandNameUnique(updateReqVO.getId(), updateReqVO.getName());
        ErpProductBrandDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductBrandDO.class);
        erpProductBrandMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_PRODUCT_BRAND_TYPE, updateReqVO.getId(), existing,
                erpProductBrandMapper.selectById(updateReqVO.getId()), updateObj.getName());
    }

    @Override
    public void deleteProductBrand(Long id) {
        ErpProductBrandDO brand = validateProductBrandExists(id);
        if (productService.getProductCountByBrand(brand.getName()) > 0) {
            throw exception(PRODUCT_BRAND_EXITS_PRODUCT);
        }
        erpProductBrandMapper.deleteById(id);
        operateLogService.recordDelete(ERP_PRODUCT_BRAND_TYPE, id, brand, brand.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductBrandList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteProductBrand(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpProductBrandImportRespVO importProductBrandList(List<ErpProductBrandImportExcelVO> list) {
        ErpProductBrandImportRespVO result = new ErpProductBrandImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return result;
        }
        Map<String, ErpProductBrandDO> brandMap = buildProductBrandNameMap();
        for (int i = 0; i < list.size(); i++) {
            ErpProductBrandImportExcelVO row = list.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            int rowNo = i + 2;
            String name = trimToNull(row.getName());
            try {
                ErpProductBrandDO existing = brandMap.get(name);
                ErpProductBrandSaveReqVO saveReqVO = new ErpProductBrandSaveReqVO();
                saveReqVO.setId(existing == null ? null : existing.getId());
                saveReqVO.setName(name);
                saveReqVO.setStatus(row.getStatus() != null ? row.getStatus()
                        : existing == null ? CommonStatusEnum.ENABLE.getStatus() : existing.getStatus());
                saveReqVO.setSort(row.getSort() != null ? row.getSort() : existing == null ? 0 : existing.getSort());
                if (existing == null) {
                    createProductBrand(saveReqVO);
                    result.setCreateCount(result.getCreateCount() + 1);
                } else {
                    updateProductBrand(saveReqVO);
                    result.setUpdateCount(result.getUpdateCount() + 1);
                }
                brandMap = buildProductBrandNameMap();
            } catch (Exception ex) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailureDetails().add(new ErpProductBrandImportRespVO.FailureItem(
                        rowNo, name, getImportFailureReason(ex)));
            }
        }
        result.setSuccessCount(result.getCreateCount() + result.getUpdateCount());
        result.setFailureCount(result.getFailureDetails().size());
        return result;
    }

    @Override
    public List<ErpProductBrandImportExcelVO> parseCsvImport(Reader reader) {
        CsvReadConfig config = CsvReadConfig.defaultConfig().setContainsHeader(true);
        CsvReader csvReader = CsvUtil.getReader(config);
        CsvData csvData = csvReader.read(reader);
        List<ErpProductBrandImportExcelVO> result = new ArrayList<>();
        for (CsvRow row : csvData.getRows()) {
            ErpProductBrandImportExcelVO item = new ErpProductBrandImportExcelVO();
            item.setName(trimToNull(readCsvValue(row, "品牌名称", "品牌")));
            item.setStatus(parseInteger(readCsvValue(row, "状态")));
            item.setSort(parseInteger(readCsvValue(row, "排序")));
            result.add(item);
        }
        return result;
    }

    @Override
    public ErpProductBrandDO getProductBrand(Long id) {
        return applyProductBrandFieldPermissions(erpProductBrandMapper.selectById(id));
    }

    @Override
    public PageResult<ErpProductBrandDO> getProductBrandPage(ErpProductBrandPageReqVO pageReqVO) {
        PageResult<ErpProductBrandDO> pageResult = erpProductBrandMapper.selectPage(pageReqVO);
        List<ErpProductBrandDO> list = pageResult.getList().stream()
                .map(this::applyProductBrandFieldPermissions)
                .collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<ErpProductBrandDO> getProductBrandListByStatus(Integer status) {
        return erpProductBrandMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpProductBrandDO> getProductBrandListByStatusForCurrentUser(Integer status) {
        return erpProductBrandMapper.selectListByStatus(status).stream()
                .map(this::applyProductBrandFieldPermissions)
                .collect(Collectors.toList());
    }

    @Override
    public void validateEnabledProductBrand(String name) {
        String brandName = trimToNull(name);
        if (brandName == null) {
            return;
        }
        ErpProductBrandDO brand = erpProductBrandMapper.selectByName(brandName);
        if (brand == null) {
            throw exception(PRODUCT_BRAND_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(brand.getStatus())) {
            throw exception(PRODUCT_BRAND_NOT_ENABLED, brandName);
        }
    }

    @VisibleForTesting
    void validateProductBrandNameUnique(Long id, String name) {
        ErpProductBrandDO brand = erpProductBrandMapper.selectByName(name);
        if (brand == null) {
            return;
        }
        if (id == null || !brand.getId().equals(id)) {
            throw exception(PRODUCT_BRAND_NAME_DUPLICATE);
        }
    }

    private ErpProductBrandDO validateProductBrandExists(Long id) {
        ErpProductBrandDO brand = erpProductBrandMapper.selectById(id);
        if (brand == null) {
            throw exception(PRODUCT_BRAND_NOT_EXISTS);
        }
        return brand;
    }

    private void normalizeSaveReqVO(ErpProductBrandSaveReqVO reqVO) {
        reqVO.setName(trimToNull(reqVO.getName()));
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (reqVO.getSort() == null) {
            reqVO.setSort(0);
        }
    }

    private Map<String, ErpProductBrandDO> buildProductBrandNameMap() {
        List<ErpProductBrandDO> brands = erpProductBrandMapper.selectListAll();
        if (CollUtil.isEmpty(brands)) {
            return new HashMap<>();
        }
        return brands.stream()
                .filter(brand -> StrUtil.isNotBlank(trimToNull(brand.getName())))
                .collect(Collectors.toMap(brand -> trimToNull(brand.getName()), brand -> brand,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private boolean isEmptyRow(ErpProductBrandImportExcelVO row) {
        return row == null
                || (!StrUtil.isNotBlank(trimToNull(row.getName())) && row.getStatus() == null && row.getSort() == null);
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

    private Integer parseInteger(String value) {
        String trim = trimToNull(value);
        if (trim == null) {
            return null;
        }
        return Integer.valueOf(trim);
    }

    private String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    private ErpProductBrandDO applyProductBrandFieldPermissions(ErpProductBrandDO brand) {
        if (brand == null) {
            return null;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return brand;
        }
        ErpProductBrandDO result = BeanUtils.toBean(brand, ErpProductBrandDO.class);
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "name")) {
            result.setName(null);
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            result.setStatus(null);
        }
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            result.setSort(null);
        }
        return result;
    }

    private void applyProductBrandSaveFieldPermissions(ErpProductBrandSaveReqVO reqVO, ErpProductBrandDO existing) {
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
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            reqVO.setSort(existing == null ? null : existing.getSort());
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

}
