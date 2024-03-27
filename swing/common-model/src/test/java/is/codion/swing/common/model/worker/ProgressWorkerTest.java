/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
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
			Thread.sleep(100);
			progressReporter.report(100);
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
