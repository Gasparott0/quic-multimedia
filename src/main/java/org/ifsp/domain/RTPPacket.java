package org.ifsp.domain;

import java.util.Arrays;

public class RTPPacket {

    private static final int RTP_HEADER_SIZE = 12;

    private int payloadTypeRtpHeader;
    private int sequenceNumberRtpHeader;
    private int timeStampRtpHeader;
    private byte[] bitStreamRtpHeader;
    private int payloadSizeRtpHeader;
    private byte[] payloadRtpHeader;

    public RTPPacket(int payloadTypeRtpHeader, int frameNumber, int timeStampRtpHeader, byte[] rtpData, int rtpDataLength) {
        this.sequenceNumberRtpHeader = frameNumber;
        this.timeStampRtpHeader = timeStampRtpHeader;
        this.payloadTypeRtpHeader = payloadTypeRtpHeader;

        this.bitStreamRtpHeader = new byte[RTP_HEADER_SIZE];

        this.bitStreamRtpHeader[0] = 2;
        this.bitStreamRtpHeader[1] = 52;
        this.bitStreamRtpHeader[2] = (byte) this.sequenceNumberRtpHeader;

        if (this.sequenceNumberRtpHeader > 127) {
            this.bitStreamRtpHeader[3] = 1;
        } else {
            this.bitStreamRtpHeader[3] = 0;
        }

        this.bitStreamRtpHeader[4] = (byte) timeStampRtpHeader;
        this.bitStreamRtpHeader[5] = 0;
        this.bitStreamRtpHeader[6] = 0;
        this.bitStreamRtpHeader[7] = 0;
        this.bitStreamRtpHeader[8] = 0;
        this.bitStreamRtpHeader[9] = 0;
        this.bitStreamRtpHeader[10] = 0;
        this.bitStreamRtpHeader[11] = 0;

        this.payloadSizeRtpHeader = rtpDataLength;
        this.payloadRtpHeader = new byte[rtpDataLength];

        this.payloadRtpHeader = Arrays.copyOf(rtpData, rtpData.length);
    }

    public RTPPacket(byte[] packet, int packetSize) {
        if (packetSize >= RTP_HEADER_SIZE) {
            this.bitStreamRtpHeader = new byte[RTP_HEADER_SIZE];
            this.bitStreamRtpHeader = Arrays.copyOf(packet, packetSize);

            this.payloadSizeRtpHeader = packetSize - RTP_HEADER_SIZE;
            this.payloadRtpHeader = new byte[this.payloadSizeRtpHeader];
            for (int i = RTP_HEADER_SIZE; i < packetSize; i++)
                this.payloadRtpHeader[i - RTP_HEADER_SIZE] = packet[i];

            this.payloadTypeRtpHeader = this.bitStreamRtpHeader[1] & 127;
            this.sequenceNumberRtpHeader = unsignedInt(this.bitStreamRtpHeader[3]) + 256 * unsignedInt(this.bitStreamRtpHeader[2]);
            this.timeStampRtpHeader = unsignedInt(this.bitStreamRtpHeader[7]) + 256 * unsignedInt(this.bitStreamRtpHeader[6]) + 65536 * unsignedInt(this.bitStreamRtpHeader[5]) + 16777216 * unsignedInt(this.bitStreamRtpHeader[4]);
        }
    }

    public int getPayload(byte[] packet) {

        for (int i = 0; i < this.payloadSizeRtpHeader; i++)
            packet[i] = this.payloadRtpHeader[i];

        return this.payloadSizeRtpHeader;
    }

    public int getPayloadLength() {
        return this.payloadSizeRtpHeader;
    }

    public int getLength() {
        return this.payloadSizeRtpHeader + RTP_HEADER_SIZE;
    }

    public int getPacket(byte[] packet) {
        //construct the packet = header + payload
        for (int i = 0; i < RTP_HEADER_SIZE; i++)
            packet[i] = this.bitStreamRtpHeader[i];
        for (int i = 0; i < this.payloadSizeRtpHeader; i++)
            packet[i + RTP_HEADER_SIZE] = this.payloadRtpHeader[i];

        return this.payloadSizeRtpHeader + RTP_HEADER_SIZE;
    }

    public int getTimeStamp() {
        return this.timeStampRtpHeader;
    }

    public int getSequenceNumber() {
        return this.sequenceNumberRtpHeader;
    }

    public int getPayloadType() {
        return this.payloadTypeRtpHeader;
    }

    public void printHeader() {
        for (int i = 0; i < (RTP_HEADER_SIZE - 4); i++) {
            for (int j = 7; j >= 0; j--)
                if (((1 << j) & (this.bitStreamRtpHeader[i] & 0xff)) != 0)
                    System.out.print("1");
                else
                    System.out.print("0");
            System.out.print(" ");
        }
        System.out.println();
    }

    public static int unsignedInt(int nb) {
        return nb >= 0 ? nb : 256 + nb;
    }

}