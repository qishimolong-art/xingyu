<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ErpStockRecordApi } from '#/api/erp/stock/record';

import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { DocAlert, Page } from '@vben/common-ui';
import { DICT_TYPE, getDictLabel } from '@vben/constants';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportStockRecordReport,
  getStockRecordReportPage,
  getStockRecordSummary,
} from '#/api/erp/stock/record';
import { $t } from '#/locales';

import { useReportGridColumns, useReportGridFormSchema } from './data';

/** 库存进出流水明细账（五期报表页） */
defineOptions({ name: 'ErpStockRecord' });

const route = useRoute();

/** 底部汇总 */
const summary = ref<ErpStockRecordApi.StockRecordSummary>({
  totalInCount: 0,
  totalInAmount: 0,
  totalOutCount: 0,
  totalOutAmount: 0,
  recordCount: 0,
});

/** 导出库存明细账 Excel */
async function handleExport() {
  const values = await gridApi.formApi.getValues();
  const data = await exportStockRecordReport(values);
  downloadFileFromBlobPart({
    fileName: '库存进出流水明细账.xls',
    source: data,
  });
}

/** 刷新汇总 */
async function refreshSummary() {
  const values = await gridApi.formApi.getValues();
  summary.value = await getStockRecordSummary(values);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useReportGridFormSchema(),
  },
  gridOptions: {
    columns: useReportGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const pageResult = await getStockRecordReportPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          // 异步刷新汇总（不阻塞分页）
          refreshSummary();
          return pageResult;
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
      zoom: true,
    },
  } as VxeTableGridOptions<ErpStockRecordApi.StockRecordReport>,
});

/** 格式化数字（空安全） */
function fmt(n?: number) {
  if (n === undefined || n === null) return '-';
  return Number(n).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  });
}

/** 接收从库存浏览页跳转过来的 query 参数，自动查询 */
onMounted(async () => {
  const { productId, warehouseId } = route.query as {
    productId?: string;
    warehouseId?: string;
  };
  if (productId || warehouseId) {
    await gridApi.formApi.setValues({
      productId: productId ? Number(productId) : undefined,
      warehouseId: warehouseId ? Number(warehouseId) : undefined,
    });
  }
  gridApi.query();
});
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert
        title="【库存】进出流水明细账"
        url="https://doc.iocoder.cn/erp/stock/"
      />
    </template>

    <Grid table-title="库存进出流水明细账">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: ['erp:stock-record:export'],
              onClick: handleExport,
            },
          ]"
        />
      </template>

      <!-- 交易类型字典标签 -->
      <template #bizType="{ row }">
        <Tag v-if="row.bizType !== undefined" :color="row.bizType % 10 === 0 ? 'blue' : 'default'">
          {{ getDictLabel(DICT_TYPE.ERP_STOCK_RECORD_BIZ_TYPE, row.bizType) }}
        </Tag>
        <span v-else>-</span>
      </template>
    </Grid>

    <!-- 底部汇总栏（客户文档 4.5） -->
    <div
      class="mt-2 flex flex-wrap items-center gap-4 rounded border border-border bg-muted p-3 text-sm"
    >
      <span class="font-medium text-foreground">汇总：</span>
      <span>
        总入库数：<span class="text-primary font-medium">
          {{ fmt(summary.totalInCount) }}
        </span>
      </span>
      <span>
        总入库金额：<span class="text-primary font-medium">
          {{ fmt(summary.totalInAmount) }}
        </span>
      </span>
      <span>
        总出库数：<span class="text-warning font-medium">
          {{ fmt(summary.totalOutCount) }}
        </span>
      </span>
      <span>
        总出库成本金额：<span class="text-warning font-medium">
          {{ fmt(summary.totalOutAmount) }}
        </span>
      </span>
      <span class="text-muted-foreground">
        记录数：{{ summary.recordCount ?? 0 }}
      </span>
    </div>
  </Page>
</template>
