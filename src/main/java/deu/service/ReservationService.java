package deu.service;

import deu.model.dto.request.data.reservation.DeleteRoomReservationRequest;
import deu.model.dto.request.data.reservation.RoomReservationLocationRequest;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.RoomReservation;
import deu.repository.ReservationRepository;
import deu.repository.RoomCapacityRepository;
import deu.service.policy.ProfessorReservationPolicy;
import deu.service.policy.ReservationPolicy;
import deu.service.policy.StudentReservationPolicy;
import lombok.Getter;

import deu.model.dto.response.NotificationDTO;
import deu.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {

    @Getter
    private static final ReservationService instance = new ReservationService();

    private ReservationService() {}

    // ======================================================================================================
    // 예약 신청
    // ======================================================================================================
    public BasicResponse createRoomReservation(RoomReservationRequest payload) {
        try {
            // === 0) 엔티티 생성 ===
            RoomReservation roomReservation = new RoomReservation();
            roomReservation.setBuildingName(payload.getBuildingName());
            roomReservation.setFloor(payload.getFloor());
            roomReservation.setLectureRoom(payload.getLectureRoom());
            roomReservation.setNumber(payload.getNumber());
            roomReservation.setTitle(payload.getTitle());
            roomReservation.setDescription(payload.getDescription());
            roomReservation.setDate(payload.getDate());
            roomReservation.setDayOfTheWeek(payload.getDayOfTheWeek());
            roomReservation.setStartTime(payload.getStartTime());
            roomReservation.setEndTime(payload.getEndTime());

            ReservationRepository repo = ReservationRepository.getInstance();

            // === 1) 학생/교수 정책 자동 선택 ===
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

            // === 2) 개별 정책 검증 ===
            try {
                policy.validate(payload);
            } catch (Exception ex) {
                return new BasicResponse("403", ex.getMessage());
            }

            // ===================================================================
            // 2-1) 정원(capacity) 정책: 정원의 50% 초과 시 예약 불가
            // ===================================================================
            try {
                RoomCapacityRepository capacityRepo = RoomCapacityRepository.getInstance();

                int capacity = capacityRepo.getCapacity(
                        payload.getBuildingName(),
                        payload.getFloor(),
                        payload.getLectureRoom()
                );

                if (capacity > 0) {
                    int limit = (int) Math.ceil(capacity * 0.5);  // ex) 3명 → 1.5 → 2명

                    List<RoomReservation> existing =
                            repo.findAll().stream()
                                    .filter(r -> r.getBuildingName().equals(payload.getBuildingName()))
                                    .filter(r -> r.getFloor().equals(payload.getFloor()))
                                    .filter(r -> r.getLectureRoom().equals(payload.getLectureRoom()))
                                    .filter(r -> r.getDate().equals(payload.getDate()))
                                    .filter(r -> r.getStartTime().equals(payload.getStartTime()))
                                    .filter(r -> !"삭제됨".equals(r.getStatus()))
                                    .toList();

                    if (existing.size() >= limit) {
                        return new BasicResponse(
                                "403",
                                "정원의 50%(" + limit + "명)를 초과하여 예약할 수 없습니다."
                        );
                    }
                }
            } catch (Exception ex) {
                return new BasicResponse("500", "정원 검증 오류: " + ex.getMessage());
            }

            // === 하루 시간 제한 (학생 120분 / 교수 180분) ===
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
                return new BasicResponse("403",
                        lower.startsWith("p")
                                ? "교수님은 하루 최대 3시간까지 예약 가능합니다."
                                : "학생은 하루 최대 2시간까지 예약 가능합니다."
                );
            }

            // === 7일간 최대 5회 ===
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

            // === 동일 사용자 중복 예약 방지 ===  
            for (RoomReservation r : repo.findByUser(number)) {
                if (r.getDate().equals(payload.getDate())
                        && r.getStartTime().equals(payload.getStartTime())) {
                    return new BasicResponse("409", "이미 해당 시간에 본인의 예약이 존재합니다.");
                }
            }

            // === 🚫 강의실 중복 방지 로직 삭제 완료 (여러 명 가능) ===
            // === 기존: isDuplicate() 제거함 ===


            // === 저장 ===
            repo.save(roomReservation);

            return new BasicResponse("200", "예약이 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }

    // ======================================================================================================
    // 개인 예약 삭제
    // ======================================================================================================
    public BasicResponse deleteRoomReservationFromUser(DeleteRoomReservationRequest payload) {
        RoomReservation target = ReservationRepository.getInstance().findById(payload.roomReservationId);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        if (!target.getNumber().equals(payload.number)) {
            return new BasicResponse("403", "본인의 예약만 삭제할 수 있습니다.");
        }

        ReservationRepository.getInstance().deleteById(payload.roomReservationId);
        return new BasicResponse("200", "예약이 삭제되었습니다.");
    }

    // ======================================================================================================
    // 개인 주간 예약 조회
    // ======================================================================================================
    public BasicResponse weekRoomReservationByUserNumber(String payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RoomReservation> list =
                ReservationRepository.getInstance().findByUser(payload).stream()
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
            } catch (Exception ignored) {}
        }

        return new BasicResponse("200", schedule);
    }

    // ======================================================================================================
    // 개인 예약 목록 조회
    // ======================================================================================================
    public BasicResponse getReservationsByUser(String payload) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RoomReservation> list =
                ReservationRepository.getInstance().findByUser(payload).stream()
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

    // ======================================================================================================
    // 예약 수정
    // ======================================================================================================
    public BasicResponse modifyRoomReservation(RoomReservationRequest payload) {
        try {
            ReservationRepository repo = ReservationRepository.getInstance();
            RoomReservation original = repo.findById(payload.getId());

            if (original == null) {
                return new BasicResponse("404", "예약을 찾을 수 없습니다.");
            }

            original.setBuildingName(payload.getBuildingName());
            original.setFloor(payload.getFloor());
            original.setLectureRoom(payload.getLectureRoom());
            original.setTitle(payload.getTitle());
            original.setDescription(payload.getDescription());
            original.setDate(payload.getDate());
            original.setDayOfTheWeek(payload.getDayOfTheWeek());
            original.setStartTime(payload.getStartTime());
            original.setEndTime(payload.getEndTime());

            // 알림 저장
            String title = "예약 수정";
            String message = String.format("[%s, %s] %s~%s 예약이 (관리자에 의해) 수정되었습니다.",
                    original.getLectureRoom(), original.getDate(), original.getStartTime(), original.getEndTime());
            saveNotification(original, title, message);

            repo.saveToFile();
            return new BasicResponse("200", "예약이 수정되었습니다.");

        } catch (Exception e) {
            return new BasicResponse("500", "예약 수정 중 오류 발생: " + e.getMessage());
        }
    }

    // ======================================================================================================
    // 강의실 기준 주간 예약 조회
    // ======================================================================================================
    public BasicResponse weekRoomReservationByLectureroom(RoomReservationLocationRequest payload) {
        RoomReservation[][] schedule = new RoomReservation[7][13];
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<RoomReservation> list =
                ReservationRepository.getInstance().findAll().stream()
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
            } catch (Exception ignored) {}
        }

        return new BasicResponse("200", schedule);
    }

    // ======================================================================================================
    // 관리자: 삭제
    // ======================================================================================================
    public BasicResponse deleteRoomReservationFromManagement(String payload) {
        RoomReservation target = ReservationRepository.getInstance().findById(payload);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        target.setStatus("삭제됨");

        // 알림 저장
        String title = "예약 취소";
        String message = String.format("[%s, %s] %s~%s 예약이 (관리자에 의해) 취소되었습니다.",
                target.getLectureRoom(), target.getDate(), target.getStartTime(), target.getEndTime());
        saveNotification(target, title, message);

        ReservationRepository.getInstance().saveToFile();

        return new BasicResponse("200", "예약이 '삭제됨' 상태로 변경되었습니다.");
    }

    // ======================================================================================================
    // 관리자: 상태 승인
    // ======================================================================================================
    public BasicResponse changeRoomReservationStatus(String payload) {
        RoomReservation target = ReservationRepository.getInstance().findById(payload);

        if (target == null) {
            return new BasicResponse("404", "예약을 찾을 수 없습니다.");
        }

        if ("승인".equals(target.getStatus())) {
            return new BasicResponse("409", "이미 승인된 예약입니다.");
        }

        target.setStatus("승인");

        String title = "예약 승인";
        String message = String.format("[%s, %s] %s~%s 예약이 승인되었습니다.",
                target.getLectureRoom(), target.getDate(), target.getStartTime(), target.getEndTime());
        saveNotification(target, title, message);

        ReservationRepository.getInstance().saveToFile();

        return new BasicResponse("200", "예약 상태가 승인으로 변경되었습니다.");
    }

    // ======================================================================================================
    // 관리자: 대기 예약 조회
    // ======================================================================================================
    public BasicResponse findAllRoomReservation() {
        List<RoomReservation> list =
                ReservationRepository.getInstance().findAll().stream()
                        .filter(r -> "대기".equals(r.getStatus()))
                        .toList();

        return new BasicResponse("200", list);
    }

    // ======================================================================================================
    // 알림 저장 공통 메서드
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
