package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;

@FunctionalInterface
public interface ErpStockExportPageLoader {

    PageResult<ErpStockRespVO> load(ErpStockPageReqVO reqVO);

}
