package net.tiny.ws.auth;

import java.util.Map;
import java.util.logging.Level;

public class Options {

	public static final String KEY_DEBUG    = "debug";
	public static final String KEY_LOG_LEVEL= "logging.level";

	private Map<String, ?> options;

	public static Options valueOf(Map<String, ?> options) {
		Options instance = new Options();
		instance.setOptions(options);
		return instance;
	}

	public Map<String, ?> getOptions() {
		return this.options;
	}

	public void setOptions(Map<String, ?> options) {
		this.options = options;
	}
	/**
	 * Get a numeric option from the module's options.
	 *
	 * @param name Name of the option
	 * @param value Default value for the option
	 * @return The boolean value of the options object.
	 */
	public int getOption(String name, int value) {
		String opt = ((String) options.get(name));
		if (opt == null) return value;
		try {
			return Integer.parseInt(opt);
		} catch (Exception e) {
			return value;
		}
	}

	/**
	 * Get a String option from the module's options.
	 *
	 * @param name Name of the option
	 * @param value Default value for the option
	 * @return The String value of the options object.
	 */
	public String getOption(String name, String value) {
		String opt = (String) options.get(name);
		return opt == null ? value : opt;
	}

	public String getOption(String name) {
		return getOption(name, null);
	}

	/**
	 * Get a boolean option from the module's options.
	 *
	 * @param name Name of the option
	 * @param value Default value for the option
	 * @return The boolean value of the options object.
	 */
	public boolean getOption(String name, boolean value) {
		String opt = ((String) options.get(name));
		if (opt == null) return value;
		opt = opt.trim();
		if (opt.equalsIgnoreCase("true") || opt.equalsIgnoreCase("yes") || opt.equals("1"))
			return true;
		else if (opt.equalsIgnoreCase("false") || opt.equalsIgnoreCase("no") || opt.equals("0"))
			return false;
		else
			return value;
	}

	public boolean isDebug() {
		return getOption(KEY_DEBUG, false);
	}

	public Level getLogLevel() {
		return Level.parse(getOption(KEY_LOG_LEVEL, "FINE"));
	}
}
