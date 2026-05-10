<script lang="ts" setup>
import { useVbenModal } from '@vben-core/popup-ui';

defineOptions({
  name: 'Help',
});

const [Modal] = useVbenModal({
  footer: false,
});
</script>

<template>
  <Modal class="w-1/3" title="帮助">
    <div></div>
  </Modal>
</template>
