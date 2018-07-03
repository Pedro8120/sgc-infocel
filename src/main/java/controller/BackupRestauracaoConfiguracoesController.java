/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import app.Painel;
import backup.BackupRestauracao;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import util.Config;
import util.DateUtils;
import util.alerta.Alerta;
import util.alerta.Dialogo;

/**
 * FXML Controller class
 *
 * @author cassio
 */
public class BackupRestauracaoConfiguracoesController implements Initializable {

    private Config config;

    @FXML
    private CheckBox backupAutomaticoCheckBox;
    @FXML
    private Spinner<Integer> diasSpinner;
    @FXML
    private TextField caminhoBackupText;
    @FXML
    private Label ultimoBackupLabel;
    @FXML
    private Label proximoBackupLabel;
    @FXML
    private Button btnImportar, btnBackup;
    @FXML
    private TextField caminhoComprovantesText;

    @FXML
    private VBox backupBox, comprovantesBox;

    @FXML
    private StackPane stackPane;
    private ProgressIndicator indicator = new ProgressIndicator();
    @FXML
    private VBox mySQLBox;
    @FXML
    private TextField caminhoMySQLText;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnBackup.setDisable(false);
        btnImportar.setDisable(false);

        config = Painel.config;
        stackPane.getChildren().add(indicator);
        indicator.setVisible(false);

        backupAutomaticoCheckBox.setSelected(config.BACKUP_AUTOMATICO);
        caminhoBackupText.appendText(config.DIRETORIO_BACKUP);
        caminhoComprovantesText.appendText(config.DIRETORIO_RELATORIOS);
        
        if (config.ULTIMO_BACKUP != null) {
            ultimoBackupLabel.setText(config.getUltimoBackup());
        }
        if (config.PROXIMO_BACKUP != null) {
            proximoBackupLabel.setText(config.getProximoBackup());
        }
        if (!config.BIN_MYSQL_PATH.isEmpty()) {
            caminhoMySQLText.setText(config.BIN_MYSQL_PATH);
        }
        
        if (!Config.isWindows()) {
            mySQLBox.setVisible(false);
        }

        diasSpinner.disableProperty().bind(backupAutomaticoCheckBox.selectedProperty().not());
        ultimoBackupLabel.disableProperty().bind(backupAutomaticoCheckBox.selectedProperty().not());
        proximoBackupLabel.disableProperty().bind(backupAutomaticoCheckBox.selectedProperty().not());

