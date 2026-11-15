package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.config.TokenProperties;
import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.*;
import com.team.corporate.backend.security.BootstrapAdmin;
import com.team.corporate.backend.services.AccountsService;
import com.team.corporate.backend.services.RefreshSessionsService;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=", "cps.security.bootstrap.password=",
        "cps.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "cps.security.rate-limit.account-attempts=3", "cps.security.rate-limit.ip-attempts=10",
        "cps.security.rate-limit.refresh-attempts=10"
})
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TestTime.class)
class SecurityIntegrationTest {
    private static final String PASSWORD = "correct-password-123";
    private static final String NEW_PASSWORD = "new-password-456";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private MockMvc mvc;
    @Autowired private SpringUsersRepository users;
    @Autowired private UserCredentialsRepository credentials;
    @Autowired private RefreshSessionsRepository sessions;
    @Autowired private RefreshTokensRepository refreshTokens;
    @Autowired private PasswordEncoder encoder;
    @Autowired private AccountsService accounts;
    @Autowired private RefreshSessionsService sessionService;
    @Autowired private Validator validator;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtDecoder decoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private TokenProperties tokenProperties;
    @Autowired private MutableClock clock;

    private User admin;
    private User manager;
    private User employee;
    private User legacy;

    @BeforeEach
    void setUp() {
        clock.advance(Duration.ofDays(1));
        refreshTokens.deleteAllInBatch();
        sessions.deleteAllInBatch();
        credentials.deleteAll();
        users.deleteAll();
        admin = users.save(new User("admin", UserRole.ADMIN));
        manager = users.save(new User("manager", UserRole.MANAGER));
        employee = users.save(new User("employee", UserRole.USER));
        legacy = users.save(new User("legacy", UserRole.USER));
        credentials.save(new UserCredentials(admin, encoder.encode(PASSWORD)));
        credentials.save(new UserCredentials(manager, encoder.encode(PASSWORD)));
        credentials.save(new UserCredentials(employee, encoder.encode(PASSWORD)));
    }

