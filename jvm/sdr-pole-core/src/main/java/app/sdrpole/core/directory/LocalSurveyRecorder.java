package app.sdrpole.core.directory;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.SignalObservation;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/** Converts only stable detector results into local evidence; identification remains deliberately conservative. */
public final class LocalSurveyRecorder {
    private final FrequencyDatabase database;

    public LocalSurveyRecorder(FrequencyDatabase database) { this.database = database; }

    public void record(SignalObservation signal, GeoPoint location) throws SQLException {
        var roundedHz = Math.round(signal.frequencyHz() / 100.0) * 100;
        var area = location == null ? "unknown" : String.format("%.2f,%.2f", location.latitude(), location.longitude());
        var id = roundedHz + "@" + area;
        database.upsert(List.of(new FrequencyChannel("local-survey", id,
                "Observed signal " + String.format("%.5f MHz", roundedHz / 1e6), roundedHz,
                signal.hint().mode(), signal.hint().label() + " candidate; frame confirmation may still be required",
                "Local", location, location == null ? 0 : 2.0, FrequencyChannel.Confidence.MEASURED, Instant.now())));
    }
}
