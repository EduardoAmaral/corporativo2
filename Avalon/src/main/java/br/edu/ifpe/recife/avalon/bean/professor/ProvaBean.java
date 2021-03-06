/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.ifpe.recife.avalon.bean.professor;

import br.edu.ifpe.recife.avalon.excecao.ValidacaoException;
import br.edu.ifpe.recife.avalon.model.filtro.FiltroQuestao;
import br.edu.ifpe.recife.avalon.model.avaliacao.prova.Prova;
import br.edu.ifpe.recife.avalon.model.avaliacao.prova.ProvaAluno;
import br.edu.ifpe.recife.avalon.model.questao.MultiplaEscolha;
import br.edu.ifpe.recife.avalon.model.questao.Questao;
import br.edu.ifpe.recife.avalon.model.avaliacao.prova.QuestaoProva;
import br.edu.ifpe.recife.avalon.model.questao.VerdadeiroFalso;
import br.edu.ifpe.recife.avalon.model.turma.Turma;
import br.edu.ifpe.recife.avalon.model.usuario.Usuario;
import br.edu.ifpe.recife.avalon.servico.ComponenteCurricularServico;
import br.edu.ifpe.recife.avalon.servico.ProvaServico;
import br.edu.ifpe.recife.avalon.servico.QuestaoServico;
import br.edu.ifpe.recife.avalon.servico.TurmaServico;
import br.edu.ifpe.recife.avalon.util.AvalonUtil;
import br.edu.ifpe.recife.avalon.viewhelper.ComponenteCurricularViewHelper;
import br.edu.ifpe.recife.avalon.viewhelper.PdfGeneratorViewHelper;
import br.edu.ifpe.recife.avalon.viewhelper.QuestaoDetalhesViewHelper;
import br.edu.ifpe.recife.avalon.viewhelper.VisualizarAvaliacaoViewHelper;
import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author eduardoamaral
 */
@Named(value = ProvaBean.NOME)
@SessionScoped
public class ProvaBean extends AvaliacaoBean {

    public static final String NOME = "provaBean";
    private static final String GO_GERAR_PROVA = "goGerarProva";
    private static final String GO_LISTAR_PROVA = "goListarProva";
    private static final String GO_IMPRIMIR_PROVA = "goImprimirProva";
    private static final String GO_DETALHAR_PROVA = "goDetalharProva";
    private static final String GO_RESULTADOS_PROVA = "goResultadosProva";
    private static final String GO_DETALHAR_RESULTADO_PROVA = "goDetalharResultadoProva";
    private static final String USUARIO = "usuario";

    @EJB
    private QuestaoServico questaoServico;

    @EJB
    private ComponenteCurricularServico componenteServico;

    @EJB
    private ProvaServico provaServico;

    @EJB
    private TurmaServico turmaServico;

