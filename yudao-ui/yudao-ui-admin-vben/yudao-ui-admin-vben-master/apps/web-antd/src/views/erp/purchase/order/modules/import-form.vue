<script lang="ts" setup>
import type { FileType } from 'ant-design-vue/es/upload/interface';

import { useVbenModal } from '@vben/common-ui';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, message, Upload } from 'ant-design-vue';

import {
  getPurchaseOrderImportTemplate,
  parsePurchaseOrderImportExcel,
} from '#/api/erp/purchase/order';
import { $t } from '#/locales';

const emit = defineEmits(['success']);

let uploadFile: File | null = null;

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!uploadFile) {
      message.warning('请先选择 Excel 文件');
      return;
    }
    modalApi.lock();
    try {
      const data = await parsePurchaseOrderImportExcel(uploadFile);
      await modalApi.close();
      emit('success', data);
      message.success('导入解析成功');
    } finally {
      modalApi.unlock();
    }
  },
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      uploadFile = null;
    }
  },
});

/** 上传前 */
function beforeUpload(file: FileType) {
  uploadFile = file as unknown as File;
  return false;
}

/** 下载模版 */
async function handleDownload() {
  const data = await getPurchaseOrderImportTemplate();
  downloadFileFromBlobPart({ fileName: '采购订单导入模板.xls', source: data });
}
</script>

<template>
  <Modal title="导入采购订单" class="w-1/3">
    <div class="mx-4 my-4">
      <p class="mb-4 text-gray-500">
        请上传 Excel 文件，系统将解析文件内容并自动填充订单主表信息。
      </p>
      <Upload
        :max-count="1"
        accept=".xls,.xlsx"
        :before-upload="beforeUpload"
      >
        <Button type="primary"> 选择 Excel 文件 </Button>
      </Upload>
    </div>
    <template #prepend-footer>
      <div class="flex flex-auto items-center">
        <Button @click="handleDownload"> 下载导入模板 </Button>
      </div>
    </template>
  </Modal>
</template>
