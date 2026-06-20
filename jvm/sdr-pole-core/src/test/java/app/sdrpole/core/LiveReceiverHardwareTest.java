package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LiveReceiverHardwareTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "SDR_POLE_HARDWARE_TEST", matches = "true")
    void receivesIqAndProducesSpectrumFromConnectedRadio() throws Exception {
        var device = new DeviceDiscoveryService().discover().getFirst();
        var spectrum = new CountDownLatch(1);
        var failure = new AtomicReference<String>();
        try (var receiver = new LiveNfmReceiver(
                new ReceiverConfig(device, 155_250_000, 2_000_000, 24, false, 48_000),
                new ReceiverListener() {
                    @Override public void onSpectrum(float[] bins) {
                        if (bins.length >= 512) spectrum.countDown();
                    }
                    @Override public void onError(String friendlyMessage, Throwable cause) {
                        failure.set(friendlyMessage);
                        spectrum.countDown();
                    }
                })) {
            receiver.start();
            assertTrue(spectrum.await(8, TimeUnit.SECONDS), "No IQ spectrum arrived");
            assertNull(failure.get(), failure.get());
        }
    }
}
