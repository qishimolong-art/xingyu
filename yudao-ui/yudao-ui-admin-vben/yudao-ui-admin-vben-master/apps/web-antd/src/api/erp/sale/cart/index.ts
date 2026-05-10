import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpSaleCartApi {
  export interface SaleCart {
    id?: number;
    no?: string;
    status?: number;
    customerId?: number;
    customerName?: string;
    accountId?: number;
    saleUserId?: number;
    deptId?: number;
    cartTime?: Date | number | string;
    firstAuditUserId?: number;
    firstAuditTime?: Date | number | string;
    finalAuditUserId?: number;
    finalAuditTime?: Date | number | string;
    totalCount?: number;
    totalPrice?: number;
    totalProductPrice?: number;
    totalTaxPrice?: number;
    discountPercent?: number;
    discountPrice?: number;
    otherPrice?: number;
    fileUrl?: string;
    remark?: string;
    productNames?: string;
    items?: SaleCartItem[];
  }

  export interface SaleCartItem {
    id?: number;
    cartId?: number;
    productId?: number;
    productCode?: string;
    productName?: string;
    productBarCode?: string;
    productUnitId?: number;
    productUnitName?: string;
    warehouseId?: number;
    productPrice?: number;
    count?: number;
    totalPrice?: number;
    taxPercent?: number;
    taxPrice?: number;
    stockCount?: number;
    warehousePosition?: string;
    drawingNo?: string;
    batchNo?: string;
    barCode?: string;
    brand?: string;
    vehicleModel?: string;
    originPlace?: string;
    standard?: string;
    remark?: string;
  }
}

export function getSaleCartPage(params: PageParam) {
  return requestClient.get<PageResult<ErpSaleCartApi.SaleCart>>(
    '/erp/sale-cart/page',
    { params },
  );
}

export function getSaleCart(id: number) {
  return requestClient.get<ErpSaleCartApi.SaleCart>(
    `/erp/sale-cart/get?id=${id}`,
  );
}

export function createSaleCart(data: ErpSaleCartApi.SaleCart) {
  return requestClient.post('/erp/sale-cart/create', data);
}

export function updateSaleCart(data: ErpSaleCartApi.SaleCart) {
  return requestClient.put('/erp/sale-cart/update', data);
}

export function submitSaleCart(id: number) {
  return requestClient.put('/erp/sale-cart/submit', null, { params: { id } });
}

export function firstApproveSaleCart(id: number) {
  return requestClient.put('/erp/sale-cart/first-approve', null, {
    params: { id },
  });
}

export function finalApproveSaleCart(id: number) {
  return requestClient.put('/erp/sale-cart/final-approve', null, {
    params: { id },
  });
}

export function convertSaleCartToQuote(id: number) {
  return requestClient.post('/erp/sale-cart/convert-quote', null, {
    params: { id },
  });
}
