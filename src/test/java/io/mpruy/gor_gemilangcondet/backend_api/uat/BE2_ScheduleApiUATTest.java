package io.mpruy.gor_gemilangcondet.backend_api.uat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UAT — PBI-BE2: Broadcast + REST API Jadwal
 *
 * Skenario yang dicakup:
 *   BE1-01 · Endpoint jadwal merespons dengan 200 dan data timeSlots
 *   BE2-02 · Klien baru mendapat data terkini (REST API mengembalikan snapshot terkini)
 *   BE2-03 · Pesan berisi date, timeSlots, lastUpdated
 *   BE2-04 · Booking baru tidak menghambat request jadwal lain (conflict → 409)
 *   FE3-01 · Respons memuat field lastUpdated
 *   FE3-03 · Manual refresh (GET ulang) mengembalikan data segar
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("BE2 · Schedule REST API Integration UAT")
class BE2_ScheduleApiUATTest {

    @Autowired private WebApplicationContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private static final String DATE = "2026-03-09";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BE1-01 · Koneksi dan respons jadwal
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE1-01 · GET /api/schedule mengembalikan 200 dengan timeSlots tidak kosong")
    void be1_01_scheduleEndpointReturns200WithData() throws Exception {
        mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value(DATE))
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots.length()").value(16));
    }

    @Test
    @DisplayName("BE1-01 · Jam pertama = 07:00, jam terakhir = 22:00")
    void be1_01_gridStartsAt7AndEndsAt22() throws Exception {
        mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andExpect(jsonPath("$.timeSlots[0].time").value("07:00"))
                .andExpect(jsonPath("$.timeSlots[15].time").value("22:00"));
    }

    @Test
    @DisplayName("BE1-01 · Setiap baris jam memiliki 6 slot lapangan")
    void be1_01_eachRowHasSixSlots() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        var timeSlots = body.get("timeSlots");

        assertThat(timeSlots.isArray()).isTrue();
        for (var row : timeSlots) {
            int slotCount = row.get("slots").size();
            assertThat(slotCount).isEqualTo(6)
                    .withFailMessage("Baris %s punya %d slot, seharusnya 6",
                            row.get("time").asText(), slotCount);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BE2-02 · Klien baru mendapat data terkini
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE2-02 · Setelah booking baru dibuat, GET jadwal langsung memuat slot terisi")
    void be2_02_newClientSeesLatestBookingStatus() throws Exception {
        // Arrange: buat booking di lapangan 1 jam 10:00
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "courtId", 1,
                "date", DATE,
                "time", "10:00",
                "customerName", "Tono"
        ));

        mockMvc.perform(post("/api/test/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody))
                .andExpect(status().isCreated());

        // Act: GET jadwal dari "klien baru"
        MvcResult result = mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());

        // Assert: slot jam 10:00 lapangan 1 berstatus BOOKED
        boolean found = false;
        for (var row : body.get("timeSlots")) {
            if ("10:00".equals(row.get("time").asText())) {
                var firstSlot = row.get("slots").get(0);
                assertThat(firstSlot.get("status").asText()).isEqualTo("BOOKED");
                found = true;
                break;
            }
        }
        assertThat(found).isTrue().withFailMessage("Baris 10:00 tidak ditemukan di respons");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BE2-03 · Payload respons memuat semua field wajib
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE2-03 · Respons jadwal memuat field: date, lastUpdated, timeSlots")
    void be2_03_responseHasAllRequiredTopLevelFields() throws Exception {
        mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.timeSlots").exists());
    }

    @Test
    @DisplayName("BE2-03 · Setiap slot memuat: courtName, status, lapanganId")
    void be2_03_eachSlotHasRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());

        for (var row : body.get("timeSlots")) {
            for (var slot : row.get("slots")) {
                assertThat(slot.has("courtName")).isTrue();
                assertThat(slot.has("status")).isTrue();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BE2-04 · Booking duplikat tidak mengganggu klien lain
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE2-04 · Booking ke slot yang sudah terisi mengembalikan 409, jadwal tetap konsisten")
    void be2_04_duplicateBookingReturnConflict() throws Exception {
        // Arrange: buat booking pertama
        String body = objectMapper.writeValueAsString(Map.of(
                "courtId", 2, "date", DATE, "time", "11:00", "customerName", "A"
        ));
        mockMvc.perform(post("/api/test/book").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Act: booking duplikat di slot yang sama
        String dupBody = objectMapper.writeValueAsString(Map.of(
                "courtId", 2, "date", DATE, "time", "11:00", "customerName", "B"
        ));
        mockMvc.perform(post("/api/test/book").contentType(MediaType.APPLICATION_JSON).content(dupBody))
                .andExpect(status().isConflict());

        // Assert: jadwal masih bisa diambil (tidak ada error)
        mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlots.length()").value(16));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // FE3-01 / FE3-03 · lastUpdated dan Manual Refresh
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FE3-01 · Respons selalu memuat field lastUpdated (tidak null)")
    void fe3_01_lastUpdatedFieldAlwaysPresent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/schedule").param("date", DATE))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("lastUpdated").isNull()).isFalse();
        assertThat(body.get("lastUpdated").asText()).isNotBlank();
    }

    @Test
    @DisplayName("FE3-03 · Dua GET berurutan (manual refresh) mengembalikan data konsisten")
    void fe3_03_twoConsecutiveGetsReturnConsistentData() throws Exception {
        MvcResult first  = mockMvc.perform(get("/api/schedule").param("date", DATE)).andReturn();
        MvcResult second = mockMvc.perform(get("/api/schedule").param("date", DATE)).andReturn();

        var bodyFirst  = objectMapper.readTree(first.getResponse().getContentAsString());
        var bodySecond = objectMapper.readTree(second.getResponse().getContentAsString());

        assertThat(bodyFirst.get("timeSlots").size())
                .isEqualTo(bodySecond.get("timeSlots").size());
        assertThat(bodyFirst.get("date").asText())
                .isEqualTo(bodySecond.get("date").asText());
    }

    @Test
    @DisplayName("TestBookingController · DELETE /api/test/book/{id} membatalkan booking")
    void testController_cancelBookingReturnsOk() throws Exception {
        // Buat booking terlebih dahulu
        String body = objectMapper.writeValueAsString(Map.of(
                "courtId", 5, "date", DATE, "time", "17:00", "customerName", "Tari"
        ));
        MvcResult createResult = mockMvc.perform(post("/api/test/book")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String bookingId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // DELETE — batalkan booking
        mockMvc.perform(delete("/api/test/book/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("TestBookingController · DELETE dengan ID tidak ada mengembalikan 404")
    void testController_cancelNonExistentBookingReturns404() throws Exception {
        mockMvc.perform(delete("/api/test/book/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tidak ada Authorization header — semua bookerName null (guest mode)")
    void noAuth_allBookerNamesAreNull() throws Exception {
        // Buat booking dulu
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "courtId", 3, "date", DATE, "time", "08:00", "customerName", "Rani"
        ));
        mockMvc.perform(post("/api/test/book").contentType(MediaType.APPLICATION_JSON).content(bookingBody))
                .andExpect(status().isCreated());

        // GET jadwal tanpa Authorization header
        MvcResult result = mockMvc.perform(get("/api/schedule").param("date", DATE)).andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());

        for (var row : body.get("timeSlots")) {
            for (var slot : row.get("slots")) {
                if ("BOOKED".equals(slot.get("status").asText())) {
                    var bookerNode = slot.path("bookerName");
                    boolean absent = bookerNode.isNull() || bookerNode.isMissingNode();
                    assertThat(absent).isTrue()
                            .withFailMessage("Guest tidak boleh lihat bookerName pada slot %s",
                                    slot.path("courtName").asText());
                }
            }
        }
    }
}
