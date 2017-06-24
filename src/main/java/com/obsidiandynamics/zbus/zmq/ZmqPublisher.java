package com.obsidiandynamics.zbus.zmq;

import com.obsidiandynamics.zbus.*;

public final class ZmqPublisher implements ZPublisher {
  private final ZmqBus bus;
  
  private final String topic;
  
  private final ZmqSharedSocket sharedSocket;
  
  ZmqPublisher(ZmqBus bus, String topic, ZmqSharedSocket sharedSocket) {
    this.bus = bus;
    this.topic = topic;
    this.sharedSocket = sharedSocket;
  }
  
  @Override
  public void send(Object message) {
    if (message == null) throw new NullPointerException("Message cannot be null");
    final String encoded = bus.getCodec().encode(message);
    final String payload = topic + " " + encoded;
    sharedSocket.send(payload);
  }
  
  @Override
  public void close() {
    bus.remove(this);
  }
}
