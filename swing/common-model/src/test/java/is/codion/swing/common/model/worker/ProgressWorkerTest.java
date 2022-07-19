/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.worker;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ProgressWorkerTest {

  @Test
  void test() throws Exception {
    Value<Integer> progressValue = Value.value();
    Value<String> messageValue = Value.value();

    List<Integer> stateChanges = new ArrayList<>();

    ProgressWorker.ProgressTask<Integer, String> task = progressReporter -> {
      progressReporter.setProgress(100);
      progressReporter.publish("Done");

      return 42;
    };
    ProgressWorker.builder(task)
            .onStarted(() -> stateChanges.add(0))
            .onProgress(progressValue::set)
            .onPublish(chunks -> messageValue.set(chunks.get(0)))
            .onDone(() -> {
              stateChanges.add(1);
              assertEquals(100, progressValue.get());
              assertEquals("Done", messageValue.get());
            })
            .onResult(result -> {
              stateChanges.add(2);
              assertEquals(42, result);
              assertEquals(3, stateChanges.size());
              //sanity check the order of state changes
              for (int i = 0; i < stateChanges.size(); i++) {
                assertEquals(Integer.valueOf(i), stateChanges.get(i));
              }
            })
            .onCancelled(() -> {})
            .onInterrupted(() -> {})
            .onException(throwable -> {})
            .execute()
            .get();
  }
}
