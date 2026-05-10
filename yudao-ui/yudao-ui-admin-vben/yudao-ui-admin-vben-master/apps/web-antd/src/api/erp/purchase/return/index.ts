import type { PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpPurchaseReturnApi {
  /** 采购退货信息 */
  export interface PurchaseReturn {
    id?: number; // 采购退货编号
    no?: string; // 采购退货号
    supplierId?: number; // 供应商编号
    returnTime?: Date; // 退货时间
    totalCount?: number; // 合计数量
    totalPrice: number; // 合计金额，单位：元
    discountPercent?: number; // 折扣百分比
    discountPrice?: number; // 折扣金额
    status?: number; // 状态
    remark?: string; // 备注
    totalTaxPrice?: number; // 合计税额
    otherPrice?: number; // 其他费用
    items?: PurchaseReturnItem[];
    // ========== 汽配扩展字段 ==========
    returnType?: string; // 退货类型(入库单退货等)
    purchaser?: string; // 采购员
    invoiceType?: string; // 开票类型
    transportMethod?: string; // 运输方式
    settleMethod?: string; // 结算方式
    packageCount?: number; // 件数
    freightAmount?: number; // 运费金额
    logisticsCompany?: string; // 物流公司
    docSource?: string; // 单据来源(正常退货等)
    factoryOrderNo?: string; // 厂家单号
    maker?: string; // 制单人
    dept?: string; // 部门
    shippingArea?: string; // 发货区
    warehouseType?: string; // 仓库类型
    freightType?: string; // 运费类型
    logisticsNo?: string; // 物流单号
    priority?: string; // 优先级
    orderMethod?: string; // 开单方式(正常单等)
    // ========== 三期：双模式字段 ==========
    returnMode?: number; // 退货模式（10=按单退货 BY_ORDER / 20=按库存退货 BY_STOCK）
  }

  /** 采购退货项 */
  export interface PurchaseReturnItem {
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
    // ========== 汽配扩展字段 ==========
    partCode?: string; // 零件编码
    partName?: string; // 零件名称
    vehicleModel?: string; // 适用车型
    originPlace?: string; // 产地
    packageQty?: number; // 包装数
    wholeQty?: number; // 整件数
    warehousePosition?: string; // 仓位 / 货架位
    drawingNo?: string; // 图号
    batchNo?: string; // 批次
    barCode?: string; // 条形码
    brand?: string; // 品牌
    businessEntity?: string; // 所属经营
    // ========== 三期：按单退货关联字段 ==========
    sourceInId?: number; // 原采购入库单 ID（按单退货时必填）
    sourceInItemId?: number; // 原采购入库项 ID（按单退货时必填）
    sourceInNo?: string; // 原采购入库单号（冗余）
    // 以下为前端临时展示字段，不传后端
    inCount?: number; // 原入库数量（按单退货时展示）
    returnableCount?: number; // 可退数量 = 原入库数量 - 已退数量
  }
}

/** 查询采购退货分页 */
export function getPurchaseReturnPage(params: any) {
  return requestClient.get<PageResult<ErpPurchaseReturnApi.PurchaseReturn>>(
    '/erp/purchase-return/page',
    {
      params,
    },
  );
}

/** 查询采购退货详情 */
export function getPurchaseReturn(id: number) {
  return requestClient.get<ErpPurchaseReturnApi.PurchaseReturn>(
    `/erp/purchase-return/get?id=${id}`,
  );
}

/** 新增采购退货 */
export function createPurchaseReturn(
  data: ErpPurchaseReturnApi.PurchaseReturn,
) {
  return requestClient.post('/erp/purchase-return/create', data);
}

/** 修改采购退货 */
export function updatePurchaseReturn(
  data: ErpPurchaseReturnApi.PurchaseReturn,
) {
  return requestClient.put('/erp/purchase-return/update', data);
}

/** 更新采购退货的状态 */
export function updatePurchaseReturnStatus(id: number, status: number) {
  return requestClient.put('/erp/purchase-return/update-status', null, {
    params: { id, status },
  });
}

/** 删除采购退货 */
export function deletePurchaseReturn(ids: number[]) {
  return requestClient.delete('/erp/purchase-return/delete', {
    params: {
      ids: ids.join(','),
    },
  });
}

/** 导出采购退货 Excel */
export function exportPurchaseReturn(params: any) {
  return requestClient.download('/erp/purchase-return/export-excel', {
    params,
  });
}
