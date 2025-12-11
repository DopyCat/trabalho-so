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
}