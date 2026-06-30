package cn.iocoder.yudao.module.system.api.permission.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldDefinitionCreateOrUpdateReqDTO implements Serializable {

    private String module;

    private String fieldKey;

    private String fieldLabel;

    private String fieldGroup;

    private Integer sort;

}
