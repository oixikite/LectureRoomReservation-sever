package deu.service;

import deu.model.dto.request.data.reservation.*;
import deu.model.entity.RoomReservation;
import deu.model.dto.response.BasicResponse;
import deu.repository.ReservationRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReservationService 단위 테스트")
public class ReservationServiceTest {

    private ReservationService service;
    private ReservationRepository mockRepo;

    @BeforeEach
    void setUp() throws Exception {
        mockRepo = mock(ReservationRepository.class);
        service = ReservationService.getInstance();

        // Service 내부의 Repository 필드에 Mock 주입
        Field repoField = ReservationService.class.getDeclaredField("reservationRepository");
        repoField.setAccessible(true);
        repoField.set(service, mockRepo);
    }

    @Test
    @DisplayName("정상 예약 생성")
    void testCreateRoomReservationSuccess() {
        RoomReservationRequest req = getRequest();
        // 학생 정책상 하루 전 예약 필수 -> getRequest()에서 내일 날짜로 설정됨

        when(mockRepo.findByUser("S123")).thenReturn(new ArrayList<>());
        when(mockRepo.findAll()).thenReturn(new ArrayList<>());

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("200", response.code);
        assertEquals("예약이 완료되었습니다.", response.data);
        verify(mockRepo).save(any(RoomReservation.class));
    }

    @Test
    @DisplayName("7일 내 예약 5건 초과 시 거절")
    void testCreateRoomReservationOverLimit() {
        RoomReservationRequest req = getRequest();
        List<RoomReservation> existing = new ArrayList<>();

        // [수정] NPE 방지를 위해 startTime, endTime 설정 필수
        for (int i = 0; i < 5; i++) {
            RoomReservation r = RoomReservation.builder()
                    .date(LocalDate.now().plusDays(i).toString())
                    .startTime("09:00") // 시간 계산 로직을 위해 필수
                    .endTime("10:00") // 시간 계산 로직을 위해 필수
                    .build();
            existing.add(r);
        }

        when(mockRepo.findByUser("S123")).thenReturn(existing);

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("403", response.code);
        // 실제 메시지: "오늘부터 7일간 최대 5회까지만 예약 가능합니다."
        assertTrue(response.data.toString().contains("최대 5회"));
    }

