package com.example.ac.desfirelearningtool;

import java.io.IOException;

/**
 * Base interface to abstract the card communication mechanism
 */
public interface ICardCommunicator {

    public byte[] transceive(byte[] data) throws IOException;

    public void connect() throws IOException;

    public boolean isConnected() throws IOException;

    public void close() throws IOException;
}
