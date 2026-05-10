<script lang="ts" setup>
import { computed } from 'vue';

import { Textarea } from 'ant-design-vue';

import { MarkdownView } from '#/components/markdown-view';

const modelValue = defineModel<string>('modelValue', { default: '' });

const preview = computed(() => modelValue.value || '');
</script>

<template>
  <div class="mx-2 grid grid-cols-2 gap-4 py-2">
    <div>
      <div class="mb-2 text-sm text-gray-500">
        Markdown 源码（支持图片/视频链接，图片可通过 infra 文件上传后粘贴 URL）
      </div>
      <Textarea
        v-model:value="modelValue"
        :auto-size="{ minRows: 18, maxRows: 32 }"
        placeholder="# 商品介绍&#10;&#10;## 特性&#10;- 支持 Markdown&#10;- 可嵌入图片、视频&#10;&#10;![示例图](https://example.com/xxx.png)"
      />
    </div>
    <div>
      <div class="mb-2 text-sm text-gray-500">预览</div>
      <div
        class="min-h-[420px] rounded border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900"
      >
        <MarkdownView :content="preview" />
      </div>
    </div>
  </div>
</template>
