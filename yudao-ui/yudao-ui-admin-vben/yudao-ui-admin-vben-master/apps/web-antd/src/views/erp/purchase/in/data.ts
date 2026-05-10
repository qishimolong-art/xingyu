import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { erpNumberFormatter } from '@vben/utils';

import { getAccountSimpleList } from '#/api/erp/finance/account';
import { getProductSimpleList } from '#/api/erp/product/product';
import { getSupplierSimpleList } from '#/api/erp/purchase/supplier';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';
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
      label: '入库单号',
      component: 'Input',
      componentProps: {
        placeholder: '系统自动生成',
        disabled: true,
      },
    },
    {
      fieldName: 'inTime',
      label: '入库时间',
      component: 'DatePicker',
      componentProps: {
        disabled: formType === 'detail',
        placeholder: '选择入库时间',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'x',
      },
      rules: 'required',
    },
    {
      fieldName: 'orderNo',
      label: '关联订单',
      component: 'Input',
      formItemClass: 'col-span-1',
      componentProps: {
        placeholder: '请选择关联订单',
        disabled: formType === 'detail',
      },
    },
    {
      fieldName: 'supplierId',
      label: '供应商',
      component: 'ApiSelect',
      componentProps: {
        disabled: formType === 'detail',
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
      label: '入库产品清单',
      component: 'Input',
      formItemClass: 'col-span-3',
    },
    // ========== 汽配扩展字段：系统信息 ==========
    {
      fieldName: 'purchaser',
      label: '采购员',
      component: 'Input',
      componentProps: {
        placeholder: '请输入采购员',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
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
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'purchaseArea',
      label: '进货区',
      component: 'Input',
      componentProps: {
        placeholder: '请输入进货区',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'accountant',
      label: '记账员',
      component: 'Input',
      componentProps: {
        placeholder: '请输入记账员',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'invoiceType',
      label: '开票类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择开票类型',
        allowClear: true,
        disabled: formType === 'detail',
        options: [
          { label: '收据', value: '收据' },
          { label: '不开票', value: '不开票' },
          { label: '专票', value: '专票' },
          { label: '普票', value: '普票' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'transportMethod',
      label: '运输方式',
      component: 'Select',
      componentProps: {
        placeholder: '请选择运输方式',
        allowClear: true,
        disabled: formType === 'detail',
        options: [
          { label: '快递', value: '快递' },
          { label: '物流', value: '物流' },
          { label: '自提', value: '自提' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'settleMethod',
      label: '结算方式',
      component: 'Select',
      componentProps: {
        placeholder: '请选择结算方式',
        allowClear: true,
        disabled: formType === 'detail',
        options: [
          { label: '挂账', value: '挂账' },
          { label: '现结', value: '现结' },
          { label: '月结', value: '月结' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'priority',
      label: '优先级',
      component: 'Select',
      componentProps: {
        placeholder: '请选择优先级',
        allowClear: true,
        disabled: formType === 'detail',
        options: [
          { label: '正常件', value: '正常件' },
          { label: '紧急件', value: '紧急件' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'orderMethod',
      label: '开单方式',
      component: 'Select',
      componentProps: {
        placeholder: '请选择开单方式',
        allowClear: true,
        disabled: formType === 'detail',
        options: [
          { label: '正常单', value: '正常单' },
          { label: '样品单', value: '样品单' },
          { label: '赠品单', value: '赠品单' },
        ],
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'factoryOrderNo',
      label: '厂家单号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入厂家单号',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'floatRate',
      label: '浮动率',
      component: 'InputNumber',
      defaultValue: 1,
      componentProps: {
        placeholder: '请输入浮动率',
        precision: 2,
        min: 0,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'packageCount',
      label: '件数',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: {
        placeholder: '请输入件数',
        precision: 0,
        min: 0,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    // ========== 汽配扩展字段：运费信息 ==========
    {
      fieldName: 'freightType1',
      label: '运费类型1',
      component: 'Input',
      componentProps: {
        placeholder: '请输入运费类型1',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'freightType2',
      label: '运费类型2',
      component: 'Input',
      componentProps: {
        placeholder: '请输入运费类型2',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'freightObject1',
      label: '运费对象1',
      component: 'Input',
      componentProps: {
        placeholder: '请输入运费对象1',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'freightObject2',
      label: '运费对象2',
      component: 'Input',
      componentProps: {
        placeholder: '请输入运费对象2',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'logisticsCompany',
      label: '物流公司',
      component: 'Input',
      componentProps: {
        placeholder: '请输入物流公司',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'totalFreight1',
      label: '总运费1',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入总运费1',
        precision: 2,
        min: 0,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'totalFreight2',
      label: '总运费2',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入总运费2',
        precision: 2,
        min: 0,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    // ========== 汽配扩展字段：供应商信息 ==========
    {
      fieldName: 'handler',
      label: '经办人',
      component: 'Input',
      componentProps: {
        placeholder: '请输入经办人',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'taxRate',
      label: '税率(%)',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入税率',
        precision: 2,
        min: 0,
        max: 100,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'purchaseDiscount',
      label: '采购折让',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入采购折让金额',
        precision: 2,
        min: 0,
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'unloader',
      label: '卸货员',
      component: 'Input',
      componentProps: {
        placeholder: '请输入卸货员',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'floatRecord',
      label: '浮动记录',
      component: 'Input',
      componentProps: {
        placeholder: '请输入浮动记录',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'receiveUnit',
      label: '收货单位',
      component: 'Input',
      componentProps: {
        placeholder: '请输入收货单位',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'paymentDate',
      label: '付款日期',
      component: 'DatePicker',
      componentProps: {
        disabled: formType === 'detail',
        placeholder: '选择付款日期',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'x',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'hasInvoice',
      label: '是否发票',
      component: 'Switch',
      componentProps: {
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'businessEntity',
      label: '所属经营',
      component: 'Input',
      componentProps: {
        placeholder: '请输入所属经营',
        disabled: formType === 'detail',
      },
      formItemClass: 'col-span-1',
    },
  ];
}

/** 表单的明细表格列 */
export function useFormItemColumns(
  formData?: any[],
  disabled?: boolean,
): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', minWidth: 50, fixed: 'left' },
    {
      field: 'warehouseId',
      title: '仓库名称',
      minWidth: 200,
      slots: { default: 'warehouseId' },
    },
    {
      field: 'productId',
      title: '产品名称',
      minWidth: 200,
      slots: { default: 'productId' },
    },
    {
      field: 'stockCount',
      title: '库存',
      minWidth: 80,
    },
    {
      field: 'productBarCode',
      title: '条码',
      minWidth: 120,
    },
    {
      field: 'productUnitName',
      title: '单位',
      minWidth: 80,
    },
    {
      field: 'packageQty',
      title: '包装数',
      formatter: 'formatAmount3',
      minWidth: 80,
    },
    {
      field: 'wholeQty',
      title: '整件数',
      minWidth: 90,
      slots: { default: 'wholeQty' },
    },
    {
      field: 'warehousePosition',
      title: '货架位',
      minWidth: 120,
      slots: { default: 'warehousePosition' },
    },
    {
      field: 'barCode',
      title: '条形码',
      minWidth: 140,
      slots: { default: 'barCode' },
    },
    {
      field: 'batchNo',
      title: '批次',
      minWidth: 100,
      slots: { default: 'batchNo' },
    },
    {
      field: 'drawingNo',
      title: '图号',
      minWidth: 100,
      slots: { default: 'drawingNo' },
    },
    {
      field: 'brand',
      title: '品牌',
      minWidth: 100,
    },
    {
      field: 'vehicleModel',
      title: '适用车型',
      minWidth: 120,
    },
    {
      field: 'originPlace',
      title: '产地',
      minWidth: 100,
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 150,
      slots: { default: 'remark' },
    },
    {
      field: 'totalCount',
      title: '原数量',
      formatter: 'formatAmount3',
      minWidth: 120,
      fixed: 'right',
      visible: formData && formData[0]?.inCount !== undefined,
    },
    {
      field: 'inCount',
      title: '已入库',
      formatter: 'formatAmount3',
      minWidth: 120,
      fixed: 'right',
      visible: formData && formData[0]?.returnCount !== undefined,
    },
    {
      field: 'count',
      title: '数量',
      minWidth: 120,
      fixed: 'right',
      slots: { default: 'count' },
    },
    {
      field: 'productPrice',
      title: '产品单价',
      fixed: 'right',
      minWidth: 120,
      slots: { default: 'productPrice' },
    },
    {
      field: 'totalProductPrice',
      fixed: 'right',
      title: '产品金额',
      minWidth: 120,
      formatter: 'formatAmount2',
    },
    {
      fixed: 'right',
      field: 'taxPercent',
      title: '税率(%)',
      minWidth: 105,
      slots: { default: 'taxPercent' },
    },
    {
      fixed: 'right',
      field: 'taxPrice',
      title: '税额',
      minWidth: 120,
      formatter: 'formatAmount2',
    },
    {
      field: 'totalPrice',
      fixed: 'right',
      title: '合计金额',
      minWidth: 120,
      formatter: 'formatAmount2',
    },
    {
      title: '操作',
      width: 50,
      fixed: 'right',
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
      label: '入库单号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入入库单号',
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
      fieldName: 'inTime',
      label: '入库时间',
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
      fieldName: 'warehouseId',
      label: '仓库',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择仓库',
        allowClear: true,
        showSearch: true,
        api: getWarehouseSimpleList,
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
      fieldName: 'orderNo',
      label: '关联订单',
      component: 'Input',
      componentProps: {
        placeholder: '请输入关联订单号',
        allowClear: true,
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
    },
    {
      fieldName: 'paymentStatus',
      label: '付款状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '未付款', value: 0 },
          { label: '部分付款', value: 1 },
          { label: '全部付款', value: 2 },
        ],
        placeholder: '请选择付款状态',
        allowClear: true,
      },
    },
    {
      fieldName: 'status',
      label: '审批状态',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.ERP_AUDIT_STATUS, 'number'),
        placeholder: '请选择审批状态',
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
      title: '入库单号',
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
      field: 'inTime',
      title: '入库时间',
      width: 160,
      formatter: 'formatDate',
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
      field: 'totalPrice',
      title: '应付金额',
      formatter: 'formatAmount2',
      minWidth: 120,
    },
    {
      field: 'paymentPrice',
      title: '已付金额',
      formatter: 'formatAmount2',
      minWidth: 120,
    },
    {
      field: 'unPaymentPrice',
      title: '未付金额',
      formatter: ({ row }) => {
        return `${erpNumberFormatter(row.totalPrice - row.paymentPrice, 2)}元`;
      },
      minWidth: 120,
    },
    {
      field: 'status',
      title: '审批状态',
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

/** 列表的搜索表单 */
export function useOrderGridFormSchema(): VbenFormSchema[] {
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
  ];
}

/** 列表的字段 */
export function useOrderGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      type: 'radio',
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
  ];
}
