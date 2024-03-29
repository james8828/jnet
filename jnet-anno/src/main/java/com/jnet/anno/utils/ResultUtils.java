package com.jnet.anno.utils;

import com.jnet.anno.common.enums.ApiResultCode;
import com.jnet.anno.common.model.ApiResult;

public class ResultUtils {

    public static  ApiResult Success(Object obj){
        return new ApiResult(ApiResultCode.Success, "", obj);
    }

    public static ApiResult Error(ApiResultCode code, String message){
        return new ApiResult(code, message);
    }
}
