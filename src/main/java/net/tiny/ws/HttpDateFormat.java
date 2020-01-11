package net.tiny.ws;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;

public final class HttpDateFormat {

    public static boolean synchronize = true;
    private static final int DATE_FORMAT_QUEUE_LEN = 10;
    private static ArrayBlockingQueue<SimpleDateFormat> RFC1123_DATE_FORMAT_QUEUE = new ArrayBlockingQueue<SimpleDateFormat>(DATE_FORMAT_QUEUE_LEN);
    /**
     * The date format pattern for RFC 1123.
     */
    private static final String RFC1123_DATE_FORMAT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final SimpleDateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.ENGLISH);
    /**
     * The date format pattern for RFC 1036.
     */
    private static final String RFC1036_DATE_FORMAT_PATTERN = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    private static final SimpleDateFormat RFC1036_DATE_FORMAT = new SimpleDateFormat(RFC1036_DATE_FORMAT_PATTERN, Locale.ENGLISH);
    /**
     * The date format pattern for ANSI C asctime().
     */
    private static final String ANSI_C_ASCTIME_DATE_FORMAT_PATTERN = "EEE MMM d HH:mm:ss yyyy";
    private static final SimpleDateFormat ANSI_C_ASCTIME_DATE_FORMAT = new SimpleDateFormat(ANSI_C_ASCTIME_DATE_FORMAT_PATTERN, Locale.ENGLISH);

    private static final String ACCESSLOG_DATE_FORMAT_PATTERN = "d/MMM/yyyy:HH:mm:ss Z";
    private static final SimpleDateFormat ACCESSLOG_DATE_FORMAT = new SimpleDateFormat(ACCESSLOG_DATE_FORMAT_PATTERN, Locale.ENGLISH);

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    private static final List<SimpleDateFormat> dateFormats;

    static {
        dateFormats = createDateFormats();
    }

    private static List<SimpleDateFormat> createDateFormats() {
        final SimpleDateFormat[] formats = new SimpleDateFormat[]{
            RFC1123_DATE_FORMAT,
            RFC1036_DATE_FORMAT,
            ANSI_C_ASCTIME_DATE_FORMAT,
            ACCESSLOG_DATE_FORMAT
        };
        formats[0].setTimeZone(GMT_TIME_ZONE);
        formats[1].setTimeZone(GMT_TIME_ZONE);
        formats[2].setTimeZone(GMT_TIME_ZONE);
        formats[3].setTimeZone(GMT_TIME_ZONE);
        return Collections.unmodifiableList(Arrays.asList(formats));
    }

    public static Date readDate(final String date) {
        ParseException pe = null;
        for (final SimpleDateFormat format : dateFormats) {
            try {
                synchronized(format) {
                    return format.parse(date);
                }
            } catch (final ParseException e) {
                pe = (pe == null) ? e : pe;
            } finally {
                 // parse can change time zone -> set it back to GMT
                format.setTimeZone(GMT_TIME_ZONE);
            }
        }
        throw new IllegalArgumentException(pe.getMessage(), pe);
    }
    /**
     * Converts HTTP date to Java Date.
     *
     * @param date not <code>null</code>
     * @return Java Date
     * @throws IllegalArgumentException if parsing fails
     */
    public static Date parse(String date) {
        if (synchronize) {
            try {
                synchronized(RFC1123_DATE_FORMAT) {
                    return RFC1123_DATE_FORMAT.parse(date);
                }
            } catch (final ParseException pe) {
                throw new IllegalArgumentException(pe.getMessage(), pe);
            } finally {
                 // parse can change time zone -> set it back to GMT
                RFC1123_DATE_FORMAT.setTimeZone(GMT_TIME_ZONE);
            }
        } else {
            SimpleDateFormat rfc1123 = RFC1123_DATE_FORMAT_QUEUE.poll();
            if (rfc1123 == null) {
                rfc1123 = new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.ENGLISH);
                rfc1123.setTimeZone(GMT_TIME_ZONE);
            }
            try {
                return rfc1123.parse(date);
            } catch (ParseException pe) {
                throw new IllegalArgumentException(pe.getMessage(), pe);
            } finally {
                // parse can change time zone -> set it back to GMT
                rfc1123.setTimeZone(GMT_TIME_ZONE);
                RFC1123_DATE_FORMAT_QUEUE.offer(rfc1123);
            }
        }

    }

    /**
     * Converts Java Data to HTTP date string.
     *
     * @param date Java Date
     * @return the HTTP date string
     */
    public static String format(Date date) {
        if (synchronize) {
            synchronized(RFC1123_DATE_FORMAT) {
                return RFC1123_DATE_FORMAT.format(date);
            }
        } else {
            SimpleDateFormat rfc1123 = RFC1123_DATE_FORMAT_QUEUE.poll();
            if (rfc1123 == null) {
                rfc1123 = new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.US);
                rfc1123.setTimeZone(GMT_TIME_ZONE);
            }
            try {
                return rfc1123.format(date);
            } finally {
                // parse can change time zone -> set it back to GMT
                //rfc1123.setTimeZone(GMT_TIME_ZONE);
                RFC1123_DATE_FORMAT_QUEUE.offer(rfc1123);
            }
        }
    }

    /**
     * Converts current data to HTTP date string.
     *
     * @return the HTTP current date string
     */
    public static String formatCurrentDate() {
        return format(new Date());
    }
}
