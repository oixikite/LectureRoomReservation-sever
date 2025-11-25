//package deu.controller;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import deu.controller.business.UserController;
//import deu.model.dto.request.data.user.LoginRequest;
//import deu.model.dto.request.data.user.LogoutRequest;
//import deu.model.dto.request.data.user.SignupRequest;
//import deu.model.dto.request.command.UserCommandRequest;
//import deu.model.dto.response.BasicResponse;
//import deu.model.dto.response.CurrentResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedStatic;
//
//import static org.mockito.Mockito.*;
//
//[기존코드] 삭제 예정
//public class SystemControllerTest {
//
//    @Test
//    @DisplayName("로그인 명령 처리: handleLogin 호출 확인")
//    void handle_login_command_should_call_handleLogin() {
//        try (MockedStatic<UserController> mockedStatic = mockStatic(UserController.class)) {
//            UserController mockUserController = mock(UserController.class);
//            mockedStatic.when(UserController::getInstance).thenReturn(mockUserController);
//
//            LoginRequest loginRequest = new LoginRequest("S2023001", "pw");
//            BasicResponse expected = new BasicResponse("200", "로그인 성공");
//            when(mockUserController.handleLogin(loginRequest)).thenReturn(expected);
//
//            SystemController controller = new SystemController();
//            Object response = controller.handle(new UserCommandRequest("로그인", loginRequest));
//
//            assertTrue(response instanceof BasicResponse);
//            assertEquals("200", ((BasicResponse) response).code);
//        }
//    }
//
//    @Test
//    @DisplayName("회원가입 명령 처리: handleSignup 호출 확인")
//    void handle_signup_command_should_call_handleSignup() {
//        try (MockedStatic<UserController> mockedStatic = mockStatic(UserController.class)) {
//            UserController mockUserController = mock(UserController.class);
//            mockedStatic.when(UserController::getInstance).thenReturn(mockUserController);
//
//            SignupRequest signupRequest = new SignupRequest("S2023001", "pw", "홍길동", "컴공");
//            BasicResponse expected = new BasicResponse("200", "회원가입 성공");
//            when(mockUserController.handleSignup(signupRequest)).thenReturn(expected);
//
//            SystemController controller = new SystemController();
//            Object response = controller.handle(new UserCommandRequest("회원가입", signupRequest));
//
//            assertTrue(response instanceof BasicResponse);
//            assertEquals("200", ((BasicResponse) response).code);
//        }
//    }
//
//    @Test
//    @DisplayName("로그아웃 명령 처리: handleLogout 호출 확인")
//    void handle_logout_command_should_call_handleLogout() {
//        try (MockedStatic<UserController> mockedStatic = mockStatic(UserController.class)) {
//            UserController mockUserController = mock(UserController.class);
//            mockedStatic.when(UserController::getInstance).thenReturn(mockUserController);
//
//            LogoutRequest logoutRequest = new LogoutRequest("S2023001", "pw");
//            BasicResponse expected = new BasicResponse("200", "로그아웃 성공");
//            when(mockUserController.handleLogout(logoutRequest)).thenReturn(expected);
//
//            SystemController controller = new SystemController();
//            Object response = controller.handle(new UserCommandRequest("로그아웃", logoutRequest));
//
//            assertTrue(response instanceof BasicResponse);
//            assertEquals("200", ((BasicResponse) response).code);
//        }
//    }
//
//    @Test
//    @DisplayName("동시접속자 수 요청 처리: handleCurrentUser 호출 확인")
//    void handle_current_user_command_should_call_handleCurrentUser() {
//        try (MockedStatic<UserController> mockedStatic = mockStatic(UserController.class)) {
//            UserController mockUserController = mock(UserController.class);
//            mockedStatic.when(UserController::getInstance).thenReturn(mockUserController);
//
//            CurrentResponse expected = new CurrentResponse(3);
//            when(mockUserController.handleCurrentUser()).thenReturn(expected);
//
//            SystemController controller = new SystemController();
//            Object response = controller.handle(new UserCommandRequest("동시접속자", null));
//
//            assertTrue(response instanceof CurrentResponse);
//            assertEquals(3, ((CurrentResponse) response).currentUserCount);
//        }
//    }
//
//    @Test
//    @DisplayName("알 수 없는 명령 처리: 404 반환")
//    void handle_unknown_command_should_return_404() {
//        SystemController controller = new SystemController();
//        Object response = controller.handle(new UserCommandRequest("삭제", null));
//
//        assertTrue(response instanceof BasicResponse);
//        assertEquals("404", ((BasicResponse) response).code);
//    }
//
//    @Test
//    @DisplayName("잘못된 타입 처리: 405 반환")
//    void handle_invalid_type_should_return_405() {
//        SystemController controller = new SystemController();
//        Object response = controller.handle("이건 명령이 아님");
//
//        assertTrue(response instanceof BasicResponse);
//        assertEquals("405", ((BasicResponse) response).code);
//    }
//}
package deu.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import deu.controller.business.UserController;
import deu.model.dto.request.data.user.LoginRequest;
import deu.model.dto.request.data.user.LogoutRequest;
import deu.model.dto.request.data.user.SignupRequest;
import deu.model.dto.request.command.UserCommandRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.dto.response.CurrentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SystemControllerTest {

    private SystemController systemController;

    @BeforeEach
    void setUp() {
        systemController = SystemController.getInstance();
    }

    // 리플렉션 주입 헬퍼 메서드
    private void injectMockController(Object target, String fieldName, Object mock) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, mock);
        } catch (Exception e) {
            throw new RuntimeException("Mock 주입 실패: " + fieldName, e);
        }
    }

    @Test
    @DisplayName("로그인 명령 처리 테스트")
    void handle_login_command_should_delegate_to_UserController() {
        System.out.println("\n=== [Test] 로그인 명령 위임 테스트 시작 ===");
        
        // 1. Mock 설정
        UserController mockUserController = mock(UserController.class);
        injectMockController(systemController, "userController", mockUserController);

        // 2. 데이터 준비
        LoginRequest loginRequest = new LoginRequest("S2023001", "pw");
        UserCommandRequest commandRequest = new UserCommandRequest("로그인", loginRequest);
        BasicResponse expected = new BasicResponse("200", "로그인 성공");

        when(mockUserController.handle(commandRequest)).thenReturn(expected);

        // 3. 실행
        System.out.println("-> SystemController에 '로그인' 요청 전송");
        Object response = systemController.handle(commandRequest);

        // 4. 결과 확인
        BasicResponse result = (BasicResponse) response;
        System.out.println("-> 반환된 결과 코드: " + result.code);
        System.out.println("-> 반환된 메시지: " + result.data);

        // 5. 검증
        assertEquals("200", result.code);
        verify(mockUserController, times(1)).handle(commandRequest);
        System.out.println("=== [Pass] 로그인 테스트 통과 (UserController 호출 확인됨) ===\n");
    }

    @Test
    @DisplayName("회원가입 명령 처리 테스트")
    void handle_signup_command_should_delegate_to_UserController() {
        System.out.println("\n=== [Test] 회원가입 명령 위임 테스트 시작 ===");
        
        UserController mockUserController = mock(UserController.class);
        injectMockController(systemController, "userController", mockUserController);

        SignupRequest signupRequest = new SignupRequest("S2023001", "pw", "홍길동", "컴공");
        UserCommandRequest commandRequest = new UserCommandRequest("회원가입", signupRequest);
        BasicResponse expected = new BasicResponse("200", "회원가입 성공");

        when(mockUserController.handle(commandRequest)).thenReturn(expected);

        System.out.println("-> SystemController에 '회원가입' 요청 전송");
        Object response = systemController.handle(commandRequest);
        
        BasicResponse result = (BasicResponse) response;
        System.out.println("-> 반환된 결과: " + result.data);

        assertEquals("200", result.code);
        verify(mockUserController, times(1)).handle(commandRequest);
        System.out.println("=== [Pass] 회원가입 테스트 통과 ===\n");
    }

    @Test
    @DisplayName("로그아웃 명령 처리 테스트")
    void handle_logout_command_should_delegate_to_UserController() {
        System.out.println("\n=== [Test] 로그아웃 명령 위임 테스트 시작 ===");

        UserController mockUserController = mock(UserController.class);
        injectMockController(systemController, "userController", mockUserController);

        LogoutRequest logoutRequest = new LogoutRequest("S2023001", "pw");
        UserCommandRequest commandRequest = new UserCommandRequest("로그아웃", logoutRequest);
        BasicResponse expected = new BasicResponse("200", "로그아웃 성공");

        when(mockUserController.handle(commandRequest)).thenReturn(expected);

        System.out.println("-> SystemController에 '로그아웃' 요청 전송");
        Object response = systemController.handle(commandRequest);

        BasicResponse result = (BasicResponse) response;
        System.out.println("-> 반환된 결과: " + result.data);

        assertEquals("200", result.code);
        System.out.println("=== [Pass] 로그아웃 테스트 통과 ===\n");
    }

    @Test
    @DisplayName("동시접속자 수 요청 처리 테스트")
    void handle_current_user_command_should_delegate_to_UserController() {
        System.out.println("\n=== [Test] 동시접속자 조회 위임 테스트 시작 ===");

        UserController mockUserController = mock(UserController.class);
        injectMockController(systemController, "userController", mockUserController);

        UserCommandRequest commandRequest = new UserCommandRequest("동시접속자", null);
        CurrentResponse expected = new CurrentResponse(3);

        when(mockUserController.handle(commandRequest)).thenReturn(expected);

        System.out.println("-> SystemController에 '동시접속자' 요청 전송");
        Object response = systemController.handle(commandRequest);
        
        CurrentResponse result = (CurrentResponse) response;
        System.out.println("-> 반환된 접속자 수: " + result.currentUserCount + "명");

        assertEquals(3, result.currentUserCount);
        System.out.println("=== [Pass] 동시접속자 테스트 통과 ===\n");
    }

    @Test
    @DisplayName("잘못된 타입(405) 처리 테스트")
    void handle_invalid_type_should_return_405() {
        System.out.println("\n=== [Test] 예외(405) 처리 테스트 시작 ===");
        
        String invalidInput = "이건 명령 객체가 아님";
        System.out.println("-> 잘못된 입력값 전송: " + invalidInput);
        
        Object response = systemController.handle(invalidInput);
        BasicResponse result = (BasicResponse) response;
        
        System.out.println("-> 반환된 코드: " + result.code);
        System.out.println("-> 반환된 메시지: " + result.data);

        assertEquals("405", result.code);
        System.out.println("=== [Pass] 예외 처리 테스트 통과 ===\n");
    }
}