/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.controller.business;

/**
 *
 * @author scq37
 */
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
     * 사용자의 알림 목록을 반환하는 핸들러
     * @param userId 사용자 ID (String)
     * @return 알림 리스트가 담긴 BasicResponse
     */
    public BasicResponse handleGetNotifications(String userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getNotifications(userId);
            // 성공적으로 조회 (데이터가 없으면 빈 리스트 반환)
            return new BasicResponse("200", notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse("500", "알림 조회 중 오류 발생: " + e.getMessage());
        }
    }
    
    /**
     * 알림 전체 내역 조회 핸들러
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
