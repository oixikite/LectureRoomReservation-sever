
package deu.controller.business;

import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.service.LectureService;
import deu.model.dto.request.data.lecture.LectureFilterRequest;
import deu.model.dto.response.LectureListResponse;
import deu.repository.LectureRepository; //[신규] CUD를 위한 Repository 임포트

import java.util.Arrays;
import java.util.List;

// 강의 컨트롤러
public class LectureController {
    private static final LectureController instance = new LectureController();

    private LectureController() {}

    public static LectureController getInstance() {
        return instance;
    }

    //[수정] Service(조회용)와 Repository(CUD용)를 둘 다 가짐
    private final LectureService lectureService = LectureService.getInstance();
    private final LectureRepository lectureRepository = LectureRepository.getInstance();

    // ===============================================================
    //기존 '주간 강의 조회' 로직 (LectureService 사용)
    // ===============================================================
    public Object handleReturnLectureOfWeek(LectureRequest payload) {
        BasicResponse result = lectureService.returnLectureOfWeek(payload);
        
         // 콘솔에 결과 데이터 출력
        System.out.println("[LectureController] 반환된 데이터: " + Arrays.deepToString((Lecture[][]) result.data));
        
        return result;
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
            return LectureListResponse.ok(lectures);

        } catch (Exception e) {
            e.printStackTrace();
            return LectureListResponse.error("500", "강의 목록 조회 중 오류 발생: " + e.getMessage());
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