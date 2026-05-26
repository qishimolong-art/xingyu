package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpOpeningBalanceUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 会计科目 Service 接口
 *
 * @author Claude
 */
public interface ErpAccountingSubjectService {

    /**
     * 创建会计科目
     */
    Long createSubject(@Valid ErpAccountingSubjectSaveReqVO createReqVO);

    /**
     * 更新会计科目（科目编码不允许修改）
     */
    void updateSubject(@Valid ErpAccountingSubjectSaveReqVO updateReqVO);

    /**
     * 删除会计科目（存在子科目则不允许删除）
     */
    void deleteSubject(Long id);

    /**
     * 获得会计科目
     */
    ErpAccountingSubjectDO getSubject(Long id);

    /**
     * 校验会计科目存在
     */
    ErpAccountingSubjectDO validateSubject(Long id);

    /**
     * 按科目编码查询
     */
    ErpAccountingSubjectDO getSubjectByCode(String subjectCode);

    /**
     * 按编号集合查询
     */
    List<ErpAccountingSubjectDO> getSubjectList(Collection<Long> ids);

    /**
     * 按编号集合查询并转 Map
     */
    Map<Long, ErpAccountingSubjectDO> getSubjectMap(Collection<Long> ids);

    /**
     * 按大类返回科目列表（树形展示）；category=null 时返回全部
     */
    List<ErpAccountingSubjectRespVO> getSubjectTreeList(Integer category);

    /**
     * 返回所有科目的树形结构
     */
    List<ErpAccountingSubjectRespVO> getSubjectTree();

    /**
     * 精简列表（凭证录入下拉用）；leafOnly=true 仅末级科目
     */
    List<ErpAccountingSubjectDO> getSubjectSimpleList(Boolean leafOnly, Integer category);

    /**
     * 仅末级科目（凭证录入下拉用）
     */
    List<ErpAccountingSubjectDO> getLeafSubjectList();

    /**
     * 分页查询
     */
    PageResult<ErpAccountingSubjectDO> getSubjectPage(ErpAccountingSubjectPageReqVO pageReqVO);

    /**
     * 批量保存期初余额
     */
    void batchUpdateOpeningBalance(@Valid ErpOpeningBalanceUpdateReqVO reqVO);

    /**
     * 期初余额 Excel 导入；返回受影响科目编号集合（subjectCode → 是否成功）
     *
     * @return import map: subjectCode -> error message（成功为 null）
     */
    Map<String, String> importOpeningBalance(List<ErpAccountingSubjectImportExcelVO> list);

    /**
     * 完整科目导入（支持新增 + 重复处理）
     *
     * @param list   Excel 解析后的明细
     * @param mode   "DRY_RUN"=仅预检不写库；"OVERWRITE"=重复时覆盖；"SKIP"=重复时跳过；null 等同于 DRY_RUN
     * @return 导入结果
     */
    ErpAccountingSubjectImportRespVO importSubjects(List<ErpAccountingSubjectImportExcelVO> list, String mode);

}
