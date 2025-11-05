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