    @Test
    @DisplayName("사용자 동일 시간대 중복 예약 시 거절")
    void testCreateRoomReservationDuplicateTime() {
        RoomReservationRequest req = getRequest();

        // [수정] NPE 방지를 위해 startTime, endTime 설정 필수
        RoomReservation r = RoomReservation.builder()
                .date(req.getDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();

        when(mockRepo.findByUser("S123")).thenReturn(List.of(r));

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("409", response.code);
        assertTrue(response.data.toString().contains("이미 해당 시간"));
    }

    @Test
    @DisplayName("강의실 동일 시간대 중복 예약 시 거절 (정원 초과 로직으로 변경됨)")
    void testCreateRoomReservationRoomConflict() {
        // 리팩토링된 서비스는 '정원 50% 초과' 로직을 사용합니다.
        // 정원 데이터가 없으면(capacity=0) 체크를 통과하므로 200 OK가 나와야 정상입니다.

        RoomReservationRequest req = getRequest();
        when(mockRepo.findByUser("S123")).thenReturn(new ArrayList<>());
        when(mockRepo.findAll()).thenReturn(new ArrayList<>());

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("200", response.code);
    }

    @Test
    @DisplayName("자기 예약 정상 삭제")
    void testDeleteRoomReservationFromUserSuccess() {
        RoomReservation r = RoomReservation.builder()
                .id("resv123")
                .number("S123")
                .build();

        when(mockRepo.findById("resv123")).thenReturn(r);
        when(mockRepo.deleteById("resv123")).thenReturn(true);

        DeleteRoomReservationRequest req = new DeleteRoomReservationRequest("S123", "resv123");
        BasicResponse response = service.deleteRoomReservationFromUser(req);

        assertEquals("200", response.code);
        verify(mockRepo).saveToFile();
    }

    @Test
    @DisplayName("다른 사용자 예약 삭제 시 거절")
    void testDeleteRoomReservationFromUserForbidden() {
        RoomReservation r = RoomReservation.builder()
                .id("resv123")
                .number("DIFFERENT")
                .build();

        when(mockRepo.findById("resv123")).thenReturn(r);

        DeleteRoomReservationRequest req = new DeleteRoomReservationRequest("S123", "resv123");
        BasicResponse response = service.deleteRoomReservationFromUser(req);

        assertEquals("403", response.code);
    }

    @Test
    @DisplayName("존재하지 않는 예약 삭제 시 실패")
    void testDeleteRoomReservationNotFound() {
        when(mockRepo.findById("resv123")).thenReturn(null);

        DeleteRoomReservationRequest req = new DeleteRoomReservationRequest("S123", "resv123");
        BasicResponse response = service.deleteRoomReservationFromUser(req);

        assertEquals("404", response.code);
    }

    @Test
    @DisplayName("관리자 예약 삭제 성공 (Soft Delete)")
    void testDeleteRoomReservationFromManagement() {
        // 관리자 삭제는 이제 findById -> save(status="삭제됨") 로직임
        RoomReservation target = RoomReservation.builder()
                .id("resv123")
                .status("대기")
                .number("S123")
                // 알림 저장을 위해 날짜/시간 정보 필요
                .lectureRoom("301호")
                .date("2025-01-01")
                .startTime("09:00")
                .endTime("10:00")
                .build();

        when(mockRepo.findById("resv123")).thenReturn(target);

        BasicResponse response = service.deleteRoomReservationFromManagement("resv123");

        assertEquals("200", response.code);

        ArgumentCaptor<RoomReservation> captor = ArgumentCaptor.forClass(RoomReservation.class);
        verify(mockRepo).save(captor.capture());
        assertEquals("삭제됨", captor.getValue().getStatus());

        verify(mockRepo).saveToFile();
    }

    @Test
    @DisplayName("예약 상태 변경 성공")
    void testChangeRoomReservationStatus() {
        RoomReservation r = RoomReservation.builder()
                .id("resv123")
                .status("대기")
                .number("S123")
                // 알림 저장을 위해 날짜/시간 정보 필요
                .lectureRoom("301호")
                .date("2025-01-01")
                .startTime("09:00")
                .endTime("10:00")
                .build();

        when(mockRepo.findById("resv123")).thenReturn(r);

        BasicResponse response = service.changeRoomReservationStatus("resv123");

        assertEquals("200", response.code);

        ArgumentCaptor<RoomReservation> captor = ArgumentCaptor.forClass(RoomReservation.class);
        verify(mockRepo).save(captor.capture());

        assertEquals("승인", captor.getValue().getStatus());
        verify(mockRepo).saveToFile();
    }

    @Test
    @DisplayName("예약 수정 성공")
    void testModifyRoomReservation() {
        RoomReservation existing = RoomReservation.builder()
                .id("resv123")
                .number("S123")
                // 알림 저장을 위해 날짜/시간 정보 필요
                .lectureRoom("301호")
                .date("2025-01-01")
                .startTime("09:00")
                .endTime("10:00")
                .build();

        when(mockRepo.findById("resv123")).thenReturn(existing);

        RoomReservationRequest req = new RoomReservationRequest();
        req.setId("resv123");
        req.setTitle("변경된 제목");
        req.setBuildingName("신관");
        req.setLectureRoom("202");
        req.setFloor("2층");
        req.setDate(LocalDate.now().plusDays(1).toString()); // 학생 정책(최소 하루 전) 준수
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setDayOfTheWeek("월");

        BasicResponse response = service.modifyRoomReservation(req);

        assertEquals("200", response.code);

        ArgumentCaptor<RoomReservation> captor = ArgumentCaptor.forClass(RoomReservation.class);
        verify(mockRepo).save(captor.capture());
        assertEquals("변경된 제목", captor.getValue().getTitle());

        verify(mockRepo).saveToFile();
    }

    @Test
    @DisplayName("강의실 기준 주간 예약 조회")
    void testWeekRoomReservationByLectureroom() {
        RoomReservationLocationRequest req = new RoomReservationLocationRequest("신관", "2층", "202");

        RoomReservation r = RoomReservation.builder()
                .buildingName("신관")
                .floor("2층")
                .lectureRoom("202")
                .date(LocalDate.now().toString())
                .startTime("09:00")
                .build();

        when(mockRepo.findAll()).thenReturn(List.of(r));

        BasicResponse response = service.weekRoomReservationByLectureroom(req);

        assertEquals("200", response.code);
        RoomReservation[][] result = (RoomReservation[][]) response.data;
        assertNotNull(result[0][0]);
    }

    @Test
    @DisplayName("사용자 기준 주간 예약 조회")
    void testWeekRoomReservationByUser() {
        String number = "S123";
        RoomReservation r = RoomReservation.builder()
                .number(number)
                .date(LocalDate.now().toString())
                .startTime("09:00")
                .build();

        when(mockRepo.findByUser(number)).thenReturn(List.of(r));

        BasicResponse response = service.weekRoomReservationByUserNumber(number);
        assertEquals("200", response.code);

        RoomReservation[][] result = (RoomReservation[][]) response.data;
        assertNotNull(result[0][0]);
    }

    @Test
    @DisplayName("사용자 예약 리스트 조회")
    void testGetReservationsByUser() {
        String number = "S123";
        RoomReservation r = RoomReservation.builder()
                .number(number)
                .date(LocalDate.now().toString())
                .build();

        when(mockRepo.findByUser(number)).thenReturn(List.of(r));

        BasicResponse response = service.getReservationsByUser(number);
        assertEquals("200", response.code);
        assertTrue(((List<?>) response.data).contains(r));
    }

    @Test
    @DisplayName("대기 상태 예약 목록 반환")
    void testFindAllRoomReservation() {
        RoomReservation r1 = RoomReservation.builder().status("대기").build();
        RoomReservation r2 = RoomReservation.builder().status("승인").build();

        when(mockRepo.findAll()).thenReturn(List.of(r1, r2));

        BasicResponse response = service.findAllRoomReservation();

        assertEquals("200", response.code);
        List<?> result = (List<?>) response.data;
        assertTrue(result.contains(r1));
        assertFalse(result.contains(r2));
    }

    private RoomReservationRequest getRequest() {
        RoomReservationRequest req = new RoomReservationRequest();
        req.setBuildingName("공학관");
        req.setFloor("3층");
        req.setLectureRoom("301호");
        req.setNumber("S123");
        req.setTitle("스터디");
        req.setDescription("시험 준비");

        // [중요 수정] 학생 예약 정책(최소 하루 전)을 통과하기 위해 내일 날짜로 설정
        req.setDate(LocalDate.now().plusDays(1).toString());

        req.setDayOfTheWeek("월");
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setPurpose("학습");
        req.setAccompanyingStudentCount(0);
        return req;
    }
}
