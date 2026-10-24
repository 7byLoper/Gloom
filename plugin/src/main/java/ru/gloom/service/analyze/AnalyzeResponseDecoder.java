package ru.gloom.service.analyze;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class AnalyzeResponseDecoder {
    private static final byte[] RESPONSE_MAGIC = {'G', 'A', 'I', 'B'};
    private static final int BATCH_COUNT_SIZE = 2;

    private AnalyzeResponseDecoder() {
    }

    static double[] decodeResponse(byte[] body, int expectedCount) {
        if (body == null || body.length < RESPONSE_MAGIC.length + BATCH_COUNT_SIZE) {
            throw new IllegalArgumentException("response is too short");
        }

        for (int i = 0; i < RESPONSE_MAGIC.length; i++) {
            if (body[i] != RESPONSE_MAGIC[i]) {
                throw new IllegalArgumentException("invalid response magic; expected GAIB");
            }
        }

        ByteBuffer buffer = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(RESPONSE_MAGIC.length);
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count != expectedCount) {
            throw new IllegalArgumentException("item count mismatch: expected " + expectedCount + ", got " + count);
        }

        if (buffer.remaining() < count * Double.BYTES) {
            throw new IllegalArgumentException("response body is truncated");
        }

        double[] probabilities = new double[count];
        for (int i = 0; i < count; i++) {
            double probability = buffer.getDouble();
            if (!Double.isNaN(probability) && (!Double.isFinite(probability) || probability < 0 || probability > 1)) {
                throw new IllegalArgumentException("probability is outside [0, 1]");
            }
            probabilities[i] = probability;
        }

        return probabilities;
    }
}
