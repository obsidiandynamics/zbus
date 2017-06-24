package com.obsidiandynamics.zbus;

public interface SafeCloseable extends AutoCloseable {
  @Override
  void close();
}
