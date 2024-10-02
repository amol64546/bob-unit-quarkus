//
///*
// * Copyright (c) 2024.
// * Gaian Solutions Pvt. Ltd.
// * All rights reserved.
// */
//package com.aidtaas.mobius.unit.config;
//
//import com.aidtaas.mobius.unit.handlers.AnsibleHandler;
//import com.aidtaas.mobius.unit.handlers.ApiOperationHandler;
//import com.aidtaas.mobius.unit.handlers.PythonHandler;
//import com.aidtaas.mobius.unit.handlers.ShellScriptHandler;
//import com.aidtaas.mobius.unit.handlers.TerraformHandler;
//import io.quarkus.runtime.StartupEvent;
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.enterprise.event.Observes;
//import java.util.stream.IntStream;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.camunda.bpm.client.ExternalTaskClient;
//import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
//
//@Slf4j
//@ApplicationScoped
//@RequiredArgsConstructor
//public class HandlerConfig {
//
//  private final ConfigProperties config;
//  private final ApiOperationHandler apiOperationHandler;
//  private final AnsibleHandler ansibleHandler;
//  private final PythonHandler pythonHandler;
//  private final ShellScriptHandler shellScriptHandler;
//  private final TerraformHandler terraformHandler;
//
//  @SneakyThrows
//  void onStart(@Observes StartupEvent ev) {
//    IntStream.range(0, config.apiWorkerCount()).forEach(this::startApiWorker);
//    IntStream.range(0, config.terraformWorkerCount()).forEach(this::startTerraformWorker);
//    IntStream.range(0, config.shellScriptWorkerCount()).forEach(this::startShellScriptWorker);
//    IntStream.range(0, config.ansibleWorkerCount()).forEach(this::startAnsibleWorker);
//    IntStream.range(0, config.pythonWorkerCount()).forEach(this::startPythonWorker);
//    log.info("External Task Clients started successfully");
//  }
//
//  private void startApiWorker(int workerIndex) {
//    ExternalTaskClient client = ExternalTaskClient.create()
//      .baseUrl(config.engineRestUrl())
//      .workerId(config.apiWorkerId() + "-" + workerIndex)
//      .maxTasks(config.apiMaxTasks())
//      .asyncResponseTimeout(config.apiAsyncResponseTimeout())
//      .backoffStrategy(new ExponentialBackoffStrategy(config.backoffMultiplier(),
//        config.maxBackoffDelay(), config.waitTime()))
//      .lockDuration(config.apiLockDuration())
//      .usePriority(config.usePriority())
//      .build();
//
//    client.subscribe("ApiOperationHandler")
//      .handler(apiOperationHandler::execute)
//      .open();
//  }
//
//  private void startTerraformWorker(int workerIndex) {
//    ExternalTaskClient client = ExternalTaskClient.create()
//      .baseUrl(config.engineRestUrl())
//      .workerId(config.terraformWorkerId() + "-" + workerIndex)
//      .maxTasks(config.terraformMaxTasks())
//      .asyncResponseTimeout(config.terraformAsyncResponseTimeout())
//      .backoffStrategy(new ExponentialBackoffStrategy(config.backoffMultiplier(),
//        config.maxBackoffDelay(), config.waitTime()))
//      .lockDuration(config.terraformLockDuration())
//      .usePriority(config.usePriority())
//      .build();
//
//    client.subscribe("TerraformHandler")
//      .handler(terraformHandler::execute)
//      .open();
//  }
//
//  private void startShellScriptWorker(int workerIndex) {
//    ExternalTaskClient client = ExternalTaskClient.create()
//      .baseUrl(config.engineRestUrl())
//      .workerId(config.shellScriptWorkerId() + "-" + workerIndex)
//      .maxTasks(config.shellScriptMaxTasks())
//      .asyncResponseTimeout(config.shellScriptAsyncResponseTimeout())
//      .backoffStrategy(new ExponentialBackoffStrategy(config.backoffMultiplier(),
//        config.maxBackoffDelay(), config.waitTime()))
//      .lockDuration(config.shellScriptLockDuration())
//      .usePriority(config.usePriority())
//      .build();
//
//    client.subscribe("ShellScriptHandler")
//      .handler(shellScriptHandler::execute)
//      .open();
//  }
//
//  private void startAnsibleWorker(int workerIndex) {
//    ExternalTaskClient client = ExternalTaskClient.create()
//      .baseUrl(config.engineRestUrl())
//      .workerId(config.ansibleWorkerId() + "-" + workerIndex)
//      .maxTasks(config.ansibleMaxTasks())
//      .asyncResponseTimeout(config.ansibleAsyncResponseTimeout())
//      .backoffStrategy(new ExponentialBackoffStrategy(config.backoffMultiplier(),
//        config.maxBackoffDelay(), config.waitTime()))
//      .lockDuration(config.ansibleLockDuration())
//      .usePriority(config.usePriority())
//      .build();
//
//    client.subscribe("AnsibleHandler")
//      .handler(ansibleHandler::execute)
//      .open();
//  }
//
//  private void startPythonWorker(int workerIndex) {
//    ExternalTaskClient client = ExternalTaskClient.create()
//      .baseUrl(config.engineRestUrl())
//      .workerId(config.pythonWorkerId() + "-" + workerIndex)
//      .maxTasks(config.pythonMaxTasks())
//      .asyncResponseTimeout(config.pythonAsyncResponseTimeout())
//      .backoffStrategy(new ExponentialBackoffStrategy(config.backoffMultiplier(),
//        config.maxBackoffDelay(), config.waitTime()))
//      .lockDuration(config.pythonLockDuration())
//      .usePriority(config.usePriority())
//      .build();
//
//    client.subscribe("PythonHandler")
//      .handler(pythonHandler::execute)
//      .open();
//  }
//}
