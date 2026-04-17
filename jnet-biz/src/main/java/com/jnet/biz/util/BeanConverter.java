package com.jnet.biz.util;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean转换工具类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public class BeanConverter {

    /**
     * Entity转VO
     */
    public static <T, V> V toVO(T entity, Class<V> voClass) {
        if (entity == null) {
            return null;
        }
        try {
            V vo = voClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        } catch (Exception e) {
            throw new RuntimeException("Bean转换失败", e);
        }
    }

    /**
     * Entity列表转VO列表
     */
    public static <T, V> List<V> toVOList(List<T> entities, Class<V> voClass) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(entity -> toVO(entity, voClass))
                .collect(Collectors.toList());
    }

    /**
     * DTO转Entity
     */
    public static <D, T> T toEntity(D dto, Class<T> entityClass) {
        if (dto == null) {
            return null;
        }
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(dto, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Bean转换失败", e);
        }
    }
}
