package deu.controller;

import deu.controller.business.*;
import deu.model.dto.request.command.*;
import deu.model.dto.response.BasicResponse;

/**
 * [Facade Pattern 적용] 시스템의 통합 인터페이스 (System Controller)
 * * 역할:
 * 1. 복잡한 서브시스템(User, Lecture, Reservation 등)에 대한 단일 진입점(Entry Point) 제공
 * 2. 클라이언트(View/Server)와 비즈니스 로직(Business Controller) 간의 결합도(Coupling) 감소
 * 3. 요청 객체(Request)의 타입에 따라 적절한 하위 컨트롤러로 책임 위임(Delegation)
 */

public class SystemController {
    
    // --- Subsystems (하위 시스템 컴포넌트) ---
    // 퍼사드는 하위 시스템의 인스턴스를 소유하며, 요청을 위임
    
    private final UserController userController = UserController.getInstance();
    private final UserManagementController userManagementController = UserManagementController.getInstance();
    private final LectureController lectureController = LectureController.getInstance();
    private final ReservationController reservationController = ReservationController.getInstance();
    private final ReservationManagementController reservationManagementController = ReservationManagementController.getInstance(); 
    private final NotificationController notificationController = NotificationController.getInstance();
  
    private static final SystemController instance = new SystemController();
    public SystemController() {}
    public static SystemController getInstance() { return instance; }
    
    /**
     * 퍼사드 패턴의 핵심 진입점 (Facade Entry Point)
     * - 클라이언트의 요청 객체 타입을 확인하여 적절한 컨트롤러로 위임(Delegate)
     * - 구체적인 명령(Command)이나 데이터(Payload)는 열어보지 않음
     *
     * * [디자인 패턴 적용 효과]
     * 1. 캡슐화(Encapsulation): 클라이언트는 구체적인 명령어("로그인", "예약" 등)의 처리 로직을 알 필요 x
     * 2. OCP(Open-Closed Principle): 새로운 기능(예: "비밀번호 변경")이 추가되어도 
     * 이 클래스(SystemController)는 수정할 필요 없이, 하위 컨트롤러(UserController)만 수정하면 됨
     */
    
    public Object handle(Object request) {
        try {
            // 사용자 관련 요청 (로그인, 로그아웃 등) -> UserController 위임
            if (request instanceof UserCommandRequest r) {                
                return userController.handle(r);
            }

            // 사용자 관리(admin) 요청 -> UserManagementController 위임
            else if (request instanceof UserManagementCommandRequest r) {
                return userManagementController.handle(r);
            }

            // 예약 요청 -> ReservationController 위임
            else if (request instanceof ReservationCommandRequest r) {
                return reservationController.handle(r);
            }

            // 예약 관리 요청 -> ReservationManagementController 위임
            else if (request instanceof ReservationManagementCommandRequest r) {
                return reservationManagementController.handle(r);
            }

            // 강의 요청 -> LectureController 위임
            else if (request instanceof LectureCommandRequest r) {
                return lectureController.handle(r);
            }
            
            // 알림 요청 -> NotificationController 위임
            else if (request instanceof NotificationCommandRequest r) {
                    return notificationController.handle(r);
            }
            
            return new BasicResponse("405", "지원하지 않는 요청 타입");
            
        } catch (Exception e) {
            e.printStackTrace(); // 로그 출력 (디버깅)
            return new BasicResponse("500", "서버 처리 중 예외 발생: " + e.getMessage());
        }
    }
}