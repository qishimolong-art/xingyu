import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpProductApi {
  /** 通用件子项 */
  export interface ProductUniversal {
    id?: number;
    universalCode: string;
    universalName?: string;
    universalVehicle?: string;
  }

  /** 产品信息 */
  export interface Product {
    id?: number; // 产品编号
    code?: string; // 配件编码（后端自动生成，只读）
    name: string; // 产品名称 / 零件名称
    barCode: string; // 产品条码
    categoryId: number; // 产品分类编号
    categoryName?: string; // 产品分类名称
    unitId: number; // 单位编号
    unitName?: string; // 单位名字
    status: number; // 产品状态
    standard?: string; // 产品规格
    remark?: string; // 产品备注
    expiryDay?: number; // 保质期天数
    weight?: number; // 重量（kg）
    purchasePrice?: number; // 采购价格，单位：元（旧字段，保留）
    salePrice?: number; // 销售价格，单位：元（旧字段，保留）
    minPrice?: number; // 最低价格，单位：元（旧字段，保留）

    // ========== 汽配扩展字段（旧，保留） ==========
    brand?: string;
    oeNumber?: string;
    originPlace?: string;
    vehicleModelText?: string;
    featureCode?: string;
    drawingNo?: string;
    shelf?: string;

    // ========== 配件信息管理扩展字段 ==========
    defaultWarehouseId?: number;
    defaultWarehouseName?: string; // 仅回显
    vehicleModel?: string; // 适用车型
    factoryCode?: string; // 厂家编码

    referencePrice?: number; // 参考价
    retailPrice?: number; // 零售价
    lastPurchasePrice?: number; // 最后一次采购入库价（只读）
    grossProfitRate?: number; // 毛利率（%）
    backupPrice1?: number; // 备用价1
    wholesalePrice?: number; // 批发价

    stockMax?: number; // 库存上限
    stockMin?: number; // 库存下限
    stockStandard?: number; // 标准库存
    packageQty?: number; // 包装数

    mainImage?: string; // 主图 URL
    detailContent?: string; // 配件详情 Markdown

    mergedFlag?: boolean; // 是否已合并
    mergedTargetId?: number; // 合并目标配件编号

    // 运行时计算字段（仅回显）
    currentStock?: number;
    inTransitStock?: number;
    availableStock?: number;
    lowStockWarning?: boolean;

    universals?: ProductUniversal[];

    createTime?: Date;
  }
}

/** 查询产品分页 */
export function getProductPage(params: PageParam) {
  return requestClient.get<PageResult<ErpProductApi.Product>>(
    '/erp/product/page',
    { params },
  );
}

/** 查询产品精简列表 */
export function getProductSimpleList() {
  return requestClient.get<ErpProductApi.Product[]>('/erp/product/simple-list');
}

/** 查询产品详情（基础信息） */
export function getProduct(id: number) {
  return requestClient.get<ErpProductApi.Product>(`/erp/product/get?id=${id}`);
}

/** 查询产品完整详情（含库存、通用件、仓库名等） */
export function getProductDetail(id: number) {
  return requestClient.get<ErpProductApi.Product>(
    `/erp/product/get-detail?id=${id}`,
  );
}

/** 新增产品 */
export function createProduct(data: ErpProductApi.Product) {
  return requestClient.post('/erp/product/create', data);
}

/** 修改产品 */
export function updateProduct(data: ErpProductApi.Product) {
  return requestClient.put('/erp/product/update', data);
}

/** 删除产品 */
export function deleteProduct(id: number) {
  return requestClient.delete(`/erp/product/delete?id=${id}`);
}

/** 导出产品 Excel */
export function exportProduct(params: any) {
  return requestClient.download('/erp/product/export-excel', { params });
}

/** 批量修改产品货架位 */
export function updateProductsShelf(ids: number[], shelf: string) {
  return requestClient.put<boolean>('/erp/product/update-shelf', null, {
    params: { ids: ids.join(','), shelf },
  });
}
