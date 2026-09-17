package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@RestController @RequestMapping("/erp/voucher-attribution")
@PreAuthorize("@ss.hasPermission('erp:voucher-attribution:generate')")
public class ErpVoucherGenerationController {
    @Resource private ErpVoucherRuleStore store;
    @Resource private ErpVoucherGenerationService generation;
    @GetMapping("/accounting-context")
    public CommonResult<Context> context(@RequestParam Integer bizType,@RequestParam Long bizId){return success(store.context(bizType,bizId));}
    @PutMapping("/accounting-context")
    public CommonResult<Boolean> save(@RequestBody Context c){store.saveContext(c);return success(true);}
    @PostMapping("/preview")
    public CommonResult<List<Preview>> preview(@RequestBody Batch b){return success(generation.preview(b));}
    @PostMapping("/generate-preview")
    public CommonResult<List<Long>> generate(@RequestBody Batch b){return success(generation.generate(b));}
}
