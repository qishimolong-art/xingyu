import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpStockRecordApi {
  /** 产品库存明细 */
  export interface StockRecord {
    id?: number; // 编号
    productId: number; // 产品编号
    warehouseId: number; // 仓库编号
    count: number; // 出入库数量
    totalCount: number; // 总库存量
    bizType: number; // 业务类型
    bizId: number; // 业务编号
    bizItemId: number; // 业务项编号
    bizNo: string; // 业务单号
    unitPrice?: number; // 本次业务单价
    totalPrice?: number; // 本次业务金额
    costPrice?: number; // 结存成本均价
    costAmount?: number; // 结存成本金额
    bizDate?: string; // 业务发生日期（ISO 字符串）
  }

  /** 库存进出流水明细账（入出分列，五期） */
  export interface StockRecordReport {
    id?: number;
    bizDate?: string;
    bizType?: number;
    bizNo?: string;
    productId?: number;
    productCode?: string;
    productName?: string;
    warehouseId?: number;
    warehouseName?: string;
    // 入库分列
    inCount?: number;
    inUnitPrice?: number;
    inAmount?: number;
    // 出库分列
    outCount?: number;
    outUnitPrice?: number;
    outAmount?: number;
    // 结存列
    totalCount?: number;
    costPrice?: number;
    costAmount?: number;
  }

  /** 库存进出流水 底部汇总（五期） */
  export interface StockRecordSummary {
    totalInCount?: number;
    totalInAmount?: number;
    totalOutCount?: number;
    totalOutAmount?: number;
    recordCount?: number;
  }
}

/** 查询产品库存明细分页 */
export function getStockRecordPage(params: PageParam) {
  return requestClient.get<PageResult<ErpStockRecordApi.StockRecord>>(
    '/erp/stock-record/page',
    { params },
  );
}

/** 导出产品库存明细 Excel */
export function exportStockRecord(params: any) {
  return requestClient.download('/erp/stock-record/export-excel', { params });
}

/** 查询库存进出流水明细账分页（五期，入出分列） */
export function getStockRecordReportPage(params: any) {
  return requestClient.get<PageResult<ErpStockRecordApi.StockRecordReport>>(
    '/erp/stock-record/report-page',
    { params },
  );
}

/** 查询库存进出流水汇总（五期，底部汇总栏） */
export function getStockRecordSummary(params: any) {
  return requestClient.get<ErpStockRecordApi.StockRecordSummary>(
    '/erp/stock-record/summary',
    { params },
  );
}

/** 导出库存进出流水明细账 Excel（五期） */
export function exportStockRecordReport(params: any) {
  return requestClient.download('/erp/stock-record/report-export-excel', { params });
}
