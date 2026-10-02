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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ProgressReporter;

import java.util.List;

/**
 * The {@link ProgressWorker} javadoc snippets, each the region of the same name.
 */
final class ProgressWorkerSnippets {

	void usage() {
		ProgressWorker.builder() // @start region=usage
						.task(this::performTask)
						.onStarted(this::displayDialog)
						.onDone(this::closeDialog)
						.onSuccess(this::handleSuccess)
						.onResult(this::handleResult)
						.onProgress(this::displayProgress)
						.onPublish(this::publishMessage)
						.onCancelled(this::displayCancelledMessage)
						.onException(this::displayException)
						.execute(); // @end
	}

	private String performTask(ProgressReporter<String> progress) {
		return "";
	}

	private void displayDialog() {}

	private void closeDialog() {}

	private void handleSuccess() {}

	private void handleResult(String result) {}

	private void displayProgress(Integer progress) {}

	private void publishMessage(List<String> messages) {}

	private void displayCancelledMessage() {}

	private void displayException(Exception exception) {}
}
