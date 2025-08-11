package com.fit2cloud.autoconfigure;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan(basePackages = {"com.fit2cloud.dao.goodsMapper"}, sqlSessionFactoryRef = "secondSqlSessionFactory")
@EnableTransactionManagement
public class SecondDatabaseConfig {

    /* 数据源 */
    @Bean
    @ConfigurationProperties("goods.datasource")
    public DataSourceProperties thirdDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("goods.datasource.hikari")
    public HikariDataSource thirdDataSource(@Qualifier("thirdDataSourceProperties")DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }


    @Bean(name = "secondSqlSessionFactory")
    public SqlSessionFactory quartzSqlSessionFactory(@Qualifier("thirdDataSource") HikariDataSource secondDataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(secondDataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:com/fit2cloud/dao/goodsMapper/*.xml"));
        return bean.getObject();
    }

    @Bean
    public MybatisPlusInterceptor thirdMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));//分页
        return interceptor;
    }

}
