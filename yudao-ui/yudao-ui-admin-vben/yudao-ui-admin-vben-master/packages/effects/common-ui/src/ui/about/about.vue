<script setup lang="ts">
import type { AboutProps, DescriptionItem } from './about';

import { h } from 'vue';

import { VbenRenderContent } from '@vben-core/shadcn-ui';

import { Page } from '../../components';

interface Props extends AboutProps {}

defineOptions({
  name: 'AboutUI',
});

withDefaults(defineProps<Props>(), {
  description: '兴宇科技ERP后台管理系统。',
  name: '兴宇科技ERP',
  title: '关于系统',
});

declare global {
  const __VBEN_ADMIN_METADATA__: {
    buildTime: string;
    version: string;
  };
}

const { buildTime, version } = __VBEN_ADMIN_METADATA__ || {};

const systemDescriptionItems: DescriptionItem[] = [
  {
    content: version,
    title: '版本号',
  },
  {
    content: buildTime,
    title: '构建时间',
  },
  {
    content: h('span', '兴宇科技ERP'),
    title: '系统名称',
  },
];
</script>

<template>
  <Page :title="title">
    <template #description>
      <p class="mt-3 text-sm/6 text-foreground">
        {{ description }}
      </p>
    </template>
    <div class="card-box p-5">
      <div>
        <h5 class="text-lg text-foreground">基本信息</h5>
      </div>
      <div class="mt-4">
        <dl class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
          <template v-for="item in systemDescriptionItems" :key="item.title">
            <div class="border-t border-border px-4 py-6 sm:col-span-1 sm:px-0">
              <dt class="text-sm/6 font-medium text-foreground">
                {{ item.title }}
              </dt>
              <dd class="mt-1 text-sm/6 text-foreground sm:mt-2">
                <VbenRenderContent :content="item.content" />
              </dd>
            </div>
          </template>
        </dl>
      </div>
    </div>
  </Page>
</template>
