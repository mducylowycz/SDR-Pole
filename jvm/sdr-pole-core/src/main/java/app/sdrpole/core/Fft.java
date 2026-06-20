package app.sdrpole.core;

final class Fft {
    private Fft() {}

    static float[] powerDb(float[] interleavedIq, int complexCount) {
        int n = Integer.highestOneBit(Math.min(complexCount, 1024));
        var re = new double[n];
        var im = new double[n];
        for (int i = 0; i < n; i++) {
            var window = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1));
            re[i] = interleavedIq[i * 2] * window;
            im[i] = interleavedIq[i * 2 + 1] * window;
        }
        transform(re, im);
        var out = new float[n];
        for (int i = 0; i < n; i++) {
            int shifted = (i + n / 2) % n;
            var power = re[shifted] * re[shifted] + im[shifted] * im[shifted];
            out[i] = (float) Math.max(-130, 10 * Math.log10(power / (n * n) + 1e-13));
        }
        return out;
    }

    private static void transform(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                var tr = re[i]; re[i] = re[j]; re[j] = tr;
                var ti = im[i]; im[i] = im[j]; im[j] = ti;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            var angle = -2 * Math.PI / len;
            var wLenRe = Math.cos(angle);
            var wLenIm = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                var wRe = 1.0;
                var wIm = 0.0;
                for (int j = 0; j < len / 2; j++) {
                    int u = i + j, v = u + len / 2;
                    var vRe = re[v] * wRe - im[v] * wIm;
                    var vIm = re[v] * wIm + im[v] * wRe;
                    re[v] = re[u] - vRe; im[v] = im[u] - vIm;
                    re[u] += vRe; im[u] += vIm;
                    var next = wRe * wLenRe - wIm * wLenIm;
                    wIm = wRe * wLenIm + wIm * wLenRe;
                    wRe = next;
                }
            }
        }
    }
}
