import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { erpPriceInputFormatter } from '@vben/utils';

import { z } from '#/adapter/form';
import { getBaseDataSimpleList } from '#/api/erp/base';
import { getAccountSimpleList } from '#/api/erp/finance/account';
import { getProductSimpleList } from '#/api/erp/product/product';
import { getSupplierSimpleList } from '#/api/erp/purchase/supplier';
import { getSimpleDeptList } from '#/api/system/dept';
import { getSimpleUserList } from '#/api/system/user';
import { getRangePickerDefaultProps } from '#/utils';

/** 表单的配置项 */
export function useFormSchema(formType: string): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'no',
      label: '订单单号',
      component: 'Input',
      componentProps: {
        placeholder: '系统自动生成',
        disabled: true,
      },
    },
    {
      fieldName: 'orderTime',
      label: '创建时间',
      component: 'DatePicker',
      componentProps: {
        placeholder: '系统自动生成',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'x',
        disabled: true,
      },
      rules: 'required',
    },
    {
      label: '供应商',
      fieldName: 'supplierId',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择供应商',
        allowClear: true,
        showSearch: true,
        api: getSupplierSimpleList,
        labelField: 'name',
        valueField: 'id',
      },
      rules: 'required',
    },
    {
      fieldName: 'purchaser',
      label: '采购员',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择采购员',
        allowClear: true,
        showSearch: true,
        api: getSimpleUserList,
        labelField: 'nickname',
        valueField: 'id',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'deptId',
      label: '部门',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择部门',
        allowClear: true,
        showSearch: true,
        api: getSimpleDeptList,
        labelField: 'name',
        valueField: 'id',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'taxPercent',
      label: '税率(%)',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '请输入税率',
        min: 0,
        max: 100,
        precision: 2,
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'orderDate',
      label: '订货日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择订货日期',
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'purchaseCycle',
      label: '采购周期',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '请输入采购周期(天)',
        min: 0,
        precision: 0,
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'arrivalDate',
      label: '到货日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择到货日期',
        format: 'YYYY-MM-DD',
        valueFormat: 'x',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'deliveryMethod',
      label: '送货方式',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择送货方式',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('delivery_method'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'purchaseType',
      label: '采购方式',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择采购方式',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('purchase_type'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'settleMethod',
      label: '结算方式',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择结算方式',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('settle_method'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'invoiceType',
      label: '开票类型',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择开票类型',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('invoice_type'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'factoryOrderNo',
      label: '厂家单号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入厂家单号',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'receiveAddress',
      label: '收货地址',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择收货地址',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('receive_address'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'orderFormula',
      label: '订货公式',
      component: 'Input',
      componentProps: {
        placeholder: '请输入订货公式',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'orderCompany',
      label: '订货公司',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择订货公司',
        allowClear: true,
        showSearch: true,
        api: () => getBaseDataSimpleList('order_company'),
        labelField: 'name',
        valueField: 'name',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'sendDate',
      label: '发出日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择发出日期',
        format: 'YYYY-MM-DD',
        valueFormat: 'x',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'latestArrivalDate',
      label: '最近到货日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择最近到货日期',
        format: 'YYYY-MM-DD',
        valueFormat: 'x',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'saleDateFrom',
      label: '销售日期从',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择销售日期从',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'saleDateTo',
      label: '到销售日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '选择到销售日期',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注',
        autoSize: { minRows: 1, maxRows: 1 },
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-2',
    },
    {
      fieldName: 'fileUrl',
      label: '附件',
      component: 'FileUpload',
      componentProps: {
        maxNumber: 1,
        maxSize: 10,
        accept: [
          'pdf',
          'doc',
          'docx',
          'xls',
          'xlsx',
          'txt',
          'jpg',
          'jpeg',
          'png',
        ],
        showDescription: formType !== 'detail',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-3',
    },
    {
      fieldName: 'items',
      label: '采购产品清单',
      component: 'Input',
      formItemClass: 'col-span-3',
    },
    {
      fieldName: 'discountPercent',
      label: '优惠率(%)',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '请输入优惠率',
        min: 0,
        max: 100,
        precision: 2,
      },
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
      rules: z.number().min(0).optional(),
    },
    {
      fieldName: 'discountPrice',
      label: '付款优惠',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '付款优惠',
        precision: 2,
        formatter: erpPriceInputFormatter,
        disabled: true,
      },
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'totalPrice',
      label: '优惠后金额',
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '优惠后金额',
        precision: 2,
        formatter: erpPriceInputFormatter,
        disabled: true,
      },
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'accountId',
      label: '结算账户',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择结算账户',
        allowClear: true,
        showSearch: true,
        api: getAccountSimpleList,
        labelField: 'name',
        valueField: 'id',
      },
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        placeholder: '请输入支付订金',
        precision: 2,
        min: 0,
      },
      fieldName: 'depositPrice',
      label: '支付订金',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
      rules: z.number().min(0).optional(),
    },
  ];
}

