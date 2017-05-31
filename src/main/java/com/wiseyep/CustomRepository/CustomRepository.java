package com.wiseyep.CustomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Frozen on 2017/5/29.
 */
@NoRepositoryBean
public interface CustomRepository<T,ID extends Serializable> extends JpaRepository<T,ID> {
    CustomRepository findBySpecification();

    CustomRepository findBySpecification(Integer pageNum, Integer pageSize);

    CustomRepository and(boolean condition, String key, Specification specification, String method);

    CustomRepository and(boolean condition, String key, Specification specification);

    CustomRepository like(boolean condition, final String key, final Object value,String method);

    CustomRepository like(boolean condition, final String key, final Object value);

    CustomRepository isNull(boolean condition,final String key);

    CustomRepository isNull(boolean condition,final String key,String method);

    CustomRepository isNotNull(boolean condition,final  String key);

    CustomRepository isNotNull(boolean condition,final  String key,String method);

    CustomRepository whereEqual(final Map<String,Object> params);

    CustomRepository equal(boolean condition,final String key,String value);

    CustomRepository equal(boolean condition, final String key, final Object value,String method);

    CustomRepository orderBy(final Map<String,Object> orderBy);

    CustomRepository orderBy(PageRequest pageRequest);

    Page<T> build(final String method);

    Page<T> build();
}
