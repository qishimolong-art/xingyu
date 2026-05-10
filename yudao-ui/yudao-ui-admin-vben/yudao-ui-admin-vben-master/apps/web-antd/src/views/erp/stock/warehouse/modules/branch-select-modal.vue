<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Checkbox, message } from 'ant-design-vue';

import { requestClient } from '#/api/request';

const CheckboxGroup = Checkbox.Group;

const emit = defineEmits(['confirm']);

/** 分店列表 */
const branchList = ref<{ label: string; value: number }[]>([]);
/** 已选中的分店ID */
const checkedIds = ref<number[]>([]);

/** 获取租户列表作为分店数据 */
async function loadBranchList() {
  try {
    const data = await requestClient.get<any[]>('/system/tenant/simple-list');
    branchList.value = (data || []).map((item: any) => ({
      label: item.name,
      value: item.id,
    }));
  } catch {
    branchList.value = [];
  }
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    emit('confirm', checkedIds.value);
    await modalApi.close();
    message.success('分店选择已保存');
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    // 加载分店列表
    await loadBranchList();
    // 回显已选中的分店
    const data = modalApi.getData<number[]>();
    checkedIds.value = data || [];
  },
});
</script>

<template>
  <Modal title="分店查询" class="w-[600px]">
    <div class="p-4">
      <div v-if="branchList.length === 0" class="text-gray-400">
        暂无分店数据
      </div>
      <CheckboxGroup v-else v-model:value="checkedIds" class="w-full">
        <div class="grid grid-cols-2 gap-2">
          <Checkbox
            v-for="item in branchList"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </Checkbox>
        </div>
      </CheckboxGroup>
    </div>
  </Modal>
</template>
