/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Provides repeatable access to uploaded request data.
 * <p>
 * The data may be held in memory or in temporary storage. Instances supplied
 * through a {@link Request} remain valid only while its handler is running.
 */
public interface UploadedData {

    /**
     * Returns the number of uploaded bytes.
     *
     * @return
     *     the data size.
     */
    long getSize();

    /**
     * Opens a new stream containing the uploaded data.
     *
     * @return
     *     the stream.
     * @throws ServerException
     *     if the data cannot be opened.
     */
    InputStream openStream() throws ServerException;

    /**
     * Copies the uploaded data to an output stream.
     *
     * @param output
     *     the destination stream.
     * @return
     *     the number of copied bytes.
     * @throws ServerException
     *     if the data cannot be read or written.
     */
    default long transferTo(final OutputStream output)
        throws ServerException {
        Objects.requireNonNull(output, "Output must not be null");

        try (InputStream input = openStream()) {
            return input.transferTo(output);
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot transfer uploaded data",
                exception
            );
        }
    }

    /**
     * Reads all uploaded data into memory.
     * <p>
     * This method deliberately allocates an array containing the entire data.
     * Prefer {@link #openStream()} or {@link #transferTo(OutputStream)} for
     * large uploads.
     *
     * @return
     *     the uploaded bytes.
     * @throws ServerException
     *     if the data cannot be read or is too large for a byte array.
     */
    default byte[] readAllBytes() throws ServerException {
        if (getSize() > Integer.MAX_VALUE) {
            throw new ServerException(
                "Uploaded data is too large for a byte array"
            );
        }

        try (InputStream input = openStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot read uploaded data",
                exception
            );
        }
    }
}
