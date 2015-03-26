package com.bedatadriven.appengine.metrics;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.concurrent.ThreadLocalRandom;


public class ThreadLocalRandomGenerator implements RandomGenerator {
    
    public static final ThreadLocalRandomGenerator INSTANCE = new ThreadLocalRandomGenerator();
    
    @Override
    public void setSeed(int seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSeed(int[] seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nextBytes(byte[] bytes) {
        ThreadLocalRandom.current().nextBytes(bytes);
    }

    @Override
    public int nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Override
    public int nextInt(int n) {
        return ThreadLocalRandom.current().nextInt(n);
    }

    @Override
    public long nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();

    }

    @Override
    public float nextFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public double nextGaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }
}
