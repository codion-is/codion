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

import is.codion.common.resource.MessageBundle;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ItemFinder;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.combobox.DefaultComboBoxBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.framework.ui.component.EntityComboBox.EntityComboBoxControl.ADD;
import static is.codion.swing.framework.ui.component.EntityComboBox.EntityComboBoxControl.EDIT;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A UI component based on the {@link EntityComboBoxModel}.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends JComboBox<Entity> {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityComboBox.class, getBundle(EntityComboBox.class.getName()));

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final KeyboardShortcuts<EntityComboBoxControl> KEYBOARD_SHORTCUTS =
					keyboardShortcuts(EntityComboBoxControl.class);

	/**
	 * The available controls.
	 * @see Builder#editPanel(Supplier)
	 */
	public enum EntityComboBoxControl implements KeyboardShortcuts.Shortcut {
		/**
		 * Displays a dialog for adding a new record.<br>
		 * Default key stroke: INSERT
		 */
		ADD(keyStroke(VK_INSERT)),
		/**
		 * Displays a dialog for editing the selected record.<br>
		 * Default key stroke: CTRL-INSERT
		 */
		EDIT(keyStroke(VK_INSERT, CTRL_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		EntityComboBoxControl(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
	}

	private final Control addControl;
	private final Control editControl;

	private EntityComboBox(DefaultBuilder builder) {
		super(builder.comboBoxModel());
		addControl = createAddControl(builder.editPanel,
						builder.keyboardShortcuts.keyStroke(ADD).get());
		editControl = createEditControl(builder.editPanel,
						builder.keyboardShortcuts.keyStroke(EDIT).get());
		builder.comboBoxModel().refresher().observer()
						.addConsumer(this::onRefreshingChanged);
	}

	@Override
	public EntityComboBoxModel getModel() {
		return (EntityComboBoxModel) super.getModel();
	}

	/**
	 * @return a Control for inserting a new record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<Control> addControl() {
		return Optional.ofNullable(addControl);
	}

	/**
	 * @return a Control for editing the selected record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<Control> editControl() {
		return Optional.ofNullable(editControl);
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
	 * Creates an Action which displays a dialog for filtering this combo box via a foreign key
	 * @param foreignKey the foreign key on which to filter
	 * @return a Control for filtering this combo box
	 */
	public Control createForeignKeyFilterControl(ForeignKey foreignKey) {
		return Control.builder(createForeignKeyFilterCommand(requireNonNull(foreignKey)))
						.smallIcon(FrameworkIcons.instance().filter())
						.build();
	}

	/**
	 * Creates a {@link ComboBoxBuilder} returning a combo box for filtering this combo box via a foreign key
	 * @param foreignKey the foreign key on which to filter
	 * @param <B> the builder type
	 * @return a {@link ComboBoxBuilder} for a foreign key filter combo box
	 */
	public <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> createForeignKeyFilterComboBox(
					ForeignKey foreignKey) {
		return (B) builder(getModel().createForeignKeyFilterComboBoxModel(requireNonNull(foreignKey)))
						.completionMode(Completion.Mode.MAXIMUM_MATCH);
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
		 * @param control the combo box control
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(EntityComboBoxControl control, KeyStroke keyStroke);
	}

	private Control createAddControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke) {
		return editPanel == null ? null : EntityControls.createAddControl(this, editPanel, keyStroke);
	}

	private Control createEditControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke) {
		return editPanel == null ? null : EntityControls.createEditControl(this, editPanel, keyStroke);
	}

	private Control.Command createForeignKeyFilterCommand(ForeignKey foreignKey) {
		return () -> {
			Collection<Entity.Key> currentFilterKeys = getModel().getForeignKeyFilterKeys(foreignKey);
			Dialogs.okCancelDialog(createForeignKeyFilterComboBox(foreignKey).build())
							.owner(this)
							.title(MESSAGES.getString("filter_by"))
							.onCancel(() -> getModel().setForeignKeyFilterKeys(foreignKey, currentFilterKeys))
							.show();
		};
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

		private final KeyboardShortcuts<EntityComboBoxControl> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

		private Supplier<EntityEditPanel> editPanel;

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
		public Builder keyStroke(EntityComboBoxControl control, KeyStroke keyStroke) {
			keyboardShortcuts.keyStroke(control).set(keyStroke);
			return this;
		}

		private EntityComboBoxModel comboBoxModel() {
			return (EntityComboBoxModel) comboBoxModel;
		}
	}
}
