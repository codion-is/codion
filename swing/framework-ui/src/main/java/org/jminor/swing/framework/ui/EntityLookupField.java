/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Conjunction;
import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.common.ui.SwingMessages;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.table.FilteredTable;
import org.jminor.swing.common.ui.textfield.SizedDocument;
import org.jminor.swing.common.ui.textfield.TextFieldHint;
import org.jminor.swing.common.ui.value.AbstractComponentValue;
import org.jminor.swing.common.ui.value.BooleanValues;
import org.jminor.swing.common.ui.value.ComponentValuePanel;
import org.jminor.swing.common.ui.value.TextValues;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.swing.common.ui.control.Controls.control;

/**
 * A UI component based on the EntityLookupModel.
 *
 * The lookup is triggered by the ENTER key and behaves in the following way:
 * If the lookup result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog allowing either a single or multiple
 * selection based on the lookup model settings.
 *
 * {@link ListSelectionProvider} is the default {@link SelectionProvider}.
 *
 * @see EntityLookupModel
 * @see #setSelectionProvider(SelectionProvider)
 */
public final class EntityLookupField extends JTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityLookupField.class.getName());

  private static final String LOOKUP_MODEL = "lookupModel";
  private static final int BORDER_SIZE = 15;
  private static final int ENABLE_LOOKUP_DELAY = 250;

  private final EntityLookupModel model;
  private final TextFieldHint searchHint;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = KeyEvents.transferFocusForwardAction(this);
  private final Action transferFocusBackwardAction = KeyEvents.transferFocusBackwardAction(this);
  /**
   * A hack used to prevent the lookup being triggered by the closing of
   * the "empty result" message, which happens on windows
   */
  private final State lookupEnabledState = States.state(true);

  private SelectionProvider selectionProvider;

  private Color validBackgroundColor;
  private Color invalidBackgroundColor;
  private boolean performingLookup = false;

  /**
   * Initializes a new EntityLookupField.
   * @param lookupModel the lookup model on which to base this lookup field
   */
  public EntityLookupField(final EntityLookupModel lookupModel) {
    requireNonNull(lookupModel, LOOKUP_MODEL);
    this.model = lookupModel;
    this.settingsPanel = new SettingsPanel(lookupModel);
    this.selectionProvider = new ListSelectionProvider(model);
    linkToModel();
    setValidBackgroundColor(getBackground());
    setInvalidBackgroundColor(Color.LIGHT_GRAY);
    setToolTipText(lookupModel.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addFocusListener(initializeFocusListener());
    addEscapeListener();
    this.searchHint = TextFieldHint.enable(this, Messages.get(Messages.SEARCH_FIELD_HINT));
    updateColors();
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, initializeLookupControl());
    Components.linkToEnabledState(lookupModel.getSearchStringRepresentsSelectedObserver(), transferFocusAction);
    Components.linkToEnabledState(lookupModel.getSearchStringRepresentsSelectedObserver(), transferFocusBackwardAction);
  }

  /**
   * @return the lookup model this lookup field is based on
   */
  public EntityLookupModel getModel() {
    return model;
  }

  /**
   * Sets the SelectionProvider, that is, the object responsible for providing the comnponent used
   * for selecting items from the lookup result.
   * @param selectionProvider the {@link SelectionProvider} implementation to use when presenting
   * a selection dialog to the user
   * @throws NullPointerException in case {@code selectionProvier} is null
   */
  public void setSelectionProvider(final SelectionProvider selectionProvider) {
    this.selectionProvider = requireNonNull(selectionProvider);
  }

  /**
   * @param validBackgroundColor the background color to use when the text represents the selected items
   */
  public void setValidBackgroundColor(final Color validBackgroundColor) {
    this.validBackgroundColor = validBackgroundColor;
  }

  /**
   * @param invalidBackgroundColor the background color to use when the text does not represent the selected items
   */
  public void setInvalidBackgroundColor(final Color invalidBackgroundColor) {
    this.invalidBackgroundColor = invalidBackgroundColor;
  }

  /**
   * Activates the transferal of focus on ENTER
   */
  public void setTransferFocusOnEnter() {
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, transferFocusAction);
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, transferFocusBackwardAction);
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entity or null in case no entity was selected
   * @see EntityLookupField
   * @see EntityDefinition#getSearchProperties()
   */
  public static Entity lookupEntity(final String entityId, final EntityConnectionProvider connectionProvider,
                                    final JComponent dialogParent, final String lookupCaption, final String dialogTitle) {
    final List<Entity> entities = lookupEntities(entityId, connectionProvider, true, dialogParent, lookupCaption, dialogTitle);

    return entities.isEmpty() ? null : entities.get(0);
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityId.
   * @param entityId the entityId of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities or an empty list in case no entity was selected
   * @see EntityLookupField
   * @see EntityDefinition#getSearchProperties()
   */
  public static List<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                            final JComponent dialogParent, final String lookupCaption, final String dialogTitle) {
    return lookupEntities(entityId, connectionProvider, false, dialogParent, lookupCaption, dialogTitle);
  }

  private void selectEntities(final List<Entity> entities) {
    final JDialog dialog = new JDialog(Windows.getParentWindow(this), MESSAGES.getString("select_entity"));
    Dialogs.prepareOkCancelDialog(dialog, this, selectionProvider.getSelectionComponent(entities),
            selectionProvider.getSelectControl(), Controls.control(dialog::dispose));
    dialog.setVisible(true);
  }

  private void linkToModel() {
    model.getSearchStringValue().link(TextValues.textValue(this));
    model.getSearchStringValue().addDataListener(data -> updateColors());
    model.addSelectedEntitiesListener(data -> setCaretPosition(0));
  }

  private void addEscapeListener() {
    final Control escapeControl = Controls.control(() -> {
      getModel().refreshSearchText();
      selectAll();
    }, getModel().getSearchStringRepresentsSelectedObserver().getReversedObserver());
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ESCAPE, escapeControl);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        updateColors();
      }
      @Override
      public void focusLost(final FocusEvent e) {
        if (!e.isTemporary()) {
          if (getText().length() == 0) {
            getModel().setSelectedEntity(null);
          }
          else if (!searchHint.isHintTextVisible() && !performingLookup && !model.searchStringRepresentsSelected()) {
            performLookup(false);
          }
        }
        updateColors();
      }
    };
  }

  private void updateColors() {
    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintTextVisible());
    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private Control initializeLookupControl() {
    return Controls.control(() -> performLookup(true),
            FrameworkMessages.get(FrameworkMessages.SEARCH), States.aggregateState(Conjunction.AND,
                    getModel().getSearchStringRepresentsSelectedObserver().getReversedObserver(), lookupEnabledState));
  }

  private void performLookup(final boolean promptUser) {
    try {
      performingLookup = true;
      if (nullOrEmpty(model.getSearchString())) {
        model.setSelectedEntities(null);
      }
      else {
        if (!model.searchStringRepresentsSelected()) {
          try {
            List<Entity> queryResult;
            try {
              Components.showWaitCursor(this);
              queryResult = model.performQuery();
            }
            finally {
              Components.hideWaitCursor(this);
            }
            if (queryResult.size() == 1) {
              model.setSelectedEntities(queryResult);
            }
            else if (promptUser) {
              if (queryResult.isEmpty()) {
                showEmptyResultMessage();
              }
              else {
                selectEntities(queryResult);
              }
            }
          }
          catch (final Exception e) {
            DefaultDialogExceptionHandler.getInstance().displayException(e, Windows.getParentWindow(this));
          }
        }
      }
      updateColors();
    }
    finally {
      performingLookup = false;
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Controls.control(() -> Dialogs.displayInDialog(EntityLookupField.this, settingsPanel,
            FrameworkMessages.get(FrameworkMessages.SETTINGS)), FrameworkMessages.get(FrameworkMessages.SETTINGS)));

    return popupMenu;
  }

  /**
   * Necessary due to a bug on windows, where pressing Enter to dismiss this message
   * triggers another lookup, resulting in a loop
   */
  private void showEmptyResultMessage() {
    final Event closeEvent = Events.event();
    final JButton okButton = new JButton(Controls.control(closeEvent::onEvent, Messages.get(Messages.OK)));
    KeyEvents.addKeyEvent(okButton, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, true,
            Controls.control(okButton::doClick));
    KeyEvents.addKeyEvent(okButton, KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_FOCUSED, true,
            Controls.control(closeEvent::onEvent));
    final JPanel buttonPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonPanel.add(okButton);
    final JLabel messageLabel = new JLabel(FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
    messageLabel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE));
    final JPanel messagePanel = new JPanel(Layouts.borderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    messagePanel.add(buttonPanel, BorderLayout.SOUTH);
    disableLookup();
    Dialogs.displayInDialog(this, messagePanel, SwingMessages.get("OptionPane.messageDialogTitle"), closeEvent);
    enableLookup();
  }

  private void disableLookup() {
    lookupEnabledState.set(false);
  }

  /**
   * @see #lookupEnabledState
   */
  private void enableLookup() {
    final Timer timer = new Timer(ENABLE_LOOKUP_DELAY, e -> lookupEnabledState.set(true));
    timer.setRepeats(false);
    timer.start();
  }

  private static List<Entity> lookupEntities(final String entityId, final EntityConnectionProvider connectionProvider,
                                             final boolean singleSelection, final JComponent dialogParent,
                                             final String lookupCaption, final String dialogTitle) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(entityId, connectionProvider);
    lookupModel.getMultipleSelectionEnabledValue().set(!singleSelection);
    final ComponentValuePanel<Entity, EntityLookupField> inputPanel = new ComponentValuePanel<>(lookupCaption,
            new ComponentValue(lookupModel, null));
    Dialogs.displayInDialog(dialogParent, inputPanel, dialogTitle, Modal.YES,
            inputPanel.getOkAction(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return lookupModel.getSelectedEntities();
    }

    return emptyList();
  }

  private static final class SettingsPanel extends JPanel {

    private SettingsPanel(final EntityLookupModel lookupModel) {
      initializeUI(lookupModel);
    }

    private void initializeUI(final EntityLookupModel lookupModel) {
      final JPanel propertyBasePanel = new JPanel(new CardLayout(5, 5));
      final SwingFilteredComboBoxModel<ColumnProperty> propertyComboBoxModel = new SwingFilteredComboBoxModel<>();
      for (final Map.Entry<ColumnProperty, EntityLookupModel.LookupSettings> entry :
              lookupModel.getPropertyLookupSettings().entrySet()) {
        propertyComboBoxModel.addItem(entry.getKey());
        propertyBasePanel.add(initializePropertyPanel(entry.getValue()), entry.getKey().getPropertyId());
      }
      if (propertyComboBoxModel.getSize() > 0) {
        propertyComboBoxModel.addSelectionListener(selected ->
                ((CardLayout) propertyBasePanel.getLayout()).show(propertyBasePanel, selected.getPropertyId()));
        propertyComboBoxModel.setSelectedItem(propertyComboBoxModel.getElementAt(0));
      }

      final JCheckBox boxAllowMultipleValues = new JCheckBox(MESSAGES.getString("enable_multiple_search_values"));
      lookupModel.getMultipleSelectionEnabledValue().link(BooleanValues.booleanButtonModelValue(boxAllowMultipleValues.getModel()));
      final SizedDocument document = new SizedDocument();
      document.setMaxLength(1);
      final JTextField multipleValueSeparatorField = new JTextField(document, "", 1);
      lookupModel.getMultipleItemSeparatorValue().link(TextValues.textValue(multipleValueSeparatorField));

      final JPanel generalSettingsPanel = new JPanel(Layouts.gridLayout(2, 1));
      generalSettingsPanel.setBorder(BorderFactory.createTitledBorder(""));

      generalSettingsPanel.add(boxAllowMultipleValues);

      final JPanel valueSeparatorPanel = new JPanel(Layouts.borderLayout());
      valueSeparatorPanel.add(multipleValueSeparatorField, BorderLayout.WEST);
      valueSeparatorPanel.add(new JLabel(MESSAGES.getString("multiple_search_value_separator")), BorderLayout.CENTER);

      generalSettingsPanel.add(valueSeparatorPanel);

      setLayout(Layouts.borderLayout());
      add(new JComboBox<>(propertyComboBoxModel), BorderLayout.NORTH);
      add(propertyBasePanel, BorderLayout.CENTER);
      add(generalSettingsPanel, BorderLayout.SOUTH);
    }

    private static JPanel initializePropertyPanel(final EntityLookupModel.LookupSettings settings) {
      final JPanel panel = new JPanel(Layouts.gridLayout(3, 1));
      final JCheckBox boxCaseSensitive = new JCheckBox(MESSAGES.getString("case_sensitive"));
      settings.getCaseSensitiveValue().link(BooleanValues.booleanButtonModelValue(boxCaseSensitive.getModel()));
      final JCheckBox boxPrefixWildcard = new JCheckBox(MESSAGES.getString("prefix_wildcard"));
      settings.getWildcardPrefixValue().link(BooleanValues.booleanButtonModelValue(boxPrefixWildcard.getModel()));
      final JCheckBox boxPostfixWildcard = new JCheckBox(MESSAGES.getString("postfix_wildcard"));
      settings.getWildcardPostfixValue().link(BooleanValues.booleanButtonModelValue(boxPostfixWildcard.getModel()));

      panel.add(boxCaseSensitive);
      panel.add(boxPrefixWildcard);
      panel.add(boxPostfixWildcard);

      return panel;
    }
  }

  /**
   * Provides a JComponent for selecting one or more of a given set of entities
   */
  public interface SelectionProvider {

    /**
     * @param entities the entities to display in the component
     * @return the component to display for selecting entities
     */
    JComponent getSelectionComponent(List<Entity> entities);

    /**
     * Sets the preferred size of the selection component.
     * @param preferredSize the preferred selection component size
     */
    void setPreferredSize(Dimension preferredSize);

    /**
     * @return a Control which sets the selected entities in the underlying {@link EntityLookupModel}
     * and disposes the selection dialog
     */
    Control getSelectControl();
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link JList}
   */
  public static final class ListSelectionProvider implements SelectionProvider {

    private final JList<Entity> list = new JList<>();
    private final JScrollPane scrollPane = new JScrollPane(list);
    private final JPanel basePanel = new JPanel(Layouts.borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link JList} based {@link SelectionProvider}.
     * @param lookupModel the {@link EntityLookupModel}
     */
    public ListSelectionProvider(final EntityLookupModel lookupModel) {
      requireNonNull(lookupModel, LOOKUP_MODEL);
      this.selectControl = Controls.control(() -> {
        lookupModel.setSelectedEntities(list.getSelectedValuesList());
        Windows.getParentDialog(list).dispose();
      }, Messages.get(Messages.OK));
      list.setSelectionMode(lookupModel.getMultipleSelectionEnabledValue().get() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
      list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          if (e.getClickCount() == 2) {
            selectControl.actionPerformed(null);
          }
        }
      });
      basePanel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getSelectionComponent(final List<Entity> entities) {
      list.setListData(entities.toArray(new Entity[0]));
      list.removeSelectionInterval(0, list.getModel().getSize());
      list.scrollRectToVisible(list.getCellBounds(0, 0));

      return basePanel;
    }

    @Override
    public void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public Control getSelectControl() {
      return selectControl;
    }
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link EntityTablePanel}
   */
  public static final class TableSelectionProvider implements SelectionProvider {

    private final FilteredTable<Entity, Property, SwingEntityTableModel> table;
    private final JScrollPane scrollPane;
    private final JPanel basePanel = new JPanel(Layouts.borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link FilteredTable} based {@link SelectionProvider}.
     * @param lookupModel the {@link EntityLookupModel}
     */
    public TableSelectionProvider(final EntityLookupModel lookupModel) {
      requireNonNull(lookupModel, LOOKUP_MODEL);
      final SwingEntityTableModel tableModel = new SwingEntityTableModel(lookupModel.getEntityId(), lookupModel.getConnectionProvider()) {
        @Override
        protected List<Entity> performQuery() {
          return emptyList();
        }
      };
      table = new FilteredTable<>(tableModel);
      selectControl = control(() -> {
        lookupModel.setSelectedEntities(tableModel.getSelectionModel().getSelectedItems());
        Windows.getParentDialog(table).dispose();
      }, Messages.get(Messages.OK));
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      final String enterActionKey = "EntityLookupField.enter";
      table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), enterActionKey);
      table.getActionMap().put(enterActionKey, selectControl);
      final Collection<ColumnProperty> lookupProperties = lookupModel.getLookupProperties();
      tableModel.getColumnModel().setColumns(lookupProperties.toArray(new Property[0]));
      tableModel.setSortingDirective(lookupProperties.iterator().next().getPropertyId(), SortingDirective.ASCENDING);
      table.setSelectionMode(lookupModel.getMultipleSelectionEnabledValue().get() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      table.setDoubleClickAction(selectControl);
      scrollPane = new JScrollPane(table);
      basePanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * @return the underlying FilteredTablePanel
     */
    public FilteredTable<Entity, Property, SwingEntityTableModel> getTable() {
      return table;
    }

    @Override
    public JComponent getSelectionComponent(final List<Entity> entities) {
      table.getModel().clear();
      table.getModel().addEntitiesAt(0, entities);
      table.scrollRectToVisible(table.getCellRect(0, 0, true));

      return basePanel;
    }

    @Override
    public void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public Control getSelectControl() {
      return selectControl;
    }
  }

  /**
   * A {@link org.jminor.swing.common.ui.value.ComponentValue} implementation for Entity values based on a EntityLookupField.
   * @see EntityLookupField
   */
  public static final class ComponentValue extends AbstractComponentValue<Entity, EntityLookupField> {

    /**
     * Instantiates a new ComponentValue
     * @param lookupModel the lookup model to base the lookup field on
     * @param initialValue the initial value
     */
    public ComponentValue(final EntityLookupModel lookupModel, final Entity initialValue) {
      super(createEntityLookupField(lookupModel, initialValue));
    }

    @Override
    protected Entity getComponentValue(final EntityLookupField component) {
      final List<Entity> selectedEntities = getComponent().getModel().getSelectedEntities();

      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setComponentValue(final EntityLookupField component, final Entity value) {
      component.getModel().setSelectedEntity(value);
    }

    private static EntityLookupField createEntityLookupField(final EntityLookupModel lookupModel, final Entity initialValue) {
      final EntityLookupField field = new EntityLookupField(lookupModel);
      lookupModel.setSelectedEntity(initialValue);

      return field;
    }
  }
}
