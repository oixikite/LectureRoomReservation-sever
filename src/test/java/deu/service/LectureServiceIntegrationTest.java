package deu.service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author scq37
 */

import deu.model.dto.request.data.lecture.LectureRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.Lecture;
import deu.repository.LectureRepository;
import deu.service.LectureService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service와 Repository를 결합한 통합 테스트 (Integration Test)
 * - Mock을 사용하지 않음
 * - 실제로 YAML 파일에 쓰고 읽는 과정을 검증함
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class LectureServiceIntegrationTest {
    private final LectureService service = LectureService.getInstance();
    private final LectureRepository repo = LectureRepository.getInstance();
    
    // 테스트용 ID 상수
    private static final String INTEGRATION_TEST_ID = "INT_TEST_001";

    @BeforeEach
    void setup() {
        // 각 테스트 실행 전, 혹시 남아있을지 모를 테스트 데이터를 정리
        repo.deleteById(INTEGRATION_TEST_ID);
    }

    @Test
    @DisplayName("통합: 실제 파일에 저장된 강의를 Service가 정상적으로 불러와 시간표를 구성하는지 확인")
    @Order(1)
    void testIntegration_SaveAndRetrieve() {
        System.out.println("\n=== [Integration Test] 실제 파일 연동 테스트 시작 ===");

        // 1. Given: 실제 Repository를 통해 파일에 데이터 저장
        String todayYoil = LocalDate.now().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN); // 오늘 요일 (예: "목")
        
        System.out.println("[Step 1] 테스트용 데이터 실제 저장 (요일: " + todayYoil + ")");
        
        Lecture realLecture = new Lecture();
        realLecture.setId(INTEGRATION_TEST_ID);
        realLecture.setTitle("통합테스트강의");
        realLecture.setProfessor("통합교수");
        realLecture.setBuilding("통합관");
        realLecture.setFloor("5");
        realLecture.setLectureroom("505호");
        realLecture.setDay(todayYoil);
        realLecture.setStartTime("10:00"); // 10시 (1교시)
        realLecture.setEndTime("11:00");

        repo.save(realLecture); // -> 실제로 lectures.yaml 파일에 쓰임

        // 2. When: Service를 호출하여 데이터 요청
        System.out.println("[Step 2] Service를 통해 데이터 조회 요청");
        LectureRequest request = new LectureRequest("통합관", "5", "505호");
        BasicResponse response = service.returnLectureOfWeek(request);

        // 3. Then: Service 결과 검증
        assertEquals("200", response.code);
        
        Lecture[][] schedule = (Lecture[][]) response.data;
        
        // 오늘(schedule[0])의 1교시(10시 -> 인덱스 1)에 데이터가 있어야 함
        Lecture retrieved = schedule[0][1]; 
        
        assertNotNull(retrieved, "실제 파일에서 읽어온 데이터가 시간표에 배치되어야 합니다.");
        assertEquals("통합테스트강의", retrieved.getTitle());
        assertEquals(INTEGRATION_TEST_ID, retrieved.getId());
        
        System.out.println("  > [성공] 저장된 데이터: " + retrieved.getTitle());
        System.out.println("=== [Integration Test] 종료 ===\n");
    }

    @Test
    @DisplayName("통합: DB(파일)에 없는 강의실을 요청하면 빈 시간표가 와야 함")
    @Order(2)
    void testIntegration_NotFound() {
        System.out.println("\n=== [Integration Test] 없는 데이터 조회 테스트 시작 ===");
        
        // 1. Given: 없는 강의실 요청
        LectureRequest request = new LectureRequest("존재하지않는관", "99", "999호");

        // 2. When
        BasicResponse response = service.returnLectureOfWeek(request);

        // 3. Then
        assertEquals("200", response.code); // 코드는 200이지만 데이터는 비어있어야 함
        Lecture[][] schedule = (Lecture[][]) response.data;
        
        assertNull(schedule[0][0], "데이터가 없으므로 비어있어야 합니다.");
        System.out.println("  > [성공] 빈 시간표 반환 확인");
        System.out.println("=== [Integration Test] 종료 ===\n");
    }

    @AfterAll
    void cleanup() {
        // 모든 테스트가 끝나면 테스트 데이터 삭제 (파일 청소)
        repo.deleteById(INTEGRATION_TEST_ID);
        System.out.println("[Cleanup] 통합 테스트용 데이터 삭제 완료");
    }
}
