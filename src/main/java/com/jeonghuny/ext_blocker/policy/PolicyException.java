package com.jeonghuny.ext_blocker.policy;

public class PolicyException extends RuntimeException {

    private final PolicyError error;
    private final String target;

    public PolicyException(PolicyError error, String target) {
        super(error.message());
        this.error = error;
        this.target = target;
    }

    public PolicyError error() { return error; }
    public String target()     { return target; }
}