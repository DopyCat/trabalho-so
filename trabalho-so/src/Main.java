import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // =========================================================
    // PARÂMETROS DERIVADOS
    // =========================================================

    /**
     * Infere o tamanho da página dividindo o tamanho da memória virtual (V)
     * pelo número total de páginas (P). Usado apenas quando P vem da entrada
     * e não representa diretamente o tamanho da página.
     */
    private static int inferirTamanhoDaPagina(int V, int P) {
        if (P == 0)
            return 0;
        return V / P;
    }

    /**
     * Calcula parâmetros derivados:
     * - tamanho da página
     * - número de frames na memória física
     * - tamanho mínimo do swap
     */
    private static DerivedParameters calcularParametrosDerivados(int M, int V, int P) {
        DerivedParameters params = new DerivedParameters();

        // Tamanho da página (em bytes, KB, etc.)
        params.tamanhoPagina = inferirTamanhoDaPagina(V, P);

        // Número de frames = memória física / tamanho da página
        params.numFrames = 0;
        if (params.tamanhoPagina > 0)
            params.numFrames = M / params.tamanhoPagina;

        // Tamanho mínimo de swap = memória virtual – memória física
        params.tamanhoSwapMinimo = (long) V - M;
        if (params.tamanhoSwapMinimo < 0)
            params.tamanhoSwapMinimo = 0;

        return params;
    }

    // =========================================================
    // FIFO – First In, First Out
    // =========================================================

    /**
     * Simula o algoritmo FIFO.
     * A página mais antiga na memória é a primeira a ser removida.
     */
    public static SimulationResult simularFIFO(int N_frames, List<Integer> requisicoes, int P) {

        Set<Integer> memoriaFisica = new HashSet<>();     // mantém páginas atualmente nos frames
        Queue<Integer> filaFIFO = new LinkedList<>();     // ordem de chegada das páginas
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();         // páginas expulsas e que ficaram no swap

        for (int paginaRequisitada : requisicoes) {

            // Se a página estava no swap e será usada novamente
            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                // HIT
                continue;
            }

            // PAGE FAULT
            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                // Ainda cabe página na memória
                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            } else {
                // Remove a página mais antiga
                int paginaASubstituir = filaFIFO.poll();
                memoriaFisica.remove(paginaASubstituir);

                // Vai para o swap
                swapState.add(paginaASubstituir);

                // Adiciona a nova página
                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            }
        }

        return new SimulationResult("FIFO", 0, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // RAND – Substituição Aleatória
    // =========================================================

    /**
     * Simula substituição RAND: remove uma página aleatória
     * quando não houver espaço disponível.
     */
    public static SimulationResult simularRAND(int N_frames, List<Integer> requisicoes, int P) {

        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();
        Random rand = new Random();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue; // HIT
            }

            // PAGE FAULT
            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
            } else {
                // Escolhe qualquer página para remover aleatoriamente
                List<Integer> frames = new ArrayList<>(memoriaFisica);
                int indiceAleatorio = rand.nextInt(frames.size());
                int paginaASubstituir = frames.get(indiceAleatorio);

                memoriaFisica.remove(paginaASubstituir);
                swapState.add(paginaASubstituir);

                memoriaFisica.add(paginaRequisitada);
            }
        }

        return new SimulationResult("RAND", 0, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // LRU – Least Recently Used
    // =========================================================

    /**
     * LRU mantém as páginas em ordem de uso.
     * A menos utilizada recentemente é removida.
     */
    public static SimulationResult simularLRU(int N_frames, List<Integer> requisicoes, int P) {

        // LinkedHashSet mantém ordem de inserção e permite reinserção
        LinkedHashSet<Integer> memoriaFisica = new LinkedHashSet<>(N_frames);
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                // HIT – mover para o final como MRU
                memoriaFisica.remove(paginaRequisitada);
                memoriaFisica.add(paginaRequisitada);
            } else {
                // PAGE FAULT
                pageFaults++;

                if (memoriaFisica.size() == N_frames) {
                    // Remove a LRU (primeiro elemento)
                    int paginaASubstituir = memoriaFisica.iterator().next();
                    memoriaFisica.remove(paginaASubstituir);
                    swapState.add(paginaASubstituir);
                }

                memoriaFisica.add(paginaRequisitada);
            }
        }

        return new SimulationResult("LRU", 0, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // MIN – Algoritmo Ótimo
    // =========================================================

    /**
     * Simula MIN: remove a página cujo próximo uso será o mais distante.
     * Se nunca será usada novamente → removida automaticamente.
     */
    public static SimulationResult simularMIN(int N_frames, List<Integer> requisicoes, int P) {

        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int i = 0; i < requisicoes.size(); i++) {

            int paginaRequisitada = requisicoes.get(i);

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue; // HIT
            }

            // PAGE FAULT
            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
            } else {
                // Seleciona página com o uso mais distante
                int paginaASubstituir = -1;
                int maiorDistancia = -1;

                for (int paginaNaMemoria : memoriaFisica) {

                    int distancia = getDistancia(paginaNaMemoria, requisicoes, i + 1);

                    // Escolhe a página que demora mais para aparecer de novo
                    if (distancia > maiorDistancia) {
                        maiorDistancia = distancia;
                        paginaASubstituir = paginaNaMemoria;
                    } else if (distancia == maiorDistancia && paginaNaMemoria > paginaASubstituir) {
                        // Desempate por valor de página
                        paginaASubstituir = paginaNaMemoria;
                    }
                }

                memoriaFisica.remove(paginaASubstituir);
                swapState.add(paginaASubstituir);
                memoriaFisica.add(paginaRequisitada);
            }
        }

        return new SimulationResult("MIN", 0, pageFaults, formatSwapState(swapState));
    }

    // =========================================================
    // FUNÇÕES AUXILIARES
    // =========================================================

    /**
     * Retorna quantas posições à frente a página aparece.
     * Se não aparecer mais → retorna valor gigante.
     */
    private static int getDistancia(int page, List<Integer> requisicoes, int start) {
        for (int j = start; j < requisicoes.size(); j++) {
            if (requisicoes.get(j).equals(page))
                return j;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Converte o estado do swap em uma string ordenada.
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
     * Estrutura com os resultados de cada simulação.
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
     * Parâmetros derivados obtidos da entrada.
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
     * Lê os parâmetros da entrada, processa todas as sequências
     * e imprime resultados dos 4 algoritmos.
     */
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\s+");

        if (!scanner.hasNextInt()) return;
        int M = scanner.nextInt();   // Memória física

        if (!scanner.hasNextInt()) return;
        int V = scanner.nextInt();   // Memória virtual

        if (!scanner.hasNext()) return;
        String A = scanner.next();   // Arquitetura (apenas para documentação)

        if (!scanner.hasNextInt()) return;
        int P = scanner.nextInt();   // Número total de páginas

        if (!scanner.hasNextInt()) return;
        int N = scanner.nextInt();   // Número de sequências de entrada

        DerivedParameters params = calcularParametrosDerivados(M, V, P);

        // Lê todas as sequências de requisições
        List<List<Integer>> todasAsSequencias = new ArrayList<>();
        for (int i = 0; i < N; i++) {

            if (!scanner.hasNextInt()) break;
            int R = scanner.nextInt(); // número de requisições

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
     * Imprime toda a saída no formato exigido.
     */
    private static void imprimirSaida(DerivedParameters params, int N, List<List<Integer>> todasAsSequencias, int P) {

        // Parâmetros derivados
        System.out.println(params.tamanhoPagina);
        System.out.println(params.numFrames);
        System.out.println(params.tamanhoSwapMinimo);
        System.out.println(N);

        // Para cada sequência
        for (List<Integer> requisicoes : todasAsSequencias) {

            // Imprime a sequência original
            System.out.println(requisicoes.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")));

            // Executa cada algoritmo
            SimulationResult resultadoFIFO = simularFIFO(params.numFrames, requisicoes, P);
            SimulationResult resultadoRAND = simularRAND(params.numFrames, requisicoes, P);
            SimulationResult resultadoLRU = simularLRU(params.numFrames, requisicoes, P);
            SimulationResult resultadoMIN = simularMIN(params.numFrames, requisicoes, P);

            // FIFO
            System.out.println("FIFO");
            System.out.println(resultadoFIFO.tempoDecorrido);
            System.out.println(resultadoFIFO.pageFaults);
            System.out.println(resultadoFIFO.swapState);

            // RAND
            System.out.println("RAND");
            System.out.println(resultadoRAND.tempoDecorrido);
            System.out.println(resultadoRAND.pageFaults);
            System.out.println(resultadoRAND.swapState);

            // LRU
            System.out.println("LRU");
            System.out.println(resultadoLRU.tempoDecorrido);
            System.out.println(resultadoLRU.pageFaults);
            System.out.println(resultadoLRU.swapState);

            // MIN
            System.out.println("MIN");
            System.out.println(resultadoMIN.tempoDecorrido);
            System.out.println(resultadoMIN.pageFaults);
            System.out.println(resultadoMIN.swapState);
        }
    }
}
