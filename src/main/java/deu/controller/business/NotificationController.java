/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.controller.business;

/**
 *
 * @author scq37
 */
import deu.model.dto.request.command.NotificationCommandRequest;
import deu.model.dto.response.BasicResponse;
import deu.model.dto.response.NotificationDTO;
import deu.service.NotificationService;
import lombok.Getter;

import java.util.List;

public class NotificationController {

    @Getter
    private static final NotificationController instance = new NotificationController();

    private final NotificationService notificationService = NotificationService.getInstance();

    private NotificationController() {}

    /**
     * [핵심] 클라이언트의 요청 객체(NotificationCommandRequest)를 받아 분기 처리
     * @param request 클라이언트가 보낸 명령 객체
     * @return 처리 결과 (BasicResponse)
     */
   public BasicResponse handle(NotificationCommandRequest request) {
        String command = request.command;
        Object payload = request.payload;
        
        try {
            // payload가 String(사용자 ID)인지 검증
            if (!(payload instanceof String)) {
                 return new BasicResponse("400", "잘못된 데이터 형식입니다. (String ID 필요)");
            }

            String userId = (String) payload;

            return switch (command) {
                case "알림 조회" -> handleGetNotifications(userId);
                case "알림 전체 조회" -> handleGetAllNotifications(userId);
                default -> new BasicResponse("400", "알 수 없는 알림 명령입니다: " + command);
            };

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "서버 알림 처리 중 오류: " + e.getMessage());
        }
    }
   
    /**
     * 사용자의 알림 목록을 반환하는 핸들러 (안 읽은 알림 위주)
     * @param userId 사용자 ID
     */
    public BasicResponse handleGetNotifications(String userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getNotifications(userId);
            // 데이터가 없어도 빈 리스트 반환 (성공 처리)
            return new BasicResponse("200", notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "알림 조회 중 오류 발생: " + e.getMessage());
        }
    }
    
    /**
     * 알림 전체 내역 조회 핸들러 (히스토리용)
     * @param userId 사용자 ID
     */
    public BasicResponse handleGetAllNotifications(String userId) {
        try {
            List<NotificationDTO> allNotifications = notificationService.getAllNotifications(userId);
            return new BasicResponse("200", allNotifications);
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "알림 전체 조회 중 오류: " + e.getMessage());
        }
    }
}
