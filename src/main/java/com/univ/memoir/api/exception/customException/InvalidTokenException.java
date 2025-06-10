package com.univ.memoir.api.exception.customException;

import com.univ.memoir.api.exception.GlobalException;
import com.univ.memoir.api.exception.codes.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidTokenException extends GlobalException {
    public InvalidTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
