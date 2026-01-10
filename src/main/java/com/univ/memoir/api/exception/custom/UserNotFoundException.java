package com.univ.memoir.api.exception.custom;

import com.univ.memoir.api.exception.GlobalException;
import com.univ.memoir.api.exception.codes.ErrorCode;

import lombok.Getter;

@Getter
public class UserNotFoundException extends GlobalException{
    public UserNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}


