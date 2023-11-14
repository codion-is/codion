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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.Action;
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

  private final ComponentValue<Entity, EntityComboBox> comboBoxValue;

  private EntityComboBoxPanel(DefaultBuilder builder) {
    comboBoxValue = builder.createComboBoxValue();
    List<Action> actions = new ArrayList<>();
    if (builder.addButton) {
      actions.add(createAddControl(comboBoxValue.component(), builder.editPanelSupplier));
    }
    if (builder.editButton) {
      actions.add(createEditControl(comboBoxValue.component(), builder.editPanelSupplier));
    }
    setLayout(new BorderLayout());
    add(createButtonPanel(comboBoxValue.component(), builder.buttonsFocusable, builder.buttonLocation,
            actions.toArray(new Action[0])), BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(comboBoxValue.component()));
  }

  /**
   * @return the {@link EntityComboBox}
   */
  public EntityComboBox entityComboBox() {
    return comboBoxValue.component();
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
     * @param addButton true if a 'Add' button should be included
     * @return this builder instance
     */
    Builder addButton(boolean addButton);

    /**
     * @param editButton true if a 'Edit' button should be included
     * @return this builder instance
     */
    Builder editButton(boolean editButton);

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

    private final ComboBoxBuilder<Entity, EntityComboBox, ?> entityComboBoxBuilder;
    private final Supplier<EntityEditPanel> editPanelSupplier;

    private boolean addButton;
    private boolean editButton;
    private boolean buttonsFocusable;
    private String buttonLocation = defaultButtonLocation();

    private DefaultBuilder(EntityComboBoxModel comboBoxModel, Supplier<EntityEditPanel> editPanelSupplier, Value<Entity> linkedValue) {
      super(linkedValue);
      this.entityComboBoxBuilder = EntityComboBox.builder(comboBoxModel);
      this.editPanelSupplier = requireNonNull(editPanelSupplier);
    }

    @Override
    public Builder addButton(boolean addButton) {
      this.addButton = addButton;
      return this;
    }

    @Override
    public Builder editButton(boolean editButton) {
      this.editButton = editButton;
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
    protected EntityComboBoxPanel createComponent() {
      return new EntityComboBoxPanel(this);
    }

    @Override
    protected ComponentValue<Entity, EntityComboBoxPanel> createComponentValue(EntityComboBoxPanel component) {
      return new EntityComboBoxPanelValue(component);
    }

    @Override
    protected void enableTransferFocusOnEnter(EntityComboBoxPanel component) {
      TransferFocusOnEnter.enable(component.entityComboBox());
    }

    @Override
    protected void setInitialValue(EntityComboBoxPanel component, Entity initialValue) {
      component.comboBoxValue.set(initialValue);
    }

    private ComponentValue<Entity, EntityComboBox> createComboBoxValue() {
      return entityComboBoxBuilder.clear().buildValue();
    }

    private static class EntityComboBoxPanelValue extends AbstractComponentValue<Entity, EntityComboBoxPanel> {

      private EntityComboBoxPanelValue(EntityComboBoxPanel component) {
        super(component);
        component.comboBoxValue.addListener(this::notifyListeners);
      }

      @Override
      protected Entity getComponentValue() {
        return component().comboBoxValue.get();
      }

      @Override
      protected void setComponentValue(Entity entity) {
        component().comboBoxValue.set(entity);
      }
    }
  }
}
