package deu.service;

import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.model.enums.DayOfWeek;
import deu.repository.LectureRepository;
import deu.model.enums.Semester;

import java.time.LocalTime;
import java.util.List;

public class LectureService {

    // Singleton 인스턴스(static 인스턴스)
    private static final LectureService instance = new LectureService();
    
    //private 생성자
    private LectureService() {}
    
    //public 접근자
    public static LectureService getInstance() {
        return instance;
    }

    // 특정 강의실의 금일 + 6일 까지의 강의 데이터를 배열로 반환한다.
    public BasicResponse returnLectureOfWeek(LectureRequest payload) {
        String building = payload.building;
        String floor = payload.floor;
        String lectureroom = payload.lectureroom;

        // 1. 오늘 기준으로 7일간의 요일 배열을 가져옴 (오늘부터 일주일간 순차 조회용)
        DayOfWeek[] orderedDays = DayOfWeek.getOrderedFromToday();

        // 2. 스케줄 2차원 배열 생성 (행: 요일 7개, 열: 0교시~12교시 총 13개)
        Lecture[][] schedule = new Lecture[7][13];

        // 3. 전체 강의 목록을 로드 (Repository의 findAll()이 최신 데이터를 가져옴)
        List<Lecture> lectures = LectureRepository.getInstance().findAll();
        if (lectures.isEmpty()) {
            return new BasicResponse("404", "파일에서 강의 정보를 불러오지 못했습니다.");
        }

        // 4. 강의 목록 중 대상 강의실에 해당하는 강의만 필터링하고 시간표에 배치
        for (Lecture lec : lectures) {
            // 유효한 강의인지 (null 여부, 강의실/건물/층 일치, 요일/시간 존재 여부 등)
            if (!isValidLectureForRoom(lec, building, floor, lectureroom)) continue;

            //5.[수정] 한글("토")과 영어("SATURDAY") 요일을 DayOfWeek Enum 타입으로 변환
            DayOfWeek lecDay = convertToDayOfWeekEnum(lec.getDay());

            if (lecDay == null) {
                // 잘못된 요일 텍스트일 경우 무시하고 계속 진행
                System.err.println("[LectureService] 잘못된 요일: " + lec.getDay());
                continue;
            }

            // 변환된 요일이 현재 7일 배열(orderedDays)의 몇 번째에 해당하는지 확인
            int dayIndex = indexOfDay(orderedDays, lecDay);
            if (dayIndex == -1) continue; // 요일이 7일 범위에 없다면 스킵

            try {
                //시작 및 종료 시간을 LocalTime으로 파싱 ("14:00" → LocalTime.of(14, 0))
                LocalTime start = LocalTime.parse(lec.getStartTime());
                LocalTime end = LocalTime.parse(lec.getEndTime());

                // 각 교시에 대해 해당 강의가 포함되는지 체크하여 스케줄에 할당
                for (int period = 0; period < 13; period++) {
                    // 각 교시는 9시부터 1시간 간격 (9~10, 10~11, ..., 21~22)
                    LocalTime periodStart = LocalTime.of(9 + period, 0);
                    LocalTime periodEnd = periodStart.plusHours(1);

                    //[수정] 겹침 계산 로직 변경 (StartA < EndB AND EndA > StartB)
                    // (11:00 종료 강의가 11:00~12:00 슬롯을 포함하지 않도록 수정)
                    if (start.isBefore(periodEnd) && end.isAfter(periodStart)) {
                        schedule[dayIndex][period] = lec;
                    }
                }
            } catch (Exception e) {
                // 시간 형식이 잘못된 경우 오류 출력하고 해당 강의 건너뜀
                System.err.println("[LectureService] 시간 파싱 오류: " + lec.getStartTime() + " ~ " + lec.getEndTime());
            }
        }

        // 최종 결과 반환 (스케줄 배열을 포함한 응답 객체)
        return new BasicResponse("200", schedule);
    }
    
    //(연/학기 + 강의실 필터 기반 강의 조회)
    public List<Lecture> findLectures(Integer year, String semester, String building, String floor, String lectureroom) {
        if (year == null || semester == null || building == null || floor == null || lectureroom == null) {
            return List.of();
        }
        
        Semester sem;
        try {
            sem = Semester.valueOf(semester.toUpperCase());
        } catch (IllegalArgumentException e) {
            sem = Semester.FIRST; // 기본값 보정
        }

        // LectureRepository 통해 실제 YAML 데이터 조회
        return LectureRepository.getInstance().findRoomLectures(year, sem, building, floor, lectureroom);
    }

    // 건물, 층, 강의실, 요일, 시간 정보가 모두 유효한지 확인
    private boolean isValidLectureForRoom(Lecture lec, String building, String floor, String room) {
        return lec != null &&
                building != null && building.equals(lec.getBuilding()) &&
                floor != null && floor.equals(lec.getFloor()) &&
                room != null && room.equals(lec.getLectureroom()) &&
                lec.getDay() != null &&
                lec.getStartTime() != null &&
                lec.getEndTime() != null;
    }

    //[추가] 한글 및 영어 요일을 DayOfWeek Enum으로 변환하는 헬퍼 메서드
    private DayOfWeek convertToDayOfWeekEnum(String day) {
        if (day == null) return null;
        
        return switch (day.toUpperCase()) { // 대소문자 무시
            case "월", "MONDAY" -> DayOfWeek.MONDAY;
            case "화", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "수", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "목", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "금", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "토", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "일", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null; // 잘못된 값이면 null
        };
    }

    //요일 배열에서 특정 요일의 인덱스 반환
    private int indexOfDay(DayOfWeek[] array, DayOfWeek target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) return i;
        }
        return -1;
    }
}