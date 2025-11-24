package deu;

import deu.command.BackupCommand;
import deu.command.RestoreCommand;
import deu.repository.ReservationRepository;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import deu.moniter.DebugState;
import deu.moniter.LogManager;
import deu.moniter.NormalState;

public class ServerMain {
    
    //기본 값을 false(normal 모드)로 설정
    private static boolean isDebugMode = false;

    public static void main(String[] args) {
        
        
        Scanner scanner = new Scanner(System.in);
        ReservationRepository repo = ReservationRepository.getInstance();

        String backupPath = System.getProperty("user.dir")
                + "/backup/reservations-backup.yaml";
        
         // 일반 모드: 로그가 하나도 안 뜸 (에러 날 때만 뜸)
        LogManager.setState(new NormalState());
        
        boolean isServerRunning = false;
        
        while(!isServerRunning){
        System.out.println("=======================================================");
        System.out.println("                 강의실 예약 서버");
        System.out.println("=======================================================\n");
        
        // 현재 상태 표시
        String currentStatus = isDebugMode ? "[상세(Debug)]" : "[일반(Normal)]";

        System.out.println("1) 예약 데이터 백업");
        System.out.println("2) 예약 데이터 복구");
        System.out.println("3) 서버 실행\n");
        System.out.println("4) 모니터링 모드 변경 " + currentStatus);
        
        System.out.print("\n번호 입력: ");

        String input = scanner.nextLine().trim();
        //엔터만 쳤을 경우 예외방지용 체크
        int choice = input.isEmpty() ? 0 : Integer.parseInt(input);
        
        switch (choice) {
                case 1:
                    new BackupCommand(repo, backupPath).execute();
                    System.out.println(">> 백업 완료.");
                    break;
                case 2:
                    new RestoreCommand(repo, backupPath).execute();
                    System.out.println(">> 복구 완료.");
                    break;
                case 3:
                    isServerRunning = true;
                    break;
                case 4:
                    // 모드 변경 (토글)
                    isDebugMode = !isDebugMode;
                    
                    if (isDebugMode) {
                        LogManager.setState(new DebugState());
                        System.out.println("\n>>> [설정] 모니터링 모드가 '상세(Debug)'로 변경되었습니다.");
                    } else {
                        LogManager.setState(new NormalState());
                        System.out.println("\n>>> [설정] 모니터링 모드가 '일반(Normal)'로 변경되었습니다.");
                    }
                    break;
                default:
                    System.out.println(">> 잘못된 입력입니다.");
            }
        }

        System.out.println("\n=======================================================");
        System.out.println("         서버 실행 중... 포트 번호: 9999");
        System.out.println("=======================================================\n");

        try (ServerSocket serverSocket = new ServerSocket(9999)) {

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }

        } catch (Exception e) {
            //에러 발생시 로그 기록
            System.err.println("[FATAL ERROR] 서버 실행 중 문제 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

