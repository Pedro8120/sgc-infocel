package backup;

/**
 * @author pedro
 */

import app.Painel;
import banco.ConexaoBanco;
import banco.dao.DAO;
import org.apache.log4j.Logger;
import util.Config;
import util.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import javafx.application.Platform;
import util.FileCrypt;
import util.alerta.Alerta;

public class BackupRestauracao extends DAO {

    public static String exportar(String path) {
        String data = DateUtils.getDataHoraPonto(System.currentTimeMillis());
        data = data.replace(".", "_");
        data = data.replace("-", "__");
        data = data.replace(":", "h");
        data = data + "m";
        String nome = "Backup_" + data;
        
        File backup = new File(path+nome+".bak");
        if (backup.exists()) {
            nome += "_Backup_Seguranca";
        }

        if (Config.isWindows()) {
            String command = Painel.config.BIN_MYSQL_PATH + "mysqldump.exe";
            ProcessBuilder pb = new ProcessBuilder(
                    command,
                    "--user=" + ConexaoBanco.USERNAME,
                    "--password=" + ConexaoBanco.PASSWORD,
                    ConexaoBanco.DATABASE,
                    "marca", "unidade_medida", "categoria_produto", "produto", "categoria_saida", "saida", "cidade", "bairro", "endereco",
                    "administrador", "cliente", "receita", "forma_pagamento", "manutencao", "venda", "venda_produto",
                    "--result-file=" + path + nome + ".sql");
            try {
                
                pb.start().waitFor();
                
                //--------------------------------------------------------------------------
                String arquivo = path + nome;//Pegando diretorio + nome do aquivo
                FileCrypt criptografia = new FileCrypt();//Criando instancia do Classe FileCrypt
                FileInputStream descriptografado = new FileInputStream(arquivo + ".sql");//Arquivo Descriptografado
                FileOutputStream criptografado = new FileOutputStream(arquivo + ".bak");//Arquivo Criptografado
                boolean finish = criptografia.criptografar(descriptografado, criptografado);//Criptografando arquivo
                new File(arquivo + ".sql").delete();//Deletando arquivo Descriptografado
                //--------------------------------------------------------------------------

                if (!finish) {//Caso houve erro ao realizar criptografia do backup
                    new File(arquivo + ".bak").delete();
                    return null;
                }
                
                return nome;
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(BackupRestauracao.class).error(ex);
                return null;
            }

        } else {
            String dumpCommand = "mysqldump -u "+ ConexaoBanco.USERNAME
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
                
                //--------------------------------------------------------------------------
                String arquivo = path + nome;//Pegando diretorio + nome do aquivo
                FileCrypt criptografia = new FileCrypt();//Criando instancia do Classe FileCrypt
                FileInputStream descriptografado = new FileInputStream(arquivo + ".sql");//Arquivo Descriptografado
                FileOutputStream criptografado = new FileOutputStream(arquivo + ".bak");//Arquivo Criptografado
                boolean finish = criptografia.criptografar(descriptografado, criptografado);//Criptografando arquivo
                descriptografado.close();
                criptografado.close();
                new File(arquivo + ".sql").delete();//Deletando arquivo Descriptografado
                //--------------------------------------------------------------------------
                
                if (!finish) {//Caso houve erro ao realizar criptografia do backup
                    new File(arquivo + ".bak").delete();
                    return null;
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
        
        String scriptImportar = null;
        String scriptBancoDados = null;
        
        File backupImportar = null;//Script SQL pra importar BD
        File criarBD = null;//Script SQL pra criar BD
        
        //Descriptografando Script SQL
        scriptImportar = descriptografarScriptBackup(pathImport);
        
        backupImportar = new File(scriptImportar);//Script SQL pra criar BD

        if (!backupImportar.exists()) {
            Alerta.erro("Erro ao Descriptografar Import");
            return false;//Erro ao Descriptografar Script do Backup
        } else {
            //Descriptografando Script SQL
            scriptBancoDados = descriptografarScriptBD();
            
            criarBD = new File(scriptBancoDados);//Script SQL pra criar BD
            
            if (!criarBD.exists()) {//Erro ao Descriptografar Script do Banco de Dados
                Alerta.erro("Erro ao Descriptografar BD");
                backupImportar.delete();
                return false;
            } else {
                
                //Realizando backup de seguranca do Banco de Dados atual
                String backup = exportar(Painel.config.DIRETORIO_BACKUP);
                
                boolean retorno = false;
                
                try {

                    //Excluindo o Banco de Dados Atual
                    if (criarBD.exists() && backupImportar.exists()) {
                        excluirBancoDados();
                        //Criando o Banco de Dados MySQL
                        executeSqlScript(getConector(), criarBD);
                        //Insere o Banco de Dados importado
                        executeSqlScript(getConector(), backupImportar);

                        retorno = true;
                        
                    } else {
                        if (!criarBD.exists()) {
                            Alerta.erro("Script BD nao existe");
                            backupImportar.delete();
                        }
                        if (!backupImportar.exists()) {
                            Alerta.erro("Script Backup nao existe");
                            criarBD.delete();
                        }
                    }
                
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.getLogger(getClass()).error(ex);
                    Alerta.erro(ex.getMessage());
                }
                
                excluirArquivosSQL(criarBD, backupImportar);
                return retorno;
            }
        }
        
    }
    
    private void excluirArquivosSQL(File file1, File file2) {
        try {
            while (file1.exists() || file2.exists()) {
                if (file1.exists()) {
                    file1.delete();
                    Platform.runLater(() -> file1.delete());
                }
                if (file2.exists()) {
                    file2.delete();
                    Platform.runLater(() -> file2.delete());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error(ex);
        }
    }
    
    private void excluirBancoDados() throws Exception {
        PreparedStatement stm;
        String sql = "DROP DATABASE " + ConexaoBanco.DATABASE;
        stm = getConector().prepareStatement(sql);
        stm.executeUpdate();
        stm.close();
    }
    
    private String descriptografarScriptBD() {
        String diretorio = Painel.config.DIRETORIO_BACKUP + "sgc_infocel.sql";
        try {
            InputStream source = getClass().getClassLoader().getResourceAsStream("script/sgc_infocel.bak");//Script BD Criptografado
            FileOutputStream destino = new FileOutputStream(diretorio);//Salva no diretorio do sistema
            boolean finish = new FileCrypt().descriptografar(source, destino);
            source.close();
            destino.close();
            if (finish) {
                return diretorio;
            }
            new File(diretorio).delete();
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(getClass()).error(ex);
            new File(diretorio).delete();
            return null;
        }
    }
    
    private String descriptografarScriptBackup(String pathImport) {
        String arquivo = pathImport.replaceAll(".bak", "");//Pegando diretorio + nome do aquivo
        try {
            FileCrypt criptografia = new FileCrypt();//Criando instancia do Classe FileCrypt
            FileInputStream criptografado = new FileInputStream(arquivo + ".bak");//Arquivo Criptografado
            FileOutputStream descriptografado = new FileOutputStream(arquivo + ".sql");//Arquivo Desriptografado
            boolean finish = criptografia.descriptografar(criptografado, descriptografado);//Descriptografando arquivo
            criptografado.close();
            descriptografado.close();
            if (finish) {
                return arquivo + ".sql";
            }
            new File(arquivo + ".sql").delete();
            return null;
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
            new File(arquivo + ".sql").delete();
            return null;
        }
    }

    private void executeSqlScript(Connection conn, File inputFile) {
        // Delimiter
        String delimiter = ";";
        // Create scanner
        Scanner scanner;
        try {
            scanner = new Scanner(inputFile).useDelimiter(delimiter);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
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
            } catch (SQLException ex) {
                Logger.getLogger(getClass()).error(ex);
                ex.printStackTrace();
            } finally {
                // Release resources
                if (currentStatement != null) {
                    try {
                        currentStatement.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(getClass()).error(ex);
                        ex.printStackTrace();
                    }
                }
                currentStatement = null;
            }
        }
    }

//    public boolean importar1(String pathImport) {
//        //evita bagunçar o bd que está na nuvem
//        if (ConexaoBanco.URL.contains("neolig")) {
//            return false;
//        }
//        
//        //primeiro faz backup do banco
//        String backup = exportar(Painel.config.DIRETORIO_BACKUP);
//        
//        try {
//            //exclui o bd
//            PreparedStatement stm;
//            String sql = "DROP DATABASE " + ConexaoBanco.DATABASE;
//            stm = getConector().prepareStatement(sql);
//            stm.executeUpdate();
//            stm.close();
//        } catch (Exception ex) {
//            Logger.getLogger(getClass()).error(ex);
//            ex.printStackTrace();
//            return false;
//        }
//
//        try {
//            
//            //Script criptografado
//            InputStream source = getClass().getClassLoader().getResourceAsStream("script/sgc_infocel.bak");//Script BD Criptografado
//            //Script descriptografado
//            FileOutputStream destino = new FileOutputStream(Painel.config.DIRETORIO_BACKUP + "sgc_infocel.sql");//Salva no diretorio do sistema
//            //Decodifica o Escript e salva na salva pasta do sistema
//            new FileCrypt().descriptografar(source, destino);
//            Thread.sleep(2000);
//            //cria o bd
//            executeSqlScript(getConector(), new File(Painel.config.DIRETORIO + "sgc_infocel.sql"));
//            Thread.sleep(3000);
//            //--------------------------------------------------------------------------
//            String arquivo = pathImport.replaceAll(".bak", "");//Pegando diretorio + nome do aquivo
//            FileCrypt criptografia = new FileCrypt();//Criando instancia do Classe FileCrypt
//            FileInputStream criptografado = new FileInputStream(arquivo + ".bak");//Arquivo Criptografado
//            FileOutputStream descriptografado = new FileOutputStream(arquivo + ".sql");//Arquivo Desriptografado
//            boolean finish = criptografia.descriptografar(criptografado, descriptografado);//Descriptografando arquivo
//            //--------------------------------------------------------------------------
//            Thread.sleep(2000);
//            if (!finish) {
//                Alerta.erro("Erro ao Descriptografar Script do Banco de Dados");
//                Platform.runLater(() -> {
//                    new File(arquivo + ".sql").delete();
//                    new File(Painel.config.DIRETORIO_BACKUP + "sgc_infocel.sql").delete();//Deletando Script de criacao do BD
//                });
//                return false;
//            }
//            
//            //insere os dados
//            executeSqlScript(getConector(), new File(arquivo + ".sql"));
//            Thread.sleep(2000);
//            //exclui o backup criado
//            //new File(backup + ".bak").delete();//NAO EXCLUIR O BACKUP POR SEGURANCA
//            
//            //--------------------------------------------------------------------------
////            Platform.runLater(() -> {
////                new File(arquivo + ".sql").delete();//Deletando arquivo Descriptografado
////            });
//            new File(arquivo + ".sql").delete();//Deletando arquivo Descriptografado
//            //--------------------------------------------------------------------------
//            Thread.sleep(1000);
//            //--------------------------------------------------------------------------
////            Platform.runLater(() -> {
////                new File(Painel.config.DIRETORIO_BACKUP + "sgc_infocel.sql").delete();//Deletando Script de criacao do BD
////            });
//            //--------------------------------------------------------------------------
//            new File(Painel.config.DIRETORIO_BACKUP + "sgc_infocel.sql").delete();
//        } catch (Exception ex) {
//            Logger.getLogger(getClass()).error(ex);
//            ex.printStackTrace();
//            Alerta.erro(ex.getMessage());
//            return false;
//        }
//
//        return true;
//    }
    
}
