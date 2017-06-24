package com.obsidiandynamics.zbus.examples.hello;

import com.obsidiandynamics.zbus.*;
import com.obsidiandynamics.zbus.codec.*;
import com.obsidiandynamics.zbus.zmq.*;

public final class HelloSubscriberSync {
  public static void main(String[] args) {
    final ZBus bus = new ZmqBus("tcp://*:5557", new StringCodec());
    final ZSubscriber sub = bus.getSubscriber("testTopic");

    System.out.println("Started Z/bus (sync) subscriber");
    
    while (true) {
      final Object received = sub.receive();
      if (received == null) break;
      
      System.out.println("Received " + received);
    }
    
    sub.close();
    bus.close();
  }
}
