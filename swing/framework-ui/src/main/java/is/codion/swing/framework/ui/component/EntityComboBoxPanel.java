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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.state.ObservableState;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.swing.framework.ui.component.EntityControls.*;
import static java.util.Objects.requireNonNull;

/**
 * A {@link EntityComboBox} based panel, with optional buttons for adding and editing items.
 */
public final class EntityComboBoxPanel extends JPanel {

	private final EntityComboBox comboBox;
	private final List<AbstractButton> buttons = new ArrayList<>(0);

	private EntityComboBoxPanel(DefaultBuilder builder) {
		comboBox = builder.createComboBox();
		List<Action> actions = new ArrayList<>();
		if (builder.includeAddButton) {
			comboBox.addControl().ifPresent(actions::add);
		}
		if (builder.includeEditButton) {
			comboBox.editControl().ifPresent(actions::add);
		}
		setLayout(new BorderLayout());
		add(createButtonPanel(comboBox, builder.buttonsFocusable, builder.buttonLocation,
						buttons, actions.toArray(new Action[0])), BorderLayout.CENTER);
		addFocusListener(new InputFocusAdapter(comboBox));
	}

	/**
	 * @return the {@link EntityComboBox}
	 */
	public EntityComboBox comboBox() {
		return comboBox;
	}

	/**
	 * @param comboBoxModel the combo box model
	 * @param editPanel the edit panel supplier
	 * @return a new builder instance
	 */
	public static Builder builder(EntityComboBoxModel comboBoxModel,
																Supplier<EntityEditPanel> editPanel) {
		return new DefaultBuilder(comboBoxModel, editPanel, null);
	}

	/**
	 * @param comboBoxModel the combo box model
	 * @param editPanel the edit panel supplier
	 * @param linkedValue the linked value
	 * @return a new builder instance
	 */
	public static Builder builder(EntityComboBoxModel comboBoxModel,
																Supplier<EntityEditPanel> editPanel,
																Value<Entity> linkedValue) {
		return new DefaultBuilder(comboBoxModel, editPanel, requireNonNull(linkedValue));
	}

	/**
	 * A builder for a {@link EntityComboBoxPanel}
	 */
	public interface Builder extends ComponentBuilder<Entity, EntityComboBoxPanel, Builder> {

		/**
		 * @param includeAddButton true if a 'Add' button should be included
		 * @return this builder instance
		 */
		Builder includeAddButton(boolean includeAddButton);

		/**
		 * @param includeEditButton true if a 'Edit' button should be included
		 * @return this builder instance
		 */
		Builder includeEditButton(boolean includeEditButton);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 */
		Builder confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 */
		Builder confirmEdit(boolean confirmEdit);

		/**
		 * Default false
		 * @param buttonsFocusable true if the buttons should be focusable
		 * @return this builder instance
		 */
		Builder buttonsFocusable(boolean buttonsFocusable);

		/**
		 * Must be one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}.
		 * @param buttonLocation the button location
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the value is not one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}
		 */
		Builder buttonLocation(String buttonLocation);

		/**
		 * @param comboBoxPreferredWidth the preferred combo box width
		 * @return this builder instance
		 */
		Builder comboBoxPreferredWidth(int comboBoxPreferredWidth);

		/**
		 * @return a new {@link EntityComboBoxPanel} based on this builder
		 */
		EntityComboBoxPanel build();
	}

	private static final class InputFocusAdapter extends FocusAdapter {

		private final EntityComboBox comboBox;

		private InputFocusAdapter(EntityComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void focusGained(FocusEvent e) {
			comboBox.requestFocusInWindow();
		}
	}

	private static final class DefaultBuilder extends AbstractComponentBuilder<Entity, EntityComboBoxPanel, Builder> implements Builder {

		private final EntityComboBox.Builder entityComboBoxBuilder;

		private boolean includeAddButton;
		private boolean includeEditButton;
		private boolean buttonsFocusable;
		private String buttonLocation = defaultButtonLocation();

		private DefaultBuilder(EntityComboBoxModel comboBoxModel, Supplier<EntityEditPanel> editPanelSupplier, Value<Entity> linkedValue) {
			super(linkedValue);
			this.entityComboBoxBuilder = EntityComboBox.builder(comboBoxModel)
							.editPanel(editPanelSupplier);
		}

		@Override
		public Builder includeAddButton(boolean includeAddButton) {
			this.includeAddButton = includeAddButton;
			return this;
		}

		@Override
		public Builder includeEditButton(boolean includeEditButton) {
			this.includeEditButton = includeEditButton;
			return this;
		}

		@Override
		public Builder confirmAdd(boolean confirmAdd) {
			this.entityComboBoxBuilder.confirmAdd(confirmAdd);
			return this;
		}

		@Override
		public Builder confirmEdit(boolean confirmEdit) {
			this.entityComboBoxBuilder.confirmEdit(confirmEdit);
			return this;
		}

		@Override
		public Builder buttonsFocusable(boolean buttonsFocusable) {
			this.buttonsFocusable = buttonsFocusable;
			return this;
		}

		@Override
		public Builder buttonLocation(String buttonLocation) {
			this.buttonLocation = validateButtonLocation(buttonLocation);
			return this;
		}

		@Override
		public Builder comboBoxPreferredWidth(int comboBoxPreferredWidth) {
			entityComboBoxBuilder.preferredWidth(comboBoxPreferredWidth);
			return this;
		}

		@Override
		protected EntityComboBoxPanel createComponent() {
			return new EntityComboBoxPanel(this);
		}

		@Override
		protected ComponentValue<Entity, EntityComboBoxPanel> createComponentValue(EntityComboBoxPanel component) {
			return new EntityComboBoxPanelValue(component);
		}

		@Override
		protected void enableTransferFocusOnEnter(EntityComboBoxPanel component, TransferFocusOnEnter transferFocusOnEnter) {
			transferFocusOnEnter.enable(component.comboBox());
			transferFocusOnEnter.enable(component.buttons.toArray(new JComponent[0]));
		}

		@Override
		protected void enableValidIndicator(ValidIndicatorFactory validIndicatorFactory,
																				EntityComboBoxPanel component, ObservableState valid) {
			validIndicatorFactory.enable(component.comboBox, valid);
		}

		private EntityComboBox createComboBox() {
			return entityComboBoxBuilder.build();
		}

		private static class EntityComboBoxPanelValue extends AbstractComponentValue<Entity, EntityComboBoxPanel> {

			private EntityComboBoxPanelValue(EntityComboBoxPanel component) {
				super(component);
				component.comboBox.getModel().selection().item().addListener(this::notifyListeners);
			}

			@Override
			protected Entity getComponentValue() {
				return component().comboBox.model().selection().item().get();
			}

			@Override
			protected void setComponentValue(Entity entity) {
				component().comboBox.model().selection().item().set(entity);
			}
		}
	}
}
