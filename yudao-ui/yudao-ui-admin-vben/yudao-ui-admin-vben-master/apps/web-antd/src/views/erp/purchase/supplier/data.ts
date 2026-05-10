import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { CommonStatusEnum, DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { z } from '#/adapter/form';
import { getBaseDataSimpleList } from '#/api/erp/base';
import { getAreaTree } from '#/api/system/area';
import { getSimpleUserList } from '#/api/system/user';

/** 基础信息表单 */
export function useBasicFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'name',
      label: '供应商名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入供应商名称',
      },
    },
    {
      fieldName: 'shortName',
      label: '简称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入简称',
      },
    },
    {
      fieldName: 'foreignName',
      label: '外文名',
      component: 'Input',
      componentProps: {
        placeholder: '请输入外文名',
      },
    },
    {
      fieldName: 'code',
      label: '编码',
      component: 'Input',
      componentProps: {
        placeholder: '系统自动生成',
        disabled: true,
      },
    },
    {
      fieldName: 'oldCode',
      label: '旧编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入旧编码',
      },
    },
    {
      fieldName: 'contact',
      label: '联系人',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系人',
      },
    },
    {
      fieldName: 'mobile',
      label: '手机号码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入手机号码',
      },
    },
    {
      fieldName: 'telephone',
      label: '联系电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系电话',
      },
    },
    {
      fieldName: 'email',
      label: '电子邮箱',
      component: 'Input',
      componentProps: {
        placeholder: '请输入电子邮箱',
      },
    },
    {
      fieldName: 'fax',
      label: '传真',
      component: 'Input',
      componentProps: {
        placeholder: '请输入传真',
      },
    },
    {
      fieldName: 'status',
      label: '开启状态',
      component: 'RadioGroup',
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
      rules: z.number().default(CommonStatusEnum.ENABLE),
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入排序',
      },
      rules: 'required',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注',
        rows: 3,
      },
      formItemClass: 'col-span-2',
    },
  ];
}

/** 分类与采购表单 */
export function useCategoryFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'region',
      label: '区域',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('region'),
        labelField: 'name',
        valueField: 'name',
        placeholder: '请选择区域',
      },
    },
    {
      fieldName: 'category',
      label: '往来类别',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('category'),
        labelField: 'name',
        valueField: 'name',
        placeholder: '请选择往来类别',
      },
    },
    {
      fieldName: 'supplierType',
      label: '供应商类型',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('supplier_type'),
        labelField: 'name',
        valueField: 'name',
        placeholder: '请选择供应商类型',
      },
    },
    {
      fieldName: 'purchaser',
      label: '采购员',
      component: 'ApiSelect',
      componentProps: {
        api: getSimpleUserList,
        labelField: 'nickname',
        valueField: 'id',
        placeholder: '请选择采购员',
      },
    },
    {
      fieldName: 'companyNature',
      label: '公司性质',
      component: 'Input',
      componentProps: {
        placeholder: '请输入公司性质',
      },
    },
    {
      fieldName: 'purchaseControl',
      label: '采购管控',
      component: 'Select',
      componentProps: {
        options: [
          { label: '是', value: '是' },
          { label: '否', value: '否' },
        ],
        placeholder: '请选择采购管控',
      },
    },
    {
      fieldName: 'arrivalCycle',
      label: '到货周期(天)',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入到货周期',
        min: 0,
        precision: 0,
      },
    },
    {
      fieldName: 'purchaseLeadDays',
      label: '采购提前期(天)',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入采购提前期',
        min: 0,
        precision: 0,
      },
    },
    {
      fieldName: 'obsolete',
      label: '淘汰',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '否', value: false },
          { label: '是', value: true },
        ],
      },
    },
    {
      fieldName: 'groupSupplier',
      label: '集团供应商',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '否', value: false },
          { label: '是', value: true },
        ],
      },
    },
    {
      fieldName: 'allowBranchOrder',
      label: '允许分店开单',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '否', value: false },
          { label: '是', value: true },
        ],
      },
    },
  ];
}

/** 结算与物流表单 */
export function useSettleFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'settleMethod',
      label: '结算方式',
      component: 'Select',
      componentProps: {
        options: [
          { label: '现金', value: '现金' },
          { label: '挂账', value: '挂账' },
          { label: '汇款', value: '汇款' },
          { label: '网上支付', value: '网上支付' },
        ],
        placeholder: '请选择结算方式',
      },
    },
    {
      fieldName: 'settleLocked',
      label: '结算锁定',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '否', value: false },
          { label: '是', value: true },
        ],
      },
    },
    {
      fieldName: 'transportMethod',
      label: '运输方式',
      component: 'Select',
      componentProps: {
        options: [
          { label: '客户自提', value: '客户自提' },
          { label: '配送服务物流托运', value: '配送服务物流托运' },
          { label: '送货上门', value: '送货上门' },
        ],
        placeholder: '请选择运输方式',
      },
    },
    {
      fieldName: 'freightType',
      label: '运费类型',
      component: 'Input',
      componentProps: {
        placeholder: '请输入运费类型',
      },
    },
    {
      fieldName: 'logisticsCompany',
      label: '物流公司',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('logistics_company'),
        labelField: 'name',
        valueField: 'name',
        placeholder: '请选择物流公司',
      },
    },
    {
      fieldName: 'arrivalPoint',
      label: '到货点',
      component: 'Input',
      componentProps: {
        placeholder: '请输入到货点',
      },
    },
    {
      fieldName: 'floatUpdateLastPrice',
      label: '浮动是否更新供应商最后进价',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: '是' },
          { label: '否', value: '否' },
        ],
        buttonStyle: 'solid',
        optionType: 'button',
      },
    },
    {
      fieldName: 'performanceProfitRef',
      label: '绩效考核利润参考依据',
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '备用价1', value: '备用价1' },
          { label: '参考价', value: '参考价' },
          { label: '零售价', value: '零售价' },
          { label: '批发价', value: '批发价' },
          { label: '最后一次采购入库价格', value: '最后一次采购入库价格' },
          { label: '库存成本价', value: '库存成本价' },
        ],
      },
      formItemClass: 'col-span-2',
    },
  ];
}

