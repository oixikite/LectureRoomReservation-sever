/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.service;

/**
 *
 * @author scq37
 */

import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.repository.LectureRepository;
import java.time.LocalDate;
import java.time.format.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *  순수 단위 테스트 (Unit Test)
 * 파일 시스템(Yaml)에 의존하지 않고 Mock 객체를 사용하여 로직만 검증함
 */

public class LectureServiceUnitTest {
    
  LectureService service = LectureService.getInstance();

    @Test
    @DisplayName("Mock 데이터를 활용한 주간 시간표 배치 로직 검증")
    void testLogicWithMock() {
        // 1. Given (상황 설정)
        LectureRequest request = new LectureRequest("공학관", "3", "305호");

        // 현재 요일을 구해서 그 요일로 강의를 만듬.
        // 예: 오늘이 토요일이면 "토", 월요일이면 "월"이 들어감
        String todayStr = LocalDate.now().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN); // "월", "화", ... "토"

        // 가짜 강의 데이터
        Lecture mockLecture = new Lecture();
        mockLecture.setTitle("유닛테스트용강의");
        mockLecture.setBuilding("공학관");
        mockLecture.setFloor("3");
        mockLecture.setLectureroom("305호");
        mockLecture.setDay(todayStr); // ★ 요일을 "오늘"로 설정!
        mockLecture.setStartTime("09:00");
        mockLecture.setEndTime("10:00");

        // 2. Mocking (Repository 가로채기)
        try (MockedStatic<LectureRepository> repoMock = Mockito.mockStatic(LectureRepository.class)) {
            LectureRepository mockRepo = mock(LectureRepository.class);
            
            // getInstance() 호출 시 가짜 Repo 반환
            repoMock.when(LectureRepository::getInstance).thenReturn(mockRepo);
            // findAll() 호출 시 가짜 데이터 리스트 반환
            when(mockRepo.findAll()).thenReturn(List.of(mockLecture));

            // 3. When (실행)
            BasicResponse res = service.returnLectureOfWeek(request);

            // 4. Then (검증)
            Lecture[][] schedule = (Lecture[][]) res.data;
                       
            assertNotNull(schedule[0][0], "오늘 0교시에 강의가 배치되어야 합니다.");
            assertEquals("유닛테스트용강의", schedule[0][0].getTitle());
        }
    }
    
}