/** 表单的明细表格列 */
export function useFormItemColumns(
  disabled: boolean,
): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 50, fixed: 'left' },
    {
      field: 'gift',
      title: '赠品',
      width: 90,
      fixed: 'left',
      slots: { default: 'gift' },
    },
    {
      field: 'productCode',
      title: '编码',
      minWidth: 110,
    },
    {
      field: 'productId',
      title: '零件名称',
      minWidth: 180,
      slots: { default: 'productId' },
    },
    {
      field: 'vehicleModel',
      title: '适用车型',
      minWidth: 120,
    },
    {
      field: 'originPlace',
      title: '产地',
      minWidth: 80,
    },
    {
      field: 'standard',
      title: '规格',
      minWidth: 80,
    },
    {
      field: 'featureCode',
      title: '特征码',
      minWidth: 90,
    },
    {
      field: 'productUnitName',
      title: '单位',
      minWidth: 60,
    },
    {
      field: 'count',
      title: '订货数',
      minWidth: 110,
      slots: { default: 'count' },
    },
    {
      field: 'productPrice',
      title: '订货价',
      minWidth: 110,
      slots: { default: 'productPrice' },
    },
    {
      field: 'totalProductPrice',
      title: '订货金额',
      minWidth: 110,
      formatter: 'formatAmount2',
    },
    {
      field: 'arrivalCount',
      title: '到货数',
      minWidth: 90,
    },
    {
      field: 'warehouseId',
      title: '所属仓库',
      minWidth: 140,
      slots: { default: 'warehouseId' },
    },
    {
      field: 'warehousePosition',
      title: '货架位',
      minWidth: 100,
      slots: { default: 'warehousePosition' },
    },
    {
      field: 'drawingNo',
      title: '图号',
      minWidth: 100,
      slots: { default: 'drawingNo' },
    },
    {
      field: 'batchNo',
      title: '批次',
      minWidth: 100,
      slots: { default: 'batchNo' },
    },
    {
      field: 'factoryCode',
      title: '厂家编码',
      minWidth: 100,
    },
    {
      field: 'brand',
      title: '品牌',
      minWidth: 80,
    },
    {
      field: 'remark',
      title: '开单备注',
      minWidth: 130,
      slots: { default: 'remark' },
    },
    {
      field: 'supplierId',
      title: '供应商',
      minWidth: 140,
      slots: { default: 'supplierId' },
    },
    {
      title: '操作',
      width: 50,
      slots: { default: 'actions' },
      visible: !disabled,
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'no',
      label: '订单单号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入订单单号',
        allowClear: true,
      },
    },
    {
      fieldName: 'productId',
      label: '产品',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择产品',
        allowClear: true,
        showSearch: true,
        api: getProductSimpleList,
        labelField: 'name',
        valueField: 'id',
      },
    },
    {
      fieldName: 'orderTime',
      label: '订单时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'supplierId',
      label: '供应商',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择供应商',
        allowClear: true,
        showSearch: true,
        api: getSupplierSimpleList,
        labelField: 'name',
        valueField: 'id',
      },
    },
    {
      fieldName: 'creator',
      label: '创建人',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择创建人',
        allowClear: true,
        showSearch: true,
        api: getSimpleUserList,
        labelField: 'nickname',
        valueField: 'id',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.ERP_AUDIT_STATUS, 'number'),
        placeholder: '请选择状态',
        allowClear: true,
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Input',
      componentProps: {
        placeholder: '请输入备注',
        allowClear: true,
      },
    },
    {
      fieldName: 'inStatus',
      label: '入库状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '未入库', value: 0 },
          { label: '部分入库', value: 1 },
          { label: '全部入库', value: 2 },
        ],
        placeholder: '请选择入库状态',
        allowClear: true,
      },
    },
    {
      fieldName: 'returnStatus',
      label: '退货状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '未退货', value: 0 },
          { label: '部分退货', value: 1 },
          { label: '全部退货', value: 2 },
        ],
        placeholder: '请选择退货状态',
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      type: 'checkbox',
      width: 50,
      fixed: 'left',
    },
    {
      field: 'no',
      title: '订单单号',
      width: 200,
      fixed: 'left',
    },
    {
      field: 'productNames',
      title: '产品信息',
      showOverflow: 'tooltip',
      minWidth: 120,
    },
    {
      field: 'supplierName',
      title: '供应商',
      minWidth: 120,
    },
    {
      field: 'orderTime',
      title: '订单时间',
      width: 160,
      formatter: 'formatDate',
    },
    {
      field: 'arrivalDate',
      title: '到货日期',
      width: 160,
      formatter: 'formatDate',
    },
    {
      field: 'purchaseType',
      title: '采购类型',
      minWidth: 100,
    },
    {
      field: 'settleMethod',
      title: '结算方式',
      minWidth: 100,
    },
    {
      field: 'creatorName',
      title: '创建人',
      minWidth: 120,
    },
    {
      field: 'totalCount',
      title: '总数量',
      formatter: 'formatAmount3',
      minWidth: 120,
    },
    {
      field: 'inCount',
      title: '入库数量',
      formatter: 'formatAmount3',
      minWidth: 120,
    },
    {
      field: 'returnCount',
      title: '退货数量',
      formatter: 'formatAmount3',
      minWidth: 120,
    },
    {
      field: 'totalProductPrice',
      title: '金额合计',
      formatter: 'formatAmount2',
      minWidth: 120,
    },
    {
      field: 'totalPrice',
      title: '含税金额',
      formatter: 'formatAmount2',
      minWidth: 120,
    },
    {
      field: 'depositPrice',
      title: '支付订金',
      formatter: 'formatAmount2',
      minWidth: 120,
      visible: false,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.ERP_AUDIT_STATUS },
      },
    },
    {
      title: '操作',
      width: 260,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
