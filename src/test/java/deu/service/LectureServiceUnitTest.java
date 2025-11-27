/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.service;

/**
 *
 * @author scq37
 */

import deu.model.dto.request.data.lecture.LectureDateRequest;
import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.repository.LectureRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;



/**
 * LectureService 단위 테스트
 * - 파일 시스템(Repository)에 의존하지 않고 Mock을 사용하여 로직을 검증
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // ★ 순서 지정 기능 활성화
class LectureServiceUnitTest {

    private final LectureService service = LectureService.getInstance();

    @Test
    @Order(1) // ★ 첫 번째로 실행
    @DisplayName("[주간 조회] 오늘 요일의 강의가 정상적으로 시간표(0~12교시)에 배치되는지 확인")
    void testReturnLectureOfWeek_HappyPath() {
        System.out.println("\n=======================================================");
        System.out.println("[Test 1] 주간 시간표 배치 로직 검증 (Happy Path)");
        System.out.println("=======================================================");
        
        // 1. Given
        String targetBuilding = "공학관";
        String targetFloor = "3";
        String targetRoom = "305호";
        LectureRequest request = new LectureRequest(targetBuilding, targetFloor, targetRoom);

        String todayYoil = LocalDate.now().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN); 
        
        System.out.println("[Step 1] 테스트 환경 설정");
        System.out.println("  > 오늘 요일: " + todayYoil);
        System.out.println("  > 타겟 강의실: " + targetBuilding + " " + targetRoom);

        Lecture validLecture = new Lecture();
        validLecture.setId("L1");
        validLecture.setTitle("자바프로그래밍");
        validLecture.setBuilding(targetBuilding);
        validLecture.setFloor(targetFloor);
        validLecture.setLectureroom(targetRoom);
        validLecture.setDay(todayYoil); 
        validLecture.setStartTime("09:00");
        validLecture.setEndTime("10:00");

        Lecture otherRoomLecture = new Lecture();
        otherRoomLecture.setId("L2");
        otherRoomLecture.setTitle("옆방강의");
        otherRoomLecture.setBuilding(targetBuilding);
        otherRoomLecture.setFloor(targetFloor);
        otherRoomLecture.setLectureroom("306호"); 
        otherRoomLecture.setDay(todayYoil);
        otherRoomLecture.setStartTime("09:00");
        otherRoomLecture.setEndTime("10:00");
        
        System.out.println("[Step 2] Mock Repository 설정");
        System.out.println("  > Mock 데이터 1: " + validLecture.getTitle() + " (" + validLecture.getLectureroom() + ")");
        System.out.println("  > Mock 데이터 2: " + otherRoomLecture.getTitle() + " (" + otherRoomLecture.getLectureroom() + ")");

        try (MockedStatic<LectureRepository> repoMock = Mockito.mockStatic(LectureRepository.class)) {
            LectureRepository mockRepo = mock(LectureRepository.class);
            repoMock.when(LectureRepository::getInstance).thenReturn(mockRepo);
            when(mockRepo.findAll()).thenReturn(List.of(validLecture, otherRoomLecture));

            System.out.println("[Step 3] Service 메서드 실행 (returnLectureOfWeek)");
            BasicResponse response = service.returnLectureOfWeek(request);

            System.out.println("[Step 4] 결과 검증");
            System.out.println("  > 응답 코드: " + response.code);
            assertEquals("200", response.code);

            Lecture[][] schedule = (Lecture[][]) response.data;

            Lecture assigned = schedule[0][0];
            if (assigned != null) {
                System.out.println("  > [성공] 오늘 0교시에 배정된 강의: " + assigned.getTitle());
                assertEquals("자바프로그래밍", assigned.getTitle());
                assertEquals("L1", assigned.getId());
            } else {
                System.out.println("  > [실패] 오늘 0교시가 비어있습니다 (배정 실패).");
                fail("강의 배정 실패");
            }

            boolean foundOther = false;
            for (Lecture[] dayRows : schedule) {
                for (Lecture l : dayRows) {
                    if (l != null && l.getId().equals("L2")) foundOther = true;
                }
            }
            if (!foundOther) {
                System.out.println("  > [성공] 다른 강의실('옆방강의') 데이터가 완벽히 필터링 되었습니다.");
            } else {
                System.out.println("  > [실패] 다른 강의실 데이터가 시간표에 섞여 들어왔습니다.");
                fail("필터링 실패");
            }
            assertFalse(foundOther);
        }
        System.out.println("-------------------------------------------------------\n");
    }

    @Test
    @Order(2)
    @DisplayName("[주간 조회] 데이터가 없을 경우 에러 없이 빈 배열을 반환해야 한다")
    void testReturnLectureOfWeek_EmptyData() {
        System.out.println("\n=======================================================");
        System.out.println("[Test 2] 데이터 없음(Empty) 처리 로직 검증");
        System.out.println("=======================================================");

        LectureRequest request = new LectureRequest("공학관", "3", "305호");

        try (MockedStatic<LectureRepository> repoMock = Mockito.mockStatic(LectureRepository.class)) {
            LectureRepository mockRepo = mock(LectureRepository.class);
            repoMock.when(LectureRepository::getInstance).thenReturn(mockRepo);
            
            System.out.println("[Step 1] Mock Repository가 빈 리스트([])를 반환하도록 설정");
            when(mockRepo.findAll()).thenReturn(Collections.emptyList());

            System.out.println("[Step 2] Service 실행");
            BasicResponse response = service.returnLectureOfWeek(request);

            System.out.println("[Step 3] 결과 검증 (404 기대)");
            System.out.println("  > 실제 응답 코드: " + response.code);
            System.out.println("  > 실제 메시지: " + response.data);

            assertEquals("404", response.code); 
            assertEquals("파일에서 강의 정보를 불러오지 못했습니다.", response.data);
        }
        System.out.println("-------------------------------------------------------\n");
    }

    @Test
    @Order(3)
    @DisplayName("[일간 조회] 특정 날짜의 강의만 1차원 배열로 반환해야 한다")
    void testReturnLectureOfDay() {
        System.out.println("\n=======================================================");
        System.out.println("[Test 3] 일간(Daily) 조회 및 시간 파싱 검증");
        System.out.println("=======================================================");

        String todayYoil = LocalDate.now().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        
        System.out.println("[Step 1] 테스트 강의 데이터 생성");
        System.out.println("  > 강의 시간: 13:00 ~ 15:00 (2시간)");
        System.out.println("  > 기대 결과: 4교시(13~14), 5교시(14~15)에 배치되어야 함");

        LectureDateRequest request = new LectureDateRequest();
        request.setTargetDate(LocalDate.now()); 
        request.setBuilding("공학관");
        request.setFloor("3");
        request.setLectureroom("305호");

        Lecture lec = new Lecture();
        lec.setTitle("일간테스트");
        lec.setBuilding("공학관");
        lec.setFloor("3");
        lec.setLectureroom("305호");
        lec.setDay(todayYoil);
        lec.setStartTime("13:00");
        lec.setEndTime("15:00"); 

        try (MockedStatic<LectureRepository> repoMock = Mockito.mockStatic(LectureRepository.class)) {
            LectureRepository mockRepo = mock(LectureRepository.class);
            repoMock.when(LectureRepository::getInstance).thenReturn(mockRepo);
            when(mockRepo.findAll()).thenReturn(List.of(lec));

            BasicResponse response = service.returnLectureOfDay(request);
            Lecture[] dailySchedule = (Lecture[]) response.data;

            System.out.println("[Step 2] 시간표 배열 검증");
            
            if (dailySchedule[4] != null && dailySchedule[5] != null) {
                 System.out.println("  > [성공] 4교시(13시) 할당됨: " + dailySchedule[4].getTitle());
                 System.out.println("  > [성공] 5교시(14시) 할당됨: " + dailySchedule[5].getTitle());
            } else {
                 System.out.println("  > [실패] 13시~15시 구간에 강의가 없습니다.");
            }

            if (dailySchedule[0] == null) {
                System.out.println("  > [성공] 0교시(09시)는 비어있습니다 (정상).");
            } else {
                 System.out.println("  > [실패] 0교시에 엉뚱한 강의가 들어있습니다.");
            }
            
            assertNotNull(dailySchedule[4]);
            assertNotNull(dailySchedule[5]);
            assertNull(dailySchedule[0]);
            assertEquals("일간테스트", dailySchedule[4].getTitle());
        }
        System.out.println("-------------------------------------------------------\n");
    }
}