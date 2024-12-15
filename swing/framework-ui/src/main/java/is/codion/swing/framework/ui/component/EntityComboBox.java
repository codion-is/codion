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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ItemFinder;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.DefaultComboBoxBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.FocusListener;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.component.EntityComboBox.ControlKeys.ADD;
import static is.codion.swing.framework.ui.component.EntityComboBox.ControlKeys.EDIT;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the {@link EntityComboBoxModel}.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends JComboBox<Entity> {

	/**
	 * The available controls.
	 * @see Builder#editPanel(Supplier)
	 */
	public static final class ControlKeys {

		/**
		 * Displays a dialog for adding a new record.<br>
		 * Default key stroke: INSERT
		 */
		public static final ControlKey<CommandControl> ADD = CommandControl.key("add", keyStroke(VK_INSERT));
		/**
		 * Displays a dialog for editing the selected record.<br>
		 * Default key stroke: CTRL-INSERT
		 */
		public static final ControlKey<CommandControl> EDIT = CommandControl.key("edit", keyStroke(VK_INSERT, CTRL_DOWN_MASK));

		private ControlKeys() {}
	}

	private final ControlMap controlMap;

	private EntityComboBox(DefaultBuilder builder) {
		super(builder.comboBoxModel());
		this.controlMap = builder.controlMap;
		this.controlMap.control(ADD).set(createAddControl(builder.editPanel,
						builder.controlMap.keyStroke(ADD).get(), builder.confirmAdd));
		this.controlMap.control(EDIT).set(createEditControl(builder.editPanel,
						builder.controlMap.keyStroke(EDIT).get(), builder.confirmEdit));
		builder.comboBoxModel().refresher().observable()
						.addConsumer(this::onRefreshingChanged);
	}

	@Override
	public EntityComboBoxModel getModel() {
		return (EntityComboBoxModel) super.getModel();
	}

	/**
	 * @return the underlying {@link EntityComboBoxModel}
	 */
	public EntityComboBoxModel model() {
		return getModel();
	}

	/**
	 * @return a Control for inserting a new record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<CommandControl> addControl() {
		return controlMap.control(ADD).optional();
	}

	/**
	 * @return a Control for editing the selected record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<CommandControl> editControl() {
		return controlMap.control(EDIT).optional();
	}

	/**
	 * Overridden as a workaround for editable combo boxes as initial focus components on
	 * detail panels stealing the focus from the parent panel on initialization
	 */
	@Override
	public void requestFocus() {
		if (isEditable()) {
			getEditor().getEditorComponent().requestFocus();
		}
		else {
			super.requestFocus();
		}
	}

	@Override
	public synchronized void addFocusListener(FocusListener listener) {
		super.addFocusListener(listener);
		if (isEditable()) {
			getEditor().getEditorComponent().addFocusListener(listener);
		}
	}

	@Override
	public synchronized void removeFocusListener(FocusListener listener) {
		super.removeFocusListener(listener);
		if (isEditable()) {
			getEditor().getEditorComponent().removeFocusListener(listener);
		}
	}

	/**
	 * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
	 * @param attribute the attribute
	 * @param <B> the builder type
	 * @return a {@link NumberField} builder bound to the selected value
	 */
	public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
					Attribute<Integer> attribute) {
		requireNonNull(attribute);
		return (B) Components.integerField(getModel().createSelectorValue(attribute))
						.columns(2)
						.selectAllOnFocusGained(true);
	}

	/**
	 * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
	 * @param itemFinder responsible for finding the item to select by value
	 * @param <B> the builder type
	 * @return a {@link NumberField} builder bound to the selected value
	 */
	public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
					ItemFinder<Entity, Integer> itemFinder) {
		requireNonNull(itemFinder);
		return (B) Components.integerField(getModel().createSelectorValue(itemFinder))
						.columns(2)
						.selectAllOnFocusGained(true);
	}

	/**
	 * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
	 * @param attribute the attribute
	 * @param <B> the builder type
	 * @return a {@link JTextField} builder bound to the selected value
	 */
	public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
					Attribute<String> attribute) {
		requireNonNull(attribute);
		return (B) Components.stringField(getModel().createSelectorValue(attribute))
						.columns(2)
						.selectAllOnFocusGained(true);
	}

	/**
	 * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
	 * @param itemFinder responsible for finding the item to select by value
	 * @param <B> the builder type
	 * @return a {@link JTextField} builder bound to the selected value
	 */
	public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
					ItemFinder<Entity, String> itemFinder) {
		requireNonNull(itemFinder);
		return (B) Components.stringField(getModel().createSelectorValue(itemFinder))
						.columns(2)
						.selectAllOnFocusGained(true);
	}

	/**
	 * Instantiates a new {@link EntityComboBox} builder
	 * @param comboBoxModel the combo box model
	 * @return a builder for a {@link EntityComboBox}
	 */
	public static Builder builder(EntityComboBoxModel comboBoxModel) {
		return builder(comboBoxModel, null);
	}

	/**
	 * Instantiates a new {@link EntityComboBox} builder
	 * @param comboBoxModel the combo box model
	 * @param linkedValue the linked value
	 * @return a builder for a {@link EntityComboBox}
	 */
	public static Builder builder(EntityComboBoxModel comboBoxModel, Value<Entity> linkedValue) {
		return new DefaultBuilder(comboBoxModel, linkedValue);
	}

	/**
	 * Builds a {@link EntityComboBox} instance.
	 * @see Builder#editPanel(Supplier)
	 */
	public interface Builder extends ComboBoxBuilder<Entity, EntityComboBox, Builder> {

		/**
		 * A edit panel is required for the add and edit controls.
		 * @param editPanel the edit panel supplier
		 * @return this builder instance
		 */
		Builder editPanel(Supplier<EntityEditPanel> editPanel);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		Builder confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 * @see #editPanel(Supplier)
		 */
		Builder confirmEdit(boolean confirmEdit);
	}

	private CommandControl createAddControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return editPanel == null ? null : EntityControls.createAddControl(this, editPanel, keyStroke, confirm);
	}

	private CommandControl createEditControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return editPanel == null ? null : EntityControls.createEditControl(this, editPanel, keyStroke, confirm);
	}

	private void onRefreshingChanged(boolean refreshing) {
		if (refreshing) {
			setCursor(Cursors.WAIT);
		}
		else {
			setCursor(Cursors.DEFAULT);
		}
	}

	private static final class DefaultBuilder extends DefaultComboBoxBuilder<Entity, EntityComboBox, Builder> implements Builder {

		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private Supplier<EntityEditPanel> editPanel;
		private boolean confirmAdd;
		private boolean confirmEdit;

		private DefaultBuilder(EntityComboBoxModel comboBoxModel, Value<Entity> linkedValue) {
			super(comboBoxModel, linkedValue);
		}

		@Override
		protected EntityComboBox createComboBox() {
			return new EntityComboBox(this);
		}

		@Override
		public Builder editPanel(Supplier<EntityEditPanel> editPanel) {
			this.editPanel = requireNonNull(editPanel);
			return this;
		}

		@Override
		public Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return this;
		}

		@Override
		public Builder confirmAdd(boolean confirmAdd) {
			this.confirmAdd = confirmAdd;
			return this;
		}

		@Override
		public Builder confirmEdit(boolean confirmEdit) {
			this.confirmEdit = confirmEdit;
			return this;
		}

		private EntityComboBoxModel comboBoxModel() {
			return (EntityComboBoxModel) comboBoxModel;
		}
	}
}
