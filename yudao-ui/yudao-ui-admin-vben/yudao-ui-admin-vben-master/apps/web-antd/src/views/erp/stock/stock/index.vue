<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpStockApi } from '#/api/erp/stock/stock';

import { useRouter } from 'vue-router';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';

import { message } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { updateProductsShelf } from '#/api/erp/product/product';
import { exportStock, getStockPage } from '#/api/erp/stock/stock';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import StockAdjustModal from './modules/stock-adjust-form.vue';

/** 产品库存管理 */
defineOptions({ name: 'ErpStock' });

const router = useRouter();

/** 导出库存 */
async function handleExport() {
  const data = await exportStock(await gridApi.formApi.getValues());
  downloadFileFromBlobPart({ fileName: '产品库存.xls', source: data });
}

/** 库存调整弹窗 */
const [AdjustModal, adjustModalApi] = useVbenModal({
  connectedComponent: StockAdjustModal,
  destroyOnClose: true,
});

/** 打开库存调整弹窗 */
function handleAdjustStock(row: ErpStockApi.Stock) {
  adjustModalApi.setData(row).open();
}

/** 查看库存流水明细 */
function handleViewDetail(row: ErpStockApi.Stock) {
  router.push({
    path: '/erp/stock/record',
    query: {
      productId: row.productId,
      warehouseId: row.warehouseId,
    },
  });
}

/** 批量修改货架位 */
async function handleBatchUpdateShelf() {
  const rows = gridApi.grid.getCheckboxRecords() as ErpStockApi.Stock[];
  if (!rows || rows.length === 0) {
    message.warning('请先勾选要修改的库存行');
    return;
  }
  const productIds = [
    ...new Set(
      rows.map((r) => r.productId).filter((id): id is number => Boolean(id)),
    ),
  ];
  if (productIds.length === 0) {
    message.warning('选中行未包含有效的产品编号');
    return;
  }
  const shelf = window.prompt(
    `请输入新的货架位（共 ${productIds.length} 个产品）`,
    '',
  );
  if (shelf === null) {
    return;
  }
  const trimmed = shelf.trim();
  if (!trimmed) {
    message.warning('请输入货架位');
    return;
  }
  await updateProductsShelf(productIds, trimmed);
  message.success('修改成功');
  gridApi.query();
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    collapsed: true,
    collapsedRows: 2,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getStockPage({
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
    toolbarConfig: {
      refresh: true,
      search: true,
      custom: true,
    },
  } as VxeTableGridOptions<ErpStockApi.Stock>,
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert
        title="【库存】产品库存、库存明细"
        url="https://doc.iocoder.cn/erp/stock/"
      />
    </template>

    <AdjustModal @success="gridApi.query()" />
    <Grid table-title="产品库存列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '批量修改货架位',
              type: 'primary',
              auth: ['erp:product:update'],
              onClick: handleBatchUpdateShelf,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['erp:stock:export'],
              onClick: handleExport,
            },
          ]"
        />
      </template>
      <template #count="{ row }">
        <a
          class="text-primary cursor-pointer font-medium"
          @click="handleAdjustStock(row)"
        >
          {{ row.count ?? '-' }}
        </a>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看明细',
              type: 'link',
              onClick: () => handleViewDetail(row),
            },
            {
              label: '编辑库存',
              type: 'link',
              auth: ['erp:stock:update'],
              onClick: () => handleAdjustStock(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
