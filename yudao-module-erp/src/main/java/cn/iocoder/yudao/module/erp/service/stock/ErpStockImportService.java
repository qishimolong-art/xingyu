package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveImportExcelVO;

import java.util.List;

public interface ErpStockImportService {

    ErpStockImportResultRespVO importStockInList(List<ErpStockImportExcelVO> list);

    ErpStockImportResultRespVO importStockOutList(List<ErpStockImportExcelVO> list);

    ErpStockImportResultRespVO importStockMoveList(List<ErpStockImportExcelVO> list);

    ErpStockImportResultRespVO importStockTransferOutList(List<ErpStockImportExcelVO> list);

    ErpStockImportResultRespVO importStockCheckList(List<ErpStockCheckImportExcelVO> list);

    ErpStockImportResultRespVO importWarehouseMoveList(List<ErpWarehouseMoveImportExcelVO> list);

}
