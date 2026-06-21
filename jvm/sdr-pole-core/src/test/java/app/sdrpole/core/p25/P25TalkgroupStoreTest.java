package app.sdrpole.core.p25;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class P25TalkgroupStoreTest {
    @TempDir Path temp;

    @Test void persistsOfflineDirectoryLabels() throws Exception {
        var store = new P25TalkgroupStore(temp.resolve("talkgroups.json"));
        var expected = List.of(new P25Talkgroup("County", 101, "Fire Dispatch", "Fire", "D", 0));
        store.save(expected);
        assertEquals(expected, store.load());
    }
}
