package com.example.prep.url;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prep.url.entity.ShortUrl;
import com.example.prep.url.repository.ShortUrlRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ShortUrlRepository shortUrlRepository;

  @Test
  void shortenRedirectAndCountVisits() throws Exception {
    String url = "https://example.com/articles/42?ref=home";
    String created =
        shorten(url, null)
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.originalUrl").value(url))
            .andExpect(jsonPath("$.createdAt").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String code = JsonPath.read(created, "$.code");

    mockMvc
        .perform(get("/r/{code}", code))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", url));
    mockMvc.perform(get("/r/{code}", code)).andExpect(status().isFound());

    mockMvc
        .perform(get("/api/v1/urls/{code}/stats", code).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.shortUrl", endsWith("/r/" + code)))
        .andExpect(jsonPath("$.originalUrl").value(url))
        .andExpect(jsonPath("$.visitCount").value(2))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void shorteningSameUrlTwiceReturnsExistingLink() throws Exception {
    String url = "https://example.com/same";
    String first =
        shorten(url, null)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String code = JsonPath.read(first, "$.code");

    shorten(url, null).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
    shorten(url, Instant.now().plus(1, ChronoUnit.DAYS))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code", not(code)));
  }

  @Test
  void invalidUrlsAreRejected() throws Exception {
    String[] invalid = {"not a url", "ftp://example.com/file", "/relative/path", "https://"};
    for (String url : invalid) {
      shorten(url, null)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Validation failed"))
          .andExpect(jsonPath("$.details[0].field").value("url"));
    }
    shorten("https://example.com/" + "a".repeat(2048), null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("url"));
  }

  @Test
  void pastExpiryIsRejected() throws Exception {
    shorten("https://example.com/past", Instant.now().minus(1, ChronoUnit.HOURS))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("expiresAt"));
  }

  @Test
  void unknownCodeReturns404() throws Exception {
    mockMvc
        .perform(get("/r/{code}", "zzzzzzz"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    mockMvc
        .perform(get("/api/v1/urls/{code}/stats", "zzzzzzz").with(jwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void malformedCodeReturns400() throws Exception {
    mockMvc.perform(get("/r/{code}", "toolongcode")).andExpect(status().isBadRequest());
  }

  @Test
  void expiredCodeReturns410ButKeepsStats() throws Exception {
    ShortUrl expired = new ShortUrl();
    expired.setCode("expd0001");
    expired.setOriginalUrl("https://example.com/expired");
    expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
    shortUrlRepository.save(expired);

    mockMvc
        .perform(get("/r/{code}", "expd0001"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.status").value(410));
    mockMvc
        .perform(get("/api/v1/urls/{code}/stats", "expd0001").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visitCount").value(0));
  }

  private ResultActions shorten(String url, Instant expiresAt) throws Exception {
    String expiry = expiresAt == null ? "null" : "\"" + expiresAt + "\"";
    String body = "{\"url\":\"" + url + "\",\"expiresAt\":" + expiry + "}";
    return mockMvc.perform(
        post("/api/v1/urls").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body));
  }
}
