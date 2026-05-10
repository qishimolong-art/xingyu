import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getProductSimpleList } from '#/api/erp/product/product';
import { getWarehouseSimpleList } from '#/api/erp/stock/warehouse';
import { getRangePickerDefaultProps } from '#/utils';

/**
 * 报表搜索表单（五期：库存进出流水明细账）
 * 列对齐客户文档 4.2 节：发生日期区间 / 单据类型多选 / 仓库 / 产品 / 车型 / 产地 / 供应商
 */
export function useReportGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'bizDate',
      label: '开单日期',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
    {
      fieldName: 'bizTypes',
      label: '单据类型',
      component: 'Select',
      componentProps: {
        mode: 'multiple',
        placeholder: '请选择单据类型（可多选）',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.ERP_STOCK_RECORD_BIZ_TYPE, 'number'),
        maxTagCount: 'responsive',
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
      fieldName: 'productCode',
      label: '零件编码',
      component: 'Input',
      componentProps: { placeholder: '请输入零件编码', allowClear: true },
    },
    {
      fieldName: 'productName',
      label: '零件名称',
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
      fieldName: 'originPlace',
      label: '产地',
      component: 'Input',
      componentProps: { placeholder: '请输入产地', allowClear: true },
    },
    {
      fieldName: 'bizNo',
      label: '单号',
      component: 'Input',
      componentProps: { placeholder: '请输入单号关键字', allowClear: true },
    },
  ];
}

/**
 * 报表列表字段（五期：入出分列）
 * 列对齐客户文档 4.3 节
 */
export function useReportGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 55, fixed: 'left' },
    {
      field: 'bizDate',
      title: '发生日期',
      width: 170,
      formatter: 'formatDateTime',
      fixed: 'left',
    },
    {
      field: 'bizType',
      title: '交易类型',
      width: 120,
      slots: { default: 'bizType' },
      fixed: 'left',
    },
    {
      field: 'bizNo',
      title: '单号',
      width: 160,
    },
    {
      field: 'productCode',
      title: '零件编码',
      width: 110,
    },
    {
      field: 'productName',
      title: '产品名称',
      minWidth: 140,
    },
    {
      field: 'warehouseName',
      title: '仓库',
      width: 100,
    },
    // ========== 入库分列 ==========
    {
      field: 'inCount',
      title: '入库数',
      width: 100,
      formatter: 'formatAmount3',
    },
    {
      field: 'inUnitPrice',
      title: '入库单价',
      width: 110,
      formatter: 'formatAmount',
    },
    {
      field: 'inAmount',
      title: '入库金额',
      width: 120,
      formatter: 'formatAmount',
    },
    // ========== 出库分列 ==========
    {
      field: 'outCount',
      title: '出库数',
      width: 100,
      formatter: 'formatAmount3',
    },
    {
      field: 'outUnitPrice',
      title: '出库成本单价',
      width: 130,
      formatter: 'formatAmount',
    },
    {
      field: 'outAmount',
      title: '出库成本金额',
      width: 130,
      formatter: 'formatAmount',
    },
    // ========== 结存列 ==========
    {
      field: 'totalCount',
      title: '结存数',
      width: 100,
      formatter: 'formatAmount3',
      fixed: 'right',
    },
    {
      field: 'costPrice',
      title: '结存单价',
      width: 110,
      formatter: 'formatAmount',
      fixed: 'right',
    },
    {
      field: 'costAmount',
      title: '结存金额',
      width: 120,
      formatter: 'formatAmount',
      fixed: 'right',
    },
  ];
}

/** 搜索表单（旧页保留） */
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
    {
      fieldName: 'bizType',
      label: '类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择类型',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.ERP_STOCK_RECORD_BIZ_TYPE, 'number'),
      },
    },
    {
      fieldName: 'bizNo',
      label: '业务单号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入业务单号',
        allowClear: true,
      },
    },
    {
      fieldName: 'createTime',
      label: '创建时间',
      component: 'RangePicker',
      componentProps: {
        ...getRangePickerDefaultProps(),
        allowClear: true,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'productName',
      title: '产品名称',
      minWidth: 150,
    },
    {
      field: 'categoryName',
      title: '产品分类',
      width: 120,
    },
    {
      field: 'unitName',
      title: '产品单位',
      width: 100,
    },
    {
      field: 'warehouseName',
      title: '仓库',
      width: 120,
    },
    {
      field: 'bizType',
      title: '类型',
      width: 100,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.ERP_STOCK_RECORD_BIZ_TYPE },
      },
    },
    {
      field: 'bizNo',
      title: '出入库单号',
      width: 200,
      showOverflow: 'tooltip',
    },
    {
      field: 'bizDate',
      title: '发生日期',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'createTime',
      title: '出入库日期',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'count',
      title: '出入库数量',
      width: 120,
      formatter: 'formatAmount3',
    },
    {
      field: 'totalCount',
      title: '库存量',
      width: 100,
      formatter: 'formatAmount3',
    },
    {
      field: 'unitPrice',
      title: '单价',
      width: 110,
      formatter: 'formatAmount',
    },
    {
      field: 'totalPrice',
      title: '金额',
      width: 110,
      formatter: 'formatAmount',
    },
    {
      field: 'costPrice',
      title: '结存单价',
      width: 110,
      formatter: 'formatAmount',
    },
    {
      field: 'costAmount',
      title: '结存金额',
      width: 120,
      formatter: 'formatAmount',
    },
    {
      field: 'creatorName',
      title: '操作人',
      width: 100,
    },
  ];
}
