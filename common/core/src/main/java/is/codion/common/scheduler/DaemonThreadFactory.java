/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import java.util.concurrent.ThreadFactory;

final class DaemonThreadFactory implements ThreadFactory {

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setDaemon(true);

    return thread;
  }
}
