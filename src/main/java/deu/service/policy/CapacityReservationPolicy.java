package deu.service.policy;

import deu.repository.ReservationRepository;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.entity.RoomReservation;

import java.util.List;

public class CapacityReservationPolicy implements ReservationPolicy {

    private static final int MAX_PEOPLE = 3;  // 전체 강의실 정원 3명
    private static final double LIMIT_PERCENT = 0.5; // 50%

    @Override
    public void validate(RoomReservationRequest payload) throws Exception {

        ReservationRepository repo = ReservationRepository.getInstance();

        // 같은 날짜 + 같은 시간 + 같은 강의실의 예약자 조회
        List<RoomReservation> list = repo.findAll().stream()
                .filter(r -> r.getDate().equals(payload.getDate()))
                .filter(r -> r.getStartTime().equals(payload.getStartTime()))
                .filter(r -> r.getLectureRoom().equals(payload.getLectureRoom()))
                .filter(r -> !"삭제됨".equals(r.getStatus())) // 삭제된 예약은 제외
                .toList();

        int currentPeople = list.size();     // 현재 예약된 사람 수
        int limitPeople = (int) Math.ceil(MAX_PEOPLE * LIMIT_PERCENT); // 50% (=> 2명)

        if (currentPeople >= limitPeople) {
            throw new Exception("해당 시간대 예약 정원을 초과했습니다. (최대 3명 중 50% = 2명까지)");
        }
    }
}
