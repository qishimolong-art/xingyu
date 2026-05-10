import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpSaleQuoteApi {
  export interface SaleQuote {
    id?: number;
    no?: string;
    status?: number;
    customerId?: number;
    customerName?: string;
    accountId?: number;
    saleUserId?: number;
    deptId?: number;
    quoteTime?: Date | number | string;
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
    items?: SaleQuoteItem[];
  }

  export interface SaleQuoteItem {
    id?: number;
    quoteId?: number;
    productId?: number;
    productCode?: string;
    productName?: string;
    productBarCode?: string;
    productUnitId?: number;
    productUnitName?: string;
    warehouseId?: number;
    productPrice?: number;
    count?: number;
    convertedCount?: number;
    totalPrice?: number;
    taxPercent?: number;
    taxPrice?: number;
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

  export interface ConvertCartReq {
    quoteId: number;
    items: Array<{
      quoteItemId: number;
      count: number;
    }>;
  }
}

export function getSaleQuotePage(params: PageParam) {
  return requestClient.get<PageResult<ErpSaleQuoteApi.SaleQuote>>(
    '/erp/sale-quote/page',
    { params },
  );
}

export function getSaleQuote(id: number) {
  return requestClient.get<ErpSaleQuoteApi.SaleQuote>(
    `/erp/sale-quote/get?id=${id}`,
  );
}

export function createSaleQuote(data: ErpSaleQuoteApi.SaleQuote) {
  return requestClient.post('/erp/sale-quote/create', data);
}

export function updateSaleQuote(data: ErpSaleQuoteApi.SaleQuote) {
  return requestClient.put('/erp/sale-quote/update', data);
}

export function approveSaleQuote(id: number) {
  return requestClient.put('/erp/sale-quote/approve', null, {
    params: { id },
  });
}

export function convertSaleQuoteToCart(data: ErpSaleQuoteApi.ConvertCartReq) {
  return requestClient.post('/erp/sale-quote/convert-cart', data);
}

export function deleteSaleQuote(ids: number[]) {
  return requestClient.delete('/erp/sale-quote/delete', {
    params: { ids: ids.join(',') },
  });
}
