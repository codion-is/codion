/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;
import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastFocusableButtonPanel;
import static is.codion.swing.framework.ui.component.EntityControls.createAddControl;
import static is.codion.swing.framework.ui.component.EntityControls.createEditControl;
import static java.util.Objects.requireNonNull;

/**
 * A {@link EntitySearchField} based panel, with optional buttons for adding and editing items.
 */
public final class EntitySearchFieldPanel extends JPanel {

  private final ComponentValue<Entity, EntitySearchField> searchFieldValue;

  private EntitySearchFieldPanel(DefaultBuilder builder) {
    searchFieldValue = builder.createSearchFieldValue();
    List<Action> actions = new ArrayList<>();
    if (builder.addButton) {
      actions.add(createAddControl(searchFieldValue.component(), builder.editPanelSupplier));
    }
    if (builder.editButton) {
      actions.add(createEditControl(searchFieldValue.component(), builder.editPanelSupplier));
    }
    setLayout(new BorderLayout());
    JPanel basePanel = builder.buttonsFocusable ?
            createEastFocusableButtonPanel(searchFieldValue.component(), actions.toArray(new Action[0])) :
            createEastButtonPanel(searchFieldValue.component(), actions.toArray(new Action[0]));
    add(basePanel, BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(searchFieldValue.component()));
  }

  /**
   * @return the {@link EntityComboBox}
   */
  public EntitySearchField entitySearchField() {
    return searchFieldValue.component();
  }

  /**
   * @param entitySearchModel the search model
   * @param editPanelSupplier the edit panel supplier
   * @return a new builder instance
   */
  public static ComponentBuilder<Entity, EntitySearchFieldPanel, Builder> builder(EntitySearchModel entitySearchModel,
                                                                                  Supplier<EntityEditPanel> editPanelSupplier) {
    return new DefaultBuilder(entitySearchModel, editPanelSupplier, null);
  }

  /**
   * @param entitySearchModel the search model
   * @param editPanelSupplier the edit panel supplier
   * @param linkedValue the linked value
   * @return a new builder instance
   */
  public static ComponentBuilder<Entity, EntitySearchFieldPanel, Builder> builder(EntitySearchModel entitySearchModel,
                                                                                  Supplier<EntityEditPanel> editPanelSupplier,
                                                                                  Value<Entity> linkedValue) {
    return new DefaultBuilder(entitySearchModel, editPanelSupplier, requireNonNull(linkedValue));
  }

  /**
   * A builder for a {@link EntitySearchFieldPanel}
   */
  public interface Builder extends ComponentBuilder<Entity, EntitySearchFieldPanel, Builder> {

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
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    Builder columns(int columns);

    /**
     * Makes the field convert all lower case input to upper case
     * @param upperCase if true the text component convert all lower case input to upper case
     * @return this builder instance
     */
    Builder upperCase(boolean upperCase);

    /**
     * Makes the field convert all upper case input to lower case
     * @param lowerCase if true the text component convert all upper case input to lower case
     * @return this builder instance
     */
    Builder lowerCase(boolean lowerCase);

    /**
     * @param searchHintEnabled true if a search hint text should be visible when the field is empty and not focused
     * @return this builder instance
     */
    Builder searchHintEnabled(boolean searchHintEnabled);

    /**
     * @param searchOnFocusLost true if search should be performed on focus lost
     * @return this builder instance
     */
    Builder searchOnFocusLost(boolean searchOnFocusLost);

    /**
     * @param selectAllOnFocusGained true if the contents should be selected when the field gains focus
     * @return this builder instance
     */
    Builder selectAllOnFocusGained(boolean selectAllOnFocusGained);

    /**
     * @param searchIndicator the search indicator
     * @return this builder instance
     */
    Builder searchIndicator(EntitySearchField.SearchIndicator searchIndicator);

