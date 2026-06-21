package app.sdrpole.core.directory;

import app.sdrpole.core.DecoderHint;
import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.SignalObservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSurveyRecorderTest {
    @TempDir Path temporary;

    @Test void storesStableObservationAsMeasuredNotIdentified() throws Exception {
        var database = new FrequencyDatabase(temporary.resolve("survey.sqlite3"));
        database.initialize();
        var location = new GeoPoint(41.88, -87.63);
        new LocalSurveyRecorder(database).record(new SignalObservation(155_250_041, -30, -55, 12_500,
                new DecoderHint("Narrow FM", DemodulationMode.NFM, .62, false)), location);
        assertTrue(database.channelsNear(location, 5, 50).stream().anyMatch(channel ->
                channel.sourceId().equals("local-survey") && channel.confidence() == FrequencyChannel.Confidence.MEASURED));
    }
}
