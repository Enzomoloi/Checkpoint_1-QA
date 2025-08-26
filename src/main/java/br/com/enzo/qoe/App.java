// Enzo de Moura Silva - RM556532

package br.com.enzo.qoe;

import java.util.List;
import java.util.Objects;


public final class App {

    private App() {}

    public enum Rating { EXCELENTE, BOM, RAZOAVEL, RUIM, INACEITAVEL }

    public static final class SampleBuckets {
        private final long satisfied;
        private final long tolerable;
        private final long frustrated;
        public SampleBuckets(long satisfied, long tolerable, long frustrated) {
            if (satisfied < 0 || tolerable < 0 || frustrated < 0) {
                throw new IllegalArgumentException("Contagens não podem ser negativas.");
            }
            this.satisfied = satisfied;
            this.tolerable = tolerable;
            this.frustrated = frustrated;
        }
        public long satisfied()  { return satisfied; }
        public long tolerable()  { return tolerable; }
        public long frustrated() { return frustrated; }
        public long total()      { return satisfied + tolerable + frustrated; }
    }

    public static double score(long satisfied, long tolerable, long frustrated) {
        if (satisfied < 0 || tolerable < 0 || frustrated < 0) {
            throw new IllegalArgumentException("Contagens não podem ser negativas.");
        }
        long total = satisfied + tolerable + frustrated;
        if (total == 0) throw new IllegalArgumentException("Total de amostras não pode ser zero.");
        double v = (satisfied + 0.5d * tolerable) / (double) total;
        return Math.max(0.0d, Math.min(1.0d, v));
    }

    public static double score(SampleBuckets buckets) {
        Objects.requireNonNull(buckets, "Buckets não pode ser nulo.");
        return score(buckets.satisfied(), buckets.tolerable(), buckets.frustrated());
    }

    public static Rating labelOf(double apdex) {
        if (Double.isNaN(apdex) || apdex < 0.0d || apdex > 1.0d) {
            throw new IllegalArgumentException("APDEX deve estar no intervalo [0.00, 1.00].");
        }
        if (apdex >= 0.94d) return Rating.EXCELENTE;
        if (apdex >= 0.85d) return Rating.BOM;
        if (apdex >= 0.70d) return Rating.RAZOAVEL;
        if (apdex >= 0.50d) return Rating.RUIM;
        return Rating.INACEITAVEL;
    }

    public static SampleBuckets splitByCutoff(List<Long> durationsMs, long T) {
        Objects.requireNonNull(durationsMs, "Lista de tempos não pode ser nula.");
        if (T <= 0) throw new IllegalArgumentException("T deve ser positivo.");

        long sat = 0, tol = 0, fru = 0;
        for (Long d : durationsMs) {
            if (d == null || d < 0) throw new IllegalArgumentException("Tempo inválido (nulo/negativo).");
            if (d <= T) sat++;
            else if (d <= 4L * T) tol++;
            else fru++;
        }
        return new SampleBuckets(sat, tol, fru);
    }

    public static double scoreOfDurations(List<Long> durationsMs, long T) {
        return score(splitByCutoff(durationsMs, T));
    }
}
