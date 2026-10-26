package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerCreditStatusBO;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerSaleStatsBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/** 客户列表与导出共用的响应组装，避免异步导出口径偏离列表。 */
@Service
public class ErpCustomerQueryService {

    private static final String FIELD_PERMISSION_MODULE = "erp_customer";

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpCustomerBusinessInfoMapper customerBusinessInfoMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpBaseDataMapper baseDataMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    public PageResult<ErpCustomerRespVO> buildCustomerPage(PageResult<ErpCustomerDO> page) {
        PageResult<ErpCustomerRespVO> response = BeanUtils.toBean(page, ErpCustomerRespVO.class);
        fillCustomerExtra(response.getList());
        fillPageStatistics(response.getList());
        maskCustomerFields(response.getList());
        fillBusinessInfoSynced(response.getList());
        return response;
    }

    public List<ErpCustomerRespVO> buildCustomerExportList(List<ErpCustomerDO> rows) {
        List<ErpCustomerRespVO> response = BeanUtils.toBean(rows, ErpCustomerRespVO.class);
        fillCustomerExtra(response);
        maskCustomerFields(response);
        return response;
    }

    public void fillCustomerExtra(List<ErpCustomerRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        list.forEach(row -> {
            if (row.getSaleUserId() != null) userIds.add(row.getSaleUserId());
            if (row.getDeveloperUserId() != null) userIds.add(row.getDeveloperUserId());
            for (String id : Arrays.asList(row.getCreator(), row.getUpdater())) {
                if (id != null && id.matches("[0-9]+")) userIds.add(Long.valueOf(id));
            }
        });
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty()
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        list.forEach(row -> {
            AdminUserRespDTO saleUser = users.get(row.getSaleUserId());
            AdminUserRespDTO developer = users.get(row.getDeveloperUserId());
            row.setSaleUserName(saleUser == null ? null : saleUser.getNickname());
            row.setDeveloperUserName(developer == null ? null : developer.getNickname());
            AdminUserRespDTO creator = row.getCreator() != null && row.getCreator().matches("[0-9]+")
                    ? users.get(Long.valueOf(row.getCreator())) : null;
            AdminUserRespDTO updater = row.getUpdater() != null && row.getUpdater().matches("[0-9]+")
                    ? users.get(Long.valueOf(row.getUpdater())) : null;
            row.setCreatorName(creator == null ? row.getCreator() : creator.getNickname());
            row.setUpdaterName(updater == null ? row.getUpdater() : updater.getNickname());
        });
        Set<Long> optionIds = new HashSet<>();
        list.forEach(row -> {
            if (row.getRouteId() != null) optionIds.add(row.getRouteId());
            if (row.getAreaId() != null) optionIds.add(row.getAreaId());
            if (row.getFreightExplainId() != null) optionIds.add(row.getFreightExplainId());
        });
        Map<Long, ErpBaseDataDO> optionMap = optionIds.isEmpty()
                ? Collections.emptyMap() : convertMap(baseDataMapper.selectByIds(optionIds), ErpBaseDataDO::getId);
        Map<String, Integer> typeCodes = new LinkedHashMap<>();
        typeCodes.put("car_owner", 1);
        typeCodes.put("repair_factory", 2);
        typeCodes.put("terminal_customer", 3);
        typeCodes.put("customer_supplier", 4);
        typeCodes.put("supplier", 5);
        typeCodes.put("store", 6);
        typeCodes.put("dealer", 7);
        Map<String, Integer> settleCodes = new LinkedHashMap<>();
        settleCodes.put("cash", 1);
        settleCodes.put("credit", 2);
        settleCodes.put("remittance", 3);
        settleCodes.put("online_payment", 4);
        Set<String> codes = new HashSet<>(typeCodes.keySet());
        codes.addAll(settleCodes.keySet());
        Map<Integer, String> types = new LinkedHashMap<>();
        Map<Integer, String> settlements = new LinkedHashMap<>();
        baseDataMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpBaseDataDO>()
                        .in(ErpBaseDataDO::getType, Arrays.asList("customer_type", "settle_method"))
                        .in(ErpBaseDataDO::getCode, codes))
                .forEach(option -> {
                    Integer value = ("customer_type".equals(option.getType()) ? typeCodes : settleCodes)
                            .get(option.getCode());
                    if (value != null) {
                        ("customer_type".equals(option.getType()) ? types : settlements)
                                .put(value, option.getName());
                    }
                });
        list.forEach(row -> {
            ErpBaseDataDO route = optionMap.get(row.getRouteId());
            ErpBaseDataDO area = optionMap.get(row.getAreaId());
            row.setRouteName(route == null ? null : route.getName());
            row.setAreaName(area == null ? null : area.getName());
            row.setCustomerTypeName(types.get(row.getCustomerType()));
            row.setSettleMethodName(settlements.get(row.getSettleMethod()));
        });
        Map<Long, List<Long>> customerDeptMap = customerService.getCustomerDeptMap(
                convertList(list, ErpCustomerRespVO::getId));
        Set<Long> deptIds = new HashSet<>();
        list.forEach(customer -> {
            if (customer.getDeptId() != null) deptIds.add(customer.getDeptId());
            List<Long> currentDeptIds = customerDeptMap.getOrDefault(customer.getId(), Collections.emptyList());
            customer.setDeptIds(currentDeptIds);
            deptIds.addAll(currentDeptIds);
        });
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        list.forEach(customer -> {
            DeptRespDTO dept = deptMap.get(customer.getDeptId());
            if (dept != null) customer.setDeptName(dept.getName());
            customer.setDeptNames(customer.getDeptIds().stream()
                    .map(deptMap::get)
                    .filter(item -> item != null && item.getName() != null)
                    .map(DeptRespDTO::getName)
                    .collect(Collectors.joining("、")));
        });
    }

    public void maskCustomerFields(List<ErpCustomerRespVO> rows) {
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, rows);
        rows.forEach(row -> {
            if (row.getSaleUserId() == null) row.setSaleUserName(null);
            if (row.getDeveloperUserId() == null) row.setDeveloperUserName(null);
            if (row.getRouteId() == null) row.setRouteName(null);
            if (row.getAreaId() == null) row.setAreaName(null);
            if (row.getCustomerType() == null) row.setCustomerTypeName(null);
            if (row.getSettleMethod() == null) row.setSettleMethodName(null);
            if (row.getCreator() == null) row.setCreatorName(null);
            if (row.getUpdater() == null) row.setUpdaterName(null);
        });
    }

    public void fillBusinessInfoSynced(List<ErpCustomerRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> customerIds = convertSet(list, ErpCustomerRespVO::getId);
        Set<Long> syncedCustomerIds = convertSet(
                customerBusinessInfoMapper.selectListByCustomerIds(customerIds),
                ErpCustomerBusinessInfoDO::getCustomerId);
        list.forEach(customer -> customer.setBusinessInfoSynced(syncedCustomerIds.contains(customer.getId())));
    }

    private void fillPageStatistics(List<ErpCustomerRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<Long> customerIds = convertList(list, ErpCustomerRespVO::getId);
        Map<Long, ErpCustomerSaleStatsBO> statsMap = convertMap(
                saleOutMapper.selectSaleStatsByCustomerIds(customerIds), ErpCustomerSaleStatsBO::getCustomerId);
        list.forEach(vo -> {
            ErpCustomerSaleStatsBO stats = statsMap.get(vo.getId());
            if (stats != null) {
                if (stats.getLastSaleTime() != null) vo.setLastSaleDate(stats.getLastSaleTime().toLocalDate());
                vo.setTotalSaleAmount(stats.getTotalSaleAmount());
                vo.setReceivableBalance(stats.getReceivableBalance());
            }
        });
        Map<Long, ErpCustomerCreditStatusBO> creditStatusMap = customerService.getCustomerCreditStatusMap(customerIds);
        list.forEach(vo -> applyCreditStatus(vo, creditStatusMap.get(vo.getId())));
    }

    private void applyCreditStatus(ErpCustomerRespVO vo, ErpCustomerCreditStatusBO status) {
        if (vo == null || status == null) {
            return;
        }
        vo.setReceivableBalance(status.getReceivableBalance());
        vo.setEarliestUnpaidDate(status.getEarliestUnpaidDate());
        vo.setDebtDays(status.getDebtDays());
        vo.setCreditAmountExceeded(status.getAmountExceeded());
        vo.setCreditTermExceeded(status.getTermExceeded());
        vo.setCreditBlocked(status.getBlocked());
        vo.setCreditBlockedReason(status.getBlockedReason());
        vo.setDisabled(Boolean.TRUE.equals(status.getBlocked()));
    }

}
