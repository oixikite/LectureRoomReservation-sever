/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.service;

import deu.model.dto.request.data.reservation.AccompanyingStudent;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.entity.RoomReservation;
import deu.model.dto.response.BasicResponse; 
import deu.repository.ReservationRepository;
import deu.repository.RoomCapacityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author oixikite
 */

@DisplayName("예약 생성 시 빌더 패턴 적용 데이터 검증 테스트")
public class ReservationServiceBuilderPatternTest {
    @InjectMocks
    private ReservationService reservationService; // 테스트 대상 (실제 객체)

    @Mock
    private ReservationRepository reservationRepository; // 가짜 저장소

    @Mock
    private RoomCapacityRepository roomCapacityRepository; // 가짜 정원 저장소

    // [중요] 정적(Static) 메소드 모킹을 위한 변수
    private MockedStatic<RoomCapacityRepository> mockedRoomCapacityRepo;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // 1. ReservationService의 reservationRepository 필드에 Mock 주입 (Reflection)
        Field repoField = ReservationService.class.getDeclaredField("reservationRepository");
        repoField.setAccessible(true);
        repoField.set(ReservationService.getInstance(), reservationRepository);
        
        reservationService = ReservationService.getInstance();

        // 2. [핵심] RoomCapacityRepository.getInstance() 호출 시 Mock을 반환하도록 설정
        // 이렇게 하면 실제 파일(room-capacity.yaml 등)을 건드리지 않습니다.
        mockedRoomCapacityRepo = mockStatic(RoomCapacityRepository.class);
        mockedRoomCapacityRepo.when(RoomCapacityRepository::getInstance).thenReturn(roomCapacityRepository);
    }

    @AfterEach
    void tearDown() {
        // [필수] 테스트가 끝나면 Static Mock을 반드시 해제해야 다른 테스트에 영향을 주지 않음
        if (mockedRoomCapacityRepo != null) {
            mockedRoomCapacityRepo.close();
        }
    }
    
    @Test
    @DisplayName("빌더 패턴을 통해 예약 목적, 인원, 동반자 정보가 엔티티에 정확히 매핑되는지 검증")    
    void testBuilderPatternDataMapping() {        
        // Given (상황 설정: 사용자가 입력한 예약 요청 데이터)
        String userId = "s20223046";
        List<AccompanyingStudent> friends = new ArrayList<>();
        friends.add(new AccompanyingStudent("s20223046", "이시연"));
        friends.add(new AccompanyingStudent("s20020425", "시연이"));

        RoomReservationRequest request = new RoomReservationRequest();
        request.setBuildingName("공학관");
        request.setFloor("3층");
        request.setLectureRoom("305호");
        request.setDate(LocalDate.now().plusDays(1).toString()); // 내일 예약
        request.setStartTime("10:00");
        request.setEndTime("12:00");
        request.setNumber(userId); // 예약자 학번
        
        // [검증 대상 필드 설정]
        request.setPurpose("소프트웨어공학 팀 프로젝트 회의"); // 사용 목적
        request.setAccompanyingStudentCount(2); // 동반 사용자 수
        request.setAccompanyingStudents(friends); // 동반 사용자 목록 (이름, 학번 포함)

        // Mock 설정: 사용자 예약 조회 시 빈 리스트 반환 (중복/제한 통과)
        when(reservationRepository.findByUser(anyString())).thenReturn(new ArrayList<>());
        // Mock 설정: 모든 예약 조회 시 빈 리스트 반환 (정원 체크 통과)
        when(reservationRepository.findAll()).thenReturn(new ArrayList<>());

        // When (동작 수행: 예약 생성 요청)
        BasicResponse response = reservationService.createRoomReservation(request);

        // Then (결과 검증)
        // 1. 응답 코드가 200(성공)인지 확인
        assertEquals("200", response.getCode(), "예약 생성 요청은 성공해야 합니다.");

        // 2. Repository.save()가 호출될 때 전달된 RoomReservation 객체 가로채기 (Capture)
        ArgumentCaptor<RoomReservation> captor = ArgumentCaptor.forClass(RoomReservation.class);
        verify(reservationRepository).save(captor.capture()); // save 메서드 호출 검증 및 인자 포획

        RoomReservation savedReservation = captor.getValue(); // 가로챈 객체 가져오기
        
        // 3. [핵심 검증] 빌더 패턴을 통해 DTO의 값이 Entity로 올바르게 매핑되었는지 확인
        assertAll("Builder Pattern Mapping Check",
            () -> assertEquals(userId, savedReservation.getNumber(), "예약자 학번이 일치해야 합니다."),
            () -> assertEquals("소프트웨어공학 팀 프로젝트 회의", savedReservation.getPurpose(), "사용 목적이 일치해야 합니다."),
            () -> assertEquals(2, savedReservation.getAccompanyingStudentCount(), "동반 사용자 수가 일치해야 합니다."),
            () -> assertNotNull(savedReservation.getAccompanyingStudents(), "동반 사용자 목록이 null이 아니어야 합니다."),
            () -> assertEquals(2, savedReservation.getAccompanyingStudents().size(), "동반 사용자 목록 크기가 일치해야 합니다."),
            () -> assertEquals("이시연", savedReservation.getAccompanyingStudents().get(0).getName(), "첫 번째 동반자 이름이 일치해야 합니다."), // 이름 확인
            () -> assertEquals("s20223046", savedReservation.getAccompanyingStudents().get(0).getStudentId(), "첫 번째 동반자 학번이 일치해야 합니다.")
        );
        
        System.out.println(">> [Builder Pattern Verified] 데이터 매핑 성공: " + savedReservation);
    }
}
