package cn.iocoder.yudao.module.erp.service.sale.bo;
@lombok.Data
public class ErpCustomerDebtDateBO {
    private Long customerId;
    private java.time.LocalDate earliestUnpaidDate;
}
