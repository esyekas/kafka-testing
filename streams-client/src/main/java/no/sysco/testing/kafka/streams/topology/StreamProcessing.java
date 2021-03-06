package no.sysco.testing.kafka.streams.topology;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.state.KeyValueStore;

public class StreamProcessing {

  // stateless
  public static Topology topologyUpperCase(final String sourceTopic, final String sinkTopic) {
    final StreamsBuilder streamsBuilder = new StreamsBuilder();
    final KStream<String, String> sourceStream =
        streamsBuilder.stream(sourceTopic, Consumed.with(Serdes.String(), Serdes.String()));

    sourceStream
        .mapValues((ValueMapper<String, String>) String::toUpperCase)
        .to(sinkTopic, Produced.with(Serdes.String(), Serdes.String()));
    return streamsBuilder.build();
  }

  // stateful
  public static Topology topologyCountAnagram(
      final String sourceTopic,
      final String sinkTopic,
      final String storeName) {
    final StreamsBuilder streamsBuilder = new StreamsBuilder();
    final KStream<String, String> sourceStream =
        streamsBuilder.stream(sourceTopic, Consumed.with(Serdes.String(), Serdes.String()));
    // 1. [null:"magic"] => ["acgim":"magic"]
    // 2. amount with same key
    sourceStream
        .map(
            (key, value) -> {
              final String newKey =
                  Stream.of(value.replaceAll(" ", "").split(""))
                      .sorted()
                      .collect(Collectors.joining());
              return KeyValue.pair(newKey, value);
            })
        .groupByKey()
        .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(storeName))
        .toStream()
        .to(sinkTopic, Produced.with(Serdes.String(), Serdes.Long()));
    return streamsBuilder.build();
  }
}
