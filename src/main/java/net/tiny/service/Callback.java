package net.tiny.service;


public interface Callback<T> {
    boolean isComplete();
    T result();
    Throwable cause();
    boolean success();
    boolean fail();
    Callback<T> complete(T result);
    Callback<T> complete();
    Callback<T> success(String message);
    Callback<T> fail(Throwable cause);
    Callback<T> fail(String message);
    public static <T> Callback<T> succeed(T t) {
        return new DefaultCallback<T>().complete(t);
    }
    public static <T> Callback<T> failed(Throwable cause) {
        return new DefaultCallback<T>().fail(cause);
    }
    public static <T> Callback<T> failed(String message) {
        return new DefaultCallback<T>().fail(message);
    }

    static class DefaultCallback<T> implements Callback<T> {
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


        @Override
        public Callback<T> success(String message) {
            if (!tryComplete(result)) {
                throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
            }
            return this;
        }

        public Callback<T> callback() {
            return this;
        }

        protected boolean tryComplete(T result) {
            synchronized (this) {
                if (this.succeeded != null) {
                    return false;
                }
                this.result = result;
                this.succeeded = Boolean.TRUE;
            }
            return true;
        }

        protected boolean tryFail(Throwable cause) {
            synchronized (this) {
              if (this.succeeded != null) {
                return false;
              }
              this.throwable = cause != null ? cause : new Throwable();
              this.succeeded = Boolean.FALSE;
            }
            return true;
        }
    }
}
