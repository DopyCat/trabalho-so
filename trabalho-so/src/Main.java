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

    // =========================================================
    // CLASSES AUXILIARES
    // =========================================================

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
