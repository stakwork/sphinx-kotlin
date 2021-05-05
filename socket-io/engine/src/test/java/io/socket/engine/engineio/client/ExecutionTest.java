package io.socket.engine.engineio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        final boolean[] wasOpened = {false};
        final boolean[] wasClosed = {false};
        final Socket socket = new Socket("http://localhost:" + PORT, opts);
        socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasOpened[0] = true;
                socket.close();
            }
        });
        socket.on(Socket.EVENT_CLOSE, new Emitter.Listener() {
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

        assertThat(wasOpened[0], is(true));
        assertThat(wasClosed[0], is(true));
        assertThat(client.dispatcher().executorService().isShutdown(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void execConnectionFailure() throws InterruptedException, IOException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);

        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        int port = PORT;
        port++;
        final boolean[] wasOpened = {false};
        final boolean[] wasClosed = {false};
        final boolean[] wasError = {false};
        final Socket socket = new Socket("http://localhost:" + port, opts);
        socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasOpened[0] = true;
            }
        });
        socket.on(Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasClosed[0] = true;
            }
        }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasError[0] = true;
                client.dispatcher().executorService().shutdown();
            }
        });
        socket.open();

        while (!client.dispatcher().executorService().isShutdown()) {
            latch.await(50, TimeUnit.MILLISECONDS);
        }

        // open should not be triggered due to incorrect port
        assertThat(wasOpened[0], is(false));

        assertThat(wasClosed[0], is(true));
        assertThat(client.dispatcher().executorService().isShutdown(), is(true));
        assertThat(wasError[0], is(true));
    }

    @Test(timeout = TIMEOUT)
    public void execImmediateClose() throws InterruptedException, IOException, URISyntaxException {
        final CountDownLatch latch = new CountDownLatch(1);

        final OkHttpClient client = new OkHttpClient();
        Socket.Options opts = new Socket.Options();
        opts.webSocketFactory = client;
        opts.callFactory = client;

        final boolean[] wasOpened = {false};
        final boolean[] wasClosed = {false};
        final Socket socket = new Socket("http://localhost:" + PORT, opts);
        socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasOpened[0] = true;
            }
        }).on(Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                wasClosed[0] = true;
                client.dispatcher().executorService().shutdown();
            }
        });
        assertThat(client.dispatcher().executorService().isShutdown(), is(false));

        socket.open();
        socket.close();

        while (!client.dispatcher().executorService().isShutdown()) {
            latch.await(50, TimeUnit.MILLISECONDS);
        }

        // open should not be triggered due to immediate close
        assertThat(wasOpened[0], is(false));

        assertThat(wasClosed[0], is(true));
        assertThat(client.dispatcher().executorService().isShutdown(), is(true));
    }
}
