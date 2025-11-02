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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.swing.common.model.action.DelayedAction;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTask;
import is.codion.swing.common.model.worker.ProgressWorker.Task;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.swing.common.model.action.DelayedAction.delayedAction;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

final class DefaultProgressWorkerDialogBuilder<T, V> extends AbstractDialogBuilder<ProgressWorkerDialogBuilder<T, V>>
				implements ProgressWorkerDialogBuilder<T, V> {

	static final ProgressWorkerDialogBuilder.BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	private static final Consumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();

	private final ProgressWorker.Builder<T, V> progressWorkerBuilder;
	private final ProgressDialog.Builder progressDialogBuilder = new ProgressDialog.DefaultBuilder();

	private Consumer<T> onResult = (Consumer<T>) EMPTY_CONSUMER;
	private @Nullable Consumer<List<V>> onPublish;
	private Consumer<Exception> onException = new DisplayExceptionInDialog();
	private int showDelay = SHOW_DELAY.getOrThrow();
	private int hideDelay = HIDE_DELAY.getOrThrow();
	private @Nullable DelayedAction showAction;
	private long startTime;

	DefaultProgressWorkerDialogBuilder(Task task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
	}

	DefaultProgressWorkerDialogBuilder(ResultTask<T> task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
	}

	DefaultProgressWorkerDialogBuilder(ProgressTask<V> task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
		this.progressDialogBuilder.maximum(task.maximum());
	}

	DefaultProgressWorkerDialogBuilder(ProgressResultTask<T, V> task) {
		this.progressWorkerBuilder = ProgressWorker.builder().task(task);
		this.progressDialogBuilder.maximum(task.maximum());
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> indeterminate(boolean indeterminate) {
		progressDialogBuilder.indeterminate(indeterminate);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> maximum(int maximum) {
		progressDialogBuilder.maximum(maximum);
		progressWorkerBuilder.maximum(maximum);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> stringPainted(boolean stringPainted) {
		progressDialogBuilder.stringPainted(stringPainted);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> border(@Nullable Border border) {
		progressDialogBuilder.border(border);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> northComponent(@Nullable JComponent northComponent) {
		progressDialogBuilder.northComponent(northComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> westComponent(@Nullable JComponent westComponent) {
		progressDialogBuilder.westComponent(westComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> eastComponent(@Nullable JComponent eastComponent) {
		progressDialogBuilder.eastComponent(eastComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> control(Supplier<? extends Control> control) {
		progressDialogBuilder.control(control);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> control(Control control) {
		progressDialogBuilder.control(control);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> progressBarSize(@Nullable Dimension progressBarSize) {
		progressDialogBuilder.progressBarSize(progressBarSize);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> delay(int show, int hide) {
		if (show < 0) {
			throw new IllegalArgumentException("show delay must be non-negative");
		}
		if (hide < 0) {
			throw new IllegalArgumentException("hide delay must be non-negative");
		}
		this.showDelay = show;
		this.hideDelay = hide;
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onPublish(Consumer<List<V>> onPublish) {
		this.onPublish = requireNonNull(onPublish);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onResult(Runnable onResult) {
		return onResult(result -> onResult.run());
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onResult(Consumer<T> onResult) {
		this.onResult = requireNonNull(onResult);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onResult(String title, String message) {
		requireNonNull(title);
		requireNonNull(message);

		return onResult(result -> showMessageDialog(owner, message, title, INFORMATION_MESSAGE));
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onException(Consumer<Exception> onException) {
		this.onException = requireNonNull(onException);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onException(String exceptionTitle) {
		return onException(new DisplayExceptionInDialog(requireNonNull(exceptionTitle)));
	}

	@Override
	public ProgressWorker<T, V> execute() {
		ProgressWorker<T, V> worker = build();
		worker.execute();

		return worker;
	}

	@Override
	public ProgressWorker<T, V> build() {
		onBuildConsumers.forEach(progressDialogBuilder::onBuild);

		ProgressDialog progressDialog = progressDialogBuilder
						.owner(owner)
						.locationRelativeTo(locationRelativeTo)
						.title(title)
						.icon(icon)
						.build();

		return progressWorkerBuilder
						.onStarted(() -> showDialog(progressDialog))
						.onProgress(progressDialog::setProgress)
						.onPublish(chunks -> publish(chunks, progressDialog))
						.onDone(() -> closeDialog(progressDialog))
						.onResult(onResult)
						.onException(onException)
						.build();
	}

	private static class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public ProgressWorkerDialogBuilder<?, ?> task(Task task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
		}

		@Override
		public <T> ProgressWorkerDialogBuilder<T, ?> task(ResultTask<T> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
		}

		@Override
		public <V> ProgressWorkerDialogBuilder<?, V> task(ProgressTask<V> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task)).indeterminate(false);
		}

		@Override
		public <T, V> ProgressWorkerDialogBuilder<T, V> task(ProgressResultTask<T, V> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task)).indeterminate(false);
		}
	}

	private void publish(List<V> chunks, ProgressDialog progressDialog) {
		progressDialog.setMessage(message(chunks));
		if (onPublish != null) {
			onPublish.accept(chunks);
		}
	}

	private @Nullable String message(List<V> chunks) {
		return chunks.isEmpty() ? null : Objects.toString(chunks.get(chunks.size() - 1));
	}

	private void showDialog(ProgressDialog progressDialog) {
		startTime = currentTimeMillis();
		showAction = delayedAction(showDelay, () -> progressDialog.setVisible(true));
	}

	private void closeDialog(ProgressDialog progressDialog) {
		cancelShowAction();
		long elapsed = currentTimeMillis() - startTime;
		long remainingDelay = hideDelay - elapsed;
		delayedAction((int) Math.max(0, remainingDelay), () -> {
			progressDialog.setVisible(false);
			progressDialog.dispose();
		});
	}

	private void cancelShowAction() {
		if (showAction != null) {
			showAction.cancel();
			showAction = null;
		}
	}

	private class DisplayExceptionInDialog implements Consumer<Exception> {

		private final String dialogTitle;

		private DisplayExceptionInDialog() {
			this(Messages.error());
		}

		private DisplayExceptionInDialog(String dialogTitle) {
			this.dialogTitle = dialogTitle;
		}

		@Override
		public void accept(Exception exception) {
			new DefaultExceptionDialogBuilder()
							.owner(owner)
							.title(dialogTitle)
							.show(exception);
		}
	}

	private static final class EmptyConsumer<T> implements Consumer<T> {

		@Override
		public void accept(T result) {}
	}
}
