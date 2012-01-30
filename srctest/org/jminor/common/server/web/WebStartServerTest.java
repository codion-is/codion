/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.web;

import org.jminor.common.model.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class WebStartServerTest {

  private final String webRoot = System.getProperty("user.dir") + System.getProperty("file.separator") + "resources";
  private WebStartServer server;

  @Test
  public void test() throws URISyntaxException, IOException {
    InputStream input = null;
    try {
      try {//lets give the server a moment to start
        Thread.sleep(500);
      }
      catch (InterruptedException ignored) {}
      input = new URL("http://localhost:12345/file_templates/EntityEditPanel.template").openStream();
      assertTrue(input.read() > 0);
    }
    finally {
      Util.closeSilently(input);
    }
  }

  @Before
  public void setUp() {
    server = new WebStartServer(webRoot, 12345);
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        server.serve();
      }
    });
  }

  @After
  public void tearDown() {
    server.stop();
  }
}
