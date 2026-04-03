package com.jom.healthy.exception.aop;

import com.jom.healthy.exception.ServiceException;
import com.jom.healthy.util.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author ZXJ
 * @Date 2021/4/9 14:14
 * @Version 1.0
 **/
@RestControllerAdvice
@Slf4j
public class ServiceExceptionHandler {


    @ExceptionHandler(ServiceException.class)
    public ResponseData ServiceExceptionHandler(ServiceException e) {
        log.error(e.getErrorMessage(),e);
        if (ObjectUtils.isEmpty(e.getCode())) {
            return ResponseData.error(e.getErrorMessage());
        }
        return ResponseData.error(e.getCode(), e.getErrorMessage());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseData ExceptionHandler(Exception e) {
//        log.error(e.getMessage(),e);
//        return ResponseData.error(e.getMessage());
//    }

}
