package dev.truewinter.simofa.deploy;

import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader extends Thread {
    private final InputStream inputStream;
    private final LogType logType;
    private final StreamReaderCallback streamReaderCallback;

    public StreamReader(InputStream inputStream, LogType logType, StreamReaderCallback streamReaderCallback) {
        this.inputStream = inputStream;
        this.logType = logType;
        this.streamReaderCallback = streamReaderCallback;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line = "";
            while ((line = reader.readLine()) != null) {
                streamReaderCallback.onLog(new SimofaLog(logType, line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface StreamReaderCallback {
        void onLog(SimofaLog log);
    }
}
