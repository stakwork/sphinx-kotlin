package io.socket.client.client;

import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    final static int TIMEOUT = 7000;
    final static int PORT = 3000;

    private Process serverProcess;
    private ExecutorService serverService;
    private Future serverOutput;
    private Future serverError;

    @Before
    public void startServer() throws IOException, InterruptedException {
        logger.fine("Starting server ...");

        final CountDownLatch latch = new CountDownLatch(1);

        File nodeDir = new File(System.getProperty("user.dir") + "/../..", ".gradle/nodejs");
        File[] files = nodeDir.listFiles();
        File nodeVersionDir = null;
        for (File file : files) {
            if (file.isDirectory() && file.getName().contains("node-v")) {
                nodeVersionDir = file;
                break;
            }
        }

        if (nodeVersionDir == null) {
            throw new RuntimeException(
                    "Could not find node dir in sphinx-kotlin/.gradle/nodejs. " +
                            "Please run ./gradlew :socket-io:client:npmInstall"
            );
        }

        String nodeExePath = new File(nodeVersionDir, "/bin/node").getPath();

        serverProcess = Runtime.getRuntime().exec(
                nodeExePath + " src/test/resources/server.js", createEnv());
        serverService = Executors.newCachedThreadPool();
        serverOutput = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()));
                String line;
                try {
                    line = reader.readLine();
                    latch.countDown();
                    do {
                        logger.fine("SERVER OUT: " + line);
                    } while ((line = reader.readLine()) != null);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
        serverError = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getErrorStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        logger.fine("SERVER ERR: " + line);
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
        latch.await(3000, TimeUnit.MILLISECONDS);
    }

    @After
    public void stopServer() throws InterruptedException {
        logger.fine("Stopping server ...");
        serverProcess.destroy();
        serverOutput.cancel(false);
        serverError.cancel(false);
        serverService.shutdown();
        serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }

    Socket client() throws URISyntaxException {
        return client(createOptions());
    }

    Socket client(String path) throws URISyntaxException {
        return client(path, createOptions());
    }

    Socket client(IO.Options opts) throws URISyntaxException {
        return client(nsp(), opts);
    }

    Socket client(String path, IO.Options opts) throws URISyntaxException {
        return IO.socket(uri() + path, opts);
    }

    String uri() {
        return "http://localhost:" + PORT;
    }

    String nsp() {
        return "/";
    }

    IO.Options createOptions() {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        return opts;
    }

    String[] createEnv() {
        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("DEBUG", "socket.io:*");
        env.put("PORT", String.valueOf(PORT));
        String[] _env = new String[env.size()];
        int i = 0;
        for (String key : env.keySet()) {
            _env[i] = key + "=" + env.get(key);
            i++;
        }
        return _env;

    }
}
