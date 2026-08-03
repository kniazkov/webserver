/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Creates an object from mutable, incrementally collected state.
 *
 * @param <T> type of object created by this builder
 */
public interface Builder<T> {
    /**
     * Creates an object when this builder is valid.
     *
     * @return a newly created object, never {@code null}
     * @throws IllegalStateException if this builder is invalid
     */
    T create();

    /**
     * Determines whether {@link #create()} can create an object.
     *
     * @return {@code true} when this builder contains valid state
     */
    boolean isValid();
}
