import java.util.*;
import java.util.stream.Collectors;

/**
 * Simulador de políticas de substituição de páginas (FIFO, RAND, LRU, MIN).
 *
 * O programa lê parâmetros de memória e sequências de requisições a partir da
 * entrada padrão e executa simulações para cada política, imprimindo
 * estatísticas como tempo decorrido, número de page faults e o estado do swap.
 *
 * Observação: apenas documentação adicionada — nenhuma alteração na lógica.
 */
public class Main {

    // =========================================================
    // PARÂMETROS DERIVADOS
    // =========================================================

    /**
     * Calcula o tamanho (em bytes) de cada página dividindo o espaço virtual
     * total `V` pelo número de páginas `P`.
     *
     * @param V espaço virtual total (bytes)
     * @param P número de páginas virtuais
     * @return tamanho da página em bytes; 0 se `P == 0`
     */
    private static int inferirTamanhoDaPagina(int V, int P) {
        if (P == 0)
            return 0;
        return V / P;
    }

    /**
     * Calcula parâmetros derivados a partir dos valores básicos de memória.
     *
     * - `tamanhoPagina`: inferido por `inferirTamanhoDaPagina`
     * - `numFrames`: número de frames de memória física disponíveis (M / tamanhoPagina)
     * - `tamanhoSwapMinimo`: tamanho mínimo necessário de swap (V - M, não-negativo)
     *
     * @param M memória física disponível (bytes)
     * @param V espaço virtual total (bytes)
     * @param P número de páginas virtuais
     * @return objeto `DerivedParameters` preenchido
     */
    private static DerivedParameters calcularParametrosDerivados(int M, int V, int P) {
        DerivedParameters params = new DerivedParameters();
        params.tamanhoPagina = inferirTamanhoDaPagina(V, P);
        params.numFrames = 0;
        if (params.tamanhoPagina > 0)
            params.numFrames = M / params.tamanhoPagina;
        params.tamanhoSwapMinimo = (long) V - M;
        if (params.tamanhoSwapMinimo < 0)
            params.tamanhoSwapMinimo = 0;
        return params;
    }

    // =========================================================
    // FIFO
    // =========================================================

