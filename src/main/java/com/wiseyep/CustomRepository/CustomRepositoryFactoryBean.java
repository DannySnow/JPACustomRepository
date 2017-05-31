package com.wiseyep.CustomRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

/**
 * Created by Frozen on 2017/5/29.
 */
public class CustomRepositoryFactoryBean<R extends JpaRepository<T,I>,T,I extends Serializable> extends JpaRepositoryFactoryBean<R,T,I> {


    public CustomRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager em){
        return new CustomRepositoryFactory(em);
    }

    private static class CustomRepositoryFactory<T,I extends Serializable> extends JpaRepositoryFactory{
        private final EntityManager entityManager;

        public CustomRepositoryFactory(EntityManager entityManager){
            super(entityManager);
            this.entityManager = entityManager;
        }

        @Override
        protected Object getTargetRepository(RepositoryInformation information){
            return new CustomRepositoryImpl<T,I>((Class<T>) information.getDomainType(),entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata)
        {
            return CustomRepositoryImpl.class;
        }
    }
}
