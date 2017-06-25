package com.obsidiandynamics.zbus.zmq;

import java.util.*;
import java.util.concurrent.*;

import com.obsidiandynamics.zbus.*;
import com.obsidiandynamics.zbus.codec.*;

public final class ZmqBus implements ZBus {
  private final String socketAddress;
  
  private final MessageCodec codec;
  
  private volatile ZmqSharedSocket sharedSocket;
  
  private final Set<SafeCloseable> endpoints = new CopyOnWriteArraySet<>();

  public ZmqBus(String socketAddress, MessageCodec codec) {
    this.socketAddress = socketAddress;
    this.codec = codec;
  }
  
  String getSocketAddress() {
    return socketAddress;
  }

  @Override
  public ZmqPublisher getPublisher(String topic) {
    final ZmqPublisher pub = new ZmqPublisher(this, topic, getOrCreateSharedSocket());
    endpoints.add(pub);
    return pub;
  }

  @Override
  public ZmqSubscriber getSubscriber(String topic) {
    final ZmqSubscriber sub = new ZmqSubscriber(this, topic);
    endpoints.add(sub);
    return sub;
  }
  
  void remove(SafeCloseable endpoint) {
    endpoints.remove(endpoint);
  }

  MessageCodec getCodec() {
    return codec;
  }
  
  private ZmqSharedSocket getOrCreateSharedSocket() {
    final ZmqSharedSocket existing = sharedSocket;
    if (existing != null) {
      return existing;
    } else {
      synchronized (this) {
        if (sharedSocket == null) {
          sharedSocket = new ZmqSharedSocket(socketAddress);
        }
        return sharedSocket;
      }
    }
  }

  @Override
  public void close() {
    for (SafeCloseable endpoint : endpoints) {
      endpoint.close();
    }
    if (sharedSocket != null) {
      sharedSocket.close();
    }
  }
  
  static String terminateTopic(String topic) {
    if (topic.contains("\n")) throw new IllegalArgumentException("Topic cannot contain the newline character");
    return topic + '\n';
  }
}
