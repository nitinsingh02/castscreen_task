package com.example.celebrareproject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD;

public class ScreenStreamServer extends NanoHTTPD {

    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();

    public ScreenStreamServer(int port) {
        super(port);
    }

    public void setLatestFrame(byte[] frame) {
        latestFrame.set(frame);
    }

    @Override
    public Response serve(IHTTPSession session) {

        try {
            // pipe allows writing to an InputStream
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);

            // start streaming thread
            new Thread(() -> startStream(pos)).start();

            Response response = newChunkedResponse(
                    Response.Status.OK,
                    "multipart/x-mixed-replace; boundary=--frame",
                    pis
            );

            response.addHeader("Connection", "close");
            response.addHeader("Cache-Control", "no-cache");

            return response;

        } catch (IOException e) {
            return newFixedLengthResponse("Stream error: " + e.getMessage());
        }
    }

    private void startStream(OutputStream outputStream) {
        try {
            while (true) {

                byte[] frame = latestFrame.get();

                if (frame != null) {
                    outputStream.write(("--frame\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: " + frame.length + "\r\n\r\n")
                            .getBytes());

                    outputStream.write(frame);
                    outputStream.write("\r\n\r\n".getBytes());
                    outputStream.flush();
                }

                Thread.sleep(50); // approx 20 FPS
            }
        } catch (Exception ignored) {
            // Client disconnected
        } finally {
            try {
                outputStream.close();
            } catch (Exception ignored) {}
        }
    }
}


/*
package com.example.celebrareproject;

import java.io.ByteArrayInputStream;

import fi.iki.elonen.NanoHTTPD;

public class ScreenStreamServer extends NanoHTTPD {
    private byte[] latestFrame;

    public ScreenStreamServer(int port) {
        super(port);
    }

    public void setLatestFrame(byte[] frame) {
        this.latestFrame = frame;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (latestFrame != null) {
            return newFixedLengthResponse(Response.Status.OK, "image/jpeg",
                    new ByteArrayInputStream(latestFrame), latestFrame.length);
        } else {
            return newFixedLengthResponse("Waiting for screen...");
        }
    }
}
*/
