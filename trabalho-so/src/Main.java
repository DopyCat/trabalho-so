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

}