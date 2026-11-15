package com.team.corporate.test.backend;

import com.team.corporate.backend.exceptions.GlobalExceptionHandler;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class GlobalExceptionHandlerTest {
    private static final String FAILURE_PATH = "/test/failure";
    private static final String SERVICE_MESSAGE = "Запись с указанным идентификатором не найдена.";
    private static final String INTERNAL_DETAIL = "Не удалось выполнить операцию. Повторите попытку позже.";
    private static final String SENSITIVE_DETAIL = "SQL password=secret /private/exports/users.csv";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private FailureController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        controller = new FailureController();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @MethodSource("notFoundExceptions")
    void missingResourcesReturn404WithServiceDetail(RuntimeException exception) throws Exception {
        controller.failure = exception;
        JsonNode problem = assertProblem(mvc.perform(get(FAILURE_PATH)).andReturn(), HttpStatus.NOT_FOUND);
        assertEquals(SERVICE_MESSAGE, problem.get("detail").asString());
    }

    static Stream<RuntimeException> notFoundExceptions() {
        return Stream.of(
                new CategoryNotFoundException(SERVICE_MESSAGE),
                new InventoryNotFoundException(SERVICE_MESSAGE),
                new OrderItemNotFoundException(SERVICE_MESSAGE),
                new OrderNotFoundException(SERVICE_MESSAGE),
                new ProductNotFoundException(SERVICE_MESSAGE),
                new UserNotFoundException(SERVICE_MESSAGE),
                new WarehouseNotFoundException(SERVICE_MESSAGE)
        );
    }

    @Test
    void validationPreservesServiceDetailButOmitsCauseAndQueryString() throws Exception {
        controller.failure = new IllegalArgumentException("Количество: минимум 1.", new SQLException(SENSITIVE_DETAIL));
        MvcResult result = mvc.perform(get(FAILURE_PATH).queryParam("token", "secret")).andReturn();
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals("Количество: минимум 1.", problem.get("detail").asString());
        assertFalse(result.getResponse().getContentAsString().contains("secret"));
    }

    @Test
    void exceptionWithoutDetailUsesStatusPhrase() throws Exception {
        controller.failure = new IllegalArgumentException();
        JsonNode problem = assertProblem(mvc.perform(get(FAILURE_PATH)).andReturn(), HttpStatus.BAD_REQUEST);
        assertEquals("Bad Request", problem.get("detail").asString());
    }

    @ParameterizedTest
    @MethodSource("conflictExceptions")
    void databaseConstraintsAndConcurrentWritesReturn409WithoutDatabaseDetails(RuntimeException exception) throws Exception {
        controller.failure = exception;
        MvcResult result = mvc.perform(get(FAILURE_PATH)).andReturn();
        assertProblem(result, HttpStatus.CONFLICT);
        assertFalse(result.getResponse().getContentAsString().contains(SENSITIVE_DETAIL));
    }

    static Stream<RuntimeException> conflictExceptions() {
        return Stream.of(
                new DataIntegrityViolationException(SENSITIVE_DETAIL, new SQLException(SENSITIVE_DETAIL)),
                new DuplicateKeyException(SENSITIVE_DETAIL),
                new OptimisticLockingFailureException(SENSITIVE_DETAIL),
                new PessimisticLockingFailureException(SENSITIVE_DETAIL),
                new CannotAcquireLockException(SENSITIVE_DETAIL)
        );
    }

    @ParameterizedTest
    @MethodSource("unavailableExceptions")
    void unavailableDatabaseReturns503WithoutConnectionDetails(RuntimeException exception) throws Exception {
        controller.failure = exception;
        JsonNode problem = assertProblem(mvc.perform(get(FAILURE_PATH)).andReturn(), HttpStatus.SERVICE_UNAVAILABLE);
        assertEquals("Сервис временно недоступен. Повторите попытку позже.", problem.get("detail").asString());
    }

    static Stream<RuntimeException> unavailableExceptions() {
        return Stream.of(
                new DataAccessResourceFailureException(SENSITIVE_DETAIL),
                new InternalAuthenticationServiceException(SENSITIVE_DETAIL, new SQLException(SENSITIVE_DETAIL)),
                new TransientDataAccessResourceException(SENSITIVE_DETAIL)
        );
    }

    @ParameterizedTest
    @MethodSource("internalExceptions")
    void unexpectedAndExportFailuresReturnGeneric500(Exception exception) throws Exception {
        controller.failure = exception;
        JsonNode problem = assertProblem(mvc.perform(get(FAILURE_PATH)).andReturn(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(INTERNAL_DETAIL, problem.get("detail").asString());
    }

    static Stream<Exception> internalExceptions() {
        return Stream.of(
                new IOException(SENSITIVE_DETAIL),
                new SecurityException(SENSITIVE_DETAIL),
                new IllegalStateException(SENSITIVE_DETAIL),
                new NullPointerException(SENSITIVE_DETAIL),
                new DataRetrievalFailureException(SENSITIVE_DETAIL),
                new HttpMessageNotWritableException(SENSITIVE_DETAIL)
        );
    }

    @Test
    void malformedJsonReturns400Problem() throws Exception {
        MvcResult result = mvc.perform(post("/test/orders")
                .contentType(MediaType.APPLICATION_JSON).content("{invalid")).andReturn();
        assertProblem(result, HttpStatus.BAD_REQUEST);
        assertFalse(result.getResponse().getContentAsString().contains("{invalid"));
    }

    @Test
    void invalidEnumReturns400Problem() throws Exception {
        MvcResult result = mvc.perform(post("/test/orders")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNKNOWN\"}")).andReturn();
        assertProblem(result, HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingBodyReturns400Problem() throws Exception {
        assertProblem(mvc.perform(post("/test/orders").contentType(MediaType.APPLICATION_JSON)).andReturn(),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidUuidReturns400Problem() throws Exception {
        assertProblem(mvc.perform(get("/test/users/not-a-uuid")).andReturn(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingRequestParameterReturns400Problem() throws Exception {
        assertProblem(mvc.perform(get("/test/search")).andReturn(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unsupportedMethodRetainsAllowHeader() throws Exception {
        MvcResult result = mvc.perform(put("/test/orders")).andReturn();
        assertProblem(result, HttpStatus.METHOD_NOT_ALLOWED);
        assertEquals("POST", result.getResponse().getHeader(HttpHeaders.ALLOW));
    }

    @Test
    void unsupportedMediaTypeReturns415Problem() throws Exception {
        assertProblem(mvc.perform(post("/test/orders").contentType(MediaType.TEXT_PLAIN).content("order")).andReturn(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void missingRouteReturns404Problem() throws Exception {
        assertProblem(mvc.perform(get("/test/missing")).andReturn(), HttpStatus.NOT_FOUND);
    }

    @Test
    void explicitHttpStatusIsPreservedByFallback() throws Exception {
        controller.failure = new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещён.");
        JsonNode problem = assertProblem(mvc.perform(get(FAILURE_PATH)).andReturn(), HttpStatus.FORBIDDEN);
        assertEquals("Доступ запрещён.", problem.get("detail").asString());
    }

    @Test
    void jsonAcceptHeaderStillProducesProblemJson() throws Exception {
        controller.failure = new ProductNotFoundException(SERVICE_MESSAGE);
        assertProblem(mvc.perform(get(FAILURE_PATH).accept(MediaType.APPLICATION_JSON)).andReturn(), HttpStatus.NOT_FOUND);
    }

    private JsonNode assertProblem(MvcResult result, HttpStatus status) throws Exception {
        assertEquals(status.value(), result.getResponse().getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, result.getResponse().getContentType());
        JsonNode problem = JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        if (problem.has("type")) {
            assertEquals("about:blank", problem.get("type").asString());
        }
        assertEquals(status.getReasonPhrase(), problem.get("title").asString());
        assertEquals(status.value(), problem.get("status").asInt());
        assertEquals(result.getRequest().getRequestURI(), problem.get("instance").asString());
        assertFalse(problem.get("detail").asString().isBlank());
        assertFalse(problem.has("exception"));
        assertFalse(problem.has("trace"));
        assertFalse(problem.has("message"));
        return problem;
    }

    @RestController
    static class FailureController {
        private Exception failure;

        @GetMapping(FAILURE_PATH)
        void fail() throws Exception {
            throw failure;
        }

        @PostMapping(value = "/test/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
        void createOrder(@RequestBody OrderRequest request) {
        }

        @GetMapping("/test/users/{id}")
        void user(@PathVariable("id") UUID id) {
        }

        @GetMapping("/test/search")
        void search(@RequestParam("quantity") int quantity) {
        }
    }

    record OrderRequest(OrderStatus status) {
    }
}
