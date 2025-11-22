package deu.controller.business;

import deu.model.dto.request.command.LectureCommandRequest;
import deu.model.dto.request.data.lecture.LectureDateRequest;
import deu.model.dto.request.data.lecture.LectureFilterRequest;
import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.repository.LectureRepository;
import deu.service.LectureService;

import java.util.List;

/**
 * 강의 관련 요청 처리 컨트롤러
 * - 조회(Read): Service 계층 이용
 * - 관리(CUD): Repository 계층 직접 이용 (요청 사항 반영)
 */
public class LectureController {
    private static final LectureController instance = new LectureController();

    //[수정] Service(조회용)와 Repository(CUD용)를 둘 다 가짐
    private final LectureService lectureService;
    private final LectureRepository lectureRepository;
    
    private LectureController() {
        // [수정] 생성자에서 인스턴스 할당
        this.lectureService = LectureService.getInstance();
        this.lectureRepository = LectureRepository.getInstance();
    }

    public static LectureController getInstance() {
        return instance;
    }
    
    /**
     * 클라이언트 요청 분기 처리 (Dispatcher)
     */
    public BasicResponse handle(LectureCommandRequest request) {
        // DTO Getter 사용
        String command = request.getCommand(); 
        Object data = request.getPayload(); // [수정] getPayload() 사용
 
        try {
            if (command == null) {
                return new BasicResponse("400", "명령어가 없습니다.");
            }

            switch (command) {
                // --- [R] 조회 기능 (Service) ---
                case "주간 강의 조회":
                    if (data instanceof LectureRequest req) {
                        return (BasicResponse) handleReturnLectureOfWeek(req);
                    }
                    break;
                case "월간 강의 조회":
                    if (data instanceof LectureDateRequest req) {
                        return lectureService.returnLectureOfMonth(req);
                    }
                    break;
                case "일간 강의 조회":
                    if (data instanceof LectureDateRequest req) {
                        return lectureService.returnLectureOfDay(req);
                    }
                    break;
                case "강의실 강의 조회": 
                    if (data instanceof LectureFilterRequest req) {
                        return (BasicResponse) handleFindLecturesByFilter(req);
                    }
                    break;

                // --- [CUD] 관리 기능 (Repository) ---
                case "강의 추가":
                    if (data instanceof Lecture req) {
                        return handleAddLecture(req);
                    }
                    break;
                case "강의 수정":
                    if (data instanceof Lecture req) {
                        return handleUpdateLecture(req);
                    }
                    break;
                case "강의 삭제":
                    if (data instanceof String req) { 
                        return handleDeleteLecture(req);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 내부 오류: " + e.getMessage());
        }

        return new BasicResponse("400", "잘못된 요청이거나 데이터 형식이 일치하지 않습니다. (" + command + ")");
    }

    // ===============================================================
    //기존 '주간 강의 조회' 로직 (LectureService 사용)
    // ===============================================================
    public Object handleReturnLectureOfWeek(LectureRequest payload) {
        BasicResponse result = lectureService.returnLectureOfWeek(payload);
        
         // 콘솔에 결과 데이터 출력
        //System.out.println("[LectureController] 반환된 데이터: " + Arrays.deepToString((Lecture[][]) result.data));
        
        return result;
    }
    
    // [신규] 월간 핸들러 추가
    public BasicResponse handleReturnLectureOfMonth(LectureDateRequest payload) {
        return lectureService.returnLectureOfMonth(payload);
    }

    // [신규] 일간 핸들러 추가
    public BasicResponse handleReturnLectureOfDay(LectureDateRequest payload) {
        return lectureService.returnLectureOfDay(payload);
    }

    // ===============================================================
    //'강의실 강의 조회' 로직 (LectureService 사용)
    // ===============================================================
    public Object handleFindLecturesByFilter(LectureFilterRequest payload) {
        try {
            List<Lecture> lectures = lectureService.findLectures(
                    payload.getYear(),
                    payload.getSemester(),
                    payload.getBuilding(),
                    payload.getFloor(),
                    payload.getLectureroom()
            );

            // 성공 응답 생성
            return new BasicResponse("200", lectures); 

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "강의 목록 조회 실패");
        }
    }

    // ===============================================================
    //[신규] CUD 기능 (LectureRepository 사용)
    // ===============================================================

    /**
     *[신규] 강의 추가
     */
    public BasicResponse handleAddLecture(Lecture lecture) {
        try {
            if ("200".equals(lectureRepository.existsById(lecture.getId()))) {
                return new BasicResponse("409", "이미 존재하는 강의 코드입니다.");
            }
            if (lectureRepository.hasTimeConflict(lecture)) {
                return new BasicResponse("409", "해당 강의실에 이미 시간이 겹치는 강의가 있습니다.");
            }
            String status = lectureRepository.save(lecture);
            return "200".equals(status)
                    ? new BasicResponse("200", "강의 추가 성공")
                    : new BasicResponse(status, "강의 추가 실패 (save 메서드 오류)");
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }

    /**
     *[신규] 강의 수정
     */
    public BasicResponse handleUpdateLecture(Lecture lecture) {
        try {
            if ("404".equals(lectureRepository.existsById(lecture.getId()))) {
                return new BasicResponse("404", "수정할 강의를 찾을 수 없습니다.");
            }
            if (lectureRepository.hasTimeConflict(lecture)) {
                return new BasicResponse("409", "해당 강의실에 이미 시간이 겹치는 강의가 있습니다.");
            }
            String status = lectureRepository.save(lecture);
            return "200".equals(status)
                    ? new BasicResponse("200", "강의 수정 성공")
                    : new BasicResponse(status, "강의 수정 실패 (save 메서드 오류)");
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }

    /**
     *[신규] 강의 삭제
     */
    public BasicResponse handleDeleteLecture(String lectureId) {
        try {
            String status = lectureRepository.deleteById(lectureId);
            return "200".equals(status)
                    ? new BasicResponse("200", "강의 삭제 성공")
                    : new BasicResponse("404", "삭제할 강의를 찾을 수 없습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 오류: " + e.getMessage());
        }
    }
}