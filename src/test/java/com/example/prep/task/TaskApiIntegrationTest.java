package com.example.prep.task;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void taskLifecycle() throws Exception {
    String body = taskJson("Write report", "IN_PROGRESS", LocalDate.now().plusDays(3));

    String created =
        mockMvc
            .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(created, "$.id");

    mockMvc
        .perform(get("/api/v1/tasks/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Write report"));
    mockMvc
        .perform(get("/api/v1/tasks").param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].id", hasItem(id)))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("IN_PROGRESS"))))
        .andExpect(jsonPath("$.page.totalElements").exists());
    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskJson("Write report", "DONE", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
    mockMvc.perform(delete("/api/v1/tasks/{id}", id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/tasks/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void invalidInputReturnsFieldErrors() throws Exception {
    String body = taskJson("x".repeat(101), "TODO", LocalDate.now().minusDays(1));

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details[?(@.field == 'title')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'dueDate')]").exists());
  }

  @Test
  void missingTitleIsRejected() throws Exception {
    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("title"));
  }

  @Test
  void unknownStatusIsRejected() throws Exception {
    String body = "{\"title\":\"A\",\"status\":\"LATER\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void unknownTaskReturns404InCommonShape() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks/{id}", 999999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Task 999999 not found"))
        .andExpect(jsonPath("$.path").value("/api/v1/tasks/999999"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void invalidUpdateReportsRealFieldNames() throws Exception {
    String body = taskJson("", "TODO", LocalDate.now().minusDays(1));

    mockMvc
        .perform(put("/api/v1/tasks/{id}", 1).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[?(@.field == 'title')]").exists())
        .andExpect(jsonPath("$.details[?(@.field == 'dueDate')]").exists());
  }

  @Test
  void updateAndDeleteOfUnknownTaskReturn404() throws Exception {
    String body = taskJson("Title", "TODO", null);

    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", 999999).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/v1/tasks/{id}", 999999)).andExpect(status().isNotFound());
  }

  @Test
  void invalidStatusFilterReturnsPlainMessage() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks").param("status", "LATER"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid value 'LATER' for parameter 'status'"));
  }

  @Test
  void unknownSortPropertyReturns400() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks").param("sort", "nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Unknown property 'nope'"));
  }

  @Test
  void unsupportedContentTypeReturns415() throws Exception {
    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.TEXT_PLAIN).content("title"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415));
  }

  @Test
  void unsupportedMethodReturns405() throws Exception {
    mockMvc
        .perform(patch("/api/v1/tasks/{id}", 1))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  void nonPositiveIdIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks/{id}", 0))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("id"));
  }

  private String taskJson(String title, String status, LocalDate dueDate) {
    String due = dueDate == null ? "null" : "\"" + dueDate + "\"";
    return """
        {"title":"%s","description":"desc","status":"%s","dueDate":%s}
        """
        .formatted(title, status, due);
  }
}
