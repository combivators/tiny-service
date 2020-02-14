package net.tiny.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.tiny.ws.Callback;

/**
 * 异步消息流的总线处理类
 *
 * <code>
 * Task task = new Task();
 * Bus<String> bus = Bus.getInstance(String.class);
 * //register to a message
 * bus.register(task, "channel1", value -> task.exec(value));
 *
 * //send a message
 * bus.publish("channel1", "Hello One");
 *
 * bus.remove("channel1", task);
 * bus.clear("channel1");
 * Bus.destroy(String.class);
 * </code>
 *
 * @param <T>
 */
public final class Bus<T> {

    private static ConcurrentMap<Class<?>, Bus<?>> instances = new ConcurrentHashMap<>();
    private static Feature feature = new Feature();

    /**
     * 设置异步调用时线程池的特性
     * @param f
     */
    public final static void setFeature(Feature f) {
        feature = f;
    }

    /**
     * 返回异步消息流的总线实体
     * @param <E>
     * @param type
     * @return
     */
    public final static <E> Bus<E> getInstance(Class<E> type) {
        return getInstance(type, true);
    }

    /**
     * 返回消息流的总线实体
     *
     * @param <E>
     * @param type
     * @param asynchronous true：异步消息 false：异步消息
     * @return
     */
    @SuppressWarnings("unchecked")
    public final static <E> Bus<E> getInstance(Class<E> type, boolean asynchronous) {
        Bus<E> bus = (Bus<E>) instances.get(type);
        if (null == bus) {
            bus = new Bus<E>(type, asynchronous);
            instances.put(type, bus);
        }
        return bus;
    }

    /**
     * 销毁指定类的消息流总线
     * @param <E>
     * @param type
     */
    @SuppressWarnings("unchecked")
    public final static <E> void destroy(Class<E> type) {
        if (instances.containsKey(type)) {
            Bus<E> bus = (Bus<E>) instances.remove(type);
            bus.clear();
        }
        if (instances.isEmpty() && feature.executor != null) {
            feature.shutdown();
        }
    }

    public final static <E> String info(Bus<E> bus) {
        return feature.toString();
    }

    /**
     * 异步调用时线程池的特性
     * 缺省设置为：5个线程，最大10个线程，超时为3秒
     */
    public static class Feature {
        public int size = 5;
        public int max = 10;
        public long timeout = 3L;
        public ExecutorService executor = null;
        private ThreadGroup group;

        public Feature size(int s) {
            assert (s > 0);
            size = s;
            return this;
        }

        public Feature max(int m) {
            assert (m > 0);
            max = m;
            return this;
        }

        public Feature timeout(long t) {
            assert (t > 0L);
            timeout = t;
            return this;
        }

        public Feature executor(ExecutorService e) {
            if (null != e && null == executor)
                executor = e;
            return this;
        }

        @Override
        public String toString() {
            if (null == executor) {
                return "[BUS] inactive thread pool.";
            }
            return String.format("%s active: %d", group.getName(), group.activeCount());
        }

        void shutdown() {
            if (null == executor)
                return;
            executor.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    executor.awaitTermination(timeout, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        public ExecutorService build() {
            if (null == executor) {
                group = new ThreadGroup("[BUS]");
                final ThreadFactory factory = new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable target) {
                        return new Thread(group, target);
                    }
                };
                executor = new ThreadPoolExecutor(size, max, timeout, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(size), factory, new ThreadPoolExecutor.AbortPolicy());
            }
            return executor;
        }
    }

    private class ObserverFunctions implements Consumer<T>, Predicate<T> {
        private List<Consumer<T>> functions = new ArrayList<>();
        private Predicate<T> filter;
        private Consumer<Callback<T>> callback;

        ObserverFunctions(Consumer<T> c, Predicate<T> p, Consumer<Callback<T>> cb) {
            functions.add(c);
            filter = p;
            callback = cb;
        }

        public void add(Consumer<T> c) {
            if (!functions.contains(c)) {
                functions.add(c);
            }
        }

        @Override
        public void accept(T t) {
            if (asynchronous) {
                // 非同期调用
                functions.stream().forEach(c -> ansync(c, t));
            } else {
                // 同期调用
                functions.stream().forEach(c -> sync(c, t));
            }
        }

        @Override
        public boolean test(T t) {
            if (filter == null)
                return true;
            return filter.test(t);
        }

        // 同期调用
        private void sync(Consumer<T> c, T t) {
            counter.incrementAndGet();
            try {
                c.accept(t);
                if(null != callback) {
                    callback.accept(Callback.succeed(t));
                }
            } catch (Throwable e) {
                if(null != callback) {
                    callback.accept(Callback.failed(e));
                }
            } finally {
                counter.decrementAndGet();
            }

        }

