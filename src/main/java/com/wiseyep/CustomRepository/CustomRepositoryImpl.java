package com.wiseyep.CustomRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Frozen on 2017/5/29.
 */
public class CustomRepositoryImpl<T,ID extends Serializable> extends SimpleJpaRepository<T,ID> implements CustomRepository<T,ID> {
    //封装一层 specification,加入方法字段,or,and
    private class MySpecification<T>{

        private String method;

        private Specification<T> specification;

        MySpecification(String method,Specification specification){
            this.method = method;
            this.specification = specification;
        }

        public String getMethod() {
            return method;
        }

        public Specification<T> getSpecification() {
            return specification;
        }
    }

    //方便外部类调用
    public class ConditionMethod{
        public final static String AND = "and";

        public final static String OR = "or";
    }

    private EntityManager entityManager;

    final String AND = "and";

    final String OR = "or";

    public CustomRepositoryImpl(Class<T> domainClass,EntityManager entityManager){
        super(domainClass,entityManager);
        this.entityManager = entityManager;
    }

    private List<String> ignoreKeyList = new ArrayList<>();

    private Integer pageNum;

    private Integer pageSize;

    private Map<String,Object> orderBy;

    private PageRequest page = null;

    private List<MySpecification<T>> specifications = new ArrayList<>();

    private ThreadLocal<List<MySpecification<T>>> threadLocal = new ThreadLocal<>();

    public List<MySpecification<T>> getSpecifications() {
        if (null == threadLocal.get())
        {
            threadLocal.set(new ArrayList<MySpecification<T>>());
        }
        return threadLocal.get();
    }

    public void setSpecifications(List<MySpecification<T>> specifications) {
        this.specifications = specifications;
    }

    @Override
    public CustomRepository findBySpecification() {
        return this;
    }

    @Override
    public CustomRepository findBySpecification(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        threadLocal.set(null);
        return this;
    }

    @Override
    public CustomRepository and(boolean condition, String key, Specification specification,String method) {
        if(condition) {
            getSpecifications().add(new MySpecification<T>(method,specification));
            this.ignoreKeyList.add(key);
        }
        return this;
    }

    @Override
    public CustomRepository and(boolean condition, String key, Specification specification) {
        return and(condition,key,specification,AND);
    }

    @Override
    public CustomRepository like(boolean condition, String key, Object value) {
        return like(condition,key,value,AND);
    }

