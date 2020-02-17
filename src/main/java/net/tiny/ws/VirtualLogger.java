package net.tiny.ws;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * Access log format
 * @see https://qiita.com/ryounagaoka/items/e7782ab29ff9fbe8f891
 *
 */
public class VirtualLogger extends Filter {

    private static final Logger LOGGER = Logger.getLogger(VirtualLogger.class.getName());

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
    private List<VirtualHost> hosts = new ArrayList<>();
    private Map<String, PrintWriter> writers = null;
    //private String out = null;
    //private PrintWriter writer;

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        final long start = System.currentTimeMillis();
        if(null != chain) {
            chain.doFilter(exchange);
        }
        String log = format(exchange, formatPattern, (System.currentTimeMillis()-start));
        final String host = exchange.getRequestHeaders().getFirst("Host");
        writeAccessLog(host, log);
    }

    @Override
    public String description() {
        return "Virtual host access log filter";
    }

    PrintWriter findAccessWriter(String virtual) {
        PrintWriter writer = writers.get(virtual);
        if (writer == null) {
            // Find host name without port
            writer = writers.get(virtual.split(":")[0]);
        }
        return writer;
    }

    void writeAccessLog(String virtual, String log) {
        PrintWriter writer = findAccessWriter(virtual);
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
        final String remote = exchange.getRemoteAddress().getAddress().getHostAddress();
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
                remote, identity, username, date, timeTaken, request, code, size, referer, agent);
    }

    public void setHosts(List<VirtualHost> virtuals) {
        this.hosts = virtuals;
        this.writers = new HashMap<>();
        for (VirtualHost virtual : hosts) {
            this.writers.put(virtual.domain(), createWriter(virtual));
        }
    }

    private PrintWriter createWriter(VirtualHost virtual) {
        final String out = virtual.log();
        PrintWriter writer = null;
        switch(out) {
        case "stdout":
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
            LOGGER.info(String.format("[WEB] Virtual '%s' access log output console '%s'.", virtual.domain(), out));
            break;
        case "stderr":
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err)));
            LOGGER.info(String.format("[WEB] Virtual '%s' access log output console '%s'.", virtual.domain(), out));
            break;
        default:
            File file = null;
            if (out.indexOf("/") == -1) {
                file = new File(virtual.home(), out);
            } else {
                file = new File(out);
            }
            try {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                LOGGER.info(String.format("[WEB] Virtual '%s' access log file '%s'.", virtual.domain(), file.getAbsolutePath()));
            } catch (IOException ex) {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
                LOGGER.severe(String.format("[WEB] Cant not access log file '%s', output to console.", file.getAbsolutePath()));
            }
            break;
        }
        return writer;
    }

    void closeAll() {
        for (PrintWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        closeAll();
    }
}
