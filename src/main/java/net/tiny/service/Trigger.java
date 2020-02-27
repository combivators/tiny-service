package net.tiny.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;


public abstract class Trigger implements Statable {

    private List<Statable> requires = new ArrayList<>();
    private Statable.States states = Statable.States.NONE;
    private Consumer<Statable.States> consumer = null;

    public List<Statable> getRequires() {
        return requires;
    }

    public void setRequires(List<Statable> requires) {
        this.requires = requires;
    }

    @Override
    public States states() {
        return states;
    }

    @Override
    public void trigger(Consumer<States> c) {
        consumer = c;
        consumer.accept(states);
    }

    protected void states(States s) {
        states = s;
        if (consumer != null) {
            consumer.accept(states);
        }
    }

    protected void await() throws InterruptedException {
        if (requires == null || requires.isEmpty())
            return;
        final CountDownLatch latch = new CountDownLatch(requires.size());
        final Consumer<Statable.States> trigger = new Consumer<Statable.States>() {
            @Override
            public void accept(Statable.States s) {
                switch(s) {
                case ALIVE:
                    latch.countDown();
                    break;
                case DONE:
                    latch.countDown();
                    break;
                default:
                    break;
                }
            }};

        for (Statable s : requires) {
            s.trigger(trigger);
        }
        latch.await();
    }
}
