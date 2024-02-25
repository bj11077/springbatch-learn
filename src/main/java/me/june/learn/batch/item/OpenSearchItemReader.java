package me.june.learn.batch.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
public class OpenSearchItemReader<T> implements ItemReader<T> {

    private final OpenSearchClient client;

    @Value("${opensearch.index}")
    private String index;

    @Value("${opensearch.timefield}")
    private String timeField;

    @Value("${spring.batch.chunk.size}")
    private int PAGE_SIZE;
    private int currentIndex = 0;

    private int sum = 0;

    private int currentSize = 0;

    private SearchResponse<T> response;

    private final Class<T> targetType;

    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        if (response == null || (currentIndex >= currentSize) && sum < response.hits().total().value() ) {
            getNextData();
        }
        if (currentIndex < currentSize) {
            Hit<T> hit = response.hits().hits().get(currentIndex++);
            sum++;
            return hit.source();
        }else {
            log.info("task end");
            return null;
        }
    }

    private void getNextData() throws IOException {
        SearchRequest request = new SearchRequest.Builder().index(index)
                .query(builder ->
                        builder.range(r->
                                r.field(timeField).gte(JsonData.of(LocalDate.now().minusDays(1)
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))))
                .from(sum).size(PAGE_SIZE).build();
        response = client.search(request, targetType);
        currentSize = response.hits().hits().size();
        currentIndex = 0;
    }
}
