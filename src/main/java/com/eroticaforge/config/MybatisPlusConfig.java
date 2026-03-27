package com.eroticaforge.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus：扫描 Mapper。
 *
 * <p>本配置类无自定义方法；{@link MapperScan#value()} 指定 Mapper 接口包路径。
 *
 * @author EroticaForge
 */
@Configuration
@MapperScan("com.eroticaforge.infrastructure.persistence.mapper")
public class MybatisPlusConfig {}
