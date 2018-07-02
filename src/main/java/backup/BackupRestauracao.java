package backup;

/**
 * @author pedro
 */

import banco.ConexaoBanco;
import banco.dao.DAO;

import java.io.*;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import util.DateUtils;

public class BackupRestauracao extends DAO {

    /* private static String url = "jdbc:mysql://neolig.com:3306/";
    //private static String url = "jdbc:mysql://localhost:3306/";
    private static String port = "3306";
    private static String database = "neoli831_teste";
    private static String user = "neoli831_teste";
    private static String pass = "teste";
     */

    /* private static String ip = "localhost";
      // private static String port = "3306";
       private static String database = "neoli831_teste";
       private static String user = "neoli831_teste";
       private static String pass = "teste";
      // private static String path = "/home/Admin/abc/";
        */
    public static String exportar(String path) {
        String dumpCommand = "mysqldump -u " + ConexaoBanco.USERNAME
                + " -p" + ConexaoBanco.PASSWORD + " "
                + ConexaoBanco.DATABASE + ConexaoBanco.getTabelas();

        Runtime rt = Runtime.getRuntime();
        File test = new File(path + "backup.sql");
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

            String nome = "Backup_" + DateUtils.getDataHoraPonto(System.currentTimeMillis());
            String novoPath = (test.getAbsolutePath()).replace("backup", nome);
            test.renameTo(new File(novoPath));

            return nome;
        } catch (Exception ex) {
            Logger.getLogger(BackupRestauracao.class).error(ex);
            return null;
        }

    }

    public static boolean importar(String path) throws SQLException, FileNotFoundException {
        // Processo
        try {
            String[] executeCmd = new String[]{
                    "mysql", "-hlocalhost", "-port3306", "--user=" + ConexaoBanco.USERNAME,
                            "--password=" + ConexaoBanco.PASSWORD, "-e", "source " + path};

            Process p = Runtime.getRuntime().exec(executeCmd);
            @SuppressWarnings("unused")
            int in = p.waitFor();
        } catch (InterruptedException x) {
            x.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

}
