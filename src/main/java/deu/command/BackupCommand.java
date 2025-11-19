package deu.command;

import deu.repository.ReservationRepository;

public class BackupCommand implements Command {

    private final ReservationRepository repository;
    private final String backupFilePath;

    public BackupCommand(ReservationRepository repository, String backupFilePath) {
        this.repository = repository;
        this.backupFilePath = backupFilePath;
    }

    @Override
    public void execute() {
        System.out.println("[BackupCommand] 백업 실행 중...");

        boolean result = repository.exportBackup(backupFilePath);

        if (result) {
            System.out.println("[BackupCommand] 백업 완료 → " + backupFilePath);
        } else {
            System.out.println("[BackupCommand] 백업 실패!");
        }
    }
}
