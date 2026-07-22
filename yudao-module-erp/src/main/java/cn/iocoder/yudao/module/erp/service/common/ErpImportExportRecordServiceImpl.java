package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDetailDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportFailureDetailBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpImportExportRecordServiceImpl implements ErpImportExportRecordService {

    private static final int DETAIL_BATCH_SIZE = 500;

    @Resource
    private ErpImportExportRecordMapper recordMapper;
    @Resource
    private ErpImportExportRecordDetailMapper detailMapper;

    @Override
    public Long createRecord(ErpImportExportRecordCreateReqBO reqBO) {
        ErpImportExportRecordDO record = BeanUtils.toBean(reqBO, ErpImportExportRecordDO.class);
        record.setStatus(ErpImportExportRecordStatusEnum.PROCESSING.getStatus());
        record.setTotalCount(0);
        record.setSuccessCount(0);
        record.setFailureCount(0);
        record.setCreateCount(0);
        record.setUpdateCount(0);
        record.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        record.setOperatorName(SecurityFrameworkUtils.getLoginUserNickname());
        if (record.getStartTime() == null) {
            record.setStartTime(LocalDateTime.now());
        }
        recordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishRecord(Long recordId, ErpImportExportRecordFinishReqBO reqBO) {
        if (recordId == null) {
            return;
        }
        ErpImportExportRecordDO update = BeanUtils.toBean(reqBO, ErpImportExportRecordDO.class);
        update.setId(recordId);
        if (update.getEndTime() == null) {
            update.setEndTime(LocalDateTime.now());
        }
        if (update.getStatus() == null) {
            update.setStatus(ErpImportExportRecordStatusEnum.ofCount(update.getSuccessCount(), update.getFailureCount()));
        }
        recordMapper.updateById(update);
        insertFailureDetails(recordId, reqBO.getFailureDetails());
    }

    @Override
    public ErpImportExportRecordRespVO getRecord(Long id) {
        return BeanUtils.toBean(recordMapper.selectById(id), ErpImportExportRecordRespVO.class);
    }

    @Override
    public PageResult<ErpImportExportRecordRespVO> getRecordPage(ErpImportExportRecordPageReqVO reqVO) {
        return BeanUtils.toBean(recordMapper.selectPage(reqVO), ErpImportExportRecordRespVO.class);
    }

    @Override
    public PageResult<ErpImportExportRecordDetailRespVO> getDetailPage(ErpImportExportRecordDetailPageReqVO reqVO) {
        return BeanUtils.toBean(detailMapper.selectPage(reqVO), ErpImportExportRecordDetailRespVO.class);
    }

    private void insertFailureDetails(Long recordId, List<ErpImportExportFailureDetailBO> failureDetails) {
        if (CollUtil.isEmpty(failureDetails)) {
            return;
        }
        List<ErpImportExportRecordDetailDO> details = failureDetails.stream()
                .map(item -> ErpImportExportRecordDetailDO.builder()
                        .recordId(recordId)
                        .rowNo(item.getRowNo())
                        .bizKey(item.getBizKey())
                        .bizName(item.getBizName())
                        .failureReason(item.getFailureReason())
                        .rawData(item.getRawData() == null ? null : JsonUtils.toJsonString(item.getRawData()))
                        .build())
                .collect(Collectors.toList());
        detailMapper.insertBatch(details, DETAIL_BATCH_SIZE);
    }

}
