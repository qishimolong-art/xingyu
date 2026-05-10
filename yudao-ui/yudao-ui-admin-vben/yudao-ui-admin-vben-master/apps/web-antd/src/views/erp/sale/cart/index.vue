<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpSaleCartApi } from '#/api/erp/sale/cart';

import { ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  convertSaleCartToQuote,
  finalApproveSaleCart,
  firstApproveSaleCart,
  getSaleCartPage,
  submitSaleCart,
} from '#/api/erp/sale/cart';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

defineOptions({ name: 'ErpSaleCart' });

const [FormModal, formModalApi] = useVbenModal({ connectedComponent: Form, destroyOnClose: true });

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData({ type: 'create' }).open();
}

function handleEdit(row: ErpSaleCartApi.SaleCart) {
  formModalApi.setData({ type: 'edit', id: row.id }).open();
}

function handleDetail(row: ErpSaleCartApi.SaleCart) {
  formModalApi.setData({ type: 'detail', id: row.id }).open();
}

async function handleSubmit(row: ErpSaleCartApi.SaleCart) {
  await submitSaleCart(row.id!);
  message.success('提交成功');
  handleRefresh();
}

async function handleFirstApprove(row: ErpSaleCartApi.SaleCart) {
  await firstApproveSaleCart(row.id!);
  message.success('初审成功');
  handleRefresh();
}

async function handleFinalApprove(row: ErpSaleCartApi.SaleCart) {
  await finalApproveSaleCart(row.id!);
  message.success('终审成功，已生成销售单');
  handleRefresh();
}

async function handleConvertQuote(row: ErpSaleCartApi.SaleCart) {
  await convertSaleCartToQuote(row.id!);
  message.success('已转为报价订单');
  handleRefresh();
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({ records }: { records: ErpSaleCartApi.SaleCart[] }) {
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
          return await getSaleCartPage({ pageNo: page.currentPage, pageSize: page.pageSize, ...formValues });
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<ErpSaleCartApi.SaleCart>,
  gridEvents: { checkboxAll: handleRowCheckboxChange, checkboxChange: handleRowCheckboxChange },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="销售手推车列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            { label: '新增销售手推车', type: 'primary', icon: ACTION_ICON.ADD, auth: ['erp:sale-cart:create'], onClick: handleCreate },
            { label: '已选', type: 'primary', disabled: isEmpty(checkedIds), ifShow: false },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            { label: $t('common.detail'), type: 'link', icon: ACTION_ICON.VIEW, auth: ['erp:sale-cart:query'], onClick: handleDetail.bind(null, row) },
            { label: $t('common.edit'), type: 'link', icon: ACTION_ICON.EDIT, auth: ['erp:sale-cart:update'], ifShow: () => row.status === 10, onClick: handleEdit.bind(null, row) },
            { label: '提交', type: 'link', icon: ACTION_ICON.AUDIT, auth: ['erp:sale-cart:submit'], ifShow: () => row.status === 10, popConfirm: { title: `确认提交${row.no}吗？`, confirm: handleSubmit.bind(null, row) } },
            { label: '初审', type: 'link', icon: ACTION_ICON.AUDIT, auth: ['erp:sale-cart:first-approve'], ifShow: () => row.status === 20, popConfirm: { title: `确认初审${row.no}吗？`, confirm: handleFirstApprove.bind(null, row) } },
            { label: '终审', type: 'link', icon: ACTION_ICON.AUDIT, auth: ['erp:sale-cart:final-approve'], ifShow: () => row.status === 30, popConfirm: { title: `确认终审${row.no}并生成销售单吗？`, confirm: handleFinalApprove.bind(null, row) } },
            { label: '转报价', type: 'link', icon: ACTION_ICON.ADD, auth: ['erp:sale-cart:convert-quote'], ifShow: () => row.status === 10, popConfirm: { title: `确认将${row.no}转为报价订单吗？`, confirm: handleConvertQuote.bind(null, row) } },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
