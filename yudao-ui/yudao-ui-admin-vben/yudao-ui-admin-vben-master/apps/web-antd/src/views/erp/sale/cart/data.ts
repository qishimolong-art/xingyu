import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { erpPriceInputFormatter } from '@vben/utils';

import { z } from '#/adapter/form';
import { getAccountSimpleList } from '#/api/erp/finance/account';
import { getProductSimpleList } from '#/api/erp/product/product';
import { getCustomerSimpleList } from '#/api/erp/sale/customer';
import { getSimpleUserList } from '#/api/system/user';
import { getRangePickerDefaultProps } from '#/utils';

const cartStatusOptions = [
  { label: '草稿', value: 10 },
  { label: '待初审', value: 20 },
  { label: '初审通过', value: 30 },
  { label: '终审通过', value: 40 },
  { label: '已生成销售单', value: 50 },
  { label: '已转报价', value: 60 },
  { label: '已取消', value: 90 },
];

export function useFormSchema(formType: string): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'no', label: '手推车单号', component: 'Input', componentProps: { placeholder: '系统自动生成', disabled: true } },
    {
      fieldName: 'cartTime',
      label: '开单时间',
      component: 'DatePicker',
      componentProps: { placeholder: '选择开单时间', showTime: true, format: 'YYYY-MM-DD HH:mm:ss', valueFormat: 'x', disabled: formType === 'detail' },
      rules: 'required',
    },
    {
      fieldName: 'customerId',
      label: '客户',
      component: 'ApiSelect',
      componentProps: { placeholder: '请选择客户', allowClear: true, showSearch: true, api: getCustomerSimpleList, labelField: 'name', valueField: 'id', disabled: formType === 'detail' },
      rules: 'required',
    },
    {
      fieldName: 'saleUserId',
      label: '销售人员',
      component: 'ApiSelect',
      componentProps: { placeholder: '请选择销售人员', allowClear: true, showSearch: true, api: getSimpleUserList, labelField: 'nickname', valueField: 'id', disabled: formType === 'detail' },
    },
    {
      fieldName: 'accountId',
      label: '结算账户',
      component: 'ApiSelect',
      componentProps: { placeholder: '请选择结算账户', allowClear: true, showSearch: true, api: getAccountSimpleList, labelField: 'name', valueField: 'id', disabled: formType === 'detail' },
    },
    { fieldName: 'remark', label: '备注', component: 'Textarea', componentProps: { placeholder: '请输入备注', autoSize: { minRows: 1, maxRows: 1 }, disabled: formType === 'detail' }, formItemClass: 'col-span-3' },
    { fieldName: 'items', label: '手推车产品清单', component: 'Input', formItemClass: 'col-span-3' },
    {
      fieldName: 'discountPercent',
      label: '优惠率(%)',
      component: 'InputNumber',
      componentProps: { placeholder: '请输入优惠率', min: 0, max: 100, precision: 2, disabled: formType === 'detail' },
      rules: z.number().min(0).optional(),
    },
    { fieldName: 'discountPrice', label: '优惠金额', component: 'InputNumber', componentProps: { precision: 2, formatter: erpPriceInputFormatter, disabled: true } },
    { fieldName: 'totalPrice', label: '优惠后金额', component: 'InputNumber', componentProps: { precision: 2, formatter: erpPriceInputFormatter, disabled: true } },
  ];
}

export function useFormItemColumns(disabled: boolean): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 50, fixed: 'left' },
    { field: 'productId', title: '产品名称', minWidth: 220, slots: { default: 'productId' } },
    { field: 'stockCount', title: '库存', minWidth: 90, formatter: 'formatAmount3' },
    { field: 'productBarCode', title: '条码', minWidth: 130 },
    { field: 'productUnitName', title: '单位', minWidth: 80 },
    { field: 'brand', title: '品牌', minWidth: 120, slots: { default: 'brand' } },
    { field: 'vehicleModel', title: '车型', minWidth: 130, slots: { default: 'vehicleModel' } },
    { field: 'originPlace', title: '产地', minWidth: 120, slots: { default: 'originPlace' } },
    { field: 'standard', title: '规格', minWidth: 120, slots: { default: 'standard' } },
    { field: 'count', title: '数量', minWidth: 120, fixed: 'right', slots: { default: 'count' } },
    { field: 'productPrice', title: '产品单价', minWidth: 120, fixed: 'right', slots: { default: 'productPrice' } },
    { field: 'totalPrice', title: '金额', minWidth: 120, fixed: 'right', formatter: 'formatAmount2' },
    { field: 'taxPercent', title: '税率(%)', minWidth: 105, fixed: 'right', slots: { default: 'taxPercent' } },
    { field: 'taxPrice', title: '税额', minWidth: 120, fixed: 'right', formatter: 'formatAmount2' },
    { field: 'remark', title: '备注', minWidth: 150, slots: { default: 'remark' } },
    { title: '操作', width: 70, fixed: 'right', slots: { default: 'actions' }, visible: !disabled },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'no', label: '手推车单号', component: 'Input', componentProps: { placeholder: '请输入手推车单号', allowClear: true } },
    { fieldName: 'customerId', label: '客户', component: 'ApiSelect', componentProps: { placeholder: '请选择客户', allowClear: true, showSearch: true, api: getCustomerSimpleList, labelField: 'name', valueField: 'id' } },
    { fieldName: 'productId', label: '产品', component: 'ApiSelect', componentProps: { placeholder: '请选择产品', allowClear: true, showSearch: true, api: getProductSimpleList, labelField: 'name', valueField: 'id' } },
    { fieldName: 'cartTime', label: '开单时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
    { fieldName: 'status', label: '状态', component: 'Select', componentProps: { options: cartStatusOptions, placeholder: '请选择状态', allowClear: true } },
    { fieldName: 'remark', label: '备注', component: 'Input', componentProps: { placeholder: '请输入备注', allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { field: 'no', title: '手推车单号', width: 190, fixed: 'left' },
    { field: 'productNames', title: '产品信息', minWidth: 180, showOverflow: 'tooltip' },
    { field: 'customerName', title: '客户', minWidth: 140 },
    { field: 'cartTime', title: '开单时间', width: 160, formatter: 'formatDate' },
    { field: 'totalCount', title: '总数量', minWidth: 110, formatter: 'formatAmount3' },
    { field: 'totalProductPrice', title: '产品金额', minWidth: 120, formatter: 'formatAmount2' },
    { field: 'totalPrice', title: '优惠后金额', minWidth: 130, formatter: 'formatAmount2' },
    { field: 'status', title: '状态', minWidth: 120, formatter: ({ cellValue }) => cartStatusOptions.find((item) => item.value === cellValue)?.label || '' },
    { field: 'sourceNo', title: '来源单号', minWidth: 170 },
    { field: 'remark', title: '备注', minWidth: 160, showOverflow: 'tooltip' },
    { title: '操作', width: 330, fixed: 'right', slots: { default: 'actions' } },
  ];
}
