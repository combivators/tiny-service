package net.tiny.dbcp;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class SimpleDataSource implements DataSource, Closeable {

    static {
        DriverManager.getDrivers();
    }

    private ThreadLocalConnection.Builder builder;
    private ThreadLocalConnection connection = null;

    public ThreadLocalConnection.Builder getBuilder() {
    	if (null == builder) {
    		builder = new ThreadLocalConnection.Builder();
    	}
    	return builder;
    }

    protected ThreadLocalConnection getThreadLocalConnection() {
    	if (null == connection) {
    		connection = builder.build();
    	}
    	return connection;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException("Not impleted");
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException("Not impleted");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return getThreadLocalConnection().getLogger();
	}

	@Override
    public Connection getConnection() throws SQLException {
        return getThreadLocalConnection().get();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not impleted");
    }

    @Override
    public void close() throws IOException {
    	getThreadLocalConnection().close();
    }

    @Override
    public String toString() {
        return String.format("%s#%d[%s]", getClass().getSimpleName(), hashCode(),
        		getThreadLocalConnection().toString());
    }

    @Override
    protected void finalize() throws Throwable {
    	close();
    }

}

