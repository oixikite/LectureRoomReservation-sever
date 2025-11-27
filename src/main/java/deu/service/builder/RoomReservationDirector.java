package deu.service.builder;

import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.entity.RoomReservation;

/**
 * Director 역할
 * - RoomReservationRequest(DTO)를 받아서
 *   RoomReservationBuilder를 이용해 RoomReservation을 조립
 */
public class RoomReservationDirector {

    private final RoomReservationBuilder builder;

    public RoomReservationDirector(RoomReservationBuilder builder) {
        this.builder = builder;
    }

    /**
     * @param req            클라이언트에서 받은 예약 요청 DTO
     * @param normalizedNumber  공백 제거/소문자 처리 등 끝낸 사용자 번호
     * @param initialStatus     교수/학생에 따라 결정된 초기 상태("승인"/"대기")
     */
    public RoomReservation construct(RoomReservationRequest req,
                                     String normalizedNumber,
                                     String initialStatus) {

        return builder
                .buildingName(req.getBuildingName())
                .floor(req.getFloor())
                .lectureRoom(req.getLectureRoom())
                .title(req.getTitle())
                .description(req.getDescription())
                .date(req.getDate())
                .dayOfTheWeek(req.getDayOfTheWeek())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .number(normalizedNumber)               // 여기서 trim된 번호 사용
                .purpose(req.getPurpose())
                .accompanyingCount(req.getAccompanyingStudentCount())
                .accompanyingStudents(req.getAccompanyingStudents())
                .status(initialStatus)                  // 교수/학생에 따라 "승인"/"대기"
                .build();
    }
}
