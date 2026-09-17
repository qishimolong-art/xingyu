package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 收款�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpFinanceReceiptMapper extends BaseMapperX<ErpFinanceReceiptDO> {

    default PageResult<ErpFinanceReceiptDO> selectPage(ErpFinanceReceiptPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpFinanceReceiptDO> query = new MPJLambdaWrapperX<ErpFinanceReceiptDO>()
                .likeIfPresent(ErpFinanceReceiptDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getReceiptTime())
                .eqIfPresent(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpFinanceReceiptDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpFinanceReceiptDO::getFinanceUserId, reqVO.getFinanceUserId())
                .eqIfPresent(ErpFinanceReceiptDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpFinanceReceiptDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpFinanceReceiptDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpFinanceReceiptDO::getRemark, reqVO.getRemark());
        if (reqVO.getBizNo() != null) {
            query.leftJoin(ErpFinanceReceiptItemDO.class, ErpFinanceReceiptItemDO::getReceiptId, ErpFinanceReceiptDO::getId)
                    .eq(reqVO.getBizNo() != null, ErpFinanceReceiptItemDO::getBizNo, reqVO.getBizNo())
                    .groupBy(ErpFinanceReceiptDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptNameAndSaleCustomer(query, reqVO.getKeyword(),
                ErpFinanceReceiptDO::getNo,
                ErpFinanceReceiptDO::getRemark);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpFinanceReceiptDO.class, query);
    }

    static void orderBy(MPJLambdaWrapperX<ErpFinanceReceiptDO> query, ErpFinanceReceiptPageReqVO reqVO) {
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
            case "receiptTime":
                return "t.receipt_time";
            case "customerName":
                return "(SELECT c.name FROM erp_customer c WHERE c.id = t.customer_id AND c.deleted = b'0')";
            case "settleMethod":
                return "(SELECT c.settle_method FROM erp_customer c WHERE c.id = t.customer_id AND c.deleted = b'0')";
            case "accountName":
                return "(SELECT a.name FROM erp_account a WHERE a.id = t.account_id AND a.deleted = b'0')";
            case "bankName":
                return "COALESCE(NULLIF((SELECT a.bank_name FROM erp_account a WHERE a.id = t.account_id AND a.deleted = b'0'), ''), "
                        + "(SELECT c.bank_name FROM erp_customer c WHERE c.id = t.customer_id AND c.deleted = b'0'))";
            case "totalPrice":
                return "t.total_price";
            case "discountPrice":
                return "t.discount_price";
            case "receiptPrice":
                return "t.receipt_price";
            case "status":
                return "t.status";
            case "remark":
                return "t.remark";
            case "financeUserName":
                return userNameExpression("finance_user_id");
            case "auditorName":
                return userNameExpression("updater");
            case "auditTime":
                return "t.update_time";
            default:
                return null;
        }
    }

    static String userNameExpression(String foreignKey) {
        return "(SELECT u.nickname FROM system_users u WHERE u.id = t." + foreignKey
                + " AND u.deleted = b'0')";
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpFinanceReceiptDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getId, id).eq(ErpFinanceReceiptDO::getStatus, status));
    }

    default ErpFinanceReceiptDO selectByNo(String no) {
        return selectOne(ErpFinanceReceiptDO::getNo, no);
    }

    default ErpFinanceReceiptDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getId, id)
                .last("FOR UPDATE"));
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpFinanceReceiptDO::getCustomerId, customerId);
    }

}
