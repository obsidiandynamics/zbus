package com.obsidiandynamics.zbus;

public interface ZSubscriber extends SafeCloseable {
  /**
   *  Blocks, waiting for a new object to be received.
   *  
   *  @return The received object, or <code>null</code> if the receive was aborted.
   */
  Object receive();
}
