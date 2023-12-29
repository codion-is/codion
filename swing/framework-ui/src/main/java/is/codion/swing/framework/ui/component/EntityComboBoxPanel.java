/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
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

  private final EntityComboBox comboBox;

  private EntityComboBoxPanel(DefaultBuilder builder) {
    comboBox = builder.createComboBox();
    List<Action> actions = new ArrayList<>();
    if (builder.add) {
      actions.add(createAddControl(comboBox, builder.editPanelSupplier));
    }
    if (builder.edit) {
      actions.add(createEditControl(comboBox, builder.editPanelSupplier));
    }
    setLayout(new BorderLayout());
    add(createButtonPanel(comboBox, builder.buttonsFocusable, builder.buttonLocation,
            actions.toArray(new Action[0])), BorderLayout.CENTER);
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
