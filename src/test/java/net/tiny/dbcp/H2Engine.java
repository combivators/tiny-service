package net.tiny.dbcp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;

public class H2Engine {
    private final static Logger LOGGER  = Logger.getLogger(H2Engine.class.getName());

    public static final int    H2_PORT = 9001;
    public static final String H2_DIR = ".";
    public static final String H2_DB  = "h2";

    private static Server h2db;
    private static String[] startSqlScripts = new String[0];
    private static String[] stopSqlScripts  = new String[0];
    private static String databaseDir;
    private static String databaseName;

    private boolean clearDatabaseOnShutdown = false;

    private H2Engine() {
        try {
            Class.forName("org.h2.Driver");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * 获得一个数据库引擎的实例，需要一个数据路径的参数
     * 该参数必须为物理存在的路径
     * @param dataPath
     * @param port
     * @param dbn 数据库名
     * @param startScript
     * @param stopScript
     * @throws SQLException
     */
    public synchronized static H2Engine getEngine(String dataPath, int port, String dbn, String[] startScripts, String[] stopScripts)
            throws SQLException	{
        H2Engine engine = new H2Engine();
        String[] args = new String[]{"-tcpPort", Integer.toString(port), "-tcpAllowOthers", "-baseDir", dataPath, "-ifNotExists"};
        h2db = Server.createTcpServer(args);
        databaseDir = dataPath;
        databaseName = dbn;
        if(startScripts != null && startScripts.length>0) {
            startSqlScripts = startScripts;
        }
        if(stopScripts != null && stopScripts.length>0) {
            stopSqlScripts = stopScripts;
        }
        return engine;
    }

    /**
     * 获得一个数据库引擎的实例，需要一个数据路径的参数
     * 该参数必须为物理存在的路径
     * @param dataPath
     * @param port
     * @param dbn 数据库名
     * @param startScript
     * @param stopScript
     * @throws SQLException
     */
    public synchronized static H2Engine getEngine(String dataPath, int port, String dbn, String startScript, String stopScript)
            throws SQLException	{
        if(startScript != null && startScript.length()>0) {
            startSqlScripts = new String[] {startScript};
        }
        if(stopScript != null && stopScript.length()>0) {
            stopSqlScripts = new String[] {stopScript};
        }
        return getEngine(dataPath, port, dbn, startSqlScripts, stopSqlScripts);
    }

    public synchronized static H2Engine getEngine(String dataPath, int port, String dbn) throws SQLException {
        return getEngine(dataPath, port, dbn, (String)null, (String)null);
    }

    public synchronized static H2Engine getEngine(String[] startScripts, String[] stopScripts) throws SQLException {
        return getEngine(H2_DIR, H2_PORT, H2_DB, startScripts, stopScripts);
    }

    public synchronized static H2Engine getEngine(String dataPath) throws SQLException {
        return getEngine(dataPath, H2_PORT, H2_DB, (String)null, (String)null);
    }

    public synchronized static H2Engine getEngine() throws SQLException {
        return getEngine(H2_DIR, H2_PORT, H2_DB, (String)null, (String)null);
    }


    public void start() throws SQLException {
        //调用H2-DB的服务入口
        h2db.start();
        LOGGER.info("H2 database engine started. - " + getURL());
        if(startSqlScripts.length > 0) {
            try {
                runScript(startSqlScripts);
            }catch(SQLException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                throw  ex;
            }
        }
    }

    public void stop() throws SQLException {
        if(stopSqlScripts.length > 0) {
            try {
                runScript(stopSqlScripts);
            }catch(Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        h2db.stop();
        int i=0;
        while(i<10 && isRunning()){
            i++;
            try{
                Thread.sleep(500);
            }catch(Exception ex){}
        }
        LOGGER.info("H2 database engine stoped.");
        if(clearDatabaseOnShutdown) {
            DeleteDbFiles.execute(databaseDir, databaseName, true);
            LOGGER.info("H2 database file '" + databaseDir + "/" + databaseName +".db' was deleted.");
        }
    }

    public boolean isRunning(){
        try{
            return h2db.isRunning(true);
        }catch(RuntimeException  e){
            return false;
        }
    }

    public String getURL() {
        String url = "jdbc:h2:" + h2db.getURL() + "/" + databaseName;
        return url;
    }

    public int getPort() {
        return h2db.getPort();
    }

    public void clearDatabase(boolean flag) {
        clearDatabaseOnShutdown = flag;
    }

    public void runScript(String[] scripts) throws SQLException {
        String url = getURL();
        Connection conn = DriverManager.getConnection(url, "sa", "");
        if(conn != null) {
            LOGGER.info("Run script - " + url);
            for(int i=0; i<scripts.length; i++) {
                PreparedStatement ps = conn.prepareStatement(scripts[i]);
                ps.executeUpdate();
                ps.close();
            }
            conn.commit();
            conn.close();
        }
    }
}
