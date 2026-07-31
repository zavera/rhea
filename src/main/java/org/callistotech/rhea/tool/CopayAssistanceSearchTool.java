package org.callistotech.rhea.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI tool the copay-assistance agent can call to pull live results about manufacturer
 * and foundation copay assistance programs via Tavily. Same pattern as
 * {@link ColoradoResourceSearchTool}, scoped to national copay assistance instead of a
 * single state's public programs.
 */
@Component
public class CopayAssistanceSearchTool {

    private final TavilySearchClient tavilySearchClient;

    public CopayAssistanceSearchTool(TavilySearchClient tavilySearchClient) {
        this.tavilySearchClient = tavilySearchClient;
    }

    @Tool(description = "Search live web results for manufacturer or foundation copay assistance programs "
            + "for a specific medication (e.g. manufacturer copay card, PAN Foundation, HealthWell Foundation, "
            + "Patient Advocate Foundation Co-Pay Relief). Use this to confirm current eligibility rules, "
            + "fund availability, or application links before recommending a program to a patient.")
    public String searchCopayAssistancePrograms(
            @ToolParam(description = "Search query, e.g. 'adalimumab manufacturer copay card eligibility'")
            String query) {
        if (!tavilySearchClient.isConfigured()) {
            return "Live search is unavailable (TAVILY_API_KEY not configured). "
                    + "Answer using only the known-programs list already provided in the prompt.";
        }
        try {
            String scopedQuery = "copay assistance program " + query + " site:.org OR site:.com";
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
