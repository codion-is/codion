/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.value.AbstractValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.text.HintTextField;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import java.util.function.Function;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.text.TextComponents.selectAllOnFocusGained;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the EntitySearchModel.
 * The search is triggered by the ENTER key and behaves in the following way:
 * If the search result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog allowing either a single or multiple
 * selection based on the search model settings.
 * {@link ListSelectionProvider} is the default {@link SelectionProvider}.
 * Use {@link EntitySearchField#builder(EntitySearchModel)} or {@link EntitySearchField#builder(EntityType, EntityConnectionProvider)} for a builder instance.
 * @see EntitySearchModel
 * @see #builder(EntityType, EntityConnectionProvider)
 * @see #builder(EntitySearchModel)
 * @see #singleSelectionValue()
 * @see #multiSelectionValue()
 * @see #setSelectionProvider(SelectionProvider)
 */
public final class EntitySearchField extends HintTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntitySearchField.class.getName());

  private final EntitySearchModel model;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = TransferFocusOnEnter.forwardAction();
  private final Action transferFocusBackwardAction = TransferFocusOnEnter.backwardAction();

  private SingleSelectionValue singleSelectionValue;
  private MultiSelectionValue multiSelectionValue;
  private SelectionProvider selectionProvider;

  private Color backgroundColor;
  private Color invalidBackgroundColor;
  private boolean performingSearch = false;
  private boolean searchOnFocusLost = true;

  private EntitySearchField(EntitySearchModel searchModel, boolean searchHintEnabled) {
    super(searchHintEnabled ? Messages.search() + "..." : null);
    requireNonNull(searchModel);
    this.model = searchModel;
    this.settingsPanel = new SettingsPanel(searchModel);
    this.selectionProvider = new ListSelectionProvider(model);
    linkToModel();
    setToolTipText(searchModel.description());
    setComponentPopupMenu(createPopupMenu());
    addFocusListener(new SearchFocusListener());
    addKeyListener(new EnterEscapeListener());
    configureColors();
    Utilities.linkToEnabledState(searchModel.searchStringModified().not(), transferFocusAction, transferFocusBackwardAction);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (model != null) {
      configureColors();
    }
    if (selectionProvider != null) {
      selectionProvider.updateUI();
    }
  }

  /**
   * @return the search model this search field is based on
   */
  public EntitySearchModel model() {
    return model;
  }

  /**
   * Sets the SelectionProvider, that is, the object responsible for providing the component used
   * for selecting items from the search result.
   * @param selectionProvider the {@link SelectionProvider} implementation to use when presenting
   * a selection dialog to the user
   * @throws NullPointerException in case {@code selectionProvider} is null
   */
  public void setSelectionProvider(SelectionProvider selectionProvider) {
    this.selectionProvider = requireNonNull(selectionProvider);
  }

  /**
   * @return true if this field triggers a search when it loses focus
   */
  public boolean isSearchOnFocusLost() {
    return searchOnFocusLost;
  }

  /**
   * @param searchOnFocusLost true if this field should trigger a search when it loses focus
   */
  public void setSearchOnFocusLost(boolean searchOnFocusLost) {
    this.searchOnFocusLost = searchOnFocusLost;
  }

  /**
   * @return a {@link ComponentValue} for selecting a single entity
   */
  public ComponentValue<Entity, EntitySearchField> singleSelectionValue() {
    if (singleSelectionValue == null) {
      singleSelectionValue = new SingleSelectionValue(this);
    }

    return singleSelectionValue;
  }

  /**
   * @return a {@link ComponentValue} for selecting multiple entities
   */
  public ComponentValue<Collection<Entity>, EntitySearchField> multiSelectionValue() {
    if (multiSelectionValue == null) {
      multiSelectionValue = new MultiSelectionValue(this);
    }

    return multiSelectionValue;
  }

  /**
   * Instantiates a new {@link EntitySearchField.Builder}
   * @param entityType the entity type
   * @param connectionProvider the connection provider
   * @return a new builder instance
   */
  public static Builder builder(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return new DefaultEntitySearchFieldBuilder(EntitySearchModel.builder(entityType, connectionProvider).build());
  }

  /**
   * Instantiates a new {@link EntitySearchField.Builder}
   * @param searchModel the search model on which to base the search field
   * @return a new builder instance
   */
  public static Builder builder(EntitySearchModel searchModel) {
    return new DefaultEntitySearchFieldBuilder(requireNonNull(searchModel));
  }

  /**
   * Builds a entity search field.
   */
  public interface Builder extends ComponentBuilder<Entity, EntitySearchField, Builder> {

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
     * @param selectionProviderFactory the selection provider factory to use
     * @return this builder instance
     */
    Builder selectionProviderFactory(Function<EntitySearchModel, SelectionProvider> selectionProviderFactory);
  }

  private void linkToModel() {
    new SearchStringValue(this).link(model.searchString());
    model.searchString().addDataListener(searchString -> updateColors());
    model.selectedEntities().addListener(() -> setCaretPosition(0));
  }

  private void configureColors() {
    this.backgroundColor = UIManager.getColor("TextField.background");
    this.invalidBackgroundColor = darker(backgroundColor);
    updateColors();
  }

  private void updateColors() {
    boolean validBackground = !model.searchStringModified().get();
    setBackground(validBackground ? backgroundColor : invalidBackgroundColor);
  }

  private void performSearch(boolean promptUser) {
    try {
      performingSearch = true;
      if (nullOrEmpty(model.searchString().get())) {
        model.selectedEntities().set(null);
      }
      else {
        if (model.searchStringModified().get()) {
          try {
            List<Entity> queryResult = performSearch();
            if (queryResult.size() == 1) {
              model.selectedEntities().set(queryResult);
            }
            else if (promptUser) {
              if (queryResult.isEmpty()) {
                JOptionPane.showMessageDialog(this, FrameworkMessages.noResultsFound(),
                        SwingMessages.get("OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE);
              }
              else {
                selectionProvider.selectEntities(this, queryResult);
              }
            }
            selectAll();
          }
          catch (Exception e) {
            Dialogs.displayExceptionDialog(e, Utilities.parentWindow(this));
          }
        }
      }
      updateColors();
    }
    finally {
      performingSearch = false;
    }
  }

  private List<Entity> performSearch() {
    WaitCursor.show(this);
    try {
      return model.search();
    }
    finally {
      WaitCursor.hide(this);
    }
  }

  private JPopupMenu createPopupMenu() {
    return menu(Controls.controls(Control.builder(() -> Dialogs.componentDialog(settingsPanel)
                    .owner(EntitySearchField.this)
                    .title(FrameworkMessages.settings())
                    .icon(FrameworkIcons.instance().settings())
                    .show())
            .name(FrameworkMessages.settings())
            .smallIcon(FrameworkIcons.instance().settings())
            .build()))
            .createPopupMenu();
  }

  private static final class SearchStringValue extends AbstractValue<String> {

    private final JTextField searchField;

    private SearchStringValue(JTextField searchField) {
      this.searchField = searchField;
      this.searchField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
    }

    @Override
    public String get() {
      return searchField.getText();
    }

    @Override
    protected void setValue(String value) {
      searchField.setText(value);
    }
  }

  private static final class SettingsPanel extends JPanel {

    private SettingsPanel(EntitySearchModel searchModel) {
      initializeUI(searchModel);
    }

    private void initializeUI(EntitySearchModel searchModel) {
      CardLayout cardLayout = new CardLayout(5, 5);
      PanelBuilder columnBasePanelBuilder = Components.panel(cardLayout);
      FilteredComboBoxModel<Item<Column<String>>> columnComboBoxModel = new FilteredComboBoxModel<>();
      EntityDefinition definition = searchModel.connectionProvider().entities().definition(searchModel.entityType());
      for (Map.Entry<Column<String>, EntitySearchModel.SearchSettings> entry :
              searchModel.columnSearchSettings().entrySet()) {
        columnComboBoxModel.addItem(Item.item(entry.getKey(), definition.columns().definition(entry.getKey()).caption()));
        columnBasePanelBuilder.add(createColumnSettingsPanel(entry.getValue()), entry.getKey().name());
      }
      JPanel columnBasePanel = columnBasePanelBuilder.build();
      if (columnComboBoxModel.getSize() > 0) {
        columnComboBoxModel.addSelectionListener(selected -> cardLayout.show(columnBasePanel, selected.get().name()));
        columnComboBoxModel.setSelectedItem(columnComboBoxModel.getElementAt(0));
      }

      JPanel generalSettingsPanel = Components.gridLayoutPanel(2, 1)
              .border(BorderFactory.createTitledBorder(""))
              .add(Components.checkBox(searchModel.singleSelection())
                      .text(MESSAGES.getString("single_selection"))
                      .build())
              .build();

      JPanel separatorPanel = Components.borderLayoutPanel()
              .centerComponent(new JLabel(MESSAGES.getString("multiple_item_separator")))
              .westComponent(Components.textField(searchModel.separator())
                      .columns(1)
                      .maximumLength(1)
                      .build())
              .build();

      generalSettingsPanel.add(separatorPanel);

      setLayout(borderLayout());
      add(Components.comboBox(columnComboBoxModel).build(), BorderLayout.NORTH);
      add(columnBasePanel, BorderLayout.CENTER);
      add(generalSettingsPanel, BorderLayout.SOUTH);
      setBorder(emptyBorder());
    }

    private static JPanel createColumnSettingsPanel(EntitySearchModel.SearchSettings settings) {
      return Components.gridLayoutPanel(3, 1)
              .add(Components.checkBox(settings.caseSensitive())
                      .text(MESSAGES.getString("case_sensitive"))
                      .build())
              .add(Components.checkBox(settings.wildcardPrefix())
                      .text(MESSAGES.getString("prefix_wildcard"))
                      .build())
              .add(Components.checkBox(settings.wildcardPostfix())
                      .text(MESSAGES.getString("postfix_wildcard"))
                      .build())
              .build();
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
  public static final class ListSelectionProvider implements SelectionProvider {

    private final DefaultListModel<Entity> listModel = new DefaultListModel<>();
    private final JList<Entity> list = new JList<>(listModel);
    private final JScrollPane scrollPane = new JScrollPane(list);
    private final JPanel basePanel = new JPanel(borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link JList} based {@link SelectionProvider}.
     * @param searchModel the {@link EntitySearchModel}
     */
    public ListSelectionProvider(EntitySearchModel searchModel) {
      selectControl = Control.builder(new SelectCommand(requireNonNull(searchModel), list))
              .name(Messages.ok())
              .build();
      list.setSelectionMode(searchModel.singleSelection().get() ?
              ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
      list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            selectControl.actionPerformed(null);
          }
        }
      });
      basePanel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void selectEntities(JComponent dialogOwner, List<Entity> entities) {
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
    public void setPreferredSize(Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public void updateUI() {
      Utilities.updateUI(basePanel, list, scrollPane, scrollPane.getVerticalScrollBar(), scrollPane.getHorizontalScrollBar());
    }

    private static final class SelectCommand implements Control.Command {

      private final EntitySearchModel searchModel;
      private final JList<Entity> list;

      private SelectCommand(EntitySearchModel searchModel, JList<Entity> list) {
        this.searchModel = searchModel;
        this.list = list;
      }

      @Override
      public void perform() {
        searchModel.selectedEntities().set(list.getSelectedValuesList());
        Utilities.disposeParentWindow(list);
      }
    }
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link FilteredTable}
   */
  public static class TableSelectionProvider implements SelectionProvider {

    private final FilteredTable<Entity, Attribute<?>> table;
    private final JScrollPane scrollPane;
    private final JPanel searchPanel = new JPanel(borderLayout());
    private final JPanel basePanel = new JPanel(borderLayout());
    private final Control selectControl;

    /**
     * Instantiates a new {@link FilteredTable} based {@link SelectionProvider}.
     * @param searchModel the {@link EntitySearchModel}
     */
    public TableSelectionProvider(EntitySearchModel searchModel) {
      requireNonNull(searchModel);
      SwingEntityTableModel tableModel = new SwingEntityTableModel(searchModel.entityType(), searchModel.connectionProvider()) {
        @Override
        protected Collection<Entity> refreshItems() {
          return emptyList();
        }
      };
      selectControl = Control.builder(createSelectCommand(searchModel, tableModel))
              .name(Messages.ok())
              .build();
      table = FilteredTable.builder(tableModel)
              .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
              .selectionMode(searchModel.singleSelection().get() ?
                      ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
              .doubleClickAction(selectControl)
              .build();
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectControl)
              .enable(table);
      KeyEvents.builder(VK_ENTER)
              .action(selectControl)
              .enable(table.searchField());
      KeyEvents.builder(VK_F)
              .modifiers(CTRL_DOWN_MASK)
              .action(Control.control(table.searchField()::requestFocusInWindow))
              .enable(table);
      tableModel.columnModel().columns().forEach(this::configureColumn);
      Collection<Column<String>> searchColumns = searchModel.searchColumns();
      tableModel.columnModel().setVisibleColumns(searchColumns.toArray(new Attribute[0]));
      tableModel.sortModel().setSortOrder(searchColumns.iterator().next(), SortOrder.ASCENDING);
      scrollPane = new JScrollPane(table);
      searchPanel.add(table.searchField(), BorderLayout.WEST);
      basePanel.add(scrollPane, BorderLayout.CENTER);
      basePanel.add(searchPanel, BorderLayout.SOUTH);
      int gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
      basePanel.setBorder(BorderFactory.createEmptyBorder(gap, gap, 0, gap));
    }

    /**
     * @return the underlying FilteredTable
     */
    public final FilteredTable<Entity, Attribute<?>> table() {
      return table;
    }

    @Override
    public final void selectEntities(JComponent dialogOwner, List<Entity> entities) {
      table.getModel().addItemsAt(0, requireNonNull(entities));
      table.scrollRectToVisible(table.getCellRect(0, 0, true));

      Dialogs.okCancelDialog(basePanel)
              .owner(dialogOwner)
              .title(MESSAGES.getString("select_entity"))
              .okAction(selectControl)
              .show();

      table.getModel().clear();
      table.searchField().setText("");
    }

    @Override
    public final void setPreferredSize(Dimension preferredSize) {
      basePanel.setPreferredSize(preferredSize);
    }

    @Override
    public void updateUI() {
      Utilities.updateUI(basePanel, searchPanel, table, scrollPane, scrollPane.getVerticalScrollBar(), scrollPane.getHorizontalScrollBar());
    }

    private Control.Command createSelectCommand(EntitySearchModel searchModel, SwingEntityTableModel tableModel) {
      return () -> {
        searchModel.selectedEntities().set(tableModel.selectionModel().getSelectedItems());
        Utilities.disposeParentWindow(table);
      };
    }

    private void configureColumn(FilteredTableColumn<Attribute<?>> column) {
      column.setCellRenderer(EntityTableCellRenderer.builder((SwingEntityTableModel) table.getModel(), column.getIdentifier()).build());
    }
  }

  private static final class SingleSelectionValue extends AbstractComponentValue<Entity, EntitySearchField> {

    private SingleSelectionValue(EntitySearchField searchField) {
      super(searchField);
      searchField.model().selectedEntity().addListener(this::notifyListeners);
    }

    @Override
    protected Entity getComponentValue() {
      return component().model().selectedEntity().get();
    }

    @Override
    protected void setComponentValue(Entity value) {
      component().model().selectedEntity().set(value);
    }
  }

  private static final class MultiSelectionValue extends AbstractComponentValue<Collection<Entity>, EntitySearchField> {

    private MultiSelectionValue(EntitySearchField searchField) {
      super(searchField);
      searchField.model().selectedEntities().addListener(this::notifyListeners);
    }

    @Override
    protected Collection<Entity> getComponentValue() {
      return component().model().selectedEntities().get();
    }

    @Override
    protected void setComponentValue(Collection<Entity> value) {
      component().model().selectedEntities().set(value);
    }
  }

  private final class SearchFocusListener implements FocusListener {

    @Override
    public void focusGained(FocusEvent e) {
      updateColors();
    }

    @Override
    public void focusLost(FocusEvent e) {
      if (!e.isTemporary()) {
        if (getText().isEmpty()) {
          model().selectedEntities().set(null);
        }
        else if (shouldPerformSearch()) {
          performSearch(false);
        }
      }
      updateColors();
    }

    private boolean shouldPerformSearch() {
      return searchOnFocusLost && !performingSearch && model.searchStringModified().get();
    }
  }

  private final class EnterEscapeListener extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (model.searchStringModified().get()) {
        if (e.getKeyCode() == VK_ENTER) {
          e.consume();
          performSearch(true);
        }
        else if (e.getKeyCode() == VK_ESCAPE) {
          e.consume();
          model.resetSearchString();
          selectAll();
        }
      }
    }
  }

  private static final class DefaultEntitySearchFieldBuilder extends AbstractComponentBuilder<Entity, EntitySearchField, Builder> implements Builder {

    private final EntitySearchModel searchModel;

    private int columns = TextFieldBuilder.DEFAULT_TEXT_FIELD_COLUMNS.get();
    private boolean upperCase;
    private boolean lowerCase;
    private boolean searchHintEnabled = true;
    private boolean searchOnFocusLost = true;
    private Function<EntitySearchModel, SelectionProvider> selectionProviderFactory;

    private DefaultEntitySearchFieldBuilder(EntitySearchModel searchModel) {
      this.searchModel = searchModel;
    }

    @Override
    public Builder columns(int columns) {
      this.columns = columns;
      return this;
    }

    @Override
    public Builder upperCase(boolean upperCase) {
      if (upperCase && lowerCase) {
        throw new IllegalArgumentException("Field is already lowercase");
      }
      this.upperCase = upperCase;
      return this;
    }

    @Override
    public Builder lowerCase(boolean lowerCase) {
      if (lowerCase && upperCase) {
        throw new IllegalArgumentException("Field is already uppercase");
      }
      this.lowerCase = lowerCase;
      return this;
    }

    @Override
    public Builder searchHintEnabled(boolean searchHintEnabled) {
      this.searchHintEnabled = searchHintEnabled;
      return this;
    }

    @Override
    public Builder searchOnFocusLost(boolean searchOnFocusLost) {
      this.searchOnFocusLost = searchOnFocusLost;
      return this;
    }

    @Override
    public Builder selectionProviderFactory(Function<EntitySearchModel, SelectionProvider> selectionProviderFactory) {
      this.selectionProviderFactory = requireNonNull(selectionProviderFactory);
      return this;
    }

    @Override
    protected EntitySearchField createComponent() {
      EntitySearchField searchField = new EntitySearchField(searchModel, searchHintEnabled);
      searchField.setColumns(columns);
      if (upperCase) {
        TextComponents.upperCase(searchField.getDocument());
      }
      if (lowerCase) {
        TextComponents.lowerCase(searchField.getDocument());
      }
      searchField.setSearchOnFocusLost(searchOnFocusLost);
      if (selectionProviderFactory != null) {
        searchField.setSelectionProvider(selectionProviderFactory.apply(searchField.model()));
      }
      selectAllOnFocusGained(searchField);

      return searchField;
    }

    @Override
    protected ComponentValue<Entity, EntitySearchField> createComponentValue(EntitySearchField component) {
      return component.singleSelectionValue();
    }

    @Override
    protected void setInitialValue(EntitySearchField component, Entity initialValue) {
      component.model().selectedEntity().set(initialValue);
    }

    @Override
    protected void enableTransferFocusOnEnter(EntitySearchField component) {
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_FOCUSED)
              .action(component.transferFocusAction)
              .enable(component);
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_FOCUSED)
              .modifiers(SHIFT_DOWN_MASK)
              .action(component.transferFocusBackwardAction)
              .enable(component);
    }
  }
}
