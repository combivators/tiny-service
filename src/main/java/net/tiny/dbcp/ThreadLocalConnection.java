package net.tiny.dbcp;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ThreadLocalConnection extends ThreadLocal<Connection> {

    private static final Logger LOGGER =  Logger.getLogger(ThreadLocalConnection.class.getSimpleName());

    interface Listener {
        void applied(String properties);
        void opened(String c);
        void cycled(String c);
        void died(String c);
        void broken(String c);
        void closed(String c);
        Logger getLogger();
    }

    private final String url;
    private final Properties connectionProperties;
    private final Map<WeakReference<Thread>, Connection> connections = new HashMap<WeakReference<Thread>, Connection>();
    private final ReferenceQueue<Thread> queue = new ReferenceQueue<Thread>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Listener listener;

    private ThreadLocalConnection(String url, String username, String password, String properties, Listener listener) {
        this.url = url;
        this.connectionProperties = parse(properties);
        if (username != null) {
            connectionProperties.setProperty("user", username);
        }
        if (password != null) {
            connectionProperties.setProperty("password", password);
        }
        this.listener = listener;
    }
    public Logger getLogger() {
        if (null != listener) {
            return LOGGER;
        }
        return null;
    }
    private Properties parse(String properties) {
        Properties prop = new Properties();
        if (null == properties || properties.isEmpty())
            return prop;
        final String[] values = properties.split(";");
        for (String value : values) {
            String[] kv = value.split("=");
            if (kv.length < 2) {
                continue;
            }
            prop.setProperty(kv[0], kv[1]);
        }
        if (null != listener) {
            listener.applied(properties);
        }
        return prop;
    }

    /**
     * close all connections
     */
    public void close() {
        SQLException ex = null;
        synchronized (connections) {
            for (Connection c : connections.values()) {
                try {
                    if (null != listener) {
                        listener.closed(c.toString());
                    }
                    c.close();
                } catch (SQLException e) {
                    ex = e;
                }
            }
            connections.clear();
        }

        if (ex != null) {
            throw new RuntimeException(String.format("Close connection error : '%s'", ex.getMessage()), ex);
        }
    }

    public String toString() {
        int active = 0;
        List<String> names;
        synchronized (connections) {
            active = connections.size();
            names = new ArrayList<String>(active);
            for (WeakReference<Thread> r : connections.keySet()) {
                Thread t = r.get();
                if (t != null) {
                    names.add(t.getName());
                } else {
                    names.add("null");
                }
            }
        }
        return "opened=" + counter.get() + ", active=" + active + ", threads=" + names;
    }

    private void closeDiedThreadConnection() {
        Reference<? extends Thread> r;
        while ((r = queue.poll()) != null) {
            synchronized (connections) { // does not run often
                Connection c = connections.get(r);
                connections.remove(r);
                try {
                    if (null != listener) {
                        listener.died(c.toString());
                    }
                    c.close();
                } catch (SQLException e) {
                    throw new RuntimeException(
                            String.format("Closed died thread connecton. Error: '%s'", e.getMessage()), e);
                }
            }
        }
    }

    private void closeBrokenConnections() {
        synchronized (connections) {
            Iterator<Connection> ite = connections.values().iterator();
            while (ite.hasNext()) {
                Connection con = ite.next();
                try {
                    Statement stat = con.createStatement();
                    stat.executeQuery("select 1").close();
                    stat.close();
                } catch (Exception e1) {
                    ite.remove();
                    try {
                        if (null != listener) {
                            listener.broken(con.toString());
                        }
                        con.close(); // broken connection, close it
                    } catch (SQLException ignore) {
                    }
                }
            }
        }
    }

    public Connection get() {
        closeDiedThreadConnection();
        Connection con = super.get();
        try {
            if (con.isClosed()) { // maybe server restarted
                closeBrokenConnections();
                remove(); // for recreate one
                return super.get(); // try to create a new one
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error '%s' when asking isClosed.", e.getMessage()), e);
        }
        if (null != listener) {
            listener.cycled(con.toString());
        }
        return con;
    }

    @Override
    protected Connection initialValue() {
        counter.incrementAndGet();
        try {
            Connection con = DriverManager.getConnection(url, connectionProperties);
            /*
            final String product = con.getMetaData().getDatabaseProductName();
            if (product.toLowerCase().contains("mysql")) {
                Statement stat = con.createStatement();
                ResultSet rs = stat.executeQuery("show variables like 'wait_timeout'");
                if (rs.next()) {
                    int timeout = rs.getInt(2);
                    if (timeout == 3600 * 8) { //
                        // server close idle connection, 8 hours => 3 days
                        stat.executeUpdate("set wait_timeout = 259200");
                    }
                }
                rs.close();
                stat.close();
            }
            */
            synchronized (connections) {
                connections.put(new WeakReference<Thread>(Thread.currentThread(), queue), con);
            }
            if (null != listener) {
                listener.opened(con.toString());
            }
            //return new NoCloseConnection(con);
            return (new _Connection(con, false) {
                @Override
                protected void close() throws SQLException {
                    // No close connection do nothing.
                }
            }).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Connect to '%s' error : %s", url, e.getMessage()), e);
        }
    }

    /**
     * 接管数据库连接，防止由于某些数据库不支持事务处理而抛出的异常
     */
    static abstract class _Connection implements InvocationHandler {
        //private static final Class<?>[] IC = new Class[] { Connection.class };
        private static final String PREPAREDSTATEMENT = "prepareStatement";
        private static final String CREATESTATEMENT = "createStatement";
        private static final String M_SETAUTOCOMMIT = "setAutoCommit";
        private static final String M_COMMIT = "commit";
        private static final String M_ROLLBACK = "rollback";

        static boolean isAssignableConnection(Class<?> type) {
            boolean is = type.isAssignableFrom(Connection.class);
            if (is) return true;
            Class<?>[] ifs = type.getInterfaces();
            if(null == ifs) return false;
            for (Class<?> i : ifs) {
                Class<?> superClass = i.getSuperclass();
                if (null != superClass) {
                    is = isAssignableConnection(superClass);
                    if (is) return true;
                }
            }
            return false;
        }


        private Connection real;
        private Object proxy;
        private boolean supportTransaction;
        private boolean coding;

        public _Connection(Connection conn, boolean coding) {
            this.real = conn;
            this.coding = coding;
            DatabaseMetaData dm = null;
            try {
                dm = conn.getMetaData();
                supportTransaction = dm.supportsTransactions();
                Class<?>[] ifs = real.getClass().getInterfaces();
                if (ifs == null || ifs.length == 0)
                    ifs = new Class[] { Connection.class };
                proxy = tryCastProxy(ifs);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        private Object tryCastProxy(Class<?>[] ifs) {
            Object p = Proxy.newProxyInstance(real.getClass().getClassLoader(), ifs, this);
            try {
                Connection.class.cast(p);
            } catch (ClassCastException e) {
                // Solved issue of 'oracle.jdbc.driver.T4CConnection Cast error'
                Class<?>[] cs = new Class<?>[ifs.length + 1];
                cs[0] = Connection.class;
                System.arraycopy(ifs, 0, cs, 1, ifs.length);
                p = Proxy.newProxyInstance(real.getClass().getClassLoader(), cs, this);
            }
            return p;
        }

        /**
         * 获取对象的代理
         *
         * @return
         */
        public Connection getConnection() {
            return (Connection)proxy;
        }

        void close() throws SQLException {
            real.close();
        }

        public Object invoke(Object proxy, Method m, Object args[]) throws Throwable {
            String method = m.getName();
            if ((M_SETAUTOCOMMIT.equals(method) || M_COMMIT.equals(method) || M_ROLLBACK.equals(method))
                    && !isSupportTransaction())
                return null;
            Object obj = null;
            try {
                if ("close".equals(m.getName())) {
                    close();
                } else {
                    obj = m.invoke(real, args);
                    if (CREATESTATEMENT.equals(method) || PREPAREDSTATEMENT.equals(method))
                        return (new _Statement((Statement)obj, coding) {}).getStatement();
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return obj;
        }

        public boolean isSupportTransaction() {
            return supportTransaction;
        }
    }

    /**
     * 数据库语句对象的代理
     */
    static abstract class _Statement implements InvocationHandler {
        private static final Class<?>[] IPS = new Class[] { PreparedStatement.class };
        private static final Class<?>[] IS = new Class[] { Statement.class };
        private static final String GETQUERYTIMEOUT = "getQueryTimeout";
        private static final String SETSTRING = "setString";
        private static final String EXECUTEQUERY = "executeQuery";
        private static final String GETRESULTSET = "getResultSet";

        private Statement statement;
        private boolean decode;
        private Class<?>[] infs;
        private Class<?>[] infs2;

        public _Statement(Statement stmt, boolean decode) {
            statement = stmt;
            this.decode = decode;
        }

        /**
         * 获取Statement实例的代理
         *
         * @return
         */
        public Statement getStatement() {
            if (statement instanceof PreparedStatement) {
                infs2 = statement.getClass().getInterfaces();
                if (infs2 == null || infs2.length == 0)
                    infs2 = IPS;
                return (Statement) Proxy.newProxyInstance(statement.getClass().getClassLoader(), infs2, this);
            } else {
                infs = statement.getClass().getInterfaces();
                if (infs == null || infs.length == 0)
                    infs = IS;
                return (Statement) Proxy.newProxyInstance(statement.getClass().getClassLoader(), infs, this);
            }
        }

        public Object invoke(Object proxy, Method m, Object args[]) throws Throwable {
            String method = m.getName();

            if (decode && SETSTRING.equals(method)) {
                try {
                    String param = (String) args[1];
                    return m.invoke(statement, new Object[] { args[0], param });
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            if (decode && (EXECUTEQUERY.equals(method) || GETRESULTSET.equals(method))) {
                try {
                    ResultSet rs = (ResultSet) m.invoke(statement, args);
                    return (new _ResultSet(rs, decode) {}).getResultSet();
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            try {
                return m.invoke(statement, args);
            } catch (InvocationTargetException e) {
                if (GETQUERYTIMEOUT.equals(method))
                    return Integer.valueOf(0);
                else
                    throw e.getTargetException();
            }
        }

    }

    /**
     * 结果集合的接管，用于处理字符集的转码
     */
    static abstract class _ResultSet implements InvocationHandler {

        private static final Class<?>[] IRS = new Class[] { ResultSet.class };
        private static final String GETSTRING = "getString";

        private ResultSet rs;
        private boolean decode;

        public _ResultSet(ResultSet rs, boolean decode) {
            this.rs = rs;
            this.decode = decode;
        }

        /**
         * 获取ResultSet的代理
         *
         * @return
         */
        public ResultSet getResultSet() {
            Class<?>[] infs = rs.getClass().getInterfaces();
            if (infs == null || infs.length == 0)
                infs = IRS;
            return (ResultSet) Proxy.newProxyInstance(rs.getClass().getClassLoader(), infs, this);
        }

        public Object invoke(Object proxy, Method m, Object args[]) throws Throwable {
            Object res = null;
            String method = m.getName();
            if (decode && GETSTRING.equals(method))
                try {
                    String result = (String) m.invoke(rs, args);
                    if (result != null) {
                        res = result;
                    }
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            try {
                res = m.invoke(rs, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return res;
        }

    }

    static abstract class DelegateConnection implements Connection {
        protected final Connection delegate;
        public DelegateConnection(Connection conn) {
            this.delegate = conn;
        }

        public Connection getDelegate() {
            return delegate;
        }

        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }

        public Statement createStatement() throws SQLException {
            return delegate.createStatement();
        }

        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return delegate.prepareStatement(sql);
        }

        public CallableStatement prepareCall(String sql) throws SQLException {
            return delegate.prepareCall(sql);
        }

        public String nativeSQL(String sql) throws SQLException {
            return delegate.nativeSQL(sql);
        }

        public void setAutoCommit(boolean autoCommit) throws SQLException {
            delegate.setAutoCommit(autoCommit);
        }

        public boolean getAutoCommit() throws SQLException {
            return delegate.getAutoCommit();
        }

        public void commit() throws SQLException {
            delegate.commit();
        }

        public void rollback() throws SQLException {
            delegate.rollback();
        }

        public void close() throws SQLException {
            delegate.close();
        }

        public boolean isClosed() throws SQLException {
            return delegate.isClosed();
        }

        public DatabaseMetaData getMetaData() throws SQLException {
            return delegate.getMetaData();
        }

        public void setReadOnly(boolean readOnly) throws SQLException {
            delegate.setReadOnly(readOnly);
        }

        public boolean isReadOnly() throws SQLException {
            return delegate.isReadOnly();
        }

        public void setCatalog(String catalog) throws SQLException {
            delegate.setCatalog(catalog);
        }

        public String getCatalog() throws SQLException {
            return delegate.getCatalog();
        }

        public void setTransactionIsolation(int level) throws SQLException {
            delegate.setTransactionIsolation(level);
        }

        public String toString() {
            return delegate.toString();
        }

        public int getTransactionIsolation() throws SQLException {
            return delegate.getTransactionIsolation();
        }

        public SQLWarning getWarnings() throws SQLException {
            return delegate.getWarnings();
        }

        public void clearWarnings() throws SQLException {
            delegate.clearWarnings();
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType,
                int resultSetConcurrency) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return delegate.getTypeMap();
        }

        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            delegate.setTypeMap(map);
        }

        public void setHoldability(int holdability) throws SQLException {
            delegate.setHoldability(holdability);
        }

        public int getHoldability() throws SQLException {
            return delegate.getHoldability();
        }

        public Savepoint setSavepoint() throws SQLException {
            return delegate.setSavepoint();
        }

        public Savepoint setSavepoint(String name) throws SQLException {
            return delegate.setSavepoint(name);
        }

        public void rollback(Savepoint savepoint) throws SQLException {
            delegate.rollback(savepoint);
        }

        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            delegate.releaseSavepoint(savepoint);
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType,
                int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency,
                    resultSetHoldability);
        }

        public CallableStatement prepareCall(String sql, int resultSetType,
                int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
                throws SQLException {
            return delegate.prepareStatement(sql, autoGeneratedKeys);
        }

        public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
                throws SQLException {
            return delegate.prepareStatement(sql, columnIndexes);
        }

        public PreparedStatement prepareStatement(String sql, String[] columnNames)
                throws SQLException {
            return delegate.prepareStatement(sql, columnNames);
        }

        public Clob createClob() throws SQLException {
            return delegate.createClob();
        }

        public Blob createBlob() throws SQLException {
            return delegate.createBlob();
        }

        public NClob createNClob() throws SQLException {
            return delegate.createNClob();
        }

        public SQLXML createSQLXML() throws SQLException {
            return delegate.createSQLXML();
        }

        public boolean isValid(int timeout) throws SQLException {
            return delegate.isValid(timeout);
        }

        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            delegate.setClientInfo(name, value);

        }

        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            delegate.setClientInfo(properties);
        }

        public String getClientInfo(String name) throws SQLException {
            return delegate.getClientInfo(name);
        }

        public Properties getClientInfo() throws SQLException {
            return delegate.getClientInfo();
        }

        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return delegate.createArrayOf(typeName, elements);
        }

        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return delegate.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            delegate.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return delegate.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            delegate.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            delegate.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return delegate.getNetworkTimeout();
        }
    }

    static class NoCloseConnection extends DelegateConnection {
        public NoCloseConnection(Connection conn) {	super(conn);}
        public void close() throws SQLException {}
    }

    static class ConnectionMonitor implements ThreadLocalConnection.Listener {

        @Override
        public void applied(String properties) {
            LOGGER.info(String.format("[DS] Connection properties : '%s'.",  properties));
        }

        @Override
        public void opened(String c) {
            LOGGER.info(String.format("[DS] Connection '%s' was opened.",  c));
        }

        @Override
        public void cycled(String c) {
            LOGGER.info(String.format("[DS] Connection '%s' was reused.",  c));
        }

        @Override
        public void died(String c) {
            LOGGER.info(String.format("[DS] Close a died connection '%s'.",  c));
        }

        @Override
        public void broken(String c) {
            LOGGER.info(String.format("[DS] Close a broken connection '%s'.",  c));
        }

        @Override
        public void closed(String c) {
            LOGGER.info(String.format("[DS] Connection '%s' was closed.",  c));
        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    }

    public static class Builder {
        private String driver;
        private String url;
        private String username;
        private String password;
        private String connectionProperties;
        private boolean tracing = false;
        //TODO
        private String masterUrl;
        private String slaveUrl;
        private String validationQuery = "select count(*) from dual"; // "SELECT 1" for H2, PostgreSQL
        private int maxSize  = 20;
        private int minSize  = 1;
        private int initSize = 1;
        private long defaultQueryTimeout     = 600L;
        private long maxWaitTime             = 5000L;
        private long maxConnLifetime         = 1200000L; //maxIdleTime
        private long minEvictIdletime        = 300000L;
        private long timeBetweenEvictionRuns = 30000L;
        private long removeAbandonedTimeout  = 300L;

        // public properties
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder driver(String driver) {
            this.driver = driver;
            if( null != driver) {
                try {
                   Class.forName(driver);
               } catch (ClassNotFoundException e) {
                   throw new IllegalArgumentException(String.format("Not found jdbc driver class '%s'.", driver));
               }
           }
            return this;
        }
        public Builder properties(String properties) {
            this.connectionProperties = properties;
            return this;
        }
        public Builder tracing(boolean enable) {
            this.tracing = enable;
            return this;
        }

        public ThreadLocalConnection build() {
            ThreadLocalConnection.Listener listener = null;
            if (tracing) {
                listener = new ConnectionMonitor();
            }
            return new ThreadLocalConnection(url, username, password, connectionProperties, listener);
         }
    }
}