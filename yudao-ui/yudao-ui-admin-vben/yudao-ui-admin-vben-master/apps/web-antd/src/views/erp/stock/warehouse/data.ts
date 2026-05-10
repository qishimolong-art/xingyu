import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpWarehouseApi } from '#/api/erp/stock/warehouse';

import { CommonStatusEnum, DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getBaseDataSimpleList } from '#/api/erp/base';
import { z } from '#/adapter/form';

/** 仓库类型选项 */
const warehouseTypeOptions = [
  { label: '正品仓库', value: 1 },
  { label: '废品仓库', value: 2 },
  { label: '待处理仓库', value: 3 },
  { label: '急件仓库', value: 4 },
  { label: '旧件仓库', value: 5 },
  { label: '寄售仓库', value: 6 },
  { label: '托管仓库', value: 7 },
  { label: '半成品仓', value: 8 },
];

/** 出入仓分组选项 */
const stockGroupTypeOptions = [
  { label: '全部', value: 1 },
  { label: '入仓单', value: 2 },
  { label: '出仓单', value: 3 },
  { label: '全部不分组', value: 4 },
];

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    // ========== 基本信息 ==========
    {
      fieldName: 'name',
      label: '仓库名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库名称',
      },
      rules: 'required',
    },
    {
      fieldName: 'warehouseCode',
      label: '仓库编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库编码',
      },
    },
    {
      fieldName: 'warehouseType',
      label: '仓库类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择仓库类型',
        options: warehouseTypeOptions,
        allowClear: true,
      },
    },
    {
      fieldName: 'storageCenterId',
      label: '仓储中心',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('storage_center'),
        labelField: 'name',
        valueField: 'id',
        placeholder: '请选择仓储中心',
        allowClear: true,
      },
    },
    {
      fieldName: 'storageWarehouseId',
      label: '仓储对应仓库',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('storage_warehouse'),
        labelField: 'name',
        valueField: 'id',
        placeholder: '请选择仓储对应仓库',
        allowClear: true,
      },
    },
    {
      fieldName: 'regionId',
      label: '区域',
      component: 'ApiSelect',
      componentProps: {
        api: () => getBaseDataSimpleList('region'),
        labelField: 'name',
        valueField: 'id',
        placeholder: '请选择区域',
        allowClear: true,
      },
    },
    {
      fieldName: 'address',
      label: '仓库地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库地址',
      },
    },
    {
      fieldName: 'warehouseLocation',
      label: '仓库地点',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库地点',
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
    // ========== 业务控制 ==========
    {
      fieldName: 'saleEnabled',
      label: '销售启用',
      component: 'Switch',
      componentProps: {
        checkedChildren: '启用',
        unCheckedChildren: '停用',
      },
      defaultValue: true,
    },
    {
      fieldName: 'purchaseEnabled',
      label: '采购启用',
      component: 'Switch',
      componentProps: {
        checkedChildren: '启用',
        unCheckedChildren: '停用',
      },
      defaultValue: true,
    },
    {
      fieldName: 'stockBillEnabled',
      label: '入出仓单',
      component: 'Switch',
      componentProps: {
        checkedChildren: '生成',
        unCheckedChildren: '不生成',
      },
      defaultValue: false,
    },
    {
      fieldName: 'ecommerceEnabled',
      label: '允许电商销售',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
    },
    {
      fieldName: 'scanControl',
      label: '扫码管控',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
    },
    {
      fieldName: 'saleBillControl',
      label: '销售开单管控',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
    },
    {
      fieldName: 'zeroStockHide',
      label: '0库存不显示',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
    },
    {
      fieldName: 'outPacking',
      label: '出仓打包装箱',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: false,
    },
    {
      fieldName: 'autoOrder',
      label: '自动订货',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: true,
    },
    {
      fieldName: 'splitOrder',
      label: '是否拆单',
      component: 'Switch',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      defaultValue: true,
    },
    // ========== 其他配置 ==========
    {
      fieldName: 'goodsToBranch',
      label: '货到分店',
      component: 'Select',
      componentProps: {
        placeholder: '请选择货到分店',
        options: getDictOptions('erp_warehouse_goods_branch', 'string'),
        allowClear: true,
      },
    },
    {
      fieldName: 'dept',
      label: '部门',
      component: 'Select',
      componentProps: {
        placeholder: '请选择部门',
        options: getDictOptions('erp_warehouse_dept', 'string'),
        allowClear: true,
      },
    },
    {
      fieldName: 'maxPickCount',
      label: '同时拣货单数',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入允许同时拣货单数',
        min: 1,
        precision: 0,
      },
      defaultValue: 100000,
    },
    {
      fieldName: 'stockGroupType',
      label: '出入仓分组',
      component: 'Select',
      componentProps: {
        placeholder: '请选择出入仓分组',
        options: stockGroupTypeOptions,
        allowClear: true,
      },
      defaultValue: 1,
    },
    {
      fieldName: 'creditControl',
      label: '额度管控',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入额度管控金额',
        min: 0,
        precision: 2,
      },
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入排序',
        precision: 0,
      },
      rules: 'required',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注',
      },
    },
  ];
}

/** 搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '仓库名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'warehouseCode',
      label: '仓库编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入仓库编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'warehouseType',
      label: '仓库类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择仓库类型',
        allowClear: true,
        options: warehouseTypeOptions,
      },
    },
    {
      fieldName: 'status',
      label: '仓库状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择仓库状态',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(
  onDefaultStatusChange?: (
    newStatus: boolean,
    row: ErpWarehouseApi.Warehouse,
  ) => PromiseLike<boolean | undefined>,
): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'warehouseCode',
      title: '仓库编码',
      minWidth: 120,
    },
    {
      field: 'name',
      title: '仓库名称',
      minWidth: 150,
    },
    {
      field: 'warehouseType',
      title: '仓库类型',
      minWidth: 110,
      formatter: ({ cellValue }) => {
        const map: Record<number, string> = {
          1: '正品仓库', 2: '废品仓库', 3: '待处理仓库', 4: '急件仓库',
          5: '旧件仓库', 6: '寄售仓库', 7: '托管仓库', 8: '半成品仓',
        };
        return map[cellValue] || '';
      },
    },
    {
      field: 'saleEnabled',
      title: '销售',
      minWidth: 70,
      formatter: ({ cellValue }) => cellValue ? '启用' : '停用',
    },
    {
      field: 'purchaseEnabled',
      title: '采购',
      minWidth: 70,
      formatter: ({ cellValue }) => cellValue ? '启用' : '停用',
    },
    {
      field: 'address',
      title: '仓库地址',
      minWidth: 180,
      showOverflow: 'tooltip',
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
      field: 'defaultStatus',
      title: '是否默认',
      minWidth: 100,
      cellRender: {
        attrs: { beforeChange: onDefaultStatusChange },
        name: 'CellSwitch',
        props: {
          checkedValue: true,
          unCheckedValue: false,
        },
      },
    },
    {
      field: 'sort',
      title: '排序',
      minWidth: 80,
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 130,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
