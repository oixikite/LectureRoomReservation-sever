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

        Field repoField = ReservationService.class.getDeclaredField("reservationRepository");
        repoField.setAccessible(true);
        repoField.set(service, mockRepo);
    }

    @Test
    @DisplayName("정상 예약 생성")
    void testCreateRoomReservationSuccess() {
        RoomReservationRequest req = getRequest();
        when(mockRepo.findByUser("S123")).thenReturn(new ArrayList<>());
        when(mockRepo.isDuplicate(anyString(), anyString(), anyString())).thenReturn(false);

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("200", response.code);
        assertEquals("예약이 완료되었습니다.", response.data);
    }

    @Test
    @DisplayName("7일 내 예약 5건 초과 시 거절")
    void testCreateRoomReservationOverLimit() {
        RoomReservationRequest req = getRequest();
        List<RoomReservation> existing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            RoomReservation r = RoomReservation.builder()
                    .date(LocalDate.now().plusDays(i).toString())
                    .build();
            existing.add(r);
        }

        when(mockRepo.findByUser("S123")).thenReturn(existing);

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("403", response.code);
        assertTrue(response.data.toString().contains("최대 5개의 예약"));
    }

    @Test
    @DisplayName("사용자 동일 시간대 중복 예약 시 거절")
    void testCreateRoomReservationDuplicateTime() {
        RoomReservationRequest req = getRequest();
        RoomReservation r = RoomReservation.builder()
                .date(req.getDate())
                .startTime(req.getStartTime())
                .build();

        when(mockRepo.findByUser("S123")).thenReturn(List.of(r));

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("409", response.code);
        assertTrue(response.data.toString().contains("이미 예약"));
    }

    @Test
    @DisplayName("강의실 동일 시간대 중복 예약 시 거절")
    void testCreateRoomReservationRoomConflict() {
        RoomReservationRequest req = getRequest();
        when(mockRepo.findByUser("S123")).thenReturn(new ArrayList<>());
        when(mockRepo.isDuplicate(anyString(), anyString(), anyString())).thenReturn(true);

        BasicResponse response = service.createRoomReservation(req);

        assertEquals("409", response.code);
        assertTrue(response.data.toString().contains("다른 예약"));
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

        // (String number, String roomReservationId)
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

        // [수정] 인자 순서 변경: (사용자ID, 예약ID) -> ("S123", "resv123")
        DeleteRoomReservationRequest req = new DeleteRoomReservationRequest("S123", "resv123");
        BasicResponse response = service.deleteRoomReservationFromUser(req);

        assertEquals("403", response.code);
    }

    @Test
    @DisplayName("존재하지 않는 예약 삭제 시 실패")
    void testDeleteRoomReservationNotFound() {
        when(mockRepo.findById("resv123")).thenReturn(null);

        // [수정] 인자 순서 변경: (사용자ID, 예약ID) -> ("S123", "resv123")
        DeleteRoomReservationRequest req = new DeleteRoomReservationRequest("S123", "resv123");
        BasicResponse response = service.deleteRoomReservationFromUser(req);

        assertEquals("404", response.code);
    }

    @Test
    @DisplayName("관리자 예약 삭제 성공")
    void testDeleteRoomReservationFromManagement() {
        when(mockRepo.deleteById("resv123")).thenReturn(true);

        BasicResponse response = service.deleteRoomReservationFromManagement("resv123");

        assertEquals("200", response.code);
        verify(mockRepo).saveToFile();
    }

    @Test
    @DisplayName("예약 상태 변경 성공")
    void testChangeRoomReservationStatus() {
        RoomReservation r = RoomReservation.builder()
                .id("resv123")
                .status("대기")
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
                .build();

        when(mockRepo.findById("resv123")).thenReturn(existing);

        RoomReservationRequest req = new RoomReservationRequest();
        req.setId("resv123");
        req.setTitle("변경된 제목");
        req.setBuildingName("신관");
        req.setLectureRoom("202");
        req.setFloor("2층");
        req.setDate(LocalDate.now().toString());
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
        req.setDate(LocalDate.now().toString());
        req.setDayOfTheWeek("월");
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setPurpose("학습");
        req.setAccompanyingStudentCount(0);
        return req;
    }
}
