import { requestClient } from '#/api/request';

export namespace ErpBaseDataApi {
  /** 基础数据信息 */
  export interface BaseData {
    id?: number;
    type: string;
    name: string;
    sort: number;
    status: number;
    remark?: string;
  }
}

/** 获取基础数据精简列表 */
export function getBaseDataSimpleList(type: string) {
  return requestClient.get<ErpBaseDataApi.BaseData[]>('/erp/base-data/simple-list', {
    params: { type },
  });
}

/** 查询基础数据分页 */
export function getBaseDataPage(params: any) {
  return requestClient.get('/erp/base-data/page', { params });
}

/** 创建基础数据 */
export function createBaseData(data: ErpBaseDataApi.BaseData) {
  return requestClient.post('/erp/base-data/create', data);
}

/** 更新基础数据 */
export function updateBaseData(data: ErpBaseDataApi.BaseData) {
  return requestClient.put('/erp/base-data/update', data);
}

/** 查询基础数据详情 */
export function getBaseData(id: number) {
  return requestClient.get<ErpBaseDataApi.BaseData>(
    `/erp/base-data/get?id=${id}`,
  );
}

/** 删除基础数据 */
export function deleteBaseData(id: number) {
  return requestClient.delete(`/erp/base-data/delete?id=${id}`);
}
