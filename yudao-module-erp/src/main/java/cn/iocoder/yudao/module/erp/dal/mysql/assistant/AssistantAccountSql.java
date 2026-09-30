package cn.iocoder.yudao.module.erp.dal.mysql.assistant;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import java.util.*;

/** Account transaction evidence, paginated in SQL; never loads the legacy full detail list. */
public final class AssistantAccountSql {
    private AssistantAccountSql() {}
    public static String source(AssistantPlan plan) {
        boolean receivable=plan.getMetric()==AssistantPlan.Metric.RECEIVABLE;
        String party=receivable?"customer":"supplier", account=receivable?"receivable":"payable", cash=receivable?"receipt":"payment";
        String settlement=receivable?ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION:ErpPurchaseInMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION;
        List<String> rows=new ArrayList<>();
        rows.add(row(party,receivable?"sale_out":"purchase_in",receivable?"销售单":"采购入库",receivable?"out_time":"in_time",settlement,receivable?"sale_user_id":"creator",true,false));
        rows.add(row(party,receivable?"sale_return":"purchase_return",receivable?"销售退货":"采购退货","return_time","-t.total_price",receivable?"sale_user_id":"creator",true,false));
        rows.add(row(party,receivable?"sale_price_adjust":"purchase_price_adjust",receivable?"销售调价":"采购调价",receivable?"adjust_date":"adjust_time","t.total_adjust_price",receivable?"adjust_user_id":"creator",true,false));
        String cashAmount="-(CASE WHEN EXISTS(SELECT 1 FROM erp_"+account+"_other o WHERE o.source_id=t.id AND o.source_type='"+(receivable?"收款单优惠":"付款单折让")+"' AND o.status=20 AND o.deleted=0 AND o.tenant_id=t.tenant_id) THEN t."+cash+"_price ELSE t.total_price END)";
        rows.add(row(party,"finance_"+cash,receivable?"收款单":"付款单",cash+"_time",cashAmount,"finance_user_id",true,false));
        rows.add(row(party,account+"_other",receivable?"应收调账":"应付调账","biz_time","t."+account+"_amount",receivable?"handler_id":"creator",true,false));
        // Write-off is shown separately; it must not be mistaken for actual cash received/paid.
        rows.add(row(party,account+"_writeoff","核销","write_off_time","-t.write_off_amount",receivable?"operator_user_id":"creator",false,true));
        return String.join(" UNION ALL ",rows);
    }
    private static String row(String party,String table,String label,String date,String amount,String self,boolean approved,boolean writeoff) {
        String no=writeoff?"COALESCE(t.biz_no,CAST(t.id AS CHAR))":"t.no";
        return "SELECT CONCAT('"+table+"-',t.id) rowId,CONVERT("+no+" USING utf8mb4) COLLATE utf8mb4_unicode_ci name,'"+label+"' docType,'元' unit,"+amount+" amount,0 gross,0 refund,0 pending,1 documents,"
            +"p.id partyId,p.name partyName,t.dept_id deptId,d.name deptName,DATE(t."+date+") day,NULL productId,NULL productName,NULL warehouseId,NULL warehouseName "
            +"FROM erp_"+table+" t JOIN erp_"+party+" p ON p.id=t."+party+"_id AND p.deleted=0 AND p.tenant_id=t.tenant_id "
            +"LEFT JOIN system_dept d ON d.id=t.dept_id AND d.deleted=0 AND d.tenant_id=t.tenant_id "
            +"WHERE t.deleted=0 AND t.tenant_id=#{tenantId}"+(approved?" AND t.status=20":"")
            +AssistantSql.scope("t","document",self)+AssistantSql.scope("p","party",party.equals("customer")?"sale_user_id":"creator");
    }
}
