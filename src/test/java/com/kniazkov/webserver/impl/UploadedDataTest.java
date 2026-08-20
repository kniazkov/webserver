/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests memory and temporary-file uploaded data implementations.
 */
final class UploadedDataTest {

    /**
     * Tests storing a small body in memory and opening range views.
     */
    @Test
    void memoryData() throws Exception {
        final byte[] source = bytes("0123456789");
        final StoredUploadedData data = UploadedDataReader.read(
            new ByteArrayByteSource(source),
            source.length,
            source.length
        );

        assertInstanceOf(MemoryUploadedData.class, data);
        assertEquals(source.length, data.getSize());
        assertArrayEquals(source, data.readAllBytes());
        assertArrayEquals(
            bytes("3456"),
            data.slice(3, 4).readAllBytes()
        );

        final ByteArrayOutputStream output =
            new ByteArrayOutputStream();

        assertEquals(source.length, data.transferTo(output));
        assertArrayEquals(source, output.toByteArray());
    }

    /**
     * Tests storing a large body in a temporary file and releasing it.
     */
    @Test
    void temporaryFileData() throws Exception {
        final byte[] source = bytes("large request body");
        final StoredUploadedData data = UploadedDataReader.read(
            new ByteArrayByteSource(source),
            source.length,
            0
        );

        assertInstanceOf(TemporaryFileUploadedData.class, data);

        final StoredUploadedData slice = data.slice(6, 7);

        assertArrayEquals(source, data.readAllBytes());
        assertArrayEquals(bytes("request"), slice.readAllBytes());

        data.close();

        assertThrows(ServerException.class, slice::openStream);
    }

    /**
     * Tests rejection of ranges outside the stored request body.
     */
    @Test
    void invalidSlice() {
        final StoredUploadedData data = new MemoryUploadedData(
            bytes("data")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> data.slice(3, 2)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> data.slice(-1, 1)
        );
    }

    /**
     * Tests that concurrent consumers can open independent streams over one
     * temporary upload.
     */
    @Test
    void concurrentTemporaryFileReaders() throws Exception {
        final byte[] source = bytes(
            "one temporary upload shared by concurrent handlers"
        );
        final StoredUploadedData data = UploadedDataReader.read(
            new ByteArrayByteSource(source),
            source.length,
            0
        );
        final List<Callable<byte[]>> readers = new ArrayList<>();

        for (int index = 0; index < 32; index++) {
            readers.add(data::readAllBytes);
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var future : executor.invokeAll(readers)) {
                assertArrayEquals(source, future.get());
            }
        } finally {
            data.close();
        }
    }

    /**
     * Converts text to bytes.
     *
     * @param value
     *     the text.
     * @return
     *     the bytes.
     */
    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
