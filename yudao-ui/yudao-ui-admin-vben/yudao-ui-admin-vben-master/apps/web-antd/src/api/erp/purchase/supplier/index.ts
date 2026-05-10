import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpSupplierApi {
  /** 供应商信息 */
  export interface Supplier {
    id?: number; // 供应商编号
    name: string; // 供应商名称
    contact: string; // 联系人
    mobile: string; // 手机号码
    telephone: string; // 联系电话
    email: string; // 电子邮箱
    fax: string; // 传真
    remark: string; // 备注
    status: number; // 开启状态
    sort: number; // 排序
    taxNo: string; // 纳税人识别号
    taxPercent: number; // 税率
    bankName: string; // 开户行
    bankAccount: string; // 开户账号
    bankAddress: string; // 开户地址
    // ========== 汽配扩展字段 ==========
    code?: string; // 编码(自动生成)
    oldCode?: string; // 旧编码
    shortName?: string; // 简称
    foreignName?: string; // 外文名
    region?: string; // 区域
    category?: string; // 往来类别
    account?: string; // 账户
    settleMethod?: string; // 结算方式
    settleLocked?: boolean; // 结算锁定
    supplierType?: string; // 供应商类型
    arrivalCycle?: number; // 到货周期(天)
    purchaseLeadDays?: number; // 采购提前期(天)
    transportMethod?: string; // 运输方式
    freightType?: string; // 运费类型
    wubiCode?: string; // 五笔码
    pinyinCode?: string; // 拼音码
    purchaser?: string; // 采购员
    companyNature?: string; // 公司性质
    obsolete?: boolean; // 淘汰
    invoiceType?: string; // 开票类型
    groupSupplier?: boolean; // 集团供应商
    allowBranchOrder?: boolean; // 是否允许分店开单
    logisticsCompany?: string; // 物流公司
    arrivalPoint?: string; // 到货点
    postalCode?: string; // 邮政编码
    memberCode?: string; // 会员编码(自动生成)
    address?: string; // 地址
    province?: string; // 省
    city?: string; // 市
    district?: string; // 区/县
    website?: string; // 网址
    legalPerson?: string; // 法定代表
    creditCode?: string; // 统一信用代码
    purchaseControl?: string; // 采购管控
    floatUpdateLastPrice?: string; // 浮动是否更新供应商最后进价
    taxpayerId?: string; // 纳税人识别号(开票)
    invoiceBank?: string; // 开票银行
    invoiceBankAccount?: string; // 开票银行账号
    invoiceAddress?: string; // 开票地址
    invoicePhone?: string; // 开票电话
    invoiceCompany?: string; // 开票单位
    financePhone?: string; // 财务联系电话
    performanceProfitRef?: string; // 绩效考核利润参考依据
  }
}

/** 查询供应商分页 */
export function getSupplierPage(params: PageParam) {
  return requestClient.get<PageResult<ErpSupplierApi.Supplier>>(
    '/erp/supplier/page',
    { params },
  );
}

/** 获得供应商精简列表 */
export function getSupplierSimpleList() {
  return requestClient.get<ErpSupplierApi.Supplier[]>(
    '/erp/supplier/simple-list',
  );
}

/** 查询供应商详情 */
export function getSupplier(id: number) {
  return requestClient.get<ErpSupplierApi.Supplier>(
    `/erp/supplier/get?id=${id}`,
  );
}

/** 新增供应商 */
export function createSupplier(data: ErpSupplierApi.Supplier) {
  return requestClient.post('/erp/supplier/create', data);
}

/** 修改供应商 */
export function updateSupplier(data: ErpSupplierApi.Supplier) {
  return requestClient.put('/erp/supplier/update', data);
}

/** 删除供应商 */
export function deleteSupplier(id: number) {
  return requestClient.delete(`/erp/supplier/delete?id=${id}`);
}

/** 导出供应商 Excel */
export function exportSupplier(params: any) {
  return requestClient.download('/erp/supplier/export-excel', { params });
}
