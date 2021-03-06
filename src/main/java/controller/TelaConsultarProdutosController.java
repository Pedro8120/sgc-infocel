/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import banco.ControleDAO;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javax.swing.SwingWorker;
import model.CategoriaProduto;
import model.Marca;
import model.Produto;
import org.apache.log4j.Logger;
import util.Formatter;
import util.alerta.Alerta;
import util.alerta.Dialogo;

/**
 * FXML Controller class
 *
 * @author cassio
 */
public class TelaConsultarProdutosController extends AnchorPane {

    private BorderPane painelPrincipal;
    private List<Produto> listaProdutos;

    @FXML
    private Button editarButton;
    @FXML
    private Button excluirButton;
    @FXML
    private TextField pesquisaText;
    @FXML
    private TableView<Produto> produtosTable;
    @FXML
    private TableColumn<CategoriaProduto, String> categoriaColumn;
    @FXML
    private TableColumn<Produto, String> descricaoColumn;
    @FXML
    private TableColumn<Marca, String> marcaColumn;
    @FXML
    private TableColumn<Float, String> quantidadeColumn;
    @FXML
    private TableColumn<Float, String> precoColumn;

    public TelaConsultarProdutosController(BorderPane painelPrincipal) {
        this.painelPrincipal = painelPrincipal;

        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getClassLoader().getResource("view/TelaConsultarProdutos.fxml"));
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
        //Desativa os Botoes de Editar e Excluir quando nenhum item na tabela esta selecionado
        editarButton.disableProperty().bind(produtosTable.getSelectionModel().selectedItemProperty().isNull());
        excluirButton.disableProperty().bind(produtosTable.getSelectionModel().selectedItemProperty().isNull());

        Formatter.toUpperCase(pesquisaText);

        pesquisaText.textProperty().addListener((obs, old, novo) -> {
            filtro(novo, listaProdutos, produtosTable);
        });
        
        produtosTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    if (event.getClickCount() == 2) {
                        editarProduto();
                    }
                }
            }
        });
        
        produtosTable.setRowFactory(tv -> new TableRow<Produto>() {
            @Override
            public void updateItem(Produto item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null) {
                    setStyle("");
                } else if (item.getEstoque() > 0f) {
                    setStyle("-fx-border-color: #8BC34A;");
                } else {
                    setStyle("-fx-border-color: #F44336;");
                }
                
            }
        });

        this.sincronizarBancoDados();
    }

    private void adicionarPainelInterno(AnchorPane novaTela) {
        this.painelPrincipal.setCenter(novaTela);
    }

    @FXML
    private void cancelarOperacao() {
        TelaInicialController telaInicial = new TelaInicialController(painelPrincipal);
        this.adicionarPainelInterno(telaInicial);
    }

    @FXML
    private void editarProduto() {
        Produto produto = produtosTable.getSelectionModel().getSelectedItem();
        TelaProdutoController tela = new TelaProdutoController(painelPrincipal);
        tela.setProduto(produto);
        this.adicionarPainelInterno(tela);
    }

    @FXML
    private void excluirProduto() {
        Produto produto = produtosTable.getSelectionModel().getSelectedItem();

        Dialogo.Resposta resposta = Alerta.confirmar("Excluir produto " + produto.getDescricao() + " ?");

        if (resposta == Dialogo.Resposta.YES) {

            try {
                if (ControleDAO.getBanco().getProdutoDAO().excluir(produto)) {
                    this.sincronizarBancoDados();
                    this.atualizarTabela();
                } else {
                    Alerta.erro("Erro ao excluir produto " + produto.getDescricao());
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error(ex);
                Alerta.erro("Erro ao excluir produto " + produto.getDescricao() + "\n" + ex.toString());
                ex.printStackTrace();
            }

        }

        produtosTable.getSelectionModel().clearSelection();
    }

    private void filtro(String texto, List lista, TableView tabela) {
        ObservableList data = FXCollections.observableArrayList(lista);

        FilteredList<Produto> dadosFiltrados = new FilteredList(data, filtro -> true);

        dadosFiltrados.setPredicate(filtro -> {
            if (texto == null || texto.isEmpty()) {
                return true;
            }
            //Coloque aqui as verificacoes da Pesquisa
            if (filtro.getDescricao().toLowerCase().contains(texto.toLowerCase())) {
                return true;
            }
            if (filtro.getCategoria().getDescricao().toLowerCase().contains(texto.toLowerCase())) {
                return true;
            }
            if (filtro.getMarca().getDescricao().toLowerCase().contains(texto.toLowerCase())) {
                return true;
            }

            return false;
        });

        SortedList dadosOrdenados = new SortedList(dadosFiltrados);
        dadosOrdenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(dadosOrdenados);
    }

    private void atualizarTabela() {
        //Transforma a lista em uma Lista Observavel
        ObservableList data = FXCollections.observableArrayList(listaProdutos);
        
        this.categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        this.descricaoColumn.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        this.marcaColumn.setCellValueFactory(new PropertyValueFactory<>("marca"));
        this.quantidadeColumn.setCellValueFactory(new PropertyValueFactory<>("estoque"));
        this.precoColumn.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));

        this.produtosTable.setItems(data);//Adiciona a lista de clientes na Tabela
    }

    private void sincronizarBancoDados() {
        //Metodo executado numa Thread separada
        SwingWorker<List, List> worker = new SwingWorker<List, List>() {
            @Override
            protected List<Produto> doInBackground() throws Exception {
                return ControleDAO.getBanco().getProdutoDAO().listar();
            }

            //Metodo chamado apos terminar a execucao numa Thread separada
            @Override
            protected void done() {
                super.done(); //To change body of generated methods, choose Tools | Templates.
                try {
                    listaProdutos = this.get();
                    atualizarTabela();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(getClass()).error(ex);
                    chamarAlerta("Erro ao consultar Banco de Dados");
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void chamarAlerta(String mensagem) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alerta.erro(mensagem);
            }
        });
    }
}
