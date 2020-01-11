package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;

import net.tiny.benchmark.Benchmarker;

public class AccessLoggerTest {

    @Test
    public void testLogFormat() throws Exception {
        AccessLogger logger = new AccessLogger();
        assertEquals("Access log filter", logger.description());

        logger.setFormat("COMBINED");
        assertEquals("COMBINED", logger.getFormat());
        assertEquals("{0} {1} {2} {3} {4} \"{5}\" {6} {7} \"{8}\" \"{9}\"", logger.getFormatPattern());

        assertEquals("{0} {1} {2} {3} {4} \"{5}\" {6} {7} \"{8}\" \"{9}\"",
                AccessLogger.getPattern(AccessLogger.COMBINED_FORMAT));

        String log = MessageFormat.format(AccessLogger.getPattern(AccessLogger.COMBINED_FORMAT),
                "127.0.0.1", "-", "hogo", "30/Jun/2019:11:57:39 +0900", "12", "GET", "200", "420", "/index.html", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        assertEquals("127.0.0.1 - hogo 30/Jun/2019:11:57:39 +0900 12 \"GET\" 200 420 \"/index.html\" \"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\"", log);
    }

    @Test
    public void testBenchmarkMessageFormat() throws Exception {
        AccessLogger logger = new AccessLogger();
        String pattern = AccessLogger.getPattern(AccessLogger.COMBINED_FORMAT);
        Benchmarker bench = new Benchmarker();
        bench.start(10000L, 1000L);
        while (bench.loop()) {
            bench.trace(System.out);
            MessageFormat.format(pattern,
                    "127.0.0.1", "-", "hogo", "30/Jun/2019:11:57:39 +0900", "12", "GET", "200", "420", "/index.html", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        }
        bench.stop();
        //Summary ETA:66ms 459889ns MIPS:0.241 0.004ms/per min:51.520K/s max:395.396K/s avg:237.786K/s mean:197.736K/s count:10000 lost:4ms 665661ns
        bench.metric(System.out);
    }

    @Test
    public void testBenchmarkWriteLog() throws Exception {
        AccessLogger logger = new AccessLogger();
        String pattern = AccessLogger.getPattern(AccessLogger.COMBINED_FORMAT);
        Benchmarker bench = new Benchmarker();
        bench.start(10000L, 1000L);
        while (bench.loop()) {
            bench.trace(System.out);
            String log = MessageFormat.format(pattern,
                    "127.0.0.1", "-", "hogo", "30/Jun/2019:11:57:39 +0900", "12", "GET", "200", "420", "/index.html", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            logger.writeAccessLog(log);
        }
        bench.stop();
        //Summary ETA:331ms 586904ns MIPS:0.034 0.029ms/per min:14.924K/s max:44.621K/s avg:33.116K/s mean:31.795K/s count:10000 lost:6ms 614539ns
        bench.metric(System.out);
    }
}
