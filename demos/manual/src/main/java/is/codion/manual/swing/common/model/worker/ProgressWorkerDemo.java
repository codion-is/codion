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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.model.worker;

import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.componentDialog;
import static is.codion.swing.common.ui.dialog.Dialogs.exceptionDialog;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JOptionPane.showMessageDialog;

final class ProgressWorkerDemo {

	private static JFrame applicationFrame;

	private ProgressWorkerDemo() {}

	private static void task() {
		// tag::taskWorker[]
		// A non-progress aware task, producing no result
		ProgressWorker.Task task = () -> {
			// Perform the task
		};

		ProgressWorker.builder(task)
						.onException(exception ->
										exceptionDialog()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::taskWorker[]
	}

	private static void resultTask() {
		// tag::resultTaskWorker[]
		// A non-progress aware task, producing a result
		ProgressWorker.ResultTask<String> task = () -> {
			// Perform the task
			return "Result";
		};

		ProgressWorker.builder(task)
						.onResult(result ->
										showMessageDialog(applicationFrame, result))
						.onException(exception ->
										exceptionDialog()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::resultTaskWorker[]
	}

	private static void progressTask() {
		// tag::progressTaskWorker[]
		// A progress aware task, producing no result
		ProgressWorker.ProgressTask<String> task = progressReporter -> {
			// Perform the task
			progressReporter.report(42);
			progressReporter.publish("Message");
		};

		ProgressWorker.builder(task)
						.onProgress(progress ->
										System.out.println("Progress: " + progress))
						.onPublish(message ->
										showMessageDialog(applicationFrame, message))
						.onException(exception ->
										exceptionDialog()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::progressTaskWorker[]
	}

	private static void progressResultTask() {
		// tag::progressResultWorker[]
		// A reusable, cancellable task, producing a result.
		// Displays a progress bar in a dialog while running.
		var task = new DemoProgressResultTask();

		ProgressWorker.builder(task.prepare(142))
						.onStarted(task::started)
						.onProgress(task::progress)
						.onPublish(task::publish)
						.onDone(task::done)
						.onCancelled(task::cancelled)
						.onException(task::failed)
						.onResult(task::finished)
						.execute();
		// end::progressResultWorker[]
	}

	// tag::progressResultTask1[]
	static final class DemoProgressResultTask implements ProgressResultTask<Integer, String> {

		private final JProgressBar progressBar = progressBar()
						.indeterminate(false)
						.stringPainted(true)
						.string("")
						.build();
		// Indicates whether the task has been cancelled
		private final AtomicBoolean cancelled = new AtomicBoolean();
		// A Control for setting the cancelled state
		private final Control cancel = Control.builder()
						.command(() -> cancelled.set(true))
						.caption("Cancel")
						.mnemonic('C')
						.build();
		// A panel containing the progress bar and cancel button
		private final JPanel progressPanel = borderLayoutPanel()
						.centerComponent(progressBar)
						.eastComponent(button(cancel).build())
						.build();
		// The dialog displaying the progress panel
		private final JDialog dialog = componentDialog(progressPanel)
						.owner(applicationFrame)
						// Trigger the cancel control with the Escape key
						.keyEvent(KeyEvents.builder(VK_ESCAPE)
										.action(cancel))
						// Prevent the dialog from closing on Escape
						.disposeOnEscape(false)
						.build();

		private int taskSize;

		@Override
		public int maximumProgress() {
			return taskSize;
		}

		@Override
		public Integer execute(ProgressReporter<String> progressReporter) throws Exception {
			List<Integer> result = new ArrayList<>();
			for (int i = 0; i < taskSize; i++) {
				Thread.sleep(50);
				if (cancelled.get()) {
					throw new CancelException();
				}
				result.add(i);
				reportProgress(progressReporter, i);
			}

			return result.stream()
							.mapToInt(Integer::intValue)
							.sum();
		}

		// Makes this task reusable by resetting the internal state
		private DemoProgressResultTask prepare(int taskSize) {
			this.taskSize = taskSize;
			progressBar.getModel().setMaximum(taskSize);
			cancelled.set(false);

			return this;
		}

		private void reportProgress(ProgressReporter<String> reporter, int progress) {
			reporter.report(progress);
			if (progress < taskSize * 0.5) {
				reporter.publish("Going strong");
			}
			else if (progress > taskSize * 0.5 && progress < taskSize * 0.85) {
				reporter.publish("Half way there");
			}
			else if (progress > taskSize * 0.85) {
				reporter.publish("Almost done");
			}
		}

		private void started() {
			dialog.setVisible(true);
		}

		private void progress(int progress) {
			progressBar.setValue(progress);
		}

		private void publish(List<String> strings) {
			progressBar.setString(strings.get(0));
		}

		private void done() {
			dialog.setVisible(false);
			// end::progressResultTask1[]
			// DEMO ONLY CODE: So that the demo JVM exits
			// when the demo is done, otherwise not required
			dialog.dispose();
			// tag::progressResultTask2[]
		}

		private void cancelled() {
			showMessageDialog(applicationFrame, "Cancelled");
		}

		private void failed(Exception exception) {
			exceptionDialog()
							.owner(applicationFrame)
							.show(exception);
		}

		private void finished(Integer result) {
			showMessageDialog(applicationFrame, "Result : " + result);
		}
	}
	// end::progressResultTask2[]

	public static void main(String[] args) {
		progressResultTask();
	}
}