    /**
     * Simula a política FIFO (first-in, first-out).
     *
     * @param N_frames número de frames disponíveis na memória física
     * @param requisicoes sequência de páginas requisitadas
     * @param P parâmetro de páginas virtuais (não usado diretamente na política)
     * @return `SimulationResult` contendo nome da política, tempo (ms), page faults e estado do swap
     */
    public static SimulationResult simularFIFO(int N_frames, List<Integer> requisicoes, int P) {

        long inicio = System.nanoTime();  // <<--- TEMPO COMEÇA

        Set<Integer> memoriaFisica = new HashSet<>();
        Queue<Integer> filaFIFO = new LinkedList<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue;
            }

            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            } else {
                int paginaASubstituir = filaFIFO.poll();
                memoriaFisica.remove(paginaASubstituir);

                swapState.add(paginaASubstituir);

                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            }
        }

        long fim = System.nanoTime();  // <<--- TEMPO TERMINA
        long tempoMs = (fim - inicio) / 1_000_000;

        return new SimulationResult("FIFO", tempoMs, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // RAND
    // =========================================================

    /**
     * Simula a política RAND (substitui um frame aleatório quando necessário).
     *
     * @param N_frames número de frames disponíveis
     * @param requisicoes sequência de páginas requisitadas
     * @param P parâmetro de páginas virtuais (não usado pela política)
     * @return `SimulationResult` com estatísticas da simulação
     */
    public static SimulationResult simularRAND(int N_frames, List<Integer> requisicoes, int P) {

        long inicio = System.nanoTime(); // <<---

        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();
        Random rand = new Random();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue;
            }

            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
            } else {
                List<Integer> frames = new ArrayList<>(memoriaFisica);
                int indiceAleatorio = rand.nextInt(frames.size());
                int paginaASubstituir = frames.get(indiceAleatorio);

                memoriaFisica.remove(paginaASubstituir);
                swapState.add(paginaASubstituir);

                memoriaFisica.add(paginaRequisitada);
            }
        }

        long fim = System.nanoTime(); // <<---
        long tempoMs = (fim - inicio) / 1_000_000;

        return new SimulationResult("RAND", tempoMs, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // LRU
    // =========================================================

    /**
     * Simula a política LRU (least recently used) utilizando um `LinkedHashSet`
     * para manter a ordem de uso.
     *
     * @param N_frames número de frames disponíveis
     * @param requisicoes sequência de páginas requisitadas
     * @param P parâmetro de páginas virtuais (não usado diretamente pela política)
     * @return `SimulationResult` com estatísticas da simulação
     */
    public static SimulationResult simularLRU(int N_frames, List<Integer> requisicoes, int P) {

        long inicio = System.nanoTime(); // <<---

        LinkedHashSet<Integer> memoriaFisica = new LinkedHashSet<>(N_frames);
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                memoriaFisica.remove(paginaRequisitada);
                memoriaFisica.add(paginaRequisitada);
            } else {

                pageFaults++;

                if (memoriaFisica.size() == N_frames) {
                    int paginaASubstituir = memoriaFisica.iterator().next();
                    memoriaFisica.remove(paginaASubstituir);
                    swapState.add(paginaASubstituir);
                }

                memoriaFisica.add(paginaRequisitada);
            }
        }

        long fim = System.nanoTime(); // <<---
        long tempoMs = (fim - inicio) / 1_000_000;

        return new SimulationResult("LRU", tempoMs, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // MIN
    // =========================================================

    /**
     * Simula a política MIN (ótima), que substitui a página cujo próximo acesso
     * está mais distante no futuro (ou não ocorre mais).
     *
     * Esta implementação calcula a distância até o próximo acesso para cada
     * página residente e seleciona a maior distância.
     *
     * @param N_frames número de frames disponíveis
     * @param requisicoes sequência de páginas requisitadas
     * @param P parâmetro de páginas virtuais (não usado diretamente pela política)
     * @return `SimulationResult` com estatísticas da simulação
     */
    public static SimulationResult simularMIN(int N_frames, List<Integer> requisicoes, int P) {

        long inicio = System.nanoTime(); // <<---

        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int i = 0; i < requisicoes.size(); i++) {

            int paginaRequisitada = requisicoes.get(i);

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue;
            }

            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
            } else {
                int paginaASubstituir = -1;
                int maiorDistancia = -1;

                for (int paginaNaMemoria : memoriaFisica) {

                    int distancia = getDistancia(paginaNaMemoria, requisicoes, i + 1);

                    if (distancia > maiorDistancia) {
                        maiorDistancia = distancia;
                        paginaASubstituir = paginaNaMemoria;
                    } else if (distancia == maiorDistancia && paginaNaMemoria > paginaASubstituir) {
                        paginaASubstituir = paginaNaMemoria;
                    }
                }

                memoriaFisica.remove(paginaASubstituir);
                swapState.add(paginaASubstituir);
                memoriaFisica.add(paginaRequisitada);
            }
        }

        long fim = System.nanoTime(); // <<---
        long tempoMs = (fim - inicio) / 1_000_000;

        return new SimulationResult("MIN", tempoMs, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // FUNÇÕES AUXILIARES
    // =========================================================


    /**
     * Retorna a posição (índice) do próximo acesso à `page` a partir do índice
     * `start` na lista de requisições. Se a página não for encontrada, retorna
     * `Integer.MAX_VALUE` para indicar que não será acessada novamente.
     *
     * @param page página a procurar
     * @param requisicoes lista de requisições
     * @param start índice de início da busca
     * @return índice do próximo acesso ou `Integer.MAX_VALUE` se não houver mais acessos
     */
    private static int getDistancia(int page, List<Integer> requisicoes, int start) {
        for (int j = start; j < requisicoes.size(); j++) {
            if (requisicoes.get(j).equals(page))
                return j;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Formata o estado do swap (conjunto de páginas atualmente no swap) em uma
     * string com números separados por espaço, ordenados.
     *
     * @param swapState conjunto de páginas no swap
     * @return string representando o estado do swap
     */
    private static String formatSwapState(Set<Integer> swapState) {
        return swapState.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    // =========================================================
    // CLASSES AUXILIARES
    // =========================================================

    /**
     * Resultado de uma simulação para uma política específica.
     * Contém: nome da política, tempo decorrido (ms), número de page faults
     * e representação do estado do swap.
     */
    static class SimulationResult {
        String politica;
        long tempoDecorrido;
        long pageFaults;
        String swapState;

        public SimulationResult(String politica, long tempoDecorrido, long pageFaults, String swapState) {
            this.politica = politica;
            this.tempoDecorrido = tempoDecorrido;
            this.pageFaults = pageFaults;
            this.swapState = swapState;
        }
    }

    /**
     * Estrutura para armazenar parâmetros derivados (tamanho de página,
     * número de frames e tamanho mínimo de swap).
     */
    static class DerivedParameters {
        int tamanhoPagina;
        int numFrames;
        long tamanhoSwapMinimo;
    }

    // =========================================================
    // MAIN
    // =========================================================

    /**
     * Ponto de entrada do programa. Lê os parâmetros de memória e N
     * sequências de requisições da entrada padrão, calcula parâmetros
     * derivados e imprime os resultados das simulações para cada sequência.
     *
     * A leitura é organizada conforme o enunciado do trabalho; este método
     * delega a impressão final para `imprimirSaida`.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\s+");

        if (!scanner.hasNextInt()) return;
        int M = scanner.nextInt();

        if (!scanner.hasNextInt()) return;
        int V = scanner.nextInt();

        if (!scanner.hasNext()) return;
        String A = scanner.next();

        if (!scanner.hasNextInt()) return;
        int P = scanner.nextInt();

        if (!scanner.hasNextInt()) return;
        int N = scanner.nextInt();

        DerivedParameters params = calcularParametrosDerivados(M, V, P);

        List<List<Integer>> todasAsSequencias = new ArrayList<>();
        for (int i = 0; i < N; i++) {

            if (!scanner.hasNextInt()) break;
            int R = scanner.nextInt();

            List<Integer> requisicoes = new ArrayList<>();
            for (int j = 0; j < R; j++) {
                if (!scanner.hasNextInt()) break;
                requisicoes.add(scanner.nextInt());
            }

            todasAsSequencias.add(requisicoes);
        }

        imprimirSaida(params, N, todasAsSequencias, P);
        scanner.close();
    }


    /**
     * Imprime os parâmetros derivados e, para cada sequência de requisições,
     * imprime a sequência seguida das estatísticas das simulações (FIFO, RAND,
     * LRU, MIN).
     *
     * @param params parâmetros derivados calculados a partir de M, V e P
     * @param N número de sequências
     * @param todasAsSequencias lista com cada sequência de requisições
     * @param P parâmetro de páginas virtuais (propagado para simulações)
     */
    private static void imprimirSaida(DerivedParameters params, int N, List<List<Integer>> todasAsSequencias, int P) {

        System.out.println(params.tamanhoPagina);
        System.out.println(params.numFrames);
        System.out.println(params.tamanhoSwapMinimo);
        System.out.println(N);

        for (List<Integer> requisicoes : todasAsSequencias) {

            System.out.println(requisicoes.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")));

            SimulationResult rFIFO = simularFIFO(params.numFrames, requisicoes, P);
            SimulationResult rRAND = simularRAND(params.numFrames, requisicoes, P);
            SimulationResult rLRU = simularLRU(params.numFrames, requisicoes, P);
            SimulationResult rMIN = simularMIN(params.numFrames, requisicoes, P);

            System.out.println("FIFO");
            System.out.println(rFIFO.tempoDecorrido);
            System.out.println(rFIFO.pageFaults);
            System.out.println(rFIFO.swapState);

            System.out.println("RAND");
            System.out.println(rRAND.tempoDecorrido);
            System.out.println(rRAND.pageFaults);
            System.out.println(rRAND.swapState);

            System.out.println("LRU");
            System.out.println(rLRU.tempoDecorrido);
            System.out.println(rLRU.pageFaults);
            System.out.println(rLRU.swapState);

            System.out.println("MIN");
            System.out.println(rMIN.tempoDecorrido);
            System.out.println(rMIN.pageFaults);
            System.out.println(rMIN.swapState);
        }
    }
}
