import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace ErpCustomerApi {
  export interface Customer {
    id?: number;
    code?: string;
    name: string;
    shortName?: string;
    contact?: string;
    mobile?: string;
    telephone?: string;
    email?: string;
    fax?: string;
    remark?: string;
    status?: number;
    sort?: number;
    taxNo?: string;
    taxPercent?: number;
    bankName?: string;
    bankAccount?: string;
    bankAddress?: string;
    createTime?: string;
  }

  export interface CustomerContact {
    id?: number;
    customerId: number;
    name: string;
    mobile?: string;
    telephone?: string;
    email?: string;
    position?: string;
    address?: string;
    primaryContact?: boolean;
    receiverContact?: boolean;
    settleContact?: boolean;
    messageContact?: boolean;
    businessCardFrontUrl?: string;
    businessCardBackUrl?: string;
    status?: number;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerContract {
    id?: number;
    customerId: number;
    contractNo?: string;
    contractDate?: string;
    contractType?: string;
    startTime?: string;
    endTime?: string;
    settleMethod?: string;
    transportMethod?: string;
    baseAmount?: number;
    taskAmount?: number;
    attachmentUrl?: string;
    status?: number;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerImage {
    id?: number;
    customerId: number;
    imageType?: string;
    imageName?: string;
    imageUrl: string;
    defaulted?: boolean;
    sort?: number;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerTask {
    id?: number;
    customerId: number;
    year: number;
    month?: number;
    taskLevel?: string;
    taskAmount?: number;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerArea {
    id?: number;
    customerId: number;
    longitude?: number;
    latitude?: number;
    mapAddress?: string;
    detailAddress?: string;
    defaulted?: boolean;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerExtend {
    id?: number;
    customerId: number;
    extendKey: string;
    extendName?: string;
    extendValue?: string;
    extendType?: string;
    sort?: number;
    remark?: string;
    createTime?: string;
  }

  export interface CustomerBusinessInfo {
    id?: number;
    customerId: number;
    creditCode?: string;
    legalPerson?: string;
    registeredCapital?: string;
    establishDate?: string;
    businessStatus?: string;
    businessScope?: string;
    rawData?: string;
    remark?: string;
    createTime?: string;
  }
}

export function getCustomerPage(params: PageParam) {
  return requestClient.get<PageResult<ErpCustomerApi.Customer>>(
    '/erp/customer/page',
    { params },
  );
}

export function getCustomerSimpleList() {
  return requestClient.get<ErpCustomerApi.Customer[]>(
    '/erp/customer/simple-list',
  );
}

export function getCustomer(id: number) {
  return requestClient.get<ErpCustomerApi.Customer>(
    `/erp/customer/get?id=${id}`,
  );
}

export function createCustomer(data: ErpCustomerApi.Customer) {
  return requestClient.post<number>('/erp/customer/create', data);
}

export function updateCustomer(data: ErpCustomerApi.Customer) {
  return requestClient.put('/erp/customer/update', data);
}

export function deleteCustomer(id: number) {
  return requestClient.delete(`/erp/customer/delete?id=${id}`);
}

export function exportCustomer(params: any) {
  return requestClient.download('/erp/customer/export-excel', { params });
}

export function getCustomerContactList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerContact[]>(
    `/erp/customer-contact/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerContact(data: ErpCustomerApi.CustomerContact) {
  return requestClient.post<number>('/erp/customer-contact/create', data);
}

export function updateCustomerContact(data: ErpCustomerApi.CustomerContact) {
  return requestClient.put('/erp/customer-contact/update', data);
}

export function deleteCustomerContact(id: number) {
  return requestClient.delete(`/erp/customer-contact/delete?id=${id}`);
}

export function getCustomerContractList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerContract[]>(
    `/erp/customer-contract/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerContract(data: ErpCustomerApi.CustomerContract) {
  return requestClient.post<number>('/erp/customer-contract/create', data);
}

export function updateCustomerContract(data: ErpCustomerApi.CustomerContract) {
  return requestClient.put('/erp/customer-contract/update', data);
}

export function deleteCustomerContract(id: number) {
  return requestClient.delete(`/erp/customer-contract/delete?id=${id}`);
}

export function getCustomerImageList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerImage[]>(
    `/erp/customer-image/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerImage(data: ErpCustomerApi.CustomerImage) {
  return requestClient.post<number>('/erp/customer-image/create', data);
}

export function updateCustomerImage(data: ErpCustomerApi.CustomerImage) {
  return requestClient.put('/erp/customer-image/update', data);
}

export function deleteCustomerImage(id: number) {
  return requestClient.delete(`/erp/customer-image/delete?id=${id}`);
}

export function getCustomerTaskList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerTask[]>(
    `/erp/customer-task/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerTask(data: ErpCustomerApi.CustomerTask) {
  return requestClient.post<number>('/erp/customer-task/create', data);
}

export function updateCustomerTask(data: ErpCustomerApi.CustomerTask) {
  return requestClient.put('/erp/customer-task/update', data);
}

export function deleteCustomerTask(id: number) {
  return requestClient.delete(`/erp/customer-task/delete?id=${id}`);
}

export function getCustomerAreaList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerArea[]>(
    `/erp/customer-area/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerArea(data: ErpCustomerApi.CustomerArea) {
  return requestClient.post<number>('/erp/customer-area/create', data);
}

export function updateCustomerArea(data: ErpCustomerApi.CustomerArea) {
  return requestClient.put('/erp/customer-area/update', data);
}

export function deleteCustomerArea(id: number) {
  return requestClient.delete(`/erp/customer-area/delete?id=${id}`);
}

export function getCustomerExtendList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerExtend[]>(
    `/erp/customer-extend/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerExtend(data: ErpCustomerApi.CustomerExtend) {
  return requestClient.post<number>('/erp/customer-extend/create', data);
}

export function updateCustomerExtend(data: ErpCustomerApi.CustomerExtend) {
  return requestClient.put('/erp/customer-extend/update', data);
}

export function deleteCustomerExtend(id: number) {
  return requestClient.delete(`/erp/customer-extend/delete?id=${id}`);
}

export function getCustomerBusinessInfoList(customerId: number) {
  return requestClient.get<ErpCustomerApi.CustomerBusinessInfo[]>(
    `/erp/customer-business-info/list-by-customer?customerId=${customerId}`,
  );
}

export function createCustomerBusinessInfo(
  data: ErpCustomerApi.CustomerBusinessInfo,
) {
  return requestClient.post<number>('/erp/customer-business-info/create', data);
}

export function updateCustomerBusinessInfo(
  data: ErpCustomerApi.CustomerBusinessInfo,
) {
  return requestClient.put('/erp/customer-business-info/update', data);
}

export function deleteCustomerBusinessInfo(id: number) {
  return requestClient.delete(`/erp/customer-business-info/delete?id=${id}`);
}
