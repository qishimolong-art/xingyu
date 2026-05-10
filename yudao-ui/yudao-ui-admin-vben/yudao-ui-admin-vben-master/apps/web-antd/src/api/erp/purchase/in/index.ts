import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpPurchaseInApi {
  /** 采购入库信息 */
  export interface PurchaseIn {
    id?: number; // 入库工单编号
    no?: string; // 采购入库号
    supplierId?: number; // 供应商编号
    inTime?: Date; // 入库时间
    totalCount?: number; // 合计数量
    totalPrice?: number; // 合计金额，单位：元
    status?: number; // 状态
    remark?: string; // 备注
    outCount?: number; // 采购出库数量
    returnCount?: number; // 采购退货数量
    discountPercent?: number; // 折扣百分比
    discountPrice?: number; // 折扣金额
    paymentPrice?: number; // 实际支付金额
    otherPrice?: number; // 其他费用
    totalProductPrice?: number; // 合计商品金额
    taxPrice?: number; // 合计税额
    // ========== 系统信息 ==========
    purchaser?: string; // 采购员
    invoiceType?: string; // 开票类型（收据/不开票/专票/普票）
    transportMethod?: string; // 运输方式（快递/物流/自提）
    settleMethod?: string; // 结算方式（挂账/现结/月结）
    purchaseArea?: string; // 进货区
    accountant?: string; // 记账员
    floatRate?: number; // 浮动率（默认 1.0）
    packageCount?: number; // 件数（默认 0）
    factoryOrderNo?: string; // 厂家单号
    orderMethod?: string; // 开单方式（正常单/样品单/赠品单）
    // ========== 运费信息 ==========
    freightType1?: string; // 运费类型 1
    freightType2?: string; // 运费类型 2
    freightObject1?: string; // 运费对象 1
    freightObject2?: string; // 运费对象 2
    logisticsCompany?: string; // 物流公司
    // ========== 供应商信息 ==========
    handler?: string; // 经办人
    taxRate?: number; // 税率
    deptId?: number; // 部门 ID
    purchaseDiscount?: number; // 采购折让金额
    priority?: string; // 优先级（正常件/紧急件）
    unloader?: string; // 卸货员
    floatRecord?: string; // 浮动记录
    receiveUnit?: string; // 收货单位
    totalFreight1?: number; // 总运费 1
    totalFreight2?: number; // 总运费 2
    paymentDate?: Date | string; // 付款日期
    hasInvoice?: boolean; // 是否发票
    // ========== 其他信息 ==========
    businessEntity?: string; // 所属经营
    items?: PurchaseInItem[]; // 采购入库明细
  }

  /** 采购项信息 */
  export interface PurchaseInItem {
    count?: number;
    id?: number;
    seq?: number;
    orderItemId?: number;
    productBarCode?: string;
    productId?: number;
    productCode?: string;
    productName: string;
    productPrice: number;
    productUnitId?: number;
    productUnitName?: string;
    totalProductPrice?: number;
    remark: string;
    stockCount?: number;
    taxPercent?: number;
    taxPrice?: number;
    totalPrice?: number;
    warehouseId?: number;
    inCount?: number;
    // ========== 汽配扩展字段 ==========
    packageQty?: number; // 包装数（从商品资料带出）
    wholeQty?: number; // 整件数（用户填写，触发 count = wholeQty × packageQty）
    warehousePosition?: string; // 仓位 / 货架位
    drawingNo?: string; // 图号
    batchNo?: string; // 批次
    barCode?: string; // 条形码
    brand?: string; // 品牌
    vehicleModel?: string; // 适用车型
    originPlace?: string; // 产地
    businessEntity?: string; // 所属经营
  }

  /** 采购入库项的可退信息（按单退货使用） */
  export interface ReturnableItem {
    sourceInId?: number; // 原采购入库单 ID
    sourceInItemId?: number; // 原采购入库项 ID
    sourceInNo?: string; // 原采购入库单号
    productId?: number;
    productUnitId?: number;
    warehouseId?: number;
    productPrice?: number; // 原入库单价
    inCount?: number; // 原入库数量
    returnedCount?: number; // 已累计退货数量
    returnableCount?: number; // 可退数量 = inCount - returnedCount
    taxPercent?: number;
    packageQty?: number;
    wholeQty?: number;
    warehousePosition?: string;
    drawingNo?: string;
    batchNo?: string;
    barCode?: string;
    brand?: string;
    vehicleModel?: string;
    originPlace?: string;
    businessEntity?: string;
    remark?: string;
  }
}

/** 查询采购入库分页 */
export function getPurchaseInPage(params: PageParam) {
  return requestClient.get<PageResult<ErpPurchaseInApi.PurchaseIn>>(
    '/erp/purchase-in/page',
    {
      params,
    },
  );
}

/** 查询采购入库详情 */
export function getPurchaseIn(id: number) {
  return requestClient.get<ErpPurchaseInApi.PurchaseIn>(
    `/erp/purchase-in/get?id=${id}`,
  );
}

/** 新增采购入库 */
export function createPurchaseIn(data: ErpPurchaseInApi.PurchaseIn) {
  return requestClient.post('/erp/purchase-in/create', data);
}

/** 修改采购入库 */
export function updatePurchaseIn(data: ErpPurchaseInApi.PurchaseIn) {
  return requestClient.put('/erp/purchase-in/update', data);
}

/** 更新采购入库的状态 */
export function updatePurchaseInStatus(id: number, status: number) {
  return requestClient.put('/erp/purchase-in/update-status', null, {
    params: {
      id,
      status,
    },
  });
}

/** 删除采购入库 */
export function deletePurchaseIn(ids: number[]) {
  return requestClient.delete('/erp/purchase-in/delete', {
    params: {
      ids: ids.join(','),
    },
  });
}

/** 导出采购入库 Excel */
export function exportPurchaseIn(params: any) {
  return requestClient.download('/erp/purchase-in/export-excel', {
    params,
  });
}

/** 查询某采购入库单的可退明细（按单退货使用） */
export function getReturnableItemsByInId(inId: number) {
  return requestClient.get<ErpPurchaseInApi.ReturnableItem[]>(
    '/erp/purchase-in/returnable-items',
    { params: { inId } },
  );
}

/** 从采购订单分批入库（自动审批生效） */
export function createPurchaseInFromOrder(data: {
  orderId: number;
  inTime?: string;
  accountId?: number;
  items: Array<{ orderItemId: number; count: number; warehouseId: number }>;
}) {
  return requestClient.post<number>('/erp/purchase-in/create-from-order', data);
}
