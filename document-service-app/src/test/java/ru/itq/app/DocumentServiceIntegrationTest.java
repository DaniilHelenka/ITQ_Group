package ru.itq.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void happyPath_createSubmitApprove() throws Exception {
        // 1. Create document
        String createBody = """
                {"author": "TestAuthor", "title": "Test Document", "initiator": "tester"}
                """;
        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.documentNumber").exists())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long docId = created.get("id").asLong();

        // 2. Submit
        String submitBody = """
                {"ids": [%d], "initiator": "tester"}
                """.formatted(docId);
        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId))
                .andExpect(jsonPath("$[0].result").value("success"));

        // 3. Approve
        String approveBody = """
                {"ids": [%d], "initiator": "tester"}
                """.formatted(docId);
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId))
                .andExpect(jsonPath("$[0].result").value("success"));

        // 4. Verify final state with history
        MvcResult getResult = mockMvc.perform(get("/api/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode doc = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(doc.get("history").size()).isEqualTo(2);
        assertThat(doc.get("history").get(0).get("action").asText()).isEqualTo("SUBMIT");
        assertThat(doc.get("history").get(1).get("action").asText()).isEqualTo("APPROVE");
    }

    @Test
    void batchSubmit_partialResults() throws Exception {
        // Create 2 documents
        long id1 = createDocument("Author1", "Doc1");
        long id2 = createDocument("Author2", "Doc2");
        long nonExistentId = 99999L;

        // Submit batch with one invalid id
        String submitBody = """
                {"ids": [%d, %d, %d], "initiator": "tester"}
                """.formatted(id1, id2, nonExistentId);

        MvcResult result = mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode results = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(results.size()).isEqualTo(3);

        assertThat(results.get(0).get("result").asText()).isEqualTo("success");
        assertThat(results.get(1).get("result").asText()).isEqualTo("success");
        assertThat(results.get(2).get("result").asText()).isEqualTo("not_found");
    }

    @Test
    void batchApprove_partialResults() throws Exception {
        // Create and submit 2 documents
        long id1 = createDocument("Author1", "DocApprove1");
        long id2 = createDocument("Author2", "DocApprove2");
        long id3 = createDocument("Author3", "DocApprove3");

        submitDocuments(id1, id2);

        // Approve batch: id1 (SUBMITTED), id2 (SUBMITTED), id3 (still DRAFT — conflict)
        String approveBody = """
                {"ids": [%d, %d, %d], "initiator": "tester"}
                """.formatted(id1, id2, id3);

        MvcResult result = mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode results = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(results.size()).isEqualTo(3);

        assertThat(results.get(0).get("result").asText()).isEqualTo("success");
        assertThat(results.get(1).get("result").asText()).isEqualTo("success");
        assertThat(results.get(2).get("result").asText()).isEqualTo("conflict");
    }

    @Test
    void approveRollback_onRegistryError() throws Exception {
        // Create and submit a document, then approve it
        long id1 = createDocument("Author1", "DocRollback");
        submitDocuments(id1);

        // First approve — success
        String approveBody = """
                {"ids": [%d], "initiator": "tester"}
                """.formatted(id1);
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("success"));

        // Second approve — should conflict (already APPROVED)
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("conflict"));

        // Document should still be APPROVED (not corrupted)
        mockMvc.perform(get("/api/documents/{id}", id1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void concurrentApproveTest() throws Exception {
        long docId = createDocument("ConcAuthor", "ConcDoc");
        submitDocuments(docId);

        String body = """
                {"documentId": %d, "threads": 5, "attempts": 10}
                """.formatted(docId);

        MvcResult result = mockMvc.perform(post("/api/documents/concurrent-approve-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("successCount").asInt()).isEqualTo(1);
        assertThat(response.get("finalStatus").asText()).isEqualTo("APPROVED");
        assertThat(response.get("conflictCount").asInt() + response.get("errorCount").asInt())
                .isEqualTo(9);
    }

    private long createDocument(String author, String title) throws Exception {
        String body = """
                {"author": "%s", "title": "%s", "initiator": "tester"}
                """.formatted(author, title);
        MvcResult result = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private void submitDocuments(long... ids) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(ids[i]);
        }
        sb.append("]");

        String body = """
                {"ids": %s, "initiator": "tester"}
                """.formatted(sb.toString());
        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
