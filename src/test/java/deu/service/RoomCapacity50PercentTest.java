package deu.service;

import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.dto.response.BasicResponse;
import deu.repository.ReservationRepository;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 정원(capacity) 50% 정책 통합 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RoomCapacity50PercentTest {

    private ReservationService service;
    private ReservationRepository repo;

    @BeforeEach
    void setUp() {
        service = ReservationService.getInstance();
        repo = ReservationRepository.getInstance();
        repo.clear();
        repo.saveToFile();
    }

    private RoomReservationRequest buildRequest(String number) {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setNumber(number);
        req.setBuildingName("정보관");
        req.setFloor("9");
        req.setLectureRoom("911");
        req.setTitle("테스트");
        req.setDescription("테스트");
        req.setDate("2025-12-30");
        req.setDayOfTheWeek("화");
        req.setStartTime("10:00");
        req.setEndTime("11:00");
        req.setPurpose("일반");
        return req;
    }

    @Test
    @Order(1)
    @DisplayName("1명 예약 → 성공")
    void testFirstReservationSuccess() {
        BasicResponse res = service.createRoomReservation(buildRequest("s0001"));
        assertEquals("200", res.code);
    }

    @Test
    @Order(2)
    @DisplayName("2명 예약 → 성공 (정원 3명 → 50% = 2명 가능)")
    void testSecondReservationSuccess() {
        service.createRoomReservation(buildRequest("s0001"));
        BasicResponse res = service.createRoomReservation(buildRequest("s0002"));

        assertEquals("200", res.code);
    }

    @Test
    @Order(3)
    @DisplayName("3명 예약 → 실패 (정원 3명 → 50% 초과)")
    void testThirdReservationFail() {
        service.createRoomReservation(buildRequest("s0001"));
        service.createRoomReservation(buildRequest("s0002"));

        BasicResponse res = service.createRoomReservation(buildRequest("s0003"));

        assertEquals("403", res.code);
        assertTrue(res.data.toString().contains("50%"));
    }
}
