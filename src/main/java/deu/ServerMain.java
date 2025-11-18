package deu;

import deu.command.BackupCommand;
import deu.command.RestoreCommand;
import deu.repository.ReservationRepository;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) {

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

        int choice = Integer.parseInt(scanner.nextLine().trim());

        if (choice == 1) {
            new BackupCommand(repo, backupPath).execute();
        } else if (choice == 2) {
            new RestoreCommand(repo, backupPath).execute();
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
            e.printStackTrace();
        }
    }
}
