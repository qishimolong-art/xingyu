package cn.iocoder.yudao.module.system.api.permission.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldDefinitionRespDTO implements Serializable {

    private String fieldKey;

    private String fieldLabel;

    private Integer sort;

}
