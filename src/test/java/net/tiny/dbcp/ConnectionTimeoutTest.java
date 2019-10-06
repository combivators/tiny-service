package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public class ConnectionTimeoutTest {

    private void printResultSet(ResultSet rs, PrintStream out) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = metaData.getColumnLabel(i);
            out.printf(label + "\t");
        }
        out.println();

        while (rs.next()) {
            for (int i = 1; i <= count; i++) {
                Object value = rs.getObject(i);
                out.printf(String.valueOf(value) + "\t");
            }
            out.println();
        }
    }



    //@Test
    @Disabled
    public void testTimeout() throws SQLException, InterruptedException {
        DataSource dataSource = mysql();
        Connection con = dataSource.getConnection();

        Statement statment = con.createStatement();
        ResultSet rs = statment.executeQuery("show global variables like 'wait_timeout'");
        printResultSet(rs, System.out);
        rs.close();

        rs = statment.executeQuery("show variables like 'wait_timeout'");
        printResultSet(rs, System.out);
        rs.close();

        statment.executeUpdate("set wait_timeout = 2");

        rs = statment.executeQuery("show variables like 'wait_timeout'");
        printResultSet(rs, System.out);
        rs.close();

        Thread.sleep(3000);
        // should timeout

        try {
            rs = statment.executeQuery("show variables like 'wait_timeout'");
            printResultSet(rs, System.out);
            rs.close();
            fail("should timeout");
        } catch (Exception ignore) {
            assertTrue(ignore instanceof SQLException);
        }
    }

    static DataSource mysql() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
            .driver("com.mysql.jdbc.Driver")
            .url("jdbc:mysql://devmysql001.com:3306/box_common?useSSL=false")
            .username("dev")
            .password("pcpfadmin")
            .properties("queryTimeoutKillsConnection=true;connectTimeout=20000;socketTimeout=600000");
        return ds;
    }
}
