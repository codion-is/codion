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
import is.codion.common.resource.MessageBundle;
import is.codion.common.value.Value;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.common.Text.nullOrEmpty;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

class DefaultExceptionDialogBuilder extends AbstractDialogBuilder<ExceptionDialogBuilder>
				implements ExceptionDialogBuilder {

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultExceptionDialogBuilder.class, getBundle(DefaultExceptionDialogBuilder.class.getName()));

	private static final int MAXIMUM_MESSAGE_LENGTH = 50;

	private Collection<Class<? extends Throwable>> unwrapExceptions = WRAPPER_EXCEPTIONS.get();
	private String message;
	private boolean unwrap = true;
	private boolean systemProperties = SYSTEM_PROPERTIES.getOrThrow();

	@Override
	public ExceptionDialogBuilder message(String message) {
		this.message = message;
		return this;
	}

	@Override
	public ExceptionDialogBuilder unwrap(boolean unwrap) {
		this.unwrap = unwrap;
		return this;
	}

	@Override
	public ExceptionDialogBuilder unwrap(Collection<Class<? extends Throwable>> exceptions) {
		this.unwrapExceptions = requireNonNull(exceptions);
		return this;
	}

	@Override
	public ExceptionDialogBuilder systemProperties(boolean systemProperties) {
		this.systemProperties = systemProperties;
		return this;
	}

	@Override
	public void show(Throwable exception) {
		requireNonNull(exception);
		Throwable rootCause = unwrap ? ExceptionDialogBuilder.unwrap(exception, unwrapExceptions) : exception;
		setTitle(rootCause);
		setMessage(rootCause);
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				displayException(rootCause);
			}
			else {
				SwingUtilities.invokeAndWait(() -> displayException(rootCause));
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setTitle(Throwable rootCause) {
		if (title == null) {
			title(Value.nullable(messageTitle(rootCause)));
		}
	}

	private void setMessage(Throwable rootCause) {
		if (message == null) {
			String rootCauseMessage = rootCause.getMessage();
			if (nullOrEmpty(rootCauseMessage)) {
				rootCauseMessage = rootCause.getCause() != null ? trimMessage(rootCause.getCause()) : trimMessage(rootCause);
			}
			message(rootCauseMessage);
		}
	}

	private void displayException(Throwable exception) {
		ExceptionPanel exceptionPanel = new ExceptionPanel(exception, message == null ? exception.getMessage() : message, systemProperties);
		new DefaultComponentDialogBuilder(exceptionPanel)
						.title(title)
						.owner(owner)
						.onShown(new OnShown(exceptionPanel))
						.show();
	}

	private static String messageTitle(Throwable e) {
		if (e instanceof FileNotFoundException) {
			return MESSAGES.getString("file_not_found");
		}

		return Messages.error();
	}

	private static String trimMessage(Throwable e) {
		String message = e.getMessage();
		if (message != null && message.length() > MAXIMUM_MESSAGE_LENGTH) {
			return message.substring(0, MAXIMUM_MESSAGE_LENGTH) + "...";
		}

		return message;
	}

	private static final class OnShown implements Consumer<JDialog> {

		private final ExceptionPanel exceptionPanel;

		private OnShown(ExceptionPanel exceptionPanel) {
			this.exceptionPanel = exceptionPanel;
		}

		@Override
		public void accept(JDialog dialog) {
			dialog.getRootPane().setDefaultButton(exceptionPanel.closeButton());
			exceptionPanel.detailsCheckBox().requestFocusInWindow();
		}
	}
}
