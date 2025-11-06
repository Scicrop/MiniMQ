package com.scicrop.MiniMQ.service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scicrop.MiniMQ.model.QueueItem;

public class MiniMQService {
  private Thread thread;
  private QueueItem queueItem;
  private Path processingFilePath;

  private static final Path PROCESSING_DIR = Path.of("/opt/infinitestack/etc/MiniMQ/processing/");
  private static final Path DONE_DIR = Path.of("/opt/infinitestack/etc/MiniMQ/done/");

  public MiniMQService(Thread originalThread) {
    long startEpoch = System.currentTimeMillis();

    // PID da JVM
    String processName = ManagementFactory.getRuntimeMXBean().getName();
    int pid = Integer.parseInt(processName.split("@")[0]);

    // LWP (Thread ID no Linux ou fallback)
    int lwp = (int) originalThread.getId();
    try {
      String tid = Long.toString(originalThread.getId());
      if (Files.exists(Path.of("/proc/self/task/" + tid))) {
        lwp = Integer.parseInt(tid);
      }
    } catch (Exception ignored) {
      System.err.println(ignored);
    }

    // Cria o item da fila (gera JSON em /processing)
    queueItem = new QueueItem(startEpoch, pid, lwp);
    this.processingFilePath = PROCESSING_DIR.resolve(
        queueItem.getUuid() + ".json");

    // Cria a thread wrapper
    Runnable wrappedRunnable = () -> {
      try {
        originalThread.run();
      } finally {
        finalizeQueueItemJson(this.queueItem.getUuid());
      }
    };

    this.thread = new Thread(wrappedRunnable, originalThread.getName());
    this.thread.start();
  }

  /**
   * Move o arquivo JSON de /processing para /done.
   */
  private static void finalizeQueueItemJson(String uuid) {
    try {
      Path source = PROCESSING_DIR.resolve(uuid + ".json");
      Path target = DONE_DIR.resolve(uuid + ".json");

      if (Files.exists(source)) {
        Files.createDirectories(DONE_DIR);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      System.err.println("❌ Falha ao mover JSON para /done: " + e.getMessage());
    }
  }

  /**
   * Verifica se uma Thread específica está viva, dado seu ID.
   */
  private static Boolean isThreadRunningById(long threadId) {
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if (t.getId() == threadId && t.isAlive()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Verifica se a Thread associada a um UUID ainda está rodando.
   * Caso tenha terminado, move automaticamente o JSON para /done.
   */
  public static Boolean isRunningByUuid(String uuid) {
    ObjectMapper mapper = new ObjectMapper();
    Path processingPath = PROCESSING_DIR.resolve(uuid + ".json");
    Path donePath = DONE_DIR.resolve(uuid + ".json");

    QueueItem item = null;
    Path sourcePath = null;

    // Tenta ler o arquivo da pasta /processing ou /done
    try {
      if (Files.exists(processingPath)) {
        item = mapper.readValue(processingPath.toFile(), QueueItem.class);
        sourcePath = processingPath;
      } else if (Files.exists(donePath)) {
        item = mapper.readValue(donePath.toFile(), QueueItem.class);
        sourcePath = donePath;
      } else {
        return false; // UUID não encontrado
      }
    } catch (IOException e) {
      System.err.println("❌ Falha ao ler JSON: " + e.getMessage());
      return false;
    }

    if (item == null || item.getLwp() == null)
      return false;

    long threadId = item.getLwp().longValue();
    boolean running = isThreadRunningById(threadId);

    // Se a thread terminou e o arquivo ainda está em /processing, mover para /done
    if (!running && sourcePath.equals(processingPath)) {
      finalizeQueueItemJson(uuid);
    }

    return running;
  }

  // Getters e setters padrão
  public QueueItem getQueueItem() {
    return queueItem;
  }

  public void setQueueItem(QueueItem queueItem) {
    this.queueItem = queueItem;
  }

  public Thread getThread() {
    return thread;
  }

  public String startBackgroundJob() {
    Thread jobThread = new Thread(() -> {
      try {
        System.out.println("Executando tarefa de fundo....");
        Thread.sleep(5000);
        System.out.println("Tarefa concluída");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "JobThread-" + System.currentTimeMillis());

    MiniMQService miniMQService = new MiniMQService(jobThread);

    return miniMQService.getQueueItem().getUuid();
  }

  public boolean isJobRunning(String uuid) {
    return MiniMQService.isRunningByUuid(uuid);
  }
}