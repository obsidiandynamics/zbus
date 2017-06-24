package com.obsidiandynamics.zbus.examples.hello;

import com.obsidiandynamics.zbus.*;
import com.obsidiandynamics.zbus.codec.*;
import com.obsidiandynamics.zbus.zmq.*;

public final class HelloPublisher {
  public static void main(String[] args) {
    final ZBus bus = new ZmqBus("tcp://*:5557", new StringCodec());
    final ZPublisher pub = bus.getPublisher("testTopic");

    System.out.println("Started Z/bus publisher");
    
    while (true) {
      pub.send("hello");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        System.out.println("Publisher exiting");
        break;
      }
    }
    
    pub.close();
    bus.close();
  }
}
