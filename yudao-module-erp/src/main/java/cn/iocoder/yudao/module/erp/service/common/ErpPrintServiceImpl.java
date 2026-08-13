package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintRecordCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.enums.print.ErpPrintModuleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_MODULE_NOT_SUPPORTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_TEMPLATE_NOT_EXISTS;

@Service
@Validated
public class ErpPrintServiceImpl implements ErpPrintService {

    public static final String MODULE_PURCHASE_ORDER = "purchase_order";
    private static final String FIELD_MODULE_PURCHASE_ORDER = "erp_purchase_order";

    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;
    @Resource
    private ErpPrintRecordMapper printRecordMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public ErpPrintFieldRespVO getFields(String moduleKey) {
        validateModule(moduleKey);
        ErpPrintFieldRespVO respVO = new ErpPrintFieldRespVO();
        respVO.setModuleKey(moduleKey);
        if (MODULE_PURCHASE_ORDER.equals(moduleKey)) {
            respVO.setGroups(buildPurchaseOrderFields());
        }
        return respVO;
    }

    @Override
    public ErpPrintTemplateRespVO getDefaultTemplate(String moduleKey) {
        validateModule(moduleKey);
        ErpPrintTemplateDO template = printTemplateMapper.selectDefaultByModuleKey(moduleKey);
        return template == null ? null : BeanUtils.toBean(template, ErpPrintTemplateRespVO.class);
    }

    @Override
    public List<ErpPrintTemplateRespVO> getTemplateList(String moduleKey) {
        validateModule(moduleKey);
        return BeanUtils.toBean(printTemplateMapper.selectListByModuleKey(moduleKey), ErpPrintTemplateRespVO.class);
    }

