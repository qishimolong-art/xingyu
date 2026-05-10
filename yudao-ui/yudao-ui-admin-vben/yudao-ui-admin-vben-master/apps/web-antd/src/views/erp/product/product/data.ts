import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { CommonStatusEnum, DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { handleTree } from '@vben/utils';

import { z } from '#/adapter/form';
import { getProductCategorySimpleList } from '#/api/erp/product/category';
import { getProductUnitSimpleList } from '#/api/erp/product/unit';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';

/** 配件"基础档案" Tab 的表单项 */
export function useBaseFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      component: 'Input',
      fieldName: 'code',
      label: '编码',
      componentProps: {
        placeholder: '保存后自动生成',
        disabled: true,
      },
      dependencies: {
        triggerFields: ['id'],
        show: (values) => !!values.id,
      },
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: '零件名称',
      rules: 'required',
      componentProps: { placeholder: '请输入零件名称' },
    },
    {
      fieldName: 'unitId',
      label: '单位',
      component: 'ApiSelect',
      componentProps: {
        api: getProductUnitSimpleList,
        labelField: 'name',
        valueField: 'id',
        placeholder: '请选择单位',
      },
      rules: 'required',
    },
    {
      fieldName: 'defaultWarehouseId',
      label: '默认仓库',
      component: 'ApiSelect',
      componentProps: {
        api: getWarehouseSimpleList,
        labelField: 'name',
        valueField: 'id',
        placeholder: '请选择默认仓库',
        allowClear: true,
      },
    },
    {
      fieldName: 'vehicleModel',
      label: '适用车型',
      component: 'Input',
      componentProps: { placeholder: '请输入适用车型' },
    },
    {
      fieldName: 'standard',
      label: '规格',
      component: 'Input',
      componentProps: { placeholder: '请输入规格' },
    },
    {
      fieldName: 'categoryId',
      label: '类别',
      component: 'ApiTreeSelect',
      componentProps: {
        api: async () => {
          const data = await getProductCategorySimpleList();
          return handleTree(data);
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择类别',
        treeDefaultExpandAll: true,
      },
      rules: 'required',
    },
    {
      fieldName: 'barCode',
      label: '条形码',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入条形码' },
    },
    {
      fieldName: 'factoryCode',
      label: '厂家编码',
      component: 'Input',
      componentProps: { placeholder: '请输入厂家编码' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        buttonStyle: 'solid',
        optionType: 'button',
      },
      rules: z.number().default(CommonStatusEnum.ENABLE),
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { placeholder: '请输入备注' },
      formItemClass: 'col-span-2',
    },
  ];
}

/** 配件"价格列表" Tab 的表单项 */
export function usePriceFormSchema(): VbenFormSchema[] {
  const priceProps = {
    placeholder: '请输入价格，单位：元',
    precision: 2,
    min: 0,
    step: 0.01,
  };
  return [
    {
      fieldName: 'referencePrice',
      label: '参考价',
      component: 'InputNumber',
      componentProps: priceProps,
    },
    {
      fieldName: 'retailPrice',
      label: '零售价',
      component: 'InputNumber',
      componentProps: priceProps,
    },
    {
      fieldName: 'lastPurchasePrice',
      label: '最后采购入库价',
      component: 'InputNumber',
      componentProps: { ...priceProps, disabled: true, placeholder: '采购入库单回写' },
    },
    {
      fieldName: 'grossProfitRate',
      label: '毛利率（%）',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入毛利率百分比',
        min: -100,
        max: 100,
        precision: 0,
      },
    },
    {
      fieldName: 'backupPrice1',
      label: '备用价1',
      component: 'InputNumber',
      componentProps: priceProps,
    },
    {
      fieldName: 'wholesalePrice',
      label: '批发价',
      component: 'InputNumber',
      componentProps: priceProps,
    },
  ];
}

