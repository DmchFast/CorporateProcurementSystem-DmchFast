package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import com.team.corporate.backend.security.BootstrapAdmin;
import com.team.corporate.backend.services.AccountsService;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=",
        "cps.security.bootstrap.password="
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    private static final String PASSWORD = "correct-password-123";
    private static final String NEW_PASSWORD = "new-password-456";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private MockMvc mvc;
    @Autowired private SpringUsersRepository users;
    @Autowired private UserCredentialsRepository credentials;
    @Autowired private PasswordEncoder encoder;
    @Autowired private AccountsService accounts;
    @Autowired private SessionRegistry sessions;
    @Autowired private Validator validator;
    @Autowired private PlatformTransactionManager transactions;

    private User admin;
    private User manager;
    private User employee;
    private User legacy;

    @BeforeEach
    void setUp() {
        for (Object principal : sessions.getAllPrincipals()) {
            sessions.getAllSessions(principal, true).forEach(session -> sessions.removeSessionInformation(session.getSessionId()));
        }
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
    void anonymousRequestsReturn401ProblemDetails() throws Exception {
        assertProblem(mvc.perform(get("/api/auth/me")).andReturn(), 401);
        assertProblem(mvc.perform(get("/api/users")).andReturn(), 401);
    }

    @Test
    void csrfEndpointAndLoginUseRealTokensAndRotateSessionId() throws Exception {
        MvcResult tokenResult = mvc.perform(get("/api/auth/csrf")).andReturn();
        assertEquals(200, tokenResult.getResponse().getStatus());
        JsonNode token = json(tokenResult);
        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        assertNotNull(session);
        String originalId = session.getId();
        MvcResult login = mvc.perform(post("/api/auth/login").session(session)
                .with(request -> {
                    request.setRequestedSessionId(originalId);
                    return request;
                })
                .header(token.get("headerName").asString(), token.get("token").asString())
                .param("username", "employee").param("password", PASSWORD)).andReturn();

        assertEquals(204, login.getResponse().getStatus());
        assertNotEquals(originalId, session.getId());
        MvcResult me = mvc.perform(get("/api/auth/me").session(session)).andReturn();
        assertEquals(200, me.getResponse().getStatus());
        assertEquals(employee.getId().toString(), json(me).get("id").asString());
        assertEquals("USER", json(me).get("role").asString());
        assertNoPassword(me);
        assertProblem(mvc.perform(post("/api/auth/logout").session(session)
                .header(token.get("headerName").asString(), token.get("token").asString())).andReturn(), 403);
        JsonNode refreshed = json(mvc.perform(get("/api/auth/csrf").session(session)).andReturn());
        assertEquals(204, mvc.perform(post("/api/auth/logout").session(session)
                .header(refreshed.get("headerName").asString(), refreshed.get("token").asString()))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void wrongPasswordAndUnknownUsernameReturnIdenticalProblems() throws Exception {
        MvcResult wrong = loginAttempt("employee", "wrong-password");
        MvcResult unknown = loginAttempt("missing", "wrong-password");
        assertProblem(wrong, 401);
        assertProblem(unknown, 401);
        assertEquals(json(wrong), json(unknown));
    }

    @Test
    void legacyUsersCannotLogInUntilPasswordIsProvisioned() throws Exception {
        assertProblem(loginAttempt("legacy", PASSWORD), 401);
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        assertProblem(mvc.perform(post("/api/auth/login")
                .param("username", "employee").param("password", PASSWORD)).andReturn(), 403);
    }

    @Test
    void userCannotManageAccountsOrAssignAdminRole() throws Exception {
        MockHttpSession session = login("employee", PASSWORD);
        assertProblem(mvc.perform(get("/api/users").session(session)).andReturn(), 403);
        assertProblem(mvc.perform(post("/api/users").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("intruder", "ADMIN", PASSWORD))).andReturn(), 403);
        assertTrue(users.findByUsername("intruder").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "ADMIN"})
    void adminCreatesAccountWithBCryptPasswordAndSafeResponse(String role) throws Exception {
        MockHttpSession session = login("admin", PASSWORD);
        MvcResult result = mvc.perform(post("/api/users").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("new-user", role, PASSWORD))).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        assertEquals(role, json(result).get("role").asString());
        assertFalse(json(result).get("createdAt").isNull());
        UUID id = UUID.fromString(json(result).get("id").asString());
        String hash = credentials.findById(id).orElseThrow().getPasswordHash();
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, hash.substring("{bcrypt}".length())));
        assertTrue(encoder.matches(PASSWORD, hash));
        assertEquals("/api/users/" + id, result.getResponse().getHeader("Location"));
        assertNoPassword(result);
        assertEquals(200, mvc.perform(get("/api/users/" + id).session(session)).andReturn().getResponse().getStatus());
        login("new-user", PASSWORD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "ADMIN"})
    void managerCannotCreateUsersOfAnyRole(String role) throws Exception {
        MockHttpSession session = login("manager", PASSWORD);
        long userCount = users.count();
        assertProblem(mvc.perform(post("/api/users").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("forbidden", role, PASSWORD))).andReturn(), 403);
        assertProblem(mvc.perform(get("/api/users").session(session)).andReturn(), 403);
        assertEquals(userCount, users.count());
        MvcResult me = mvc.perform(get("/api/auth/me").session(session)).andReturn();
        assertEquals(200, me.getResponse().getStatus());
        assertEquals("MANAGER", json(me).get("role").asString());
    }

    @Test
    void anonymousUserCannotCreateAccount() throws Exception {
        assertProblem(mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("forbidden", "USER", PASSWORD))).andReturn(), 401);
        assertTrue(users.findByUsername("forbidden").isEmpty());
    }

    @Test
    void duplicateUsernameReturns409WithoutPartialAccount() throws Exception {
        long accountCount = credentials.count();
        MvcResult result = mvc.perform(post("/api/users").session(login("admin", PASSWORD)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson(" employee ", "MANAGER", PASSWORD))).andReturn();
        assertProblem(result, 409);
        assertEquals(accountCount, credentials.count());
        assertEquals(UserRole.USER, users.findById(employee.getId()).orElseThrow().getUserRole());
    }

    @Test
    void weakPasswordReturnsValidationProblemWithoutCreatingUser() throws Exception {
        MvcResult result = mvc.perform(post("/api/users").session(login("admin", PASSWORD)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("weak-user", "USER", "short"))).andReturn();
        assertProblem(result, 400);
        assertEquals("password", json(result).get("errors").get(0).get("field").asString());
        assertFalse(result.getResponse().getContentAsString().contains("short"));
        assertTrue(users.findByUsername("weak-user").isEmpty());
    }

    @Test
    void unicodePasswordsAreSupportedWithoutTruncation() throws Exception {
        String password = "🔐".repeat(18);
        MvcResult result = mvc.perform(post("/api/users").session(login("admin", PASSWORD)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("unicode-user", "USER", password))).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        login("unicode-user", password);
        assertProblem(loginAttempt("unicode-user", "🔐".repeat(17) + "x"), 401);
        assertProblem(loginAttempt("unicode-user", password + "x"), 401);
    }

    @Test
    void overlongPasswordsReturnFieldValidationErrorsOnEveryWriteEndpoint() throws Exception {
        String password = "a".repeat(73);
        MockHttpSession session = login("admin", PASSWORD);
        String adminHash = credentials.findById(admin.getId()).orElseThrow().getPasswordHash();
        String employeeHash = credentials.findById(employee.getId()).orElseThrow().getPasswordHash();
        MvcResult create = mvc.perform(post("/api/users").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(createJson("too-long", "USER", password))).andReturn();
        assertProblem(create, 400);
        assertEquals("password", json(create).get("errors").get(0).get("field").asString());
        assertTrue(users.findByUsername("too-long").isEmpty());
        MvcResult reset = mvc.perform(put("/api/users/" + employee.getId() + "/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("password", password)))).andReturn();
        assertProblem(reset, 400);
        assertEquals("password", json(reset).get("errors").get(0).get("field").asString());
        assertEquals(employeeHash, credentials.findById(employee.getId()).orElseThrow().getPasswordHash());
        MvcResult change = mvc.perform(put("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", password)))).andReturn();
        assertProblem(change, 400);
        assertEquals("newPassword", json(change).get("errors").get(0).get("field").asString());
        assertEquals(adminHash, credentials.findById(admin.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void passwordChangeChecksCurrentPasswordAndRevokesEverySession() throws Exception {
        MockHttpSession first = login("employee", PASSWORD);
        MockHttpSession second = login("employee", PASSWORD);
        MvcResult result = mvc.perform(put("/api/auth/password").session(first).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD)))).andReturn();
        assertEquals(204, result.getResponse().getStatus());
        assertProblem(mvc.perform(get("/api/auth/me").session(first)).andReturn(), 401);
        assertProblem(mvc.perform(get("/api/auth/me").session(second)).andReturn(), 401);
        assertProblem(loginAttempt("employee", PASSWORD), 401);
        login("employee", NEW_PASSWORD);
    }

    @Test
    void wrongCurrentPasswordDoesNotChangePasswordOrRevokeSession() throws Exception {
        MockHttpSession session = login("employee", PASSWORD);
        String previousHash = credentials.findById(employee.getId()).orElseThrow().getPasswordHash();
        MvcResult result = mvc.perform(put("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("currentPassword", "wrong", "newPassword", NEW_PASSWORD)))).andReturn();
        assertProblem(result, 401);
        assertEquals(previousHash, credentials.findById(employee.getId()).orElseThrow().getPasswordHash());
        assertEquals(200, mvc.perform(get("/api/auth/me").session(session)).andReturn().getResponse().getStatus());
    }

    @Test
    void adminCanProvisionLegacyUserAndResetPassword() throws Exception {
        MockHttpSession session = login("admin", PASSWORD);
        String url = "/api/users/" + legacy.getId() + "/password";
        assertEquals(204, mvc.perform(put(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("password", PASSWORD)))).andReturn().getResponse().getStatus());
        MockHttpSession legacySession = login("legacy", PASSWORD);
        assertEquals(204, mvc.perform(put(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("password", NEW_PASSWORD)))).andReturn().getResponse().getStatus());
        assertProblem(mvc.perform(get("/api/auth/me").session(legacySession)).andReturn(), 401);
        assertProblem(loginAttempt("legacy", PASSWORD), 401);
        login("legacy", NEW_PASSWORD);
    }

    @Test
    void adminCanQueryAllAvailableUsersWithoutCredentials() throws Exception {
        MvcResult result = mvc.perform(get("/api/users").session(login("admin", PASSWORD))).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        Set<String> ids = new HashSet<>();
        for (JsonNode user : json(result)) {
            ids.add(user.get("id").asString());
        }
        assertEquals(Set.of(admin.getId().toString(), manager.getId().toString(),
                employee.getId().toString(), legacy.getId().toString()), ids);
        assertNoPassword(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"employee", "manager"})
    void nonAdminsCannotQueryUsersOrResetAnyPassword(String username) throws Exception {
        MockHttpSession session = login(username, PASSWORD);
        assertProblem(mvc.perform(get("/api/users").session(session)).andReturn(), 403);
        assertProblem(mvc.perform(get("/api/users/" + admin.getId()).session(session)).andReturn(), 403);
        for (UUID id : List.of(admin.getId(), manager.getId(), employee.getId())) {
            String previousHash = credentials.findById(id).orElseThrow().getPasswordHash();
            MvcResult result = mvc.perform(put("/api/users/" + id + "/password").session(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JSON.writeValueAsString(Map.of("password", NEW_PASSWORD)))).andReturn();

            assertProblem(result, 403);
            assertEquals(previousHash, credentials.findById(id).orElseThrow().getPasswordHash());
        }
        assertEquals(200, mvc.perform(get("/api/auth/me").session(session)).andReturn().getResponse().getStatus());
    }

    @Test
    void anonymousUserCannotResetPasswords() throws Exception {
        assertProblem(mvc.perform(put("/api/users/" + employee.getId() + "/password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("password", NEW_PASSWORD)))).andReturn(), 401);
    }

    @Test
    void csrfRemainsRequiredForAuthenticatedMutationsAndLogout() throws Exception {
        MockHttpSession session = login("admin", PASSWORD);
        assertProblem(mvc.perform(post("/api/users").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("no-csrf", "USER", PASSWORD))).andReturn(), 403);
        assertProblem(mvc.perform(post("/api/auth/logout").session(session)).andReturn(), 403);
        assertEquals(200, mvc.perform(get("/api/auth/me").session(session)).andReturn().getResponse().getStatus());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = login("employee", PASSWORD);
        assertEquals(204, mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andReturn().getResponse().getStatus());
        assertTrue(session.isInvalid());
        assertProblem(mvc.perform(get("/api/auth/me")).andReturn(), 401);
    }

    @Test
    @WithMockUser(username = "employee", roles = "USER")
    void methodSecurityPreventsBypassingAdminChecks() {
        assertThrows(AccessDeniedException.class,
                () -> accounts.createUser(new CreateUserRequest("bypass", PASSWORD, UserRole.MANAGER)));
        assertThrows(AccessDeniedException.class, () -> accounts.resetPassword(manager.getId(), NEW_PASSWORD));
        assertThrows(AccessDeniedException.class, () -> accounts.getUsers());
        assertThrows(AccessDeniedException.class, () -> accounts.getUser(manager.getId()));
        assertThrows(AccessDeniedException.class, () -> accounts.currentUser("manager"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void methodSecurityRejectsManagerAccountAdministration() {
        assertThrows(AccessDeniedException.class,
                () -> accounts.createUser(new CreateUserRequest("bypass", PASSWORD, UserRole.USER)));
        assertThrows(AccessDeniedException.class, () -> accounts.resetPassword(employee.getId(), NEW_PASSWORD));
        assertThrows(AccessDeniedException.class, () -> accounts.getUsers());
    }

    @Test
    void bootstrapCreatesAdminWithoutOverwritingExistingPassword() {
        runBootstrap("first-admin", PASSWORD);
        User created = users.findByUsername("first-admin").orElseThrow();
        assertEquals(UserRole.ADMIN, created.getUserRole());
        String hash = credentials.findById(created.getId()).orElseThrow().getPasswordHash();
        assertTrue(encoder.matches(PASSWORD, hash));
        assertTrue(hash.startsWith("{bcrypt}"));
        runBootstrap("first-admin", NEW_PASSWORD);
        assertEquals(hash, credentials.findById(created.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void bootstrapCanEnableExistingAdminButNeverPromotesUserOrManager() {
        credentials.deleteById(admin.getId());
        runBootstrap("admin", PASSWORD);
        assertTrue(credentials.existsById(admin.getId()));
        assertThrows(IllegalStateException.class, () -> runBootstrap("manager", PASSWORD));
        assertEquals(UserRole.MANAGER, users.findById(manager.getId()).orElseThrow().getUserRole());
        assertThrows(IllegalStateException.class, () -> runBootstrap("legacy", PASSWORD));
        assertEquals(UserRole.USER, users.findById(legacy.getId()).orElseThrow().getUserRole());
        assertFalse(credentials.existsById(legacy.getId()));
    }

    @Test
    void bootstrapRejectsIncompleteOrWeakConfigurationWithoutCreatingUser() {
        assertThrows(IllegalStateException.class, () -> runBootstrap("first-admin", ""));
        assertThrows(IllegalStateException.class, () -> runBootstrap("", PASSWORD));
        assertThrows(IllegalStateException.class, () -> runBootstrap("first-admin", "short"));
        assertThrows(IllegalStateException.class, () -> runBootstrap("first-admin", "a".repeat(73)));
        assertTrue(users.findByUsername("first-admin").isEmpty());
    }

    private void runBootstrap(String username, String password) {
        var bootstrap = new BootstrapAdmin(users, credentials, encoder, validator, username, password);
        new TransactionTemplate(transactions).executeWithoutResult(
                status -> bootstrap.run(new DefaultApplicationArguments()));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = loginAttempt(username, password);
        assertEquals(204, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MvcResult loginAttempt(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", username).param("password", password)).andReturn();
    }

    private String createJson(String username, String role, String password) {
        return JSON.writeValueAsString(Map.of("username", username, "role", role, "password", password));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertProblem(MvcResult result, int status) throws Exception {
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, result.getResponse().getContentType());
        JsonNode body = json(result);
        assertEquals(status, body.get("status").asInt());
        assertEquals(result.getRequest().getRequestURI(), body.get("instance").asString());
        assertTrue(body.get("detail").isString());
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
}
