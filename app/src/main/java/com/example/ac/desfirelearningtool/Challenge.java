package com.example.ac.desfirelearningtool;

public class Challenge {

    private final byte[] challenge;
    private final byte[] challengeResponse;

    public Challenge(byte[] challenge, byte[] challengeResponse) {
        this.challenge = challenge;
        this.challengeResponse = challengeResponse;
    }

    public byte[] getChallenge() {
        return challenge;
    }

    public byte[] getChallengeResponse() {
        return challengeResponse;
    }
}
