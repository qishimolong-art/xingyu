package cn.iocoder.yudao.module.erp.framework.datapermission.config;

import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ERP data permission configuration.
 */
@Configuration(proxyBeanMethods = false)
public class ErpDataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer erpDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // Product management with dept_id.
            rule.addDeptColumn("erp_product", "dept_id");
            rule.addDeptColumn("erp_product_category", "dept_id");
            rule.addDeptColumn("erp_product_price_system", "dept_id");
            rule.addDeptColumn("erp_product_unit", "dept_id");
            rule.addDeptColumn("erp_product_universal", "dept_id");
            rule.addDeptColumn("erp_price_system", "dept_id");
            rule.addUserColumn("erp_product", "creator");
            rule.addUserColumn("erp_product_category", "creator");
            rule.addUserColumn("erp_product_price_system", "creator");
            rule.addUserColumn("erp_product_unit", "creator");
            rule.addUserColumn("erp_product_universal", "creator");
            rule.addUserColumn("erp_price_system", "creator");

            // Purchase documents with dept_id.
            rule.addDeptColumn("erp_purchase_in", "dept_id");
            rule.addDeptColumn("erp_purchase_invoice", "dept_id");
            rule.addDeptColumn("erp_purchase_order", "dept_id");
            rule.addDeptColumn("erp_purchase_price_adjust", "dept_id");
            rule.addDeptColumn("erp_purchase_return", "dept_id");
            rule.addDeptColumn("erp_supplier", "dept_id");
            rule.addUserColumn("erp_purchase_in", "creator");
            rule.addUserColumn("erp_purchase_suggestion", "creator");
            rule.addUserColumn("erp_supplier", "creator");
            rule.addUserColumn("erp_supplier_account", "creator");
            rule.addUserColumn("erp_supplier_bill", "creator");
            rule.addUserColumn("erp_supplier_business_info", "creator");
            rule.addUserColumn("erp_supplier_contact", "creator");
            rule.addUserColumn("erp_supplier_contract", "creator");
            rule.addUserColumn("erp_supplier_extend", "creator");
            rule.addUserColumn("erp_supplier_extend_info", "creator");
            rule.addUserColumn("erp_supplier_image", "creator");
            rule.addUserColumn("erp_supplier_task", "creator");
            rule.addUserColumn("erp_purchase_invoice", "handler_id");
            rule.addUserColumn("erp_purchase_order", "purchaser");
            rule.addUserColumn("erp_purchase_price_adjust", "adjuster");
            rule.addUserColumn("erp_purchase_return", "handler");

            // Sale documents with dept_id.
            rule.addDeptColumn("erp_customer", "dept_id");
            rule.addDeptColumn("erp_customer_contact", "dept_id");
            rule.addDeptColumn("erp_sale_cart", "dept_id");
            rule.addDeptColumn("erp_sale_order", "dept_id");
            rule.addDeptColumn("erp_sale_out", "dept_id");
            rule.addDeptColumn("erp_sale_price_adjust", "dept_id");
            rule.addDeptColumn("erp_sale_quote", "dept_id");
            rule.addDeptColumn("erp_sale_return", "dept_id");
            rule.addUserColumn("erp_customer", "sale_user_id");
            rule.addUserColumn("erp_customer_contact", "creator");
            rule.addUserColumn("erp_sale_cart", "sale_user_id");
            rule.addUserColumn("erp_sale_order", "sale_user_id");
            rule.addUserColumn("erp_sale_out", "sale_user_id");
            rule.addUserColumn("erp_sale_price_adjust", "adjust_user_id");
            rule.addUserColumn("erp_sale_quote", "sale_user_id");
            rule.addUserColumn("erp_sale_return", "sale_user_id");

            // Stock documents with dept_id.
            rule.addDeptColumn("erp_stock", "dept_id");
            rule.addDeptColumn("erp_stock_check", "dept_id");
            rule.addDeptColumn("erp_stock_in", "dept_id");
            rule.addDeptColumn("erp_stock_move", "dept_id");
            rule.addDeptColumn("erp_stock_out", "dept_id");
            rule.addDeptColumn("erp_stock_record", "dept_id");
            rule.addDeptColumn("erp_warehouse_move", "dept_id");
            rule.addDeptColumn("erp_warehouse", "dept_id");
            rule.addUserColumn("erp_stock", "creator");
            rule.addUserColumn("erp_stock_check", "creator");
            rule.addUserColumn("erp_stock_in", "creator");
            rule.addUserColumn("erp_stock_move", "creator");
            rule.addUserColumn("erp_stock_out", "creator");
            rule.addUserColumn("erp_stock_record", "creator");
            rule.addUserColumn("erp_warehouse_move", "creator");
            rule.addUserColumn("erp_warehouse", "creator");

            // Chain order documents with dept_id.
            rule.addDeptColumn("erp_chain_order", "dept_id");
            rule.addUserColumn("erp_chain_order", "creator");

            // Finance documents with dept_id.
            rule.addDeptColumn("erp_account", "dept_id");
            rule.addDeptColumn("erp_finance_payment", "dept_id");
            rule.addDeptColumn("erp_finance_receipt", "dept_id");
            rule.addDeptColumn("erp_finance_transfer", "dept_id");
            rule.addUserColumn("erp_account", "creator");
            rule.addUserColumn("erp_finance_payment", "finance_user_id");
            rule.addUserColumn("erp_finance_receipt", "finance_user_id");
            rule.addUserColumn("erp_finance_transfer", "finance_user_id");

            // Finance payable documents with dept_id.
            rule.addDeptColumn("erp_other_payable", "dept_id");
            rule.addDeptColumn("erp_payable_expense", "dept_id");
            rule.addDeptColumn("erp_payable_expense_item", "dept_id");
            rule.addDeptColumn("erp_payable_misc", "dept_id");
            rule.addDeptColumn("erp_payable_other", "dept_id");
            rule.addDeptColumn("erp_pre_payment", "dept_id");
            rule.addUserColumn("erp_other_payable", "creator");
            rule.addUserColumn("erp_payable_expense", "handler_id");
            rule.addUserColumn("erp_payable_expense_item", "handler_id");
            rule.addUserColumn("erp_payable_misc", "handler_id");
            rule.addUserColumn("erp_payable_other", "handler_id");
            rule.addUserColumn("erp_pre_payment", "creator");

            // Finance receivable documents with dept_id.
            rule.addDeptColumn("erp_other_receivable", "dept_id");
            rule.addDeptColumn("erp_pre_receipt", "dept_id");
            rule.addDeptColumn("erp_pre_receivable", "dept_id");
            rule.addDeptColumn("erp_receivable_misc", "dept_id");
            rule.addDeptColumn("erp_receivable_other", "dept_id");
            rule.addDeptColumn("erp_receivable_other_income", "dept_id");
            rule.addDeptColumn("erp_receivable_other_income_item", "dept_id");
            rule.addDeptColumn("erp_receivable_other_item", "dept_id");
            rule.addDeptColumn("erp_receivable_writeoff", "dept_id");
            rule.addDeptColumn("erp_payable_writeoff", "dept_id");
            rule.addUserColumn("erp_other_receivable", "creator");
            rule.addUserColumn("erp_pre_receipt", "creator");
            rule.addUserColumn("erp_pre_receivable", "creator");
            rule.addUserColumn("erp_receivable_misc", "handler_id");
            rule.addUserColumn("erp_receivable_other", "handler_id");
            rule.addUserColumn("erp_receivable_other_income", "handler_id");
            rule.addUserColumn("erp_receivable_other_income_item", "handler_id");
            rule.addUserColumn("erp_receivable_other_item", "handler_id");
            rule.addUserColumn("erp_receivable_writeoff", "operator_user_id");
            rule.addUserColumn("erp_payable_writeoff", "operator_user_id");

            // Finance accounting documents with dept_id.
            rule.addDeptColumn("erp_voucher", "dept_id");
            rule.addDeptColumn("erp_voucher_attribution", "dept_id");
            rule.addUserColumn("erp_voucher", "maker_user_id");
            rule.addUserColumn("erp_voucher_attribution", "handler_user_id");
        };
    }

}
