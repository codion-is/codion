/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory implementation producing daemon threads
 */
public final class DaemonThreadFactory implements ThreadFactory {
  @Override
  public Thread newThread(final Runnable runnable) {
    final Thread thread = new Thread(runnable);
    thread.setDaemon(true);

    return thread;
  }
}
