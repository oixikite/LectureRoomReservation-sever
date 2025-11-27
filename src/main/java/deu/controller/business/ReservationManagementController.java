package deu.controller.business;

import deu.model.dto.request.data.reservation.DeleteRoomReservationRequest;
import deu.model.dto.request.data.reservation.RoomReservationRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.entity.RoomReservation;
import deu.service.ReservationService;
import lombok.Getter;
import deu.model.dto.request.command.ReservationManagementCommandRequest;

// 예약 관리 컨트롤러
public class ReservationManagementController {
    @Getter
    private static final ReservationManagementController instance = new ReservationManagementController();

    private ReservationManagementController() {}

    private final ReservationService reservationService = ReservationService.getInstance();

    // 예약 수정
    public BasicResponse handleModifyRoomReservation(RoomReservationRequest payload) {
        return reservationService.modifyRoomReservation(payload);
    }

    // 관리자 예약 삭제
    public BasicResponse handleDeleteRoomReservation(DeleteRoomReservationRequest payload) {
        return reservationService.deleteRoomReservationFromManagement(payload);
    }

    // 예약 상태 변경 "대기 -> 완료"
    public BasicResponse handleChangeRoomReservationStatus(String payload) {
        return reservationService.changeRoomReservationStatus(payload);
    }

    // 예약 상태가 "대기" 인 모든 예약 내역 반환
    public BasicResponse handleFindAllRoomReservation() {
        return reservationService.findAllRoomReservation();
    }
    
    /**
     * 퍼사드(SystemController)로부터 위임받은 요청을 처리하는 진입점
     */
    public BasicResponse handle(ReservationManagementCommandRequest request) {
        return switch (request.command) {
            case "예약 수정" -> handleModifyRoomReservation((RoomReservationRequest) request.payload);
            case "예약 삭제" -> handleDeleteRoomReservation((DeleteRoomReservationRequest) request.payload);
            case "예약 상태 변경" -> handleChangeRoomReservationStatus((String) request.payload);
            // "예약 대기 전체 조회"는 페이로드(입력값)가 필요 없는 메서드입니다.
            case "예약 대기 전체 조회" -> handleFindAllRoomReservation(); 
            default -> new BasicResponse("404", "알 수 없는 예약 관리 명령어: " + request.command);
        };
    }

}
