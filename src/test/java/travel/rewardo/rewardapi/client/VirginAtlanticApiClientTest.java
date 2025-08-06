package travel.rewardo.rewardapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.rewardo.rewardapi.scraper.vs.client.VirginAtlanticApiClient;
import travel.rewardo.rewardapi.scraper.vs.model.api.AwardCalendar;
import travel.rewardo.rewardapi.scraper.vs.model.api.PointsDay;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirginAtlanticApiClientTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    @Mock
    private Response initialResponse;

    @Mock
    private Response secondResponse;

    @Mock
    private ResponseBody responseBody;

    private ObjectMapper objectMapper;
    private VirginAtlanticApiClient apiClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Register JSR310 module for Java 8 date/time types
        objectMapper.findAndRegisterModules();
        apiClient = new VirginAtlanticApiClient(httpClient, objectMapper);
    }

    @Test
    void fetchRewardSeatInfo_Success() throws IOException {
        // Given
        String requestBody = "{\"slice\":{\"origin\":\"LHR\",\"destination\":\"JFK\",\"departure\":\"2025-10-01\"},\"years\":[2025],\"months\":[\"OCTOBER\"],\"permittedCarriers\":[\"VS\"],\"passengers\":[\"ADULT\"]}";
        String responseJson = "[{\"date\":\"2025-10-01\",\"minPrice\":199.72,\"currency\":\"GBP\",\"minAwardPointsTotal\":10500,\"seats\":{\"awardEconomy\":{\"cabinPointsValue\":0,\"isSaverAward\":false,\"cabinClassSeatCount\":100,\"cabinClassSeatCountString\":\"100+\"},\"awardComfortPlusPremiumEconomy\":{\"cabinPointsValue\":0,\"isSaverAward\":false,\"cabinClassSeatCount\":50,\"cabinClassSeatCountString\":\"50+\"},\"awardBusiness\":{\"cabinPointsValue\":0,\"isSaverAward\":false,\"cabinClassSeatCount\":50,\"cabinClassSeatCountString\":\"50+\"}},\"pointsDays\":[{\"date\":\"2025-10-01\",\"minPrice\":399.72,\"currency\":\"GBP\",\"minAwardPointsTotal\":20000,\"seats\":{\"awardEconomy\":{\"cabinPointsValue\":34000,\"isSaverAward\":false,\"cabinClassSeatCount\":9,\"cabinClassSeatCountString\":\"9+\"},\"awardComfortPlusPremiumEconomy\":{\"cabinPointsValue\":20000,\"isSaverAward\":false,\"cabinClassSeatCount\":9,\"cabinClassSeatCountString\":\"9+\"},\"awardBusiness\":{\"cabinPointsValue\":170000,\"isSaverAward\":false,\"cabinClassSeatCount\":9,\"cabinClassSeatCountString\":\"9+\"}}}],\"month\":\"OCTOBER\",\"year\":\"2025\",\"totalAwardsSeatsForMonth\":200,\"originPrettyName\":null,\"destinationPrettyName\":null}]";
        
        // Mock first call
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(initialResponse, secondResponse);
        
        // Mock first response
        when(initialResponse.isSuccessful()).thenReturn(true);
        when(initialResponse.header("Location")).thenReturn("https://example.com/results");
        when(initialResponse.headers("Set-Cookie")).thenReturn(Collections.singletonList("session=abc123; Path=/; HttpOnly"));
        
        // Mock second response
        when(secondResponse.isSuccessful()).thenReturn(true);
        when(secondResponse.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(responseJson);
        
        // When
        List<AwardCalendar> result = apiClient.fetchRewardSeatInfo(requestBody);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        AwardCalendar calendar = result.get(0);
        assertEquals(LocalDate.parse("2025-10-01"), calendar.getDateFound());
        assertEquals(199.72, calendar.getMinPrice());
        assertEquals("GBP", calendar.getCurrency());
        assertEquals(10500, calendar.getMinAwardPointsTotal());
        
        // Check seats
        assertNotNull(calendar.getSeats());
        assertEquals(0, calendar.getSeats().getAwardEconomy().getCabinPointsValue());
        assertEquals(false, calendar.getSeats().getAwardEconomy().getIsSaverAward());
        assertEquals(100, calendar.getSeats().getAwardEconomy().getCabinClassSeatCount());
        
        // Check pointsDays
        assertNotNull(calendar.getPointsDays());
        assertEquals(1, calendar.getPointsDays().size());
        
        PointsDay pointsDay = calendar.getPointsDays().get(0);
        assertEquals(LocalDate.parse("2025-10-01"), pointsDay.getDateFound());
        assertEquals(399.72, pointsDay.getMinPrice());
        assertEquals(20000, pointsDay.getMinAwardPointsTotal());
        
        // Verify interactions
        verify(httpClient, times(2)).newCall(any(Request.class));
        verify(initialResponse).header("Location");
        verify(initialResponse).headers("Set-Cookie");
        verify(secondResponse).body();
    }

    @Test
    void fetchRewardSeatInfo_FirstRequestFails() throws IOException {
        // Given
        String requestBody = "{\"slice\":{\"origin\":\"LHR\",\"destination\":\"JFK\",\"departure\":\"2025-10-01\"},\"years\":[2025],\"months\":[\"OCTOBER\"],\"permittedCarriers\":[\"VS\"],\"passengers\":[\"ADULT\"]}";
        
        // Mock call
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(initialResponse);
        
        // Mock response
        when(initialResponse.isSuccessful()).thenReturn(false);
        when(initialResponse.code()).thenReturn(500);
        
        // When & Then
        IOException exception = assertThrows(IOException.class, () -> apiClient.fetchRewardSeatInfo(requestBody));
        assertTrue(exception.getMessage().contains("Unexpected response code: 500"));
        
        // Verify interactions
        verify(httpClient).newCall(any(Request.class));
        verify(initialResponse).isSuccessful();
    }

    @Test
    void fetchRewardSeatInfo_MissingLocationHeader() throws IOException {
        // Given
        String requestBody = "{\"slice\":{\"origin\":\"LHR\",\"destination\":\"JFK\",\"departure\":\"2025-10-01\"},\"years\":[2025],\"months\":[\"OCTOBER\"],\"permittedCarriers\":[\"VS\"],\"passengers\":[\"ADULT\"]}";
        
        // Mock call
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(initialResponse);
        
        // Mock response
        when(initialResponse.isSuccessful()).thenReturn(true);
        when(initialResponse.header("Location")).thenReturn(null);
        
        // When & Then
        IOException exception = assertThrows(IOException.class, () -> apiClient.fetchRewardSeatInfo(requestBody));
        assertEquals("Location header not found in the response", exception.getMessage());
        
        // Verify interactions
        verify(httpClient).newCall(any(Request.class));
        verify(initialResponse).isSuccessful();
        verify(initialResponse).header("Location");
    }
}