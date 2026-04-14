package com.yupi.yuaiagent.exception;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public BaseResponse<?> bizExceptionHandler(BizException e) {
        log.warn("biz exception: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.OPERATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("param invalid: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数不合法");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("illegal argument: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.PARAMS_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> exceptionHandler(Exception e) {
        log.error("system exception", e);
        return BaseResponse.error(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
    }
}