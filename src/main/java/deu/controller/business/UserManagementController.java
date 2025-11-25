package deu.controller.business;

import deu.model.dto.request.command.UserManagementCommandRequest;
import deu.model.dto.request.data.user.*;
import deu.model.dto.response.BasicResponse;
import deu.service.UserService;

public class UserManagementController {

    private static final UserManagementController instance = new UserManagementController();

    private UserManagementController() {}

    public static UserManagementController getInstance() {
        return instance;
    }

    private final UserService userService = UserService.getInstance();

    // 사용자 정보 수정 처리
    public Object handleUpdateUser(UserDataModificationRequest payload) {
        return userService.update(payload);
    }

    // 사용자 삭제 처리
    public Object handleDeleteUser(DeleteRequest payload) {
        return userService.delete(payload);
    }

    // 사용자 단일 조회 처리
    public Object handleFindUser(FindRequest payload) {
        return userService.find(payload);
    }

    // 전체 사용자 목록 조회 처리
    public Object handleFindAllUsers() {
        return userService.findAll();
    }
    
    //퍼사드로부터 위임받은 요청 처리
    public Object handle(UserManagementCommandRequest request) {
        return switch (request.command) {
            case "사용자 수정" -> handleUpdateUser((UserDataModificationRequest) request.payload);
            case "사용자 삭제" -> handleDeleteUser((DeleteRequest) request.payload);
            case "사용자 조회" -> handleFindUser((FindRequest) request.payload);
            case "전체 사용자 조회" -> handleFindAllUsers();
            default -> new BasicResponse("404", "알 수 없는 사용자 관리 명령어");
        };
    }
}