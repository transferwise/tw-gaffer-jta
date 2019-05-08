package com.transferwise.common.gaffer.test.suspended.app;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component("idGenerator")
public class IdGenerator {
    private final AtomicInteger seq = new AtomicInteger();

    public int next() {
        return seq.addAndGet(1);
    }
}
