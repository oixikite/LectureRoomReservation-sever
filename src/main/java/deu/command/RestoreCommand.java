package deu.command;

import deu.repository.ReservationRepository;

public class RestoreCommand implements Command {

    private final ReservationRepository repository;
    private final String backupFilePath;

    public RestoreCommand(ReservationRepository repository, String backupFilePath) {
        this.repository = repository;
        this.backupFilePath = backupFilePath;
    }

    @Override
    public void execute() {
        System.out.println("[RestoreCommand] 복구 실행 중...");

        boolean result = repository.importBackup(backupFilePath);

        if (result) {
            System.out.println("[RestoreCommand] 복구 완료 ← " + backupFilePath);
        } else {
            System.out.println("[RestoreCommand] 복구 실패!");
        }
    }
}
