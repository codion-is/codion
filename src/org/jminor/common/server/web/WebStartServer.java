/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.web;

import Acme.Serve.Serve;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple web server for serving files.
 *
 * <pre>
 * WebStartServer server = new WebStartServer("c:\webstart");
 * server.serve();
 * </pre>
 */
public final class WebStartServer extends Serve {

  public static final int DEFAULT_PORT = 8080;

  /**
   * Instantiates a new WebStartServer on the default port.
   * @param documentRoot the document root
   */
  public WebStartServer(final String documentRoot) {
    this(documentRoot, DEFAULT_PORT);
  }

  /**
   * Instantiates a new WebStartServer on the given port.
   * @param documentRoot the document root
   * @param port the port on which to serve files
   */
  public WebStartServer(final String documentRoot, final int port) {
    final PathTreeDictionary aliases = new PathTreeDictionary();
    aliases.put("/*", new File(documentRoot));

    setMappingTable(aliases);

    // setting properties for the server, and exchangeable Acceptors
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("port", port);
    properties.put(Acme.Serve.Serve.ARG_NOHUP, "nohup");

    arguments = properties;

    addDefaultServlets(null); //file servlet
  }

  public void stop() {
    notifyStop();
    destroyAllServlets();
  }
}
