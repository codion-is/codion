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
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.value.ValueObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.AbstractDialogBuilder;
import is.codion.swing.common.ui.dialog.DialogBuilder;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.button;
import static is.codion.swing.common.ui.component.Components.flowLayoutPanel;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * Provides edit and selection dialogs for entities.
 */
public final class EntityDialogs {

  private static final ResourceBundle EDIT_PANEL_MESSAGES = ResourceBundle.getBundle(EntityEditPanel.class.getName());

  /**
   * @param editModel the edit model to use
   * @param attribute the attribute to edit
   * @param <T> the attribute type
   * @return a new builder
   */
  public static <T> EditDialogBuilder<T> editDialog(SwingEntityEditModel editModel, Attribute<T> attribute) {
    return new DefaultEntityEditDialogBuilder<>(editModel, attribute);
  }

  /**
   * Creates a new {@link SelectionDialogBuilder} instance for searching for and selecting one or more entities from a table model.
   * @param tableModel the table model on which to base the table panel
   * @return a new builder instance
   */
  public static SelectionDialogBuilder selectionDialog(SwingEntityTableModel tableModel) {
    return new DefaultSelectionDialogBuilder(tableModel);
  }

  /**
   * Builds a dialog for editing single attributes for one or more entities
   * @param <T> the attribute type
   */
  public interface EditDialogBuilder<T> extends DialogBuilder<EditDialogBuilder<T>> {

    /**
     * @param componentFactory the component factory, if null then the default is used
     * @return this builder
     */
    EditDialogBuilder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory);

    /**
     * @param onValidationException called on validation exception
     * @return this builder
     */
    EditDialogBuilder<T> onValidationException(Consumer<ValidationException> onValidationException);

    /**
     * @param onException called on exception
     * @return this builder
     */
    EditDialogBuilder<T> onException(Consumer<Throwable> onException);

    /**
     * @param updater the updater to use
     * @param <E> the edit model type
     * @return this builder
     */
    <E extends SwingEntityEditModel> EditDialogBuilder<T> updater(Updater<E> updater);

    /**
     * Displays a dialog for editing the given entity
     * @param entity the entity to edit
     */
    void edit(Entity entity);

    /**
     * Displays a dialog for editing the given entities
     * @param entities the entities to edit
     */
    void edit(Collection<Entity> entities);

    /**
     * Handles performing the actual update when entities are edited.
     * @param <E> the edit model type
     */
    interface Updater<E extends SwingEntityEditModel> {

