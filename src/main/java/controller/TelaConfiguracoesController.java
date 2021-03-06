package controller;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.apache.log4j.Logger;

public class TelaConfiguracoesController extends AnchorPane {
    
    private BorderPane painelPrincipal;
    
    @FXML
    private TabPane configuracoesTab;
    @FXML
    private Tab administradoresTab;
    @FXML
    private Tab pagamentoTab;
    @FXML
    private Tab produtosTab;
    @FXML
    private Tab enderecosTab;
    @FXML
    private Tab backupTab;
    
    
    public TelaConfiguracoesController(BorderPane painelPrincipal) {
        this.painelPrincipal = painelPrincipal;
        
        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getClassLoader().getResource("view/TelaConfiguracoes.fxml"));
            fxml.setRoot(this);
            fxml.setController(this);
            fxml.load();
        } catch (IOException ex) {
            //Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
    @FXML
    public void initialize() {
        administradoresTab();
    }
    
    private void adicionarPainelInterno(AnchorPane novaTela) {
        this.painelPrincipal.setCenter(novaTela);
    }
    
    @FXML
    private void voltar() {
        TelaInicialController telaInicial = new TelaInicialController(painelPrincipal);
        this.adicionarPainelInterno(telaInicial);
    }
    
    @FXML
    private void administradoresTab() {
        try {
            AnchorPane painel = FXMLLoader.load(getClass().getResource("/view/AdministradorConfiguracoes.fxml"));
            administradoresTab.setContent(painel);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
    @FXML
    private void pagamentoTab() {
        try {
            HBox painel = FXMLLoader.load(getClass().getResource("/view/FormasPagamentoConfiguracoes.fxml"));
            pagamentoTab.setContent(painel);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
    @FXML
    private void produtosTab() {
        try {
            HBox painel = FXMLLoader.load(getClass().getResource("/view/ProdutosConfiguracoes.fxml"));
            produtosTab.setContent(painel);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
    @FXML
    private void enderecosTab() {
        try {
            HBox painel = FXMLLoader.load(getClass().getResource("/view/EnderecosConfiguracoes.fxml"));
            enderecosTab.setContent(painel);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
    @FXML
    private void backupTab() {
        try {
            AnchorPane painel = FXMLLoader.load(getClass().getResource("/view/BackupRestauracaoConfiguracoes.fxml"));
            backupTab.setContent(painel);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
        }
    }
    
}
