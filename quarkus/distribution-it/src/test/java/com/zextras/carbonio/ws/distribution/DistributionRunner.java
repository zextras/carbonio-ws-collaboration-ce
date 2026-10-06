// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.distribution;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Drives a copy of the app-ce quarkus-app directory as an operator would. */
final class DistributionRunner implements AutoCloseable {

  private final Path dist;
  private final Path root;
  private final Path log;
  private Process process;

  private DistributionRunner(Path root, Path dist) {
    this.root = root;
    this.dist = dist;
    this.log = root.resolve("run.log");
  }

  /**
   * Copies the distribution to {@code root/quarkus-app}. The directory name matters: re-augmentation
   * derives its output directory (and the user-providers directory it checks) as {@code
   * <parent>/quarkus-app}. Logs live in {@code root}, outside the directory that gets rebuilt.
   */
  static DistributionRunner freshCopy(Path source, Path root) {
    Path target = root.resolve("quarkus-app");
    try {
      Files.createDirectories(root);
      if (Files.exists(root)) {
        try (Stream<Path> walk = Files.walk(root)) {
          walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
      }
      try (Stream<Path> walk = Files.walk(source)) {
        for (Path p : (Iterable<Path>) walk::iterator) {
          Path dest = target.resolve(source.relativize(p).toString());
          if (Files.isDirectory(p)) {
            Files.createDirectories(dest);
          } else {
            Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
      Files.createDirectories(target.resolve("plugins"));
      return new DistributionRunner(root, target);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void addPlugin(Path jar) throws IOException {
    Files.copy(jar, dist.resolve("plugins").resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
  }

  /** Runs the re-augmentation and returns how long it took. */
  Duration reaugment() throws Exception {
    long start = System.nanoTime();
    Process rebuild = java(List.of("-Dquarkus.launch.rebuild=true"), root.resolve("rebuild.log"));
    try {
      if (!rebuild.waitFor(5, TimeUnit.MINUTES)) {
        throw new IllegalStateException(
            "Re-augmentation timed out:\n" + Files.readString(root.resolve("rebuild.log")));
      }
      if (rebuild.exitValue() != 0) {
        throw new IllegalStateException("Re-augmentation failed:\n" + Files.readString(root.resolve("rebuild.log")));
      }
    } finally {
      terminate(rebuild);
    }
    return Duration.ofNanos(System.nanoTime() - start);
  }

  void start(Map<String, String> properties) throws IOException, InterruptedException {
    stop();
    List<String> args = new ArrayList<>();
    properties.forEach((k, v) -> args.add("-D" + k + "=" + v));
    process = java(args, log);
  }

  Duration awaitReady(int port) throws Exception {
    long start = System.nanoTime();
    HttpClient http = HttpClient.newHttpClient();
    URI ready = URI.create("http://127.0.0.1:" + port + "/health/ready");
    while (System.nanoTime() - start < Duration.ofSeconds(90).toNanos()) {
      if (!process.isAlive()) {
        throw new IllegalStateException("Application exited:\n" + log());
      }
      try {
        if (http.send(HttpRequest.newBuilder(ready).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
          return Duration.ofNanos(System.nanoTime() - start);
        }
      } catch (IOException notYet) {
        // still starting
      }
      Thread.sleep(250);
    }
    throw new IllegalStateException("Application not ready in 90s:\n" + log());
  }

  int awaitExit(Duration timeout) throws InterruptedException {
    if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
      throw new IllegalStateException("Application did not exit");
    }
    return process.exitValue();
  }

  /** Graceful stop, then {@code destroyForcibly} if the process is still alive after 30 s. Idempotent. */
  void stop() throws InterruptedException {
    terminate(process);
    process = null;
  }

  private static void terminate(Process p) throws InterruptedException {
    if (p == null || !p.isAlive()) {
      return;
    }
    p.destroy();
    if (!p.waitFor(30, TimeUnit.SECONDS)) {
      p.destroyForcibly();
      p.waitFor(10, TimeUnit.SECONDS);
    }
  }

  String log() throws IOException {
    return Files.exists(log) ? Files.readString(log) : "";
  }

  Path dir() {
    return dist;
  }

  private Process java(List<String> systemProperties, Path output) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.addAll(systemProperties);
    command.add("-jar");
    command.add("quarkus-run.jar");
    return new ProcessBuilder(command)
        .directory(dist.toFile())
        .redirectErrorStream(true)
        .redirectOutput(output.toFile())
        .start();
  }

  @Override
  public void close() throws Exception {
    stop();
  }
}