      /**
       * Updates the given entities, assuming they are all modified.
       * @param editModel the underlying edit model
       * @param entities the modified entities
       * @throws ValidationException in case of a validation failure
       * @throws DatabaseException in case of a database exception
       */
      void update(E editModel, Collection<Entity> entities) throws ValidationException, DatabaseException;
    }
  }

  /**
   * A builder for a selection dialog.
   */
  public interface SelectionDialogBuilder extends DialogBuilder<SelectionDialogBuilder> {

    /**
     * @param preferredSize the preferred dialog size
     * @return this builder instance
     */
    SelectionDialogBuilder preferredSize(Dimension preferredSize);

    /**
     * @return a List containing the selected entities
     * @throws CancelException in case the user cancels the operation
     */
    List<Entity> select();

    /**
     * Displays an entity table in a dialog for selecting a single entity
     * @return the selected entity or {@link Optional#empty()} if none was selected
     */
    Optional<Entity> selectSingle();
  }

  private static final class DefaultEntityEditDialogBuilder<T> extends AbstractDialogBuilder<EditDialogBuilder<T>>
          implements EditDialogBuilder<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditDialogBuilder.class);

    private final SwingEntityEditModel editModel;
    private final Attribute<T> attribute;

    private EntityComponentFactory<T, Attribute<T>, ?> componentFactory = new EditEntityComponentFactory<>();
    private Consumer<ValidationException> onValidationException = new DefaultValidationExceptionHandler();
    private Consumer<Throwable> onException = new DefaultExceptionHandler();
    private Updater<SwingEntityEditModel> updater;

    private DefaultEntityEditDialogBuilder(SwingEntityEditModel editModel, Attribute<T> attribute) {
      this.editModel = requireNonNull(editModel);
      this.attribute = requireNonNull(attribute);
    }

    @Override
    public EditDialogBuilder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory) {
      this.componentFactory = componentFactory == null ? new EditEntityComponentFactory<>() : componentFactory;
      return this;
    }

    @Override
    public EditDialogBuilder<T> onValidationException(Consumer<ValidationException> onValidationException) {
      this.onValidationException = requireNonNull(onValidationException);
      return this;
    }

    @Override
    public EditDialogBuilder<T> onException(Consumer<Throwable> onException) {
      this.onException = requireNonNull(onException);
      return this;
    }

    @Override
    public <E extends SwingEntityEditModel> EditDialogBuilder<T> updater(Updater<E> updater) {
      this.updater = (Updater<SwingEntityEditModel>) requireNonNull(updater);
      return this;
    }

    @Override
    public void edit(Entity entity) {
      edit(Collections.singleton(requireNonNull(entity)));
    }

    @Override
    public void edit(Collection<Entity> entities) {
      Set<EntityType> entityTypes = requireNonNull(entities).stream()
              .map(Entity::entityType)
              .collect(toSet());
      if (entityTypes.isEmpty()) {
        return;//no entities
      }
      if (entityTypes.size() > 1) {
        throw new IllegalArgumentException("All entities must be of the same type when editing");
      }

      EntityDefinition entityDefinition = editModel.entityDefinition();
      AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
      Collection<Entity> selectedEntities = entities.stream()
              .map(Entity::copy)
              .collect(toList());
      Collection<T> values = Entity.distinct(attribute, selectedEntities);
      T initialValue = values.size() == 1 ? values.iterator().next() : null;
      ComponentValue<T, ?> componentValue = editSelectedComponentValue(attribute, initialValue);
      InputValidator<T> inputValidator = new InputValidator<>(entityDefinition, attribute, componentValue);
      if (updater == null) {
        updater = new DefaultUpdater(owner, locationRelativeTo, onException);
      }
      boolean updatePerformed = false;
      while (!updatePerformed) {
        T newValue = Dialogs.inputDialog(componentValue)
                .owner(owner)
                .locationRelativeTo(locationRelativeTo)
                .title(FrameworkMessages.edit())
                .caption(attributeDefinition.caption())
                .inputValidator(inputValidator)
                .show();
        selectedEntities.forEach(entity -> entity.put(attribute, newValue));
        updatePerformed = update(selectedEntities.stream()
                .filter(Entity::modified)
                .collect(toList()));
      }
    }

    private ComponentValue<T, ? extends JComponent> editSelectedComponentValue(Attribute<T> attribute, T initialValue) {
      if (componentFactory == null) {
        EditEntityComponentFactory<T, Attribute<T>, JComponent> entityComponentFactory = new EditEntityComponentFactory<>();

        return entityComponentFactory.componentValue(attribute, editModel, initialValue);
      }

      return componentFactory.componentValue(attribute, editModel, initialValue);
    }

    private boolean update(Collection<Entity> entities) {
      try {
        updater.update(editModel, entities);

        return true;
      }
      catch (CancelException ignored) {/*ignored*/}
      catch (ValidationException e) {
        LOG.debug(e.getMessage(), e);
        onValidationException.accept(e);
      }
      catch (Exception e) {
        LOG.error(e.getMessage(), e);
        onException.accept(e);
      }

      return false;
    }

    private final class DefaultExceptionHandler implements Consumer<Throwable> {
      @Override
      public void accept(Throwable exception) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
          focusOwner = owner;
        }
        Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
      }
    }

    private final class DefaultValidationExceptionHandler implements Consumer<ValidationException> {

      @Override
      public void accept(ValidationException exception) {
        requireNonNull(exception);
        String title = editModel.entityDefinition().attributes()
                .definition(exception.attribute())
                .caption();
        JOptionPane.showMessageDialog(locationRelativeTo == null ? owner : locationRelativeTo, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
      }
    }

    private static final class DefaultUpdater implements Updater<SwingEntityEditModel> {

      private final Window dialogOwner;
      private final Component locationRelativeTo;
      private final Consumer<Throwable> exceptionHandler;

      private DefaultUpdater(Window dialogOwner, Component locationRelativeTo, Consumer<Throwable> exceptionHandler) {
        this.dialogOwner = dialogOwner;
        this.locationRelativeTo = locationRelativeTo;
        this.exceptionHandler = exceptionHandler;
      }

      @Override
      public void update(SwingEntityEditModel editModel, Collection<Entity> entities) throws ValidationException {
        EntityEditModel.Update update = editModel.createUpdate(entities);
        update.notifyBeforeUpdate();
        progressWorkerDialog(update::update)
                .title(EDIT_PANEL_MESSAGES.getString("updating"))
                .owner(dialogOwner)
                .locationRelativeTo(locationRelativeTo)
                .onException(exceptionHandler)
                .onResult(update::notifyAfterUpdate)
                .execute();
      }
    }

    private static final class InputValidator<T> implements Predicate<T> {

      private final EntityDefinition entityDefinition;
      private final Attribute<T> attribute;
      private final ComponentValue<T, ?> componentValue;

      private InputValidator(EntityDefinition entityDefinition, Attribute<T> attribute, ComponentValue<T, ?> componentValue) {
        this.entityDefinition = entityDefinition;
        this.attribute = attribute;
        this.componentValue = componentValue;
      }

      @Override
      public boolean test(T value) {
        Entity entity = entityDefinition.entity();
        entity.put(attribute, value);
        try {
          entityDefinition.validator().validate(entity, attribute);
          componentValue.validate(value);
        }
        catch (ValidationException | IllegalArgumentException e) {
          return false;
        }

        return true;
      }
    }
  }

  private static final class EditEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> extends DefaultEntityComponentFactory<T, A, C> {

    private static final int TEXT_INPUT_PANEL_COLUMNS = 20;

    @Override
    public ComponentValue<T, C> componentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
      AttributeDefinition<T> attributeDefinition = editModel.entityDefinition()
              .attributes().definition(attribute);
      if (attributeDefinition.items().isEmpty() && attribute.type().isString()) {
        //special handling for non-item based String attributes, text field panel instead of a text field
        return (ComponentValue<T, C>) new EntityComponents(editModel.entityDefinition())
                .textFieldPanel((Attribute<String>) attribute)
                .initialValue((String) initialValue)
                .columns(TEXT_INPUT_PANEL_COLUMNS)
                .buildValue();
      }

      return super.componentValue(attribute, editModel, initialValue);
    }
  }

  private static final class DefaultSelectionDialogBuilder extends AbstractDialogBuilder<SelectionDialogBuilder>
          implements SelectionDialogBuilder {

    private final SwingEntityTableModel tableModel;

    private Dimension preferredSize;

    private DefaultSelectionDialogBuilder(SwingEntityTableModel tableModel) {
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public SelectionDialogBuilder preferredSize(Dimension preferredSize) {
      this.preferredSize = requireNonNull(preferredSize);
      return this;
    }

    @Override
    public List<Entity> select() {
      return new EntitySelectionDialog(tableModel, owner, locationRelativeTo,
              titleProvider, icon, preferredSize, false).selectEntities();
    }

    @Override
    public Optional<Entity> selectSingle() {
      List<Entity> entities = new EntitySelectionDialog(tableModel, owner, locationRelativeTo,
              titleProvider, icon, preferredSize, true).selectEntities();

      return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }
  }

  private static final class EntitySelectionDialog {

    private final JDialog dialog;
    private final List<Entity> selectedEntities = new ArrayList<>();
    private final SwingEntityTableModel tableModel;
    private final EntityTablePanel entityTablePanel;

    private final Control okControl = Control.builder(this::ok)
            .name(Messages.ok())
            .mnemonic(Messages.okMnemonic())
            .build();
    private final Control cancelControl;
    private final Control searchControl = Control.builder(this::search)
            .name(FrameworkMessages.search())
            .mnemonic(FrameworkMessages.searchMnemonic())
            .build();

    private EntitySelectionDialog(SwingEntityTableModel tableModel, Window owner, Component locationRelativeTo,
                                  ValueObserver<String> titleObserver, ImageIcon icon, Dimension preferredSize,
                                  boolean singleSelection) {
      this.dialog = new JDialog(owner, titleObserver == null ? null : titleObserver.get());
      if (titleObserver != null) {
        titleObserver.addDataListener(dialog::setTitle);
      }
      if (icon != null) {
        dialog.setIconImage(icon.getImage());
      }
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      this.tableModel = requireNonNull(tableModel, "tableModel");
      this.tableModel.editModel().readOnly().set(true);
      this.entityTablePanel = createTablePanel(tableModel, preferredSize, singleSelection);
      this.cancelControl = Control.builder(dialog::dispose)
              .name(Messages.cancel())
              .mnemonic(Messages.cancelMnemonic())
              .build();
      KeyEvents.builder(VK_ESCAPE)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(cancelControl)
              .enable(dialog.getRootPane());
      JButton okButton = button(okControl).build();
      JPanel buttonPanel = flowLayoutPanel(FlowLayout.RIGHT)
              .add(okButton)
              .add(button(cancelControl).build())
              .add(button(searchControl).build())
              .build();
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityTablePanel, BorderLayout.CENTER);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
      dialog.pack();
      dialog.setLocationRelativeTo(locationRelativeTo);
      dialog.setModal(true);
      dialog.setResizable(true);
    }

    private EntityTablePanel createTablePanel(SwingEntityTableModel tableModel, Dimension preferredSize,
                                              boolean singleSelection) {
      EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
      tablePanel.initialize();
      tablePanel.table().addDoubleClickListener(mouseEvent -> {
        if (!tableModel.selectionModel().isSelectionEmpty()) {
          okControl.actionPerformed(null);
        }
      });
      tablePanel.conditionPanelVisible().set(true);
      tablePanel.table().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
      tablePanel.table().setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION);
      if (preferredSize != null) {
        tablePanel.setPreferredSize(preferredSize);
      }


      return tablePanel;
    }

    private void ok() {
      selectedEntities.addAll(tableModel.selectionModel().getSelectedItems());
      dialog.dispose();
    }

    private void search() {
      tableModel.refresh();
      if (tableModel.getRowCount() > 0) {
        tableModel.selectionModel().setSelectedIndexes(singletonList(0));
        entityTablePanel.table().requestFocusInWindow();
      }
      else {
        JOptionPane.showMessageDialog(parentWindow(entityTablePanel), FrameworkMessages.noSearchResults());
      }
    }

    private List<Entity> selectEntities() {
      dialog.setVisible(true);

      return selectedEntities;
    }
  }
}
