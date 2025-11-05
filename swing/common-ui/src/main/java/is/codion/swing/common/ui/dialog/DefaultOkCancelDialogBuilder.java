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
import is.codion.common.reactive.state.ObservableState;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends DefaultActionDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

	private @Nullable ObservableState okEnabled;
	private @Nullable ObservableState cancelEnabled;
	private @Nullable Runnable onOk;
	private @Nullable Runnable onCancel;
	private @Nullable Action okAction;
	private @Nullable Action cancelAction;

	@Override
	public OkCancelDialogBuilder action(Action action) {
		throw new UnsupportedOperationException("Adding an action directly is not supported");
	}

	@Override
	public OkCancelDialogBuilder defaultAction(Action defaultAction) {
		throw new UnsupportedOperationException("Adding a default action is not supported");
	}

	@Override
	public OkCancelDialogBuilder escapeAction(Action escapeAction) {
		throw new UnsupportedOperationException("Adding an escape action is not supported");
	}

	@Override
	public OkCancelDialogBuilder okEnabled(ObservableState okEnabled) {
		if (okAction != null) {
			throw new IllegalStateException("OK action has already been set");
		}
		this.okEnabled = requireNonNull(okEnabled);

		return this;
	}

	@Override
	public OkCancelDialogBuilder cancelEnabled(ObservableState cancelEnabled) {
		if (cancelAction != null) {
			throw new IllegalStateException("Cancel action has already been set");
		}
		this.cancelEnabled = requireNonNull(cancelEnabled);

		return this;
	}

	@Override
	public OkCancelDialogBuilder onOk(Runnable onOk) {
		if (okAction != null) {
			throw new IllegalStateException("OK action has already been set");
		}
		this.onOk = requireNonNull(onOk);

		return this;
	}

	@Override
	public OkCancelDialogBuilder onCancel(Runnable onCancel) {
		if (cancelAction != null) {
			throw new IllegalStateException("Cancel action has already been set");
		}
		this.onCancel = requireNonNull(onCancel);

		return this;
	}

	@Override
	public OkCancelDialogBuilder okAction(Action okAction) {
		if (onOk != null) {
			throw new IllegalStateException("onOk has already been set");
		}
		this.okAction = requireNonNull(okAction);
		return this;
	}

	@Override
	public OkCancelDialogBuilder cancelAction(Action cancelAction) {
		if (onCancel != null) {
			throw new IllegalStateException("onCancel has already been set");
		}
		this.cancelAction = requireNonNull(cancelAction);
		return this;
	}

	@Override
	public JDialog build() {
		actions().clear();
		if (okAction == null) {
			okAction = Control.builder()
							.command(onOk == null ? new DefaultOkCommand(component()) : new PerformAndCloseCommand(onOk, component()))
							.caption(Messages.ok())
							.mnemonic(Messages.okMnemonic())
							.enabled(okEnabled)
							.build();
		}
		else if (okAction instanceof Control) {
			okAction = ((Control) okAction).copy()
							.caption(Messages.ok())
							.mnemonic(Messages.okMnemonic())
							.build();
		}
		else {
			okAction.putValue(Action.NAME, Messages.ok());
			okAction.putValue(Action.MNEMONIC_KEY, (int) Messages.okMnemonic());
		}
		if (cancelAction == null) {
			cancelAction = Control.builder()
							.command(onCancel == null ? new DefaultCancelCommand(component()) : new PerformAndCloseCommand(onCancel, component()))
							.caption(Messages.cancel())
							.mnemonic(Messages.cancelMnemonic())
							.enabled(cancelEnabled)
							.build();
		}
		else if (cancelAction instanceof Control) {
			cancelAction = ((Control) cancelAction).copy()
							.caption(Messages.cancel())
							.mnemonic(Messages.cancelMnemonic())
							.build();
		}
		else {
			cancelAction.putValue(Action.NAME, Messages.cancel());
			cancelAction.putValue(Action.MNEMONIC_KEY, (int) Messages.cancelMnemonic());
		}
		super.defaultAction(okAction);
		super.escapeAction(cancelAction);

		return super.build();
	}

	private static final class PerformAndCloseCommand implements Control.Command {

		private final Runnable command;
		private final @Nullable JComponent component;

		private PerformAndCloseCommand(Runnable command, @Nullable JComponent component) {
			this.command = command;
			this.component = component;
		}

		@Override
		public void execute() {
			command.run();
			Utilities.disposeParentWindow(component);
		}
	}

	private static final class DefaultOkCommand implements Control.Command {

		private final @Nullable JComponent component;

		private DefaultOkCommand(@Nullable JComponent component) {
			this.component = component;
		}

		@Override
		public void execute() {
			Utilities.disposeParentWindow(component);
		}
	}

	private static final class DefaultCancelCommand implements Control.Command {

		private final @Nullable JComponent component;

		private DefaultCancelCommand(@Nullable JComponent component) {
			this.component = component;
		}

		@Override
		public void execute() {
			Utilities.disposeParentWindow(component);
		}
	}
}
