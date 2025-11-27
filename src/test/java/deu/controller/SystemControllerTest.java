package deu.controller;

import deu.controller.business.LectureController;
import deu.controller.business.NotificationController;
import deu.controller.business.ReservationController;
import deu.controller.business.ReservationManagementController;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import deu.controller.business.UserController;
import deu.controller.business.UserManagementController;
import deu.model.dto.request.command.LectureCommandRequest;
import deu.model.dto.request.command.NotificationCommandRequest;
import deu.model.dto.request.command.ReservationCommandRequest;
import deu.model.dto.request.command.ReservationManagementCommandRequest;
import deu.model.dto.request.command.UserCommandRequest;
import deu.model.dto.request.command.UserManagementCommandRequest;
import deu.model.dto.response.BasicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemControllerTest {

    private SystemController systemController;

    private UserController mockUser;
    private UserManagementController mockUserManage;
    private LectureController mockLecture;
    private ReservationController mockReservation;
    private ReservationManagementController mockResManage;
    private NotificationController mockNotification;

    @BeforeEach
    void setUp() {
        systemController = SystemController.getInstance();

        mockUser = mock(UserController.class);
        mockUserManage = mock(UserManagementController.class);
        mockLecture = mock(LectureController.class);
        mockReservation = mock(ReservationController.class);
        mockResManage = mock(ReservationManagementController.class);
        mockNotification = mock(NotificationController.class);

        injectMock(systemController, "userController", mockUser);
        injectMock(systemController, "userManagementController", mockUserManage);
        injectMock(systemController, "lectureController", mockLecture);
        injectMock(systemController, "reservationController", mockReservation);
        injectMock(systemController, "reservationManagementController", mockResManage);
        injectMock(systemController, "notificationController", mockNotification);
    }

    private void injectMock(Object target, String fieldName, Object mock) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, mock);
        } catch (Exception e) {
            throw new RuntimeException("Mock 주입 실패: " + fieldName, e);
        }
    }

    // --- [1] 사용자(User) 라우팅 테스트 ---
    @Test
    @Order(1) // 1번으로 실행
    @DisplayName("UserCommandRequest -> UserController 위임 확인")
    void test_user_routing() {
        System.out.println("\n=== [Test 1] 사용자(User) 라우팅 테스트 ===");
        
        UserCommandRequest request = new UserCommandRequest("로그인", null);
        when(mockUser.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '로그인' 요청 준비 완료");

        systemController.handle(request);
        System.out.println("-> When: SystemController.handle() 실행");

        verify(mockUser, times(1)).handle(request);
        System.out.println("-> Then: UserController.handle() 호출 확인됨 (Pass)");
    }

    // --- [2] 강의(Lecture) 라우팅 테스트 ---
    @Test
    @Order(2) // 2번으로 실행
    @DisplayName("LectureCommandRequest -> LectureController 위임 확인")
    void test_lecture_routing() {
        System.out.println("\n=== [Test 2] 강의(Lecture) 라우팅 테스트 ===");
        
        LectureCommandRequest request = new LectureCommandRequest("강의 조회", null);
        when(mockLecture.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '강의 조회' 요청 준비 완료");

        systemController.handle(request);

        verify(mockLecture, times(1)).handle(request);
        System.out.println("-> Then: LectureController.handle() 호출 확인됨 (Pass)");
    }

    // --- [3] 예약(Reservation) 라우팅 테스트 ---
    @Test
    @Order(3) // 3번으로 실행
    @DisplayName("ReservationCommandRequest -> ReservationController 위임 확인")
    void test_reservation_routing() {
        System.out.println("\n=== [Test 3] 예약(Reservation) 라우팅 테스트 ===");
        
        ReservationCommandRequest request = new ReservationCommandRequest("예약 요청", null);
        when(mockReservation.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '예약 요청' 준비 완료");

        systemController.handle(request);
        verify(mockReservation, times(1)).handle(request);
        System.out.println("-> Then: ReservationController.handle() 호출 확인됨 (Pass)");
    }

    // --- [4] 예약 관리(Admin) 라우팅 테스트 ---
    @Test
    @Order(4) // 4번으로 실행
    @DisplayName("ReservationManagementCommandRequest -> ReservationManagementController 위임 확인")
    void test_reservation_management_routing() {
        System.out.println("\n=== [Test 4] 예약 관리(Admin) 라우팅 테스트 ===");
        
        ReservationManagementCommandRequest request = new ReservationManagementCommandRequest("예약 취소", null);
        when(mockResManage.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '예약 취소' 요청 준비 완료");

        systemController.handle(request);
        verify(mockResManage, times(1)).handle(request);
        System.out.println("-> Then: ReservationManagementController.handle() 호출 확인됨 (Pass)");
    }

    // --- [5] 유저 관리(Admin) 라우팅 테스트 ---
    @Test
    @Order(5) // 5번으로 실행
    @DisplayName("UserManagementCommandRequest -> UserManagementController 위임 확인")
    void test_user_management_routing() {
        System.out.println("\n=== [Test 5] 유저 관리(Admin) 라우팅 테스트 ===");
        
        UserManagementCommandRequest request = new UserManagementCommandRequest("유저 삭제", null);
        when(mockUserManage.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '유저 삭제' 요청 준비 완료");

        systemController.handle(request);
        verify(mockUserManage, times(1)).handle(request);
        System.out.println("-> Then: UserManagementController.handle() 호출 확인됨 (Pass)");
    }

    // --- [6] 알림(Notification) 라우팅 테스트 ---
    @Test
    @Order(6) // 6번으로 실행
    @DisplayName("NotificationCommandRequest -> NotificationController 위임 확인")
    void test_notification_routing() {
        System.out.println("\n=== [Test 6] 알림(Notification) 라우팅 테스트 ===");
        
        NotificationCommandRequest request = new NotificationCommandRequest("알림 조회", null);
        when(mockNotification.handle(request)).thenReturn(new BasicResponse("200", "OK"));
        System.out.println("-> Given: '알림 조회' 요청 준비 완료");

        systemController.handle(request);
        verify(mockNotification, times(1)).handle(request);
        System.out.println("-> Then: NotificationController.handle() 호출 확인됨 (Pass)");
    }
    
    // --- [7] 예외 방어 테스트 ---
    @Test
    @Order(7) // 7번으로 실행
    @DisplayName("[예외] 하위 컨트롤러 에러 발생 시 500 응답 반환")
    void test_exception_handling() {
        System.out.println("\n=== [Test 7] 예외 처리(Exception Handling) 테스트 ===");
        
        UserCommandRequest request = new UserCommandRequest("에러유발", null);
        // 에러를 강제로 던짐
        when(mockUser.handle(request)).thenThrow(new RuntimeException("DB 터짐"));
        System.out.println("-> Given: 하위 컨트롤러에서 'DB 터짐' 에러 발생 설정");

        Object response = systemController.handle(request);
        System.out.println("-> When: SystemController 실행 (에러 발생)");
        
        BasicResponse result = (BasicResponse) response;
        System.out.println("-> Result: 코드=" + result.code + ", 메시지=" + result.data);
        
        assertEquals("500", result.code);
        assertTrue(((String)result.data).contains("DB 터짐"));
        System.out.println("-> Then: 500 에러 코드 및 메시지 확인 완료 (Pass)");
    }
}