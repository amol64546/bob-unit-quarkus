
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Slf4j
@ApplicationScoped
public class KafkaProducer {

  private final Emitter<String> actionLogEmitter;
  private final Emitter<String> nodeRelationEmitter;
  private final Emitter<String> camundaHistoryEmitter;

  public KafkaProducer(@Channel("action-log") Emitter<String> actionLogEmitter,
                       @Channel("node-relation") Emitter<String> nodeRelationEmitter,
                       @Channel("camunda-history") Emitter<String> camundaHistoryEmitter) {
    this.actionLogEmitter = actionLogEmitter;
    this.nodeRelationEmitter = nodeRelationEmitter;
    this.camundaHistoryEmitter = camundaHistoryEmitter;
  }

  /**
   * Publishes the given payload to the "action-log" Kafka topic.
   *
   * @param payload The payload to publish.
   */
  public void publishActionLog(String payload) {
    log.info("Publishing message to Kafka topic 'action-log'");
    actionLogEmitter.send(payload);
  }

  /**
   * Publishes the given payload to the "node-relation" Kafka topic.
   *
   * @param payload The payload to publish.
   */
  public void publishNodeRelation(String payload) {
    log.info("Publishing message to Kafka topic 'node-relation'");
    nodeRelationEmitter.send(payload);
  }

  /**
   * Publishes the given payload to the "camunda-history" Kafka topic.
   *
   * @param payload The payload to publish.
   */
  public void publishCamundaHistory(String payload) {
    log.info("Publishing message to Kafka topic 'camunda-history'");
    camundaHistoryEmitter.send(payload);
  }

}