/** 配件"扩展信息" Tab 的表单项 */
export function useExtendFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'stockMax',
      label: '库存上限',
      component: 'InputNumber',
      componentProps: { placeholder: '请输入库存上限', min: 0, precision: 0 },
    },
    {
      fieldName: 'stockMin',
      label: '库存下限',
      component: 'InputNumber',
      componentProps: { placeholder: '请输入库存下限', min: 0, precision: 0 },
    },
    {
      fieldName: 'stockStandard',
      label: '标准库存',
      component: 'InputNumber',
      componentProps: { placeholder: '请输入标准库存', min: 0, precision: 0 },
    },
    {
      fieldName: 'packageQty',
      label: '包装数',
      component: 'InputNumber',
      componentProps: { placeholder: '请输入包装数', min: 1, precision: 0 },
    },
    {
      fieldName: 'weight',
      label: '重量（kg）',
      component: 'InputNumber',
      componentProps: {
        placeholder: '请输入重量（kg）',
        precision: 2,
        min: 0,
        step: 0.01,
      },
    },
  ];
}

/** 配件"库存信息" Tab 的（只读）表单项 */
export function useStockReadonlySchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'currentStock',
      label: '当前库存',
      component: 'InputNumber',
      componentProps: { disabled: true, placeholder: '系统自动计算' },
    },
    {
      fieldName: 'inTransitStock',
      label: '在途数量',
      component: 'InputNumber',
      componentProps: { disabled: true, placeholder: '系统自动计算' },
    },
    {
      fieldName: 'availableStock',
      label: '可用库存',
      component: 'InputNumber',
      componentProps: { disabled: true, placeholder: '系统自动计算' },
    },
  ];
}

/** 列表搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'code',
      label: '编码',
      component: 'Input',
      componentProps: { placeholder: '请输入配件编码', allowClear: true },
    },
    {
      fieldName: 'name',
      label: '名称',
      component: 'Input',
      componentProps: { placeholder: '请输入零件名称', allowClear: true },
    },
    {
      fieldName: 'vehicleModel',
      label: '适用车型',
      component: 'Input',
      componentProps: { placeholder: '请输入适用车型', allowClear: true },
    },
    {
      fieldName: 'factoryCode',
      label: '厂家编码',
      component: 'Input',
      componentProps: { placeholder: '请输入厂家编码', allowClear: true },
    },
    {
      fieldName: 'categoryId',
      label: '分类',
      component: 'ApiTreeSelect',
      componentProps: {
        api: async () => {
          const data = await getProductCategorySimpleList();
          return handleTree(data);
        },
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
        placeholder: '请选择分类',
        treeDefaultExpandAll: true,
      },
    },
  ];
}

/** 列表列 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { field: 'code', title: '编码', minWidth: 110, fixed: 'left' },
    { field: 'name', title: '零件名称', minWidth: 180 },
    { field: 'vehicleModel', title: '适用车型', minWidth: 140 },
    { field: 'standard', title: '规格', minWidth: 100 },
    { field: 'categoryName', title: '类别', minWidth: 120 },
    { field: 'unitName', title: '单位', minWidth: 80 },
    { field: 'barCode', title: '条形码', minWidth: 130 },
    { field: 'factoryCode', title: '厂家编码', minWidth: 120 },
    { field: 'defaultWarehouseName', title: '默认仓库', minWidth: 120 },
    {
      field: 'retailPrice',
      title: '零售价',
      minWidth: 100,
      formatter: 'formatAmount2',
    },
    {
      field: 'referencePrice',
      title: '参考价',
      minWidth: 100,
      formatter: 'formatAmount2',
    },
    {
      field: 'currentStock',
      title: '当前库存',
      minWidth: 110,
      slots: { default: 'currentStock' },
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 90,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.COMMON_STATUS },
      },
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 170,
      formatter: 'formatDateTime',
    },
    { title: '操作', width: 130, fixed: 'right', slots: { default: 'actions' } },
  ];
}