        SpinnerValueFactory<Integer> valores = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, config.BACKUP_A_CADA_DIA);
        diasSpinner.setValueFactory(valores);

        diasSpinner.setOnMouseClicked((e) -> {
            int dias = diasSpinner.getValue();
            config.BACKUP_A_CADA_DIA = dias;
            LocalDate ultimoBackup = DateUtils.createLocalDate(config.ULTIMO_BACKUP);
            LocalDate proximoBackup = ultimoBackup.plusDays(dias);
            config.PROXIMO_BACKUP = DateUtils.getLong(proximoBackup);
            proximoBackupLabel.setText(config.getProximoBackup());
            try {
                config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        });

        backupAutomaticoCheckBox.setOnAction((e) -> {
            boolean selecionado = backupAutomaticoCheckBox.isSelected();
            config.BACKUP_AUTOMATICO = selecionado;
            if (selecionado) {

                if (config.ULTIMO_BACKUP == null) {
                    Long hoje = System.currentTimeMillis();
                    config.ULTIMO_BACKUP = hoje;
                }

                int dias = diasSpinner.getValue();
                LocalDate ultimoBackup = DateUtils.createLocalDate(config.ULTIMO_BACKUP);
                LocalDate proximoBackup = ultimoBackup.plusDays(dias);
                config.PROXIMO_BACKUP = DateUtils.getLong(proximoBackup);

                Long hoje = System.currentTimeMillis();
                if (DateUtils.getLong(proximoBackup) < hoje) {
                    hoje = System.currentTimeMillis();
                    config.ULTIMO_BACKUP = hoje;
                    ultimoBackup = DateUtils.createLocalDate(config.ULTIMO_BACKUP);
                    proximoBackup = ultimoBackup.plusDays(dias);
                    config.PROXIMO_BACKUP = DateUtils.getLong(proximoBackup);
                }

                proximoBackupLabel.setText(config.getProximoBackup());
                ultimoBackupLabel.setText(config.getUltimoBackup());
            }
            try {
                config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        });
    }

    private void atualizarDatas(Long data) {
        if (Painel.config.BACKUP_AUTOMATICO) {
            int dias = diasSpinner.getValue();
            Painel.config.ULTIMO_BACKUP = data;

            LocalDate ultimoBackup = DateUtils.createLocalDate(data);
            LocalDate proximoBackup = ultimoBackup.plusDays(dias);
            Painel.config.PROXIMO_BACKUP = DateUtils.getLong(proximoBackup);

            proximoBackupLabel.setText(Painel.config.getProximoBackup());
            ultimoBackupLabel.setText(Painel.config.getUltimoBackup());

            try {
                Painel.config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
    
    public void backupAutomatico() {
        String path = Painel.config.DIRETORIO_BACKUP + Config.getBarra();// + nome + ".sql";

        //Metodo executado numa Thread separada
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                File pasta = new File(path);
                if (pasta.exists() == false) {
                    pasta.mkdirs();
                }

                return BackupRestauracao.exportar(path);
            }

            //Metodo chamado apos terminar a execucao numa Thread separada
            @Override
            protected void done() {
                super.done(); //To change body of generated methods, choose Tools | Templates.

                try {
                    if (get() != null) {
                        Alerta.info("Nome do arquivo: " + get(), "Backup automático realizado com sucesso!");
                        atualizarDatas(System.currentTimeMillis());
                    } else {
                        Alerta.erro("Erro ao realizar backup automático");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(getClass()).error(ex);
                    Alerta.erro("Erro ao realizar backup automático", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    @FXML
    private void realizarBackup(ActionEvent event) {
        backupBox.setVisible(false);
        comprovantesBox.setVisible(false);
        btnBackup.setVisible(false);
        btnImportar.setVisible(false);
        indicator.setVisible(true);

        // String nome = "Backup_" + DateUtils.getDataHoraPonto(System.currentTimeMillis());
        String path = caminhoBackupText.getText() + Config.getBarra();// + nome + ".sql";

        //Metodo executado numa Thread separada
        SwingWorker<String, String> worker = new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                File pasta = new File(path);
                if (pasta.exists() == false) {
                    pasta.mkdirs();
                }

                return BackupRestauracao.exportar(path);
            }

            //Metodo chamado apos terminar a execucao numa Thread separada
            @Override
            protected void done() {
                backupBox.setVisible(true);
                comprovantesBox.setVisible(true);
                btnBackup.setVisible(true);
                btnImportar.setVisible(true);
                indicator.setVisible(false);

                super.done(); //To change body of generated methods, choose Tools | Templates.

                try {
                    if (get() != null) {
                        Alerta.info("Nome do arquivo: " + get(), "Backup realizado com sucesso!");
                        atualizarDatas(System.currentTimeMillis());
                    } else {
                        Alerta.erro("Erro ao realizar backup");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(getClass()).error(ex);
                    Alerta.erro("Erro ao realizar backup", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    @FXML
    private void importar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Escolha o diretório para Backup");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SQL files (*.bak)", "*.bak");
        chooser.getExtensionFilters().add(extFilter);

        if ((new File(config.DIRETORIO_BACKUP).exists()))
            chooser.setInitialDirectory(new File(config.DIRETORIO_BACKUP));
        
        File arquivo = chooser.showOpenDialog(Painel.palco);
        
        if (arquivo != null) {
            String path = arquivo.getAbsolutePath();

            caminhoBackupText.setText(path);

            Dialogo.Resposta resposta = Alerta.confirmar("Deseja realmente importar o Banco de Dados " + arquivo.getName());
            if (resposta == Dialogo.Resposta.YES) {
                importarBancoDados();
            }
        }
    }

    private void importarBancoDados() {
        backupBox.setVisible(false);
        comprovantesBox.setVisible(false);
        btnBackup.setVisible(false);
        btnImportar.setVisible(false);
        indicator.setVisible(true);

        //Metodo executado numa Thread separada
        SwingWorker<Boolean, Boolean> worker = new SwingWorker<Boolean, Boolean>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (new BackupRestauracao().importar(caminhoBackupText.getText())) {
                    return true;
                } else {
                    return false;
                }
            }

            //Metodo chamado apos terminar a execucao numa Thread separada
            @Override
            protected void done() {
                backupBox.setVisible(true);
                comprovantesBox.setVisible(true);
                btnBackup.setVisible(true);
                btnImportar.setVisible(true);
                indicator.setVisible(false);
                
                super.done(); //To change body of generated methods, choose Tools | Templates.

                try {
                    if (get()) {
                        Alerta.info("Nome do arquivo: " + (new File(caminhoBackupText.getText())).getName(), "Importado com sucesso!");
                    } else {
                        Alerta.erro("Erro ao realizar importação");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(getClass()).error(ex);
                    Alerta.erro("Erro ao realizar importação", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    @FXML
    private void alterarBackup(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolha o diretório para Backup");

        if ((new File(config.DIRETORIO_BACKUP).exists()))
            chooser.setInitialDirectory(new File(config.DIRETORIO_BACKUP));

        File arquivo = chooser.showDialog(Painel.palco);

        if (arquivo != null) {
            String diretorio = arquivo.getAbsolutePath();

            caminhoBackupText.setText(diretorio);
            config.DIRETORIO_BACKUP = diretorio;
            
            try {
                config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    @FXML
    private void alterarComprovantes(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolha o diretório dos Comprovantes");

        File pasta = new File(config.DIRETORIO_RELATORIOS);

        if ((new File(config.DIRETORIO_BACKUP).exists()))
            chooser.setInitialDirectory(new File(config.DIRETORIO_RELATORIOS));

        File arquivo = chooser.showDialog(Painel.palco);

        if (arquivo != null) {
            String diretorio = arquivo.getAbsolutePath() + Config.getBarra();

            caminhoComprovantesText.setText(diretorio);
            config.DIRETORIO_RELATORIOS = diretorio;

            try {
                config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    @FXML
    private void alterarMySQL(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolha a pasta de instalação do MySQL [BIN]");

        if ((new File(config.BIN_MYSQL_PATH).exists()))
            chooser.setInitialDirectory(new File(config.BIN_MYSQL_PATH));

        File arquivo = chooser.showDialog(Painel.palco);

        if (arquivo != null) {
            String diretorio = arquivo.getAbsolutePath();

            caminhoMySQLText.setText(diretorio);
            config.BIN_MYSQL_PATH = diretorio;
            
            try {
                config.salvarArquivo();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
}
