<script lang="ts" setup>
import type {
  WorkbenchProjectItem,
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
} from '@vben/common-ui';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  AnalysisChartCard,
  WorkbenchHeader,
  WorkbenchProject,
  WorkbenchQuickNav,
  WorkbenchTodo,
  WorkbenchTrends,
} from '@vben/common-ui';
import { preferences } from '@vben/preferences';
import { useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

import AnalyticsVisitsSource from '../analytics/analytics-visits-source.vue';

const userStore = useUserStore();

// 这是一个示例数据，实际项目中需要根据实际情况进行调整
// url 也可以是内部路由，在 navTo 方法中识别处理，进行内部跳转
// 例如：url: /dashboard/workspace
const projectItems: WorkbenchProjectItem[] = [
  {
    color: '#6DB33F',
    content: '采购、入库、退货业务流程',
    date: '2025-01-02',
    group: '供应链管理',
    icon: 'lucide:shopping-cart',
    title: '采购管理',
    url: '/erp/purchase/order',
  },
  {
    color: '#409EFF',
    content: '销售订单、出库、退货管理',
    date: '2025-02-03',
    group: '销售管理',
    icon: 'lucide:chart-no-axes-combined',
    title: '销售管理',
    url: '/erp/sale/order',
  },
  {
    color: '#ff4d4f',
    content: '库存盘点、调拨、出入库记录',
    date: '2025-03-04',
    group: '仓储管理',
    icon: 'lucide:warehouse',
    title: '库存管理',
    url: '/erp/stock/stock',
  },
  {
    color: '#1890ff',
    content: '收款、付款、结算账户',
    date: '2025-04-05',
    group: '财务管理',
    icon: 'lucide:wallet-cards',
    title: '财务管理',
    url: '/erp/finance/account',
  },
  {
    color: '#e18525',
    content: '客户资料、往来记录、销售协同',
    date: '2025-05-06',
    group: '客户关系',
    icon: 'lucide:users-round',
    title: '客户管理',
    url: '/erp/sale/customer',
  },
  {
    color: '#2979ff',
    content: '产品资料、分类、单位维护',
    date: '2025-06-01',
    group: '基础资料',
    icon: 'lucide:package-search',
    title: '产品管理',
    url: '/erp/product/product',
  },
];

// 同样，这里的 url 也可以使用以 http 开头的外部链接
const quickNavItems: WorkbenchQuickNavItem[] = [
  {
    color: '#1fdaca',
    icon: 'ion:home-outline',
    title: '首页',
    url: '/',
  },
  {
    color: '#ff6b6b',
    icon: 'lucide:shopping-bag',
    title: '商城中心',
    url: '/mall',
  },
  {
    color: '#7c3aed',
    icon: 'tabler:ai',
    title: 'AI 大模型',
    url: '/ai',
  },
  {
    color: '#3fb27f',
    icon: 'simple-icons:erpnext',
    title: 'ERP 系统',
    url: '/erp',
  },
  {
    color: '#4daf1bc9',
    icon: 'simple-icons:civicrm',
    title: 'CRM 系统',
    url: '/crm',
  },
  {
    color: '#1a73e8',
    icon: 'fa-solid:hdd',
    title: 'IoT 物联网',
    url: '/iot',
  },
];

const todoItems = ref<WorkbenchTodoItem[]>([
  {
    completed: false,
    content: `核对采购入库单与供应商送货明细，确保数量和金额一致`,
    date: '2024-07-15 09:30:00',
    title: '采购入库核对',
  },
  {
    completed: false,
    content: `跟进本周销售订单出库进度，处理待发货单据`,
    date: '2024-08-30 14:20:00',
    title: '销售出库跟进',
  },
  {
    completed: false,
    content: `完成月度库存盘点，处理库存差异和调拨记录`,
    date: '2024-07-25 16:45:00',
    title: '库存盘点处理',
  },
  {
    completed: false,
    content: `复核收付款单据，更新客户与供应商往来余额`,
    date: '2024-07-10 11:15:00',
    title: '财务单据复核',
  },
]);
const trendItems: WorkbenchTrendItem[] = [
  {
    avatar: 'svg:avatar-1',
    content: `提交了 <a>采购订单</a> 的入库申请`,
    date: '刚刚',
    title: '采购专员',
  },
  {
    avatar: 'svg:avatar-2',
    content: `审核通过了 <a>销售出库单</a>`,
    date: '1个小时前',
    title: '仓库主管',
  },
  {
    avatar: 'svg:avatar-3',
    content: `更新了 <a>产品库存</a> 安全库存阈值`,
    date: '1天前',
    title: '库存管理员',
  },
  {
    avatar: 'svg:avatar-4',
    content: `新增了 <a>客户档案</a> 并分配跟进人`,
    date: '2天前',
    title: '销售经理',
  },
  {
    avatar: 'svg:avatar-1',
    content: `完成了 <a>收款单</a> 复核`,
    date: '3天前',
    title: '财务人员',
  },
  {
    avatar: 'svg:avatar-2',
    content: `归档了 <a>采购退货单</a>`,
    date: '1周前',
    title: '采购主管',
  },
  {
    avatar: 'svg:avatar-3',
    content: `完成了 <a>库存调拨</a> 审核`,
    date: '1周前',
    title: '运营主管',
  },
  {
    avatar: 'svg:avatar-4',
    content: `生成了 <a>销售统计报表</a>`,
    date: '2021-04-01 20:00',
    title: '系统管理员',
  },
  {
    avatar: 'svg:avatar-4',
    content: `维护了 <a>产品分类</a> 基础资料`,
    date: '2021-03-01 20:00',
    title: '资料管理员',
  },
];

const router = useRouter();

// 这是一个示例方法，实际项目中需要根据实际情况进行调整
// This is a sample method, adjust according to the actual project requirements
function navTo(nav: WorkbenchProjectItem | WorkbenchQuickNavItem) {
  if (nav.url?.startsWith('http')) {
    openWindow(nav.url);
    return;
  }
  if (nav.url?.startsWith('/')) {
    router.push(nav.url).catch((error) => {
      console.error('Navigation failed:', error);
    });
  } else {
    console.warn(`Unknown URL for navigation item: ${nav.title} -> ${nav.url}`);
  }
}
</script>

<template>
  <div class="p-5">
    <WorkbenchHeader
      :avatar="userStore.userInfo?.avatar || preferences.app.defaultAvatar"
    >
      <template #title>
        早安, {{ userStore.userInfo?.nickname }}, 开始您一天的工作吧！
      </template>
      <template #description> 今日晴，20℃ - 32℃！ </template>
    </WorkbenchHeader>

    <div class="mt-5 flex flex-col lg:flex-row">
      <div class="mr-4 w-full lg:w-3/5">
        <WorkbenchProject :items="projectItems" title="项目" @click="navTo" />
        <WorkbenchTrends :items="trendItems" class="mt-5" title="最新动态" />
      </div>
      <div class="w-full lg:w-2/5">
        <WorkbenchQuickNav
          :items="quickNavItems"
          class="mt-5 lg:mt-0"
          title="快捷导航"
          @click="navTo"
        />
        <WorkbenchTodo :items="todoItems" class="mt-5" title="待办事项" />
        <AnalysisChartCard class="mt-5" title="访问来源">
          <AnalyticsVisitsSource />
        </AnalysisChartCard>
      </div>
    </div>
  </div>
</template>
