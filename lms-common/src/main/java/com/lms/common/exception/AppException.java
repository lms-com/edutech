package com.lms.common.exception;

public class AppException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String errorMessage;

    public AppException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getMessage();
    }

    public AppException(ErrorCode errorCode, String errorMessage){
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ErrorCode getErrorCode(){
        return errorCode;
    }

    public String getErrorMessage(){
        return errorMessage;
    }
}
