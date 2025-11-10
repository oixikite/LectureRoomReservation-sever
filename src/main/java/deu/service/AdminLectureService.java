package deu.service;

import deu.model.entity.Lecture;
import deu.model.enums.Semester;
import deu.repository.LectureRepository;

import java.util.*;
import java.util.stream.Collectors;

public class AdminLectureService {

    private final LectureRepository repo = LectureRepository.getInstance();

    /** 특정 연/학기 + 건물/층/강의실의 전체 강의 목록 */
    public List<Lecture> listByRoom(int year, Semester semester,
                                    String building, String floor, String room) {
        return repo.findRoomLectures(year, semester, building, floor, room);
    }

    /** 특정 연/학기의 전체 강의 목록 */
    public List<Lecture> listByTerm(int year, Semester semester) {
        return repo.findAllByYearAndSemester(year, semester);
    }

    /** 신규 또는 수정 저장 (겹침 검증 포함) */
    public Result upsert(Lecture lecture) {
        // 기본 필수값 검증
        if (lecture == null ||
            isBlank(lecture.getId()) ||
            isBlank(lecture.getTitle()) ||
            isBlank(lecture.getBuilding()) ||
            isBlank(lecture.getFloor()) ||
            isBlank(lecture.getLectureroom()) ||
            isBlank(lecture.getDay()) ||
            isBlank(lecture.getStartTime()) ||
            isBlank(lecture.getEndTime()) ||
            lecture.getYear() == null ||
            lecture.getSemester() == null) {
            return Result.badRequest("필수 항목 누락");
        }

        // 시간 형식 간단 검증 (HH:mm)
        if (!validTime(lecture.getStartTime()) || !validTime(lecture.getEndTime()) ||
            lecture.getStartTime().compareTo(lecture.getEndTime()) >= 0) {
            return Result.badRequest("시간 형식 오류 또는 시작/종료 순서 오류");
        }

        // 시간 겹침 검증
        if (repo.hasTimeConflict(lecture)) {
            return Result.conflict("동일 강의실/요일에서 시간이 겹칩니다.");
        }

        // 저장
        String code = repo.save(lecture);
        return code.equals("200") ? Result.ok("저장 성공") : Result.serverError("저장 실패");
    }

    /** 삭제 */
    public Result delete(String lectureId) {
        if (isBlank(lectureId)) return Result.badRequest("id 누락");
        String code = repo.deleteById(lectureId);
        return code.equals("200") ? Result.ok("삭제 성공") : Result.notFound("해당 id 없음");
    }

    /** 편의: 연/학기 목록 */
    public List<String> listTerms() {
        return repo.findAll().stream()
                .filter(l -> l.getYear() != null && l.getSemester() != null)
                .map(l -> l.getYear() + "-" + l.getSemester().name())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** 편의: 건물/층/강의실 목록 (해당 연/학기 기준) */
    public Map<String, Map<String, List<String>>> listRoomsByTerm(int year, Semester semester) {
        Map<String, Map<String, List<String>>> map = new TreeMap<>();
        for (Lecture l : repo.findAllByYearAndSemester(year, semester)) {
            map.computeIfAbsent(l.getBuilding(), b -> new TreeMap<>())
               .computeIfAbsent(l.getFloor(), f -> new ArrayList<>())
               .add(l.getLectureroom());
        }
        // 중복 제거 + 정렬
        map.values().forEach(floorMap ->
                floorMap.replaceAll((f, rooms) -> rooms.stream().distinct().sorted().toList()));
        return map;
    }

    // ------------ 내부 유틸 ------------
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static boolean validTime(String t) {
        return t != null && t.length() == 5 && t.charAt(2) == ':' &&
               Character.isDigit(t.charAt(0)) && Character.isDigit(t.charAt(1)) &&
               Character.isDigit(t.charAt(3)) && Character.isDigit(t.charAt(4));
    }

    // ------------ 응답 모델 ------------
    public record Result(boolean ok, String message, String code) {
        public static Result ok(String msg) { return new Result(true, msg, "200"); }
        public static Result badRequest(String msg) { return new Result(false, msg, "400"); }
        public static Result notFound(String msg) { return new Result(false, msg, "404"); }
        public static Result conflict(String msg) { return new Result(false, msg, "409"); }
        public static Result serverError(String msg) { return new Result(false, msg, "500"); }
    }
}
