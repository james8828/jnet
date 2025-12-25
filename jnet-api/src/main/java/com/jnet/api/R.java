package com.jnet.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @author staitech
 */
@Schema(description = "响应信息")
public class R<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成功 */
    public static final int SUCCESS = 200;
    public static final String SUCCESS_DESC = "操作成功";

    /** 失败 */
    public static final int FAIL = 500;
    public static final String FAIL_DESC = "操作失败";

    /** 警告 */
    public static final int WARNING = 400;
    public static final String WARNING_DESC = "警告";
    @Schema(description = "返回码")
    private int code;
    @Schema(description = "返回信息")
    private String msg;
    @Schema(description = "返回数据")
    private T data;

    public static <T> R<T> success()
    {
        return restResult(null, SUCCESS, SUCCESS_DESC);
    }

    public static <T> R<T> success(T data)
    {
        return restResult(data, SUCCESS, SUCCESS_DESC);
    }

    public static <T> R<T> success(T data, String msg)
    {
        return restResult(data, SUCCESS, msg);
    }

    public static <T> R<T> fail()
    {
        return restResult(null, FAIL, FAIL_DESC);
    }

    public static <T> R<T> fail(String msg)
    {
        return restResult(null, FAIL, msg);
    }

    public static <T> R<T> fail(T data)
    {
        return restResult(data, FAIL, FAIL_DESC);
    }

    public static <T> R<T> fail(T data, String msg)
    {
        return restResult(data, FAIL, msg);
    }

    public static <T> R<T> fail(int code, String msg)
    {
        return restResult(null, code, msg);
    }

    public static <T> R<T> warn()
    {
        return restResult(null, WARNING, WARNING_DESC);
    }

    public static <T> R<T> warn(String msg)
    {
        return restResult(null, WARNING, msg);
    }

    public static <T> R<T> warn(T data)
    {
        return restResult(data, WARNING, WARNING_DESC);
    }

    public static <T> R<T> warn(T data, String msg)
    {
        return restResult(data, WARNING, msg);
    }

    public static <T> R<T> warn(int code, String msg)
    {
        return restResult(null, code, msg);
    }

    private static <T> R<T> restResult(T data, int code, String msg)
    {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public T getData()
    {
        return data;
    }

    public void setData(T data)
    {
        this.data = data;
    }
}
