package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Database(
    report = true,
    startScripts = {"create table bookmark (markid INTEGER,logid INTEGER,siteid INTEGER,userid INTEGER,marktype INTEGER,createTime DATE,markorder INTEGER);"},
    stopScripts = {"drop table bookmark;"})
public class ThreadLocalConnectionTest {

    static SimpleDataSource h2database() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:tcp://localhost:9001/h2")
          .username("sa")
          .password("");
        return ds;
    }

    @Test
    public void testSimpleDataSource() throws Exception {

        SimpleDataSource ds = h2database();
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
    public void testDatabaseQuery() throws Exception {
        SimpleDataSource ds = h2database();

        Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM bookmark");
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            System.out.println("markid : " + rs.getInt("markid"));
        }

        rs.close();
        ps.close();
        ds.close();
    }
}
