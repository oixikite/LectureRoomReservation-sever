package deu.service;

import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.service.policy.StudentReservationPolicy;
import deu.service.policy.ProfessorReservationPolicy;
import deu.service.policy.ReservationPolicy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

class ReservationServicePolicyTest {

    // 전략을 바꿨을 때 동작이 달라지는지 테스트
    @Test
    void testReservationServiceUsesStudentStrategy() {
        ReservationPolicy student = new StudentReservationPolicy();

        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().toString()); // 오늘 예약 → 학생은 불가
        req.setStartTime("09:00");
        req.setEndTime("10:00");

        Exception ex = assertThrows(Exception.class, () -> student.validate(req));
        assertEquals("학생 예약은 최소 하루 전에만 가능합니다.", ex.getMessage());
    }

    @Test
    void testReservationServiceUsesProfessorStrategy() {
        ReservationPolicy prof = new ProfessorReservationPolicy();

        RoomReservationRequest req = new RoomReservationRequest();
        req.setDate(LocalDate.now().toString()); // 오늘 예약 → 교수는 가능
        req.setStartTime("09:00");
        req.setEndTime("10:00");

        assertDoesNotThrow(() -> prof.validate(req));
    }
}
