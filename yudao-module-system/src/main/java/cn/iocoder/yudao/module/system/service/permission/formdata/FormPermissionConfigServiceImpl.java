package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateColumnRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateTableRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionFieldConfigSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionFieldConfigDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionTableConfigDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FormPermissionFieldConfigMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FormPermissionTableConfigMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionFieldValueTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FormPermissionConfigServiceImpl implements FormPermissionConfigService {

    private static final Set<String> INTERNAL_TABLES = new HashSet<String>() {{
        add("form_data_permission");
        add("form_permission_table_config");
        add("form_permission_field_config");
    }};
    private static final Map<String, String> ERP_FORM_TABLE_BY_COMPONENT = new HashMap<String, String>() {{
        put("erp/purchase/supplier/index", "erp_supplier");
        put("erp/purchase/order/index", "erp_purchase_order");
        put("erp/purchase/in/index", "erp_purchase_in");
        put("erp/purchase/return/index", "erp_purchase_return");
        put("erp/purchase/invoice/index", "erp_purchase_invoice");
        put("erp/purchase/priceadjust/index", "erp_purchase_price_adjust");
        put("erp/sale/customer/index", "erp_customer");
        put("erp/sale/cart/index", "erp_sale_cart");
        put("erp/sale/order/index", "erp_sale_order");
        put("erp/sale/quote/index", "erp_sale_quote");
        put("erp/sale/out/index", "erp_sale_out");
        put("erp/sale/return/index", "erp_sale_return");
        put("erp/product/product/index", "erp_product");
        put("erp/product/category/index", "erp_product_category");
        put("erp/product/unit/index", "erp_product_unit");
        put("erp/product/pricesystem/index", "erp_price_system");
        put("erp/stock/stock/index", "erp_stock");
        put("erp/stock/record/index", "erp_stock_record");
        put("erp/stock/stock-record/index", "erp_stock_record");
        put("erp/stock/warehouse/index", "erp_warehouse");
        put("erp/stock/in/index", "erp_stock_in");
        put("erp/stock/out/index", "erp_stock_out");
        put("erp/stock/move/index", "erp_stock_move");
        put("erp/stock/check/index", "erp_stock_check");
        put("erp/stock/warehouse-move/index", "erp_warehouse_move");
        put("erp/stock/transfer-in/index", "erp_stock_transfer_in");
        put("erp/stock/transfer-out/index", "erp_stock_transfer_out");
        put("erp/finance/account/index", "erp_account");
        put("erp/finance/cash-bank/account/index", "erp_account");
        put("erp/finance/payment/index", "erp_finance_payment");
        put("erp/finance/receipt/index", "erp_finance_receipt");
        put("erp/finance/transfer/index", "erp_finance_transfer");
        put("erp/finance/cash-bank/payment/index", "erp_finance_payment");
        put("erp/finance/cash-bank/receipt/index", "erp_finance_receipt");
        put("erp/finance/cash-bank/transfer/index", "erp_finance_transfer");
        put("erp/base/region/index", "erp_base_region");
        put("erp/base/category/index", "erp_base_category");
        put("erp/base/supplier-type/index", "erp_supplier_type");
        put("erp/base/logistics-company/index", "erp_logistics_company");
        put("erp/finance/accounting/subject/index", "erp_accounting_subject");
        put("erp/finance/accounting/book-open/index", "erp_book_open");
        put("erp/finance/accounting/voucher/index", "erp_voucher");
        put("erp/finance/accounting/voucher-attribution/index", "erp_voucher_attribution");
        put("erp/finance/accounting/voucher-word/index", "erp_voucher_word");
        put("erp/finance/accounting/pre-receipt/index", "erp_pre_receipt");
        put("erp/finance/accounting/pre-payment/index", "erp_pre_payment");
        put("erp/finance/accounting/pre-receivable/index", "erp_pre_receivable");
        put("erp/finance/accounting/other-receivable/index", "erp_other_receivable");
        put("erp/finance/accounting/other-payable/index", "erp_other_payable");
    }};

    @Resource
    private FormPermissionTableConfigMapper tableConfigMapper;
    @Resource
    private FormPermissionFieldConfigMapper fieldConfigMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private FormPermissionConfigRegistry registry;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<FormPermissionConfigRespVO> getConfigPage(FormPermissionPageReqVO pageReqVO) {
        PageResult<FormPermissionTableConfigDO> pageResult = tableConfigMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<FormPermissionTableConfigDO>()
                        .likeIfPresent(FormPermissionTableConfigDO::getFormType, pageReqVO.getFormType())
                        .eqIfPresent(FormPermissionTableConfigDO::getEnabled, pageReqVO.getEnabled())
                        .orderByDesc(FormPermissionTableConfigDO::getId));
        PageResult<FormPermissionConfigRespVO> result = BeanUtils.toBean(pageResult, FormPermissionConfigRespVO.class);
        if (CollUtil.isEmpty(result.getList())) {
            return result;
        }
        List<String> formTypes = result.getList().stream().map(FormPermissionConfigRespVO::getFormType)
                .collect(Collectors.toList());
        Map<String, Long> countMap = fieldConfigMapper.selectListByFormTypes(formTypes).stream()
                .filter(field -> Boolean.TRUE.equals(field.getEnabled()))
                .collect(Collectors.groupingBy(FormPermissionFieldConfigDO::getFormType, Collectors.counting()));
        result.getList().forEach(item -> item.setFieldCount(countMap.getOrDefault(item.getFormType(), 0L).intValue()));
        return result;
    }

    @Override
    public List<FormPermissionCandidateTableRespVO> getCandidateTables() {
        Set<String> configuredFormTypes = tableConfigMapper.selectListByEnabled(null).stream()
                .map(FormPermissionTableConfigDO::getFormType).collect(Collectors.toSet());
        List<MenuDO> menus = menuMapper.selectList(new LambdaQueryWrapperX<MenuDO>()
                .eq(MenuDO::getType, MenuTypeEnum.MENU.getType())
                .eq(MenuDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .likeRight(MenuDO::getComponent, "erp/")
                .orderByAsc(MenuDO::getSort));
        Map<String, FormPermissionCandidateTableRespVO> candidateMap = new LinkedHashMap<>();
        for (MenuDO menu : menus) {
            if (menu.getComponent().startsWith("erp/system/")) {
                continue;
            }
            String formType = resolveErpFormType(menu);
            if (!StringUtils.hasText(formType) || INTERNAL_TABLES.contains(formType) || !tableExists(formType)) {
                continue;
            }
            if (getAllColumns(formType).stream().noneMatch(FormPermissionCandidateColumnRespVO::getRecommended)) {
                continue;
            }
            candidateMap.computeIfAbsent(formType, key -> {
                FormPermissionCandidateTableRespVO vo = new FormPermissionCandidateTableRespVO();
                vo.setFormType(formType);
                vo.setTableDesc(menu.getName());
                vo.setConfigured(configuredFormTypes.contains(formType));
                return vo;
            });
        }
        return candidateMap.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<FormPermissionCandidateColumnRespVO> getCandidateColumns(String tableName) {
        FormPermissionIdentifierUtils.checkIdentifier(tableName, "表名");
        return getAllColumns(tableName).stream()
                .filter(FormPermissionCandidateColumnRespVO::getRecommended)
                .collect(Collectors.toList());
    }

    @Override
    public FormPermissionConfigRespVO getConfig(String formType) {
        FormPermissionIdentifierUtils.checkIdentifier(formType, "表名");
        FormPermissionTableConfigDO tableConfig = tableConfigMapper.selectByFormType(formType);
        if (tableConfig == null) {
            FormPermissionConfigRespVO empty = new FormPermissionConfigRespVO();
            empty.setFormType(formType);
            empty.setEnabled(Boolean.TRUE);
            empty.setFieldCount(0);
            empty.setFields(BeanUtils.toBean(fieldConfigMapper.selectListByFormType(formType),
                    FormPermissionFieldConfigRespVO.class));
            return empty;
        }
        FormPermissionConfigRespVO vo = BeanUtils.toBean(tableConfig, FormPermissionConfigRespVO.class);
        List<FormPermissionFieldConfigRespVO> fields = BeanUtils.toBean(fieldConfigMapper.selectListByFormType(formType),
                FormPermissionFieldConfigRespVO.class);
        vo.setFields(fields);
        vo.setFieldCount((int) fields.stream().filter(field -> Boolean.TRUE.equals(field.getEnabled())).count());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(FormPermissionConfigSaveReqVO reqVO) {
        FormPermissionIdentifierUtils.checkIdentifier(reqVO.getFormType(), "表名");
        ensureTableExists(reqVO.getFormType());
        Map<String, FormPermissionCandidateColumnRespVO> columnMap = getAllColumns(reqVO.getFormType()).stream()
                .collect(Collectors.toMap(FormPermissionCandidateColumnRespVO::getColumnName, item -> item));

        FormPermissionTableConfigDO tableConfig = tableConfigMapper.selectByFormType(reqVO.getFormType());
        if (tableConfig == null) {
            tableConfig = BeanUtils.toBean(reqVO, FormPermissionTableConfigDO.class);
            tableConfigMapper.insert(tableConfig);
        } else {
            tableConfig.setTableDesc(reqVO.getTableDesc());
            tableConfig.setEnabled(reqVO.getEnabled());
            tableConfigMapper.updateById(tableConfig);
        }

        fieldConfigMapper.deleteByFormType(reqVO.getFormType());
        if (CollUtil.isNotEmpty(reqVO.getFields())) {
            for (FormPermissionFieldConfigSaveReqVO fieldReqVO : reqVO.getFields()) {
                FormPermissionIdentifierUtils.checkIdentifier(fieldReqVO.getColumnName(), "列名");
                String valueType = StringUtils.hasText(fieldReqVO.getValueType())
                        ? fieldReqVO.getValueType() : FormPermissionFieldValueTypeEnum.SINGLE_ID.getCode();
                if (!FormPermissionFieldValueTypeEnum.contains(valueType)) {
                    throw new IllegalArgumentException("不支持的值类型: " + valueType);
                }
                FormPermissionCandidateColumnRespVO column = columnMap.get(fieldReqVO.getColumnName());
                if (column == null) {
                    throw new IllegalArgumentException("字段不存在: " + fieldReqVO.getColumnName());
                }
                if (!Boolean.TRUE.equals(column.getRecommended())) {
                    throw new IllegalArgumentException("字段不是人员相关字段: " + fieldReqVO.getColumnName());
                }
                FormPermissionFieldConfigDO fieldConfig = BeanUtils.toBean(fieldReqVO, FormPermissionFieldConfigDO.class);
                fieldConfig.setFormType(reqVO.getFormType());
                fieldConfig.setValueType(valueType);
                if (fieldConfig.getColumnDesc() == null) {
                    fieldConfig.setColumnDesc(column.getColumnDesc());
                }
                fieldConfigMapper.insert(fieldConfig);
            }
        }
        registry.refresh();
    }

    private void ensureTableExists(String tableName) {
        if (!tableExists(tableName)) {
            throw new IllegalArgumentException("表不存在: " + tableName);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, tableName);
        return count != null && count > 0;
    }

    private String resolveErpFormType(MenuDO menu) {
        String component = menu.getComponent();
        String explicitFormType = ERP_FORM_TABLE_BY_COMPONENT.get(component);
        if (StringUtils.hasText(explicitFormType)) {
            return explicitFormType;
        }
        String componentFormType = resolveErpFormTypeByComponent(component);
        if (StringUtils.hasText(componentFormType) && tableExists(componentFormType)) {
            return componentFormType;
        }
        return resolveErpFormTypeByPermission(menu.getPermission());
    }

    private String resolveErpFormTypeByPermission(String permission) {
        if (!StringUtils.hasText(permission) || !permission.startsWith("erp:")) {
            return null;
        }
        String permissionModule = permission.substring("erp:".length());
        int actionIndex = permissionModule.indexOf(':');
        if (actionIndex >= 0) {
            permissionModule = permissionModule.substring(0, actionIndex);
        }
        if (!StringUtils.hasText(permissionModule)) {
            return null;
        }
        return "erp_" + permissionModule.replace('-', '_');
    }

    private String resolveErpFormTypeByComponent(String component) {
        if (!StringUtils.hasText(component) || !component.startsWith("erp/")) {
            return null;
        }
        String path = component.substring("erp/".length());
        int indexSuffix = path.lastIndexOf("/index");
        if (indexSuffix >= 0) {
            path = path.substring(0, indexSuffix);
        }
        if (!StringUtils.hasText(path)) {
            return null;
        }
        return "erp_" + path.replace('/', '_').replace('-', '_');
    }

    private List<FormPermissionCandidateColumnRespVO> getAllColumns(String tableName) {
        FormPermissionIdentifierUtils.checkIdentifier(tableName, "表名");
        String sql = "SELECT column_name, column_comment, data_type FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String columnName = rs.getString("column_name");
            String columnDesc = rs.getString("column_comment");
            FormPermissionCandidateColumnRespVO vo = new FormPermissionCandidateColumnRespVO();
            vo.setColumnName(columnName);
            vo.setColumnDesc(columnDesc);
            vo.setDataType(rs.getString("data_type"));
            vo.setRecommended(isUserRelatedColumn(columnName, columnDesc));
            return vo;
        }, tableName);
    }

    private boolean isUserRelatedColumn(String columnName, String columnDesc) {
        String lower = columnName.toLowerCase(Locale.ROOT);
        if (lower.endsWith("_name") || lower.endsWith("name") || lower.endsWith("_no")
                || lower.endsWith("no") || lower.contains("phone") || lower.contains("mobile")) {
            return false;
        }
        if ("creator".equals(lower) || "updater".equals(lower) || "user_id".equals(lower)
                || lower.endsWith("_user_id") || lower.endsWith("_user_ids") || lower.endsWith("_uid")
                || lower.endsWith("_by") || lower.contains("handler") || lower.contains("owner")
                || lower.contains("purchaser") || lower.contains("sale_user") || lower.contains("salesman")
                || lower.contains("operator") || lower.contains("auditor") || lower.contains("approver")) {
            return true;
        }
        if (columnDesc == null) {
            return false;
        }
        return columnDesc.contains("用户") || columnDesc.contains("人员") || columnDesc.contains("负责人")
                || columnDesc.contains("采购员") || columnDesc.contains("销售员") || columnDesc.contains("业务员")
                || columnDesc.contains("经手人") || columnDesc.contains("制单人") || columnDesc.contains("创建人")
                || columnDesc.contains("更新人") || columnDesc.contains("操作员") || columnDesc.contains("审核人")
                || columnDesc.contains("审批人");
    }

}
