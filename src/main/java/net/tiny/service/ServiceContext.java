package net.tiny.service;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ServiceContext {

	Logger LOGGER = Logger.getLogger(ServiceContext.class.getName());

	//Main process id
	String PID = "PID";

	public static interface Listener {
		void created(String address);
		void bind(String name);
		void unbind(String name);

		void invoke(Object inst, Method method, Object param);

		void debug(String msg);
		void info(String msg);
		void warn(String msg, Throwable exception);
		void error(String msg, Throwable exception);
	}

	Listener getListener();
	void setListener(Listener listener);

	// Service Register
	void bind(String name, Object target, boolean singleton);
	void unbind(String name);
	boolean isBinding(String name);

	String getAddress();
	String getType();

	boolean exist(String name);
	boolean exist(String name, Class<?> classType);

	void destroy();
	void refresh();

	// Service locator
	<T> T lookup(String name, Class<T> classType);
	<T> Collection<T> lookupGroup(Class<T> classType);
	<T> T lookup(Class<T> classType);
	Object lookup(String name);

	int getProcessId();

	Listener MONITOR = new Listener() {
		@Override
		public void invoke(Object inst, Method method, Object param) {
			LOGGER.info(" [invoke] - " + inst.getClass().getName() + "."
					+ method.getName() + "('" + param + "')");
		}
		@Override
		public void bind(String name) {
			LOGGER.info(" [registry] - bind '" + name+ "'");
		}

		@Override
		public void unbind(String name) {
			LOGGER.info(" [registry] - unbind '" + name+ "'");
		}

		@Override
		public void created(String address) {
			LOGGER.info(" [registry] - created on '" + address+ "'");
		}

		@Override
		public void debug(String msg) {
			LOGGER.fine(msg);
		}

		@Override
		public void info(String msg) {
			LOGGER.info(msg);
		}

		@Override
		public void warn(String msg, Throwable exception) {
			if(null != exception) {
				LOGGER.log(Level.WARNING, msg, exception);
			} else {
				LOGGER.log(Level.WARNING, msg);
			}
		}

		@Override
		public void error(String msg, Throwable exception) {
			if(null != exception) {
				LOGGER.log(Level.SEVERE, msg, exception);
			} else {
				LOGGER.log(Level.SEVERE, msg);
			}
		}
	};
}
