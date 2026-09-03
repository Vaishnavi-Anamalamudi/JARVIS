package com.adaptivegateway.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
            Set<String> dependencies = new LinkedHashSet<>();
            String[] existing = definition.getDependsOn();
            if (existing != null) {
                dependencies.addAll(Arrays.asList(existing));
            }
            dependencies.add("flyway");
            definition.setDependsOn(dependencies.toArray(String[]::new));
        };
    }
}
