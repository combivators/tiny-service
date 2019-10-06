package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Database validationQuery notes
 * Validation Query sql:
 * hsqldb - select 1 from INFORMATION_SCHEMA.SYSTEM_USERS
 * Oracle - select 1 from dual
 * DB2 - select 1 from sysibm.sysdummy1
 * mysql - select 1
 * microsoft SQL Server - select 1 (tested on SQL-Server 9.0, 10.5 [2008])
 * postgresql - select 1
 * ingres - select 1
 * derby - values 1
 * H2 - select 1
 * Firebird - select 1 from rdb$database
 *
 */
public class ConnectionTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    String mysqlSafe(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isHighSurrogate(ch) && !Character.isLowSurrogate(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    static interface OneIF {}
    static interface TwoIF extends Connection {}

    @Test
    public void testMySQLSafe() throws Exception {
        String str = "abc\uD83D\uDC4Adefg";
        System.out.println(mysqlSafe(str));
        assertEquals("abc👊defg", str);
    }

    //DB2 "values(current timestamp)"

    @Test
    public void testH2Embedded() throws Exception {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:~/test");

        //ds.setUsername("sa");
        //ds.setPassword("sa");
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        System.out.println(ds.toString());

        Statement stmt = conn.createStatement();
        assertNotNull(conn);

        ResultSet rset = stmt.executeQuery("SELECT 1");
        assertNotNull(rset);

        int numcols = rset.getMetaData().getColumnCount();
        assertEquals(1, numcols);
        assertTrue(rset.next());
        assertEquals("1", rset.getString(1));

        rset.close();
        stmt.close();
        conn.close();
        ds.close();
    }

    @Test
    public void testSQLLite() throws Exception {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.sqlite.JDBC")
          .url("jdbc:sqlite:sample.db");

        Connection conn = ds.getConnection();
        assertNotNull(conn);
        System.out.println(ds.toString());

        Statement stmt = conn.createStatement();
        assertNotNull(conn);

        ResultSet rset = stmt.executeQuery("SELECT 1");
        assertNotNull(rset);

        int numcols = rset.getMetaData().getColumnCount();
        assertEquals(1, numcols);
        assertTrue(rset.next());
        assertEquals("1", rset.getString(1));

        rset.close();
        stmt.close();
        conn.close();
        ds.close();
    }

    @Test
    public void testDerbyEmbedded() throws Exception {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.apache.derby.jdbc.EmbeddedDriver")
          .url("jdbc:derby:derby")
          .properties("create=true");

        Connection conn = ds.getConnection();
        assertNotNull(conn);
        System.out.println(ds.toString());

        Statement stmt = conn.createStatement();
        assertNotNull(conn);

        ResultSet rset = stmt.executeQuery("values 1");
        assertNotNull(rset);

        int numcols = rset.getMetaData().getColumnCount();
        assertEquals(1, numcols);
        assertTrue(rset.next());
        assertEquals("1", rset.getString(1));

        rset.close();
        stmt.close();
        conn.close();
        ds.close();
    }

    //@Test
    @Disabled
    public void testMySQL() throws Exception {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("com.mysql.jdbc.Driver")
          .url("jdbc:mysql://devmysql001.pf.com:3306/box_common?useSSL=false")
          .username("dev")
          .password("pcpfadmin")
          .properties("queryTimeoutKillsConnection=true;connectTimeout=20000;socketTimeout=600000");

        Connection conn = ds.getConnection();
        assertNotNull(conn);
        System.out.println(ds.toString());

        Statement stmt = conn.createStatement();
        assertNotNull(conn);

        ResultSet rset = stmt.executeQuery("SELECT 1");
        assertNotNull(rset);

        int numcols = rset.getMetaData().getColumnCount();
        assertEquals(1, numcols);
        assertTrue(rset.next());
        assertEquals("1", rset.getString(1));

        rset.close();
        stmt.close();
        conn.close();
        ds.close();
    }

    //@Test
    @Disabled
    public void testOracle() throws Exception {
        // mvn install:install-file -Dfile=ojdbc8-12.2.0.1.jar -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
        // mvn install:install-file -Dfile=ons-12.2.0.1.jar -DgroupId=com.oracle -DartifactId=ons -Dversion=12.2.0.1 -Dpackaging=jar
        // mvn install:install-file -Dfile=simplefan-12.2.0.1.jar -DgroupId=com.oracle -DartifactId=simplefan -Dversion=12.2.0.1 -Dpackaging=jar
        // mvn install:install-file -Dfile=ucp-12.2.0.1.jar -DgroupId=com.oracle -DartifactId=ucp -Dversion=12.2.0.1 -Dpackaging=jar

        final String ORACLE_URL_TEMPLATE = "jdbc:oracle:thin:@(DESCRIPTION=(ENABLE=BROKEN)(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST={0})(PORT={1,number,#})))(CONNECT_DATA=(SERVICE_NAME={2})(UR=A)(SERVER=DEDICATED)))";
        final String url = MessageFormat.format(ORACLE_URL_TEMPLATE, "cluster230.db.com", 50000, "sv_psmetad");
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("oracle.jdbc.driver.OracleDriver")
          .url(url)
          .username("ops_writer")
          .password("pcpfadmin")
          .properties("implicitCachingEnabled=true");

        Connection conn = ds.getConnection();
        assertNotNull(conn);
        System.out.println(ds.toString());

        Statement stmt = conn.createStatement();
        assertNotNull(conn);

        ResultSet rset = stmt.executeQuery("SELECT COUNT(*) FROM DUAL");
        assertNotNull(rset);

        int numcols = rset.getMetaData().getColumnCount();
        assertEquals(1, numcols);
        assertTrue(rset.next());
        assertEquals("1", rset.getString(1));

        rset.close();
        stmt.close();
        conn.close();
        ds.close();
    }
}
