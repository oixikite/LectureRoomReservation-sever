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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // 싱글톤 Service 내부에 Mock Repository 강제 주입 (Reflection 사용)
        // 주의: 실제 코드 구조에 따라 생성자 주입이나 Setter 주입이 가능하면 그 방식을 사용하세요.
        // 여기서는 private 필드에 Mock을 주입하는 방식을 가정합니다.
        Field repoField = ReservationService.class.getDeclaredField("reservationRepository");
        repoField.setAccessible(true);
        repoField.set(ReservationService.getInstance(), reservationRepository);
        
        // 테스트 대상 인스턴스 재설정 (싱글톤 패턴 고려)
        reservationService = ReservationService.getInstance();
    }

    @Test
    @DisplayName("[Builder Pattern] 예약 시 목적, 인원, 학번, 이름(동반자) 정보가 정확히 등록되어야 한다")
    void testBuilderPatternMapping() {
        // ... (기존 테스트 로직 동일) ...
        // Given (상황 설정: 사용자가 입력한 예약 요청 데이터)
        String userId = "S20230001";
        List<AccompanyingStudent> friends = new ArrayList<>();
        friends.add(new AccompanyingStudent("S20230002", "김철수")); // 동반자 이름, 학번
        friends.add(new AccompanyingStudent("S20230003", "이영희"));

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
            () -> assertEquals("김철수", savedReservation.getAccompanyingStudents().get(0).getName(), "첫 번째 동반자 이름이 일치해야 합니다."), // 이름 확인
            () -> assertEquals("S20230002", savedReservation.getAccompanyingStudents().get(0).getStudentId(), "첫 번째 동반자 학번이 일치해야 합니다.")
        );
        
        System.out.println(">> 빌더 패턴 데이터 매핑 검증 완료: " + savedReservation);
    }
}
