package com.itcall.modules.datasource.mybatis.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "${app.mybatis.mapper-package}")
public class MybatisConfig {

    private final MybatisProperties properties;

    public MybatisConfig(MybatisProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(properties.getMapperLocations());
        factoryBean.setMapperLocations(resources);

        if (properties.getConfigLocation() != null) {
            factoryBean.setConfigLocation(
                    new PathMatchingResourcePatternResolver().getResource(properties.getConfigLocation()));
        }

        // Setting simplified VFS if needed for jar scanning, usually boot starter
        // handles it but redundant check doesn't hurt.
        // But plain bean is enough.

        // Add configuration for snake_case to camelCase map if essential, or leave to
        // mybatis-config.xml if user provides one.
        // Assuming default, but let's enable mapUnderscoreToCamelCase programmatically
        // if no config xml is provided?
        // User didn't specify, but it's common practice. I will add a basic
        // configuration object if configLocation is null.
        if (properties.getConfigLocation() == null) {
            org.apache.ibatis.session.Configuration mybatisConfig = new org.apache.ibatis.session.Configuration();
            mybatisConfig.setMapUnderscoreToCamelCase(true);
            factoryBean.setConfiguration(mybatisConfig);
        }

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
