package com.eroticaforge.domain;

/**
 * 乐观锁冲突：客户端提交的 {@code version} 与数据库当前行不一致，或目标行不存在。
 *
 * @author EroticaForge
 */
public final class OptimisticLockException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * @param message 异常说明（建议包含 storyId、期望版本等上下文）
     */
    public OptimisticLockException(String message) {
        super(message);
    }
}
