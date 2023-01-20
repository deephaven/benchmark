package io.deephaven.benchmark.producer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

public class KafkaAdmin {
	final Properties props;
	
	public KafkaAdmin(String bootstrapServers, String schemaRegistryUrl) {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("key.deserializer", StringDeserializer.class);
		props.put("value.deserializer", KafkaAvroDeserializer.class);
		props.put("schema.registry.url", schemaRegistryUrl);
		props.put("request.timeout.ms", 5000);
		props.put("consumer.request.timeout.ms", 5000);
		props.put("default.api.timeout.ms", 5000);
		this.props = props;
	}
	
	public KafkaAdmin(Properties props) {
		this.props = props;
	}
	
	public void deleteTopic(String topic) {
		try(AdminClient admin = KafkaAdminClient.create(props)) {
			Set<String> names = admin.listTopics().names().get();
			if(names.contains(topic)) admin.deleteTopics(List.of(topic)).all().get();
			
		} catch(Exception ex) {
			throw new RuntimeException("Failed to delete topic: " + topic, ex);
		}
	}

	public long getMessageCount(String topic) {
		try(KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props)) {
			List<TopicPartition> partitions = consumer.partitionsFor(topic).stream().map(p -> new TopicPartition(topic, p.partition())).toList();
			consumer.assign(partitions);
			consumer.seekToEnd(Collections.emptySet());
			Map<TopicPartition, Long> endPartitions = partitions.stream().collect(Collectors.toMap(Function.identity(), consumer::position));
			return partitions.stream().mapToLong(p -> endPartitions.get(p)).sum();
		} catch(Exception ex) {
			throw new RuntimeException("Failed to get topic message count: " + topic, ex);
		}
	}
	
}
