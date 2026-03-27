package com.eroticaforge.application.dto.api;

/**
 * 与《API 接口定义》第 4 节一致的错误响应体。
 *
 * @param code    错误码（如 400、404、409）
 * @param message 简短说明
 * @param error   具体原因（可含异常消息）
 * @author EroticaForge
 */
public record ApiErrorResponse(int code, String message, String error) {}
