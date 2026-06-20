package app.sdrpole.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

interface SoapyNative extends Library {
    SoapyNative INSTANCE = Loader.load();

    Pointer SoapySDRDevice_makeStrArgs(String args);
    int SoapySDRDevice_unmake(Pointer device);
    String SoapySDRDevice_lastError();
    int SoapySDRDevice_setSampleRate(Pointer device, int direction, long channel, double rate);
    int SoapySDRDevice_setFrequency(Pointer device, int direction, long channel, double frequency, Pointer args);
    int SoapySDRDevice_setGainMode(Pointer device, int direction, long channel, boolean automatic);
    int SoapySDRDevice_setGain(Pointer device, int direction, long channel, double gain);
    int SoapySDRDevice_setGainElement(Pointer device, int direction, long channel, String name, double gain);
    void SoapySDRDevice_writeSetting(Pointer device, String key, String value);
    Pointer SoapySDRDevice_setupStream(Pointer device, int direction, String format,
                                      Pointer channels, long numChannels, Pointer args);
    int SoapySDRDevice_activateStream(Pointer device, Pointer stream, int flags, long timeNs, long numElems);
    int SoapySDRDevice_deactivateStream(Pointer device, Pointer stream, int flags, long timeNs);
    int SoapySDRDevice_closeStream(Pointer device, Pointer stream);
    long SoapySDRDevice_getStreamMTU(Pointer device, Pointer stream);
    int SoapySDRDevice_readStream(Pointer device, Pointer stream, Pointer[] buffers,
                                  long numElems, IntByReference flags,
                                  LongByReference timeNs, long timeoutUs);

    final class Loader {
        private Loader() {}
        static SoapyNative load() {
            NativeLibrary.addSearchPath("SoapySDR", "/usr/local/lib");
            NativeLibrary.addSearchPath("SoapySDR", "/opt/homebrew/lib");
            return Native.load("SoapySDR", SoapyNative.class);
        }
    }
}
