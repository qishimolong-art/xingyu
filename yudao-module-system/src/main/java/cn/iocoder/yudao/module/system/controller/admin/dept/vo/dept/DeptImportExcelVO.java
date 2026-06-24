package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部门 Excel 导入 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeptImportExcelVO {

    @ExcelProperty("部门名称")
    private String name;

    @ExcelProperty("上级部门名称")
    private String parentName;

    @ExcelProperty("显示顺序")
    private Integer sort;

    @ExcelProperty("负责人名称")
    private String leaderUserName;

    @ExcelProperty("状态")
    private String status;

}
