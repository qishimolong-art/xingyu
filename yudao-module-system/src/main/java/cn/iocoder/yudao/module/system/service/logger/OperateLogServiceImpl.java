package cn.iocoder.yudao.module.system.service.logger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.iocoder.yudao.module.system.controller.admin.logger.vo.operatelog.OperateLogModuleOptionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO;
import cn.iocoder.yudao.module.system.dal.mysql.logger.OperateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 操作日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class OperateLogServiceImpl implements OperateLogService {

    private static final List<String> DEFAULT_MODULE_OPTIONS = Arrays.asList(
            "采购订单", "采购入库", "采购退货", "采购调价", "采购票据", "供应商档案",
            "销售订单", "销售出库", "销售退货", "销售报价", "销售手推车", "销售调价", "客户档案",
            "配件信息", "配件分类", "配件单位", "仓库信息",
            "银行账户", "收款单", "付款单", "银行转账单", "其他应收", "其他收入", "其他应付", "费用支出",
            "库存入库单", "库存出库单", "库存调拨单", "库存盘点单",
            "用户管理", "角色管理", "部门管理", "岗位管理", "菜单管理", "字典管理", "操作日志", "登录日志");

    @Resource
    private OperateLogMapper operateLogMapper;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        OperateLogDO log = BeanUtils.toBean(createReqDTO, OperateLogDO.class);
        operateLogMapper.insert(log);
    }

    @Override
    public OperateLogDO getOperateLog(Long id) {
        return operateLogMapper.selectById(id);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO) {
        return operateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        return operateLogMapper.selectPage(pageReqDTO);
    }

    @Override
    public List<OperateLogModuleOptionRespVO> getOperateLogModuleOptions() {
        TreeSet<String> modules = new TreeSet<>();
        modules.addAll(DEFAULT_MODULE_OPTIONS);
        operateLogMapper.selectListForModuleOptions().stream()
                .map(OperateLogDO::getType)
                .map(OperateLogServiceImpl::normalizeModuleName)
                .filter(StringUtils::hasText)
                .forEach(modules::add);
        return modules.stream()
                .map(module -> new OperateLogModuleOptionRespVO(module, module))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static String normalizeModuleName(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String moduleName = type.trim();
        if (moduleName.startsWith("ERP")) {
            return moduleName.substring("ERP".length()).trim();
        }
        if (moduleName.startsWith("SYSTEM")) {
            return moduleName.substring("SYSTEM".length()).trim();
        }
        return moduleName;
    }

}
