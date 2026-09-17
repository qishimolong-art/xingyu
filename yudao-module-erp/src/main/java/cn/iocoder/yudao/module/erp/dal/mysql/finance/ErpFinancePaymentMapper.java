package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 付款�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpFinancePaymentMapper extends BaseMapperX<ErpFinancePaymentDO> {

    default PageResult<ErpFinancePaymentDO> selectPage(ErpFinancePaymentPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpFinancePaymentDO> query = new MPJLambdaWrapperX<ErpFinancePaymentDO>()
                .inIfPresent(ErpFinancePaymentDO::getId, reqVO.getIds())
                .likeIfPresent(ErpFinancePaymentDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getPaymentTime())
                .eqIfPresent(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpFinancePaymentDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpFinancePaymentDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinancePaymentDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpFinancePaymentDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpFinancePaymentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinancePaymentDO::getRemark, reqVO.getRemark());
        if (reqVO.getBizNo() != null) {
            query.leftJoin(ErpFinancePaymentItemDO.class, ErpFinancePaymentItemDO::getPaymentId, ErpFinancePaymentDO::getId)
                    .eq(reqVO.getBizNo() != null, ErpFinancePaymentItemDO::getBizNo, reqVO.getBizNo())
                    .groupBy(ErpFinancePaymentDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplier(query, reqVO.getKeyword(),
                ErpFinancePaymentDO::getNo,
                ErpFinancePaymentDO::getRemark);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpFinancePaymentDO.class, query);
    }

    static void orderBy(MPJLambdaWrapperX<ErpFinancePaymentDO> query, ErpFinancePaymentPageReqVO reqVO) {
        String expression = getOrderExpression(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (expression != null && (asc || desc)) {
            query.last("ORDER BY " + expression + (asc ? " ASC" : " DESC") + ", t.id DESC");
            return;
        }
        query.last("ORDER BY t.id DESC");
    }

    static String getOrderExpression(String field) {
        if (field == null) {
            return null;
        }
        switch (field.trim()) {
            case "no":
                return "t.no";
            case "paymentTime":
                return "t.payment_time";
            case "supplierName":
                return "(SELECT s.name FROM erp_supplier s WHERE s.id = t.supplier_id AND s.deleted = b'0')";
            case "settleMethod":
                return "(SELECT s.settle_method FROM erp_supplier s WHERE s.id = t.supplier_id AND s.deleted = b'0')";
            case "creatorName":
                return userNameExpression("creator");
            case "createTime":
                return "t.create_time";
            case "financeUserName":
                return userNameExpression("finance_user_id");
            case "deptName":
                return "(SELECT d.name FROM system_dept d WHERE d.id = t.dept_id AND d.deleted = b'0')";
            case "accountName":
                return "(SELECT a.name FROM erp_account a WHERE a.id = t.account_id AND a.deleted = b'0')";
            case "bankName":
                return "COALESCE(NULLIF((SELECT a.bank_name FROM erp_account a "
                        + "WHERE a.id = t.account_id AND a.deleted = b'0'), ''), "
                        + "(SELECT s.bank_name FROM erp_supplier s "
                        + "WHERE s.id = t.supplier_id AND s.deleted = b'0'))";
            case "totalPrice":
                return "t.total_price";
            case "discountPrice":
                return "t.discount_price";
            case "paymentPrice":
                return "t.payment_price";
            case "status":
                return "t.status";
            case "updaterName":
                return userNameExpression("updater");
            case "updateTime":
                return "t.update_time";
            case "auditorName":
                return "CASE WHEN t.status = 20 THEN " + userNameExpression("updater") + " ELSE NULL END";
            case "auditTime":
                return "CASE WHEN t.status = 20 THEN t.update_time ELSE NULL END";
            case "remark":
                return "t.remark";
            default:
                return null;
        }
    }

    static String userNameExpression(String foreignKey) {
        return "(SELECT u.nickname FROM system_users u WHERE u.id = t." + foreignKey
                + " AND u.deleted = b'0')";
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpFinancePaymentDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getId, id).eq(ErpFinancePaymentDO::getStatus, status));
    }

    default ErpFinancePaymentDO selectByNo(String no) {
        return selectOne(ErpFinancePaymentDO::getNo, no);
    }

    default ErpFinancePaymentDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getId, id)
                .last("FOR UPDATE"));
    }

}
