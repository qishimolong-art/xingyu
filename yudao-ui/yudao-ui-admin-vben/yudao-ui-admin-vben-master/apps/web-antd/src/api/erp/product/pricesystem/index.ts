import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpPriceSystemApi {
  /** 价格体系信息 */
  export interface PriceSystem {
    id?: number; // 价格体系编号
    code?: string; // 价格体系编码
    name?: string; // 价格体系名称
    status?: number; // 状态（0=启用 1=停用）
    sort?: number; // 排序
    remark?: string; // 备注
    creator?: string; // 创建人
    createTime?: Date | string; // 创建时间
  }
}

/** 查询价格体系分页 */
export function getPriceSystemPage(
  params: PageParam & { name?: string; status?: number; code?: string },
) {
  return requestClient.get<PageResult<ErpPriceSystemApi.PriceSystem>>(
    '/erp/price-system/page',
    { params },
  );
}

/** 查询价格体系详情 */
export function getPriceSystem(id: number) {
  return requestClient.get<ErpPriceSystemApi.PriceSystem>(
    `/erp/price-system/get?id=${id}`,
  );
}

/** 新增价格体系 */
export function createPriceSystem(data: ErpPriceSystemApi.PriceSystem) {
  return requestClient.post('/erp/price-system/create', data);
}

/** 修改价格体系 */
export function updatePriceSystem(data: ErpPriceSystemApi.PriceSystem) {
  return requestClient.put('/erp/price-system/update', data);
}

/** 删除价格体系 */
export function deletePriceSystem(ids: number[]) {
  return requestClient.delete('/erp/price-system/delete', {
    params: { ids: ids.join(',') },
  });
}

/** 获得启用的价格体系列表（下拉用） */
export function getPriceSystemSimpleList() {
  return requestClient.get<ErpPriceSystemApi.PriceSystem[]>(
    '/erp/price-system/simple-list',
  );
}
