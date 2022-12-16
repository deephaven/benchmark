package io.deephaven.verify.producer;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.deephaven.verify.connect.ColumnDefs;
import io.deephaven.verify.util.Metrics;
import io.deephaven.verify.util.Threads;

public class AvroKafkaGenerator implements Generator {
	final private ExecutorService queue = Threads.single("AvroKafkaGenerator");
	final private Producer<String, GenericRecord> producer;
	final private ColumnDefs columnDefs;
	final private AvroSchema schema;
	final private String topic;
	final private AtomicBoolean isClosed = new AtomicBoolean(false);

	public AvroKafkaGenerator(String bootstrapServers, String schemaRegistryUrl, String topic, ColumnDefs columnDefs) {
		cleanupTopic(bootstrapServers, schemaRegistryUrl, topic);
		this.producer = createProducer(bootstrapServers, schemaRegistryUrl);
		this.topic = topic;
		this.columnDefs = columnDefs;
		this.schema = publishSchema(topic, schemaRegistryUrl, getSchemaJson(topic, columnDefs));
	}

	//ASync method to generate data.
	public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs) {
		checkClosed();
		var r = new Callable<Metrics>() {
			@Override
			public Metrics call() {
				final long maxDuration = maxDurationSecs * 1000;
				final long beginTime = System.currentTimeMillis();
				long recCount = 0;
				long duration = 0;
				boolean isDone = false;
				while(!isClosed.get() && !isDone) {
					try {
						if(recCount >= maxRecordCount) {
							isDone = true;
							continue;
						}
						GenericRecord rec = new GenericData.Record(schema.rawSchema());
						for(int i = 0, n = columnDefs.getCount(); i < n; i++) {
							rec.put(i, columnDefs.nextValue(i, recCount));
						}
						producer.send(new ProducerRecord<>(topic, rec));
						if(perRecordPauseMillis <= 0) Thread.yield();
						else Threads.sleep(perRecordPauseMillis);
						
						++recCount;
						duration = System.currentTimeMillis() - beginTime;
						if(duration > maxDuration) isDone = true;
					} catch(Exception ex) {
						throw new RuntimeException("Failed to send to topic: " + topic, ex);
					}
				}
				Metrics metrics = new Metrics(topic, "generator").set("duration.secs", duration / 1000.0)
					.set("record.count", recCount).set("send.rate", recCount / (duration / 1000.0));
				return metrics;
			}
		};
		return queue.submit(r);
	}

	public void produce(Map<String,Object> row) {
		checkClosed();
		GenericRecord rec = new GenericData.Record(schema.rawSchema());
		row.entrySet().stream().forEach(e-> {
			rec.put(e.getKey(), e.getValue());
		});
		
		try {
			producer.send(new ProducerRecord<>(topic, rec));
		} catch(Exception ex) {
			throw new RuntimeException("Failed to send to topic: " + topic + " record: " + row.toString(), ex);
		}
	}
	
	public void close() {
		if(isClosed.get()) return;
		isClosed.set(true);
		queue.shutdown();
		producer.flush();
		producer.close();
	}
	
	private void checkClosed() {
		if(isClosed.get()) throw new RuntimeException("Generator is closed");
	}
	
	private Producer<String, GenericRecord> createProducer(String bootstrapServer, String schemaRegistryUrl) {
		Properties props = new Properties();
		props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
		props.put("schema.registry.url", schemaRegistryUrl);
		props.put(ACKS_CONFIG, "0");
		props.put(COMPRESSION_TYPE_CONFIG, "zstd");
		props.put(BATCH_SIZE_CONFIG, 16384 * 4);
		props.put(BUFFER_MEMORY_CONFIG, 32 * 1024 * 1024L * 4);
		props.put(LINGER_MS_CONFIG, 200);
		return new KafkaProducer<>(props);
	}
	
	private void cleanupTopic(String bootstrapServers, String schemaRegistryUrl, String topic) {
		var admin = new KafkaAdmin(bootstrapServers, schemaRegistryUrl);
		admin.deleteTopic(topic);
		long messageCount = admin.getMessageCount(topic);
		if(messageCount > 0) throw new RuntimeException("Failed to delete topic: " + topic + "=" + messageCount + " msgs");
	}
	
	private AvroSchema publishSchema(String topic, String schemaRegistryUrl, String schemaJson) {
		try {
			AvroSchema schema = new AvroSchema(schemaJson);
			CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegistryUrl, 20);
			String subject = topic + "_record";
			for(String s: client.getAllSubjects()) {
				if(s.equals(subject)) {
					client.deleteSubject(s, false);
					client.deleteSubject(s, true);
				}
			}
			client.register(subject, schema);
			return schema;
		} catch(Exception ex) {
			throw new RuntimeException("Failed to publish schema: " + schemaRegistryUrl, ex);
		}
	}
	
	private String getSchemaJson(String topic, ColumnDefs fieldDefs) {
		var schema = "{ 'type' : 'record',\n" + 
			"  'namespace' : 'io.deephaven.verify',\n" + 
			"  'name' : '" + topic + "',\n" +
			"  'fields' : [\n";
		
		for(Map.Entry<String,String> e: fieldDefs.toTypeMap().entrySet()) {
			schema += "    { 'name' : '" + e.getKey() + "', 'type' : '" + e.getValue() + "' },\n";
		};
		
		schema = schema.replaceAll(",\n$","\n") + "  ]\n}\n";
		return schema.replace("'", "\"");
	}

}
