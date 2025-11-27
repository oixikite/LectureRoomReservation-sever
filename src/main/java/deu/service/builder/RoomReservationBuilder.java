package deu.service.builder;

import deu.model.entity.RoomReservation;
import deu.model.dto.request.data.reservation.AccompanyingStudent;

import java.util.List;

/**
 * AbstractBuilder 역할
 * - RoomReservation을 단계적으로 생성하기 위한 추상 빌더
 */
public abstract class RoomReservationBuilder {

    // 실제로 만들어질 예약 엔티티
    protected RoomReservation reservation = new RoomReservation();

    public abstract RoomReservationBuilder buildingName(String name);
    public abstract RoomReservationBuilder floor(String floor);
    public abstract RoomReservationBuilder lectureRoom(String room);
    public abstract RoomReservationBuilder title(String title);
    public abstract RoomReservationBuilder description(String description);
    public abstract RoomReservationBuilder date(String date);
    public abstract RoomReservationBuilder dayOfTheWeek(String dayOfTheWeek);
    public abstract RoomReservationBuilder startTime(String start);
    public abstract RoomReservationBuilder endTime(String end);
    public abstract RoomReservationBuilder number(String number);
    public abstract RoomReservationBuilder purpose(String purpose);
    public abstract RoomReservationBuilder accompanyingCount(Integer count);
    public abstract RoomReservationBuilder accompanyingStudents(List<AccompanyingStudent> students);
    public abstract RoomReservationBuilder status(String status);

    // 최종 완성된 RoomReservation 반환
    public RoomReservation build() {
        return reservation;
    }
}
