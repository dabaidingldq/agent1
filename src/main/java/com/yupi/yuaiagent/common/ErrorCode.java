package com.yupi.yuaiagent.common;

public interface ErrorCode {

    int SUCCESS = 0;

    int PARAMS_ERROR = 40000;

    int NO_AUTH_ERROR = 40100;

    int FORBIDDEN_ERROR = 40300;

    int NOT_FOUND_ERROR = 40400;

    int SYSTEM_ERROR = 50000;

    int OPERATION_ERROR = 50001;
}