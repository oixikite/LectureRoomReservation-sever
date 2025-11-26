package deu.service.policy;

import deu.model.dto.request.data.reservation.RoomReservationRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

class StudentReservationPolicyTest {

    private final StudentReservationPolicy policy = new StudentReservationPolicy();

    // 하루전 예약 불가능 → 예외
    @Test
    void testStudentCannotReserveToday() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().toString());
        req.setStartTime("09:00");
        req.setEndTime("10:00");

        Exception ex = assertThrows(Exception.class, () -> policy.validate(req));
        assertEquals("학생 예약은 최소 하루 전에만 가능합니다.", ex.getMessage());
    }

    // 2시간 초과 예약 불가능
    @Test
    void testStudentMaxTwoHours() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().plusDays(1).toString());
        req.setStartTime("09:00");
        req.setEndTime("12:00"); // 3시간

        Exception ex = assertThrows(Exception.class, () -> policy.validate(req));
        assertEquals("학생 예약은 최대 2시간까지만 가능합니다.", ex.getMessage());
    }

    // 정상 예약 → 예외 없음
    @Test
    void testStudentValidReservation() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().plusDays(1).toString());
        req.setStartTime("09:00");
        req.setEndTime("11:00");

        assertDoesNotThrow(() -> policy.validate(req));
    }
}
