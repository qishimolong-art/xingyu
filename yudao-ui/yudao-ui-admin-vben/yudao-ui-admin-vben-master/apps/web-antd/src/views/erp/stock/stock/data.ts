import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { getProductCategorySimpleList } from '#/api/erp/product/category';
import { getPriceSystemSimpleList } from '#/api/erp/product/pricesystem';
import { getProductSimpleList } from '#/api/erp/product/product';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';

/** 搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
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
    // ========== 产品条件 ==========
    {
      fieldName: 'productCode',
      label: '零件编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入零件编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'productName',
      label: '零件名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入零件名称',
        allowClear: true,
      },
    },
    {
      fieldName: 'drawingNo',
      label: '图号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入图号',
        allowClear: true,
      },
    },
    {
      fieldName: 'vehicleModel',
      label: '适用车型',
      component: 'Input',
      componentProps: {
        placeholder: '请输入适用车型',
        allowClear: true,
      },
    },
    {
      fieldName: 'originPlace',
      label: '产地',
      component: 'Input',
      componentProps: {
        placeholder: '请输入产地',
        allowClear: true,
      },
    },
    {
      fieldName: 'brand',
      label: '品牌',
      component: 'Input',
      componentProps: {
        placeholder: '请输入品牌',
        allowClear: true,
      },
    },
    {
      fieldName: 'shelf',
      label: '货架',
      component: 'Input',
      componentProps: {
        placeholder: '请输入货架位',
        allowClear: true,
      },
    },
    {
      fieldName: 'featureCode',
      label: '特征码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入特征码',
        allowClear: true,
      },
    },
    {
      fieldName: 'standard',
      label: '规格',
      component: 'Input',
      componentProps: {
        placeholder: '请输入规格',
        allowClear: true,
      },
    },
    {
      fieldName: 'factoryCode',
      label: '厂家编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入厂家编码',
        allowClear: true,
      },
    },
    {
      fieldName: 'barCode',
      label: '条形码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入条形码',
        allowClear: true,
      },
    },
    {
      fieldName: 'oeNumber',
      label: 'OE号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入OE号',
        allowClear: true,
      },
    },
    {
      fieldName: 'categoryId',
      label: '产品分类',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择产品分类',
        allowClear: true,
        showSearch: true,
        api: getProductCategorySimpleList,
        labelField: 'name',
        valueField: 'id',
      },
    },
    {
      fieldName: 'productStatus',
      label: '产品状态',
      component: 'Select',
      componentProps: {
        placeholder: '请选择产品状态',
        allowClear: true,
        options: [
          { label: '启用', value: 0 },
          { label: '停用', value: 1 },
        ],
      },
    },
    // ========== 数量条件 ==========
    {
      fieldName: 'countFilter',
      label: '库存数',
      component: 'Select',
      componentProps: {
        placeholder: '请选择',
        allowClear: true,
        options: [
          { label: '全部', value: 0 },
          { label: '大于 0', value: 1 },
          { label: '等于 0', value: 2 },
        ],
      },
    },
    {
      fieldName: 'countMin',
      label: '库存数量最小',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最小值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'countMax',
      label: '库存数量最大',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最大值',
        class: 'w-full',
      },
    },
    // ========== 上下限 ==========
    {
      fieldName: 'stockMaxMin',
      label: '库存上限最小',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最小值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'stockMaxMax',
      label: '库存上限最大',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最大值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'stockMinMin',
      label: '库存下限最小',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最小值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'stockMinMax',
      label: '库存下限最大',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最大值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'stockStandardMin',
      label: '标准库存最小',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最小值',
        class: 'w-full',
      },
    },
    {
      fieldName: 'stockStandardMax',
      label: '标准库存最大',
      component: 'InputNumber',
      componentProps: {
        placeholder: '最大值',
        class: 'w-full',
      },
    },
    // ========== 特殊筛选 ==========
    {
      fieldName: 'shelfDuplicateOnly',
      label: '货架位重复',
      component: 'Checkbox',
      renderComponentContent: () => ({
        default: () => '仅显示重复的货架位',
      }),
    },
    {
      fieldName: 'shelfEmptyOnly',
      label: '空置货架位',
      component: 'Checkbox',
      renderComponentContent: () => ({
        default: () => '仅显示空置货架位',
      }),
    },
    {
      fieldName: 'positiveCountOnly',
      label: '正库存',
      component: 'Checkbox',
      renderComponentContent: () => ({
        default: () => '只显示正库存',
      }),
    },
    // ========== 价格体系 ==========
    {
      fieldName: 'priceSystemId',
      label: '价格体系',
      component: 'ApiSelect',
      componentProps: {
        placeholder: '请选择价格体系',
        allowClear: true,
        showSearch: true,
        api: getPriceSystemSimpleList,
        labelField: 'name',
        valueField: 'id',
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
      field: 'productName',
      title: '产品名称',
      minWidth: 150,
      fixed: 'left',
    },
    {
      field: 'unitName',
      title: '产品单位',
      minWidth: 100,
    },
    {
      field: 'categoryName',
      title: '产品分类',
      minWidth: 120,
    },
    {
      field: 'count',
      title: '库存量',
      minWidth: 100,
      formatter: 'formatAmount3',
      slots: { default: 'count' },
    },
    {
      field: 'productCode',
      title: '编码',
      minWidth: 110,
    },
    {
      field: 'drawingNo',
      title: '图号',
      minWidth: 110,
    },
    {
      field: 'standard',
      title: '规格',
      minWidth: 110,
    },
    {
      field: 'featureCode',
      title: '特征码',
      minWidth: 100,
    },
    {
      field: 'vehicleModel',
      title: '适用车型',
      minWidth: 140,
    },
    {
      field: 'brand',
      title: '品牌',
      minWidth: 100,
    },
    {
      field: 'originPlace',
      title: '产地',
      minWidth: 100,
    },
    {
      field: 'shelf',
      title: '货架',
      minWidth: 100,
    },
    {
      field: 'lastPurchasePrice',
      title: '进价',
      minWidth: 100,
      formatter: 'formatAmount',
    },
    {
      field: 'occupiedCount',
      title: '占用数',
      minWidth: 90,
      formatter: 'formatAmount3',
    },
    {
      field: 'pendingInCount',
      title: '未入数',
      minWidth: 90,
      formatter: 'formatAmount3',
    },
    {
      field: 'inTransitCount',
      title: '在途数',
      minWidth: 90,
      formatter: 'formatAmount3',
    },
    {
      field: 'stockMin',
      title: '库存下限',
      minWidth: 90,
    },
    {
      field: 'stockMax',
      title: '库存上限',
      minWidth: 90,
    },
    {
      field: 'stockStandard',
      title: '标准库存',
      minWidth: 100,
    },
    {
      field: 'costPrice',
      title: '成本均价',
      minWidth: 120,
      formatter: 'formatAmount',
    },
    {
      field: 'costAmount',
      title: '成本金额',
      minWidth: 120,
      formatter: 'formatAmount',
    },
    {
      field: 'currentPrice',
      title: '价格体系单价',
      minWidth: 120,
      formatter: 'formatAmount',
      visible: false,
    },
    {
      field: 'currentPriceAmount',
      title: '价格体系金额',
      minWidth: 120,
      formatter: 'formatAmount',
      visible: false,
    },
    {
      field: 'warehouseName',
      title: '仓库',
      minWidth: 120,
    },
    {
      field: 'actions',
      title: '操作',
      width: 160,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
