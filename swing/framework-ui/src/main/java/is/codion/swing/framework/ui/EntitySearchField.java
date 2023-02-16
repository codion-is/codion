/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.value.AbstractValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;
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
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.text.HintTextField;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
import java.util.function.Function;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.Utilities.darker;
import static is.codion.swing.common.ui.component.text.TextComponents.selectAllOnFocusGained;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.*;
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
 * Use {@link EntitySearchField#builder(EntitySearchModel)} for a builder instance.
 * @see EntitySearchModel
 * @see #builder(EntitySearchModel)
 * @see #lookupDialogBuilder(EntityType, EntityConnectionProvider)
 * @see #setSelectionProvider(SelectionProvider)
 */
public final class EntitySearchField extends HintTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntitySearchField.class.getName());

  private static final int BORDER_SIZE = 15;

  private final EntitySearchModel model;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = TransferFocusOnEnter.forwardAction();
  private final Action transferFocusBackwardAction = TransferFocusOnEnter.backwardAction();

  private SelectionProvider selectionProvider;

  private Color backgroundColor;
  private Color invalidBackgroundColor;
  private boolean performingSearch = false;

  private EntitySearchField(EntitySearchModel searchModel, boolean searchHintEnabled) {
    super(searchHintEnabled ? Messages.search() + "..." : null);
    requireNonNull(searchModel);
    this.model = searchModel;
    this.settingsPanel = new SettingsPanel(searchModel);
    this.selectionProvider = new ListSelectionProvider(model);
    linkToModel();
    setToolTipText(searchModel.getDescription());
    setComponentPopupMenu(createPopupMenu());
    addFocusListener(new SearchFocusListener());
    addKeyListener(new EnterEscapeListener());
    configureColors();
    Utilities.linkToEnabledState(searchModel.searchStringRepresentsSelectedObserver(), transferFocusAction);
    Utilities.linkToEnabledState(searchModel.searchStringRepresentsSelectedObserver(), transferFocusBackwardAction);
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
   * Initializes a new {@link EntitySearchField.Builder}
   * @param searchModel the search model on which to base the search field
   * @return a new builder instance
   */
  public static Builder builder(EntitySearchModel searchModel) {
    return new DefaultEntitySearchFieldBuilder(requireNonNull(searchModel));
  }

  /**
   * Instantiates a new {@link LookupDialogBuilder} instance
   * @param entityType the entity type to lookup
   * @param connectionProvider the connection provider
   * @return a new {@link LookupDialogBuilder} instance
   */
  public static LookupDialogBuilder lookupDialogBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return new DefaultLookupDialogBuilder(entityType, connectionProvider);
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
     * @param selectionProviderFactory the selection provider factory to use
     * @return this builder instance
     */
    Builder selectionProviderFactory(Function<EntitySearchModel, SelectionProvider> selectionProviderFactory);

    /**
     * Creates a new {@link ComponentValue} based on this {@link EntitySearchField}, for multiple values.
     * @return a new ComponentValue
     */
    ComponentValue<List<Entity>, EntitySearchField> buildComponentValueMultiple();
  }

  /**
   * A builder for a dialog for performing a entity lookup via a {@link EntitySearchField}.
   * @see EntityDefinition#searchAttributes()
   */
  public interface LookupDialogBuilder {

    /**
     * @param owner the dialog owner
     * @return this builder instance
     */
    LookupDialogBuilder owner(JComponent owner);

    /**
     * @param title the title to display on the dialog
     * @return this builder instance
     */
    LookupDialogBuilder title(String title);

    /**
     * If no search field is specified one is created.
     * @param searchField the search field to use for lookup
     * @return this builder instance
     * @throws IllegalArgumentException in case the search field entity type does not match the builder entity type
     */
    LookupDialogBuilder searchField(EntitySearchField searchField);

    /**
     * @return the selected entity, an empty Optional in case none was selected
     * @throws is.codion.common.model.CancelException in case the user cancelled
     */
    Optional<Entity> lookupSingle();

    /**
     * @return the selected entities
     * @throws is.codion.common.model.CancelException in case the user cancelled
     */
    List<Entity> lookup();
  }

  private void linkToModel() {
    new SearchFieldValue(this).link(model.searchStringValue());
    model.searchStringValue().addDataListener(searchString -> updateColors());
    model.addSelectedEntitiesListener(entities -> setCaretPosition(0));
  }

  private void configureColors() {
    this.backgroundColor = UIManager.getColor("TextField.background");
    this.invalidBackgroundColor = darker(backgroundColor);
    updateColors();
  }

  private void updateColors() {
    boolean validBackground = model.searchStringRepresentsSelected();
    setBackground(validBackground ? backgroundColor : invalidBackgroundColor);
  }

  private void performSearch(boolean promptUser) {
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
          catch (Exception e) {
            Dialogs.displayExceptionDialog(e, Utilities.getParentWindow(this));
          }
        }
      }
      updateColors();
    }
    finally {
      performingSearch = false;
    }
  }

  private JPopupMenu createPopupMenu() {
    JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(Control.builder(() -> Dialogs.componentDialog(settingsPanel)
                    .owner(EntitySearchField.this)
                    .title(FrameworkMessages.settings())
                    .icon(FrameworkIcons.instance().settings())
                    .show())
            .caption(FrameworkMessages.settings())
            .smallIcon(FrameworkIcons.instance().settings())
            .build());

    return popupMenu;
  }

  /**
   * Necessary due to a bug on Windows, where pressing Enter to dismiss this message
   * triggers another search, resulting in a loop
   */
  private void showEmptyResultMessage() {
    Event<?> closeEvent = Event.event();
    JButton okButton = Components.button(control(closeEvent::onEvent))
            .caption(Messages.ok())
            .build();
    KeyEvents.builder(KeyEvent.VK_ENTER)
            .action(control(okButton::doClick))
            .enable(okButton);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .action(control(closeEvent::onEvent))
            .enable(okButton);
    JPanel buttonPanel = Components.panel(flowLayout(FlowLayout.CENTER))
            .add(okButton)
            .build();
    JLabel messageLabel = Components.label(FrameworkMessages.noResultsFromCondition())
            .border(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE))
            .build();
    JPanel messagePanel = Components.panel(borderLayout())
            .add(messageLabel, BorderLayout.CENTER)
            .add(buttonPanel, BorderLayout.SOUTH)
            .build();

    Dialogs.componentDialog(messagePanel)
            .owner(this)
            .title(SwingMessages.get("OptionPane.messageDialogTitle"))
            .closeEvent(closeEvent)
            .show();
  }

  private static final class SearchFieldValue extends AbstractValue<String> {

    private final JTextField searchField;

    private SearchFieldValue(JTextField searchField) {
      this.searchField = searchField;
      this.searchField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
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
      PanelBuilder propertyBasePanelBuilder = Components.panel(cardLayout);
      FilteredComboBoxModel<Item<Attribute<String>>> propertyComboBoxModel = new FilteredComboBoxModel<>();
      EntityDefinition definition = searchModel.connectionProvider().entities().definition(searchModel.entityType());
      for (Map.Entry<Attribute<String>, EntitySearchModel.SearchSettings> entry :
              searchModel.attributeSearchSettings().entrySet()) {
        propertyComboBoxModel.addItem(Item.item(entry.getKey(), definition.property(entry.getKey()).caption()));
        propertyBasePanelBuilder.add(createPropertyPanel(entry.getValue()), entry.getKey().name());
      }
      JPanel propertyBasePanel = propertyBasePanelBuilder.build();
      if (propertyComboBoxModel.getSize() > 0) {
        propertyComboBoxModel.addSelectionListener(selected -> cardLayout.show(propertyBasePanel, selected.value().name()));
        propertyComboBoxModel.setSelectedItem(propertyComboBoxModel.getElementAt(0));
      }

      JPanel generalSettingsPanel = Components.panel(gridLayout(2, 1))
              .border(BorderFactory.createTitledBorder(""))
              .add(Components.checkBox(searchModel.multipleSelectionEnabledState())
                      .caption(MESSAGES.getString("enable_multiple_search_values"))
                      .build())
              .build();

      JPanel valueSeparatorPanel = Components.panel(borderLayout())
              .add(new JLabel(MESSAGES.getString("multiple_search_value_separator")), BorderLayout.CENTER)
              .add(Components.textField(searchModel.multipleItemSeparatorValue())
                      .columns(1)
                      .maximumLength(1)
                      .build(), BorderLayout.WEST)
              .build();

      generalSettingsPanel.add(valueSeparatorPanel);

      setLayout(borderLayout());
      add(Components.comboBox(propertyComboBoxModel).build(), BorderLayout.NORTH);
      add(propertyBasePanel, BorderLayout.CENTER);
      add(generalSettingsPanel, BorderLayout.SOUTH);
      int gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
      setBorder(BorderFactory.createEmptyBorder(gap, gap, 0, gap));
    }

    private static JPanel createPropertyPanel(EntitySearchModel.SearchSettings settings) {
      return Components.panel(gridLayout(3, 1))
              .add(Components.checkBox(settings.caseSensitiveState())
                      .caption(MESSAGES.getString("case_sensitive"))
                      .build())
              .add(Components.checkBox(settings.wildcardPrefixState())
                      .caption(MESSAGES.getString("prefix_wildcard"))
                      .build())
              .add(Components.checkBox(settings.wildcardPostfixState())
                      .caption(MESSAGES.getString("postfix_wildcard"))
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
      requireNonNull(searchModel);
      selectControl = Control.builder(createSelectCommand(searchModel))
              .caption(Messages.ok())
              .build();
      list.setSelectionMode(searchModel.multipleSelectionEnabledState().get() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
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

    private Control.Command createSelectCommand(EntitySearchModel searchModel) {
      return () -> {
        searchModel.setSelectedEntities(list.getSelectedValuesList());
        Utilities.disposeParentWindow(list);
      };
    }
  }

  /**
   * A {@link SelectionProvider} implementation based on {@link FilteredTable}
   */
  public static class TableSelectionProvider implements SelectionProvider {

    private final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table;
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
      table = FilteredTable.filteredTable(tableModel);
      selectControl = Control.builder(createSelectCommand(searchModel, tableModel))
              .caption(Messages.ok())
              .build();
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectControl)
              .enable(table);
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .action(selectControl)
              .enable(table.searchField());
      KeyEvents.builder(KeyEvent.VK_F)
              .modifiers(InputEvent.CTRL_DOWN_MASK)
              .action(Control.control(() -> table.searchField().requestFocusInWindow()))
              .enable(table);
      tableModel.columnModel().columns().forEach(this::configureColumn);
      Collection<Attribute<String>> searchAttributes = searchModel.searchAttributes();
      tableModel.columnModel().setVisibleColumns(searchAttributes.toArray(new Attribute[0]));
      tableModel.sortModel().setSortOrder(searchAttributes.iterator().next(), SortOrder.ASCENDING);
      table.setSelectionMode(searchModel.multipleSelectionEnabledState().get() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      table.setDoubleClickAction(selectControl);
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
    public final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table() {
      return table;
    }

    @Override
    public final void selectEntities(JComponent dialogOwner, List<Entity> entities) {
      table.getModel().addEntitiesAt(0, requireNonNull(entities));
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
        searchModel.setSelectedEntities(tableModel.selectionModel().getSelectedItems());
        Utilities.disposeParentWindow(table);
      };
    }

    private <T> void configureColumn(FilteredTableColumn<Attribute<?>> column) {
      Property<T> property = table.getModel().entityDefinition().property((Attribute<T>) column.getIdentifier());
      column.setCellRenderer(EntityTableCellRenderer.builder(table.getModel(), property).build());
    }
  }

  private static final class SearchFieldSingleValue extends AbstractComponentValue<Entity, EntitySearchField> {

    private SearchFieldSingleValue(EntitySearchField searchField) {
      super(searchField);
      searchField.model().addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    protected Entity getComponentValue() {
      List<Entity> selectedEntities = component().model().getSelectedEntities();

      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setComponentValue(Entity value) {
      component().model().setSelectedEntity(value);
    }
  }

  private static final class SearchFieldMultipleValues extends AbstractComponentValue<List<Entity>, EntitySearchField> {

    private SearchFieldMultipleValues(EntitySearchField searchField) {
      super(searchField);
      searchField.model().addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    protected List<Entity> getComponentValue() {
      return component().model().getSelectedEntities();
    }

    @Override
    protected void setComponentValue(List<Entity> value) {
      component().model().setSelectedEntities(value);
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
          model().setSelectedEntity(null);
        }
        else if (shouldPerformSearch()) {
          performSearch(false);
        }
      }
      updateColors();
    }

    private boolean shouldPerformSearch() {
      return !performingSearch && !model.searchStringRepresentsSelected();
    }
  }

  private final class EnterEscapeListener extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (!model.searchStringRepresentsSelected()) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          e.consume();
          performSearch(true);
        }
        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
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
    public Builder selectionProviderFactory(Function<EntitySearchModel, SelectionProvider> selectionProviderFactory) {
      this.selectionProviderFactory = requireNonNull(selectionProviderFactory);
      return this;
    }

    @Override
    public ComponentValue<List<Entity>, EntitySearchField> buildComponentValueMultiple() {
      return new SearchFieldMultipleValues(build());
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
      if (selectionProviderFactory != null) {
        searchField.setSelectionProvider(selectionProviderFactory.apply(searchField.model()));
      }
      selectAllOnFocusGained(searchField);

      return searchField;
    }

    @Override
    protected ComponentValue<Entity, EntitySearchField> createComponentValue(EntitySearchField component) {
      return new SearchFieldSingleValue(component);
    }

    @Override
    protected void setInitialValue(EntitySearchField component, Entity initialValue) {
      component.model().setSelectedEntity(initialValue);
    }

    @Override
    protected void enableTransferFocusOnEnter(EntitySearchField component) {
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_FOCUSED)
              .action(component.transferFocusAction)
              .enable(component);
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_FOCUSED)
              .modifiers(InputEvent.SHIFT_DOWN_MASK)
              .action(component.transferFocusBackwardAction)
              .enable(component);
    }
  }

  private static final class DefaultLookupDialogBuilder implements LookupDialogBuilder {

    private final EntityType entityType;
    private final EntityConnectionProvider connectionProvider;

    private EntitySearchField searchField;
    private JComponent owner;
    private String title;

    private DefaultLookupDialogBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
      this.entityType = requireNonNull(entityType);
      this.connectionProvider = requireNonNull(connectionProvider);
    }

    @Override
    public LookupDialogBuilder owner(JComponent owner) {
      this.owner = owner;
      return this;
    }

    @Override
    public LookupDialogBuilder title(String title) {
      this.title = title;
      return this;
    }

    @Override
    public LookupDialogBuilder searchField(EntitySearchField searchField) {
      if (!requireNonNull(searchField).model().entityType().equals(entityType)) {
        throw new IllegalArgumentException("Search field entity type must be the same as lookup entity type: " + entityType);
      }
      this.searchField = searchField;
      return this;
    }

    @Override
    public Optional<Entity> lookupSingle() {
      List<Entity> entities = lookupEntities(entityType, connectionProvider, true, owner, title);

      return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }

    @Override
    public List<Entity> lookup() {
      return lookupEntities(entityType, connectionProvider, false, owner, title);
    }

    private List<Entity> lookupEntities(EntityType entityType, EntityConnectionProvider connectionProvider,
                                        boolean singleSelection, JComponent dialogOwner, String dialogTitle) {
      if (searchField == null) {
        searchField = new EntitySearchField(EntitySearchModel.entitySearchModel(entityType, connectionProvider), true);
      }
      searchField.model.multipleSelectionEnabledState().set(!singleSelection);

      return Dialogs.inputDialog(new SearchFieldMultipleValues(searchField))
              .owner(dialogOwner)
              .title(dialogTitle)
              .show();
    }
  }
}
