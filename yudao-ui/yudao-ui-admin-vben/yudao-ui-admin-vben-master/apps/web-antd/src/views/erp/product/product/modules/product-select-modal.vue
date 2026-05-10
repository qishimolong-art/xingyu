<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpProductApi } from '#/api/erp/product/product';

import { ref } from 'vue';

import { handleTree } from '@vben/utils';

import { message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProductCategorySimpleList } from '#/api/erp/product/category';
import { getProductPage } from '#/api/erp/product/product';

const emit = defineEmits<{
  success: [rows: ErpProductApi.Product[]];
}>();

const open = ref<boolean>(false);
const selectedRows = ref<ErpProductApi.Product[]>([]);

function useSearchSchema(): VbenFormSchema[] {
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
        allowClear: true,
      },
    },
  ];
}

function useColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { field: 'code', title: '编码', width: 110, fixed: 'left' },
    { field: 'name', title: '名称', minWidth: 180 },
    { field: 'vehicleModel', title: '车型', minWidth: 140 },
    { field: 'factoryCode', title: '厂家编码', minWidth: 120 },
    { field: 'categoryName', title: '分类', minWidth: 120 },
    { field: 'unitName', title: '单位', width: 80 },
    {
      field: 'currentStock',
      title: '当前库存',
      width: 130,
      slots: { default: 'currentStock' },
    },
    {
      field: 'referencePrice',
      title: '参考价',
      width: 100,
      formatter: 'formatAmount2',
    },
    {
      field: 'retailPrice',
      title: '零售价',
      width: 100,
      formatter: 'formatAmount2',
    },
    {
      field: 'purchasePrice',
      title: '采购价',
      width: 100,
      formatter: 'formatAmount2',
    },
    { field: 'remark', title: '零件备注', minWidth: 120 },
    {
      field: 'weight',
      title: '重量',
      width: 80,
    },
    {
      field: 'backupPrice1',
      title: '备用价1',
      width: 100,
      formatter: 'formatAmount2',
    },
    {
      field: 'wholesalePrice',
      title: '批发价',
      width: 100,
      formatter: 'formatAmount2',
    },
  ];
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useSearchSchema(),
  },
  gridOptions: {
    columns: useColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getProductPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    checkboxConfig: {
      highlight: true,
      range: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<ErpProductApi.Product>,
  gridEvents: {
    checkboxChange: ({ records }: { records: ErpProductApi.Product[] }) => {
      selectedRows.value = records;
    },
    checkboxAll: ({ records }: { records: ErpProductApi.Product[] }) => {
      selectedRows.value = records;
    },
    cellClick: ({ row, column }: { row: ErpProductApi.Product; column: any }) => {
      // 点击 checkbox 列时 vxe-table 已自行处理勾选，跳过以避免双重 toggle 导致状态错乱
      if (column?.type === 'checkbox') {
        return;
      }
      const grid = gridApi.grid;
      grid.toggleCheckboxRow(row);
      selectedRows.value = grid.getCheckboxRecords();
    },
  },
});

function openModal() {
  open.value = true;
  selectedRows.value = [];
  gridApi.formApi?.resetForm();
  gridApi.query();
}

function handleOk() {
  if (selectedRows.value.length === 0) {
    message.warning('请至少勾选一条配件');
    return;
  }
  emit('success', [...selectedRows.value]);
  open.value = false;
}

defineExpose({ open: openModal });
</script>

<template>
  <Modal
    v-model:open="open"
    class="!w-[85vw]"
    title="选择配件"
    :ok-text="`确定（已选 ${selectedRows.length} 条）`"
    @ok="handleOk"
  >
    <Grid class="h-[70vh]" table-title="配件列表">
      <template #currentStock="{ row }">
        <span>{{ row.currentStock ?? 0 }}</span>
        <Tag v-if="row.lowStockWarning" color="warning" class="ml-1">
          低库存
        </Tag>
      </template>
    </Grid>
  </Modal>
</template>