        // 非同期调用
        private void ansync(Consumer<T> c, T t) {
            feature.build().execute(new Runnable() {
                @Override
                public void run() {
                    counter.incrementAndGet();
                    try {
                        c.accept(t);
                        if(null != callback) {
                            callback.accept(Callback.succeed(t));
                        }
                    } catch (Throwable e) {
                        if(null != callback) {
                            callback.accept(Callback.failed(e));
                        }
                    } finally {
                        counter.decrementAndGet();
                    }
                }
            });
        }
    }

    private class KeyObservers {
        private ConcurrentMap<Object, ObserverFunctions> observers = new ConcurrentHashMap<Object, ObserverFunctions>();

        KeyObservers(Object observer, Consumer<T> consumer, Predicate<T> predicate, Consumer<Callback<T>> callback) {
            observers.put(observer, new ObserverFunctions(consumer, predicate, callback));
        }

        public void add(Object observer, Consumer<T> consumer, Predicate<T> predicate, Consumer<Callback<T>> callback) {
            ObserverFunctions value = observers.get(observer);
            if (null != value) {
                value.add(consumer);
            } else {
                observers.put(observer, new ObserverFunctions(consumer, predicate, callback));
            }
        }

        public void remove(Object observer) {
            observers.remove(observer);
        }

        public void clear() {
            observers.clear();
        }

        public void accept(final T t) {
            final Predicate<ObserverFunctions> filter = new Predicate<ObserverFunctions>() {
                @Override
                public boolean test(Bus<T>.ObserverFunctions f) {
                    return f.test(t);
                }
            };

            Stream<ObserverFunctions> stream = observers.values().stream();
            if (null != filter) {
                stream = stream.filter(filter);
            }
            stream.forEach(o -> o.accept(t));
        }
    }

    private ConcurrentMap<String, KeyObservers> keyObservers = new ConcurrentHashMap<>();

    private final Class<?> type;
    private final boolean asynchronous;
    private AtomicInteger counter;

    private <E> Bus(Class<E> type, boolean ansyc) {
        this.type = type;
        this.asynchronous = ansyc;
        this.counter = new AtomicInteger();
    }

    /**
     * @see Bus#register(Object, String, Consumer, Predicate, Consumer)
     * @param observer
     * @param channel
     * @param consumer
     * @param callback
     */
    public void register(Object observer, String channel, Consumer<T> consumer) {
        register(observer, channel, consumer, null, null);
    }

    /**
     * @see Bus#register(Object, String, Consumer, Predicate, Consumer)
     * @param observer
     * @param channel
     * @param consumer
     * @param callback
     */
    public void register(Object observer, String channel, Consumer<T> consumer, Predicate<T> filter) {
        register(observer, channel, consumer, filter, null);
    }

    /**
     * @see Bus#register(Object, String, Consumer, Predicate, Consumer)
     * @param observer
     * @param channel
     * @param consumer
     * @param callback
     */
    public void register(Object observer, String channel, Consumer<T> consumer, Consumer<Callback<T>> callback) {
        register(observer, channel, consumer, null, callback);
    }

    /**
     * 注册指定通道的监听器，以及过滤器和回调器
     *
     * @param observer
     * @param channel
     * @param consumer
     * @param filter
     * @param callback
     */
    public void register(Object observer, String channel, Consumer<T> consumer, Predicate<T> filter, Consumer<Callback<T>> callback) {
        KeyObservers observers = keyObservers.get(channel);
        if (null != observers) {
            observers.add(observer, consumer, filter, callback);
        } else {
            keyObservers.put(channel, new KeyObservers(observer, consumer, filter, callback));
        }
    }

    /**
     * 删除指定通道的监听器
     *
     * @param channel
     * @param observer
     */
    public void remove(String channel, Object observer) {
        KeyObservers observers = keyObservers.get(channel);
        if (observers != null)
            observers.remove(observer);
    }

    /**
     * 删除指定的监听器
     *
     * @param observer
     */
    public void remove(Object observer) {
        keyObservers.values().stream().forEach(s -> s.remove(observer));
    }

    /**
     * 清除指定通道所以的监听器
     *
     * @param channel
     */
    public void clear(String channel) {
        KeyObservers observers = keyObservers.remove(channel);
        if (null != observers)
            observers.clear();
    }

    protected void clear() {
        synchronized (keyObservers) {
            keyObservers.values().stream().forEach(o -> o.clear());
            keyObservers.clear();
        }
    }

    /**
     * 向指定的通道发送消息
     *
     * @param channel
     * @param message
     */
    public void publish(String channel, T message) {
        KeyObservers observers = keyObservers.get(channel);
        if (observers != null)
            observers.accept(message);
    }

    public boolean valid(String channel) {
        if (null == channel || channel.isEmpty())
            return false;
        return keyObservers.containsKey(channel);
    }

    @Override
    public String toString() {
        return String.format("{%s} channels:%s running:%d", type.getSimpleName(),
                keyObservers.size(), counter.get());
    }
}
