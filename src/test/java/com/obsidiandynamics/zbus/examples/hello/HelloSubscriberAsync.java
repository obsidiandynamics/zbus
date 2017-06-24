package com.obsidiandynamics.zbus.examples.hello;

import com.obsidiandynamics.zbus.*;
import com.obsidiandynamics.zbus.codec.*;
import com.obsidiandynamics.zbus.zmq.*;

public final class HelloSubscriberAsync {
  public static void main(String[] args) {
    final ZBus bus = new ZmqBus("tcp://*:5557", new StringCodec());

    System.out.println("Started Z/bus (async) subscriber");
    
    final AsyncSubscriber sub = AsyncSubscriber
    .using(() -> bus.getSubscriber("testTopic"))
    .onReceive(msg -> {
      System.out.println("Received " + msg);
    });
    
    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      System.out.println("Subscriber exiting");
    }
    
    sub.close();
    bus.close();
  }
}
