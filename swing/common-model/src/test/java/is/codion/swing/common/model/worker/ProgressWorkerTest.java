/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.worker;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ProgressWorkerTest {

  @Test
  void test() {
    final Value<Integer> progressValue = Value.value();
    final Value<String> messageValue = Value.value();

    final ProgressWorker.ProgressTask<Integer, String> task = progressReporter -> {
      progressReporter.setProgress(100);
      progressReporter.publish("Done");

      return 42;
    };
    ProgressWorker.builder(task)
            .onProgress(progressValue::set)
            .onPublish(chunks -> messageValue.set(chunks.get(0)))
            .onDone(() -> {
              assertEquals(100, progressValue.get());
              assertEquals("Done", messageValue.get());
            })
            .onResult(result -> assertEquals(42, result))
            .onInterrupted(() -> {})
            .onException(throwable -> {})
            .onStarted(() -> {})
            .execute();

  }
}
