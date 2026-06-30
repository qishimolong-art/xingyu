package cn.iocoder.yudao.module.erp.dal.mysql.base;

import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErpRecycleBinMapper {

    @Select("<script>"
            + "SELECT source_type AS sourceType, source_id AS sourceId, code, name, disabled_by AS disabledBy, disabled_time AS disabledTime "
            + "FROM ("
            + "  SELECT 'supplier' AS source_type, id AS source_id, code, name, disabled_by, COALESCE(disabled_time, update_time) AS disabled_time "
            + "  FROM erp_supplier WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'customer' AS source_type, id AS source_id, code, name, disabled_by, COALESCE(disabled_time, update_time) AS disabled_time "
            + "  FROM erp_customer WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'product' AS source_type, id AS source_id, code, name, disabled_by, COALESCE(disabled_time, update_time) AS disabled_time "
            + "  FROM erp_product WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'warehouse' AS source_type, id AS source_id, warehouse_code AS code, name, disabled_by, COALESCE(disabled_time, update_time) AS disabled_time "
            + "  FROM erp_warehouse WHERE deleted = b'0' AND status = 1 "
            + ") t "
            + "WHERE 1 = 1 "
            + "<if test='sourceType != null and sourceType != \"\"'> AND t.source_type = #{sourceType} </if>"
            + "<if test='name != null and name != \"\"'> AND t.name LIKE CONCAT('%', #{name}, '%') </if>"
            + "<if test='code != null and code != \"\"'> AND t.code LIKE CONCAT('%', #{code}, '%') </if>"
            + "ORDER BY t.disabled_time DESC, t.source_id DESC "
            + "LIMIT #{offset}, #{pageSize}"
            + "</script>")
    List<ErpRecycleBinRespVO> selectPage(@Param("sourceType") String sourceType,
                                         @Param("name") String name,
                                         @Param("code") String code,
                                         @Param("offset") Integer offset,
                                         @Param("pageSize") Integer pageSize);

    @Select("<script>"
            + "SELECT COUNT(1) FROM ("
            + "  SELECT 'supplier' AS source_type, code, name FROM erp_supplier WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'customer' AS source_type, code, name FROM erp_customer WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'product' AS source_type, code, name FROM erp_product WHERE deleted = b'0' AND status = 1 "
            + "  UNION ALL "
            + "  SELECT 'warehouse' AS source_type, warehouse_code AS code, name FROM erp_warehouse WHERE deleted = b'0' AND status = 1 "
            + ") t "
            + "WHERE 1 = 1 "
            + "<if test='sourceType != null and sourceType != \"\"'> AND t.source_type = #{sourceType} </if>"
            + "<if test='name != null and name != \"\"'> AND t.name LIKE CONCAT('%', #{name}, '%') </if>"
            + "<if test='code != null and code != \"\"'> AND t.code LIKE CONCAT('%', #{code}, '%') </if>"
            + "</script>")
    Long selectCount(@Param("sourceType") String sourceType,
                     @Param("name") String name,
                     @Param("code") String code);

}
