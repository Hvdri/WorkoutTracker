package com.workout_tracker.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end coverage of the workout logging flow:
// register → create split → activate → add template → add exercise to template →
// create log → add exercise log → add set → complete → log appears in history as COMPLETED.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutLoggingFlowIntegrationTest {

    @Autowired WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void fullWorkoutLoggingFlow_endsWithCompletedLogInHistory() throws Exception {
        String token = registerAndGetToken("flowuser", "flowuser@test.com", "password123");

        // 1. Create split
        long splitId = extractId(postJson("/api/splits", token, """
                {"name":"Push/Pull/Legs"}
                """, status().isCreated()));

        // 2. Activate split
        mockMvc.perform(put("/api/splits/{id}/activate", splitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        // 3. Add a template
        long templateId = extractId(postJson(
                "/api/splits/" + splitId + "/templates", token, """
                        {"name":"Push","orderIndex":0}
                        """, status().isCreated()));

        // 4. Pick the first seeded exercise (deterministic — exercise catalog is sort-by-name)
        MvcResult exercisesResp = mockMvc.perform(get("/api/exercises").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();
        Number exerciseIdNum = JsonPath.read(
                exercisesResp.getResponse().getContentAsString(), "$.content[0].id");
        long exerciseId = exerciseIdNum.longValue();

        // 5. Add the exercise to the template
        postJson("/api/splits/" + splitId + "/templates/" + templateId + "/exercises", token,
                """
                        {"exerciseId":%d,"targetSets":3,"targetReps":8,"orderIndex":0}
                        """.formatted(exerciseId),
                status().isCreated());

        // 6. Create a workout log
        long logId = extractId(postJson("/api/logs", token, """
                {"date":"2026-04-01","templateId":%d,"notes":"warmup felt OK"}
                """.formatted(templateId), status().isCreated()));

        // 7. Add an exercise log under the workout log
        long exerciseLogId = extractId(postJson(
                "/api/logs/" + logId + "/exercises", token,
                """
                        {"exerciseId":%d}
                        """.formatted(exerciseId),
                status().isCreated()));

        // 8. Add a set: 80kg × 8 reps @ RPE 8
        postJson("/api/logs/" + logId + "/exercises/" + exerciseLogId + "/sets", token,
                """
                        {"setNumber":1,"weightKg":80.0,"reps":8,"rpe":8}
                        """, status().isCreated());

        // 9. Complete the log
        mockMvc.perform(post("/api/logs/{id}/complete", logId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 10. History contains the log with status COMPLETED + the set we added
        mockMvc.perform(get("/api/logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) logId))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].exerciseLogs[0].sets[0].weightKg").value(80.0))
                .andExpect(jsonPath("$.content[0].exerciseLogs[0].sets[0].reps").value(8));
    }

    @Test
    void createLog_withNoActiveSplit_returns409() throws Exception {
        String token = registerAndGetToken("noactive", "noactive@test.com", "password123");

        // No split has been created/activated, so any templateId triggers the rule check.
        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-04-01","templateId":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getSplit_byNonOwner_returns404() throws Exception {
        // 404 (not 403): standardized to avoid leaking existence to non-owners.
        String ownerToken = registerAndGetToken("ownerA", "ownerA@test.com", "password123");
        String attackerToken = registerAndGetToken("attackerB", "attackerB@test.com", "password123");

        long splitId = extractId(postJson("/api/splits", ownerToken, """
                {"name":"Mine"}
                """, status().isCreated()));

        mockMvc.perform(get("/api/splits/{id}", splitId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void activeSplit_endpoint_returnsActiveOr204() throws Exception {
        String token = registerAndGetToken("activetest", "activetest@test.com", "password123");

        // Initially: no active split → 204
        mockMvc.perform(get("/api/splits/active").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Create + activate, then it should return 200 with the split
        long splitId = extractId(postJson("/api/splits", token, """
                {"name":"PPL"}
                """, status().isCreated()));
        mockMvc.perform(put("/api/splits/{id}/activate", splitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/splits/active").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) splitId))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void activatingSecondSplit_deactivatesFirst() throws Exception {
        // Business rule #1 verified end-to-end.
        String token = registerAndGetToken("ruleone", "ruleone@test.com", "password123");

        long splitA = extractId(postJson("/api/splits", token, """
                {"name":"A"}
                """, status().isCreated()));
        long splitB = extractId(postJson("/api/splits", token, """
                {"name":"B"}
                """, status().isCreated()));

        mockMvc.perform(put("/api/splits/{id}/activate", splitA)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/splits/{id}/activate", splitB)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Active should now be B; A should be inactive
        mockMvc.perform(get("/api/splits/{id}", splitA)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
        mockMvc.perform(get("/api/splits/{id}", splitB)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void deleteSetLog_byOwner_succeeds_byOther_returns404() throws Exception {
        // Set up: owner creates a complete set chain.
        String ownerToken = registerAndGetToken("setowner", "setowner@test.com", "password123");
        String otherToken = registerAndGetToken("setother", "setother@test.com", "password123");

        long splitId = extractId(postJson("/api/splits", ownerToken, """
                {"name":"S"}
                """, status().isCreated()));
        mockMvc.perform(put("/api/splits/{id}/activate", splitId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        long templateId = extractId(postJson(
                "/api/splits/" + splitId + "/templates", ownerToken, """
                        {"name":"T","orderIndex":0}
                        """, status().isCreated()));

        MvcResult exercisesResp = mockMvc.perform(get("/api/exercises").param("size", "1"))
                .andExpect(status().isOk()).andReturn();
        long exerciseId = ((Number) JsonPath.read(
                exercisesResp.getResponse().getContentAsString(), "$.content[0].id")).longValue();

        long logId = extractId(postJson("/api/logs", ownerToken, """
                {"date":"2026-04-01","templateId":%d}
                """.formatted(templateId), status().isCreated()));
        long exerciseLogId = extractId(postJson(
                "/api/logs/" + logId + "/exercises", ownerToken,
                """
                        {"exerciseId":%d}
                        """.formatted(exerciseId), status().isCreated()));
        long setId = extractId(postJson(
                "/api/logs/" + logId + "/exercises/" + exerciseLogId + "/sets", ownerToken,
                """
                        {"setNumber":1,"weightKg":50.0,"reps":10}
                        """, status().isCreated()));

        // Other user tries to delete the set → 404 (ownership walks parent chain;
        // standardized on 404 to avoid leaking existence).
        mockMvc.perform(delete("/api/logs/{l}/exercises/{e}/sets/{s}",
                        logId, exerciseLogId, setId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // Owner deletes successfully
        mockMvc.perform(delete("/api/logs/{l}/exercises/{e}/sets/{s}",
                        logId, exerciseLogId, setId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSet_viaWrongLogIdInUrl_returns404() throws Exception {
        // Set up a real owned set, then try to delete it via a URL whose logId doesn't match.
        // Without path-chain validation this used to return 204 — now 404.
        String token = registerAndGetToken("pathchain", "pathchain@test.com", "password123");

        long splitId = extractId(postJson("/api/splits", token, """
                {"name":"S"}
                """, status().isCreated()));
        mockMvc.perform(put("/api/splits/{id}/activate", splitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        long templateId = extractId(postJson(
                "/api/splits/" + splitId + "/templates", token, """
                        {"name":"T","orderIndex":0}
                        """, status().isCreated()));
        long exerciseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/exercises").param("size", "1"))
                        .andReturn().getResponse().getContentAsString(),
                "$.content[0].id")).longValue();
        long logId = extractId(postJson("/api/logs", token, """
                {"date":"2026-04-01","templateId":%d}
                """.formatted(templateId), status().isCreated()));
        long exerciseLogId = extractId(postJson(
                "/api/logs/" + logId + "/exercises", token, """
                        {"exerciseId":%d}
                        """.formatted(exerciseId), status().isCreated()));
        long setId = extractId(postJson(
                "/api/logs/" + logId + "/exercises/" + exerciseLogId + "/sets", token, """
                        {"setNumber":1,"weightKg":50.0,"reps":10}
                        """, status().isCreated()));

        // Attempt deletion via a wrong logId in the path — the set is owned, but the URL lies.
        mockMvc.perform(delete("/api/logs/{l}/exercises/{e}/sets/{s}",
                        999_999L, exerciseLogId, setId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MvcResult postJson(String path, String token, String body, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(expectedStatus)
                .andReturn();
    }

    private long extractId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String registerAndGetToken(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"%s"}
                                """.formatted(username, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
