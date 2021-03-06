package controller;

import banco.ControleDAO;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javax.swing.SwingWorker;
import model.Administrador;
import model.Bairro;
import model.CategoriaProduto;
import model.Cidade;
import model.Cliente;
import model.Endereco;
import model.FormaPagamento;
import model.Marca;
import model.Venda;
import model.VendaProduto;
import org.apache.log4j.Logger;
import util.DateUtils;
import util.alerta.Alerta;

public class TelaVendaController extends AnchorPane {

    private BorderPane painelPrincipal;
    
    private boolean voltarTelaInicial = false;

    private Cliente cliente;
    private Venda venda;

    List<Cidade> listaCidades;
    List<Bairro> listaBairros;
    List<FormaPagamento> listaPagamentos;
    List<Administrador> listaAdministrador;

    List<VendaProduto> listaProdutoVenda;

    @FXML
    private TextField nomeText;
    @FXML
    private TextField telefoneText;
    @FXML
    private TextField cpfText;
    @FXML
    private TextField rgText;

    @FXML
    private HBox cidadeBox;
    @FXML
    private ComboBox<Cidade> cidadeComboBox;

    @FXML
    private HBox adicionarCidadeBox;
    @FXML
    private TextField adicionarCidadeText;

    @FXML
    private HBox bairroBox;
    @FXML
    private ComboBox<Bairro> bairroComboBox;

    @FXML
    private HBox adicionarBairroBox;
    @FXML
    private TextField adicionarBairroText;

    @FXML
    private TextField ruaText;
    @FXML
    private TextField numeroText;

    @FXML
    private DatePicker dataDatePicker;
    @FXML
    private ComboBox<Administrador> vendedorComboBox;
    @FXML
    private ComboBox<FormaPagamento> formarPagComboBox;
    @FXML
    private Spinner<Integer> parcelasSpinner;
    @FXML
    private Button removerButton;
    @FXML
    private Label totalLabel;
    @FXML
    private Label parcelasLabel;
    @FXML
    private TextField descontoText;

    @FXML
    private TableView<VendaProduto> produtosTable;
    @FXML
    private TableColumn<CategoriaProduto, String> categoriaColumn;
    @FXML
    private TableColumn<String, String> descricaoColumn;
    @FXML
    private TableColumn<Marca, String> marcaColumn;
    @FXML
    private TableColumn<Float, String> precoColumn;
    @FXML
    private TableColumn<Float, String> quantidadeColumn;
    @FXML
    private TableColumn<Float, String> totalColumn;
    
    @FXML
    private HBox valorParcelaBox;
    @FXML
    private Label valorParcelasLabel;

    public TelaVendaController(BorderPane painelPrincipal) {
        this.painelPrincipal = painelPrincipal;

        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getClassLoader().getResource("view/TelaVenda.fxml"));
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
        boolean desabilitar = true;
        
        this.nomeText.setDisable(desabilitar);
        this.telefoneText.setDisable(desabilitar);
        this.cpfText.setDisable(desabilitar);
        this.rgText.setDisable(desabilitar);
        this.cidadeBox.setDisable(desabilitar);
        this.bairroBox.setDisable(desabilitar);
        this.ruaText.setDisable(desabilitar);
        this.numeroText.setDisable(desabilitar);
        this.vendedorComboBox.setDisable(desabilitar);
        this.dataDatePicker.setDisable(desabilitar);
        
