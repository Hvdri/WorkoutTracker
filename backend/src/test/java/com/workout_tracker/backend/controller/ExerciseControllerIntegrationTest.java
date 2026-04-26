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

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // each test rolls back — no fixture leakage between methods or test classes
class ExerciseControllerIntegrationTest {

    // Mirrors the admin credentials seeded by DataInitializer under the test profile.
    // Centralized so a drift in application-test.yaml fails one constant, not eight call sites.
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ── Public reads ─────────────────────────────────────────────────────────

    @Test
    void list_isPublic_returnsSeededCatalog() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.content[0].name").exists())
                .andExpect(jsonPath("$.content[0].muscleGroup").exists());
    }

    @Test
    void list_filterByMuscleGroup_returnsOnlyThatGroup() throws Exception {
        mockMvc.perform(get("/api/exercises").param("muscleGroup", "CHEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].muscleGroup",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("CHEST"))));
    }

    @Test
    void list_filterByName_returnsCaseInsensitiveMatches() throws Exception {
        // Self-contained: create a uniquely-named row and search for a substring of its name.
        // Doesn't rely on any specific seed entry.
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        createExercise(adminToken, "ZebraPressX1", "CHEST");

        mockMvc.perform(get("/api/exercises").param("name", "zebrapress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[*].name",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.containsStringIgnoringCase("zebrapress"))));
    }

    @Test
    void list_combinedFilters_appliesBoth() throws Exception {
        // Self-contained: insert two distinct exercises with the same name fragment under
        // different muscle groups, then assert combined filter only returns the one in CHEST.
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        createExercise(adminToken, "QuokkaCombo1", "CHEST");
        createExercise(adminToken, "QuokkaCombo2", "BACK");

        mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", "CHEST")
                        .param("name", "quokkacombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].muscleGroup",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("CHEST"))))
                .andExpect(jsonPath("$.content[*].name",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.containsStringIgnoringCase("quokkacombo"))));
    }

    @Test
    void getById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/exercises/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── Admin-only writes — auth boundary ────────────────────────────────────

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Exercise","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withUserRole_returns403() throws Exception {
        String userToken = registerAndGetToken("regularuser", "regular@test.com", "password123");

        mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"User Exercise","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/exercises/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_withUserRole_returns403() throws Exception {
        String userToken = registerAndGetToken("updateuser", "updateuser@test.com", "password123");

        mockMvc.perform(put("/api/exercises/{id}", 1)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/exercises/{id}", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_withUserRole_returns403() throws Exception {
        String userToken = registerAndGetToken("deleteuser", "deleteuser@test.com", "password123");

        mockMvc.perform(delete("/api/exercises/{id}", 1)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ── Admin happy paths ────────────────────────────────────────────────────

    @Test
    void create_withAdminRole_returns201_thenDelete_returns204() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult created = mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cable Crossover","muscleGroup":"CHEST","description":"Cable fly variant"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/exercises/{id}", id.longValue())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_withAdminRole_returns200AndPersistsChanges() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        MvcResult created = mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cable Pullover","muscleGroup":"BACK"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/exercises/{id}", id.longValue())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cable Pullover","muscleGroup":"BACK","description":"Updated desc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated desc"));

        // Verify the change is persisted, not just echoed
        mockMvc.perform(get("/api/exercises/{id}", id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated desc"));
    }

    @Test
    void update_renameToExistingName_returns409() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        // Self-contained: create both source and target inside the test so this doesn't
        // break when the seed list changes.
        long sourceId = createExercise(adminToken, "RenameSourceA", "CHEST");
        createExercise(adminToken, "RenameTargetA", "CHEST");

        mockMvc.perform(put("/api/exercises/{id}", sourceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"RenameTargetA","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void update_unknownId_returns404() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(put("/api/exercises/{id}", 999_999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phantom","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(delete("/api/exercises/{id}", 999_999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        // Self-contained: insert a unique row first, then attempt the duplicate.
        createExercise(adminToken, "DupCheckOriginal", "CHEST");

        mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"DupCheckOriginal","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    void create_emptyName_returns400WithFieldError() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","muscleGroup":"CHEST"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_missingMuscleGroup_returns400WithFieldError() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Some Lift"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.muscleGroup").exists());
    }

    @Test
    void create_nameTooLong_returns400WithFieldError() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        // 101 chars — DTO caps at 100
        String longName = "X".repeat(101);
        String body = "{\"name\":\"" + longName + "\",\"muscleGroup\":\"CHEST\"}";

        mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    // Creates an exercise as admin and returns its id. Lets tests build their own fixtures
    // instead of reaching into the seeded catalog.
    private long createExercise(String adminToken, String name, String muscleGroup) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","muscleGroup":"%s"}
                                """.formatted(name, muscleGroup)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
