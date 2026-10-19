package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.framework.ip.core.utils.AreaUtils;

final class ErpSaleReportAreaFormatter {

    private ErpSaleReportAreaFormatter() {
    }

    static String format(Long areaId) {
        if (areaId == null || areaId < Integer.MIN_VALUE || areaId > Integer.MAX_VALUE) {
            return null;
        }
        return AreaUtils.format(areaId.intValue());
    }
}
