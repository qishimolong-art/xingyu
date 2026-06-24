package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门 Excel 导出 VO
 */
@Data
@ExcelIgnoreUnannotated
public class DeptExportExcelVO {

    @ExcelProperty("部门编号")
    private Long id;

    @ExcelProperty("部门名称")
    private String name;

    @ExcelProperty("父部门")
    private String parentName;

    @ExcelProperty("显示顺序")
    private Integer sort;

    @ExcelProperty("负责人")
    private String leaderUserName;

    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
