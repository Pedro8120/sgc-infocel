package backup;

import java.util.*;

import java.io.File;


public class Teste {


    private static String SEPARATOR = File.separator;

    private static String MYSQL_PATH =
            "C:" + SEPARATOR +
                    "Arquivos de programas" + SEPARATOR +
                    "MySQL" + SEPARATOR +
                    "MySQL Server 5.7" + SEPARATOR +
                    "bin" + SEPARATOR;


    // Lista dos bancos de dados a serem "backupeados"; se desejar adicionar mais,

    // basta colocar o nome separado por espaços dos outros nomes

    private String dbName = "neoli831_teste";


    private List<String> dbList = new ArrayList<String>();


    public Teste() {
        String command = MYSQL_PATH + "mysqldump.exe";

        System.out.println("Iniciando backups...\n\n");

        // Contador
        int i = 1;
        // Tempo

        long time1, time2, time;
        // Início

        time1 = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(
                command,
                "--user=neoli831_teste",
                "--password=teste",
                dbName,
                "--result-file=" + "." + SEPARATOR + dbName + ".sql");
        try {
            System.out.println(
                    "Backup do banco de dados (" + i + "): " + dbName + " ...");
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        i++;

        // Fim
        time2 = System.currentTimeMillis();
        // Tempo total da operação

        time = time2 - time1;

        // Avisa do sucesso
        System.out.println("\nBackups realizados com sucesso.\n\n");
        System.out.println("Tempo total de processamento: " + time + " ms\n");
        System.out.println("Finalizando...");
    }
}