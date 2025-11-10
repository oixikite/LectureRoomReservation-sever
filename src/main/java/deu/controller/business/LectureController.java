package deu.controller.business;

import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.service.LectureService;
import deu.model.dto.request.data.lecture.LectureFilterRequest;
import deu.model.dto.response.LectureListResponse; 

import java.util.Arrays;
import java.util.List;

// 강의 컨트롤러
public class LectureController {
    private static final LectureController instance = new LectureController();

    private LectureController() {}

    public static LectureController getInstance() {
        return instance;
    }

    private final LectureService lectureService = LectureService.getInstance();

    public Object handleReturnLectureOfWeek(LectureRequest payload) {
        BasicResponse result = lectureService.returnLectureOfWeek(payload);

        // 콘솔에 결과 데이터 출력
        // System.out.println("[LectureController] 반환된 데이터: " + Arrays.deepToString((Lecture[][]) result.data));

        return result;
    }
    
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
}
