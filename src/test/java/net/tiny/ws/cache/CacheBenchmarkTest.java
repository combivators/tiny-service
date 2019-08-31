package net.tiny.ws.cache;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class CacheBenchmarkTest {

    public static final int COST = 1000;
    public static final int SIZE = 1000;
    // 重复率
//    public static final double DUPLICATION_RATE = 0.0;
    // 容量率
//    public static final double CAPACITY_RATE = 1.0;
    // 容量
//    public static final int CAPACITY = (int) (Math.round(SIZE * (1.0 - DUPLICATION_RATE) * CAPACITY_RATE));
//
//    public static final List<Integer> LIST = IntStream.rangeClosed(0, SIZE - 1)
//            .boxed()
//            .map(it -> (int) ((double) it * (1.0 - DUPLICATION_RATE)))
//            .collect(Collectors.toList());
//
//    public static final int UNIQUE = (int) LIST.stream()
//            .distinct()
//            .count();

    @Test
    public void testBenchmarkCache() throws Exception {
        // 重复率
        double duplicationRate = 0.0;
        // 容量率
        double capacityRate = 1.0;
        benchmarkCache(duplicationRate, capacityRate);
        System.out.println("---------------------------------");

        // 重复率
        duplicationRate = 0.2;
        // 容量率
        capacityRate = 1.0;
        benchmarkCache(duplicationRate, capacityRate);
        System.out.println("---------------------------------");
        // 重复率
        duplicationRate = 0.8;
        // 容量率
        capacityRate = 1.0;
        benchmarkCache(duplicationRate, capacityRate);
        System.out.println("---------------------------------");
        // 重复率
        duplicationRate = 0.2;
        // 容量率
        capacityRate = 0.5;
        benchmarkCache(duplicationRate, capacityRate);
        System.out.println("---------------------------------");
    }


    void benchmarkCache(final double duplicationRate, final double capacityRate) throws Exception {

        // 重复率
        //final double duplicationRate = 0.0;
        // 容量率
        //final double capacityRate = 1.0;
        // 容量
        final int capacity = (int) (Math.round(SIZE * (1.0 - duplicationRate) * capacityRate));

        final List<Integer> list = IntStream.rangeClosed(0, SIZE - 1)
                .boxed()
                .map(it -> (int) ((double) it * (1.0 - duplicationRate)))
                .collect(Collectors.toList());

        final int unique = (int) list.stream()
                .distinct()
                .count();

        Function<Integer, Integer> calc = it -> it + IntStream.rangeClosed(1, COST * 10000).sum();
        Collections.shuffle(list);
        System.out.println(String.format("Cost:%d  Size:%d  Unique:%d  Duplication Rate:%.2f  Capacity:%d  Capacity Rate:%.2f",
                COST, SIZE, unique, duplicationRate, capacity, capacityRate));

        seri("None Cache", list, new Cache.NonCache<>(calc));
        para("None Cache", list, new Cache.NonCache<>(calc));
        seri("None LRU", list, new Cache.NonLruCache<>(capacity, calc));
        para("None LRU", list, new Cache.NonLruCache<>(capacity, calc));
        seri("LRU Cache1", list, new Cache.LruCache1<>(capacity, calc));
        para("LRU Cache1", list, new Cache.LruCache1<>(capacity, calc));
        seri("LRU Cache2", list, new Cache.LruCache2<>(capacity, calc));
        para("LRU Cache2", list, new Cache.LruCache2<>(capacity, calc));
        seri("LRU Cache3", list, new Cache.LruCache3<>(capacity, calc));
        para("LRU Cache3", list, new Cache.LruCache3<>(capacity, calc));
        seri("BarakbCache", list, new Cache.LruCache4<>(capacity, calc));
        para("BarakbCache", list, new Cache.LruCache4<>(capacity, calc));

    }

    private void seri(String name, List<Integer> list, Calculator<Integer, Integer> calculator) {
        long start = System.currentTimeMillis();
        list.stream()
            .mapToInt(calculator::get)
            .sum();
        System.out.println(name + " Serial:\t" + (System.currentTimeMillis() - start) + "ms.");
    }

    private void para(String name, List<Integer> list, Calculator<Integer, Integer> calculator) {
        long start = System.currentTimeMillis();
        list.parallelStream()
            .mapToInt(calculator::get)
            .sum();
        System.out.println(name + " Parallel:\t" + (System.currentTimeMillis() - start) + "ms.");
    }
}
