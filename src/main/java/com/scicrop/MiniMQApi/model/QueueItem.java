package com.scicrop.MiniMQApi.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class QueueItem {
  private Long startEpoch;
  private Integer jvmPid;
  private Integer lwp;
  private String uuid;

  public QueueItem(Long startEpoch, Integer jvmPid, Integer lwp) {
    this.startEpoch = startEpoch;
    this.jvmPid = jvmPid;
    this.lwp = lwp;
    this.uuid = generateUuid();
    persistAsJson();
  }

  private String generateUuid() {
    String base = startEpoch + "-" + jvmPid + "-" + lwp;
    return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private void persistAsJson() {
    try {

      File dir = new File("/opt/infinitestack/etc/MiniMQ/");
      if (!dir.exists()) {
        dir.mkdirs();
      }
      File file = new File(dir, uuid + ".json");

      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter().writeValue(file, this);
    } catch (IOException e) {
      System.err.println("Falha ao persistir QueueItem: " + e);
    }
  }

  public Long getStartEpoch() {
    return startEpoch;
  }

  public Integer getJvmPid() {
    return jvmPid;
  }

  public Integer getLwp() {
    return lwp;
  }

  public String getUuid() {
    return uuid;
  }
}