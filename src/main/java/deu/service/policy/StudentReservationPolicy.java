package deu.service.policy;

import deu.model.dto.request.data.reservation.RoomReservationRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class StudentReservationPolicy implements ReservationPolicy {

    @Override
    public void validate(RoomReservationRequest payload) throws Exception {

        LocalDate today = LocalDate.now();
        LocalDate reservationDate = LocalDate.parse(payload.getDate());

        LocalTime start = LocalTime.parse(payload.getStartTime());
        LocalTime end = LocalTime.parse(payload.getEndTime());
        long minutes = ChronoUnit.MINUTES.between(start, end);

        if (!reservationDate.isAfter(today)) {
            throw new Exception("학생 예약은 최소 하루 전에만 가능합니다.");
        }

        if (!end.isAfter(start)) {
            throw new Exception("종료 시간은 시작 시간 이후여야 합니다.");
        }

        if (minutes > 120) {
            throw new Exception("학생 예약은 최대 2시간까지만 가능합니다.");
        }
    }
}
