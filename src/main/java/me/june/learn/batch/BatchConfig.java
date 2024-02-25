package me.june.learn.batch;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.june.learn.batch.item.BatchDto;
import me.june.learn.batch.item.OpenSearchItemReader;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final KafkaTemplate<String, BatchDto> kafkaTemplate;

    private final OpenSearchClient client;

    @Value("${spring.batch.chunk.size}")
    private int chunkSize;

    @Bean
    public Job defaultJob(JobRepository jobRepository, Step defaultStep) {
        return new JobBuilder("defaultJob", jobRepository)
                .start(defaultStep)
                .build();
    }

    @Bean
    public Step defaultStep(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager){
        return new StepBuilder("defaultStep", jobRepository)
                .<BatchDto,BatchDto>chunk(chunkSize,platformTransactionManager)
                .reader(reader())
                .writer(writer())
                .build();

    }

    @Bean
    public OpenSearchItemReader<BatchDto> reader(){
        return new OpenSearchItemReader<>(client,BatchDto.class);
    }

    @Bean
    @SneakyThrows
    public KafkaItemWriter<String, BatchDto> writer()  {
        KafkaItemWriter<String, BatchDto> writer = new KafkaItemWriter<>();
        writer.setKafkaTemplate(kafkaTemplate);
        writer.setDelete(Boolean.FALSE);
        writer.setItemKeyMapper(BatchDto::id);
        writer.afterPropertiesSet();
        return writer;
    }
}