    @Override
    public CustomRepository like(boolean condition, final String key, final Object value,String method) {
        if(condition) {
            getSpecifications().add(new MySpecification<T>(method,new Specification<T>() {
                @Override
                public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    Predicate predicate = cb.conjunction();
                    predicate.getExpressions().add(cb.like(root.<String>get(key),"%" + value.toString() + "%"));
                    return predicate;
                }
            }));
            this.ignoreKeyList.add(key);
        }
        return this;
    }

    @Override
    public CustomRepository isNull(boolean condition,final String key){
        return isNull(condition,key,AND);
    }

    @Override
    public CustomRepository isNull(boolean condition,final String key, String method) {
        if (condition)
        {
            getSpecifications().add(new MySpecification<T>(method, new Specification() {
                @Override
                public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                    Predicate predicate = cb.conjunction();
                    predicate.getExpressions().add(cb.isNull(root.get(key)));
                    return predicate;
                }
            }));
            this.ignoreKeyList.add(key);
        }
        return this;
    }

    @Override
    public CustomRepository isNotNull(boolean condition, final String key, String method) {
        if (condition)
        {
            getSpecifications().add(new MySpecification<T>(method, new Specification() {
                @Override
                public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                    Predicate predicate = cb.conjunction();
                    predicate.getExpressions().add(cb.isNotNull(root.get(key)));
                    return predicate;
                }
            }));
            this.ignoreKeyList.add(key);
        }
        return this;
    }

    @Override
    public CustomRepository isNotNull(boolean condition, final String key) {
        return isNotNull(condition,key,AND);
    }

    @Override
    public Page<T> build(){
        return build(null);
    }

    @Override
    public Page<T> build(final String method) {
        Specification<T> specification = new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                Predicate[] predicates = new Predicate[getSpecifications().size()];
                Predicate predicate = null;
                for(int i = 0; i < getSpecifications().size(); i++) {
                    MySpecification mySpecification = getSpecifications().get(i);
                    Specification spec = mySpecification.getSpecification();
                    if (null != spec)
                    {
                        if (spec.toPredicate(root,query,cb).getExpressions().size() != 0)
                        {
                            predicates[i] = spec.toPredicate(root, query, cb);
                            if (mySpecification.getMethod().equals(AND))
                            {
                                if (predicate == null)
                                {
                                    predicate = cb.and(cb.and(spec.toPredicate(root,query,cb)));
                                }
                                else
                                {
                                    predicate = cb.and(predicate,cb.and(spec.toPredicate(root,query,cb)));
                                }
                            }
                            else
                            {
                                if (predicate == null)
                                {
                                    predicate = cb.or(spec.toPredicate(root,query,cb));
                                }
                                else
                                {
                                    predicate = cb.or(predicate,cb.or(spec.toPredicate(root,query,cb)));
                                }
                            }
                        }
                    }
                }
                return predicate;
            }
        };
        Page<T> resultPage = null;
        try {
            resultPage = findBySpecification(specification);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            return resultPage;
        }
    }


    public Page<T> findBySpecification(Specification<T> specification) throws JsonProcessingException, IllegalAccessException, ClassNotFoundException {
        List<T> dataList = null;
        Page<T> resultList;
        PageRequest pageRequest = null;
        try {
            if (null == pageNum && null == pageSize)
            {
                dataList = this.findAll(specification);
            }
            else
            {
                //排序
                List<Sort.Order> orderList = new ArrayList<>();
                if (null != orderBy)
                {
                    for (Map.Entry<String,Object> entry : orderBy.entrySet())
                    {
                        if (null != entry.getValue())
                        {
                            if (entry.getValue().equals("正"))
                            {
                                orderList.add(new Sort.Order(Sort.Direction.ASC,entry.getKey()));
                            }
                            else
                            {
                                orderList.add(new Sort.Order(Sort.Direction.DESC,entry.getKey()));
                            }
                        }
                    }
                    if (orderList.size() == 0)
                    {
                        //默认排序
                        orderList.add(new Sort.Order("id"));
                    }
                    pageRequest = new PageRequest(pageNum - 1,pageSize,new Sort(orderList));
                }
                else
                {
                    pageRequest = page;
                }
                dataList = this.findAll(specification,pageRequest).getContent();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        resultList = pageRequest == null ? new PageImpl<T>(dataList) : new PageImpl<T>(dataList,pageRequest,this.findAll(specification).size());
        //重置结果
//        threadLocal.set(new ArrayList<MySpecification<T>>());
        return resultList;
    }

    @Override
    public CustomRepository whereEqual(final Map<String,Object> params)
    {
        for (Map.Entry<String,Object> entry : params.entrySet())
        {
            if (ignoreKeyList.contains(entry.getKey()) || null == entry.getValue())
            {
                continue;
            }
            equal(null != entry.getValue(),entry.getKey(),entry.getValue().toString());
        }
        return this;
    }

    @Override
    public CustomRepository equal(boolean condition, final String key, final String value)
    {
        return equal(condition,key,value,AND);
    }

    @Override
    public CustomRepository equal(boolean condition, final String key, final Object value, String method) {
        if (condition)
        {
            getSpecifications().add(new MySpecification<T>(method,new Specification<T>() {
                @Override
                public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    Predicate predicate = cb.conjunction();
                    predicate.getExpressions().add(cb.equal(root.<String>get(key), value));
                    return predicate;
                }
            }));
        }
        return this;
    }

    @Override
    public CustomRepository orderBy(final Map<String, Object> orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public CustomRepository orderBy(PageRequest pageRequest){
        this.page = pageRequest;
        return this;
    }
}
