package com.tuuzed.tunnel.webframework;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;


public class C3p0Test {
    @Before
    public void setUp() {
        Log4jInitializer.builder().initialize();
    }

    @Test
    public void testH2() throws PropertyVetoException, SQLException {

        ComboPooledDataSource dataSource = new ComboPooledDataSource();

        dataSource.setDriverClass("org.h2.Driver");
        // 内存   jdbc:h2:mem:DBName
        // 嵌入式  jdbc:h2:file:~/.h2/DBName
        dataSource.setJdbcUrl("jdbc:h2:mem:test");
        dataSource.setUser("pi");
        dataSource.setPassword("pi");
        dataSource.setInitialPoolSize(6);
        dataSource.setMaxPoolSize(30);
        dataSource.setMaxIdleTime(600);

        System.out.println(dataSource.getConnection());

    }

    @Test
    public void testMariaDB() throws PropertyVetoException, SQLException {

        ComboPooledDataSource dataSource = new ComboPooledDataSource();

        dataSource.setDriverClass("org.mariadb.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://192.168.43.86:3306/test");
        dataSource.setUser("pi");
        dataSource.setPassword("pi");
        dataSource.setInitialPoolSize(6);
        dataSource.setMaxPoolSize(30);
        dataSource.setMaxIdleTime(600);

        System.out.println(dataSource.getConnection());

    }

}
