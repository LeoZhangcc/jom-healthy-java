package com.jom.healthy.validator;

/**
 * 用于参数校验的接口
 */
public interface BaseValidatingParam {

    /**
     * 校验请求参数是否为空
     */
    default String checkParam() {
        return null;
    }
}