    private final VisualizarAvaliacaoViewHelper visualizarViewHelper = new VisualizarAvaliacaoViewHelper();
    private final ComponenteCurricularViewHelper componenteViewHelper = new ComponenteCurricularViewHelper();
    private final QuestaoDetalhesViewHelper detalhesViewHelper = new QuestaoDetalhesViewHelper();
    private final HttpSession sessao = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);

    private Usuario usuarioLogado;
    private boolean exibirModalExclusao;
    private Prova prova = new Prova();
    private List<Prova> provas = new ArrayList<>();

    private List<ProvaAluno> resultados = new ArrayList<>();
    private ProvaAluno provaAlunoResultado = new ProvaAluno();
    private Prova provaResultadoSelecionada = new Prova();
    private boolean provaVF;

    private boolean exibirModalReagendamento;
    private Date dataHoraInicioReagendamento;
    private Date dataHoraFimReagendamento;

    private final List<QuestaoProva> questoesProva = new ArrayList<>();
    private final List<QuestaoProva> questoesProvaSelecionadas = new ArrayList<>();

    private List<Turma> turmas = new ArrayList<>();
    private String nomeTurmaSelecionada;

    /**
     * Cria uma nova instância de <code>ProvaBean</code>.
     */
    public ProvaBean() {
        usuarioLogado = (Usuario) sessao.getAttribute(USUARIO);
    }

    /**
     * Inicializa a página de listar provas.
     *
     * @return
     */
    public String iniciarPagina() {
        usuarioLogado = (Usuario) sessao.getAttribute(USUARIO);
        limparPagina();
        buscarProvas();

        return GO_LISTAR_PROVA;
    }

    /**
     * Inicializa a página de gerar nova prova.
     *
     * @return
     */
    public String iniciarPaginaGerar() {
        inicializarHelpers(true);
        limparPaginaGerar();
        buscarTurmas();

        return GO_GERAR_PROVA;
    }

    /**
     * Inicializa a página de detalhar prova.
     *
     * @param provaSelecionada
     * @return
     */
    public String iniciarPaginaDetalhar(Prova provaSelecionada) {
        prova = provaSelecionada;
        carregarQuestoesDetalhar();
        return GO_DETALHAR_PROVA;
    }

    /**
     * Inicializa a página para gerarArquivo uma prova.
     *
     * @return rota para página de geração de prova
     */
    public String iniciarPaginaImprimir() {
        inicializarHelpers(false);
        limparPaginaImprimir();

        return GO_IMPRIMIR_PROVA;
    }

    /**
     * Inicializa a página de resultados de uma prova.
     *
     * @param prova
     * @return
     */
    public String iniciarPaginaResultados(Prova prova) {
        provaResultadoSelecionada = prova;
        buscarResultados(prova);

        return GO_RESULTADOS_PROVA;
    }

    /**
     * Inicializa a prova selecionada para detalha-la.
     *
     * @param provaSelecionada
     * @return rota
     */
    public String iniciarPaginaDetalharResultado(ProvaAluno provaSelecionada) {
        provaAlunoResultado = provaSelecionada;
        provaVF = provaAlunoResultado.getProva().getQuestoes().get(0).getQuestao() instanceof VerdadeiroFalso;
        return GO_DETALHAR_RESULTADO_PROVA;
    }

    /**
     * Carrega as provas do professor logado.
     */
    private void buscarProvas() {
        this.provas.clear();
        this.provas = provaServico.buscarProvasProfessor(usuarioLogado);
    }

    /**
     * Recupera todos os resultados dos alunos em uma prova.
     *
     * @param prova
     */
    private void buscarResultados(Prova prova) {
        resultados = provaServico.buscarResultadosProva(prova);
    }

    /**
     * Limpa a página Listar Provas.
     */
    private void limparPagina() {
        prova = new Prova();
        fecharModalExclusao();
        fecharModalReagendamento();
    }

    /**
     * Limpra a página de geração de provas.
     */
    private void limparPaginaGerar() {
        prova = new Prova();
        nomeTurmaSelecionada = "";
        inicializarQuestoes();
    }

    /**
     * Limpa os campos da tela de geração de provas.
     */
    private void limparPaginaImprimir() {
        inicializarQuestoes();
    }

    private void inicializarQuestoes() {
        super.setTodosSelecionados(false);
        questoesProvaSelecionadas.clear();
        questoesProva.clear();
    }

    /**
     * Inicializa os ViewHelpers.
     *
     * @param apenasQuestoesObjetivas
     */
    private void inicializarHelpers(boolean apenasQuestoesObjetivas) {
        FiltroQuestao filtro = new FiltroQuestao(usuarioLogado.getEmail(), apenasQuestoesObjetivas);

        componenteViewHelper.inicializar(componenteServico);
        detalhesViewHelper.inicializar();
        getPesquisarQuestoesViewHelper().inicializar(questaoServico, filtro);
    }

    /**
     * Seleciona uma prova para exclusão.
     *
     * @param provaSelecionada - prova selecionada.
     */
    public void selecionarProvaExclusao(Prova provaSelecionada) {
        prova = provaSelecionada;
        exibirModalExclusao();
    }

    /**
     * Exclui uma prova selecionada.
     */
    public void excluirProva() {
        provaServico.excluir(prova);
        provas.remove(prova);
        exibirModalExclusao = false;
    }

    /**
     * Exibe o modal de exclusão.
     */
    private void exibirModalExclusao() {
        exibirModalExclusao = true;
    }

    /**
     * Fecha o modal de exclusão.
     */
    public void fecharModalExclusao() {
        exibirModalExclusao = false;
    }

    /**
     * Pesquisa as questões disponíveis para impressão.
     */
    public void pesquisar() {
        buscarQuestoes();
        if (questoesProva.isEmpty()) {
            exibirMensagem(FacesMessage.SEVERITY_INFO, AvalonUtil.getInstance().getMensagem("pesquisa.sem.dados"));
        }
    }

    /**
     * Carrega as as questões do a partir do filtro informado.
     */
    private void buscarQuestoes() {
        inicializarQuestoes();
        for (Questao questao : super.getPesquisarQuestoesViewHelper().pesquisar()) {
            QuestaoProva questaoProva = new QuestaoProva();
            questaoProva.setQuestao(questao);
            questaoProva.setProva(prova);
            questoesProva.add(questaoProva);
        }
    }

    /**
     * Seleciona uma questão da lista de questões.
     *
     * @param questaoProva - questão de prova selecionada.
     */
    public void selecionarQuestaoProva(QuestaoProva questaoProva) {
        if (questaoProva.getQuestao().isSelecionada()) {
            questoesProvaSelecionadas.add(questaoProva);
        } else {
            questoesProvaSelecionadas.remove(questaoProva);
            super.setTodosSelecionados(false);
        }
    }

    /**
     * Marca ou desmarca todas as questões disponíveis para gerar uma prova
     * online.
     */
    public void selecionarTodasQuestoesProva() {
        questoesProvaSelecionadas.clear();

        for (QuestaoProva questaoProva : questoesProva) {
            questaoProva.getQuestao().setSelecionada(super.isTodosSelecionados());
            selecionarQuestaoProva(questaoProva);
        }

    }

    /**
     * Salva uma nova prova.
     *
     * @return
     */
    public String adicionarProva() {
        String navegacao = null;

        try {
            prepararProvaSalvar();
            provaServico.salvar(prova);
            navegacao = iniciarPagina();
        } catch (ValidacaoException ex) {
            exibirMensagem(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }

        return navegacao;
    }

    /**
     * Prepara a prova para finalizar cadastro.
     */
    private void prepararProvaSalvar() {
        prova.setComponenteCurricular(componenteViewHelper.getComponenteCurricularPorId(getPesquisarQuestoesViewHelper().getFiltro().getIdComponenteCurricular()));
        prova.setDataCriacao(Calendar.getInstance().getTime());
        prova.setProfessor(usuarioLogado);
        prova.setQuestoes(new ArrayList<>(questoesProvaSelecionadas));
        prova.setTurma(buscarTurmaPorCodigo());
    }

    /**
     * Recupera a turma selecionada para prova.
     *
     * @return
     */
    private Turma buscarTurmaPorCodigo() {
        if (nomeTurmaSelecionada != null && !nomeTurmaSelecionada.isEmpty()) {
            Collections.sort(turmas);
            return turmas.get(Arrays.binarySearch(turmas.toArray(), new Turma(nomeTurmaSelecionada)));
        }

        return null;
    }

    /**
     * Inicializa a lista de questões a detalhar.
     */
    private void carregarQuestoesDetalhar() {

        if (!prova.getQuestoes().isEmpty()) {

            if (isProvaVerdadeiroFalso()) {
                carregarQuestoesVerdadeiroFalso();
            } else {
                carregarQuetoesMultiplaEscolha();
            }
        }
    }

    /**
     * Verifica se a prova é de V/F
     *
     * @return
     */
    private boolean isProvaVerdadeiroFalso() {
        return prova.getQuestoes().get(0).getQuestao() instanceof VerdadeiroFalso;
    }

    /**
     * Carrega as questões de verdadeiro ou falso da prova para detalhá-la.
     */
    private void carregarQuestoesVerdadeiroFalso() {
        List<VerdadeiroFalso> questoesVF = new ArrayList<>();

        for (QuestaoProva questaoProva : prova.getQuestoes()) {
            questoesVF.add((VerdadeiroFalso) questaoProva.getQuestao());
        }

        visualizarViewHelper.setQuestoesVerdadeiroFalso(questoesVF);
    }

    /**
     * Carrega as questões de múltipla escolha da prova para detalhá-la.
     */
    private void carregarQuetoesMultiplaEscolha() {
        List<MultiplaEscolha> questoesME = new ArrayList<>();

        for (QuestaoProva questaoProva : prova.getQuestoes()) {
            questoesME.add((MultiplaEscolha) questaoProva.getQuestao());
        }

        visualizarViewHelper.setQuestoesMultiplaEscolha(questoesME);
    }

    /**
     * Seleciona uma prova para reagendamento.
     *
     * @param provaSelecionada
     */
    public void selecionarProvaReagendamento(Prova provaSelecionada) {
        prova = provaSelecionada;
        dataHoraInicioReagendamento = new Date(prova.getDataHoraInicio().getTime());
        dataHoraFimReagendamento = new Date(prova.getDataHoraFim().getTime());
        exibirModalReagendamento();
    }

    /**
     * Reagenda uma prova.
     */
    public void reagendarProva() {
        try {
            validarReagendamento();
            prova.setDataHoraInicio(dataHoraInicioReagendamento);
            prova.setDataHoraFim(dataHoraFimReagendamento);
            provaServico.reagendarProva(prova);
            fecharModalReagendamento();
        } catch (ValidacaoException ex) {
            exibirMensagem(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    /**
     * Exibe o modal de reagendamento.
     */
    public void exibirModalReagendamento() {
        exibirModalReagendamento = true;
    }

    /**
     * Fecha o modal de reagendamento.
     */
    public void fecharModalReagendamento() {
        exibirModalReagendamento = false;
        buscarProvas();
    }

    /**
     * Valida se a data de início de uma prova em andamento foi alterada.
     *
     * @throws ValidacaoException
     */
    private void validarReagendamento() throws ValidacaoException {
        if (prova.getDataHoraInicio().getTime() != dataHoraInicioReagendamento.getTime() && prova.getDataHoraInicio().before(Calendar.getInstance().getTime())) {
            throw new ValidacaoException(AvalonUtil.getInstance().getMensagemValidacao("prova.reagendamento.data.inicio.em.andamento"));
        }
    }

    /**
     * Gera o PDF da prova para impressão.
     *
     * @return
     */
    public String imprimirPdf() {
        try {
            PdfGeneratorViewHelper pdf = new PdfGeneratorViewHelper();
            FacesContext facesContext = FacesContext.getCurrentInstance();
            criarResponse(facesContext);
            OutputStream responseOutputStream = FacesContext.getCurrentInstance().getExternalContext().getResponseOutputStream();
            List<Questao> questoesImpressao = new ArrayList<>(getQuestoesSelecionadas());

            pdf.gerarArquivo(responseOutputStream, questoesImpressao);

            facesContext.responseComplete();
        } catch (IOException ex) {
            Logger.getLogger(ProvaBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    /**
     * Cria o response da requisição de download.
     *
     * @return
     */
    private void criarResponse(FacesContext context) {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.reset();
        response.setHeader("Content-Type", "application/pdf");
        response.setHeader("Content-Disposition", "attachment;" + "filename=Prova_" + System.currentTimeMillis() + ".pdf");
    }

    /**
     * Retorna as questões selecionadas.
     *
     * @return
     */
    private List<Questao> getQuestoesSelecionadas() {
        List<Questao> questoes = new ArrayList<>();

        for (QuestaoProva questaoProva : questoesProvaSelecionadas) {
            questoes.add(questaoProva.getQuestao());
        }

        return questoes;
    }

    /**
     * Lista as turmas do professor logado.
     */
    private void buscarTurmas() {
        turmas = turmaServico.buscarTurmas(usuarioLogado);
    }

    /*
        GETTERS AND SETTERS
     */
    public ComponenteCurricularViewHelper getComponenteViewHelper() {
        return componenteViewHelper;
    }

    public QuestaoDetalhesViewHelper getDetalhesViewHelper() {
        return detalhesViewHelper;
    }

    public VisualizarAvaliacaoViewHelper getVisualizarViewHelper() {
        return visualizarViewHelper;
    }

    public Prova getProva() {
        return prova;
    }

    public List<Prova> getProvas() {
        return provas;
    }

    public boolean isExibirModalExclusao() {
        return exibirModalExclusao;
    }

    public List<ProvaAluno> getResultados() {
        return resultados;
    }

    public Prova getProvaResultadoSelecionada() {
        return provaResultadoSelecionada;
    }

    public boolean isExibirModalReagendamento() {
        return exibirModalReagendamento;
    }

    public Date getDataHoraInicioReagendamento() {
        return dataHoraInicioReagendamento;
    }

    public void setDataHoraInicioReagendamento(Date dataHoraInicioReagendamento) {
        this.dataHoraInicioReagendamento = dataHoraInicioReagendamento;
    }

    public Date getDataHoraFimReagendamento() {
        return dataHoraFimReagendamento;
    }

    public void setDataHoraFimReagendamento(Date dataHoraFimReagendamento) {
        this.dataHoraFimReagendamento = dataHoraFimReagendamento;
    }

    public List<QuestaoProva> getQuestoesProva() {
        return questoesProva;
    }

    public List<QuestaoProva> getQuestoesProvaSelecionadas() {
        return questoesProvaSelecionadas;
    }

    public List<Turma> getTurmas() {
        return turmas;
    }

    public String getNomeTurmaSelecionada() {
        return nomeTurmaSelecionada;
    }

    public void setNomeTurmaSelecionada(String nomeTurmaSelecionada) {
        this.nomeTurmaSelecionada = nomeTurmaSelecionada;
    }

    public ProvaAluno getProvaAlunoResultado() {
        return provaAlunoResultado;
    }

    public boolean isProvaVF() {
        return provaVF;
    }

}
