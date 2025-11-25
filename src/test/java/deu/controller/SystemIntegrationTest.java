/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.controller;

/**
 *
 * @author scq37
 */

import deu.model.dto.request.command.UserCommandRequest;
import deu.model.dto.request.data.user.LoginRequest;
import deu.model.dto.request.data.user.SignupRequest;
import deu.model.dto.response.BasicResponse;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/*Systemcontroller 통합 테스트
가짜 데이터 사용x
SystemController -> UserController -> Service -> Repository -> 파일(DB)까지 실제로 데이터가 저장되고 읽히는지 확인
시나리오: "회원가입을 한 뒤 -> 그 아이디로 로그인을 하면 -> 성공해야 한다"는 시나리오를 검증

*실행 전 필독:
users.yaml 파일: 이 테스트는 실제로 파일에 데이터를 쓰기때문에, 테스트가 끝나고 data/users.yaml 파일을
열면 test_integration_001이라는 유저가 추가되어 있음. 확인할 것

재실행 시: 테스트를 두 번째 실행하면 "회원가입" 단계에서 "이미 존재하는 아이디"라고 뜰 수 있음.

그래도 2단계(로그인)는 여전히 성공(초록불)해야 정상
*/



// 순서가 중요한 시나리오 테스트이므로 순서 지정 기능 활성화
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemIntegrationTest {
    //Mock이 아니라 '진짜' 인스턴스를 가져옴
    // 생성자가 public이지만, 편의상 getInstance()를 사용
    private final SystemController systemController = SystemController.getInstance();

    // 통합 테스트에 사용할 임시 계정 (테스트 할 때마다 저장되므로 주의)
    // 매번 테스트가 성공하려면 실행 전 users.yaml에서 이 ID를 지우거나, ID를 바꿔야 함
    private static final String TEST_ID = "test_integration_001";
    private static final String TEST_PW = "password123";

    @Test
    @Order(1)
    @DisplayName("[통합 1단계] 실제 회원가입 시나리오")
    void integration_signup_test() {
        System.out.println("\n=== [통합 Test] 1. 회원가입 프로세스 시작 ===");
        
        // 1. 요청 데이터 생성
        SignupRequest signupDto = new SignupRequest(TEST_ID, TEST_PW, "통합테스터", "컴퓨터공학과");
        UserCommandRequest command = new UserCommandRequest("회원가입", signupDto);

        System.out.println("-> [요청] ID: " + TEST_ID + " / PW: " + TEST_PW + " 로 가입 시도");

        // 2. 퍼사드(SystemController)에게 전달
        // (System -> User -> Service -> Repo -> 파일저장 까지 한 번에 실행됨)
        Object responseObj = systemController.handle(command);

        // 3. 결과 확인
        BasicResponse response = (BasicResponse) responseObj;
        System.out.println("-> [응답 코드] " + response.code);
        System.out.println("-> [응답 메시지] " + response.data);

        // 검증: 성공(200)하거나, 만약 이미 테스트를 돌려서 파일에 있다면 중복 오류가 뜰 수 있음.
        // 여기서는 "서버가 죽지 않고 응답을 줬는가"를 핵심으로 봄
        assertNotNull(response.code);
        
        if (response.code.equals("200")) {
            System.out.println(">>> 회원가입 성공 (파일에 저장됨)");
        } else {
            System.out.println(">>> 회원가입 실패 (혹은 이미 존재함): " + response.data);
        }
    }

    @Test
    @Order(2)
    @DisplayName("[통합 2단계] 실제 로그인 시나리오")
    void integration_login_test() {
        System.out.println("\n=== [통합 Test] 2. 로그인 프로세스 시작 ===");

        // 1. 방금 가입한(혹은 존재하는) ID로 로그인 시도
        LoginRequest loginDto = new LoginRequest(TEST_ID, TEST_PW);
        UserCommandRequest command = new UserCommandRequest("로그인", loginDto);

        System.out.println("-> [요청] ID: " + TEST_ID + " 로 로그인 시도");

        // 2. 퍼사드에게 전달
        Object responseObj = systemController.handle(command);
        BasicResponse response = (BasicResponse) responseObj;

        System.out.println("-> [응답 코드] " + response.code);
        System.out.println("-> [응답 메시지] " + response.data);

        // 3. 검증
        // 회원가입이 성공했든, 이미 있었든, 비밀번호가 맞으니 로그인 성공(200)이어야 함
        assertEquals("200", response.code, "DB(파일)에 있는 정보와 일치하여 로그인이 되어야 합니다.");
        System.out.println(">>> 로그인 성공 (파일 읽기 검증 완료)");
    }
    
    @Test
    @Order(3)
    @DisplayName("[통합 3단계] 예외 처리 시나리오")
    void integration_error_test() {
        System.out.println("\n=== [통합 Test] 3. 잘못된 요청 필터링 시작 ===");
        
        // DTO가 아닌 String을 보냈을 때 SystemController가 잘 막아내는지 확인
        Object responseObj = systemController.handle("이상한 문자열");
        BasicResponse response = (BasicResponse) responseObj;
        
        System.out.println("-> [응답 코드] " + response.code);
        System.out.println("-> [응답 메시지] " + response.data);
        
        assertEquals("405", response.code);
        System.out.println(">>> 예외 처리 성공");
    }
}
