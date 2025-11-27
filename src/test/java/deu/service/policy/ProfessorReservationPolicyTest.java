package deu.service.policy;

import deu.model.dto.request.data.reservation.RoomReservationRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

class ProfessorReservationPolicyTest {

    private final ProfessorReservationPolicy policy = new ProfessorReservationPolicy();

    // 종료 시간이 시작시간보다 빠르면 오류
    @Test
    void testProfessorEndTimeBeforeStartTime() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().plusDays(1).toString());
        req.setStartTime("10:00");
        req.setEndTime("09:00");

        Exception ex = assertThrows(Exception.class, () -> policy.validate(req));
        assertEquals("종료 시간은 시작 시간 이후여야 합니다.", ex.getMessage());
    }

    // 교수는 당일 예약 가능
    @Test
    void testProfessorCanReserveToday() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().toString());
        req.setStartTime("09:00");
        req.setEndTime("10:00");

        assertDoesNotThrow(() -> policy.validate(req));
    }

    // 정상
    @Test
    void testProfessorValidReservation() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().plusDays(3).toString());
        req.setStartTime("09:00");
        req.setEndTime("11:00");

        assertDoesNotThrow(() -> policy.validate(req));
    }
}
