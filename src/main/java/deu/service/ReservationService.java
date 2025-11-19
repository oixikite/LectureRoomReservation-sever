package deu.service;

import deu.model.dto.request.data.reservation.DeleteRoomReservationRequest;
import deu.model.dto.request.data.reservation.RoomReservationLocationRequest;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.entity.RoomReservation;
import deu.repository.ReservationRepository;
import deu.model.dto.response.BasicResponse;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {

    // 싱글톤 인스턴스
    @Getter
    private static final ReservationService instance = new ReservationService();

    // [수정 1] 테스트가 가능하도록 Repository를 멤버 변수로 선언
    private ReservationRepository reservationRepository;

    private ReservationService() {
        // 생성 시점에 싱글톤 Repository를 할당
        this.reservationRepository = ReservationRepository.getInstance();
    }

    // 사용자 관점 ========================================================================================================
    // 예약 신청
    public BasicResponse createRoomReservation(RoomReservationRequest payload) {
        try {
            RoomReservation roomReservation = RoomReservation.builder()
                    .buildingName(payload.getBuildingName())
                    .floor(payload.getFloor())
                    .lectureRoom(payload.getLectureRoom())
                    .number(payload.getNumber())
                    .title(payload.getTitle())
                    .description(payload.getDescription())
                    .date(payload.getDate())
                    .dayOfTheWeek(payload.getDayOfTheWeek())
                    .startTime(payload.getStartTime())
                    .endTime(payload.getEndTime())
                    .purpose(payload.getPurpose())
                    .accompanyingStudentCount(payload.getAccompanyingStudentCount())
                    .accompanyingStudents(payload.getAccompanyingStudents())
                    .build();

            // [수정 2] 멤버 변수 사용
            ReservationRepository repo = this.reservationRepository;

            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(6);

            List<RoomReservation> userReservations = repo.findByUser(payload.getNumber());

            long countWithin7Days = userReservations.stream()
                    .filter(r -> {
                        try {
                            LocalDate date = LocalDate.parse(r.getDate());
                            return !date.isBefore(today) && !date.isAfter(maxDate);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            if (countWithin7Days >= 5) {
                return new BasicResponse("403", "오늘부터 7일 간 최대 5개의 예약만 가능합니다.");
            }

            for (RoomReservation r : userReservations) {
                if (r.getDate().equals(payload.getDate())
                        && r.getStartTime().equals(payload.getStartTime())) {
                    return new BasicResponse("409", "같은 시간대에 이미 예약이 존재합니다.");
                }
            }

            boolean isDup = repo.isDuplicate(
                    roomReservation.getDate(),
                    roomReservation.getStartTime(),
                    roomReservation.getLectureRoom()
            );

            if (isDup) {
                return new BasicResponse("409", "해당 시간에 다른 예약이 존재합니다.");
            }

            repo.save(roomReservation);
            repo.saveToFile(); // [수정 3] 파일 저장 추가
            return new BasicResponse("200", "예약이 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }

    // 개인별 예약 삭제
    public BasicResponse deleteRoomReservationFromUser(DeleteRoomReservationRequest payload) {
        ReservationRepository repo = this.reservationRepository;
        RoomReservation target = repo.findById(payload.roomReservationId);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        if (!target.getNumber().equals(payload.number)) {
            return new BasicResponse("403", "본인의 예약만 삭제할 수 있습니다.");
        }

        repo.deleteById(payload.roomReservationId);
        repo.saveToFile(); // [수정 3] 파일 저장 추가
        return new BasicResponse("200", "예약이 삭제되었습니다.");
    }

    // 개인별 주간 예약 조회
    public BasicResponse weekRoomReservationByUserNumber(String payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        List<RoomReservation> reservations = this.reservationRepository.findByUser(payload).stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getDate(), formatter);
                        return !date.isBefore(today) && !date.isAfter(today.plusDays(6));
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        for (RoomReservation r : reservations) {
            try {
                int dayIndex = (int) ChronoUnit.DAYS.between(today, LocalDate.parse(r.getDate(), formatter));
                int periodIndex = Integer.parseInt(r.getStartTime().split(":")[0]) - 9;
                if (dayIndex >= 0 && dayIndex < 7 && periodIndex >= 0 && periodIndex < 13) {
                    schedule[dayIndex][periodIndex] = r;
                }
            } catch (Exception ignored) {
            }
        }

        return new BasicResponse("200", schedule);
    }

    // 사용자별 예약 리스트 조회
    public BasicResponse getReservationsByUser(String payload) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(6);

        List<RoomReservation> reservations = this.reservationRepository
                .findByUser(payload).stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getDate(), formatter);
                        return !date.isBefore(today) && !date.isAfter(endDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        return new BasicResponse("200", reservations);
    }

    // 통합 관점 ==========================================================================================================
    // 예약 수정
    public BasicResponse modifyRoomReservation(RoomReservationRequest payload) {
        try {
            ReservationRepository repo = this.reservationRepository;

            RoomReservation original = repo.findById(payload.getId());
            if (original == null) {
                return new BasicResponse("404", "예약을 찾을 수 없습니다.");
            }

            // Builder 패턴을 사용해 새 객체 생성
            RoomReservation modifiedReservation = original.toBuilder()
                    .buildingName(payload.getBuildingName())
                    .floor(payload.getFloor())
                    .lectureRoom(payload.getLectureRoom())
                    .title(payload.getTitle())
                    .description(payload.getDescription())
                    .date(payload.getDate())
                    .dayOfTheWeek(payload.getDayOfTheWeek())
                    .startTime(payload.getStartTime())
                    .endTime(payload.getEndTime())
                    .purpose(payload.getPurpose())
                    .accompanyingStudentCount(payload.getAccompanyingStudentCount())
                    .accompanyingStudents(payload.getAccompanyingStudents())
                    .build();

            repo.save(modifiedReservation);
            repo.saveToFile(); // [수정 3] 파일 저장 추가

            return new BasicResponse("200", "예약이 수정되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "예약 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 건물 강의실별 주간 예약 조회
    public BasicResponse weekRoomReservationByLectureroom(RoomReservationLocationRequest payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        List<RoomReservation> reservations = this.reservationRepository.findAll().stream()
                .filter(r -> r.getBuildingName().equals(payload.building)
                && r.getFloor().equals(payload.floor)
                && r.getLectureRoom().equals(payload.lectureroom))
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getDate(), formatter);
                        return !date.isBefore(today) && !date.isAfter(today.plusDays(6));
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        for (RoomReservation r : reservations) {
            try {
                int dayIndex = (int) ChronoUnit.DAYS.between(today, LocalDate.parse(r.getDate(), formatter));
                int periodIndex = Integer.parseInt(r.getStartTime().split(":")[0]) - 9;
                if (dayIndex >= 0 && dayIndex < 7 && periodIndex >= 0 && periodIndex < 13) {
                    schedule[dayIndex][periodIndex] = r;
                }
            } catch (Exception ignored) {
            }
        }

        return new BasicResponse("200", schedule);
    }

    // 관리자 관점 ========================================================================================================
    // 관리자 예약 삭제
    public BasicResponse deleteRoomReservationFromManagement(String payload) {
        boolean deleted = this.reservationRepository.deleteById(payload);
        if (deleted) {
            this.reservationRepository.saveToFile();
            return new BasicResponse("200", "예약이 삭제되었습니다.");
        }
        return new BasicResponse("404", "예약을 찾을 수 없습니다.");
    }

    // 예약 상태 변경
    public BasicResponse changeRoomReservationStatus(String payload) {
        RoomReservation target = this.reservationRepository.findById(payload);
        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        RoomReservation approvedReservation = target.toBuilder()
                .status("승인")
                .build();

        this.reservationRepository.save(approvedReservation);
        this.reservationRepository.saveToFile(); // [수정 3] 파일 저장 추가
        return new BasicResponse("200", "예약 상태가 승인로 변경되었습니다.");
    }

    // 예약 상태가 "대기" 인 모든 예약 내역 반환
    public BasicResponse findAllRoomReservation() {
        List<RoomReservation> result = this.reservationRepository.findAll().stream()
                .filter(r -> "대기".equals(r.getStatus()))
                .toList();

        return new BasicResponse("200", result);
    }
}
