
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */

/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import com.aidtaas.mobius.unit.aclmodels.ActionLogCreateRequest;
import com.aidtaas.mobius.unit.aclmodels.DataIngestionOperation;
import com.aidtaas.mobius.unit.aclmodels.NodeRelationCreateRequest;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionLogRequestStatus;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionSource;
import com.aidtaas.mobius.unit.aclmodels.enums.ActionType;
import com.aidtaas.mobius.unit.aclmodels.enums.NodeType;
import com.aidtaas.mobius.unit.aclmodels.enums.RequesterType;
import com.aidtaas.mobius.unit.config.Config;
import com.aidtaas.mobius.unit.config.ConfigProperties;
import com.aidtaas.mobius.unit.config.DynamicRestClient;
import com.aidtaas.mobius.unit.config.KafkaProducer;
import com.aidtaas.mobius.unit.config.URLResolver;
import com.aidtaas.mobius.unit.dto.ApiResponseBody;
import com.aidtaas.mobius.unit.model.Workflow;
import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.aidtaas.mobius.unit.constants.BobConstants.AUTHORIZATION;
import static com.aidtaas.mobius.unit.constants.BobConstants.DELETE;
import static com.aidtaas.mobius.unit.constants.BobConstants.GET;
import static com.aidtaas.mobius.unit.constants.BobConstants.POST;


