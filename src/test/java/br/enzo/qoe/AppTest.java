// Enzo de Moura Silva - RM556532

package br.com.enzo.qoe;

import static org.junit.jupiter.api.Assertions.*;
import static br.com.enzo.qoe.App.Rating.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class AppTest {

    private static long CUT;
    private static final double EPS = 1e-9;

    private List<Long> durations;

    @BeforeAll
    static void global() {
        CUT = 550L;
        assertTrue(CUT > 0);
    }

    @BeforeEach
    void setup() {
        durations = new ArrayList<>();
    }

    @Nested
    @DisplayName("Cálculo a partir de contagens (exemplos diretos)")
    class CountBased {

        @Test
        void exemploBasico() {
            long sat = 310, tol = 60, fru = 30;
            double apdex = App.score(sat, tol, fru);
            assertEquals(0.85d, apdex, EPS);
            assertEquals(BOM, App.labelOf(apdex));
        }

        @Test
        void totalIgualAoRM_556532() {
            long total = 556_532L;
            long sat   = 380_000L;
            long tol   = 120_000L;
            long fru   = total - (sat + tol);
            assertEquals(total, sat + tol + fru);

            double apdex = App.score(sat, tol, fru);
            assertTrue(apdex > 0.79 && apdex < 0.792, "apdex esperado ~0.7906");
            assertEquals(RAZOAVEL, App.labelOf(apdex));
        }
    }

    @Nested
    @DisplayName("Cálculo a partir de tempos (splitByCutoff + score)")
    class DurationBased {

        @Test
        void splitECalculo() {
            long T = CUT;
            durations.addAll(List.of(
                20L, 300L, 550L,
                T + 1, 2*T, 4*T,
                4*T + 10, 9000L
            ));
            App.SampleBuckets buckets = App.splitByCutoff(durations, T);
            assertEquals(3, buckets.satisfied());
            assertEquals(3, buckets.tolerable());
            assertEquals(2, buckets.frustrated());

            double apdex = App.score(buckets);
            assertEquals(0.5625d, apdex, EPS);
            assertEquals(RUIM, App.labelOf(apdex));
        }
    }

    @Nested
    @DisplayName("Fronteiras de classificação")
    class Boundaries {

        static Stream<Arguments> boundaryCases() {
            return Stream.of(
                Arguments.of(0.94d, EXCELENTE),
                Arguments.of(1.00d, EXCELENTE),
                Arguments.of(0.93d, BOM),
                Arguments.of(0.85d, BOM),
                Arguments.of(0.84d, RAZOAVEL),
                Arguments.of(0.70d, RAZOAVEL),
                Arguments.of(0.69d, RUIM),
                Arguments.of(0.50d, RUIM),
                Arguments.of(0.49d, INACEITAVEL),
                Arguments.of(0.00d, INACEITAVEL)
            );
        }

        @ParameterizedTest
        @MethodSource("boundaryCases")
        void classificaNasFronteiras(double value, App.Rating expected) {
            assertEquals(expected, App.labelOf(value));
        }
    }

    @Nested
    @DisplayName("Validações de entrada")
    class Validation {

        @Test
        void totalZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> App.score(0, 0, 0));
        }

        @Test
        void contagensNegativasLancamExcecao() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> App.score(-1, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> App.score(0, -1, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> App.score(0, 0, -1))
            );
        }

        @Test
        void labelComValorInvalidoLancaExcecao() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> App.labelOf(-0.01)),
                () -> assertThrows(IllegalArgumentException.class, () -> App.labelOf(1.01)),
                () -> assertThrows(IllegalArgumentException.class, () -> App.labelOf(Double.NaN))
            );
        }

        @Test
        void splitComTemposInvalidosLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> App.splitByCutoff(List.of(10L, -1L), CUT));
            assertThrows(IllegalArgumentException.class,
                () -> App.splitByCutoff(java.util.Arrays.asList(10L, (Long) null), CUT));
            assertThrows(IllegalArgumentException.class, () -> App.splitByCutoff(List.of(), 0L));
        }
    }
}
