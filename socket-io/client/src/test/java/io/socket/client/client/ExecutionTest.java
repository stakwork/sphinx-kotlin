package io.socket.client.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.socket.engine.emitter.Emitter;
import okhttp3.OkHttpClient;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ExecutionTest extends Connection {

    final static int TIMEOUT = 5 * 1000;

    @Test(timeout = TIMEOUT)
    public void execConnection() throws InterruptedException, IOException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);

        IO.Options options = new IO.Options();
        options.forceNew = true;

        final OkHttpClient client = new OkHttpClient();
        options.webSocketFactory = client;
        options.callFactory = client;

        final boolean[] wasConnect = {false};
        final boolean[] wasClosed = {false};
        final Socket socket = IO.socket("http://localhost:" + PORT, options);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasConnect[0] = true;
                socket.close();
            }
        });
        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasClosed[0] = true;
                client.dispatcher().executorService().shutdown();
            }
        });
        socket.open();

        while (!client.dispatcher().executorService().isShutdown()) {
            latch.await(50, TimeUnit.MILLISECONDS);
        }

        assertThat(wasConnect[0], is(true));
        assertThat(wasClosed[0], is(true));
    }

    @Test(timeout = TIMEOUT)
    public void execConnectionFailure() throws InterruptedException, IOException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);

        int port = PORT;
        port++;
        IO.Options options = new IO.Options();
        options.forceNew = true;
        options.reconnection = false;

        final OkHttpClient client = new OkHttpClient();
        options.webSocketFactory = client;
        options.callFactory = client;

        final boolean[] wasConnecting = {false};
        final boolean[] wasOpen = {false};
        final boolean[] wasTimeout = {false};
        final boolean[] wasError = {false};
        final boolean[] wasDisconnect = {false};
        final Socket socket = IO.socket("http://localhost:" + port, options);
        socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasConnecting[0] = true;
            }
        }).on(io.socket.engine.engineio.client.Socket.EVENT_OPEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasOpen[0] = true;
            }
        }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasTimeout[0] = true;
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasError[0] = true;
                client.dispatcher().executorService().shutdown();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasDisconnect[0] = true;
            }
        });
        socket.open();

        while (!client.dispatcher().executorService().isShutdown()) {
            latch.await(50, TimeUnit.MILLISECONDS);
        }

        // timeout, disconnect, & open should not occur due to error
        assertThat(wasTimeout[0], is(false));
        assertThat(wasDisconnect[0], is(false));
        assertThat(wasOpen[0], is(false));

        assertThat(wasConnecting[0], is(true));
        assertThat(wasError[0], is(true));
    }

    @Test(timeout = TIMEOUT)
    public void execImmediateClose() throws InterruptedException, IOException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);

        IO.Options options = new IO.Options();
        options.forceNew = true;

        final OkHttpClient client = new OkHttpClient();
        options.webSocketFactory = client;
        options.callFactory = client;

        final boolean[] wasConnecting = {false};
        final boolean[] wasConnect = {false};
        final boolean[] wasDisconnect = {false};
        final boolean[] wasClose = {false};
        final Socket socket = IO.socket("http://localhost:" + PORT, options);
        socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasConnecting[0] = true;
            }
        }).on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasConnect[0] = true;
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasDisconnect[0] = true;
            }
        }).on(io.socket.engine.engineio.client.Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasClose[0] = true;
                client.dispatcher().executorService().shutdown();
            }
        });
        socket.connect();
        socket.disconnect();

        while (!wasConnecting[0]) {
            latch.await(50, TimeUnit.MILLISECONDS);
        }

        // EVENT_CONNECTING should be the only event emitted
        assertThat(wasConnecting[0], is(true));

        assertThat(wasConnect[0], is(false));
        assertThat(wasDisconnect[0], is(false));
        assertThat(wasClose[0], is(false));
        assertThat(client.dispatcher().executorService().isShutdown(), is(false));
    }
}
