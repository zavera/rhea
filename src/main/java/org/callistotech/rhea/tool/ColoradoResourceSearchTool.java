package org.callistotech.rhea.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI tool the insurance-match agent can call to pull live results about
 * Colorado public assistance / health insurance programs via Tavily.
 */
@Component
public class ColoradoResourceSearchTool {

    private final TavilySearchClient tavilySearchClient;

    public ColoradoResourceSearchTool(TavilySearchClient tavilySearchClient) {
        this.tavilySearchClient = tavilySearchClient;
    }

    @Tool(description = "Search live web results for Colorado state public assistance and health "
            + "insurance programs (e.g. Health First Colorado, Connect for Health Colorado, CICP). "
            + "Colorado and unemployment status are always the two governing criteria for this search, "
            + "on top of whatever else the query asks. Use this to confirm current eligibility rules, "
            + "enrollment windows, or application links before recommending a program to a patient.")
    public String searchColoradoPublicResources(
            @ToolParam(description = "Search query, e.g. 'special enrollment period after job loss'")
            String query) {
        if (!tavilySearchClient.isConfigured()) {
            return "Live search is unavailable (TAVILY_API_KEY not configured). "
                    + "Answer using only the known-programs list already provided in the prompt.";
        }
        try {
            String scopedQuery = "Colorado unemployment " + query + " state program site:.gov OR site:.org";
            List<TavilySearchClient.TavilyResult> results = tavilySearchClient.search(scopedQuery);
            if (results.isEmpty()) {
                return "No live search results found for: " + query;
            }
            return results.stream()
                    .map(r -> "- %s (%s): %s".formatted(r.title(), r.url(), r.content()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Live search failed (" + e.getMessage() + "). "
                    + "Answer using only the known-programs list already provided in the prompt.";
        }
    }
}
