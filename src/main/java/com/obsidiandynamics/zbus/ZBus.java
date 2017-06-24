package com.obsidiandynamics.zbus;

public interface ZBus extends SafeCloseable {
  ZPublisher getPublisher(String topic);
  
  ZSubscriber getSubscriber(String topic);
}
