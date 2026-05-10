import { requestClient } from '#/api/request';

export namespace ErpFieldConfigApi {
  /** 字段配置 */
  export interface FieldConfig {
    id?: number;
    moduleKey: string;
    fieldName: string;
    fieldLabel?: string;
    required: boolean;
    sort?: number;
    creator?: string;
    createTime?: Date | string;
  }

  /** 批量保存请求 */
  export interface BatchUpdateReq {
    moduleKey: string;
    items: Array<{
      fieldName: string;
      fieldLabel?: string;
      required: boolean;
      sort?: number;
    }>;
  }

  /** 模块定义 */
  export interface Module {
    key: string;
    name: string;
  }
}

/** 查询某模块的字段配置列表 */
export function getFieldConfigList(moduleKey: string) {
  return requestClient.get<ErpFieldConfigApi.FieldConfig[]>(
    '/erp/field-config/list',
    { params: { moduleKey } },
  );
}

/** 批量保存某模块的全部字段配置（一次性覆盖） */
export function batchUpdateFieldConfig(data: ErpFieldConfigApi.BatchUpdateReq) {
  return requestClient.put('/erp/field-config/batch-update', data);
}

/** 清空某模块的自定义字段配置 */
export function resetFieldConfig(moduleKey: string) {
  return requestClient.put('/erp/field-config/reset', null, {
    params: { moduleKey },
  });
}

/** 获取所有可配置模块列表 */
export function getFieldConfigModules() {
  return requestClient.get<ErpFieldConfigApi.Module[]>(
    '/erp/field-config/modules',
  );
}
