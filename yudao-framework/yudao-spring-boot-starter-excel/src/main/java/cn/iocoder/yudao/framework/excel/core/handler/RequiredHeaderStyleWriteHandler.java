package cn.iocoder.yudao.framework.excel.core.handler;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 导入模板必填表头样式处理器。
 */
public class RequiredHeaderStyleWriteHandler implements CellWriteHandler {

    private static final String REQUIRED_HEADER_PREFIX = "* ";

    private final Set<String> requiredFieldNames;

    public RequiredHeaderStyleWriteHandler() {
        this(Collections.emptySet());
    }

    public RequiredHeaderStyleWriteHandler(Set<String> requiredFieldNames) {
        this.requiredFieldNames = requiredFieldNames != null ? requiredFieldNames : Collections.emptySet();
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                 Integer relativeRowIndex, Boolean isHead) {
        ExcelRequired required = getRequiredAnnotation(head);
        if (!Boolean.TRUE.equals(isHead) || !isRequired(head, required)) {
            return;
        }
        addRequiredHeaderPrefix(cell);
        addRequiredStyle(writeSheetHolder, cell);
        addRequiredComment(writeSheetHolder, cell, required != null ? required.value() : "\u5fc5\u586b");
    }

    private static ExcelRequired getRequiredAnnotation(Head head) {
        if (head == null || head.getField() == null) {
            return null;
        }
        return head.getField().getAnnotation(ExcelRequired.class);
    }

    private boolean isRequired(Head head, ExcelRequired required) {
        if (required != null) {
            return true;
        }
        return head != null && head.getField() != null && requiredFieldNames.contains(head.getField().getName());
    }

    private static void addRequiredHeaderPrefix(Cell cell) {
        if (cell.getCellType() != CellType.STRING) {
            return;
        }
        String value = cell.getStringCellValue();
        if (value == null || value.startsWith("*") || value.startsWith("\uff0a")) {
            return;
        }
        cell.setCellValue(REQUIRED_HEADER_PREFIX + value);
    }

    private static void addRequiredStyle(WriteSheetHolder writeSheetHolder, Cell cell) {
        Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(cell.getCellStyle());
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private static void addRequiredComment(WriteSheetHolder writeSheetHolder, Cell cell, String prompt) {
        if (cell.getCellComment() != null) {
            return;
        }
        CreationHelper helper = writeSheetHolder.getSheet().getWorkbook().getCreationHelper();
        Drawing<?> drawing = writeSheetHolder.getSheet().createDrawingPatriarch();
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 3);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(helper.createRichTextString(prompt));
        comment.setAuthor("system");
        cell.setCellComment(comment);
    }

}
