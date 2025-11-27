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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author oixikite
 */

@DisplayName("예약 시스템 통합 테스트 (Service + Repository + Builder)")
public class ReservationServiceIntegrationTest {
    private ReservationService reservationService;
    private ReservationRepository reservationRepository;
    
    // 테스트 후 데이터 삭제를 위해 예약 정보를 저장할 변수
    private String createdReservationId; 
    private String testUserId = "S20223046"; // 테스트용 학번

    @BeforeEach
    void setUp() {
        // [핵심] Mock이 아닌 실제 싱글톤 인스턴스를 가져옵니다.
        reservationService = ReservationService.getInstance();
        reservationRepository = ReservationRepository.getInstance();
    }

    @AfterEach
    void tearDown() {
        // [중요] 테스트가 끝날 때마다 생성된 데이터를 실제로 삭제하여 파일을 깨끗하게 유지합니다.
        if (createdReservationId != null) {
            reservationRepository.deleteById(createdReservationId);
            reservationRepository.saveToFile(); // 변경사항(삭제)을 파일에 반영
            System.out.println(">> [Clean-up] 테스트 데이터 삭제 완료: " + createdReservationId);
        }
    }

    @Test
    @DisplayName("[통합] 예약 신청 시 빌더 패턴을 통해 데이터가 실제 저장소에 올바르게 저장되는지 검증")
    void testCreateReservationIntegration() {
        // 1. Given (테스트 데이터 준비)
        List<AccompanyingStudent> friends = new ArrayList<>();
        friends.add(new AccompanyingStudent("S20239999", "테스트친구"));

        RoomReservationRequest request = new RoomReservationRequest();
        request.setBuildingName("공학관");
        request.setFloor("1층");
        request.setLectureRoom("101호"); // 실제 데이터 파일에 영향이 적은 강의실 선택
        request.setDate(LocalDate.now().plusDays(2).toString()); // 모레 날짜
        request.setStartTime("18:00");
        request.setEndTime("19:00");
        request.setNumber(testUserId);
        
        // 검증할 핵심 필드 (빌더 패턴 적용 부분)
        String expectedPurpose = "통합 테스트 목적";
        int expectedCount = 1;
        
        request.setPurpose(expectedPurpose);
        request.setAccompanyingStudentCount(expectedCount);
        request.setAccompanyingStudents(friends);

        // 2. When (실제 서비스 호출)
        BasicResponse response = reservationService.createRoomReservation(request);

        // 3. Then (결과 검증)
        // 3-1. 응답 코드 확인
        assertEquals("200", response.getCode(), "예약이 정상적으로 승인되어야 합니다.");

        // 3-2. 실제 저장소(Repository)에서 데이터 조회하여 검증
        // (주의: createRoomReservation은 ID를 반환하지 않을 수 있으므로, 학번으로 조회)
        List<RoomReservation> userReservations = reservationRepository.findByUser(testUserId);
        assertFalse(userReservations.isEmpty(), "저장소에 예약 데이터가 존재해야 합니다.");

        // 방금 만든 예약 찾기 (가장 최근 것)
        RoomReservation savedReservation = userReservations.get(userReservations.size() - 1);
        this.createdReservationId = savedReservation.getId(); // ID 백업 (삭제용)

        // 3-3. 데이터 정합성 확인 (빌더 패턴이 실제 객체 생성까지 잘 이어졌는지)
        assertAll("Integration Data Check",
            () -> assertEquals(testUserId, savedReservation.getNumber()),
            () -> assertEquals(expectedPurpose, savedReservation.getPurpose()),
            () -> assertEquals(expectedCount, savedReservation.getAccompanyingStudentCount()),
            () -> assertNotNull(savedReservation.getAccompanyingStudents()),
            () -> assertEquals("테스트친구", savedReservation.getAccompanyingStudents().get(0).getName())
        );

        System.out.println(">> 통합 테스트 성공: 실제 파일 저장 및 데이터 검증 완료.");
    }
}
