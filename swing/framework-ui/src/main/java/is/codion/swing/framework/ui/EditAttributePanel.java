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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel.EditTask.Result;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.label;
import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.BorderLayout.CENTER;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.showMessageDialog;

final class EditAttributePanel<T> extends JPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EditAttributePanel.class);

	private static final MessageBundle EDIT_PANEL_MESSAGES =
					messageBundle(EntityEditPanel.class, getBundle(EntityEditPanel.class.getName()));

	private final ComponentValue<? extends JComponent, T> componentValue;
	private final SwingEntityEditModel editModel;
	private final Collection<Entity> entities;
	private final Attribute<T> attribute;
	private final State valid = State.state(true);
	private final State modified = State.state();
	private final Value<String> message = Value.nonNull("");
	private final State updating = State.state();
	private final JPanel componentPanel;
	private final JProgressBar progressBar;

	EditAttributePanel(SwingEntityEditModel editModel, Collection<Entity> entities, Attribute<T> attribute,
										 ComponentValue<?, T> componentValue, @Nullable String caption) {
		setLayout(borderLayout());
		setBorder(createEmptyBorder(GAP.getOrThrow(), GAP.getOrThrow(), 0, GAP.getOrThrow()));
		this.editModel = editModel;
		this.attribute = attribute;
		this.entities = entities;
		this.componentValue = componentValue;
		this.componentPanel = createComponentPanel(caption);
		this.progressBar = createProgressBar();
		configureComponent(componentValue);
		updateStates();
		add(componentPanel, CENTER);
	}

	Control update() {
		return Control.builder()
						.command(this::performUpdate)
						.caption(Messages.ok())
						.enabled(State.and(modified, valid, updating.not()))
						.build();
	}

	Control cancel() {
		return Control.builder()
						.command(this::closeDialog)
						.caption(Messages.cancel())
						.enabled(updating.not())
						.build();
	}

	private void performUpdate() {
		updating.set(true);
		Collection<Entity> toUpdate = entities.stream()
						.map(Entity::copy)
						.map(Entity.Copy::mutable)
						.collect(toList());
		editModel.applyEdit(toUpdate, attribute, componentValue.get());
		ProgressWorker.builder()
						.task(editModel.updateTask(toUpdate.stream()
														.filter(Entity::modified)
														.collect(toList()))
										.prepare()::perform)
						.onStarted(this::showProgress)
						.onResult(this::onResult)
						.onException(this::onException)
						.execute();
	}

	private void onResult(Result result) {
		result.handle();
		closeDialog();
	}

	private void updateStates() {
		EntityValidator validator = editModel.editor().validator().getOrThrow();
		T value = componentValue.get();
		Collection<Entity> toUpdate = entities.stream()
						.map(Entity::copy)
						.map(Entity.Copy::mutable)
						.collect(toList());
		editModel.applyEdit(toUpdate, attribute, value);
		modified.set(toUpdate.stream().anyMatch(Entity::modified));
		for (Entity entity : toUpdate) {
			try {
				validator.validate(entity, attribute);
				componentValue.validate(value);
				valid.set(true);
				message.clear();
			}
			catch (IllegalArgumentException e) {
				valid.set(false);
				message.set(e.getMessage());
				return;
			}
		}
	}

	private void onException(Exception exception) {
		updating.set(false);
		hideProgress();
		if (exception instanceof ValidationException) {
			handleValidationException((ValidationException) exception);
		}
		else if (exception instanceof ExecutionException) {
			Throwable cause = exception.getCause();
			LOG.error(cause.getMessage(), cause);
			if (cause instanceof Exception) {
				handleException((Exception) cause);
			}
			else {
				handleException(new RuntimeException(cause));
			}
		}
		else {
			handleException(exception);
		}
	}

	private void handleValidationException(ValidationException exception) {
		String title = editModel.entityDefinition().attributes()
						.definition(exception.attribute())
						.caption();
		showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}

	private void handleException(Exception exception) {
		Dialogs.displayException(exception, Ancestor.window().of(this).get());
	}

	private JPanel createComponentPanel(@Nullable String caption) {
		BorderLayoutPanelBuilder inputPanelBuilder = borderLayoutPanel();
		if (caption != null) {
			inputPanelBuilder.north(label(caption));
		}
		return inputPanelBuilder
						.center(componentValue.component())
						.border(emptyBorder())
						.build();
	}

	private static JProgressBar createProgressBar() {
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(true);
		progressBar.setString(EDIT_PANEL_MESSAGES.getString("updating"));

		return progressBar;
	}

	private void configureComponent(ComponentValue<?, T> componentValue) {
		componentValue.addListener(this::updateStates);
		if (componentValue.component() instanceof JTextField) {
			((JTextField) componentValue.component()).selectAll();
		}
		message.addConsumer(componentValue.component()::setToolTipText);
	}

	private void closeDialog() {
		Ancestor.window().of(this).dispose();
	}

	private void showProgress() {
		remove(componentPanel);
		add(progressBar, CENTER);
		revalidate();
		repaint();
	}

	private void hideProgress() {
		remove(progressBar);
		add(componentPanel, CENTER);
		revalidate();
		repaint();
	}
}
