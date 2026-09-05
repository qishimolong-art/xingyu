package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Optional one-time ops backfill for supplier/product mnemonic codes.
 */
@Component
@ConditionalOnProperty(prefix = "erp.mnemonic-code-backfill", name = "enabled", havingValue = "true")
@Slf4j
public class ErpMnemonicCodeBackfillRunner implements ApplicationRunner {

    @Resource
    private ErpSupplierMapper supplierMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        DataPermissionUtils.executeIgnore(() -> TenantUtils.executeIgnore(() -> {
            int supplierCount = backfillSuppliers();
            int productCount = backfillProducts();
            log.info("ERP mnemonic code backfill finished, suppliers={}, products={}", supplierCount, productCount);
        }));
    }

    private int backfillSuppliers() {
        List<ErpSupplierDO> suppliers = supplierMapper.selectList(new LambdaQueryWrapperX<ErpSupplierDO>()
                .select(ErpSupplierDO::getId, ErpSupplierDO::getName,
                        ErpSupplierDO::getPinyinCode, ErpSupplierDO::getWubiCode)
                .isNotNull(ErpSupplierDO::getName)
                .and(w -> w.isNull(ErpSupplierDO::getPinyinCode).or().eq(ErpSupplierDO::getPinyinCode, "")
                        .or().isNull(ErpSupplierDO::getWubiCode).or().eq(ErpSupplierDO::getWubiCode, "")));
        int count = 0;
        for (ErpSupplierDO supplier : suppliers) {
            boolean needPinyin = !StringUtils.hasText(supplier.getPinyinCode());
            boolean needWubi = !StringUtils.hasText(supplier.getWubiCode());
            if (!needPinyin && !needWubi) {
                continue;
            }
            LambdaUpdateWrapper<ErpSupplierDO> update = new LambdaUpdateWrapper<ErpSupplierDO>()
                    .eq(ErpSupplierDO::getId, supplier.getId());
            if (needPinyin) {
                update.set(ErpSupplierDO::getPinyinCode, ErpMnemonicCodeUtils.buildPinyinCode(supplier.getName()))
                        .and(w -> w.isNull(ErpSupplierDO::getPinyinCode).or().eq(ErpSupplierDO::getPinyinCode, ""));
            }
            if (needWubi) {
                update.set(ErpSupplierDO::getWubiCode, ErpMnemonicCodeUtils.buildWubiCode(supplier.getName()))
                        .and(w -> w.isNull(ErpSupplierDO::getWubiCode).or().eq(ErpSupplierDO::getWubiCode, ""));
            }
            count += supplierMapper.update(null, update);
        }
        return count;
    }

    private int backfillProducts() {
        List<ErpProductDO> products = productMapper.selectList(new LambdaQueryWrapperX<ErpProductDO>()
                .select(ErpProductDO::getId, ErpProductDO::getName,
                        ErpProductDO::getPinyinCode, ErpProductDO::getWubiCode)
                .isNotNull(ErpProductDO::getName)
                .and(w -> w.isNull(ErpProductDO::getPinyinCode).or().eq(ErpProductDO::getPinyinCode, "")
                        .or().isNull(ErpProductDO::getWubiCode).or().eq(ErpProductDO::getWubiCode, "")));
        int count = 0;
        for (ErpProductDO product : products) {
            boolean needPinyin = !StringUtils.hasText(product.getPinyinCode());
            boolean needWubi = !StringUtils.hasText(product.getWubiCode());
            if (!needPinyin && !needWubi) {
                continue;
            }
            LambdaUpdateWrapper<ErpProductDO> update = new LambdaUpdateWrapper<ErpProductDO>()
                    .eq(ErpProductDO::getId, product.getId());
            if (needPinyin) {
                update.set(ErpProductDO::getPinyinCode, ErpMnemonicCodeUtils.buildPinyinCode(product.getName()))
                        .and(w -> w.isNull(ErpProductDO::getPinyinCode).or().eq(ErpProductDO::getPinyinCode, ""));
            }
            if (needWubi) {
                update.set(ErpProductDO::getWubiCode, ErpMnemonicCodeUtils.buildWubiCode(product.getName()))
                        .and(w -> w.isNull(ErpProductDO::getWubiCode).or().eq(ErpProductDO::getWubiCode, ""));
            }
            count += productMapper.update(null, update);
        }
        return count;
    }

}
