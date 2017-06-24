package com.obsidiandynamics.zbus.codec;

public interface MessageCodec {
  String encode(Object obj);
  
  Object decode(String str);
}
