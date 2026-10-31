package ru.star.recommendation.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Две базы данных:
 * <ul>
 *     <li>основная ({@code spring.datasource.*}, PostgreSQL) — правила, JPA и Liquibase;</li>
 *     <li>база знаний о клиентах (H2, только чтение) — доступ только через JdbcTemplate.</li>
 * </ul>
 */
@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "recommendationsDataSource")
    public DataSource recommendationsDataSource(
            @Value("${application.recommendations-db.url}") String url,
            @Value("${application.recommendations-db.driver-class-name}") String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setReadOnly(true);
        return dataSource;
    }

    @Bean(name = "recommendationsJdbcTemplate")
    public JdbcTemplate recommendationsJdbcTemplate(
            @Qualifier("recommendationsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
