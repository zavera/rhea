package org.callistotech.rhea.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Tavily search API (https://tavily.com), used to pull
 * live results about Colorado public assistance / insurance programs.
 */
@Component
public class TavilySearchClient {

    private final RestClient restClient;
    private final String apiKey;

    public TavilySearchClient(
            @Value("${tavily.base-url}") String baseUrl,
            @Value("${tavily.api-key}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings("unchecked")
    public List<TavilyResult> search(String query) {
        if (!isConfigured()) {
            throw new IllegalStateException("TAVILY_API_KEY is not set");
        }

        Map<String, Object> request = Map.of(
                "api_key", apiKey,
                "query", query,
                "search_depth", "basic",
                "max_results", 5,
                "include_answer", false);

        Map<String, Object> response = restClient.post()
                .uri("/search")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("results") instanceof List<?> rawResults)) {
            return List.of();
        }

        return rawResults.stream()
                .filter(Map.class::isInstance)
                .map(o -> (Map<String, Object>) o)
                .map(m -> new TavilyResult(
                        String.valueOf(m.getOrDefault("title", "")),
                        String.valueOf(m.getOrDefault("url", "")),
                        String.valueOf(m.getOrDefault("content", ""))))
                .toList();
    }

    public record TavilyResult(String title, String url, String content) {
    }
}
