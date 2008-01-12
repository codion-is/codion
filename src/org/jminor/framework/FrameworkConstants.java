/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework;

/**
 * Constants used throughout the framework
 */
public class FrameworkConstants {

  public static final String CONNECTION_TYPE_LOCAL = "local";
  public static final String CONNECTION_TYPE_REMOTE = "remote";

  public static final String CLIENT_CONNECTION_TYPE = "jminor.client.connection.type";
  public static final String REPORT_PATH_PROPERTY = "jminor.report.path";
  public static final String DEFAULT_USERNAME_PROPERTY = "jminor.client.defaultuser";
  public static final String DATABASE_TYPE_PROPERTY = "jminor.db.type";
  public static final String DATABASE_HOST_PROPERTY = "jminor.db.host";
  public static final String DATABASE_SID_PROPERTY = "jminor.db.sid";
  public static final String DATABASE_PORT_PROPERTY = "jminor.db.port";
  public static final String SERVER_HOST_NAME_PROPERTY = "jminor.server.hostname";
  public static final String SERVER_PORT_PROPERTY = "jminor.server.port";
  public static final String SERVER_DB_PORT_PROPERTY = "jminor.server.db.port";
  public static final String SERVER_LOGGING_ON = "jminor.server.logging.status";
  public static final String SERVER_CONNECTION_LOG_SIZE = "jminor.server.logging.clientlogsize";
  public static final String SERVER_SECURE_CONNECTION = "jminor.server.connection.secure";
  public static final String SERVER_POOLING_INITIAL = "jminor.server.pooling.initial";
  public static final String PROFILING_THINKTIME_PROPERTY = "jminor.profiling.thinktime";
  public static final String PROFILING_LOGIN_WAIT_PROPERTY = "jminor.profiling.loginwait";
  public static final String PROFILING_BATCH_SIZE_PROPERTY = "jminor.profiling.batchsize";

  public static final String ENTITY_SERVER_NAME_PREFIX = "EntityDbServer";
}
