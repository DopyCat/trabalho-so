import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // =========================================================
    // PARÂMETROS DERIVADOS
    // =========================================================

    private static int inferirTamanhoDaPagina(int V, int P) {
        if (P == 0)
            return 0;
        return V / P;
    }

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

    public static SimulationResult simularFIFO(int N_frames, List<Integer> requisicoes, int P) {
        Set<Integer> memoriaFisica = new HashSet<>();
        Queue<Integer> filaFIFO = new LinkedList<>();
        long pageFaults = 0;

        // Swap inicia vazio (só páginas expulsas e que não voltam)
        Set<Integer> swapState = new HashSet<>();

        for (int paginaRequisitada : requisicoes) {

            // Caso a página esteja no swap e volte a ser colocada na memória → remover do swap
            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                // HIT — nada a expulsar
                continue;
            }

            // PAGE FAULT
            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                // Cabe na memória
                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            } else {
                // Substituição FIFO
                int paginaASubstituir = filaFIFO.poll();
                memoriaFisica.remove(paginaASubstituir);

                // Página expulsa vai para o swap
                swapState.add(paginaASubstituir);

                // Insere página nova
                memoriaFisica.add(paginaRequisitada);
                filaFIFO.offer(paginaRequisitada);
            }
        }

        return new SimulationResult("FIFO", 0, pageFaults, formatSwapState(swapState));
    }

     // =========================================================
    // RAND
    // =========================================================
    public static SimulationResult simularRAND(int N_frames, List<Integer> requisicoes, int P) {
        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();
        Random rand = new Random();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue; // hit
            }

            pageFaults++;

            if (memoriaFisica.size() < N_frames) {
                memoriaFisica.add(paginaRequisitada);
            } else {
                // Escolhe página aleatória para substituir
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
    // LRU
    // =========================================================
    public static SimulationResult simularLRU(int N_frames, List<Integer> requisicoes, int P) {
        LinkedHashSet<Integer> memoriaFisica = new LinkedHashSet<>(N_frames);
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int paginaRequisitada : requisicoes) {

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                // HIT: move para o final (MRU)
                memoriaFisica.remove(paginaRequisitada);
                memoriaFisica.add(paginaRequisitada);
            } else {
                // FAULT
                pageFaults++;

                if (memoriaFisica.size() == N_frames) {
                    // Remove LRU (primeira do LinkedHashSet)
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
    // MIN
    // =========================================================
    public static SimulationResult simularMIN(int N_frames, List<Integer> requisicoes, int P) {
        Set<Integer> memoriaFisica = new HashSet<>();
        long pageFaults = 0;

        Set<Integer> swapState = new HashSet<>();

        for (int i = 0; i < requisicoes.size(); i++) {

            int paginaRequisitada = requisicoes.get(i);

            swapState.remove(paginaRequisitada);

            if (memoriaFisica.contains(paginaRequisitada)) {
                continue; // hit
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
                        // desempate
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

    private static int getDistancia(int page, List<Integer> requisicoes, int start) {
        for (int j = start; j < requisicoes.size(); j++) {
            if (requisicoes.get(j).equals(page))
                return j;
        }
        return Integer.MAX_VALUE;
    }

    private static String formatSwapState(Set<Integer> swapState) {
        return swapState.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

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

    static class DerivedParameters {
        int tamanhoPagina;
        int numFrames;
        long tamanhoSwapMinimo;
    }

    // =========================================================
    // MAIN 
    // =========================================================

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

    private static void imprimirSaida(DerivedParameters params, int N, List<List<Integer>> todasAsSequencias, int P) {
        System.out.println(params.tamanhoPagina);
        System.out.println(params.numFrames);
        System.out.println(params.tamanhoSwapMinimo);
        System.out.println(N);

        for (List<Integer> requisicoes : todasAsSequencias) {
            System.out.println(requisicoes.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" ")));

            SimulationResult resultadoFIFO = simularFIFO(params.numFrames, requisicoes, P);
            SimulationResult resultadoRAND = simularRAND(params.numFrames, requisicoes, P);
            SimulationResult resultadoLRU = simularLRU(params.numFrames, requisicoes, P);
            SimulationResult resultadoMIN = simularMIN(params.numFrames, requisicoes, P);

            System.out.println("FIFO");
            System.out.println(resultadoFIFO.tempoDecorrido);
            System.out.println(resultadoFIFO.pageFaults);
            System.out.println(resultadoFIFO.swapState);

            System.out.println("RAND");
            System.out.println(resultadoRAND.tempoDecorrido);
            System.out.println(resultadoRAND.pageFaults);
            System.out.println(resultadoRAND.swapState);

            System.out.println("LRU");
            System.out.println(resultadoLRU.tempoDecorrido);
            System.out.println(resultadoLRU.pageFaults);
            System.out.println(resultadoLRU.swapState);

            System.out.println("MIN");
            System.out.println(resultadoMIN.tempoDecorrido);
            System.out.println(resultadoMIN.pageFaults);
            System.out.println(resultadoMIN.swapState);
        }
    }
}
