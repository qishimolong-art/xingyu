package cn.iocoder.yudao.module.erp.service.stock.cost;

import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/** Test-only execution of the actual DELIMITER migration; never fabricates a replacement trigger. */
public final class IsolatedStockCursorMigration {
    private IsolatedStockCursorMigration() { }
    public static void apply(JdbcTemplate jdbc, String schema) throws Exception {
        if (!schema.matches("report_(sale_return_current_cost|claim)_test")
                || !schema.equals(jdbc.queryForObject("SELECT DATABASE()", String.class))
                || !Integer.valueOf(33379).equals(jdbc.queryForObject("SELECT @@port", Integer.class))
                || !jdbc.queryForObject("SELECT @@datadir", String.class).replace('\\','/')
                    .contains("/Temp/xingyu-report-mysql-7286cc2f1e0148cb9bc6239a5c4e08d3/data")) {
            throw new IllegalStateException("Cursor migration is restricted to the named private test instance");
        }
        String source=Paths.get("../sql/mysql/erp_stock_record_cursor_20260909.sql").toAbsolutePath().normalize().toString().replace('\\','/');
        Process process=new ProcessBuilder("D:/MySQL8.4.11/bin/mysql.exe","--no-defaults","--protocol=TCP",
                "--host=127.0.0.1","--port=33379","--user=root","--default-character-set=utf8mb4",
                "--database="+schema,"--execute=source "+source).redirectErrorStream(true).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("Private cursor migration client timed out");}
        String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        if(process.exitValue()!=0)throw new IllegalStateException("Private cursor migration failed: "+output);
    }
}