@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class ActionLogUtil {

  private final ConfigProperties config;

  private final DynamicRestClient dynamicRestClient;

  private final KafkaProducer kafkaProducer;

  private final URLResolver urlResolver;

  private final CacheUtils cacheUtils;

  /**
   * Action log.
   *
   * @param actionSource           the action source
   * @param actionType             the action type
   * @param requesterType          the requester type
   * @param requesterId            the requester id
   * @param nodeType               the node type
   * @param nodeId                 the node id
   * @param requestObject
   * @param responseObject
   * @param actionLogRequestStatus
   */
  public void actionLog(ActionSource actionSource, ActionType actionType, RequesterType requesterType,
                        String requesterId, NodeType nodeType, String nodeId, Object requestObject,
                        Object responseObject, ActionLogRequestStatus actionLogRequestStatus) {
    ActionLogCreateRequest actionLogCreateRequest = ActionLogCreateRequest.builder()
      .id(UUID.randomUUID() + "_" + System.currentTimeMillis())
      .timestamp(System.currentTimeMillis())
      .actionType(actionType.name())
      .message(actionType.name() + " " + nodeType.name())
      .requesterType(requesterType.name())
      .nodeType(nodeType.name())
      .actionSource(actionSource.name())
      .requesterType(requesterType.getValue())
      .requesterId(requesterId)
      .nodeId(nodeId)
      .requestObject(requestObject)
      .responseObject(responseObject)
      .status(actionLogRequestStatus.name())
      .build();

    DataIngestionOperation dataIngestionOperation = DataIngestionOperation.builder()
      .actionType(ActionType.CREATE)
      .object(actionLogCreateRequest)
      .id(nodeId)
      .schemaId(config.aclActionLogSchemaId())
      .tenantId(requesterId)
      .build();

    try {
      kafkaProducer.publishActionLog(Config.OBJECT_MAPPER.writeValueAsString(dataIngestionOperation));
    } catch (JsonProcessingException e) {
      log.error("Error for action log request to string: ", e);
      throw new ObjectMappingException("Error while converting action log request to string");
    }
  }

  /**
   * Create node relations for constructs.
   *
   * @param wf            the wf
   * @param requesterId   the requester id
   * @param requesterType the requester type
   * @param auth          the auth
   */
  public void createNodeRelationsForConstructs(Workflow wf, String requesterId,
                                               RequesterType requesterType, String auth) {
    log.info("Node relation: Creation");
    if (wf.getDeployedVersion() > 1) {
      deleteNodeRelationsForConstructs(wf, auth);
    }

    List<NodeRelationCreateRequest> nodeRelationCreateRequestList = new ArrayList<>();

    if (MapUtils.isNotEmpty(wf.getConstructs())) {
      Map<NodeType, List<String>> nodes = wf.getConstructs();

      nodes.forEach((NodeType nodeType, List<String> nodeIds) -> {
        if (CollectionUtils.isNotEmpty(nodes.get(nodeType))) {
          nodeIds.forEach((String nodeId) -> {
            var nodeRelationCreateRequest = nodeRelation(requesterType, requesterId, nodeType, nodeId,
              NodeType.TENANT, requesterId);
            nodeRelationCreateRequestList.add(nodeRelationCreateRequest);
          });
        }
      });
    }
    bulkNodeRelation(nodeRelationCreateRequestList, auth);

    // Add the workflow to the cache
    String wfAsString;
    try {
      wfAsString = Config.OBJECT_MAPPER.writeValueAsString(wf);
    } catch (JsonProcessingException e) {
      log.error("Error converting Workflow to string: ", e);
      throw new ObjectMappingException("Error while converting Workflow to string");
    }
    String cacheKey = wf.getWfId() + "-" + wf.getDeployedVersion();
    cacheUtils.addToCache(cacheKey, wfAsString);
  }

  /**
   * Node relation.
   *
   * @param requesterType the requester type
   * @param requesterId   the requester id
   * @param fromNode      the from node
   * @param fromNodeId    the from node id
   * @param toNode        the to node
   * @param toNodeId      the to node id
   * @return the node relation create request
   */
  public NodeRelationCreateRequest nodeRelation(RequesterType requesterType, String requesterId,
                                                NodeType fromNode, String fromNodeId,
                                                NodeType toNode, String toNodeId) {
    return NodeRelationCreateRequest.builder()
      .requesterType(requesterType)
      .requesterId(requesterId)
      .fromNode(fromNode)
      .fromNodeId(fromNodeId)
      .toNode(toNode)
      .toNodeId(toNodeId)
      .universeId(null)
      .permissionRequired(true)
      .build();
  }

  /**
   * Bulk node relation.
   *
   * @param nodeRelationCreateRequestList the node relation create request list
   * @param auth                          the auth
   */
  public void bulkNodeRelation(List<NodeRelationCreateRequest> nodeRelationCreateRequestList, String auth) {

    try {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put(AUTHORIZATION, auth);
      dynamicRestClient.makeApiCall(config.aclCreateBulkNodeRelationUrl(),
        nodeRelationCreateRequestList, POST, headers);
      log.info("Bulk Node relation api call successful");
    } catch (RuntimeException e) {
      log.error("---------Exception msg for bulk node relation: {}", e.getMessage());
      nodeRelationCreateRequestList.stream().forEach((NodeRelationCreateRequest nodeRelationCreateRequest) -> {
        try {
          kafkaProducer.publishNodeRelation(Config.OBJECT_MAPPER.writeValueAsString(nodeRelationCreateRequest));
        } catch (JsonProcessingException ex) {
          log.error("Error for node relation request to string: ", ex);
          throw new ObjectMappingException("Error while converting list of action log request to string");
        }
      });
      log.info("Bulk Node relation published to kafka successfully");
    }
  }

  /**
   * Delete node relations for constructs.
   *
   * @param wf   the wf
   * @param auth the auth
   */
  public void deleteNodeRelationsForConstructs(Workflow wf, String auth) {

    log.info("Node relation: Deletion");
    String cacheKey = wf.getWfId() + "-" + (wf.getDeployedVersion() - 1);
    String workflowFromCache = cacheUtils.getFromCache(cacheKey);
    Workflow prevWf;
    if (StringUtils.isNotEmpty(workflowFromCache)) {
      try {
        prevWf = Config.OBJECT_MAPPER.readValue(workflowFromCache, Workflow.class);
      } catch (JsonProcessingException e) {
        throw new ObjectMappingException("Error parsing previous workflow JSON from cache", e);
      }
    } else {
      prevWf = getPreviousDeployedWorkflow(wf.getWfId(), wf.getDeployedVersion() - 1, auth);
    }

    if (MapUtils.isNotEmpty(prevWf.getConstructs())) {
      Map<NodeType, List<String>> nodes = wf.getConstructs();

      nodes.forEach((NodeType nodeType, List<String> nodeIds) -> {
        if (CollectionUtils.isNotEmpty(nodes.get(nodeType))) {
          nodeIds.forEach(nodeId ->
            deleteRelation(nodeType, nodeId, auth));
        }
      });
    }

  }

  /**
   * Delete relation.
   *
   * @param nodeType the node type
   * @param nodeId   the node id
   * @param auth     the auth
   */
  public void deleteRelation(NodeType nodeType, String nodeId, String auth) {

    try {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("nodeId", nodeId);
      queryParams.put("nodeType", String.valueOf(nodeType));
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put(AUTHORIZATION, auth);
      String url = urlResolver.constructUrl(config.aclDeleteNodeRelationUrl(), queryParams, null);
      dynamicRestClient.makeApiCall(url, null, DELETE, headers);
      log.info("Delete Node relation api call successful");
    } catch (RuntimeException e) {
      log.error("---------Exception msg for node relation: {}", e.getMessage());
    }
  }

  /**
   * Gets previous deployed workflow.
   *
   * @param workflowId   the workflow id
   * @param deploymentId the deployment id
   * @param auth         the auth
   * @return the previous deployed workflow
   */
  private Workflow getPreviousDeployedWorkflow(String workflowId, Integer deploymentId, String auth) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("wfId", workflowId.toString());
    queryParams.put("deployedVersion", deploymentId.toString());
    String url = urlResolver.constructUrl(config.serviceDeployedWf(), queryParams, null);
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(AUTHORIZATION, auth);
    ApiResponseBody apiResponseBody = dynamicRestClient.makeApiCall(url, null, GET, headers);
    try {
      return Config.OBJECT_MAPPER.readValue(apiResponseBody.getBody(), Workflow.class);
    } catch (JsonProcessingException e) {
      throw new ObjectMappingException("Error parsing previous deployed workflow JSON", e);
    }
  }

}
