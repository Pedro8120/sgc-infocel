package util;

import java.io.Serializable;

public class Config implements Serializable {

    public String DIRETORIO;
    
    public String BIN_MYSQL_PATH; //"C:\\Arquivos de programas\\MySQL\\MySQL Server 5.7\\bin\\";
    public String DIRETORIO_BACKUP;
    public boolean BACKUP_AUTOMATICO;
    public int BACKUP_A_CADA_DIA;
    public Long ULTIMO_BACKUP;
    public Long PROXIMO_BACKUP;
    public String DIRETORIO_RELATORIOS;
    
    public Config(String diretorio) {
        DIRETORIO = diretorio;
        BIN_MYSQL_PATH = "";
        DIRETORIO_BACKUP = DIRETORIO + "Backup_SGC" + getBarra();
        BACKUP_AUTOMATICO = false;
        BACKUP_A_CADA_DIA = 1;
        ULTIMO_BACKUP = null;
        PROXIMO_BACKUP = null;
        DIRETORIO_RELATORIOS = DIRETORIO + "Relatorios" + getBarra();
    }
    
    public static String getBarra() {
        return System.getProperty("file.separator");
    }

    public static boolean isWindows(){
        return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
    }

    public String getUltimoBackup() {
        return DateUtils.getDataHora(ULTIMO_BACKUP);
    }
    
    public String getProximoBackup() {
        return DateUtils.formatDate(PROXIMO_BACKUP);
    }
    
    public void salvarArquivo() throws Exception {
        Arquivo.gravar(this, DIRETORIO);
    }
    
    public void verificar(String diretorio) {
        if (!DIRETORIO.equals(diretorio)) {
            DIRETORIO = diretorio;
            DIRETORIO_BACKUP = DIRETORIO + "Backup_SGC" + getBarra();
            DIRETORIO_RELATORIOS = DIRETORIO + "Relatorios" + getBarra();
        }
    }
}
