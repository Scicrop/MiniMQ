package com.scicrop.MiniMQApi.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scicrop.MiniMQApi.service.MiniMQService;

@RestController
@RequestMapping("/api/minimq")
public class MiniMQController {
  // MiniMQService miniMQService = new MiniMQService();

  @PostMapping("/start")
  public String startJobString() {
        Thread jobThread = new Thread(() -> {
      try {
        System.out.println("Executando tarefa de fundo....");
        Thread.sleep(5000);
        System.out.println("Tarefa concluída");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "JobThread-" + System.currentTimeMillis());
    MiniMQService data = new MiniMQService(jobThread);
    return "Tarefa iniciada com UUID: " + jobThread;
  }
}