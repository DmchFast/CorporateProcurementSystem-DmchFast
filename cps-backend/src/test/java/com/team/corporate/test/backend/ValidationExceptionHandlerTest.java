package com.team.corporate.test.backend;

import com.team.corporate.backend.exceptions.GlobalExceptionHandler;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ValidationExceptionHandlerTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SENSITIVE_DETAIL = "private-password-value";
    private static final String INTERNAL_DETAIL = "Не удалось выполнить операцию. Повторите попытку позже.";

    private LocalValidatorFactoryBean validator;
    private ValidationController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        controller = new ValidationController();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void requestBodyReturnsAllFieldErrorsIncludingNestedFields() throws Exception {
        MvcResult result = mvc.perform(post("/validation/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"quantity\":0,\"items\":[{\"quantity\":0}]}")).andReturn();
        assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(
                new Error("name", "Название обязательно."),
                new Error("name", "Длина названия: от 3 до 8 символов."),
                new Error("quantity", "Количество: минимум 1."),
                new Error("items[0].quantity", "Количество позиции: минимум 1.")
        ), errors(problem));
    }

    @Test
    void rejectedValuesAreNotIncludedInValidationResponse() throws Exception {
        MvcResult result = mvc.perform(post("/validation/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + SENSITIVE_DETAIL + "\",\"quantity\":1,\"items\":[]}")).andReturn();
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(new Error("name", "Длина названия: от 3 до 8 символов.")), errors(problem));
        assertFalse(result.getResponse().getContentAsString().contains(SENSITIVE_DETAIL));
    }

    @Test
    void objectLevelConstraintUsesRootField() throws Exception {
        MvcResult result = mvc.perform(post("/validation/range").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimum\":5,\"maximum\":1}")).andReturn();
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(new Error("$", "Минимум не должен превышать максимум.")), errors(problem));
    }

    @Test
    void methodValidationUsesPublicQueryPathAndHeaderNames() throws Exception {
        MvcResult result = mvc.perform(get("/validation/parameters/0")
                .queryParam("quantity", "0").header("X-Code", "x")).andReturn();
        assertInstanceOf(HandlerMethodValidationException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(
                new Error("identifier", "Идентификатор: минимум 1."),
                new Error("quantity", "Количество: минимум 1."),
                new Error("X-Code", "Код: минимум 3 символа.")
        ), errors(problem));
    }

    @Test
    void methodValidationAlsoIncludesNestedRequestBodyErrors() throws Exception {
        MvcResult result = mvc.perform(post("/validation/mixed").queryParam("quantity", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"valid\",\"quantity\":1,\"items\":[{\"quantity\":0}]}")).andReturn();
        assertInstanceOf(HandlerMethodValidationException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(
                new Error("quantity", "Количество: минимум 1."),
                new Error("items[0].quantity", "Количество позиции: минимум 1.")
        ), errors(problem));
    }

    @Test
    void methodValidationUsesRootFieldForObjectLevelConstraint() throws Exception {
        MvcResult result = mvc.perform(post("/validation/mixed-range").queryParam("quantity", "1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"minimum\":5,\"maximum\":1}")).andReturn();
        assertInstanceOf(HandlerMethodValidationException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(new Error("$", "Минимум не должен превышать максимум.")), errors(problem));
    }

    @Test
    void methodValidationIncludesIndicesForRequestBodyLists() throws Exception {
        MvcResult result = mvc.perform(post("/validation/product-list").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"name\":\"valid\",\"quantity\":0,\"items\":[]},"
                        + "{\"name\":\"valid\",\"quantity\":0,\"items\":[]}]")).andReturn();
        assertInstanceOf(HandlerMethodValidationException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(
                new Error("[0].quantity", "Количество: минимум 1."),
                new Error("[1].quantity", "Количество: минимум 1.")
        ), errors(problem));
    }

    @Test
    void constraintViolationExceptionUsesSameFieldErrorFormat() throws Exception {
        controller.failure = new ConstraintViolationException(validator.validate(
                new ProductRequest(SENSITIVE_DETAIL, 0, List.of())));
        MvcResult result = mvc.perform(get("/validation/exception")).andReturn();
        JsonNode problem = assertProblem(result, HttpStatus.BAD_REQUEST);
        assertEquals(Set.of(
                new Error("name", "Длина названия: от 3 до 8 символов."),
                new Error("quantity", "Количество: минимум 1.")
        ), errors(problem));
        assertFalse(result.getResponse().getContentAsString().contains(SENSITIVE_DETAIL));
    }

    @Test
    void invalidReturnValueRemainsGeneric500() throws Exception {
        MvcResult result = mvc.perform(get("/validation/return")).andReturn();
        assertInstanceOf(HandlerMethodValidationException.class, result.getResolvedException());
        JsonNode problem = assertProblem(result, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(INTERNAL_DETAIL, problem.get("detail").asString());
        assertFalse(problem.has("errors"));
        assertFalse(result.getResponse().getContentAsString().contains(SENSITIVE_DETAIL));
    }

    @Test
    void constraintViolationOnReturnValueRemainsGeneric500() throws Exception {
        var method = ValidationController.class.getDeclaredMethod("invalidReturnValue");
        controller.failure = new ConstraintViolationException(
                validator.forExecutables().validateReturnValue(controller, method, ""));
        MvcResult result = mvc.perform(get("/validation/exception")).andReturn();
        JsonNode problem = assertProblem(result, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(INTERNAL_DETAIL, problem.get("detail").asString());
        assertFalse(problem.has("errors"));
        assertFalse(result.getResponse().getContentAsString().contains(SENSITIVE_DETAIL));
    }

    @Test
    void validRequestStillSucceeds() throws Exception {
        MvcResult result = mvc.perform(post("/validation/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"valid\",\"quantity\":1,\"items\":[{\"quantity\":1}]}")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertNull(result.getResolvedException());
    }

    private JsonNode assertProblem(MvcResult result, HttpStatus status) throws Exception {
        assertEquals(status.value(), result.getResponse().getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, result.getResponse().getContentType());
        JsonNode problem = JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(status.value(), problem.get("status").asInt());
        assertEquals(status.getReasonPhrase(), problem.get("title").asString());
        assertEquals(result.getRequest().getRequestURI(), problem.get("instance").asString());
        if (status == HttpStatus.BAD_REQUEST) {
            assertEquals("Запрос содержит ошибки валидации.", problem.get("detail").asString());
            assertTrue(problem.get("errors").isArray());
        }
        return problem;
    }

    private Set<Error> errors(JsonNode problem) {
        Set<Error> result = new HashSet<>();
        for (JsonNode error : problem.get("errors")) {
            assertEquals(2, error.size());
            result.add(new Error(error.get("field").asString(), error.get("message").asString()));
        }
        assertEquals(problem.get("errors").size(), result.size());
        return result;
    }

    private record Error(String field, String message) {
    }

    @RestController
    static class ValidationController {
        private RuntimeException failure;

        @PostMapping("/validation/products")
        void createProduct(@Valid @RequestBody ProductRequest request) {
        }

        @PostMapping("/validation/range")
        void range(@Valid @RequestBody RangeRequest request) {
        }

        @GetMapping("/validation/parameters/{identifier}")
        void parameters(
                @PathVariable("identifier") @Min(value = 1, message = "Идентификатор: минимум 1.") int internalId,
                @RequestParam("quantity") @Min(value = 1, message = "Количество: минимум 1.") int internalQuantity,
                @RequestHeader("X-Code") @Size(min = 3, message = "Код: минимум 3 символа.") String internalCode) {
        }

        @PostMapping("/validation/mixed")
        void mixed(@Valid @RequestBody ProductRequest request,
                   @RequestParam("quantity") @Min(value = 1, message = "Количество: минимум 1.") int amount) {
        }

        @PostMapping("/validation/mixed-range")
        void mixedRange(@Valid @RequestBody RangeRequest request,
                        @RequestParam("quantity") @Min(1) int amount) {
        }

        @PostMapping("/validation/product-list")
        void productList(@Valid @RequestBody @Size(min = 1) List<ProductRequest> requests) {
        }

        @GetMapping("/validation/exception")
        void fail() {
            throw failure;
        }

        @GetMapping("/validation/return")
        @NotBlank(message = SENSITIVE_DETAIL)
        String invalidReturnValue() {
            return "";
        }
    }

    record ProductRequest(
            @NotBlank(message = "Название обязательно.")
            @Size(min = 3, max = 8, message = "Длина названия: от 3 до 8 символов.") String name,
            @Min(value = 1, message = "Количество: минимум 1.") int quantity,
            @Valid List<LineRequest> items) {
    }

    record LineRequest(@Min(value = 1, message = "Количество позиции: минимум 1.") int quantity) {
    }

    @ValidRange
    record RangeRequest(int minimum, int maximum) {
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @Constraint(validatedBy = RangeValidator.class)
    public @interface ValidRange {
        String message() default "Минимум не должен превышать максимум.";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class RangeValidator implements ConstraintValidator<ValidRange, RangeRequest> {
        @Override
        public boolean isValid(RangeRequest value, ConstraintValidatorContext context) {
            return value == null || value.minimum() <= value.maximum();
        }
    }
}
