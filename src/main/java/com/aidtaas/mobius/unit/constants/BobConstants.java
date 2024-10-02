
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.constants;

import com.jayway.jsonpath.DocumentContext;
import java.util.Set;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.JsonPath.parse;

/**
 * This class represents BPMN constants.
 * It includes constants for web hooks, authorization, global authorization, job ID,
 * byte size, and process definition substring.
 */

public final class BobConstants {

  public static final String AUTHORIZATION = "Authorization";

  public static final String AUTHORIZATION_GLOBAL = "$_AUTHORIZATION";
  public static final String ENVIRONMENT_GLOBAL = "$_ENVIRONMENT";
  public static final String APP_ID_GLOBAL = "$_APP_ID";
  public static final String TENANT_ID_GLOBAL = "$_TENANT_ID";
  public static final String REQUESTER_ID_GLOBAL = "$_REQUESTER_ID";
  public static final String REQUESTER_TYPE_GLOBAL = "$_REQUESTER_TYPE";
  public static final String APP_ID = "appId";

  public static final int BYTE_SIZE = 4096;

  public static final int PROC_DEF_SUBSTRING = 14;

  public static final Integer HTTP_STATUS_CODE_200 = 200;
  public static final Integer HTTP_STATUS_CODE_300 = 300;
  public static final Integer HTTP_STATUS_CODE_404 = 404;
  public static final Integer HTTP_STATUS_CODE_500 = 500;

  public static final String HTTP_METHOD = "HTTP_METHOD";
  public static final String HTTP_PAYLOAD = "HTTP_PAYLOAD";
  public static final String HTTP_HEADERS = "HTTP_HEADERS";
  public static final String PATH_PARAMETERS = "PATH_PARAMETERS";
  public static final String QUERY_PARAM = "QUERY_PARAM";
  public static final String URL = "URL";

  public static final String RETRIEVE = "Retrieving {} value for {}";
  public static final String RUN_TIME_VARIABLE_FORMAT = System.getProperty("runtime.variable.format", "%s");
  public static final String PATH_DELIMITER = "/";
  public static final String SCHEMA_ID = "schemaId";
  public static final String INPUT_KEY_FORMAT = "$_INPUTS_%s";
  public static final String OUTPUT_KEY_FORMAT = "$_OUTPUTS_%s";
  public static final String VALIDATING_INPUTS_FOR_ACTIVITY_OF_PROCESS_INSTANCE =
    "Validating inputs for activity {} of process instance {}";
  public static final String RUN_RUN_INSTALL_SERVICES = "/run-run/install_services";
  public static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("\\r?\\n");
  public static final Pattern PIP_INSTALL_PATTERN = Pattern.compile("(?m)^.*pip install.*(?:\r?\n)?");
  public static final DocumentContext EMPTY_JSON_CONTEXT = parse("{}");
  public static final Pattern ROOT_PATTERN = Pattern.compile("ROOT.");
  public static final String CURRENT_RESPONSE_CODE = "$_RESPONSE_CODE[%s]";
  public static final String API_RESPONSE = "$_API_RESPONSE[%s]";
  public static final String GLOBAL_ERROR_VARIABLE = "$_ERROR_[%s][%s]";
  public static final String BUYER_ID = "buyerId";
  public static final String PRODUCT_ID = "productId";
  public static final String COMPONENT_ID = "componentId";
  public static final String PRODUCT_MASTER_CONFIG_ID = "productMasterConfigId";
  public static final String INTERFACE_TYPE = "interfaceType";
  public static final String INTERFACE_PATH = "interfacePath";
  public static final String API_OPERATION_HANDLER = "ApiOperationHandler";
  public static final String TERRAFORM_HANDLER = "TerraformHandler";
  public static final String COMPONENT_NAME = "componentName";
  public static final String ITEMS = "items";
  public static final String TP_TENANT = "TP";
  public static final String FILE_URL = "fileUrl";
  public static final String USER = "user";
  public static final String PASSWORD = "password";
  public static final String PRIVATE_KEY_PATH = "privateKeyPath";
  public static final String HOST = "host";
  public static final String PORT = "port";
  public static final String SCRIPT_NAME = "scriptName";
  public static final String CREATE = "create";
  public static final String SCRIPT_VARIABLES = "scriptVariables";
  public static final String RESULT_VARIABLE = "resultVariable";
  public static final Set<String> VARIABLE_KEYS = Set.of("THREADS", "TABLE_SIZE", "TABLES", "TIME", "REPORT_INTERVAL");
  public static final String OUTPUTS = "outputs";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String MULTIPART_FORM_DATA = "multipart/form-data";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String APPLICATION_X_ND_JSON = "application/x-ndjson";
  public static final double MAX_ATTEMPTS = 5.0;
  public static final double BACK_OFF_MULTIPLIER = 2.0;

