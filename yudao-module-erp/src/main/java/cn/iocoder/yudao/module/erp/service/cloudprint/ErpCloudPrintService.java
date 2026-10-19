package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDevicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintPreviewSaleOutRespVO;

import java.util.List;

public interface ErpCloudPrintService {

    default ErpCloudPrintPreviewSaleOutRespVO previewSaleOut(Long saleOutId, Integer copies, Long templateId) {
        return previewSaleOut(saleOutId, copies, templateId, null, null);
    }

    ErpCloudPrintPreviewSaleOutRespVO previewSaleOut(Long saleOutId, Integer copies, Long templateId,
                                                     String itemOrderField, String itemOrderDirection);

    byte[] getSaleOutPreviewFile(String previewToken, String previewItemId);

    List<ErpCloudPrintTaskRespVO> submitSaleOut(String previewToken);

    List<ErpCloudPrintTaskRespVO> getTaskListBySaleOut(Long saleOutId);

    List<ErpCloudPrintDeviceRespVO> getEnabledDevices();

    PageResult<ErpCloudPrintDeviceRespVO> getDevicePage(ErpCloudPrintDevicePageReqVO pageReqVO);

    ErpCloudPrintDeviceRespVO getDevice(Long id);

    Long createDevice(ErpCloudPrintDeviceSaveReqVO createReqVO);

    void updateDevice(ErpCloudPrintDeviceSaveReqVO updateReqVO);

    void deleteDevice(Long id);

    void updateDeviceStatus(Long id, Integer status);

    void setDefaultDevice(Long id);

    ErpCloudPrintDeviceRespVO refreshDeviceStatus(Long id);

    ErpCloudPrintDeviceRespVO resumeQueue(Long deviceId, String failedTaskAction);

}
