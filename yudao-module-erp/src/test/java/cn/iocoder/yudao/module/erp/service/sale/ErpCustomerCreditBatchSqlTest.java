package cn.iocoder.yudao.module.erp.service.sale;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerDebtDateBO;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ErpCustomerCreditBatchSqlTest {
 @Test void batchDebtDatesMatchReversePositiveEventConsumptionIncludingNullDatesAndUnrelatedCustomers() throws Exception {
  UnpooledDataSource data = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:customer_debt_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
  try (Connection connection=data.getConnection(); Statement s=connection.createStatement()) {
   s.execute("CREATE TABLE erp_sale_out(id bigint, customer_id bigint, status int, deleted int, out_time timestamp, total_price decimal(20,2))");
   s.execute("CREATE TABLE erp_sale_price_adjust(id bigint, customer_id bigint, status int, deleted int, adjust_date timestamp, total_adjust_price decimal(20,2))");
   s.execute("CREATE TABLE erp_receivable_other(id bigint, customer_id bigint, status int, deleted int, biz_time timestamp, receivable_amount decimal(20,2))");
   s.execute("INSERT INTO erp_sale_out VALUES(1,1,20,0,'2026-10-20',100),(2,1,20,0,'2026-10-10',100),(3,1,0,0,'2026-10-22',999),(4,1,20,1,'2026-10-23',999),(5,99,20,0,'2026-10-01',999999)");
   s.execute("INSERT INTO erp_sale_price_adjust VALUES(1,1,20,0,'2026-10-21',-50),(2,2,20,0,NULL,40)");
   s.execute("INSERT INTO erp_receivable_other VALUES(1,2,20,0,'2026-10-18',100),(2,3,20,0,'2026-10-01',10)");
  }
  Configuration config = new Configuration(new Environment("test",new JdbcTransactionFactory(),data)); config.addMapper(ErpCustomerMapper.class);
  try(SqlSession session=new SqlSessionFactoryBuilder().build(config).openSession()) {
   Map<Long,java.math.BigDecimal> balances = new LinkedHashMap<>();
   balances.put(1L,new java.math.BigDecimal("150")); balances.put(2L,new java.math.BigDecimal("50")); balances.put(3L,new java.math.BigDecimal("100"));
   List<ErpCustomerDebtDateBO> rows=session.getMapper(ErpCustomerMapper.class).selectEarliestUnpaidDates(balances);
   Map<Long,LocalDate> dates=new HashMap<>(); rows.forEach(row -> dates.put(row.getCustomerId(), row.getEarliestUnpaidDate()));
   assertEquals(3,dates.size());assertEquals(LocalDate.of(2026,10,10),dates.get(1L));assertEquals(LocalDate.of(2026,10,18),dates.get(2L));assertEquals(LocalDate.of(2026,10,1),dates.get(3L));assertFalse(dates.containsKey(99L));
   balances.clear();balances.put(2L,new java.math.BigDecimal("30"));
   assertNull(session.getMapper(ErpCustomerMapper.class).selectEarliestUnpaidDates(balances).get(0).getEarliestUnpaidDate());
  }
 }
}
