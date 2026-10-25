package com.resumeanalyzer.analysis.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

class OpenAiLlmAnalysisServiceTest {

    private static final String ENDPOINT = "https://api.openai.test/v1/chat/completions";
    private static final String API_KEY = "test-api-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsStructuredRequestAndParsesOpenAiResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiLlmAnalysisService service = new OpenAiLlmAnalysisService(
                builder.build(),
                objectMapper,
                API_KEY,
                "gpt-test",
                ENDPOINT);
        ResumeAnalysisResult expected = new ResumeAnalysisResult(
                82,
                "Strong Java experience; AWS experience is not shown.",
                List.of("Java"),
                List.of("AWS"),
                List.of("backend"),
                List.of("cloud"),
                List.of("Add relevant cloud project details if applicable."));
        String responseBody = openAiResponse(objectMapper.writeValueAsString(expected));

        server.expect(requestTo(ENDPOINT))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.max_completion_tokens").value(3000))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Resume text")))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ResumeAnalysisResult actual = service.analyze("Resume text", "Job description");

        assertEquals(expected, actual);
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyWithoutSendingARequest() {
        OpenAiLlmAnalysisService service = new OpenAiLlmAnalysisService(
                RestClient.builder().build(),
                objectMapper,
                " ",
                "gpt-test",
                ENDPOINT);

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> service.analyze("Resume text", "Job description"));

        assertEquals(AnalysisException.Reason.PROVIDER_NOT_CONFIGURED, exception.getReason());
    }

    @Test
    void rejectsInputsOverTheConfiguredSafeLimitWithoutSendingARequest() {
        OpenAiLlmAnalysisService service = new OpenAiLlmAnalysisService(
                RestClient.builder().build(),
                objectMapper,
                API_KEY,
                "gpt-test",
                ENDPOINT);

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> service.analyze("r".repeat(100_000), "j"));

        assertEquals(AnalysisException.Reason.REQUEST_TOO_LARGE, exception.getReason());
    }

    @Test
    void reportsProviderRateLimitAsAnExplicitProviderFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiLlmAnalysisService service = new OpenAiLlmAnalysisService(
                builder.build(),
                objectMapper,
                API_KEY,
                "gpt-test",
                ENDPOINT);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(TOO_MANY_REQUESTS));

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> service.analyze("Resume text", "Job description"));

        assertEquals(AnalysisException.Reason.PROVIDER_FAILURE, exception.getReason());
        server.verify();
    }

    @Test
    void rejectsMalformedProviderOutput() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiLlmAnalysisService service = new OpenAiLlmAnalysisService(
                builder.build(),
                objectMapper,
                API_KEY,
                "gpt-test",
                ENDPOINT);
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(openAiResponse("{\"overallScore\":120}"), MediaType.APPLICATION_JSON));

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> service.analyze("Resume text", "Job description"));

        assertEquals(AnalysisException.Reason.PROVIDER_FAILURE, exception.getReason());
        server.verify();
    }

    private String openAiResponse(String content) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "choices",
                List.of(Map.of(
                        "finish_reason", "stop",
                        "message", Map.of("content", content)))));
    }
}
