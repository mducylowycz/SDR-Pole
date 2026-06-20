package app.sdrpole.core;

import app.sdrpole.plugin.DecoderPlugin;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class PluginRegistry {
    public List<DecoderPlugin> load(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) return List.of();
        List<URL> jars;
        try (var paths = Files.list(directory)) {
            jars = paths.filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .map(p -> {
                        try { return p.toUri().toURL(); }
                        catch (Exception e) { throw new IllegalArgumentException(e); }
                    }).toList();
        }
        var loader = new URLClassLoader(jars.toArray(URL[]::new), DecoderPlugin.class.getClassLoader());
        var plugins = new ArrayList<DecoderPlugin>();
        ServiceLoader.load(DecoderPlugin.class, loader).forEach(plugins::add);
        return List.copyOf(plugins);
    }
}
