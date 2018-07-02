package backup;

/**
 * @author pedro
 */
import banco.ConexaoBanco;
import banco.dao.DAO;
import org.apache.log4j.Logger;
import util.Config;
import util.DateUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class BackupRestauracao extends DAO {

    public static String exportar(String path) {
        String data = DateUtils.getDataHoraPonto(System.currentTimeMillis());
        data = data.replace(".", "_");
        data = data.replace("-", "__");
        data = data.replace(":", "h");
        data = data + "m";
        String nome = "backup_" + data;

        if (Config.isWindows()) {
            String command = Config.BIN_MYSQL_PATH + "mysqldump.exe";
            ProcessBuilder pb = new ProcessBuilder(
                    command,
                    "--user=neoli831_teste",
                    "--password=teste",
                    ConexaoBanco.DATABASE,
                    "marca", "unidade_medida", "categoria_produto", "produto", "categoria_saida", "saida", "cidade", "bairro", "endereco",
                    "administrador", "cliente", "receita", "forma_pagamento", "manutencao", "venda", "venda_produto",
                    "--result-file=" + path + nome + ".sql");
            try {
                pb.start().waitFor();
                return nome;
            } catch (Exception e) {
                e.printStackTrace();
                Logger.getLogger(BackupRestauracao.class).error(e);
                return null;
            }

        } else {
            String dumpCommand = "mysqldump -u " + ConexaoBanco.USERNAME
                    + " -p" + ConexaoBanco.PASSWORD + " "
                    + ConexaoBanco.DATABASE + ConexaoBanco.getTabelas();

            Runtime rt = Runtime.getRuntime();
            File test = new File(path + nome + ".sql");
            PrintStream ps;
            try {
                Process child = rt.exec(dumpCommand);
                ps = new PrintStream(test);
                InputStream in = child.getInputStream();
                int ch;
                while ((ch = in.read()) != -1) {
                    ps.write(ch);
                }

                InputStream err = child.getErrorStream();
                while ((ch = err.read()) != -1) {
                    System.out.write(ch);
                }

                return nome;
            } catch (Exception ex) {
                Logger.getLogger(BackupRestauracao.class).error(ex);
                return null;
            }
        }
    }

    public boolean importar(String pathImport) {
        //evita bagunçar o bd que está na nuvem
        if (ConexaoBanco.URL.contains("neolig")) {
            return false;
        }

        //primeiro faz backup do banco
        String backup = exportar((new File("")).getAbsolutePath() + Config.getBarra());

        try {
            //exclui o bd
            PreparedStatement stm;
            String sql = "DROP DATABASE " + ConexaoBanco.DATABASE;
            stm = getConector().prepareStatement(sql);
            stm.executeUpdate();
            stm.close();
        } catch (Exception e) {
        }

        try {
            //cria o bd
            executeSqlScript(getConector(), new File(getClass().getClassLoader().getResource( "script_bd.sql").getFile()));

            //insere os dados
            executeSqlScript(getConector(), new File(pathImport));

            //exclui o backup criado
            File file = new File(backup+".sql");
            file.delete();
        } catch (Exception e) {
            //importar(getClass().getClassLoader().getResource(backup).getFile());
            //e.printStackTrace();
        }

        return true;
    }

    public void executeSqlScript(Connection conn, File inputFile) {
        // Delimiter
        String delimiter = ";";
        // Create scanner
        Scanner scanner;
        try {
            scanner = new Scanner(inputFile).useDelimiter(delimiter);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
        // Loop through the SQL file statements
        Statement currentStatement = null;
        while (scanner.hasNext()) {
            // Get statement
            String rawStatement = scanner.next() + delimiter;
            try {
                // Execute statement
                if (!rawStatement.isEmpty()) {
                    currentStatement = conn.createStatement();
                    currentStatement.execute(rawStatement);
                }
            } catch (SQLException e) {
              //  e.printStackTrace();
            } finally {
                // Release resources
                if (currentStatement != null) {
                    try {
                        currentStatement.close();
                    } catch (SQLException e) {
                      //  e.printStackTrace();
                    }
                }
                currentStatement = null;
            }
        }
    }
}
