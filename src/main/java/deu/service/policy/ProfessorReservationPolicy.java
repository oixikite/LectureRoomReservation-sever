package deu.service.policy;

import deu.model.dto.request.data.reservation.RoomReservationRequest;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class ProfessorReservationPolicy implements ReservationPolicy {

    @Override
    public void validate(RoomReservationRequest payload) throws Exception {

        LocalTime start = LocalTime.parse(payload.getStartTime());
        LocalTime end = LocalTime.parse(payload.getEndTime());

        long minutes = ChronoUnit.MINUTES.between(start, end);

        if (!end.isAfter(start)) {
            throw new Exception("종료 시간은 시작 시간 이후여야 합니다.");
        }

        if (minutes > 180) {
            throw new Exception("교수 예약은 최대 3시간까지만 가능합니다.");
        }
    }
}
