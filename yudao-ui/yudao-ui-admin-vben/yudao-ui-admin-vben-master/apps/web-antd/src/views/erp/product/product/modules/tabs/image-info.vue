<script lang="ts" setup>
import { ImageUpload } from '#/components/upload';

const modelValue = defineModel<string>('modelValue', { default: '' });
</script>

<template>
  <div class="mx-2 py-4">
    <ImageUpload
      v-model:model-value="modelValue"
      :max-number="1"
      :max-size="2"
      help-text="支持 jpg / png / gif，单张不超过 2MB，用于小程序主图"
    />
  </div>
</template>
