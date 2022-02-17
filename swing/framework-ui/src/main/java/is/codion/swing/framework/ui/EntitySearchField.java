/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.DefaultEntitySearchModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.ComponentValues;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.common.ui.textfield.TextFieldHint;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.swing.common.ui.Utilities.darker;
import static is.codion.swing.common.ui.control.Control.control;
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
  private final TextFieldHint searchHint;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = TransferFocusOnEnter.forwardAction(this);
  private final Action transferFocusBackwardAction = TransferFocusOnEnter.backwardAction(this);

  private SelectionProvider selectionProvider;

  private Color backgroundColor;
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
    setToolTipText(searchModel.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addFocusListener(initializeFocusListener());
    addKeyListener(new EnterKeyListener());
    addKeyListener(new EscapeKeyListener());
    this.searchHint = TextFieldHint.create(this, Messages.get(Messages.SEARCH_FIELD_HINT));
    configureColors();
    Utilities.linkToEnabledState(searchModel.getSearchStringRepresentsSelectedObserver(), transferFocusAction);
    Utilities.linkToEnabledState(searchModel.getSearchStringRepresentsSelectedObserver(), transferFocusBackwardAction);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (model != null) {
      configureColors();
      searchHint.updateHint();
    }
    if (selectionProvider != null) {
      selectionProvider.updateUI();
    }
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
   * Creates a new {@link ComponentValue} based on this {@link EntitySearchField}.
   * @return a new ComponentValue
   */
  public ComponentValue<Entity, EntitySearchField> componentValueSingle() {
    return new SearchFieldSingleValue(this);
  }

  /**
   * Creates a new {@link ComponentValue} based on this {@link EntitySearchField}.
   * @return a new ComponentValue
   */
  public ComponentValue<List<Entity>, EntitySearchField> componentValueMultiple() {
    return new SearchFieldMultipleValues(this);
  }

  /**
   * Performs a search for the given entity type, using a {@link EntitySearchField} displayed
   * in a dialog, using the default search attributes for the given entityType.
   * @param entityType the entityType of the entity to perform a search for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param dialogTitle the title to display on the dialog
   * @return the selected entity, an empty Optional in case none was selected
   * @throws is.codion.common.model.CancelException in case the user cancelled
   * @see EntityDefinition#getSearchAttributes()
   */
  public static Optional<Entity> lookupEntity(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                              final JComponent dialogParent, final String dialogTitle) {
    final List<Entity> entities = lookupEntities(entityType, connectionProvider, true, dialogParent, dialogTitle);

    return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
  }

  /**
   * Performs a search for the given entity type, using a {@link EntitySearchField} displayed
   * in a dialog, using the default search attributes for the given entityType.
   * @param entityType the entityType of the entity to perform a search for
   * @param connectionProvider the connection provider
   * @param dialogParent the component serving as the dialog parent
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities
   * @throws is.codion.common.model.CancelException in case the user cancelled
   * @see EntityDefinition#getSearchAttributes()
   */
  public static List<Entity> lookupEntities(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                            final JComponent dialogParent, final String dialogTitle) {
    return lookupEntities(entityType, connectionProvider, false, dialogParent, dialogTitle);
  }

  private void linkToModel() {
    ComponentValues.textComponent(this).link(model.getSearchStringValue());
    model.getSearchStringValue().addDataListener(searchString -> updateColors());
    model.addSelectedEntitiesListener(entities -> {
      setCaretPosition(0);
      if (entities.isEmpty()) {
        searchHint.updateHint();
      }
    });
  }

  private KeyEvents.Builder createForwardEvent() {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(transferFocusAction);
  }

  private KeyEvents.Builder createBackwardEvent() {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .modifiers(InputEvent.SHIFT_DOWN_MASK)
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

  private void configureColors() {
    final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
    this.backgroundColor = lookAndFeel.getDefaults().getColor("TextField.background");
    this.invalidBackgroundColor = darker(backgroundColor);
    updateColors();
  }

  private void updateColors() {
    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintVisible());
    setBackground(validBackground ? backgroundColor : invalidBackgroundColor);
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
            WaitCursor.show(this);
            try {
              queryResult = model.performQuery();
            }
            finally {
              WaitCursor.hide(this);
            }
            if (queryResult.size() == 1) {
              model.setSelectedEntities(queryResult);
            }
            else if (promptUser) {
              if (queryResult.isEmpty()) {
                showEmptyResultMessage();
              }
              else {
                selectionProvider.selectEntities(this, queryResult);
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
    popupMenu.add(Control.builder(() -> Dialogs.componentDialog(settingsPanel)
                    .owner(EntitySearchField.this)
                    .title(FrameworkMessages.get(FrameworkMessages.SETTINGS))
                    .show())
            .caption(FrameworkMessages.get(FrameworkMessages.SETTINGS))
            .build());

    return popupMenu;
  }

  /**
   * Necessary due to a bug on Windows, where pressing Enter to dismiss this message
   * triggers another search, resulting in a loop
   */
  private void showEmptyResultMessage() {
    final Event<?> closeEvent = Event.event();
    final JButton okButton = Components.button(control(closeEvent::onEvent))
            .caption(Messages.get(Messages.OK))
            .build();
    KeyEvents.builder(KeyEvent.VK_ENTER)
            .onKeyPressed()
            .action(control(okButton::doClick))
            .enable(okButton);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .onKeyPressed()
            .action(control(closeEvent::onEvent))
            .enable(okButton);
    final JPanel buttonPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonPanel.add(okButton);
    final JLabel messageLabel = new JLabel(FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
    messageLabel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE));
    final JPanel messagePanel = new JPanel(Layouts.borderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    messagePanel.add(buttonPanel, BorderLayout.SOUTH);
    Dialogs.componentDialog(messagePanel)
            .owner(this)
            .title(SwingMessages.get("OptionPane.messageDialogTitle"))
            .closeEvent(closeEvent)
            .show();
  }

  private static List<Entity> lookupEntities(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                             final boolean singleSelection, final JComponent dialogParent, final String dialogTitle) {
    final EntitySearchModel searchModel = new DefaultEntitySearchModel(entityType, connectionProvider);
    searchModel.getMultipleSelectionEnabledValue().set(!singleSelection);

    return new EntitySearchField(searchModel).componentValueMultiple().showDialog(dialogParent, dialogTitle);
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

      final JPanel generalSettingsPanel = new JPanel(Layouts.gridLayout(2, 1));
      generalSettingsPanel.setBorder(BorderFactory.createTitledBorder(""));
      generalSettingsPanel.add(Components.checkBox(searchModel.getMultipleSelectionEnabledValue())
              .caption(MESSAGES.getString("enable_multiple_search_values"))
              .build());

      final JPanel valueSeparatorPanel = new JPanel(Layouts.borderLayout());
      valueSeparatorPanel.add(new JLabel(MESSAGES.getString("multiple_search_value_separator")), BorderLayout.CENTER);
      valueSeparatorPanel.add(Components.textField(searchModel.getMultipleItemSeparatorValue())
              .columns(1)
              .maximumLength(1)
              .build(), BorderLayout.WEST);

      generalSettingsPanel.add(valueSeparatorPanel);

      setLayout(Layouts.borderLayout());
      add(new JComboBox<>(propertyComboBoxModel), BorderLayout.NORTH);
      add(propertyBasePanel, BorderLayout.CENTER);
      add(generalSettingsPanel, BorderLayout.SOUTH);
    }

    private static JPanel initializePropertyPanel(final EntitySearchModel.SearchSettings settings) {
      final JPanel panel = new JPanel(Layouts.gridLayout(3, 1));
      panel.add(Components.checkBox(settings.getCaseSensitiveValue())
              .caption(MESSAGES.getString("case_sensitive"))
              .build());
      panel.add(Components.checkBox(settings.getWildcardPrefixValue())
              .caption(MESSAGES.getString("prefix_wildcard"))
              .build());
      panel.add(Components.checkBox(settings.getWildcardPostfixValue())
              .caption(MESSAGES.getString("postfix_wildcard"))
              .build());

      return panel;
    }
  }

  /**
   * Provides a way for the user to select one or more of a given set of entities
   */
  public interface SelectionProvider {

    /**
     * Displays a dialog for selecting from the given entities.
     * @param dialogOwner the dialog owner
     * @param entities the entities to select from
     */
    void selectEntities(JComponent dialogOwner, List<Entity> entities);

    /**
     * Sets the preferred size of the selection component.
     * @param preferredSize the preferred selection component size
     */
    void setPreferredSize(Dimension preferredSize);

    /**
     * Updates the UI of all the components used in this selection provider.
     */
    void updateUI();
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link JList}
   */
  public static class ListSelectionProvider implements SelectionProvider {

    private final DefaultListModel<Entity> listModel = new DefaultListModel<>();
    private final JList<Entity> list = new JList<>(listModel);
    private final JScrollPane scrollPane = new JScrollPane(list);
    private final JPanel basePanel = new JPanel(Layouts.borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link JList} based {@link SelectionProvider}.
     * @param searchModel the {@link EntitySearchModel}
     */
    public ListSelectionProvider(final EntitySearchModel searchModel) {
      requireNonNull(searchModel, SEARCH_MODEL);
      selectControl = Control.builder(createSelectCommand(searchModel))
              .caption(Messages.get(Messages.OK))
              .build();
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
    public final void selectEntities(final JComponent dialogOwner, final List<Entity> entities) {
      requireNonNull(entities).forEach(listModel::addElement);
      list.scrollRectToVisible(list.getCellBounds(0, 0));

      Dialogs.okCancelDialog(basePanel)
              .owner(dialogOwner)
              .title(MESSAGES.getString("select_entity"))
              .okAction(selectControl)
              .show();

      listModel.removeAllElements();
    }

    @Override
    public final void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public final void updateUI() {
      Utilities.updateUI(basePanel, list, scrollPane, scrollPane.getVerticalScrollBar(), scrollPane.getHorizontalScrollBar());
    }

    private Control.Command createSelectCommand(final EntitySearchModel searchModel) {
      return () -> {
        searchModel.setSelectedEntities(list.getSelectedValuesList());
        Windows.getParentDialog(list).dispose();
      };
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
      selectControl = Control.builder(createSelectCommand(searchModel, tableModel))
              .caption(Messages.get(Messages.OK))
              .build();
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .onKeyPressed()
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectControl)
              .enable(table);
      final Collection<Attribute<String>> searchAttributes = searchModel.getSearchAttributes();
      tableModel.getColumnModel().setColumns(searchAttributes.toArray(new Attribute[0]));
      tableModel.getSortModel().setSortOrder(searchAttributes.iterator().next(), SortOrder.ASCENDING);
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
    public final void selectEntities(final JComponent dialogOwner, final List<Entity> entities) {
      table.getModel().addEntitiesAt(0, requireNonNull(entities));
      table.scrollRectToVisible(table.getCellRect(0, 0, true));

      Dialogs.okCancelDialog(basePanel)
              .owner(dialogOwner)
              .title(MESSAGES.getString("select_entity"))
              .okAction(selectControl)
              .show();

      table.getModel().clear();
    }

    @Override
    public final void setPreferredSize(final Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public void updateUI() {
      Utilities.updateUI(basePanel, table, scrollPane, scrollPane.getVerticalScrollBar(), scrollPane.getHorizontalScrollBar());
    }

    private Control.Command createSelectCommand(final EntitySearchModel searchModel, final SwingEntityTableModel tableModel) {
      return () -> {
        searchModel.setSelectedEntities(tableModel.getSelectionModel().getSelectedItems());
        Windows.getParentDialog(table).dispose();
      };
    }
  }

  private static final class SearchFieldSingleValue extends AbstractComponentValue<Entity, EntitySearchField> {

    private SearchFieldSingleValue(final EntitySearchField searchField) {
      super(searchField);
      searchField.getModel().addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    protected Entity getComponentValue(final EntitySearchField component) {
      final List<Entity> selectedEntities = component.getModel().getSelectedEntities();

      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setComponentValue(final EntitySearchField component, final Entity value) {
      component.getModel().setSelectedEntity(value);
    }
  }

  private static final class SearchFieldMultipleValues extends AbstractComponentValue<List<Entity>, EntitySearchField> {

    private SearchFieldMultipleValues(final EntitySearchField searchField) {
      super(searchField);
      searchField.getModel().addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    protected List<Entity> getComponentValue(final EntitySearchField component) {
      return component.getModel().getSelectedEntities();
    }

    @Override
    protected void setComponentValue(final EntitySearchField component, final List<Entity> value) {
      component.getModel().setSelectedEntities(value);
    }
  }

  private final class EnterKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER && !model.searchStringRepresentsSelected()) {
        e.consume();
        performSearch(true);
      }
    }
  }

  private final class EscapeKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !model.searchStringRepresentsSelected()) {
        e.consume();
        model.refreshSearchText();
        selectAll();
      }
    }
  }
}
