package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
/** 调用方必须先校验源单访问权。来源去重不能受凭证制单人筛选影响；租户隔离仍生效。 */
@Component
public class ErpVoucherOriginLookup {
    @Resource private ErpVoucherMapper mapper;
    @DataPermission(enable=false)
    public List<ErpVoucherDO> find(Integer type,Long id){return mapper.selectListByBiz(type,id);}
}
