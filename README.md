Z/bus
===
[ ![Download](https://api.bintray.com/packages/obsidiandynamics/zbus/zbus-core/images/download.svg) ](https://bintray.com/obsidiandynamics/zbus/zbus-core/_latestVersion)

Z/bus is a simple library for building distributed applications using a pub/sub pattern over [0MQ](http://zeromq.org/).

# Why Z/bus
0MQ if fast, flexible and by most accounts a pretty awesome little library for building distributed applications. It has but one Achilles' heel; it isn't thread-safe. That's an understatement. Consider an `ArrayList` which we all know isn't thread-safe, but you can wrap it in a `synchronized` block or guard it with a `Lock`. By contrast, 0MQ blows up or hangs if you attempt to access its components from different threads, regardless of how you choose to manage contention. That's because behind the scenes, 0MQ sockets are assigned to specific 'owner' threads; bad things happen if this relationship is broken.

# How it works
Z/bus wraps the 0MQ publisher socket in a queue to serialise writes, thereby ensuring that a socket is always written to from its 'owner' thread. Proper multi-threading support is provided, with each writer thread getting its own `ZPublisher` endpoint, but behind the scenes the writes flow through to a 'shared', single-threaded socket queue.

Conversely, subscriber sockets are wrapped in handlers that validate ownership rules, and detect interruption (e.g. a socket close or context termination) and handle this gracefully. As per 0MQ's rules, you can only receive messages from the same thread that opened the socket, but failing to do so is now communicated with an exception, and doesn't result in problems down the track. Also, you can close the subscriber endpoint and interrupt the receiver from any thread without dire side-effects.

# Codecs
Aside from arbitrating 0MQ socket access, Z/bus offers an object mapping layer, allowing the application to deal directly with objects, rather than strings or byte arrays. This is done by specifying a `MessageCodec` - a user-defined mapping from objects to strings. The simplest is a `StringMessageCodec`, which is effectively a pass-through - taking in and putting out `String`s, thus giving you full control how messages are serialised at the application layer.

Alternatively, you can use the built-in `GsonCodec` and `GensonCodec` to leverage the [Gson](https://github.com/google/gson) and [Genson](https://owlike.github.io/genson/) libraries respectively. Or you can roll your own, by simply implementing the `MessageCodec` interface.

# Getting started
## Get the binaries
Z/bus builds are hosted on JCenter (MavenCentral is coming soon). Just add the following snippet to your build file (replacing the version number in the snippet with the version shown on the Download badge at the top of this README).

For Maven:

```xml
<dependency>
  <groupId>com.obsidiandynamics.zbus</groupId>
  <artifactId>zbus-core</artifactId>
  <version>0.1.0</version>
  <type>pom</type>
</dependency>
```

For Gradle:

```groovy
compile 'com.obsidiandynamics.zbus:zbus-core:0.1.0'
```

## Hello world
Note: the examples below assume the following imports:

```java
import com.obsidiandynamics.zbus.*;
import com.obsidiandynamics.zbus.codec.*;
import com.obsidiandynamics.zbus.zmq.*;
```

Let's write a basic 'hello world' pub/sub application.

Starting with the publisher...

```java
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
```

And now the subscriber...

```java
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
```

Start these examples as two separate process. (The order isn't relevant, as 0MQ allows for either party to operate independent of the other.) You should see the text 'hello' being printed in one second intervals.

Both examples have the first line in common: `ZBus bus = new ZmqBus("tcp://*:5557", new StringCodec())`. This creates a new instance of a Z/bus listening on port 5557 across all interfaces, using direct string (un)marshaling. The bus can act as either the publisher or a subscriber (but never both within the same process), depending on how you use it. The publisher example creates a new publisher with `ZBus.getPublisher(String topic)` while the subscriber does `ZBus.getSubscriber(String topic)`. The topics must match exactly, or the messages will not flow through.

> Note: The underlying 0MQ library is more relaxed with respect to topic matching, allowing for subscriptions to a leading substring of the published topic, rather to the exact topic string. This could be used to implement topic hierarchies. At this stage Z/bus is stricter, requiring an exact topic match and only a single topic per subscriber. This could be improved in the future.

Next, the publisher goes into an endless loop, sending the string `hello` using `ZPublisher.send(Object message)` at one second intervals. Conversely, the subscriber hangs a read on `ZSubscriber.receive()`, which returns when a message is received (or `null` if the connection is terminated on the subscriber's end).

Afterwards both parties clean up by closing both the publisher/subscriber and the underlying bus. Note: closing the bus will also have the effect of closing all endpoints.

# Async subscriber
The blocking receive is a fairly standard pattern for receiving messages in 0MQ, and is achieved with a conventional `ZSubscriber`. As an added convenience, Z/bus also offers the `AsyncSubscriber` - a dispatcher that uses the callback pattern, thus avoiding the need to block.

```java
public static void main(String[] args) {
  final ZBus bus = new ZmqBus("tcp://*:5557", new StringCodec());

  System.out.println("Started Z/bus (async) subscriber");
  
  final AsyncSubscriber sub = AsyncSubscriber
  .using(() -> bus.getSubscriber("testTopic"))
  .onReceive(msg -> {
    System.out.println("Received " + msg);
  });
  
  try {
    Thread.sleep(Long.MAX_VALUE);
  } catch (InterruptedException e) {
    System.out.println("Subscriber exiting");
  }
  
  sub.close();
  bus.close();
}
```

The example above is similar to our first subscriber, but in this case there is no `ZSubscriber` instantiated. Instead, we use `AsyncSubscriber`, passing it a factory for creating a `ZSubscriber` instance via a lambda expression. The `onReceive()` method is then called, supplying a handler for receiving messages. That's it, Z/bus will now start receiving messages and delegating them onto the `onReceive` handler.

> Note: The factory here is crucial, as 0MQ requires that the socket receiver must be the same thread that opened the socket. Because `AsyncSubscriber` runs in a dedicated thread, it must be the one that opens the socket maintains custody over it. Like a conventional subscriber, an `AsyncSubscriber` can be closed by any thread.