package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class H2EngineTest {

	static H2Engine engine;

	@BeforeAll
	public static void beforeAll() throws Exception {
        engine = H2Engine.getEngine();
        engine.start();
    }

	@AfterAll
	public static void afterAll() throws Exception {
        engine.clearDatabase(true);
        engine.stop();
    }

    @Test
	public void testStarStop() throws Exception {
		try{
			createTable();
			Thread.sleep(1000L);
		} catch( Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	private void createTable() throws Exception{
		Class.forName("org.h2.Driver");
		Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9001/h2","sa","");
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("create table bookmark (markid INTEGER,logid INTEGER,siteid INTEGER,userid INTEGER,marktype INTEGER,createTime DATE,markorder INTEGER);");
			ps.executeUpdate();
			ps.close();

			ps = conn.prepareStatement("SELECT * FROM bookmark");
			rs = ps.executeQuery();
			while(rs.next()){
				System.out.println("markid : " + rs.getInt("markid"));
			}
			ps.close();

			ps = conn.prepareStatement("drop table bookmark;");
			ps.executeUpdate();
			ps.close();
		} finally {
			if(rs!=null)
				rs.close();
			if(ps!=null)
				ps.close();
			if(conn!=null)
				conn.close();
		}
	}
}
