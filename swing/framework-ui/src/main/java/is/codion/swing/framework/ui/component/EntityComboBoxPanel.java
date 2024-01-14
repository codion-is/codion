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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.framework.ui.component.EntityControls.*;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

/**
 * A {@link EntityComboBox} based panel, with optional buttons for adding and editing items.
 */
public final class EntityComboBoxPanel extends JPanel {

  /**
   * The default keyboard shortcut keyStrokes.
   */
  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS =
          keyboardShortcuts(KeyboardShortcut.class, EntityComboBoxPanel::defaultKeyStroke);

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcut {
    /**
     * Displays a dialog for adding a new record.
     */
    ADD,
    /**
     * Displays a dialog for editing the selected record.
     */
    EDIT
  }

  private final EntityComboBox comboBox;
  private final List<AbstractButton> buttons = new ArrayList<>(0);

  private EntityComboBoxPanel(DefaultBuilder builder) {
    comboBox = builder.createComboBox();
    List<Action> actions = new ArrayList<>();
    if (builder.add) {
      actions.add(createAddControl(comboBox, builder.editPanelSupplier,
              builder.keyboardShortcuts.keyStroke(KeyboardShortcut.ADD).get()));
    }
    if (builder.edit) {
      actions.add(createEditControl(comboBox, builder.editPanelSupplier,
              builder.keyboardShortcuts.keyStroke(KeyboardShortcut.EDIT).get()));
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
   * @param editPanelSupplier the edit panel supplier
   * @return a new builder instance
   */
  public static ComponentBuilder<Entity, EntityComboBoxPanel, Builder> builder(EntityComboBoxModel comboBoxModel,
                                                                               Supplier<EntityEditPanel> editPanelSupplier) {
    return new DefaultBuilder(comboBoxModel, editPanelSupplier, null);
  }

  /**
   * @param comboBoxModel the combo box model
   * @param editPanelSupplier the edit panel supplier
   * @param linkedValue the linked value
   * @return a new builder instance
   */
  public static ComponentBuilder<Entity, EntityComboBoxPanel, Builder> builder(EntityComboBoxModel comboBoxModel,
                                                                               Supplier<EntityEditPanel> editPanelSupplier,
                                                                               Value<Entity> linkedValue) {
    return new DefaultBuilder(comboBoxModel, editPanelSupplier, requireNonNull(linkedValue));
  }

  /**
   * A builder for a {@link EntityComboBoxPanel}
   */
  public interface Builder extends ComponentBuilder<Entity, EntityComboBoxPanel, Builder> {

    /**
     * @param add true if a 'Add' button should be included
     * @return this builder instance
     */
    Builder add(boolean add);

    /**
     * @param edit true if a 'Edit' button should be included
     * @return this builder instance
     */
    Builder edit(boolean edit);

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
     * @param keyboardShortcut the keyboard shortcut key
     * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
     * @return this builder instance
     */
    Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);

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

  private static KeyStroke defaultKeyStroke(KeyboardShortcut shortcut) {
    switch (shortcut) {
      case ADD: return keyStroke(VK_INSERT);
      case EDIT: return keyStroke(VK_INSERT, CTRL_DOWN_MASK);
      default: throw new IllegalArgumentException();
    }
  }

  private static final class DefaultBuilder extends AbstractComponentBuilder<Entity, EntityComboBoxPanel, Builder> implements Builder {

    private final ComboBoxBuilder<Entity, EntityComboBox, ?> entityComboBoxBuilder;
    private final Supplier<EntityEditPanel> editPanelSupplier;
    private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

    private boolean add;
    private boolean edit;
    private boolean buttonsFocusable;
    private String buttonLocation = defaultButtonLocation();

    private DefaultBuilder(EntityComboBoxModel comboBoxModel, Supplier<EntityEditPanel> editPanelSupplier, Value<Entity> linkedValue) {
      super(linkedValue);
      this.entityComboBoxBuilder = EntityComboBox.builder(comboBoxModel);
      this.editPanelSupplier = requireNonNull(editPanelSupplier);
    }

    @Override
    public Builder add(boolean add) {
      this.add = add;
      return this;
    }

    @Override
    public Builder edit(boolean edit) {
      this.edit = edit;
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
    public Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
      keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
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
    protected void enableTransferFocusOnEnter(EntityComboBoxPanel component) {
      TransferFocusOnEnter.enable(component.comboBox());
      component.buttons.forEach(TransferFocusOnEnter::enable);
    }

    @Override
    protected void setInitialValue(EntityComboBoxPanel component, Entity initialValue) {
      component.comboBox.setSelectedItem(initialValue);
    }

    private EntityComboBox createComboBox() {
      return entityComboBoxBuilder.clear().build();
    }

    private static class EntityComboBoxPanelValue extends AbstractComponentValue<Entity, EntityComboBoxPanel> {

      private EntityComboBoxPanelValue(EntityComboBoxPanel component) {
        super(component);
        component.comboBox.getModel().addSelectionListener(entity -> notifyListeners());
      }

      @Override
      protected Entity getComponentValue() {
        return component().comboBox.getModel().selectedValue();
      }

      @Override
      protected void setComponentValue(Entity entity) {
        component().comboBox.setSelectedItem(entity);
      }
    }
  }
}
