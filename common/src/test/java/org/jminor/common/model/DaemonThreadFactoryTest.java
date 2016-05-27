/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertTrue;

public final class DaemonThreadFactoryTest {

  @Test
  public void daemonThreadFactory() {
    final ThreadFactory factory = new DaemonThreadFactory();
    final Thread thread = factory.newThread(new Runnable() {
      @Override
      public void run() {}
    });
    assertTrue(thread.isDaemon());
  }
}
