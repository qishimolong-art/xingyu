package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Optional one-time ops backfill for customer/supplier/product mnemonic codes.
 */
@Component
@ConditionalOnProperty(prefix = "erp.mnemonic-code-backfill", name = "enabled", havingValue = "true")
@Slf4j
public class ErpMnemonicCodeBackfillRunner implements ApplicationRunner {

    @Resource
    private ErpCustomerMapper customerMapper;
    @Resource
    private ErpSupplierMapper supplierMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Value("${erp.mnemonic-code-backfill.rebuild-product-pinyin-code:false}")
    private boolean rebuildProductPinyinCode;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        DataPermissionUtils.executeIgnore(() -> TenantUtils.executeIgnore(() -> {
            int customerCount = backfillCustomers();
            int supplierCount = backfillSuppliers();
            int productCount = backfillProducts();
            log.info("ERP mnemonic code backfill finished, customers={}, suppliers={}, products={}",
                    customerCount, supplierCount, productCount);
        }));
    }

    private int backfillCustomers() {
        List<ErpCustomerDO> customers = customerMapper.selectList(new LambdaQueryWrapperX<ErpCustomerDO>()
                .select(ErpCustomerDO::getId, ErpCustomerDO::getName,
                        ErpCustomerDO::getPinyinCode, ErpCustomerDO::getWubiCode)
                .isNotNull(ErpCustomerDO::getName)
                .and(w -> w.isNull(ErpCustomerDO::getPinyinCode).or().eq(ErpCustomerDO::getPinyinCode, "")
                        .or().isNull(ErpCustomerDO::getWubiCode).or().eq(ErpCustomerDO::getWubiCode, "")));
        int count = 0;
        for (ErpCustomerDO customer : customers) {
            boolean needPinyin = !StringUtils.hasText(customer.getPinyinCode());
            boolean needWubi = !StringUtils.hasText(customer.getWubiCode());
            if (!needPinyin && !needWubi) {
                continue;
            }
            LambdaUpdateWrapper<ErpCustomerDO> update = new LambdaUpdateWrapper<ErpCustomerDO>()
                    .eq(ErpCustomerDO::getId, customer.getId());
            if (needPinyin) {
                update.set(ErpCustomerDO::getPinyinCode, ErpMnemonicCodeUtils.buildPinyinCode(customer.getName()))
                        .and(w -> w.isNull(ErpCustomerDO::getPinyinCode).or().eq(ErpCustomerDO::getPinyinCode, ""));
            }
            if (needWubi) {
                update.set(ErpCustomerDO::getWubiCode, ErpMnemonicCodeUtils.buildWubiCode(customer.getName()))
                        .and(w -> w.isNull(ErpCustomerDO::getWubiCode).or().eq(ErpCustomerDO::getWubiCode, ""));
            }
            count += customerMapper.update(null, update);
        }
        return count;
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
        LambdaQueryWrapperX<ErpProductDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.select(ErpProductDO::getId, ErpProductDO::getName,
                ErpProductDO::getPinyinCode, ErpProductDO::getWubiCode);
        queryWrapper.isNotNull(ErpProductDO::getName);
        if (!rebuildProductPinyinCode) {
            queryWrapper.and(w -> w.isNull(ErpProductDO::getPinyinCode).or().eq(ErpProductDO::getPinyinCode, "")
                    .or().isNull(ErpProductDO::getWubiCode).or().eq(ErpProductDO::getWubiCode, ""));
        }
        List<ErpProductDO> products = productMapper.selectList(queryWrapper);
        int count = 0;
        for (ErpProductDO product : products) {
            boolean rebuildPinyin = shouldRebuildProductPinyinCode(product);
            boolean needPinyin = !StringUtils.hasText(product.getPinyinCode()) || rebuildPinyin;
            boolean needWubi = !StringUtils.hasText(product.getWubiCode());
            if (!needPinyin && !needWubi) {
                continue;
            }
            LambdaUpdateWrapper<ErpProductDO> update = new LambdaUpdateWrapper<ErpProductDO>()
                    .eq(ErpProductDO::getId, product.getId());
            if (needPinyin) {
                update.set(ErpProductDO::getPinyinCode, ErpMnemonicCodeUtils.buildPinyinCode(product.getName()))
                        .and(w -> {
                            if (rebuildPinyin) {
                                w.eq(ErpProductDO::getPinyinCode, product.getPinyinCode());
                            } else {
                                w.isNull(ErpProductDO::getPinyinCode).or().eq(ErpProductDO::getPinyinCode, "");
                            }
                        });
            }
            if (needWubi) {
                update.set(ErpProductDO::getWubiCode, ErpMnemonicCodeUtils.buildWubiCode(product.getName()))
                        .and(w -> w.isNull(ErpProductDO::getWubiCode).or().eq(ErpProductDO::getWubiCode, ""));
            }
            count += productMapper.update(null, update);
        }
        return count;
    }

    private boolean shouldRebuildProductPinyinCode(ErpProductDO product) {
        if (!rebuildProductPinyinCode || !StringUtils.hasText(product.getPinyinCode())
                || !StringUtils.hasText(product.getName())) {
            return false;
        }
        String legacyCode = ErpMnemonicCodeUtils.buildLegacyPinyinCode(product.getName());
        String newCode = ErpMnemonicCodeUtils.buildPinyinCode(product.getName());
        return StringUtils.hasText(legacyCode)
                && !legacyCode.equals(newCode)
                && legacyCode.equals(product.getPinyinCode());
    }

}