        valorParcelaBox.setVisible(false);
    }

    private void adicionarPainelInterno(AnchorPane novaTela) {
        this.painelPrincipal.setCenter(novaTela);
    }
    
    public void voltarTelaInicial(boolean telaInicial) {
        this.voltarTelaInicial = telaInicial;
    }
    
    @FXML
    private void cancelarOperacao() {
        if (voltarTelaInicial) {
            TelaInicialController tela = new TelaInicialController(painelPrincipal);
            this.adicionarPainelInterno(tela);
        } else {
            TelaConsultarVendasController tela = new TelaConsultarVendasController(painelPrincipal);
            this.adicionarPainelInterno(tela);
        }
    }

    private void adicionarDadosCliente(Cliente cliente) {
        this.nomeText.setText(cliente.getNome());
        this.telefoneText.setText(cliente.getTelefone());
        this.cpfText.setText(cliente.getCpf());
        this.rgText.setText(cliente.getRg());

        Endereco endereco = cliente.getEndereco();
        Bairro bairro = endereco.getBairro();
        Cidade cidade = bairro.getCidade();

        //this.selecionarCidade();
        this.cidadeComboBox.setValue(cidade);
        //this.cidadeComboBox.getSelectionModel().select(cidade);

        //sincronizarBancoDadosBairro(cidade);
        bairroComboBox.setValue(bairro);

        this.ruaText.setText(endereco.getRua());
        this.numeroText.setText(endereco.getNumero());
    }

    private void adicionarDadosVenda(Venda venda) {
        Long data = venda.getData();
        Administrador vendedor = venda.getAdministrador();
        FormaPagamento formaPagamento = venda.getFormaPagamento();
        
        int parcelas = venda.getQuantidadeParcelas();
        
        this.dataDatePicker.setValue(DateUtils.createLocalDate(data));
        this.vendedorComboBox.setValue(vendedor);
        this.formarPagComboBox.setValue(formaPagamento);
        
        SpinnerValueFactory<Integer> valores = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, parcelas);
        parcelasSpinner.setValueFactory(valores);
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
        adicionarDadosVenda(venda);
        this.cliente = venda.getCliente();
        adicionarDadosCliente(cliente);
        sincronizarBancoDadosProdutosVenda(venda);
    }

    private void sincronizarBancoDadosProdutosVenda(Venda venda) {
        //Metodo executado numa Thread separada
        SwingWorker<List, List> worker = new SwingWorker<List, List>() {
            @Override
            protected List<VendaProduto> doInBackground() throws Exception {
                return ControleDAO.getBanco().getVendaDAO().buscarVendaProduto(venda);
            }

            //Metodo chamado apos terminar a execucao numa Thread separada
            @Override
            protected void done() {
                super.done(); //To change body of generated methods, choose Tools | Templates.
                try {
                    listaProdutoVenda = this.get();
                    atualizarTabela();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(getClass()).error(ex);
                    Alerta.erro("Erro ao consultar Banco de Dados");
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void atualizarTabela() {
        //Transforma a lista em uma Lista Observavel
        ObservableList data = FXCollections.observableArrayList(listaProdutoVenda);
        
        this.categoriaColumn.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        this.descricaoColumn.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        this.marcaColumn.setCellValueFactory(new PropertyValueFactory<>("marca"));
        this.precoColumn.setCellValueFactory(new PropertyValueFactory<>("precoProduto"));
        this.quantidadeColumn.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        this.totalColumn.setCellValueFactory(new PropertyValueFactory<>("precoTotal"));
        atualizarPrecoParcelas();
        
        float preco = venda.getPrecoTotal();
        float precoAtualizado = 0f;
        
        for (VendaProduto vp : listaProdutoVenda) {
            precoAtualizado += vp.getPrecoTotal();
        }
        
        float diferenca = precoAtualizado - preco;
        Float porcentagem = (diferenca * 100) / precoAtualizado;
        Platform.runLater(() -> descontoText.setText(String.valueOf(porcentagem.intValue())));
        
        Platform.runLater(() -> {
            this.totalLabel.setText(new DecimalFormat("#,###.00").format(venda.getPrecoTotal()));
            this.produtosTable.setItems(data);//Adiciona a lista de clientes na Tabela
        });
        
    }
    
    private void atualizarPrecoParcelas() {
        Integer parcela = parcelasSpinner.getValue();
        if (parcela != null) {
            if (parcela > 1 && !listaProdutoVenda.isEmpty()) {
                double valor = venda.getPrecoTotal()/parcela;
                setParcelasLabel(valor);
                Platform.runLater(() -> valorParcelaBox.setVisible(true));
            } else {
                setParcelasLabel(0);
                Platform.runLater(() -> valorParcelaBox.setVisible(false));
            }
        }
    }
    
    private void setParcelasLabel(double valor) {
        Platform.runLater(()-> {
            valorParcelasLabel.setText(new DecimalFormat("#,###.00").format(valor));
        });
    }

}
