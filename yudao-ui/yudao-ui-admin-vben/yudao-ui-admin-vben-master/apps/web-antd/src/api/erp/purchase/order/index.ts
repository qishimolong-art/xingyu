import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpPurchaseOrderApi {
  /** 采购订单信息 */
  export interface PurchaseOrder {
    id?: number; // 订单工单编号
    no?: string; // 采购订单号
    supplierId?: number; // 供应商编号
    supplierName?: string; // 供应商名称
    orderTime?: Date | string; // 订单时间
    totalCount?: number; // 合计数量
    totalPrice?: number; // 合计金额，单位：元
    totalProductPrice?: number; // 产品金额，单位：元
    discountPercent?: number; // 优惠率，百分比
    discountPrice?: number; // 优惠金额，单位：元
    depositPrice?: number; // 定金金额，单位：元
    accountId?: number; // 结算账户编号
    status?: number; // 状态
    remark?: string; // 备注
    fileUrl?: string; // 附件地址
    inCount?: number; // 采购入库数量
    count?: number; // 数量
    returnCount?: number; // 采购退货数量
    inStatus?: number; // 入库状态
    returnStatus?: number; // 退货状态
    productNames?: string; // 产品名称列表
    creatorName?: string; // 创建人名称
    createTime?: Date; // 创建时间
    items?: PurchaseOrderItem[]; // 订单项列表
    // ========== 汽配扩展字段 ==========
    arrivalDate?: Date | string; // 到货日期
    deliveryMethod?: string; // 送货方式
    purchaseType?: string; // 采购类型
    orderFormula?: string; // 订货公式
    sendDate?: Date | string; // 发出日期
    latestArrivalDate?: Date | string; // 最近到货日期
    saleDateFrom?: Date | string; // 销售日期从
    saleDateTo?: Date | string; // 到销售日期
    factoryOrderNo?: string; // 厂家单号
    receiveAddress?: string; // 收货地址
    invoiceType?: string; // 开票类型
    settleMethod?: string; // 结算方式
    // ========== 新增字段 ==========
    purchaser?: number; // 采购员（用户ID）
    deptId?: number; // 部门ID
    orderDate?: Date | string; // 订货日期
    purchaseCycle?: number; // 采购周期(天)
    orderCompany?: string; // 订货公司
    taxPercent?: number; // 税率(%)
  }

  /** 采购订单项信息 */
  export interface PurchaseOrderItem {
    id?: number; // 订单项编号
    seq?: number; // 序号（前端表格行 key，不传后端）
    orderId?: number; // 采购订单编号
    productId?: number; // 产品编号
    productCode?: string; // 产品编码（前端展示用）
    productName?: string; // 产品名称
    productBarCode?: string; // 产品条码
    productUnitId?: number; // 产品单位编号
    productUnitName?: string; // 产品单位名称
    productPrice?: number; // 产品单价，单位：元
    totalProductPrice?: number; // 产品总价，单位：元
    count?: number; // 数量
    totalPrice?: number; // 总价，单位：元
    taxPercent?: number; // 税率，百分比
    taxPrice?: number; // 税额，单位：元
    totalTaxPrice?: number; // 含税总价，单位：元
    remark?: string; // 备注
    stockCount?: number; // 库存数量（显示字段）
    // ========== 汽配扩展字段 ==========
    warehouseId?: number; // 仓库编号
    warehousePosition?: string; // 货架位
    vehicleModel?: string; // 适用车型（产品带出）
    originPlace?: string; // 产地（产品带出）
    standard?: string; // 规格（产品带出）
    featureCode?: string; // 特征码（产品带出）
    drawingNo?: string; // 图号（产品带出）
    batchNo?: string; // 批次
    factoryCode?: string; // 厂家编码（产品带出）
    brand?: string; // 品牌（产品带出）
    supplierId?: number; // 默认供应商编号
    arrivalCount?: number; // 到货数量
    gift?: boolean; // 是否赠品
  }
}

/** 查询采购订单分页 */
export function getPurchaseOrderPage(params: PageParam) {
  return requestClient.get<PageResult<ErpPurchaseOrderApi.PurchaseOrder>>(
    '/erp/purchase-order/page',
    { params },
  );
}

/** 查询采购订单详情 */
export function getPurchaseOrder(id: number) {
  return requestClient.get<ErpPurchaseOrderApi.PurchaseOrder>(
    `/erp/purchase-order/get?id=${id}`,
  );
}

/** 新增采购订单 */
export function createPurchaseOrder(data: ErpPurchaseOrderApi.PurchaseOrder) {
  return requestClient.post('/erp/purchase-order/create', data);
}

/** 修改采购订单 */
export function updatePurchaseOrder(data: ErpPurchaseOrderApi.PurchaseOrder) {
  return requestClient.put('/erp/purchase-order/update', data);
}

/** 更新采购订单的状态 */
export function updatePurchaseOrderStatus(id: number, status: number) {
  return requestClient.put('/erp/purchase-order/update-status', null, {
    params: { id, status },
  });
}

/** 删除采购订单 */
export function deletePurchaseOrder(ids: number[]) {
  return requestClient.delete('/erp/purchase-order/delete', {
    params: { ids: ids.join(',') },
  });
}

/** 导出采购订单 Excel */
export function exportPurchaseOrder(params: any) {
  return requestClient.download('/erp/purchase-order/export-excel', { params });
}

/** 解析采购订单导入Excel（仅解析不创建） */
export function parsePurchaseOrderImportExcel(file: File) {
  return requestClient.upload('/erp/purchase-order/parse-import-excel', { file });
}

/** 下载采购订单导入模板 */
export function getPurchaseOrderImportTemplate() {
  return requestClient.download('/erp/purchase-order/get-import-template');
}

/** 可入库明细项 */
export interface InableItem {
  orderItemId?: number;
  productId?: number;
  productName?: string;
  productCode?: string;
  productUnitName?: string;
  productPrice?: number;
  orderCount?: number;
  inCount?: number;
  inableCount?: number;
  warehouseId?: number;
  gift?: boolean;
  vehicleModel?: string;
  originPlace?: string;
  brand?: string;
  standard?: string;
  drawingNo?: string;
  warehousePosition?: string;
}

/** 查询采购订单的可入库明细 */
export function getInableItemsByOrderId(orderId: number) {
  return requestClient.get<InableItem[]>(
    '/erp/purchase-order/inable-items',
    { params: { orderId } },
  );
}
