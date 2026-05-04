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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end coverage of the social flow described in ProjectContext Phase 4 / Scenario 5:
// userB completes a workout + posts it, userA follows userB, userA's feed shows the post.
// Also exercises the rule violations: self-follow, duplicate follow, posting an unfinished log.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialFlowIntegrationTest {

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
    void fullSocialFlow_followAndSeePostInFeed() throws Exception {
        // Two users: A is the viewer/follower, B is the author.
        String tokenA = registerAndGetToken("alice", "alice@test.com", "password123");
        String tokenB = registerAndGetToken("bob", "bob@test.com", "password123");
        long userIdB = readUserIdFromMyProfile(tokenB);

        // Bob completes a workout and posts it.
        long logIdB = bobCompletesAWorkout(tokenB);
        long postId = extractId(postJson("/api/posts", tokenB, """
                {"workoutLogId":%d,"caption":"PRs all day"}
                """.formatted(logIdB), status().isCreated()));

        // Alice's feed is empty until she follows Bob.
        mockMvc.perform(get("/api/social/feed").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Alice follows Bob.
        mockMvc.perform(post("/api/social/follow/{id}", userIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        // Following list reflects it.
        mockMvc.perform(get("/api/social/following").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) userIdB))
                .andExpect(jsonPath("$[0].username").value("bob"));

        // Bob's followers list reflects it from the other side.
        mockMvc.perform(get("/api/social/followers").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));

        // Alice's feed now contains Bob's post.
        mockMvc.perform(get("/api/social/feed").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) postId))
                .andExpect(jsonPath("$.content[0].username").value("bob"))
                .andExpect(jsonPath("$.content[0].caption").value("PRs all day"));

        // Unfollowing empties Alice's feed again.
        mockMvc.perform(delete("/api/social/follow/{id}", userIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/social/feed").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void selfFollow_returns409() throws Exception {
        // Rule #4 — verified end-to-end through the controller and exception handler.
        String token = registerAndGetToken("solo", "solo@test.com", "password123");
        long userId = readUserIdFromMyProfile(token);

        mockMvc.perform(post("/api/social/follow/{id}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("yourself")));
    }

    @Test
    void duplicateFollow_returns409() throws Exception {
        String tokenA = registerAndGetToken("dupA", "dupA@test.com", "password123");
        String tokenB = registerAndGetToken("dupB", "dupB@test.com", "password123");
        long userIdB = readUserIdFromMyProfile(tokenB);

        mockMvc.perform(post("/api/social/follow/{id}", userIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/social/follow/{id}", userIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void postingNonCompletedLog_returns409() throws Exception {
        // The log is created but never completed → POST /api/posts must reject with 409,
        // not silently expose a half-finished workout.
        String token = registerAndGetToken("inprog", "inprog@test.com", "password123");
        long logId = createLogButDoNotComplete(token);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutLogId":%d,"caption":"too soon"}
                                """.formatted(logId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("completed")));
    }

    @Test
    void profileUpdate_persistsAndReadsBack() throws Exception {
        // Sanity-check the profile path: PUT then GET reflects changes.
        String token = registerAndGetToken("profed", "profed@test.com", "password123");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"hi from test","fitnessGoal":"hypertrophy",
                                 "heightCm":180.0,"weightKg":75.0,"gender":"MALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("hi from test"))
                .andExpect(jsonPath("$.weightKg").value(75.0))
                .andExpect(jsonPath("$.gender").value("MALE"));

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fitnessGoal").value("hypertrophy"));
    }

    @Test
    void profileUpdate_emptyGenderString_returns400() throws Exception {
        // Regression: previously {"gender":""} silently cleared the field via PATCH semantics.
        // Now gender is a Gender enum, so Jackson rejects empty/unknown values at parse time.
        String token = registerAndGetToken("emptygen", "emptygen@test.com", "password123");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileUpdate_unknownGenderValue_returns400() throws Exception {
        String token = registerAndGetToken("badgen", "badgen@test.com", "password123");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":"gandalf"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileUpdate_blankFitnessGoal_returns400() throws Exception {
        // Free-form text — but @Pattern rejects empty/whitespace-only so it can't be
        // silently cleared by sending "" or "   ". Null still means "unchanged".
        String token = registerAndGetToken("blankgoal", "blankgoal@test.com", "password123");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fitnessGoal":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fitnessGoal").exists());

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fitnessGoal":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    // Walks the workout-domain prerequisites for posting: split → activate → template →
    // exercise → log → set → complete. Returns the completed log id.
    private long bobCompletesAWorkout(String token) throws Exception {
        long splitId = extractId(postJson("/api/splits", token, """
                {"name":"PPL"}
                """, status().isCreated()));
        mockMvc.perform(put("/api/splits/{id}/activate", splitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        long templateId = extractId(postJson(
                "/api/splits/" + splitId + "/templates", token, """
                        {"name":"Push","orderIndex":0}
                        """, status().isCreated()));
        long exerciseId = firstSeededExerciseId();
        postJson("/api/splits/" + splitId + "/templates/" + templateId + "/exercises", token,
                """
                        {"exerciseId":%d,"targetSets":3,"targetReps":8,"orderIndex":0}
                        """.formatted(exerciseId), status().isCreated());
        long logId = extractId(postJson("/api/logs", token, """
                {"date":"2026-04-15","templateId":%d}
                """.formatted(templateId), status().isCreated()));
        long exerciseLogId = extractId(postJson(
                "/api/logs/" + logId + "/exercises", token, """
                        {"exerciseId":%d}
                        """.formatted(exerciseId), status().isCreated()));
        postJson("/api/logs/" + logId + "/exercises/" + exerciseLogId + "/sets", token, """
                {"setNumber":1,"weightKg":80.0,"reps":8}
                """, status().isCreated());
        mockMvc.perform(post("/api/logs/{id}/complete", logId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return logId;
    }

    // Same chain, minus the /complete step — used to exercise the "log not completed" rule.
    private long createLogButDoNotComplete(String token) throws Exception {
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
        return extractId(postJson("/api/logs", token, """
                {"date":"2026-04-15","templateId":%d}
                """.formatted(templateId), status().isCreated()));
    }

    private long firstSeededExerciseId() throws Exception {
        MvcResult resp = mockMvc.perform(get("/api/exercises").param("size", "1"))
                .andExpect(status().isOk()).andReturn();
        return ((Number) JsonPath.read(
                resp.getResponse().getContentAsString(), "$.content[0].id")).longValue();
    }

    private long readUserIdFromMyProfile(String token) throws Exception {
        MvcResult resp = mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return ((Number) JsonPath.read(
                resp.getResponse().getContentAsString(), "$.userId")).longValue();
    }

    private MvcResult postJson(String path, String token, String body, ResultMatcher expectedStatus) throws Exception {
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
