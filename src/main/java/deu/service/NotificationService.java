/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.service;

/**
 *
 * @author scq37
 */
import deu.model.dto.response.NotificationDTO;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 알림 내역을 관리하는 서비스 (싱글톤)
 * 파일 입출력을 통해 데이터를 영구 보존합니다.
 */
public class NotificationService {

    private static final NotificationService instance = new NotificationService();
    
    // 저장 파일 경로
    private static final String FILE_PATH = "data/notifications.dat";

    // Key: 사용자 ID, Value: 해당 사용자의 알림 리스트
    private Map<String, List<NotificationDTO>> notificationDatabase;

    private NotificationService() {
        // 서비스 시작 시 파일에서 데이터 로드
        this.notificationDatabase = loadFromFile();
        if (this.notificationDatabase == null) {
            this.notificationDatabase = new ConcurrentHashMap<>();
        }
    }

    public static NotificationService getInstance() {
        return instance;
    }

    /**
     * 특정 사용자에게 알림을 추가하고 파일에 저장합니다.
     * @param userId 사용자 ID (학번)
     * @param notification 추가할 알림 DTO
     */
    public synchronized void addNotification(String userId, NotificationDTO notification) {
        List<NotificationDTO> list = notificationDatabase.computeIfAbsent(userId, k -> new ArrayList<>());
        list.add(notification);
        saveToFile(); // 변경 사항 즉시 저장
        System.out.println("[NotificationService] " + userId + "에게 알림 저장됨: " + notification.getTitle());
    }

    /**
     * 특정 사용자의 모든 알림 목록을 반환합니다.
     * @param userId 사용자 ID
     * @return 알림 리스트 (없으면 빈 리스트 반환)
     */
  public synchronized List<NotificationDTO> getNotifications(String userId) {
        if (notificationDatabase.containsKey(userId)) {
            List<NotificationDTO> allNotifications = notificationDatabase.get(userId);
            List<NotificationDTO> unreadNotifications = new ArrayList<>();
            
            boolean needSave = false;

            // 안 읽은 알림만 골라내고 상태 변경
            for (NotificationDTO notification : allNotifications) {
                if (!notification.isRead()) { // 안 읽음(false)인 경우
                    unreadNotifications.add(notification);
                    notification.setRead(true); // 읽음(true)으로 변경
                    needSave = true;
                }
            }

            // 변경사항이 있으면 파일에 저장
            if (needSave) {
                saveToFile();
            }

            return unreadNotifications;
        }
        return new ArrayList<>();
    }
    
    // --- 파일 입출력 메서드 ---

    private void saveToFile() {
        // 폴더 생성
        File file = new File(FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(notificationDatabase);
        } catch (IOException e) {
            System.err.println("[NotificationService] 파일 저장 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<NotificationDTO>> loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<String, List<NotificationDTO>>) ois.readObject();
        } catch (Exception e) {
            System.err.println("[NotificationService] 파일 로드 실패 (새 DB 생성): " + e.getMessage());
            return null;
        }
    }
}