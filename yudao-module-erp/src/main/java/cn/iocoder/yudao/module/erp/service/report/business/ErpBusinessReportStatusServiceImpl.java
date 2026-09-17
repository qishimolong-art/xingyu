package cn.iocoder.yudao.module.erp.service.report.business;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.business.ErpBusinessReportStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.business.ErpBusinessReportStatusRespVO.ModuleStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

@Service
public class ErpBusinessReportStatusServiceImpl implements ErpBusinessReportStatusService {

    private static final String NAVIGATION_SQL = "code/sql/mysql/erp_business_report_navigation_v220.sql";
    private static final String EXTENSION_07_08_10_SQL = "code/sql/mysql/erp_business_report_extension_07_08_10_20260916.sql";
    private static final String FINANCIAL_SQL = "code/sql/mysql/erp_business_report_financial_20260916.sql";
    private static final String EXTENSION_11_12_SQL = "code/sql/mysql/erp_business_report_extension_11_12_20260916.sql";
    private static final String TRADE_PERMISSION_SQL = "code/sql/mysql/erp_trade_report_permission_v225.sql";
    private static final String STOCK_CURSOR_SQL = "code/sql/mysql/erp_stock_record_cursor_20260909.sql";

    @Override
    public ErpBusinessReportStatusRespVO getStatus() {
        ErpBusinessReportStatusRespVO respVO = new ErpBusinessReportStatusRespVO();
        respVO.setVersion("2026-09-17-plan00");
        respVO.setReportName("统一经营报表");
        respVO.setGlobalNotes(Arrays.asList(
                "所有计划统一归入经营报表，不再拆成独立正式报表口径",
                "采购价、销售价本阶段暂按含税口径展示，税率和未税净额待客户确认",
                "毛利率、评分、费用分摊、折旧、财务公式未确认前不能作为正式结果",
                "双成本开关默认关闭；库存游标 SQL 仅双成本启用准备、期初核对或开账前需要"
        ));
        respVO.setModules(Arrays.asList(
                module("00", "公共基础", "PARTIAL_READY",
                        list("统一经营报表前端入口、公共壳、阶段入口和预检脚本已有基础", "本接口返回 00-12 的统一状态、依赖和不可用原因"),
                        list("统一来源链字段、正式下钻模型、不可用原因模型仍待后续服务化", "真实菜单 SQL、角色授权、缓存刷新和真实角色验收未由本接口执行"),
                        list(NAVIGATION_SQL),
                        list("无新开关；只读状态接口不启用核算开关"),
                        list("任一经营报表相关查询权限可访问状态接口", "菜单 SQL 执行后需给真实角色勾选经营报表入口"),
                        list("本接口只反映当前确认口径，不替代真实角色、真实账套和浏览器验收")),
                module("01", "销售及客户经营", "READONLY_AVAILABLE",
                        list("销售 V2 真实过账与快照接口", "订单履约、拣货送货、退货调价和客户应收概览只读展示"),
                        list("毛利、最低售价、新客/沉睡/流失、忠诚度、回款率和商品维度收付款分摊未形成正式服务"),
                        list(TRADE_PERMISSION_SQL),
                        list("erp.reporting.dual-cost-enabled=false 默认关闭"),
                        list("erp:sale-report-v2:query", "erp:sale-report-v2:export", "erp:sale-report:query"),
                        list("销售价暂按含税；未知税率、未税收入、毛利和毛利率不能按 0 或临时公式输出")),
                module("02", "采购及供应商经营", "READONLY_AVAILABLE",
                        list("采购 V2 真实过账与快照接口", "采购订单、采购入库、采购退货、采购调价和供应商应付概览只读展示"),
                        list("供应商评分、履约质量、费用分摊、期间均价、退货差额损益和不含税口径仍待确认或闭环"),
                        list(TRADE_PERMISSION_SQL),
                        list("erp.reporting.dual-cost-enabled=false 默认关闭"),
                        list("erp:purchase-report-v2:query", "erp:purchase-report-v2:export", "erp:purchase-report:query"),
                        list("采购价暂按含税；供应商退款差额在税率和成本税基未确认前只展示不可用")),
                module("03", "库存、双成本及商品追溯", "READONLY_AVAILABLE_WITH_SWITCH_OFF",
                        list("库存 V2、库存流水、收发存、盘点和移仓可只读展示", "普通只读展示不依赖库存游标 SQL"),
                        list("正式两套成本余额、发生额、历史成本快照和零数量调价追溯未启用"),
                        list(STOCK_CURSOR_SQL + "（仅双成本准备、期初核对或启用前需要）"),
                        list("erp.reporting.dual-cost-enabled=false 默认关闭"),
                        list("erp:stock-record:query", "erp:stock-record:export", "erp:report-stock-opening:confirm"),
                        list("缺失成本或遮罩金额必须展示不可用/遮罩，不能转成 0 成本")),
                module("04", "变价调拨、移仓及在途", "READONLY_AVAILABLE",
                        list("调拨台账、调拨出入库、仓库移货、采购转调拨和手推车自动调拨链只读核对"),
                        list("调出过账与调入收货拆分、多级变价利润、在途结算成本、拒收退回、内部利润抵销仍缺"),
                        list(NAVIGATION_SQL),
                        list("双成本开关关闭时仅展示旧台账口径"),
                        list("erp:stock-transfer-ledger:query", "erp:stock-transfer-ledger:export"),
                        list("同部门移仓不得产生收入或利润；跨部门下钻不能越过库存和部门权限")),
                module("05", "销售采购往来及对账", "READONLY_AVAILABLE",
                        list("应收、应付、预收、预付、应收冲应付可只读展示"),
                        list("账龄历史截止、逾期、对账确认、差异待办、核销穿透、一款多单和一单多款仍需正式服务"),
                        list(NAVIGATION_SQL),
                        Collections.<String>emptyList(),
                        list("erp:receivable-report:query", "erp:payable-report:query", "erp:receivable-account:query", "erp:payable-account:query"),
                        list("其他往来继续与销售应收、采购应付隔离；应收冲应付只展示轧抵参考")),
                module("06", "其他应收及其他应付", "READONLY_AVAILABLE",
                        list("其他应收、其他应付独立页面和对象信息只读展示"),
                        list("期初、收回/支付、核销、冲销、调整、余额台账和历史核对仍不完整"),
                        list(NAVIGATION_SQL),
                        Collections.<String>emptyList(),
                        list("erp:other-receivable:query", "erp:other-payable:query"),
                        list("其他往来不能串入销售应收、采购应付权限或余额口径")),
                module("07", "账户资金及总部归集", "READONLY_AVAILABLE",
                        list("账户余额、账户流水、收款、付款、转账只读线索可展示"),
                        list("在途转款、到账确认、手续费承担部门、总部代收代付、外部对账差异和资金事件模型未闭环"),
                        list(EXTENSION_07_08_10_SQL),
                        Collections.<String>emptyList(),
                        list("erp:account:query", "erp:finance-receipt:query", "erp:finance-payment:query", "erp:finance-transfer:query"),
                        list("账户管理部门与业务归属部门权限需要分离核对；转账本金不直接计入经营收支")),
                module("08", "费用、分摊及其他经营收支", "READONLY_AVAILABLE",
                        list("费用支付、其他经营收入只读展示", "分摊和预算以准备看板展示"),
                        list("公共费用分摊单、预算版本、承担部门/付款部门拆分、分摊替代关系和反向分摊仍缺"),
                        list(EXTENSION_07_08_10_SQL),
                        Collections.<String>emptyList(),
                        list("erp:payable-expense:query", "erp:receivable-other-income:query"),
                        list("预算缺失时不能显示虚假完成率；公共费用分摊未确认前不进入部门利润")),
                module("09", "部门利润、公司抵销及驾驶舱", "BOUNDARY_ONLY",
                        list("可复用系统分析指标做联调和第一阶段展示"),
                        list("部门利润、公司抵销、未实现利润、目标配置、指标下钻、公式版本均未正式实现"),
                        list(NAVIGATION_SQL),
                        list("双成本、费用分摊、公司抵销、目标评分公式均未启用"),
                        list("erp:system-report:query"),
                        list("接口数据只能标识为非正式驾驶舱；静态数据只能标识为演示占位")),
                module("10", "发票、凭证及财务报表", "BOUNDARY_ONLY",
                        list("凭证、科目、票据和三大报表模板入口存在", "财务只读工作台可展示准备状态"),
                        list("科目余额、试算平衡、明细账、会计期间、结账反结账、现金流项目和财务报表公式版本未闭环"),
                        list(EXTENSION_07_08_10_SQL, FINANCIAL_SQL),
                        Collections.<String>emptyList(),
                        list("erp:voucher:query", "erp:voucher-rule:query", "erp:voucher-attribution:generate"),
                        list("三大报表金额列必须保持不可用边界，公式未确认前不能导出为正式报表")),
                module("11", "固定资产及折旧", "PREPARING",
                        list("当前仅准备看板"),
                        list("资产卡片、期初导入、月度折旧、部门转移、处置和折旧凭证全部缺失"),
                        list(EXTENSION_11_12_SQL),
                        Collections.<String>emptyList(),
                        Collections.<String>emptyList(),
                        list("不能生成固定资产金额、折旧费用、处置损益或凭证结果；同资产同期间必须避免重复计提")),
                module("12", "异常、审计、业务追踪及岗位绩效", "READONLY_CLUES",
                        list("库存流水、系统操作日志、导入导出记录、采购发票 OCR 异常可作只读线索"),
                        list("完整业务链索引、异常规则、待办闭环、处理复核、评分规则版本和岗位绩效服务未完成"),
                        list(EXTENSION_11_12_SQL),
                        Collections.<String>emptyList(),
                        list("erp:stock-record:query", "system:operate-log:query", "erp:purchase-invoice-ocr:query", "erp:import-record:query", "erp:export-record:query"),
                        list("这些线索不等同于完整业务链、异常闭环或绩效评分；下钻不能绕过部门或金额权限"))
        ));
        return respVO;
    }

    private static ModuleStatus module(String code, String name, String status, java.util.List<String> displayCapabilities,
                                       java.util.List<String> pendingItems, java.util.List<String> dependencySql,
                                       java.util.List<String> dependencySwitches, java.util.List<String> dependencyPermissions,
                                       java.util.List<String> riskNotes) {
        ModuleStatus moduleStatus = new ModuleStatus();
        moduleStatus.setCode(code);
        moduleStatus.setName(name);
        moduleStatus.setStatus(status);
        moduleStatus.setDisplayCapabilities(displayCapabilities);
        moduleStatus.setPendingItems(pendingItems);
        moduleStatus.setDependencySql(dependencySql);
        moduleStatus.setDependencySwitches(dependencySwitches);
        moduleStatus.setDependencyPermissions(dependencyPermissions);
        moduleStatus.setRiskNotes(riskNotes);
        return moduleStatus;
    }

    private static java.util.List<String> list(String... values) {
        return Arrays.asList(values);
    }

}

