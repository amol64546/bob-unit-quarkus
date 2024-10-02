
/*
 * Copyright (c) 2024. Gaian Solutions Pvt. Ltd. All rights reserved.
 */
package com.aidtaas.mobius.unit.handlers;

import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.config.KafkaProducer;
import com.aidtaas.mobius.unit.dto.HistoryEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;

import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaHistoryEventHandler implements HistoryEventHandler {

  private final KafkaProducer kafkaProducer;

  /**
   * This method is responsible for handling a single HistoryEvent. It first converts the
   * HistoryEvent into a HistoryEventDto object. Then it tries to convert the HistoryEventDto object
   * into a JSON string using the ObjectMapper. If the conversion is successful, it sends the JSON
   * string to a Kafka topic. The Kafka topic name is retrieved from the application properties. If
   * there is an error during the conversion of the HistoryEventDto to JSON, it logs the error.
   *
   * @param historyEvent The HistoryEvent object that needs to be handled.
   */
  @Override
  public void handleEvent(HistoryEvent historyEvent) {

    log.info("-----Virtual Thread log : {}",
      String.format(Thread.currentThread().getName(), "handleEvent"));
    HistoryEventDto historyEventDto = new HistoryEventDto(historyEvent.getClass(), historyEvent);

    String jsonString = null;

    try {
      jsonString = Config.OBJECT_MAPPER.writeValueAsString(historyEventDto);
    } catch (JsonProcessingException e) {
      log.error("Error writing json to string", e);
    }

    kafkaProducer.publishCamundaHistory(jsonString);

    log.info("History Event: {}, Published to Kafka", historyEvent.getClass());

  }

  /**
   * This method is responsible for handling a list of HistoryEvents. It processes the list of
   * HistoryEvents in parallel by calling the handleEvent method for each HistoryEvent.
   *
   * @param historyEvents The list of HistoryEvent objects that need to be handled.
   */
  @Override
  public void handleEvents(List<HistoryEvent> historyEvents) {
    historyEvents.parallelStream().forEach(this::handleEvent);
  }
}