    @Test
    void loginIssuesSignedJwtAndHashedRefreshSessionWithoutHttpSession() throws Exception {
        MvcResult result = loginAttempt("employee", PASSWORD);
        assertEquals(200, result.getResponse().getStatus());
        Tokens tokens = tokenResponse(result);
        Jwt jwt = decoder.decode(tokens.access());
        assertEquals(employee.getId().toString(), jwt.getSubject());
        assertEquals(tokenProperties.issuer(), jwt.getIssuer().toString());
        assertTrue(jwt.getAudience().contains(tokenProperties.audience()));
        assertEquals("access", jwt.getClaimAsString("token_use"));
        assertEquals("Bearer", json(result).get("tokenType").asString());
        assertEquals(900, json(result).get("expiresIn").asLong());
        assertTrue(sessions.existsById(UUID.fromString(jwt.getClaimAsString("sid"))));
        assertEquals(1, refreshTokens.count());
        String stored = refreshTokens.findAll().getFirst().getTokenHash();
        assertEquals(64, stored.length());
        assertNotEquals(tokens.refresh(), stored);
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertTrue(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL).contains("no-store"));
        MvcResult me = me(tokens);
        assertEquals(200, me.getResponse().getStatus());
        assertEquals(employee.getId().toString(), json(me).get("id").asString());
        assertEquals("USER", json(me).get("role").asString());
        assertNoPassword(me);
    }

    @Test
    void anonymousRequestsNeedBearerAuthentication() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/me")).andReturn();
        assertProblem(result, 401);
        assertEquals("Bearer", result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertProblem(mvc.perform(get("/api/users")).andReturn(), 401);
    }

    @Test
    void httpSessionCannotSubstituteForJwt() throws Exception {
        var session = new MockHttpSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", "", AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
        assertProblem(mvc.perform(get("/api/users").session(session)).andReturn(), 401);
    }

    @Test
    void wrongPasswordAndUnknownUsernameReturnIdenticalProblems() throws Exception {
        MvcResult wrong = loginAttempt("employee", "wrong-password");
        MvcResult unknown = loginAttempt("missing", "wrong-password");
        assertProblem(wrong, 401);
        assertProblem(unknown, 401);
        assertEquals(json(wrong), json(unknown));
        assertEquals(0, sessions.count());
    }

    @Test
    void legacyUserWithoutPasswordCannotLogIn() throws Exception {
        assertProblem(loginAttempt("legacy", PASSWORD), 401);
    }

    @Test
    void refreshRotatesTokensAndWorksWithExpiredAccessHeader() throws Exception {
        Tokens original = login("employee", PASSWORD);
        clock.advance(Duration.ofMinutes(16));
        assertProblem(me(original), 401);
        MvcResult result = mvc.perform(post("/api/auth/refresh").header(HttpHeaders.AUTHORIZATION, bearer(original))
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody(original))).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        Tokens renewed = tokenResponse(result);
        assertNotEquals(original.access(), renewed.access());
        assertNotEquals(original.refresh(), renewed.refresh());
        assertEquals(200, me(renewed).getResponse().getStatus());
        assertEquals(1, sessions.count());
        assertEquals(2, refreshTokens.count());
        assertEquals(1, refreshTokens.findAll().stream().filter(token -> token.getUsedAt() != null).count());
    }

    @Test
    void refreshReplayRevokesWholeSessionButNotOtherDevice() throws Exception {
        Tokens original = login("employee", PASSWORD);
        Tokens otherDevice = login("employee", PASSWORD);
        Tokens rotated = tokenResponse(refresh(original));
        assertProblem(refresh(original), 401);
        assertProblem(me(rotated), 401);
        assertProblem(refresh(rotated), 401);
        assertEquals(200, me(otherDevice).getResponse().getStatus());
        UUID id = UUID.fromString(decoder.decode(rotated.access()).getClaimAsString("sid"));
        assertNotNull(sessions.findById(id).orElseThrow().getRevokedAt());
    }

    @Test
    void concurrentRefreshOnlySucceedsOnceAndReplayRevocationCommits() throws Exception {
        Tokens original = login("employee", PASSWORD);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> { start.await(); return refresh(original); });
            var second = executor.submit(() -> { start.await(); return refresh(original); });
            start.countDown();
            MvcResult left = first.get(15, TimeUnit.SECONDS);
            MvcResult right = second.get(15, TimeUnit.SECONDS);
            assertEquals(Set.of(200, 401), Set.of(left.getResponse().getStatus(), right.getResponse().getStatus()));
            Tokens renewed = tokenResponse(left.getResponse().getStatus() == 200 ? left : right);
            assertProblem(me(renewed), 401);
        }
    }

    @Test
    void expiredRefreshSessionCannotIssueTokens() throws Exception {
        Tokens tokens = login("employee", PASSWORD);
        clock.advance(Duration.ofDays(31));
        assertProblem(refresh(tokens), 401);
        assertProblem(me(tokens), 401);
    }

    @Test
    void logoutRevokesAccessAndRefreshAndIsIdempotent() throws Exception {
        Tokens tokens = login("employee", PASSWORD);
        assertEquals(204, logout(tokens).getResponse().getStatus());
        assertProblem(me(tokens), 401);
        assertProblem(refresh(tokens), 401);
        assertEquals(204, logout(tokens).getResponse().getStatus());
    }

    @Test
    void malformedAndForgedTokensAreRejected() throws Exception {
        assertProblem(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer malformed")).andReturn(), 401);
        Tokens tokens = login("employee", PASSWORD);
        String[] parts = tokens.access().split("\\.");
        String signature = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertProblem(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + parts[0] + "." + parts[1] + "." + signature)).andReturn(), 401);
    }

    @Test
    void issuerAudienceExpiryAndSessionBindingAreValidated() throws Exception {
        Tokens tokens = login("employee", PASSWORD);
        for (String claim : List.of("iss", "aud", "exp", "sid", "sub", "token_use")) {
            String forged = signedToken(tokens, claims -> {
                switch (claim) {
                    case "iss" -> claims.put("iss", "wrong-issuer");
                    case "aud" -> claims.put("aud", List.of("wrong-audience"));
                    case "exp" -> claims.remove("exp");
                    case "sid" -> claims.put("sid", UUID.randomUUID().toString());
                    case "sub" -> claims.put("sub", admin.getId().toString());
                    case "token_use" -> claims.put("token_use", "refresh");
                }
            });
            assertProblem(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)).andReturn(), 401);
        }
    }

    @Test
    void currentDatabaseRoleOverridesStaleJwtRoleClaim() throws Exception {
        Tokens tokens = login("admin", PASSWORD);
        admin.setUserRole(UserRole.USER);
        users.save(admin);
        assertProblem(mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))).andReturn(), 403);
        assertEquals("USER", json(me(tokens)).get("role").asString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "ADMIN"})
    void adminCreatesAccountWithBcryptAndSafeResponse(String role) throws Exception {
        Tokens tokens = login("admin", PASSWORD);
        MvcResult result = mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON).content(createJson("new-user", role, PASSWORD))).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        assertEquals(role, json(result).get("role").asString());
        assertFalse(json(result).get("createdAt").isNull());
        UUID id = UUID.fromString(json(result).get("id").asString());
        String hash = credentials.findById(id).orElseThrow().getPasswordHash();
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, hash.substring("{bcrypt}".length())));
        assertNoPassword(result);
        assertEquals("/api/users/" + id, result.getResponse().getHeader(HttpHeaders.LOCATION));
        login("new-user", PASSWORD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"employee", "manager"})
    void onlyAdminsCanCreateQueryAndResetUsers(String username) throws Exception {
        Tokens tokens = login(username, PASSWORD);
        assertProblem(mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))).andReturn(), 403);
        for (String role : List.of("USER", "MANAGER", "ADMIN")) {
            assertProblem(mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                    .contentType(MediaType.APPLICATION_JSON).content(createJson("forbidden", role, PASSWORD))).andReturn(), 403);
        }
        assertTrue(users.findByUsername("forbidden").isEmpty());
        for (UUID id : List.of(admin.getId(), manager.getId(), employee.getId())) {
            String hash = credentials.findById(id).orElseThrow().getPasswordHash();
            assertProblem(mvc.perform(put("/api/users/" + id + "/password").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                    .contentType(MediaType.APPLICATION_JSON).content(passwordBody(NEW_PASSWORD))).andReturn(), 403);
            assertEquals(hash, credentials.findById(id).orElseThrow().getPasswordHash());
        }
    }

    @Test
    void anonymousUserCannotCreateOrResetAccounts() throws Exception {
        assertProblem(mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(createJson("forbidden", "USER", PASSWORD))).andReturn(), 401);
        assertProblem(mvc.perform(put("/api/users/" + employee.getId() + "/password")
                .contentType(MediaType.APPLICATION_JSON).content(passwordBody(NEW_PASSWORD))).andReturn(), 401);
    }

    @Test
    void adminCanListAllUsersWithoutCredentials() throws Exception {
        Tokens tokens = login("admin", PASSWORD);
        MvcResult result = mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        Set<String> ids = new HashSet<>();
        for (JsonNode user : json(result)) ids.add(user.get("id").asString());
        assertEquals(Set.of(admin.getId().toString(), manager.getId().toString(),
                employee.getId().toString(), legacy.getId().toString()), ids);
        assertNoPassword(result);
    }

    @Test
    void passwordChangeRevokesEveryUserSessionAtomically() throws Exception {
        Tokens first = login("employee", PASSWORD);
        Tokens second = login("employee", PASSWORD);
        Tokens adminTokens = login("admin", PASSWORD);
        assertEquals(204, change(first, PASSWORD, NEW_PASSWORD).getResponse().getStatus());
        assertProblem(me(first), 401);
        assertProblem(me(second), 401);
        assertProblem(refresh(first), 401);
        assertProblem(refresh(second), 401);
        assertEquals(200, me(adminTokens).getResponse().getStatus());
        assertProblem(loginAttempt("employee", PASSWORD), 401);
        login("employee", NEW_PASSWORD);
    }

    @Test
    void wrongCurrentPasswordChangesNeitherPasswordNorSession() throws Exception {
        Tokens tokens = login("employee", PASSWORD);
        String hash = credentials.findById(employee.getId()).orElseThrow().getPasswordHash();
        assertProblem(change(tokens, "wrong", NEW_PASSWORD), 401);
        assertEquals(hash, credentials.findById(employee.getId()).orElseThrow().getPasswordHash());
        assertEquals(200, me(tokens).getResponse().getStatus());
    }

    @Test
    void adminResetRevokesExistingSessionAndEnablesUsersWithoutPassword() throws Exception {
        Tokens adminTokens = login("admin", PASSWORD);
        Tokens old = login("employee", PASSWORD);
        assertEquals(204, reset(adminTokens, employee.getId(), NEW_PASSWORD).getResponse().getStatus());
        assertProblem(me(old), 401);
        assertProblem(refresh(old), 401);
        assertProblem(loginAttempt("employee", PASSWORD), 401);
        login("employee", NEW_PASSWORD);
        assertEquals(204, reset(adminTokens, legacy.getId(), PASSWORD).getResponse().getStatus());
        login("legacy", PASSWORD);
    }

    @Test
    void accountBruteForceLimitAppliesAcrossIpsAndExpires() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertProblem(loginAttempt("employee", "wrong-password", "198.51.100." + attempt), 401);
        }
        MvcResult limited = loginAttempt("employee", PASSWORD, "198.51.100.4");
        assertProblem(limited, 429);
        assertEquals("900", limited.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
        clock.advance(Duration.ofMinutes(16));
        login("employee", PASSWORD);
    }

    @Test
    void ipLimitPreventsUsernameSprayingAndIgnoresSpoofedForwardedHeaders() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "203.0.113." + attempt)
                    .with(request -> { request.setRemoteAddr("198.51.100.10"); return request; })
                    .content(loginJson("missing-" + attempt, "wrong-password"))).andReturn();
            assertProblem(result, 401);
        }
        assertProblem(loginAttempt("another-missing", "wrong-password", "198.51.100.10"), 429);
    }

    @Test
    void successfulLoginResetsAccountAttemptCounter() throws Exception {
        assertProblem(loginAttempt("employee", "wrong-password"), 401);
        assertProblem(loginAttempt("employee", "wrong-password"), 401);
        login("employee", PASSWORD);
        assertProblem(loginAttempt("employee", "wrong-password"), 401);
        login("employee", PASSWORD);
    }

    @Test
    void currentPasswordChecksAreRateLimited() throws Exception {
        Tokens tokens = login("employee", PASSWORD);
        for (int attempt = 0; attempt < 3; attempt++) assertProblem(change(tokens, "wrong", NEW_PASSWORD), 401);
        assertProblem(change(tokens, PASSWORD, NEW_PASSWORD), 429);
        assertEquals(200, me(tokens).getResponse().getStatus());
    }

    @Test
    void refreshGuessingIsRateLimited() throws Exception {
        Tokens unknown = new Tokens("", "not-a-real-refresh-token");
        for (int attempt = 0; attempt < 10; attempt++) assertProblem(refresh(unknown), 401);
        assertProblem(refresh(unknown), 429);
    }

    @Test
    void invalidPasswordBodiesReturnFieldErrorsWithoutMutations() throws Exception {
        Tokens tokens = login("admin", PASSWORD);
        long count = users.count();
        MvcResult create = mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON).content(createJson("too-long", "USER", "a".repeat(73)))).andReturn();
        assertProblem(create, 400);
        assertEquals("password", json(create).get("errors").get(0).get("field").asString());
        assertEquals(count, users.count());
        assertProblem(reset(tokens, employee.getId(), "short"), 400);
        assertProblem(change(tokens, PASSWORD, "a".repeat(73)), 400);
        assertEquals(200, me(tokens).getResponse().getStatus());
    }

    @Test
    void passwordsAtBcryptLimitAreNotSilentlyTruncated() throws Exception {
        Tokens adminTokens = login("admin", PASSWORD);
        String password = "🔐".repeat(18);
        assertEquals(204, reset(adminTokens, employee.getId(), password).getResponse().getStatus());
        login("employee", password);
        assertProblem(loginAttempt("employee", password + "x"), 401);
    }

    @Test
    void duplicateUserReturns409WithoutPartialCredentials() throws Exception {
        Tokens tokens = login("admin", PASSWORD);
        long count = credentials.count();
        MvcResult result = mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON).content(createJson(" employee ", "ADMIN", PASSWORD))).andReturn();
        assertProblem(result, 409);
        assertEquals(count, credentials.count());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void methodSecurityCannotBeBypassed() {
        assertThrows(AccessDeniedException.class,
                () -> accounts.createUser(new CreateUserRequest("bypass", PASSWORD, UserRole.USER)));
        assertThrows(AccessDeniedException.class, () -> accounts.getUsers());
        assertThrows(AccessDeniedException.class, () -> accounts.resetPassword(employee.getId(), NEW_PASSWORD));
        assertThrows(AccessDeniedException.class, () -> accounts.currentUser("admin"));
    }

    @Test
    void expiredSessionCleanupCascadesToTokenHistory() throws Exception {
        login("employee", PASSWORD);
        clock.advance(Duration.ofDays(31));
        sessionService.deleteExpiredSessions();
        assertEquals(0, sessions.count());
        assertEquals(0, refreshTokens.count());
    }

    @Test
    void bootstrapCreatesOnlyAdminAndNeverOverwritesOrPromotes() {
        runBootstrap("first-admin", PASSWORD);
        User created = users.findByUsername("first-admin").orElseThrow();
        assertEquals(UserRole.ADMIN, created.getUserRole());
        String hash = credentials.findById(created.getId()).orElseThrow().getPasswordHash();
        assertTrue(encoder.matches(PASSWORD, hash));
        runBootstrap("first-admin", NEW_PASSWORD);
        assertEquals(hash, credentials.findById(created.getId()).orElseThrow().getPasswordHash());
        assertThrows(IllegalStateException.class, () -> runBootstrap("manager", PASSWORD));
        assertThrows(IllegalStateException.class, () -> runBootstrap("legacy", PASSWORD));
        assertThrows(IllegalStateException.class, () -> runBootstrap("", PASSWORD));
        assertThrows(IllegalStateException.class, () -> runBootstrap("missing-password", ""));
        assertThrows(IllegalStateException.class, () -> runBootstrap("weak", "short"));
        assertThrows(IllegalStateException.class, () -> runBootstrap("long", "a".repeat(73)));
        assertTrue(users.findByUsername("weak").isEmpty());
    }

    private void runBootstrap(String username, String password) {
        var bootstrap = new BootstrapAdmin(users, credentials, encoder, validator, username, password);
        new TransactionTemplate(transactions).executeWithoutResult(status -> bootstrap.run(new DefaultApplicationArguments()));
    }

    private Tokens login(String username, String password) throws Exception {
        MvcResult result = loginAttempt(username, password);
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return tokenResponse(result);
    }

    private MvcResult loginAttempt(String username, String password) throws Exception {
        return loginAttempt(username, password, "127.0.0.1");
    }

    private MvcResult loginAttempt(String username, String password, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .content(loginJson(username, password))).andReturn();
    }

    private MvcResult refresh(Tokens tokens) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(tokens))).andReturn();
    }

    private MvcResult logout(Tokens tokens) throws Exception {
        return mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(tokens))).andReturn();
    }

    private MvcResult me(Tokens tokens) throws Exception {
        return mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens))).andReturn();
    }

    private MvcResult change(Tokens tokens, String current, String next) throws Exception {
        return mvc.perform(put("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("currentPassword", current, "newPassword", next)))).andReturn();
    }

    private MvcResult reset(Tokens tokens, UUID userId, String password) throws Exception {
        return mvc.perform(put("/api/users/" + userId + "/password").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON).content(passwordBody(password))).andReturn();
    }

    private String signedToken(Tokens tokens, Consumer<Map<String, Object>> changes) {
        Jwt original = decoder.decode(tokens.access());
        var claims = JwtClaimsSet.builder().claims(values -> { values.putAll(original.getClaims()); changes.accept(values); }).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String bearer(Tokens tokens) { return "Bearer " + tokens.access(); }
    private String refreshBody(Tokens tokens) { return JSON.writeValueAsString(Map.of("refreshToken", tokens.refresh())); }
    private String passwordBody(String password) { return JSON.writeValueAsString(Map.of("password", password)); }
    private String loginJson(String username, String password) {
        return JSON.writeValueAsString(Map.of("username", username, "password", password));
    }
    private String createJson(String username, String role, String password) {
        return JSON.writeValueAsString(Map.of("username", username, "role", role, "password", password));
    }
    private JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    private Tokens tokenResponse(MvcResult result) throws Exception {
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        JsonNode body = json(result);
        return new Tokens(body.get("accessToken").asString(), body.get("refreshToken").asString());
    }
    private void assertProblem(MvcResult result, int status) throws Exception {
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, result.getResponse().getContentType());
        JsonNode body = json(result);
        assertEquals(status, body.get("status").asInt());
        assertEquals(result.getRequest().getRequestURI(), body.get("instance").asString());
        assertNoPassword(result);
    }
    private void assertNoPassword(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(PASSWORD));
        assertFalse(body.contains(NEW_PASSWORD));
        assertFalse(body.contains("passwordHash"));
        assertFalse(body.contains("password_hash"));
        assertFalse(body.contains("{bcrypt}"));
    }
    private record Tokens(String access, String refresh) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestTime {
        @Bean @Primary
        MutableClock testClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-15T12:00:00Z"));
        void advance(Duration duration) { now.updateAndGet(instant -> instant.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return now.get(); }
    }
}
