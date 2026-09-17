package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * 仅登记 Cart 到实际调出单的主键。父行串行化后按已知主键当前读，
 * 不以空来源范围锁保护后续 INSERT。调用方仍负责业务对象及部门权限。
 */
@Service
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class ErpSaleCartTransferLinkService {
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private ErpSaleCartMapper saleCartMapper;
    @Resource private ErpStockMoveMapper stockMoveMapper;

    /** 真正的新建入口；已有 Cart 不能通过这个方法补一个假空定位。 */
    public void insertCart(ErpSaleCartDO cart) {
        if (cart.getId() != null) {
            throw conflict("新手推车不得携带既有 ID");
        }
        if (saleCartMapper.insert(cart) != 1 || cart.getId() == null) {
            throw conflict("新手推车创建失败");
        }
        try {
            if (jdbcTemplate.update("INSERT INTO erp_sale_cart_transfer_link(tenant_id,cart_id,out_ids) VALUES(?,?,JSON_ARRAY())",
                    tenant(), cart.getId()) != 1) {
                throw conflict("新手推车调拨定位创建失败");
            }
        } catch (BadSqlGrammarException failure) {
            throw missingSchema();
        }
    }

    public List<ErpStockMoveDO> lockAndGet(Long cartId) {
        List<Long> ids = lockIds(cartId);
        List<ErpStockMoveDO> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ErpStockMoveDO move = stockMoveMapper.selectByIdForUpdate(id);
            validateIdentity(cartId, move);
            result.add(move);
        }
        return result;
    }

    public void append(Long cartId, Long moveId) {
        List<Long> ids = lockIds(cartId);
        ErpStockMoveDO move = stockMoveMapper.selectByIdForUpdate(moveId);
        validateIdentity(cartId, move);
        if (ids.contains(moveId)) {
            throw conflict("调出单已登记，不能重复追加");
        }
        ids.add(moveId);
        Collections.sort(ids);
        save(cartId, ids);
    }

    /** 在真实调出单软删除前移除，后续失败仍回滚整个事务。 */
    public void remove(Long cartId, Long moveId) {
        List<Long> ids = lockIds(cartId);
        validateIdentity(cartId, stockMoveMapper.selectByIdForUpdate(moveId));
        if (!ids.remove(moveId)) {
            throw conflict("待删除调出单未登记，请先核对");
        }
        save(cartId, ids);
    }

    private List<Long> lockIds(Long cartId) {
        if (cartId == null || saleCartMapper.selectByIdForUpdate(cartId) == null) {
            throw conflict("手推车不存在或不可访问");
        }
        List<String> rows;
        try {
            rows = jdbcTemplate.query("SELECT out_ids FROM erp_sale_cart_transfer_link WHERE tenant_id=? AND cart_id=? FOR UPDATE",
                    (rs, row) -> rs.getString(1), tenant(), cartId);
        } catch (BadSqlGrammarException failure) {
            throw missingSchema();
        }
        if (rows.size() != 1) {
            // 不在缺行锁后 upsert 空集合；历史关系只能通过维护窗口迁移核对。
            throw conflict("手推车缺少调拨定位，请先执行 00H 迁移或核对，不能按无调拨处理");
        }
        JsonNode values = JsonUtils.parseTree(rows.get(0));
        if (values == null || !values.isArray()) {
            throw conflict("手推车调拨定位格式损坏，请先核对");
        }
        TreeSet<Long> ids = new TreeSet<>();
        for (JsonNode value : values) {
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0
                    || !ids.add(value.longValue())) {
                throw conflict("手推车调拨定位包含无效或重复 ID，请先核对");
            }
        }
        return new ArrayList<>(ids);
    }

    private void validateIdentity(Long cartId, ErpStockMoveDO move) {
        if (move == null || !Integer.valueOf(30).equals(move.getSourceType()) || !Objects.equals(cartId, move.getSourceId())
                || (move.getTransferDirection() != null && !Integer.valueOf(10).equals(move.getTransferDirection()))) {
            throw conflict("手推车调拨定位与实际调出单不一致，请先核对");
        }
    }

    private void save(Long cartId, List<Long> ids) {
        if (jdbcTemplate.update("UPDATE erp_sale_cart_transfer_link SET out_ids=? WHERE tenant_id=? AND cart_id=?",
                JsonUtils.toJsonString(ids), tenant(), cartId) != 1) {
            throw conflict("手推车调拨定位更新失败");
        }
    }

    private Long tenant() {
        Long id = TenantContextHolder.getTenantId();
        if (id == null) {
            throw conflict("缺少租户上下文");
        }
        return id;
    }

    private ServiceException missingSchema() {
        return conflict("00H 手推车调拨定位尚未部署，请先执行关联迁移");
    }

    private ServiceException conflict(String message) {
        return new ServiceException(409, message);
    }
}