    @Override
    public ErpPrintTemplateRespVO getTemplate(Long id) {
        ErpPrintTemplateDO template = validateTemplateExists(id);
        return BeanUtils.toBean(template, ErpPrintTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTemplate(ErpPrintTemplateSaveReqVO reqVO) {
        validateModule(reqVO.getModuleKey());
        ErpPrintTemplateDO saveObj = BeanUtils.toBean(reqVO, ErpPrintTemplateDO.class);
        if (saveObj.getStatus() == null) {
            saveObj.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (saveObj.getDefaulted() == null) {
            saveObj.setDefaulted(true);
        }
        if (Boolean.TRUE.equals(saveObj.getDefaulted())) {
            printTemplateMapper.clearDefaultByModuleKey(saveObj.getModuleKey());
        }
        if (saveObj.getId() == null) {
            printTemplateMapper.insert(saveObj);
            return saveObj.getId();
        }
        validateTemplateExists(saveObj.getId());
        printTemplateMapper.updateById(saveObj);
        return saveObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAsTemplate(ErpPrintTemplateSaveReqVO reqVO) {
        reqVO.setId(null);
        return saveTemplate(reqVO);
    }

    @Override
    public void recordPrint(ErpPrintRecordCreateReqVO reqVO) {
        validateModule(reqVO.getModuleKey());
        Long loginUserId = getLoginUserId();
        AdminUserRespDTO user = loginUserId == null ? null : adminUserApi.getUser(loginUserId);
        printRecordMapper.insert(ErpPrintRecordDO.builder()
                .moduleKey(reqVO.getModuleKey())
                .businessId(reqVO.getBusinessId())
                .businessNo(reqVO.getBusinessNo())
                .templateId(reqVO.getTemplateId())
                .printerId(loginUserId)
                .printerName(user == null ? null : user.getNickname())
                .printTime(LocalDateTime.now())
                .build());
    }

    @Override
    public Map<Long, Long> getPrintCountMap(String moduleKey, Collection<Long> businessIds) {
        if (CollUtil.isEmpty(businessIds)) {
            return new LinkedHashMap<>();
        }
        Map<Long, Long> result = new LinkedHashMap<>();
        businessIds.forEach(id -> result.put(id, 0L));
        printRecordMapper.selectListByBusinessIds(moduleKey, businessIds)
                .forEach(record -> result.compute(record.getBusinessId(), (key, value) -> value == null ? 1L : value + 1L));
        return result;
    }

    @Override
    public Map<Long, LocalDateTime> getLastPrintTimeMap(String moduleKey, Collection<Long> businessIds) {
        if (CollUtil.isEmpty(businessIds)) {
            return new LinkedHashMap<>();
        }
        Map<Long, LocalDateTime> result = new LinkedHashMap<>();
        printRecordMapper.selectListByBusinessIds(moduleKey, businessIds).forEach(record -> {
            if (!result.containsKey(record.getBusinessId())) {
                result.put(record.getBusinessId(), record.getPrintTime());
            }
        });
        return result;
    }

    private ErpPrintTemplateDO validateTemplateExists(Long id) {
        ErpPrintTemplateDO template = printTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(PRINT_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private void validateModule(String moduleKey) {
        if (!ErpPrintModuleEnum.isSupported(moduleKey)) {
            throw exception(PRINT_MODULE_NOT_SUPPORTED);
        }
    }

    private List<ErpPrintFieldRespVO.Group> buildPurchaseOrderFields() {
        List<ErpPrintFieldRespVO.Group> groups = new ArrayList<>();
        List<ErpPrintFieldRespVO.Field> mainFields = new ArrayList<>();
        List<ErpPrintFieldRespVO.Field> detailFields = new ArrayList<>();
        for (FieldDefinitionRespDTO definition : permissionApi.getFieldDefinitions(FIELD_MODULE_PURCHASE_ORDER)) {
            ErpPrintFieldRespVO.Field field = buildPurchaseOrderField(definition);
            if (field == null) {
                continue;
            }
            if (definition.getFieldKey() != null && definition.getFieldKey().startsWith("item_")) {
                detailFields.add(field);
            } else {
                mainFields.add(field);
            }
        }
        groups.add(new ErpPrintFieldRespVO.Group("main", "主表字段", "拖到单元格", mainFields));
        groups.add(new ErpPrintFieldRespVO.Group("detail", "明细字段", "拖到明细行", detailFields));
        groups.add(new ErpPrintFieldRespVO.Group("system", "系统字段", "通用", fields(
                field("打印时间", "print.now", "system"),
                field("当前用户", "currentUser.nickname", "system")
        )));
        return groups;
    }

    private ErpPrintFieldRespVO.Field buildPurchaseOrderField(FieldDefinitionRespDTO definition) {
        if (definition == null || definition.getFieldKey() == null) {
            return null;
        }
        String code = toPurchaseOrderPrintCode(definition.getFieldKey());
        if (code == null) {
            return null;
        }
        String source = definition.getFieldKey().startsWith("item_") ? "erp_purchase_order / detail_item"
                : "erp_purchase_order / main_form";
        return field(definition.getFieldLabel(), code, source);
    }

    private String toPurchaseOrderPrintCode(String fieldKey) {
        if ("items".equals(fieldKey)) {
            return "items";
        }
        if (fieldKey.startsWith("item_")) {
            return switch (fieldKey) {
                case "item_productId" -> "items.productName";
                case "item_totalProductPrice" -> "items.totalPrice";
                default -> "items." + lowerFirst(fieldKey.substring("item_".length()));
            };
        }
        return switch (fieldKey) {
            case "no" -> "order.no";
            case "supplierId" -> "supplier.name";
            case "purchaser" -> "purchaser.nickname";
            case "deptId" -> "dept.name";
            default -> "order." + lowerFirst(fieldKey);
        };
    }

    private String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private List<ErpPrintFieldRespVO.Field> fields(ErpPrintFieldRespVO.Field... fields) {
        return List.of(fields);
    }

    private ErpPrintFieldRespVO.Field field(String name, String code, String source) {
        return new ErpPrintFieldRespVO.Field(name, code, source);
    }

}
