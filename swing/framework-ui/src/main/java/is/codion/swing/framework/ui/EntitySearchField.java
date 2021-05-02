/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.model.table.SortingDirective;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.DefaultEntitySearchModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValuePanel;
import is.codion.swing.framework.model.SwingEntityTableModel;

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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the EntitySearchModel.
 *
 * The search is triggered by the ENTER key and behaves in the following way:
 * If the search result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog allowing either a single or multiple
 * selection based on the search model settings.
 *
 * {@link ListSelectionProvider} is the default {@link SelectionProvider}.
 *
 * @see EntitySearchModel
 * @see #setSelectionProvider(SelectionProvider)
 */
public final class EntitySearchField extends JTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntitySearchField.class.getName());

  private static final String SEARCH_MODEL = "searchModel";
  private static final int BORDER_SIZE = 15;

  private final EntitySearchModel model;
  private final TextFields.Hint searchHint;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = Components.transferFocusForwardAction(this);
  private final Action transferFocusBackwardAction = Components.transferFocusBackwardAction(this);

  private SelectionProvider selectionProvider;

  private Color validBackgroundColor;
  private Color invalidBackgroundColor;
  private boolean performingSearch = false;
  private boolean transferFocusOnEnter = false;

  /**
   * Initializes a new EntitySearchField.
   * @param searchModel the search model on which to base this search field
   */
  public EntitySearchField(final EntitySearchModel searchModel) {
    requireNonNull(searchModel, SEARCH_MODEL);
    this.model = searchModel;
    this.settingsPanel = new SettingsPanel(searchModel);
    this.selectionProvider = new ListSelectionProvider(model);
    linkToModel();
    setValidBackgroundColor(getBackground());
    setInvalidBackgroundColor(getBackground().darker());
    setToolTipText(searchModel.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addFocusListener(initializeFocusListener());
    addKeyListener(new EnterKeyListener());
    addKeyListener(new EscapeKeyListener());
    this.searchHint = TextFields.hint(this, Messages.get(Messages.SEARCH_FIELD_HINT));
    updateColors();
    Components.linkToEnabledState(searchModel.getSearchStringRepresentsSelectedObserver(), transferFocusAction);
    Components.linkToEnabledState(searchModel.getSearchStringRepresentsSelectedObserver(), transferFocusBackwardAction);
  }

  /**
   * @return the search model this search field is based on
   */
  public EntitySearchModel getModel() {
    return model;
  }

  /**
   * Sets the SelectionProvider, that is, the object responsible for providing the component used
   * for selecting items from the search result.
   * @param selectionProvider the {@link SelectionProvider} implementation to use when presenting
   * a selection dialog to the user
   * @throws NullPointerException in case {@code selectionProvider} is null
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
   * @return true if focus should be transferred on Enter
   */
  public boolean isTransferFocusOnEnter() {
    return transferFocusOnEnter;
  }

  /**
   * @param transferFocusOnEnter specifies whether focus should be transferred on Enter
   */
  public void setTransferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
    if (transferFocusOnEnter) {
      createForwardEvent().enable(this);
      createBackwardEvent().enable(this);
    }
    else {
      createForwardEvent().disable(this);
      createBackwardEvent().disable(this);
    }
  }

  /**
   * Performs a search for the given entity type, using a {@link EntitySearchField} displayed
   * in a dialog, using the default search attributes for the given entityType.
   * @param entityType the entityType of the entity to perform a search for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entity or null in case no entity was selected
   * @see EntitySearchField
   * @see EntityDefinition#getSearchAttributes()
   */
  public static Entity lookupEntity(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                    final JComponent dialogParent, final String lookupCaption, final String dialogTitle) {
    final List<Entity> entities = lookupEntities(entityType, connectionProvider, true, dialogParent, lookupCaption, dialogTitle);

    return entities.isEmpty() ? null : entities.get(0);
  }

  /**
   * Performs a search for the given entity type, using a {@link EntitySearchField} displayed
   * in a dialog, using the default search attributes for the given entityType.
   * @param entityType the entityType of the entity to perform a search for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities or an empty list in case no entity was selected
   * @see EntitySearchField
   * @see EntityDefinition#getSearchAttributes()
   */
  public static List<Entity> lookupEntities(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                            final JComponent dialogParent, final String lookupCaption, final String dialogTitle) {
    return lookupEntities(entityType, connectionProvider, false, dialogParent, lookupCaption, dialogTitle);
  }

  private void selectEntities(final List<Entity> entities) {
    final JDialog dialog = new JDialog(Windows.getParentWindow(this), MESSAGES.getString("select_entity"));
    Dialogs.prepareOkCancelDialog(dialog, selectionProvider.getSelectionComponent(entities),
            selectionProvider.getSelectControl(), Control.control(dialog::dispose));
    dialog.setVisible(true);
  }

  private void linkToModel() {
    ComponentValue.stringTextComponent(this).link(model.getSearchStringValue());
    model.getSearchStringValue().addDataListener(searchString -> updateColors());
    model.addSelectedEntitiesListener(entities -> {
      setCaretPosition(0);
      if (entities.isEmpty()) {
        searchHint.updateHint();
      }
    });
  }

  private KeyEvents.KeyEventBuilder createForwardEvent() {
    return KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(transferFocusAction);
  }

  private KeyEvents.KeyEventBuilder createBackwardEvent() {
    return KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .modifiers(KeyEvent.SHIFT_DOWN_MASK)
            .onKeyPressed()
            .action(transferFocusBackwardAction);
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
          if (getText().isEmpty()) {
            getModel().setSelectedEntity(null);
          }
          else if (!searchHint.isHintVisible() && !performingSearch && !model.searchStringRepresentsSelected()) {
            performSearch(false);
          }
        }
        updateColors();
      }
    };
  }

  private void updateColors() {
    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintVisible());
    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private void performSearch(final boolean promptUser) {
    try {
      performingSearch = true;
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
            selectAll();
          }
          catch (final Exception e) {
            DefaultDialogExceptionHandler.getInstance().displayException(e, Windows.getParentWindow(this));
          }
        }
      }
      updateColors();
    }
    finally {
      performingSearch = false;
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Control.builder()
            .command(() -> Dialogs.displayInDialog(EntitySearchField.this, settingsPanel, FrameworkMessages.get(FrameworkMessages.SETTINGS)))
            .name(FrameworkMessages.get(FrameworkMessages.SETTINGS))
            .build());

    return popupMenu;
  }

  /**
   * Necessary due to a bug on windows, where pressing Enter to dismiss this message
   * triggers another search, resulting in a loop
   */
  private void showEmptyResultMessage() {
    final Event<?> closeEvent = Event.event();
    final JButton okButton = Control.builder()
            .command(closeEvent::onEvent)
            .name(Messages.get(Messages.OK))
            .build().createButton();
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER)
            .onKeyPressed()
            .action(Control.control(okButton::doClick))
            .enable(okButton);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ESCAPE)
            .onKeyPressed()
            .action(Control.control(closeEvent::onEvent))
            .enable(okButton);
    final JPanel buttonPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonPanel.add(okButton);
    final JLabel messageLabel = new JLabel(FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
    messageLabel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE));
    final JPanel messagePanel = new JPanel(Layouts.borderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    messagePanel.add(buttonPanel, BorderLayout.SOUTH);
    Dialogs.displayInDialog(this, messagePanel, SwingMessages.get("OptionPane.messageDialogTitle"), closeEvent);
  }

  private static List<Entity> lookupEntities(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                             final boolean singleSelection, final JComponent dialogParent,
                                             final String lookupCaption, final String dialogTitle) {
    final EntitySearchModel searchModel = new DefaultEntitySearchModel(entityType, connectionProvider);
    searchModel.getMultipleSelectionEnabledValue().set(!singleSelection);
    final ComponentValuePanel<Entity, EntitySearchField> inputPanel = new ComponentValuePanel<>(lookupCaption,
            new SearchFieldValue(searchModel, null));
    Dialogs.displayInDialog(dialogParent, inputPanel, dialogTitle, Modal.YES,
            inputPanel.getOkAction(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return searchModel.getSelectedEntities();
    }

    return emptyList();
  }

  private static final class SettingsPanel extends JPanel {

    private SettingsPanel(final EntitySearchModel searchModel) {
      initializeUI(searchModel);
    }

    private void initializeUI(final EntitySearchModel searchModel) {
      final JPanel propertyBasePanel = new JPanel(new CardLayout(5, 5));
      final SwingFilteredComboBoxModel<Item<Attribute<String>>> propertyComboBoxModel = new SwingFilteredComboBoxModel<>();
      final EntityDefinition definition = searchModel.getConnectionProvider().getEntities().getDefinition(searchModel.getEntityType());
      for (final Map.Entry<Attribute<String>, EntitySearchModel.SearchSettings> entry :
              searchModel.getAttributeSearchSettings().entrySet()) {
        propertyComboBoxModel.addItem(Item.item(entry.getKey(), definition.getProperty(entry.getKey()).getCaption()));
        propertyBasePanel.add(initializePropertyPanel(entry.getValue()), entry.getKey().getName());
      }
      if (propertyComboBoxModel.getSize() > 0) {
        propertyComboBoxModel.addSelectionListener(selected ->
                ((CardLayout) propertyBasePanel.getLayout()).show(propertyBasePanel, selected.getValue().getName()));
        propertyComboBoxModel.setSelectedItem(propertyComboBoxModel.getElementAt(0));
      }

      final JCheckBox boxAllowMultipleValues = new JCheckBox(MESSAGES.getString("enable_multiple_search_values"));
      ComponentValue.booleanToggleButton(boxAllowMultipleValues).link(searchModel.getMultipleSelectionEnabledValue());
      final JTextField multipleValueSeparatorField = new JTextField(new SizedDocument(1), "", 1);
      ComponentValue.stringTextComponent(multipleValueSeparatorField).link(searchModel.getMultipleItemSeparatorValue());

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

    private static JPanel initializePropertyPanel(final EntitySearchModel.SearchSettings settings) {
      final JPanel panel = new JPanel(Layouts.gridLayout(3, 1));
      final JCheckBox boxCaseSensitive = new JCheckBox(MESSAGES.getString("case_sensitive"));
      ComponentValue.booleanToggleButton(boxCaseSensitive).link(settings.getCaseSensitiveValue());
      final JCheckBox boxPrefixWildcard = new JCheckBox(MESSAGES.getString("prefix_wildcard"));
      ComponentValue.booleanToggleButton(boxPrefixWildcard).link(settings.getWildcardPrefixValue());
      final JCheckBox boxPostfixWildcard = new JCheckBox(MESSAGES.getString("postfix_wildcard"));
      ComponentValue.booleanToggleButton(boxPostfixWildcard).link(settings.getWildcardPostfixValue());

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
     * @return a Control which sets the selected entities in the underlying {@link EntitySearchModel}
     * and disposes the selection dialog
     */
    Control getSelectControl();
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link JList}
   */
  public static class ListSelectionProvider implements SelectionProvider {

    private final JList<Entity> list = new JList<>();
    private final JScrollPane scrollPane = new JScrollPane(list);
    private final JPanel basePanel = new JPanel(Layouts.borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link JList} based {@link SelectionProvider}.
     * @param searchModel the {@link EntitySearchModel}
     */
    public ListSelectionProvider(final EntitySearchModel searchModel) {
      requireNonNull(searchModel, SEARCH_MODEL);
      this.selectControl = Control.builder().command(() -> {
        searchModel.setSelectedEntities(list.getSelectedValuesList());
        Windows.getParentDialog(list).dispose();
      }).name(Messages.get(Messages.OK)).build();
      list.setSelectionMode(searchModel.getMultipleSelectionEnabledValue().get() ?
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
    public final JComponent getSelectionComponent(final List<Entity> entities) {
      list.setListData(entities.toArray(new Entity[0]));
      list.removeSelectionInterval(0, list.getModel().getSize());
      list.scrollRectToVisible(list.getCellBounds(0, 0));

      return basePanel;
    }

    @Override
    public final void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public final Control getSelectControl() {
      return selectControl;
    }
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link EntityTablePanel}
   */
  public static class TableSelectionProvider implements SelectionProvider {

    private final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table;
    private final JScrollPane scrollPane;
    private final JPanel basePanel = new JPanel(Layouts.borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link FilteredTable} based {@link SelectionProvider}.
     * @param searchModel the {@link EntitySearchModel}
     */
    public TableSelectionProvider(final EntitySearchModel searchModel) {
      requireNonNull(searchModel, SEARCH_MODEL);
      final SwingEntityTableModel tableModel = new SwingEntityTableModel(searchModel.getEntityType(), searchModel.getConnectionProvider()) {
        @Override
        protected Collection<Entity> refreshItems() {
          return emptyList();
        }
      };
      table = new FilteredTable<>(tableModel);
      selectControl = Control.builder().command(() -> {
        searchModel.setSelectedEntities(tableModel.getSelectionModel().getSelectedItems());
        Windows.getParentDialog(table).dispose();
      }).name(Messages.get(Messages.OK)).build();
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      final String enterActionKey = "EntitySearchField.enter";
      table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), enterActionKey);
      table.getActionMap().put(enterActionKey, selectControl);
      final Collection<Attribute<String>> searchAttributes = searchModel.getSearchAttributes();
      tableModel.getColumnModel().setColumns(searchAttributes.toArray(new Attribute[0]));
      tableModel.getSortModel().setSortingDirective(searchAttributes.iterator().next(), SortingDirective.ASCENDING);
      table.setSelectionMode(searchModel.getMultipleSelectionEnabledValue().get() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      table.setDoubleClickAction(selectControl);
      scrollPane = new JScrollPane(table);
      basePanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * @return the underlying FilteredTablePanel
     */
    public final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> getTable() {
      return table;
    }

    @Override
    public final JComponent getSelectionComponent(final List<Entity> entities) {
      table.getModel().clear();
      table.getModel().addEntitiesAt(0, entities);
      table.scrollRectToVisible(table.getCellRect(0, 0, true));

      return basePanel;
    }

    @Override
    public final void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public final Control getSelectControl() {
      return selectControl;
    }
  }

  /**
   * A {@link is.codion.swing.common.ui.value.ComponentValue} implementation for Entity values based on a {@link EntitySearchField}.
   * @see EntitySearchField
   */
  public static final class SearchFieldValue extends AbstractComponentValue<Entity, EntitySearchField> {

    /**
     * Instantiates a new ComponentValue
     * @param searchModel the search model to base the search field on
     * @param initialValue the initial value
     */
    public SearchFieldValue(final EntitySearchModel searchModel, final Entity initialValue) {
      super(createEntitySearchField(searchModel, initialValue));
    }

    @Override
    protected Entity getComponentValue(final EntitySearchField component) {
      final List<Entity> selectedEntities = getComponent().getModel().getSelectedEntities();

      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setComponentValue(final EntitySearchField component, final Entity value) {
      component.getModel().setSelectedEntity(value);
    }

    private static EntitySearchField createEntitySearchField(final EntitySearchModel searchModel, final Entity initialValue) {
      final EntitySearchField field = new EntitySearchField(searchModel);
      searchModel.setSelectedEntity(initialValue);

      return field;
    }
  }

  private final class EnterKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (!model.searchStringRepresentsSelected() && e.getKeyCode() == KeyEvent.VK_ENTER) {
        e.consume();
        performSearch(true);
      }
    }
  }

  private final class EscapeKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (!model.searchStringRepresentsSelected() && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        e.consume();
        model.refreshSearchText();
        selectAll();
      }
    }
  }
}
