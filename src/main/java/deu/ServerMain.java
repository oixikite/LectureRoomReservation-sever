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

    public static void main(String[] args) {
        
        LogManager.setState(new DebugState());
        // 일반 모드: 로그가 하나도 안 뜸 (에러 날 때만 뜸)
        //LogManager.setState(new NormalState());

        Scanner scanner = new Scanner(System.in);
        ReservationRepository repo = ReservationRepository.getInstance();

        String backupPath = System.getProperty("user.dir")
                + "/backup/reservations-backup.yaml";

        System.out.println("=======================================================");
        System.out.println("        강의실 예약 서버 (관리자 모드)");
        System.out.println("=======================================================\n");

        System.out.println("1) 예약 데이터 백업");
        System.out.println("2) 예약 데이터 복구");
        System.out.println("3) 서버 실행\n");
        System.out.print("번호 입력: ");

        String input = scanner.nextLine().trim();
        //엔터만 쳤을 경우 예외방지용 체크
        int choice = input.isEmpty() ? 3 : Integer.parseInt(input);
        
        //int choice = Integer.parseInt(scanner.nextLine().trim());

        if (choice == 1) {
            new BackupCommand(repo, backupPath).execute();
            LogManager.log("관리자가 예약 데이터 백업을 실행하였습니다.");
        } else if (choice == 2) {
            new RestoreCommand(repo, backupPath).execute();
            LogManager.log("관리자가 예약 데이터 복구를 실행하였습니다.");
        }

        System.out.println("\n=======================================================");
        LogManager.log("         서버 실행 중... 포트 번호: 9999");
        System.out.println("=======================================================\n");

        try (ServerSocket serverSocket = new ServerSocket(9999)) {

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }

        } catch (Exception e) {
            //에러 발생시 로그 기록
            LogManager.log("[FATAL ERROR] 서버 실행 중 문제 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
