package net.tiny.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * @see https://github.com/florent37/Multiplatform-Bus
 */
public class BusTest {

    @Test
    public void testPublishSimpleMessage() throws Exception {
        One one = new One();
        Two two = new Two();
        Three three = new Three();
        final String messageKey = "my_message_key";

        //register to a message
        Bus<Boolean> bus = Bus.getInstance(Boolean.class, false);
        bus.register(one, messageKey, new Consumer<Boolean>() {
                @Override
                public void accept(Boolean t) {
                    System.out.println("One " + t);
                    one.done = true;
                }});

        bus.register(two, messageKey, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean t) {
                System.out.println("Two " + t);
                two.done = true;
            }});

        bus.register(three, messageKey, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean t) {
                System.out.println("Three " + t);
                three.done = true;
            }});

        //send a message
        bus.publish(messageKey, true);

        assertTrue(one.done);
        assertTrue(two.done);
        assertTrue(three.done);
        assertEquals("[BUS] inactive thread pool.", Bus.info(bus));
        Bus.destroy(Boolean.class);
    }


    @Test
    public void testPublishParamsMessage() throws Exception {
        One one = new One();
        Two two = new Two();
        Three three = new Three();
        final String channel = "params_message_key";

        //register to a message
        Bus<Long> bus = Bus.getInstance(Long.class, false);
        bus.register(one, channel, value -> one.exec(value));
        bus.register(two, channel, value -> two.exec(value));
        bus.register(three, channel, value -> three.exec(value));

        //send a message
        bus.publish(channel, 1234567890L);

        assertTrue(one.done);
        assertTrue(two.done);
        assertTrue(three.done);

        bus.remove(channel, one);
        bus.remove(two);

        one.done = false;
        two.done = false;
        three.done = false;
        //send a message
        bus.publish(channel, 1234567890L);
        assertFalse(one.done);
        assertFalse(two.done);
        assertTrue(three.done);

        Bus.destroy(Long.class);
    }


    @Test
    public void testPublishWithMessageFilter() throws Exception {
        One one = new One();
        Two two = new Two();
        Three three = new Three();
        final String channel = "filter_message_key";

        //register to a message
        Bus<String> bus = Bus.getInstance(String.class, false);
        bus.register(one, channel, one, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return t.contains("One");
            }});

        bus.register(two, channel, two, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return t.contains("Hello");
            }});

        bus.register(three, channel, three, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return t.contains("Three");
            }});

        //send a message
        bus.publish(channel, "Hello One");

        assertTrue(one.done);
        assertTrue(two.done);
        assertFalse(three.done);

        Bus.destroy(String.class);
    }

    @Test
    public void testPublishSyncLongTask() throws Exception {
        One one = new One();
        one.delay = 300L;
        Two two = new Two();
        two.delay = 100L;
        Three three = new Three();
        three.delay = 200L;
        final String channel = "sync_consuming_key";

        //register to a message
        Bus<Long> bus = Bus.getInstance(Long.class, false);
        bus.register(one, channel, value -> one.exec(value));
        bus.register(two, channel, value -> two.exec(value));
        bus.register(three, channel, value -> three.exec(value));

        long st = System.currentTimeMillis();
        //send a message
        bus.publish(channel, 1234567890L);
        long et = System.currentTimeMillis() - st;
        System.out.println(String.format("同期发送耗时%dms", et));
        assertTrue (et > 500L); //同期发送耗时600ms

        assertTrue(one.done);
        assertTrue(two.done);
        assertTrue(three.done);

        bus.clear(channel);
        bus.remove(two);

        Bus.destroy(Long.class);
    }

    @Test
    public void testPublishAnsyncLongTask() throws Exception {
        One one = new One();
        one.delay = 300L;
        Two two = new Two();
        two.delay = 100L;
        Three three = new Three();
        three.delay = 200L;
        final String channel = "ansync_consuming_key";

        //register to a message
        Bus<Double> bus = Bus.getInstance(Double.class);
        bus.register(one, channel, value -> one.exec(value));
        bus.register(two, channel, value -> two.exec(value));
        bus.register(three, channel, value -> three.exec(value));

        long st = System.currentTimeMillis();
        //send a message
        bus.publish(channel, 123.456789d);
        long et = System.currentTimeMillis() - st;
        System.out.println(String.format("非同期发送耗时%dms", et));
        assertTrue (et < 10L); //非同期发送耗时2ms
        System.out.println(bus.toString());


        assertFalse(one.done);
        assertFalse(two.done);
        assertFalse(three.done);

        Thread.sleep(650L);

        assertTrue(one.done);
        assertTrue(two.done);
        assertTrue(three.done);

        bus.clear(channel);
        bus.remove(two);

        Bus.destroy(Double.class);
    }


    @Test
    public void testPublishCallback() throws Exception {
        CallbackTask task = new CallbackTask();
        final String channel = "callback_key";

        //register to a message
        Bus<String> bus = Bus.getInstance(String.class);

        bus.register(task, channel, value -> task.exec(value), callback -> {
                    if(callback.success()) {
                        System.out.println("Success!");
                    } else {
                        callback.cause().printStackTrace();
                    }
                });
        //send a message
        bus.publish(channel, "Request");

        Thread.sleep(100L);
        bus.clear(channel);


        Bus.destroy(String.class);
    }

    //////////////////////////////////////
    // Test Inner Classes
    static interface MircoServiceTask {
        public void exec(String... params);
        public void exec(Long id);
        public void exec(Double value);
        public void exec(Properties prop);
    }

    static abstract class AbstrackTask implements MircoServiceTask, Consumer<String> {
        long delay = 0L;
        boolean done = false;

        @Override
        public void accept(String t) {
            delay();
            exec(t.split("[ ]"));
        }
        @Override
        public void exec(Long id) {
            delay();
            System.out.println(String.format("%s exec id : %d", getClass().getSimpleName(), id));
            done = true;
        }
        @Override
        public void exec(Double value) {
            delay();
            System.out.println(String.format("%s exec value : %.4f", getClass().getSimpleName(), value));
            done = true;
        }
        @Override
        public void exec(Properties prop) {
            delay();
            System.out.println(String.format("%s exec prop : %d", getClass().getSimpleName(), prop.size()));
            done = true;
        }

        protected void delay() {
            if (delay > 0L) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {}
            }
        }
    }
    static class One extends AbstrackTask {
        public void exec(String... params) {
            System.out.println("One params : " + params.length);
            done = true;
        }
    }

    static class Two extends AbstrackTask {
        public void exec(String... params) {
            System.out.println("Two params : " + params.length);
            done = true;
        }
    }

    static class Three extends AbstrackTask {
        public void exec(String... params) {
            System.out.println("Three params : " + params.length);
            done = true;
        }
    }

    static class CallbackTask {
        boolean done = false;
        public void exec(String... params) {
            System.out.println("CallbackTask params : " + params.length);
            done = true;
        }
    }
}
