package org.ifsp.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

public class VideoStream {

    private final FileInputStream videoFileStream;

    public VideoStream(String filename) throws FileNotFoundException {
        File videoFile = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile());
        this.videoFileStream = new FileInputStream(videoFile);
    }

    public int getNextFrame(byte[] frame) throws IOException {
        byte[] frameLength = new byte[5];
        //read current frame length
        this.videoFileStream.read(frameLength, 0, 5);

        //transform frame_length to integer
        String stringLength = new String(frameLength);
        int length = Integer.parseInt(stringLength);

        return this.videoFileStream.read(frame, 0, length);
    }
}