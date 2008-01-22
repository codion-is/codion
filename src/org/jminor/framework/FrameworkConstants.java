/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework;

/**
 * Constants used throughout the framework
 */
public class FrameworkConstants {

  /**
   * Indicates a local database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  public static final String CONNECTION_TYPE_LOCAL = "local";

  /**
   * Indicates a remote database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  public static final String CONNECTION_TYPE_REMOTE = "remote";

  /**
   * Specifies whether the client should connect locally or remotely
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   */
  public static final String CLIENT_CONNECTION_TYPE = "jminor.client.connection.type";

  /**
   * The report path used for the default report generation
   */
  public static final String REPORT_PATH_PROPERTY = "jminor.report.path";

  /**
   * Default username for the login panel
   */
  public static final String DEFAULT_USERNAME_PROPERTY = "jminor.client.defaultuser";

  /**
   * The host on which to locate the server
   */
  public static final String SERVER_HOST_NAME_PROPERTY = "jminor.server.hostname";

  /**
   * If specified, the client will look for a server running on this port
   */
  public static final String SERVER_PORT_PROPERTY = "jminor.server.port";

  /**
   * The port on which the server should export the remote database connections
   */
  public static final String SERVER_DB_PORT_PROPERTY = "jminor.server.db.port";

  /**
   * The initial logging status on the server, either 1 (on) or (0) off
   */
  public static final String SERVER_LOGGING_ON = "jminor.server.logging.status";

  /**
   * Specifies the size of the (circular) log the server keeps in memory for each connected client
   */
  public static final String SERVER_CONNECTION_LOG_SIZE = "jminor.server.logging.clientlogsize";

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, 1 (on) or 0 (off)
   */
  public static final String SERVER_SECURE_CONNECTION = "jminor.server.connection.secure";

  /**
   * Specifies a comma seperated list of usernames for which to create connection pools on startup
   */
  public static final String SERVER_POOLING_INITIAL = "jminor.server.pooling.initial";

  /**
   * Specifies the initial think time setting for the profiling client
   * (max think time = thinktime, min think time = max think time / 2)
   */
  public static final String PROFILING_THINKTIME_PROPERTY = "jminor.profiling.thinktime";

  /**
   * Specifies the number time which the max think time is multiplied with when initializing the clients
   */
  public static final String PROFILING_LOGIN_WAIT_PROPERTY = "jminor.profiling.loginwait";

  /**
   * Specifies the initial client batch size
   */
  public static final String PROFILING_BATCH_SIZE_PROPERTY = "jminor.profiling.batchsize";

  /**
   * The JMinor server name prefix
   */
  public static final String JMINOR_SERVER_NAME_PREFIX = "JMinor EntityDb Server";

  /**
   * The class providing remote db connections
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "org.jminor.framework.server.EntityDbRemoteProvider";

  /**
   * The class providing local db connections
   */
  public static final String LOCAL_CONNECTION_PROVIDER = "org.jminor.framework.db.EntityDbProvider";
}
