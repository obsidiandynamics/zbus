package com.obsidiandynamics.zbus;

public interface ZPublisher extends SafeCloseable {
  void send(Object message);
}
