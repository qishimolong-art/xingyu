<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpSaleQuoteApi } from '#/api/erp/sale/quote';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveSaleQuote,
  convertSaleQuoteToCart,
  deleteSaleQuote,
  getSaleQuote,
  getSaleQuotePage,
} from '#/api/erp/sale/quote';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'ErpSaleQuote' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ type: 'create' }).open();
}

function handleEdit(row: ErpSaleQuoteApi.SaleQuote) {
  formModalApi.setData({ type: 'edit', id: row.id }).open();
}

function handleDetail(row: ErpSaleQuoteApi.SaleQuote) {
  formModalApi.setData({ type: 'detail', id: row.id }).open();
}

async function handleDelete(ids: number[]) {
  const hideLoading = message.loading({ content: $t('ui.actionMessage.deleting'), duration: 0 });
  try {
    await deleteSaleQuote(ids);
    message.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleApprove(row: ErpSaleQuoteApi.SaleQuote) {
  await approveSaleQuote(row.id!);
  message.success('审核成功，已生成销售单');
  handleRefresh();
}

async function handleConvertCart(row: ErpSaleQuoteApi.SaleQuote) {
  const detail = await getSaleQuote(row.id!);
  const items = (detail.items || [])
    .map((item) => ({
      quoteItemId: item.id!,
      count: (item.count || 0) - (item.convertedCount || 0),
    }))
    .filter((item) => item.count > 0);
  if (items.length === 0) {
    message.warning('没有可转换的报价明细');
    return;
  }
  await convertSaleQuoteToCart({ quoteId: row.id!, items });
  message.success('已转为销售手推车');
  handleRefresh();
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: ErpSaleQuoteApi.SaleQuote[] }) {
  checkedIds.value = records.map((item) => item.id!);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSaleQuotePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<ErpSaleQuoteApi.SaleQuote>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="报价订单列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新增报价订单',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['erp:sale-quote:create'],
              onClick: handleCreate,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              disabled: isEmpty(checkedIds),
              icon: ACTION_ICON.DELETE,
              auth: ['erp:sale-quote:delete'],
              popConfirm: { title: '是否删除所选中数据？', confirm: handleDelete.bind(null, checkedIds) },
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, auth: ['erp:sale-quote:query'], onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, auth: ['erp:sale-quote:update'], ifShow: () => row.status !== 50, onClick: handleEdit.bind(null, row) },
            {
              label: '审核生成销售单',
              type: 'link',
              icon: ACTION_ICON.AUDIT,
              auth: ['erp:sale-quote:approve'],
              ifShow: () => row.status === 10,
              popConfirm: { title: `确认审核${row.no}并生成销售单吗？`, confirm: handleApprove.bind(null, row) },
            },
            {
              label: '转手推车',
              type: 'link',
              icon: ACTION_ICON.ADD,
              auth: ['erp:sale-quote:convert-cart'],
              ifShow: () => row.status === 10 || row.status === 30,
              popConfirm: { title: `确认将${row.no}剩余数量转为销售手推车吗？`, confirm: handleConvertCart.bind(null, row) },
            },
            {
              label: $t('common.delete'),
              type: 'link',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: ['erp:sale-quote:delete'],
              ifShow: () => row.status !== 50,
              popConfirm: { title: $t('ui.actionMessage.deleteConfirm', [row.no]), confirm: handleDelete.bind(null, [row.id!]) },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
