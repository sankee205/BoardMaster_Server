/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mobileapp.configuration;


import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;
import static com.mycompany.mobileapp.configuration.DatasourceProducer.JNDI_NAME;


/**
 *
 * @author mikael
 */
@Singleton
@DataSourceDefinition(
    name = JNDI_NAME,
    className = "org.postgresql.ds.PGSimpleDataSource",
    url = "jdbc:postgresql://localhost/"
)


public class DatasourceProducer {
    public static final String JNDI_NAME =  "jdbc/mobileappPool";

    @Resource(lookup = JNDI_NAME)
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}