/** 地址信息表单 */
export function useAddressFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'address',
      label: '地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入地址',
      },
      formItemClass: 'col-span-2',
    },
    {
      fieldName: 'areaIds',
      label: '省/市/区县',
      component: 'ApiCascader',
      componentProps: {
        api: getAreaTree,
        fieldNames: { label: 'name', value: 'name', children: 'children' },
        placeholder: '请选择省/市/区县',
        showSearch: true,
        changeOnSelect: false,
      },
    },
    {
      fieldName: 'postalCode',
      label: '邮政编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入邮政编码',
      },
    },
    {
      fieldName: 'website',
      label: '网址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入网址',
      },
    },
  ];
}

/** 开票信息表单 */
export function useInvoiceFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'invoiceType',
      label: '开票类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '收据', value: '收据' },
          { label: '普通发票', value: '普通发票' },
          { label: '增值税发票', value: '增值税发票' },
        ],
        placeholder: '请选择开票类型',
      },
    },
    {
      fieldName: 'taxpayerId',
      label: '纳税人识别号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入纳税人识别号',
      },
    },
    {
      fieldName: 'invoiceBank',
      label: '开票银行',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开票银行',
      },
    },
    {
      fieldName: 'invoiceBankAccount',
      label: '开票银行账号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开票银行账号',
      },
    },
    {
      fieldName: 'invoiceAddress',
      label: '开票地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开票地址',
      },
    },
    {
      fieldName: 'invoicePhone',
      label: '开票电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开票电话',
      },
    },
    {
      fieldName: 'invoiceCompany',
      label: '开票单位',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开票单位',
      },
    },
  ];
}

/** 财务信息表单 */
export function useFinanceFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'account',
      label: '账户',
      component: 'Input',
      componentProps: {
        placeholder: '请输入账户',
      },
    },
    {
      fieldName: 'bankName',
      label: '开户行',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开户行',
      },
    },
    {
      fieldName: 'bankAccount',
      label: '开户账号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开户账号',
      },
    },
    {
      fieldName: 'bankAddress',
      label: '开户地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入开户地址',
      },
    },
    {
      fieldName: 'taxNo',
      label: '纳税人识别号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入纳税人识别号',
      },
    },
    {
      fieldName: 'taxPercent',
      label: '税率(%)',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入税率',
        min: 0,
        precision: 2,
      },
    },
    {
      fieldName: 'financePhone',
      label: '财务联系电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入财务联系电话',
      },
    },
    {
      fieldName: 'memberCode',
      label: '会员编码',
      component: 'Input',
      componentProps: {
        placeholder: '系统自动生成',
        disabled: true,
      },
    },
    {
      fieldName: 'legalPerson',
      label: '法定代表',
      component: 'Input',
      componentProps: {
        placeholder: '请输入法定代表',
      },
    },
    {
      fieldName: 'creditCode',
      label: '统一信用代码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入统一信用代码',
      },
    },
  ];
}

/** 兼容旧的 useFormSchema（供其他地方引用） */
export function useFormSchema(): VbenFormSchema[] {
  return useBasicFormSchema();
}

/** 搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '供应商名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入供应商名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'code',
      label: '编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'mobile',
      label: '手机号码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入手机号码',
        allowClear: true,
      },
    },
    {
      fieldName: 'telephone',
      label: '联系电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入联系电话',
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'code',
      title: '编码',
      minWidth: 120,
    },
    {
      field: 'name',
      title: '供应商名称',
      minWidth: 150,
    },
    {
      field: 'shortName',
      title: '简称',
      minWidth: 100,
    },
    {
      field: 'contact',
      title: '联系人',
      minWidth: 100,
    },
    {
      field: 'mobile',
      title: '手机号码',
      minWidth: 130,
    },
    {
      field: 'telephone',
      title: '联系电话',
      minWidth: 130,
    },
    {
      field: 'region',
      title: '区域',
      minWidth: 100,
    },
    {
      field: 'supplierType',
      title: '供应商类型',
      minWidth: 110,
    },
    {
      field: 'settleMethod',
      title: '结算方式',
      minWidth: 100,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.COMMON_STATUS },
      },
    },
    {
      field: 'sort',
      title: '排序',
      minWidth: 80,
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 150,
      showOverflow: 'tooltip',
    },
    {
      title: '操作',
      width: 130,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
