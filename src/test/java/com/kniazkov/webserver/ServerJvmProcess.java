/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Starts a server in a separate JVM for lifecycle testing.
 */
public final class ServerJvmProcess {

    /**
     * The marker printed after the server has started.
     */
    static final String STARTED = "SERVER_STARTED";

    /**
     * Utility class.
     */
    private ServerJvmProcess() {
    }

    /**
     * Starts a server, returns from the main thread and stops the server from
     * a virtual thread after a short delay.
     *
     * @param arguments
     *     unused command-line arguments.
     * @throws ServerException
     *     if the server cannot be started.
     */
    public static void main(final String[] arguments)
        throws ServerException {

        final Server server = Server.start(
            new Options.Builder()
                .setPort(0)
                .build()
        );

        Thread.startVirtualThread(
            () -> {
                try {
                    Thread.sleep(1000);
                    server.stop();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    System.exit(2);
                } catch (ServerException exception) {
                    System.exit(3);
                }
            }
        );

        System.out.println(STARTED);
        System.out.flush();
    }
}
