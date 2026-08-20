/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Uploaded data backed by one shared temporary file.
 */
final class TemporaryFileUploadedData implements StoredUploadedData {

    /**
     * The shared temporary file.
     */
    private final Storage storage;

    /**
     * The first byte in this view.
     */
    private final long offset;

    /**
     * The number of bytes in this view.
     */
    private final long length;

    /**
     * Creates a complete temporary-file data object.
     *
     * @param storage
     *     the shared storage.
     * @param length
     *     the complete data length.
     */
    private TemporaryFileUploadedData(
        final Storage storage,
        final long length
    ) {
        this(storage, 0, length);
    }

    /**
     * Creates a view over temporary-file data.
     *
     * @param storage
     *     the shared storage.
     * @param offset
     *     the view offset.
     * @param length
     *     the view length.
     */
    private TemporaryFileUploadedData(
        final Storage storage,
        final long offset,
        final long length
    ) {
        this.storage = storage;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public long getSize() {
        return length;
    }

    @Override
    public InputStream openStream() throws ServerException {
        return storage.open(offset, length);
    }

    @Override
    public StoredUploadedData slice(
        final long value,
        final long size
    ) {
        validateRange(value, size);
        return new TemporaryFileUploadedData(
            storage,
            offset + value,
            size
        );
    }

    @Override
    public void close() throws ServerException {
        storage.close();
    }

    /**
     * Reads an exact number of bytes into a temporary file.
     *
     * @param source
     *     the source.
     * @param length
     *     the exact length.
     * @return
     *     the uploaded data.
     * @throws ServerException
     *     if the data cannot be stored.
     */
    static TemporaryFileUploadedData read(
        final ByteSource source,
        final long length
    ) throws ServerException {
        final Path path;

        try {
            path = Files.createTempFile(
                "foundry19-upload-",
                ".tmp"
            );
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot create temporary upload storage",
                exception
            );
        }

        final Storage storage = new Storage(path);

        try (
            OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(path)
            )
        ) {
            for (long index = 0; index < length; index++) {
                final int value = source.read();

                if (value == -1) {
                    throw new ServerException(
                        "Unexpected end of HTTP request body"
                    );
                }

                output.write(value);
            }

            return new TemporaryFileUploadedData(storage, length);
        } catch (IOException | ServerException exception) {
            try {
                storage.close();
            } catch (ServerException cleanup) {
                exception.addSuppressed(cleanup);
            }

            if (exception instanceof ServerException failure) {
                throw failure;
            }

            throw new ServerException(
                "Cannot store uploaded request data",
                exception
            );
        }
    }

    /**
     * Validates a requested view range.
     *
     * @param value
     *     the offset.
     * @param size
     *     the length.
     */
    private void validateRange(
        final long value,
        final long size
    ) {
        if (
            value < 0
                || size < 0
                || value > length
                || size > length - value
        ) {
            throw new IllegalArgumentException(
                "Uploaded data range is outside the request body"
            );
        }
    }

    /**
     * Shared ownership of a temporary file and its open streams.
     */
    private static final class Storage {

        /**
         * The temporary file.
         */
        private final Path path;

        /**
         * Streams opened for request consumers.
         */
        private final List<InputStream> streams = new ArrayList<>();

        /**
         * Whether this storage has been released.
         */
        private boolean closed;

        /**
         * Creates temporary-file storage.
         *
         * @param path
         *     the temporary file.
         */
        Storage(final Path path) {
            this.path = path;
        }

        /**
         * Opens a bounded view of the temporary file.
         *
         * @param offset
         *     the view offset.
         * @param length
         *     the view length.
         * @return
         *     the bounded stream.
         * @throws ServerException
         *     if the stream cannot be opened.
         */
        synchronized InputStream open(
            final long offset,
            final long length
        ) throws ServerException {
            if (closed) {
                throw new ServerException(
                    "Uploaded data is no longer available"
                );
            }

            InputStream input = null;

            try {
                input = Files.newInputStream(path);
                input.skipNBytes(offset);

                final InputStream bounded = new BoundedInputStream(
                    input,
                    length,
                    this
                );

                streams.add(bounded);
                return bounded;
            } catch (IOException exception) {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException cleanup) {
                        exception.addSuppressed(cleanup);
                    }
                }

                throw new ServerException(
                    "Cannot open uploaded data",
                    exception
                );
            }
        }

        /**
         * Stops tracking a stream closed by a consumer.
         *
         * @param stream
         *     the closed stream.
         */
        synchronized void release(final InputStream stream) {
            streams.remove(stream);
        }

        /**
         * Closes streams and deletes the temporary file.
         *
         * @throws ServerException
         *     if cleanup fails.
         */
        synchronized void close() throws ServerException {
            if (closed) {
                return;
            }

            closed = true;
            IOException failure = null;

            final List<InputStream> opened = List.copyOf(streams);
            streams.clear();

            for (InputStream stream : opened) {
                try {
                    stream.close();
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }

            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }

            if (failure != null) {
                throw new ServerException(
                    "Cannot release temporary upload storage",
                    failure
                );
            }
        }
    }

    /**
     * Input stream that exposes a fixed number of bytes.
     */
    private static final class BoundedInputStream
        extends FilterInputStream {

        /**
         * The number of bytes remaining.
         */
        private long remaining;

        /**
         * The shared storage tracking this stream.
         */
        private final Storage storage;

        /**
         * Whether the stream has been closed.
         */
        private boolean closed;

        /**
         * Creates a bounded stream.
         *
         * @param input
         *     the source stream.
         * @param remaining
         *     the maximum number of bytes.
         * @param storage
         *     the shared storage tracking the stream.
         */
        BoundedInputStream(
            final InputStream input,
            final long remaining,
            final Storage storage
        ) {
            super(input);
            this.remaining = remaining;
            this.storage = storage;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) {
                return -1;
            }

            final int result = super.read();

            if (result >= 0) {
                remaining--;
            }

            return result;
        }

        @Override
        public int read(
            final byte[] buffer,
            final int offset,
            final int length
        ) throws IOException {
            if (remaining == 0) {
                return -1;
            }

            final int requested = (int) Math.min(
                length,
                remaining
            );

            final int result = super.read(
                buffer,
                offset,
                requested
            );

            if (result > 0) {
                remaining -= result;
            }

            return result;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }

            closed = true;

            try {
                super.close();
            } finally {
                storage.release(this);
            }
        }
    }
}
