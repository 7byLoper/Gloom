package ru.gloom.service.analyze;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeBatchDispatcherTest {
    @Test
    void unscoredWindowStaysSeparateFromLegitProbability() {
        byte[] response = response(0.0, Double.NaN, 0.97);
        double[] probabilities = AnalyzeBatchDispatcher.decodeResponse(response, 3);
        assertEquals(0.0, probabilities[0]);
        assertTrue(Double.isNaN(probabilities[1]));
        assertEquals(0.97, probabilities[2]);
    }

    @Test
    void malformedProbabilityCannotBecomeAFlag() {
        for (double value : new double[]{-0.1, 1.1, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> AnalyzeBatchDispatcher.decodeResponse(response(value), 1));
        }
        assertThrows(IllegalArgumentException.class,
                () -> AnalyzeBatchDispatcher.decodeResponse(response(0.5), 2));
    }

    private static byte[] response(double... probabilities) {
        ByteBuffer buffer = ByteBuffer.allocate(6 + probabilities.length * Double.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'G', 'A', 'I', 'B'}).putShort((short) probabilities.length);
        for (double probability : probabilities) {
            buffer.putDouble(probability);
        }
        return buffer.array();
    }
}