  public static final int DEFAULT_RETRIES_NO = 3;
  public static final String TERRAFORM_TFVARS = "terraform.tfvars";
  public static final String TERRAFORM_FOLDER_PREFIX = "Terraform_";
  public static final String TERRAFORM_SHOW_JSON = "cd %s%s && terraform show -json";
  public static final String TERRAFORM_PATH_TEMPLATE = "tenant_%s\\/%s\\/%s";
  public static final String MKDIR_COMMAND = "mkdir %s";
  public static final String GIT_CLONE_COMMAND = "cd %s && git clone %s";
  public static final String TERRAFORM_SED_COMMAND = "cd %s && sed -i 's/\\$storageaccountname/terraformstatefilesrun/g; " +
    "s/\\$containername/terraformstate/g; " +
    "s/\\$storageaccountkey/%s\\/terraform.tfstate/g; " +
    "s/\\$storageaccountaccesskey/%s/g' backend.tf";
  public static final String TERRAFORM_INIT_COMMAND = "cd %s && terraform init";
  public static final String TERRAFORM_PLAN_COMMAND = "cd %s && terraform plan";
  public static final String TERRAFORM_APPLY_COMMAND = "cd %s && terraform apply -auto-approve -lock=false";
  public static final String TERRAFORM_DESTROY_COMMAND = "cd %s && terraform destroy -auto-approve";
  public static final String AZ_STORAGE_BLOB_COMMAND_FORMAT =
    "az storage blob lease break --container-name terraformstate --blob-name %s " +
      "&& az storage blob delete --container-name terraformstate --name %s";
  public static final String STORAGE_BLOB_COMMAND_FORMAT =
    "%s --account-name terraformstatefilesrun " +
      "--account-key %s";
  public static final String SCRIPT_SOURCE_NOT_SUPPORTED_YET = "Script source of type %s is not supported yet";

  public static final String SHELL_FOLDER_PREFIX = "Shell_";
  public static final String JMETER_EXECUTE_COMMAND = "/root/testflink/apache-jmeter/bin/jmeter.sh -n -t \"%s\" -Jid=%s";
  public static final String CD_AND_EXECUTE_COMMAND = "cd %s && %s";
  public static final String SED_COMMAND_FOR_PLACEHOLDER = "sed -i 's#$%s#%s#g' %s";
  public static final String CD_AND_CHMOD = "cd %s && chmod +777 %s";
  public static final String CD_AND_EXECUTE_SCRIPT = "cd %s && ./%s";
  public static final String ERROR_FAILED_TO_CALL_REST_API = "Failed to call rest api";
  public static final String NO_CONTENT_FROM_THE_RESPONSE = "No content from the response";
  public static final int SEC_TO_MS_MULTIPLIER = 1000;

  public static final String BEARER = "Bearer ";

  public static final String WORKFLOW_ID_GLOBAL = "$_WORKFLOW_ID";
  public static final String TENANT_ID = "tenantId";
  public static final String REQUESTER_ID = "requesterId";
  public static final String REQUESTER_TYPE = "requesterType";
  public static final String USER_ID = "userId";

  public static final String DRAFT = "Draft";
  public static final String DEPLOYED = "Deployed";
  public static final String EXECUTED = "EXECUTED";
  public static final String RUNNING = "RUNNING";

  public static final String STRING = "string";
  public static final String JSON = "json";
  public static final String FILE = "file";

  public static final String GAIANWORKFLOW = "GaianWorkflow";
  public static final String GAIANWORKFLOWS = "GaianWorkflows";
  public static final String BPMN = ".bpmn";
  public static final String VALUE = "value";

  public static final Integer BAD_REQUEST_0 = 400000;
  public static final Integer UNAUTHORIZED = 401000;
  public static final Integer INTERNAL_ERROR_1 = 500000;

  public static final String BPMN_PROCESS_GAIANWF = "bpmn:process id=\"GaianWorkflows";
  public static final String WF_ALREADY_DEPLOYED = "Workflow is already deployed";
  public static final String DEPLOYMENT_SOURCE = "deployment-source";
  public static final String DEPLOYMENT_SOURCE_VALUE = "Bob-Internals";
  public static final String ENABLE_DUPLICATE_FILTERING = "enable-duplicate-filtering";
  public static final String DEPLOYMENT_NAME = "deployment-name";
  public static final String DEPLOYMENT_ACTIVATION_TIME = "deployment-activation-time";
  public static final String DATE_TIME_FORMATTER_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  public static final String DATA = "data";

  public static final String BOB_CACHE = "bobCache";
  public static final String REDIS = "redis://";

  public static final String WF_ID = "{wfId}";

  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String DELETE = "DELETE";

  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

  public static final String ACCEPT = "accept";

  public static final String TYPE = "type";
  public static final String NAME = "name_";
  public static final String TEXT_2 = "text2_";
  public static final String BYTEARRAY_ID = "bytearray_id_";
  public static final String HISTORY = "history";
  public static final Integer ACTIVE_STATE = 1;
  public static final Integer SUSPEND_STATE = 2;

  public static final String BPMN_DEFINITIONS_TAG = "bpmn:definitions";
  public static final String BPMN_PROCESS_TAG = "bpmn:process";
  public static final String BPMN_SERVICE_TASK_TAG = "bpmn:serviceTask";
  public static final String BPMN_EXTENSION_ELEMENTS_TAG = "bpmn:extensionElements";
  public static final String CAMUNDA_INPUT_OUTPUT_TAG = "camunda:inputOutput";
  public static final String CAMUNDA_INPUT_PARAMETER_TAG = "camunda:inputParameter";
  public static final String OUTPUTS_TAG = "outputs";
  public static final String CONTENT_TAG = "content";
  public static final String ID_TAG = "id";
  public static final String SPINJAR_COM_MINIDEV_JSON_JSONARRAY = "spinjar.com.minidev.json.JSONArray";

  public static final String PROC_DEF_ID = "procDefId";
  public static final String BOB_OBJECT_MAPPER = "BobObjectMapper";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String TENANT_ID_DEPLOY = "tenant-id";
  public static final String VARIABLES = "variables";
  public static final String BUSINESS_KEY = "businessKey";
  public static final String FILENAME = "filename";
  public static final String MIMETYPE = "mimetype";
  public static final String ENCODING = "encoding";
  public static final String UTF_8 = "UTF-8";
  public static final String VALUE_INFO = "valueInfo";

  public static final int THREADMULTIPLIER = 5000;
  private BobConstants() {
  }
}
