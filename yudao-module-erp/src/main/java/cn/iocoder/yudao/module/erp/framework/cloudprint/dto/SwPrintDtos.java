package cn.iocoder.yudao.module.erp.framework.cloudprint.dto;

import lombok.Data;
import lombok.experimental.Accessors;

public final class SwPrintDtos {

    private SwPrintDtos() {
    }

    @Data
    public static class SwResponse<T> {
        private Integer code;
        private String message;
        private Boolean success;
        private T data;
    }

    @Data
    public static class PtFileData {
        private Boolean success;
        private String code;
        private String reqId;
        private String message;
    }

    @Data
    @Accessors(chain = true)
    public static class PtFileReq {
        private String devid;
        private String reqid;
        private Integer type;
        private Integer width;
        private Integer height;
        private Integer pcopy;
        private Integer ptype;
        private Integer rotate;
    }

    @Data
    public static class DeviceInfo {
        private String devid;
        private String devType;
        private String nickname;
        private String devicename;
        private String state;
        private Integer status;
        private Integer code;
        private String message;
        private Integer taskcnt;
        private Integer taskcount;
        private String version;
        private String lastdate;
        private String typename;
    }

}
