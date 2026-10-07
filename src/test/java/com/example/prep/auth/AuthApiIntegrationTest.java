package com.example.prep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prep.user.entity.Role;
import com.example.prep.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

  private static final String TEST_PASSWORD = "test-pass-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserService userService;
  @Autowired private JwtEncoder jwtEncoder;

  @Test
  void registerCreatesUserWithUserRole() throws Exception {
    String email = uniqueEmail();

    register(email, TEST_PASSWORD)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void duplicateEmailReturns409() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());

    register(email.toUpperCase(), TEST_PASSWORD)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void concurrentDuplicateRegistrationsYieldOneCreatedAndConflicts() throws Exception {
    String email = uniqueEmail();
    int attempts = 8;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(attempts);
    try {
      List<Future<MockHttpServletResponse>> results = new ArrayList<>();
      for (int i = 0; i < attempts; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return register(email, TEST_PASSWORD).andReturn().getResponse();
                }));
      }
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<MockHttpServletResponse> result : results) {
        MockHttpServletResponse response = result.get(60, TimeUnit.SECONDS);
        statuses.add(response.getStatus());
        if (response.getStatus() == 409) {
          assertThat(response.getContentAsString()).doesNotContain("SQL", "constraint", email);
        }
      }
      assertThat(statuses).containsOnly(201, 409);
      assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void invalidRegistrationReturns400WithFieldErrors() throws Exception {
    register("not-an-email", "short")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details[?(@.field == 'email')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'password')]").exists());
  }

  @Test
  void passwordOverBcryptByteLimitReturns400() throws Exception {
    String password = String.valueOf((char) 0xE9).repeat(40);

    register(uniqueEmail(), password)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[?(@.field == 'password')]").exists());
    login(uniqueEmail(), password)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[?(@.field == 'password')]").exists());
  }

  @Test
  void loginReturnsTokenThatUnlocksOwnProfile() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());

    String token = loginToken(email, TEST_PASSWORD);

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void loginResponseHasBearerTypeAnd15MinuteExpiry() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());

    login(email, TEST_PASSWORD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900));
  }

  @Test
  void wrongPasswordReturns401Json() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());

    login(email, "test-wrong-pass")
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void unknownEmailReturns401Json() throws Exception {
    login(uniqueEmail(), TEST_PASSWORD)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void missingTokenReturns401Json() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
  }

  @Test
  void invalidTokenReturns401Json() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void userTokenCannotListUsers() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());
    String token = loginToken(email, TEST_PASSWORD);

    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.path").value("/api/v1/users"));
  }

  @Test
  void adminTokenCanListUsers() throws Exception {
    String email = uniqueEmail();
    userService.create(email, TEST_PASSWORD, Role.ADMIN);
    String token = loginToken(email, TEST_PASSWORD);

    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.email == '" + email + "')].role").value("ADMIN"))
        .andExpect(jsonPath("$.content[*].passwordHash").doesNotExist())
        .andExpect(jsonPath("$.page.totalElements").exists());
  }

  @Test
  void userTokenCannotReachAdminListWithHead() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());
    String token = loginToken(email, TEST_PASSWORD);

    mockMvc
        .perform(head("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(head("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void signedTokenWithoutExpiryReturns401() throws Exception {
    String email = uniqueEmail();
    register(email, TEST_PASSWORD).andExpect(status().isCreated());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(email)
            .issuedAt(Instant.now())
            .claim("roles", List.of("ADMIN"))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  private ResultActions register(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentialsJson(email, password)));
  }

  private ResultActions login(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentialsJson(email, password)));
  }

  private String loginToken(String email, String password) throws Exception {
    String body =
        login(email, password)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  private String credentialsJson(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  private String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@example.com";
  }
}