    /**
     * @param selectorFactory the selector factory to use
     * @return this builder instance
     */
    Builder selectorFactory(Function<EntitySearchModel, EntitySearchField.Selector> selectorFactory);

    /**
     * @param limit the search result limit
     * @return this builder instance
     */
    Builder limit(int limit);

    /**
     * @return a new {@link EntitySearchFieldPanel} based on this builder
     */
    EntitySearchFieldPanel build();
  }

  private static final class InputFocusAdapter extends FocusAdapter {

    private final EntitySearchField searchField;

    private InputFocusAdapter(EntitySearchField searchField) {
      this.searchField = searchField;
    }

    @Override
    public void focusGained(FocusEvent e) {
      searchField.requestFocusInWindow();
    }
  }

  private static final class DefaultBuilder extends AbstractComponentBuilder<Entity, EntitySearchFieldPanel, Builder> implements Builder {

    private final EntitySearchField.Builder searchFieldBuilder;
    private final Supplier<EntityEditPanel> editPanelSupplier;

    private boolean addButton;
    private boolean editButton;
    private boolean buttonsFocusable;

    private DefaultBuilder(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanelSupplier, Value<Entity> linkedValue) {
      super(linkedValue);
      this.searchFieldBuilder = EntitySearchField.builder(searchModel);
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
    public Builder columns(int columns) {
      searchFieldBuilder.columns(columns);
      return this;
    }

    @Override
    public Builder upperCase(boolean upperCase) {
      searchFieldBuilder.upperCase(upperCase);
      return this;
    }

    @Override
    public Builder lowerCase(boolean lowerCase) {
      searchFieldBuilder.lowerCase(lowerCase);
      return this;
    }

    @Override
    public Builder searchHintEnabled(boolean searchHintEnabled) {
      searchFieldBuilder.searchHintEnabled(searchHintEnabled);
      return this;
    }

    @Override
    public Builder searchOnFocusLost(boolean searchOnFocusLost) {
      searchFieldBuilder.searchOnFocusLost(searchOnFocusLost);
      return this;
    }

    @Override
    public Builder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
      searchFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
      return this;
    }

    @Override
    public Builder searchIndicator(EntitySearchField.SearchIndicator searchIndicator) {
      searchFieldBuilder.searchIndicator(searchIndicator);
      return this;
    }

    @Override
    public Builder selectorFactory(Function<EntitySearchModel, EntitySearchField.Selector> selectorFactory) {
      searchFieldBuilder.selectorFactory(selectorFactory);
      return this;
    }

    @Override
    public Builder limit(int limit) {
      searchFieldBuilder.limit(limit);
      return this;
    }

    @Override
    protected EntitySearchFieldPanel createComponent() {
      return new EntitySearchFieldPanel(this);
    }

    @Override
    protected ComponentValue<Entity, EntitySearchFieldPanel> createComponentValue(EntitySearchFieldPanel component) {
      return new EntitySearchFieldPanelValue(component);
    }

    @Override
    protected void enableTransferFocusOnEnter(EntitySearchFieldPanel component) {
      TransferFocusOnEnter.enable(component.entitySearchField());
    }

    @Override
    protected void setInitialValue(EntitySearchFieldPanel component, Entity initialValue) {
      component.searchFieldValue.set(initialValue);
    }

    private ComponentValue<Entity, EntitySearchField> createSearchFieldValue() {
      return searchFieldBuilder.clear().buildValue();
    }

    private static class EntitySearchFieldPanelValue extends AbstractComponentValue<Entity, EntitySearchFieldPanel> {

      private EntitySearchFieldPanelValue(EntitySearchFieldPanel component) {
        super(component);
        component.searchFieldValue.addListener(this::notifyListeners);
      }

      @Override
      protected Entity getComponentValue() {
        return component().searchFieldValue.get();
      }

      @Override
      protected void setComponentValue(Entity entity) {
        component().searchFieldValue.set(entity);
      }
    }
  }
}
