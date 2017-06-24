package com.obsidiandynamics.zbus;

import java.util.function.*;

public final class AsyncSubscriber extends Thread implements SafeCloseable {
  private final Supplier<ZSubscriber> factory;
  
  private Consumer<Object> receiver;
  
  private volatile ZSubscriber subscriber;
  
  private AsyncSubscriber(Supplier<ZSubscriber> factory) {
    super("AsyncSubsriber");
    this.factory = factory;
  }
  
  public static AsyncSubscriber using(Supplier<ZSubscriber> factory) {
    return new AsyncSubscriber(factory);
  }
  
  public synchronized AsyncSubscriber onReceive(Consumer<Object> receiver) {
    if (this.receiver != null) {
      throw new IllegalStateException("Subscriber already running");
    }
    this.receiver = receiver;
    start();
    return this;
  }
  
  @Override
  public void run() {
    subscriber = factory.get();
    for (;;) {
      final Object r = subscriber.receive();
      if (r != null) {
        receiver.accept(r);
      } else {
        break;
      }
    }
  }

  @Override
  public void close() {
    subscriber.close();
    try {
      join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
