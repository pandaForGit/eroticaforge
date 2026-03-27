package com.eroticaforge.application.dto.api;

/**
 * 与《API 接口定义》第 4 节一致的成功响应封装。
 *
 * @param code    业务/HTTP 语义码，成功时为 200
 * @param message 提示文案
 * @param data    载荷，可为 {@code null}
 * @param <T>     载荷类型
 * @author EroticaForge
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 构造成功响应（code=200，message=success）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 封装结果
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }
}
