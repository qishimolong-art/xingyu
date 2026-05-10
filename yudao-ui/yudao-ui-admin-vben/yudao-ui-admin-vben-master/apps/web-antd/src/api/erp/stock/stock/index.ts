import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpStockApi {
  /** 产品库存信息 */
  export interface Stock {
    id?: number; // 编号
    productId: number; // 产品编号
    warehouseId: number; // 仓库编号
    count: number; // 库存数量
    costPrice?: number; // 成本均价（移动加权平均）
    costAmount?: number; // 成本金额 = count × costPrice

    // 产品/仓库回显（后端聚合）
    productName?: string;
    unitName?: string;
    categoryName?: string;
    warehouseName?: string;

    // ========== 产品扩展字段 ==========
    productCode?: string;
    drawingNo?: string;
    standard?: string;
    featureCode?: string;
    vehicleModel?: string;
    brand?: string;
    originPlace?: string;
    shelf?: string;
    categoryId?: number;
    lastPurchasePrice?: number;
    oeNumber?: string;
    factoryCode?: string;
    productBarCode?: string;
    referencePrice?: number;
    retailPrice?: number;
    backupPrice1?: number;
    wholesalePrice?: number;
    stockMax?: number;
    stockMin?: number;
    stockStandard?: number;
    packageQty?: number;
    weight?: number;

    // ========== 聚合字段 ==========
    occupiedCount?: number;
    pendingInCount?: number;
    inTransitCount?: number;

    // ========== 价格体系动态列 ==========
    currentPrice?: number;
    currentPriceAmount?: number;
  }

  /** 产品库存查询参数 */
  export interface StockQueryReqVO {
    productId: number;
    warehouseId: number;
  }

  /** 库存调整请求 */
  export interface StockAdjustReqVO {
    productId: number;
    warehouseId: number;
    targetCount: number;
    reason?: string;
    remark?: string;
  }
}

/** 查询产品库存分页 */
export function getStockPage(params: PageParam) {
  return requestClient.get<PageResult<ErpStockApi.Stock>>('/erp/stock/page', {
    params,
  });
}

/** 获得产品库存数量 */
export function getStockCount(productId: number, warehouseId?: number) {
  const params: any = { productId };
  if (warehouseId !== undefined) {
    params.warehouseId = warehouseId;
  }
  return requestClient.get<number>('/erp/stock/get-count', {
    params,
  });
}

/** 导出产品库存 Excel */
export function exportStock(params: any) {
  return requestClient.download('/erp/stock/export-excel', {
    params,
  });
}

/** 获取库存数量 */
export function getWarehouseStockCount(params: ErpStockApi.StockQueryReqVO) {
  return requestClient.get<number>('/erp/stock/get-count', {
    params,
  });
}

/** 手动调整库存 */
export function adjustStock(data: ErpStockApi.StockAdjustReqVO) {
  return requestClient.put<number>('/erp/stock/adjust', data);
}
