package net.tiny.ws;

import java.util.function.Function;

public interface Callback<T> {
    boolean isComplete();
    T result();
    Throwable cause();
    boolean success();
    boolean fail();
    Callback<T> complete(T result);
    Callback<T> complete();
    Callback<T> fail(Throwable cause);
    Callback<T> fail(String message);
    Callback<T> setHandler(Handler<Callback<T>> handler);
    Handler<Callback<T>> handler();

    default <U> Callback<U> map(Function<T, U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Callback<U> ret = Callback.callback();
        setHandler(ar -> {
            if (ar.success()) {
                U mapped;
                try {
                    mapped = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(mapped);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }

    default <V> Callback<V> map(V value) {
        return map(t -> value);
    }

    default Callback<T> otherwise(Function<Throwable, T> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Callback<T> ret = Callback.callback();
        setHandler(ar -> {
            if (ar.success()) {
                ret.complete(result());
            } else {
                T value;
                try {
                    value = mapper.apply(ar.cause());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(value);
            }
        });
        return ret;
    }

    default Callback<T> otherwise(T value) {
        return otherwise(err -> value);
    }

    default Callback<T> recover(Function<Throwable, Callback<T>> mapper) {
        if (mapper == null) {
          throw new NullPointerException();
        }
        Callback<T> ret = Callback.callback();
        setHandler(ar -> {
            if (ar.success()) {
                ret.complete(result());
            } else {
                Callback<T> mapped;
                try {
                    mapped = mapper.apply(ar.cause());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                mapped.setHandler(ret.handler());
            }
        });
        return ret;
    }

    default <U> Callback<U> compose(Handler<T> handler, Callback<U> next) {
        setHandler(ar -> {
            if (ar.success()) {
                try {
                    handler.handle(ar.result());
                } catch (Throwable err) {
                    if (next.isComplete()) {
                        throw err;
                    }
                    next.fail(err);
                }
            } else {
                next.fail(ar.cause());
            }
        });
        return next;
    }

    default <U> Callback<U> compose(Function<T, Callback<U>> mapper) {
        if (mapper == null) {
          throw new NullPointerException();
        }
        Callback<U> ret = Callback.callback();
        setHandler(ar -> {
            if (ar.success()) {
                Callback<U> apply;
                try {
                    apply = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                apply.setHandler(ret.handler());
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> Callback<T> castCallback(Handler<Callback<T>> handler) {
        return (Callback<T>)handler;
    }
    public static <T> Callback<T> callback() {
        return new DefaultCallback<>();
    }

    public static <T> Callback<T> callback(Handler<Callback<T>> handler) {
        DefaultCallback<T> cbh = new DefaultCallback<>();
        handler.handle(cbh);
        return cbh;
    }
    public static <T> Callback<T> succeed(T t) {
        return new DefaultCallback<T>().complete(t);
    }
    public static <T> Callback<T> failed(Throwable cause) {
        return new DefaultCallback<T>().fail(cause);
    }
    public static <T> Callback<T> failed(String message) {
        return new DefaultCallback<T>().fail(message);
    }

    interface CallbackHandler<T> extends Handler<Callback<T>> {
        T callback();
    }
    static class DefaultCallback<T> implements Callback<T>, Handler<Callback<T>> {
        private Handler<Callback<T>> handler;
        private Boolean succeeded = null;
        private T result;
        private Throwable throwable;
        @Override
        public T result() {
            return this.result;
        }

        @Override
        public Throwable cause() {
            return this.throwable;
        }

        @Override
        public boolean success() {
            return (succeeded != null && succeeded);
        }

        @Override
        public boolean fail() {
            return !success();
        }

        @Override
        public void handle(Callback<T> callback) {
            if(isComplete())
                throw new IllegalStateException("Result is already complete.");
            if (callback.success()) {
                complete(callback.result());
            } else {
                fail(callback.cause());
            }
        }

        @Override
        public Callback<T> setHandler(Handler<Callback<T>> handler) {
            boolean callHandler;
            synchronized (this) {
                this.handler = handler;
                callHandler = isComplete();
            }
            if (callHandler) {
                handler.handle(this);
            }
            return this;
        }

        @Override
        public boolean isComplete() {
            return (null != succeeded);
        }

        @Override
        public Callback<T> complete(T result) {
            if (!tryComplete(result)) {
                throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
            }
            return this;
        }

        @Override
        public Callback<T> complete() {
            complete(null);
            return this;
        }

        protected boolean tryComplete(T result) {
            Handler<Callback<T>> h = null;
            synchronized (this) {
                if (this.succeeded != null) {
                    return false;
                }
                this.result = result;
                this.succeeded = Boolean.TRUE;
                h = this.handler;
            }
            if (h != null) {
                h.handle(this);
            }
            return true;
        }

        @Override
        public Callback<T> fail(Throwable cause) {
            if (!tryFail(cause)) {
                throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
            }
            return this;
        }

        @Override
        public Callback<T> fail(String message) {
            if (!tryFail(new Throwable(message))) {
                throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
            }
            return this;
        }

        protected boolean tryFail(Throwable cause) {
            Handler<Callback<T>> h;
            synchronized (this) {
              if (this.succeeded != null) {
                return false;
              }
              this.throwable = cause != null ? cause : new Throwable();
              this.succeeded = Boolean.FALSE;
              h = this.handler;
            }
            if (h != null) {
              h.handle(this);
            }
            return true;
        }

        public Callback<T> callback() {
            return this;
        }
        @Override
        public Handler<Callback<T>> handler() {
            return this;
        }
    }
}
