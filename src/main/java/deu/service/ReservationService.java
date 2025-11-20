package deu.service;

import deu.model.dto.request.data.reservation.DeleteRoomReservationRequest;
import deu.model.dto.request.data.reservation.RoomReservationLocationRequest;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.dto.response.NotificationDTO;
import deu.model.entity.RoomReservation;
import deu.repository.ReservationRepository;
import deu.repository.RoomCapacityRepository;
import deu.service.policy.ProfessorReservationPolicy;
import deu.service.policy.ReservationPolicy;
import deu.service.policy.StudentReservationPolicy;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {

    @Getter
    private static final ReservationService instance = new ReservationService();

    // [Refactor] 테스트 용이성을 위해 Repository를 멤버 변수로 선언 (HEAD 반영)
    private ReservationRepository reservationRepository;

    private ReservationService() {
        // 생성 시점에 싱글톤 Repository를 할당
        this.reservationRepository = ReservationRepository.getInstance();
    }

    // 사용자 관점 ========================================================================================================
    // 예약 신청
    public BasicResponse createRoomReservation(RoomReservationRequest payload) {
        try {
            // [Refactor] Builder Pattern 사용 (HEAD 반영)
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

            ReservationRepository repo = this.reservationRepository;

            // [Feature] 1. 학생/교수 정책 자동 선택 및 검증 (Remote 반영)
            String number = payload.getNumber() == null ? "" : payload.getNumber().trim();
            String lower = number.toLowerCase();
            ReservationPolicy policy;

            if (lower.startsWith("p")) {
                policy = new ProfessorReservationPolicy();
            } else if (lower.startsWith("s")) {
                policy = new StudentReservationPolicy();
            } else {
                return new BasicResponse("400", "사용자 번호 형식이 올바르지 않습니다. (S**** / P****)");
            }

            try {
                policy.validate(payload);
            } catch (Exception ex) {
                return new BasicResponse("403", ex.getMessage());
            }

            // [Feature] 2. 정원(capacity) 정책: 정원의 50% 초과 시 예약 불가 (Remote 반영)
            try {
                RoomCapacityRepository capacityRepo = RoomCapacityRepository.getInstance();
                int capacity = capacityRepo.getCapacity(
                        payload.getBuildingName(),
                        payload.getFloor(),
                        payload.getLectureRoom()
                );

                if (capacity > 0) {
                    int limit = (int) Math.ceil(capacity * 0.5);

                    List<RoomReservation> existing = repo.findAll().stream()
                            .filter(r -> r.getBuildingName().equals(payload.getBuildingName()))
                            .filter(r -> r.getFloor().equals(payload.getFloor()))
                            .filter(r -> r.getLectureRoom().equals(payload.getLectureRoom()))
                            .filter(r -> r.getDate().equals(payload.getDate()))
                            .filter(r -> r.getStartTime().equals(payload.getStartTime()))
                            .filter(r -> !"삭제됨".equals(r.getStatus()))
                            .toList();

                    if (existing.size() >= limit) {
                        return new BasicResponse("403", "정원의 50%(" + limit + "명)를 초과하여 예약할 수 없습니다.");
                    }
                }
            } catch (Exception ex) {
                return new BasicResponse("500", "정원 검증 오류: " + ex.getMessage());
            }

            // [Feature] 3. 하루 시간 제한 검사 (Remote 반영)
            List<RoomReservation> todays = repo.findByUser(number).stream()
                    .filter(r -> r.getDate().equals(payload.getDate()))
                    .toList();

            int usedMinutes = 0;
            for (RoomReservation r : todays) {
                LocalTime s = LocalTime.parse(r.getStartTime());
                LocalTime e = LocalTime.parse(r.getEndTime());
                usedMinutes += (int) ChronoUnit.MINUTES.between(s, e);
            }

            int newMinutes = (int) ChronoUnit.MINUTES.between(
                    LocalTime.parse(payload.getStartTime()),
                    LocalTime.parse(payload.getEndTime())
            );

            int limitMinutes = lower.startsWith("p") ? 180 : 120;
            if (usedMinutes + newMinutes > limitMinutes) {
                return new BasicResponse("403", lower.startsWith("p")
                        ? "교수님은 하루 최대 3시간까지 예약 가능합니다."
                        : "학생은 하루 최대 2시간까지 예약 가능합니다.");
            }

            // [Feature] 4. 7일간 최대 5회 검사 (Remote 반영)
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(6);

            long countWithin7Days = repo.findByUser(number).stream()
                    .filter(r -> {
                        try {
                            LocalDate d = LocalDate.parse(r.getDate());
                            return !d.isBefore(today) && !d.isAfter(maxDate);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            if (countWithin7Days >= 5) {
                return new BasicResponse("403", "오늘부터 7일간 최대 5회까지만 예약 가능합니다.");
            }

            // [Feature] 5. 동일 사용자 중복 예약 방지 (Remote 반영)
            for (RoomReservation r : repo.findByUser(number)) {
                if (r.getDate().equals(payload.getDate())
                        && r.getStartTime().equals(payload.getStartTime())) {
                    return new BasicResponse("409", "이미 해당 시간에 본인의 예약이 존재합니다.");
                }
            }

            // (강의실 중복 방지 로직은 Remote 정책에 따라 제거됨)
            repo.save(roomReservation);
            repo.saveToFile(); // [Refactor] 파일 저장 명시
            return new BasicResponse("200", "예약이 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }

    // 개인별 예약 삭제 (HEAD 로직 유지)
    public BasicResponse deleteRoomReservationFromUser(DeleteRoomReservationRequest payload) {
        ReservationRepository repo = this.reservationRepository;
        RoomReservation target = repo.findById(payload.roomReservationId);
        
        //예약 존재 여부 확인
        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }
        
        //본인 확인
        if (!target.getNumber().equals(payload.number)) {
            return new BasicResponse("403", "본인의 예약만 삭제할 수 있습니다.");
        }
        
        //이미 취소된 예약인지 확인
        if ("취소".equals(target.getStatus())) {
             return new BasicResponse("409", "이미 취소된 예약입니다.");
        }
        
        //상태를 '취소'로 변경하여 새 객체 생성 (Builder 사용)
        RoomReservation cancelledReservation = target.toBuilder()
                .status("취소")
                .build();
        
        //저장 (Repository의 save는 ID가 같으면 덮어쓰도록 수정됨)
        repo.save(cancelledReservation);
        repo.saveToFile();

//        repo.deleteById(payload.roomReservationId);
//        repo.saveToFile();
        return new BasicResponse("200", "예약이 삭제되었습니다.");
        
    }

    // 개인별 주간 예약 조회
    public BasicResponse weekRoomReservationByUserNumber(String payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // [Refactor] 멤버 변수 사용 (HEAD) + 로직 (Remote) 병합
        List<RoomReservation> list = this.reservationRepository.findByUser(payload).stream()
                .filter(r -> {
                    try {
                        LocalDate d = LocalDate.parse(r.getDate(), fmt);
                        return !d.isBefore(today) && !d.isAfter(today.plusDays(6));
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        for (RoomReservation r : list) {
            try {
                int di = (int) ChronoUnit.DAYS.between(today, LocalDate.parse(r.getDate(), fmt));
                int pi = Integer.parseInt(r.getStartTime().split(":")[0]) - 9;
                if (di >= 0 && di < 7 && pi >= 0 && pi < 13) {
                    schedule[di][pi] = r;
                }
            } catch (Exception ignored) {
            }
        }

        return new BasicResponse("200", schedule);
    }

    // 사용자별 예약 리스트 조회
    public BasicResponse getReservationsByUser(String payload) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RoomReservation> list = this.reservationRepository.findByUser(payload).stream()
                .filter(r -> {
                    try {
                        LocalDate d = LocalDate.parse(r.getDate(), fmt);
                        return !d.isBefore(today) && !d.isAfter(end);
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        return new BasicResponse("200", list);
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

            // [Refactor] Builder 패턴으로 새 객체 생성 (HEAD 반영)
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

            // [Feature] 알림 저장 (Remote 반영)
            String title = "예약 수정";
            String message = String.format("[%s, %s] %s~%s 예약이 (관리자에 의해) 수정되었습니다.",
                    original.getLectureRoom(), original.getDate(), original.getStartTime(), original.getEndTime());
            saveNotification(original, title, message);

            repo.saveToFile();

            return new BasicResponse("200", "예약이 수정되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "예약 수정 중 오류 발생: " + e.getMessage());
        }
    }

    // 건물 강의실별 주간 예약 조회
    public BasicResponse weekRoomReservationByLectureroom(RoomReservationLocationRequest payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RoomReservation> list = this.reservationRepository.findAll().stream()
                .filter(r -> r.getBuildingName().equals(payload.building)
                && r.getFloor().equals(payload.floor)
                && r.getLectureRoom().equals(payload.lectureroom))
                .filter(r -> {
                    try {
                        LocalDate d = LocalDate.parse(r.getDate(), fmt);
                        return !d.isBefore(today) && !d.isAfter(today.plusDays(6));
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        for (RoomReservation r : list) {
            try {
                int di = (int) ChronoUnit.DAYS.between(today, LocalDate.parse(r.getDate(), fmt));
                int pi = Integer.parseInt(r.getStartTime().split(":")[0]) - 9;
                if (di >= 0 && di < 7 && pi >= 0 && pi < 13) {
                    schedule[di][pi] = r;
                }
            } catch (Exception ignored) {
            }
        }

        return new BasicResponse("200", schedule);
    }

    // 관리자 관점 ========================================================================================================
    // 관리자 예약 삭제 (Soft Delete & Notification - Remote 반영)
    public BasicResponse deleteRoomReservationFromManagement(String payload) {
        RoomReservation target = this.reservationRepository.findById(payload);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        // [Feature] Soft Delete 및 알림 (Remote 기능 우선)
        // (Builder를 써도 되지만, 상태 변경만 필요하므로 여기선 기존 객체를 변경하여 저장하거나 toBuilder 사용)
        RoomReservation deletedReservation = target.toBuilder()
                .status("삭제됨")
                .build();

        this.reservationRepository.save(deletedReservation);

        // 알림 저장
        String title = "예약 취소";
        String message = String.format("[%s, %s] %s~%s 예약이 (관리자에 의해) 취소되었습니다.",
                target.getLectureRoom(), target.getDate(), target.getStartTime(), target.getEndTime());
        saveNotification(target, title, message);

        this.reservationRepository.saveToFile();
        return new BasicResponse("200", "예약이 '삭제됨' 상태로 변경되었습니다.");
    }

    // 예약 상태 변경 (승인)
    public BasicResponse changeRoomReservationStatus(String payload) {
        RoomReservation target = this.reservationRepository.findById(payload);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        if ("승인".equals(target.getStatus())) {
            return new BasicResponse("409", "이미 승인된 예약입니다.");
        }

        // [Refactor] Builder 사용 (HEAD)
        RoomReservation approvedReservation = target.toBuilder()
                .status("승인")
                .build();

        this.reservationRepository.save(approvedReservation);

        // [Feature] 알림 저장 (Remote)
        String title = "예약 승인";
        String message = String.format("[%s, %s] %s~%s 예약이 승인되었습니다.",
                target.getLectureRoom(), target.getDate(), target.getStartTime(), target.getEndTime());
        saveNotification(target, title, message);

        this.reservationRepository.saveToFile();

        return new BasicResponse("200", "예약 상태가 승인으로 변경되었습니다.");
    }

    // 예약 상태가 "대기" 인 모든 예약 내역 반환
    public BasicResponse findAllRoomReservation() {
        List<RoomReservation> list = this.reservationRepository.findAll().stream()
                .filter(r -> "대기".equals(r.getStatus()))
                .toList();

        return new BasicResponse("200", list);
    }

    // ======================================================================================================
    // 알림 저장 공통 메서드 (Remote 반영)
    // ======================================================================================================
    private void saveNotification(RoomReservation reservation, String title, String message) {
        try {
            String studentId = reservation.getNumber();
            if (studentId == null || studentId.isBlank()) {
                return;
            }

            NotificationDTO notification = new NotificationDTO(
                    title,
                    message,
                    System.currentTimeMillis()
            );

            NotificationService.getInstance().addNotification(studentId, notification);

        } catch (Exception e) {
            System.err.println("[ReservationService] 알림 저장 오류: " + e.getMessage());
        }
    }
}
