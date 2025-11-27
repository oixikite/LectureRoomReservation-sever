package deu.service.builder;

import deu.model.dto.request.data.reservation.AccompanyingStudent;
import deu.model.entity.RoomReservation;

import java.util.List;

/**
 * ConcreteBuilder 역할
 * - 실제로 RoomReservation의 각 필드를 채우는 구현 클래스
 */
public class DefaultRoomReservationBuilder extends RoomReservationBuilder {

    @Override
    public RoomReservationBuilder buildingName(String name) {
        reservation.setBuildingName(name);
        return this;
    }

    @Override
    public RoomReservationBuilder floor(String floor) {
        reservation.setFloor(floor);
        return this;
    }

    @Override
    public RoomReservationBuilder lectureRoom(String room) {
        reservation.setLectureRoom(room);
        return this;
    }

    @Override
    public RoomReservationBuilder title(String title) {
        reservation.setTitle(title);
        return this;
    }

    @Override
    public RoomReservationBuilder description(String description) {
        reservation.setDescription(description);
        return this;
    }

    @Override
    public RoomReservationBuilder date(String date) {
        reservation.setDate(date);
        return this;
    }

    @Override
    public RoomReservationBuilder dayOfTheWeek(String dayOfTheWeek) {
        reservation.setDayOfTheWeek(dayOfTheWeek);
        return this;
    }

    @Override
    public RoomReservationBuilder startTime(String start) {
        reservation.setStartTime(start);
        return this;
    }

    @Override
    public RoomReservationBuilder endTime(String end) {
        reservation.setEndTime(end);
        return this;
    }

    @Override
    public RoomReservationBuilder number(String number) {
        reservation.setNumber(number);
        return this;
    }

    @Override
    public RoomReservationBuilder purpose(String purpose) {
        reservation.setPurpose(purpose);
        return this;
    }

    @Override
    public RoomReservationBuilder accompanyingCount(Integer count) {
        if (count != null) {
            reservation.setAccompanyingStudentCount(count);
        }
        return this;
    }

    @Override
    public RoomReservationBuilder accompanyingStudents(List<AccompanyingStudent> students) {
        reservation.setAccompanyingStudents(students);
        return this;
    }

    @Override
    public RoomReservationBuilder status(String status) {
        reservation.setStatus(status);
        return this;
    }
}
