package com.obsidiandynamics.zbus.zmq;

import org.zeromq.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.*;

import com.obsidiandynamics.zbus.*;

import zmq.*;

public final class ZmqSubscriber implements ZSubscriber {
  private final OwnerThread owner = new OwnerThread();
  
  private final ZmqBus bus;
  
  private final String terminatedTopic;
  
  private final Context context;
  
  private final Socket socket;

  ZmqSubscriber(ZmqBus bus, String topic) {
    this.bus = bus;
    terminatedTopic = ZmqBus.terminateTopic(topic);
    context = ZMQ.context(1);
    socket = context.socket(ZMQ.SUB);
    socket.connect(bus.getSocketAddress());
    socket.setHWM(0);
    socket.subscribe(topic.getBytes(ZMQ.CHARSET));
  }
  
  @Override
  public Object receive() {
    owner.verifyCurrent();
    
    final String str;
    try {
      str = socket.recvStr();
    } catch (ZMQException e) {
      if (e.getErrorCode() == ZError.ETERM) {
        socket.setLinger(0);
        socket.close();
        return null;
      } else {
        throw e;
      }
    }
    
    if (str != null) {
      final String encoded = str.substring(terminatedTopic.length());
      return bus.getCodec().decode(encoded);
    } else {
      return null;
    }
  }
  
  @Override
  public void close() {
    if (owner.isCurrent()) {
      socket.setLinger(0);
      socket.close();
    }
    context.term();
    bus.remove(this);
  }
}
