package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.vin.ErpVinRecognizeRespVO;

public interface ErpVinRecognizeService {

    ErpVinRecognizeRespVO recognize(String vin);

}
