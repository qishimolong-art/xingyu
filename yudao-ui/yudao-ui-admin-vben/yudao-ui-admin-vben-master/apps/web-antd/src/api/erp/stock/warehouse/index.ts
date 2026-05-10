import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpWarehouseApi {
  /** 仓库信息 */
  export interface Warehouse {
    id?: number; // 仓库编号
    name: string; // 仓库名称
    address?: string; // 仓库地址
    sort: number; // 排序
    remark?: string; // 备注
    principal?: string; // 负责人
    warehousePrice?: number; // 仓储费，单位：元
    truckagePrice?: number; // 搬运费，单位：元
    status: number; // 开启状态
    defaultStatus?: boolean; // 是否默认
    // ========== 扩展字段 ==========
    warehouseType?: number; // 仓库类型
    storageCenterId?: number; // 仓储中心ID
    storageWarehouseId?: number; // 仓储对应仓库ID
    saleEnabled?: boolean; // 销售启用
    purchaseEnabled?: boolean; // 采购启用
    stockBillEnabled?: boolean; // 入出仓单
    ecommerceEnabled?: boolean; // 允许电商销售
    scanControl?: boolean; // 扫码管控
    saleBillControl?: boolean; // 销售开单管控
    zeroStockHide?: boolean; // 销售0库存不显示
    goodsToBranch?: string; // 货到分店
    dept?: string; // 部门
    warehouseLocation?: string; // 仓库地点
    outPacking?: boolean; // 出仓打包装箱
    autoOrder?: boolean; // 自动订货
    maxPickCount?: number; // 允许同时拣货单数
    warehouseCode?: string; // 仓库编码
    splitOrder?: boolean; // 是否拆单
    stockGroupType?: number; // 出入仓分组
    creditControl?: number; // 额度管控
    regionId?: number; // 区域ID
    // ========== 关联数据 ==========
    branchTenantIds?: number[]; // 分店租户ID列表
    storageCenterName?: string; // 仓储中心名称
    storageWarehouseName?: string; // 仓储对应仓库名称
    regionName?: string; // 区域名称
  }
}

/** 查询仓库分页 */
export function getWarehousePage(params: PageParam) {
  return requestClient.get<PageResult<ErpWarehouseApi.Warehouse>>(
    '/erp/warehouse/page',
    { params },
  );
}

/** 查询仓库精简列表 */
export function getWarehouseSimpleList() {
  return requestClient.get<ErpWarehouseApi.Warehouse[]>(
    '/erp/warehouse/simple-list',
  );
}

/** 查询仓库详情 */
export function getWarehouse(id: number) {
  return requestClient.get<ErpWarehouseApi.Warehouse>(
    `/erp/warehouse/get?id=${id}`,
  );
}

/** 新增仓库 */
export function createWarehouse(data: ErpWarehouseApi.Warehouse) {
  return requestClient.post('/erp/warehouse/create', data);
}

/** 修改仓库 */
export function updateWarehouse(data: ErpWarehouseApi.Warehouse) {
  return requestClient.put('/erp/warehouse/update', data);
}

/** 修改仓库默认状态 */
export function updateWarehouseDefaultStatus(
  id: number,
  defaultStatus: boolean,
) {
  return requestClient.put('/erp/warehouse/update-default-status', null, {
    params: { id, defaultStatus },
  });
}

/** 删除仓库 */
export function deleteWarehouse(id: number) {
  return requestClient.delete(`/erp/warehouse/delete?id=${id}`);
}

/** 导出仓库 Excel */
export function exportWarehouse(params: any) {
  return requestClient.download('/erp/warehouse/export-excel', { params });
}
