package net.tiny.ws;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

//import java.util.logging.ConsoleHandler;
//import java.util.logging.Formatter;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * @see https://qiita.com/ryounagaoka/items/e7782ab29ff9fbe8f891
 *
 */
public class AccessLogger extends Filter {

	private static final Logger LOGGER = Logger.getLogger(AccessLogger.class.getName());

    /*
     * LogFormat "%h %l %u %t \"%r\" %>s %b" common
     * Exp:
     * 127.0.0.1 - username [10/0ct/2011:13:55:36 -0700] "GET /index.html HTTP/1.0" 200 2326
     **/
    public static final String COMMON_FORMAT = "%h %l %u [%t] \"%r\" %>s %b";

    /*
     * LogFormat "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" ¥"%{User-agent}i¥" combined
     * Exp:
     * 127.0.0.1 - username [10/0ct/2011:13:55:36 -0700] "GET /index.html HTTP/1.0" 200 2326 "http://webserver:8080/index.html" "Mozilla/4.08 [en] (Win98; I ; Nav)"
     **/
    public static final String COMBINED_FORMAT = "%h %l %u %t %T \"%r\" %>s %b \"%{Referer}i\" \"%{User-agent}i\"";

    public static enum Format {
        COMMON,
        COMBINED
    }

    private static final String[] PATTERN_KEYS = new String[] {
        "%h", "%l", "%u", "%t", "%T", "%r", "%>s", "%b", "%{Referer}i", "%{User-agent}i" };

    private static	final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("d/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);


    private static String COMMON_PATTERN = null;
    private static String COMBINED_PATTERN = null;

    static String getPattern(final String pattern) {
        StringBuilder builder = new StringBuilder(pattern);
        for(int i=0; i<PATTERN_KEYS. length; i++) {
            String sub = "{" + i + "}";
            int pos = builder. indexOf(PATTERN_KEYS[i]);
            if(pos >= 0) {
                builder = builder.insert(pos, sub);
                pos = pos + sub.length();
                builder = builder.delete(pos, pos + PATTERN_KEYS[i].length());
            }
        }
        return builder.toString();
    }

    static String common() {
        if(COMMON_PATTERN == null)
            COMMON_PATTERN = getPattern(COMMON_FORMAT);
        return COMMON_PATTERN;
    }

    static String combined() {
        if(COMBINED_PATTERN == null)
            COMBINED_PATTERN = getPattern(COMBINED_FORMAT);
        return COMBINED_PATTERN;
    }

    private Format format = Format.COMBINED;
    private String formatPattern = combined();
    private String out = null;
    private PrintWriter writer;

    public AccessLogger() {
    	setOut("stderr");
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        final long start = System.currentTimeMillis();
        if(null != chain) {
            chain.doFilter(exchange);
        }
        String log = format(exchange, formatPattern, (System.currentTimeMillis()-start));
        writeAccessLog(log);
    }

    @Override
    public String description() {
        return "Access log filter";
    }

    public void setOut(String file) {
    	this.out = file;
    	switch(out) {
    	case "stdout":
            setLogger(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out))));
    		break;
    	case "stderr":
            setLogger(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err))));
    		break;
    	default:
            try {
                setLogger(new PrintWriter(new BufferedWriter(new FileWriter(file))));
            } catch (IOException ex) {
            	setLogger(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out))));
            	LOGGER.severe(String.format("Cant not access log file '%s', output to console.", file));
            }
    		break;
    	}
    }

    protected void setLogger(Writer logger) {
        if (logger instanceof PrintWriter) {
            writer = (PrintWriter)logger;
        } else {
            if (!(logger instanceof BufferedWriter)) {
                writer = new PrintWriter(new BufferedWriter(logger));
            } else {
                writer = new PrintWriter(logger);
            }
        }
    }

    void writeAccessLog(String log) {
        writer.println(log);
        writer.flush();
    }

    public String getFormatPattern() {
        return formatPattern;
    }
    public void setFormatPattern(String pattern) {
        this.formatPattern = pattern;
    }

    public String getFormat() {
        return format.name();
    }

    public void setFormat(String format) {
        switch (Format.valueOf(format.toUpperCase())) {
        case COMMON:
            setFormatPattern(common());
            break;
        case COMBINED:
        default:
            setFormatPattern(combined());
            break;
        }
    }

    String formatDate(Date date) {
        synchronized(DATE_FORMAT) {
            return DATE_FORMAT.format(date);
        }
    }

    private String format(HttpExchange exchange, String formatPattern, long time) {
        final String host = exchange.getRemoteAddress().getAddress().getHostAddress();
        String identity = "-";
        String username = "-";
        final HttpPrincipal principal = exchange.getPrincipal();
        if (principal!=null) {
            username = principal.getUsername();
        }
        final String date = formatDate(Calendar.getInstance().getTime());
        final String request = exchange.getRequestMethod() + " "
                + exchange.getRequestURI() + " "
                + exchange.getProtocol();
        final String code = Integer.toString(exchange.getResponseCode());
        String size = exchange.getResponseHeaders().getFirst("Content-length");
        if(size == null) {
            size = "0";
        }

        final Headers headers = exchange.getRequestHeaders();
        final String agent = headers.getFirst("User-agent");
        String referer = headers.getFirst("Referer");
        if(null == referer) {
            referer = "";
        }
        String timeTaken = String.format("%.3f", ((float)time/1000f));
        return MessageFormat.format(formatPattern,
                host, identity, username, date, timeTaken, request, code, size, referer, agent);
    }

    @Override
    protected void finalize() throws Throwable {
        if(null != writer) {
            writer.close();
        }
    }
}
