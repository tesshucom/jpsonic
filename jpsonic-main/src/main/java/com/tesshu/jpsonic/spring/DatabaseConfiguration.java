/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.spring;

import java.io.File;
import java.util.Map;

import javax.sql.DataSource;

import com.tesshu.jpsonic.dao.DaoHelper;
import com.tesshu.jpsonic.dao.GenericDaoHelper;
import com.tesshu.jpsonic.dao.LegacyHsqlDaoHelper;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.PlayerUtils;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    private final Environment environment;

    public static class ProfileNameConstants {

        public static final String LEGACY = "legacy";
        public static final String EMBED = "embed";
        public static final String JNDI = "jndi";

        private ProfileNameConstants() {
        }
    }

    public DatabaseConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @DependsOn("liquibase")
    public DaoHelper legacyDaoHelper(DataSource dataSource) {
        return environment.acceptsProfiles(Profiles.of(ProfileNameConstants.LEGACY))
                ? new LegacyHsqlDaoHelper(dataSource) : new GenericDaoHelper(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn("liquibase")
    public DaoHelper daoHelper(DataSource dataSource) {
        return new GenericDaoHelper(dataSource);
    }

    @Bean
    @Profile(ProfileNameConstants.LEGACY)
    public DataSource legacyDataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        dataSource.setUrl(SettingsService.getDefaultJDBCUrl());
        dataSource.setUsername(SettingsService.getDefaultJDBCUsername());
        dataSource.setPassword(SettingsService.getDefaultJDBCPassword());
        return dataSource;
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI") // Because it's spring API
    @Bean
    @Profile(ProfileNameConstants.EMBED)
    public DataSource embedDataSource(@Value("${DatabaseConfigEmbedDriver}") String driver,
            @Value("${DatabaseConfigEmbedUrl}") String url, @Value("${DatabaseConfigEmbedUsername}") String username,
            @Value("${DatabaseConfigEmbedPassword}") String password) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driver);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        return basicDataSource;
    }

    @Bean
    @Profile(ProfileNameConstants.JNDI)
    public DataSource jndiDataSource(@Value("${DatabaseConfigJNDIName}") String jndiName) {
        JndiDataSourceLookup jndiLookup = new JndiDataSourceLookup();
        return jndiLookup.getDataSource(jndiName);
    }

    @Bean
    public File rollbackFile() {
        return new File(SettingsService.getJpsonicHome(), "rollback.sql");
    }

    @Bean
    public String userTableQuote(@Value("${DatabaseUsertableQuote:}") String value) {
        return value;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource,
            @Value("${DatabaseMysqlMaxlength:512}") String mysqlVarcharLimit, String userTableQuote) {
        SpringLiquibase springLiquibase = new AirsonicSpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog("classpath:liquibase/db-changelog.xml");
        springLiquibase.setRollbackFile(rollbackFile());
        Map<String, String> parameters = LegacyMap.of("defaultMusicFolder", PlayerUtils.getDefaultMusicFolder(),
                "mysqlVarcharLimit", mysqlVarcharLimit, "userTableQuote", userTableQuote);
        springLiquibase.setChangeLogParameters(parameters);
        return springLiquibase;
    }

}
