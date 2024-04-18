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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.Configuration;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.SwingMessages;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.list.ListBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.text.HintTextField;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
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
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.TextComponents.selectAllOnFocusGained;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.dialog.Dialogs.okCancelDialog;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.component.EntitySearchField.SearchIndicator.WAIT_CURSOR;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A UI component based on the EntitySearchModel.
 * The search is triggered by the ENTER key and behaves in the following way:
 * If the search result is empty a message is shown, if a single entity fits the
 * condition then that entity is selected, otherwise a component displaying the entities
 * fitting the condition is shown in a dialog allowing either a single or multiple
 * selection based on the search model settings.
 * {@link ListSelector} is the default {@link Selector}.
 * Use {@link EntitySearchField#builder(EntitySearchModel)} or {@link EntitySearchField#builder(EntityType, EntityConnectionProvider)} for a builder instance.
 * @see EntitySearchModel
 * @see #builder(EntityType, EntityConnectionProvider)
 * @see #builder(EntitySearchModel)
 * @see #singleSelectionValue()
 * @see #multiSelectionValue()
 * @see #selectorFactory()
 */
public final class EntitySearchField extends HintTextField {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntitySearchField.class.getName());

	/**
	 * Specifies the way a {@link EntitySearchField} indicates that a search is in progress.
	 * Value type: {@link SearchIndicator}<br>
	 * Default value: {@link SearchIndicator#WAIT_CURSOR}
	 */
	public static final PropertyValue<SearchIndicator> SEARCH_INDICATOR =
					Configuration.enumValue("is.codion.swing.framework.ui.component.EntitySearchField.searchIndicator", SearchIndicator.class, WAIT_CURSOR);

	/**
	 * The ways which a search field can indicate that a search is in progress.
	 */
	public enum SearchIndicator {
		/**
		 * Display a wait cursor while searching.
		 */
		WAIT_CURSOR,
		/**
		 * Display an indeterminate progress bar while searching
		 */
		PROGRESS_BAR
	}

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS = keyboardShortcuts(KeyboardShortcut.class);

	/**
	 * The available keyboard shortcuts.
	 * @see Builder#editPanel(Supplier)
	 */
	public enum KeyboardShortcut implements KeyboardShortcuts.Shortcut {
		/**
		 * Displays a dialog for adding a new record.<br>
		 * Default: INSERT
		 */
		ADD(keyStroke(VK_INSERT)),
		/**
		 * Displays a dialog for editing the selected record.<br>
		 * Default: CTRL-INSERT
		 */
		EDIT(keyStroke(VK_INSERT, CTRL_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		KeyboardShortcut(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public KeyStroke defaultKeystroke() {
			return defaultKeystroke;
		}
	}

	private final EntitySearchModel model;
	private final Action transferFocusAction = TransferFocusOnEnter.forwardAction();
	private final Action transferFocusBackwardAction = TransferFocusOnEnter.backwardAction();
	private final State searchOnFocusLost = State.state(true);
	private final State searching = State.state();
	private final Value<Function<EntitySearchModel, Selector>> selectorFactory;
	private final Value<SearchIndicator> searchIndicator;
	private final Control addControl;
	private final Control editControl;

	private SettingsPanel settingsPanel;
	private SingleSelectionValue singleSelectionValue;
	private MultiSelectionValue multiSelectionValue;
	private ProgressWorker<List<Entity>, ?> searchWorker;
	private Consumer<Boolean> searchIndicatorConsumer;
	private Control searchControl;

	private Color backgroundColor;
	private Color invalidBackgroundColor;

	private EntitySearchField(DefaultEntitySearchFieldBuilder builder) {
		super(builder.searchHintEnabled ? Messages.search() + "..." : null);
		model = requireNonNull(builder.searchModel);
		addControl = createAddControl(builder.editPanel,
						builder.keyboardShortcuts.keyStroke(KeyboardShortcut.ADD).get());
		editControl = createEditControl(builder.editPanel,
						builder.keyboardShortcuts.keyStroke(KeyboardShortcut.EDIT).get());
		if (builder.columns != -1) {
			setColumns(builder.columns);
		}
		if (builder.upperCase) {
			TextComponents.upperCase(getDocument());
		}
		if (builder.lowerCase) {
			TextComponents.lowerCase(getDocument());
		}
		searchOnFocusLost.set(builder.searchOnFocusLost);
		searchIndicator = Value.nonNull(builder.searchIndicator).build();
		updateSearchIndicator();
		selectorFactory = Value.nonNull(builder.selectorFactory).build();
		if (builder.selectAllOnFocusGained) {
			selectAllOnFocusGained(this);
		}
		setToolTipText(model.description());
		setComponentPopupMenu(createPopupMenu());
		configureColors();
		bindEvents();
	}

	private Control createAddControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke) {
		return editPanel == null ? null : EntityControls.createAddControl(this, editPanel, keyStroke);
	}

	private Control createEditControl(Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke) {
		return editPanel == null ? null : EntityControls.createEditControl(this, editPanel, keyStroke);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (model != null) {
			configureColors();
		}
		if (searchIndicatorConsumer instanceof ProgressBarWhileSearching) {
			((ProgressBarWhileSearching) searchIndicatorConsumer).progressBar.updateUI();
		}
	}

	/**
	 * @return the search model this search field is based on
	 */
	public EntitySearchModel model() {
		return model;
	}

	/**
	 * @param transferFocusOnEnter true if this component should transfer focus on Enter
	 */
	public void transferFocusOnEnter(boolean transferFocusOnEnter) {
		KeyEvents.Builder transferForward = KeyEvents.builder(VK_ENTER)
						.condition(WHEN_FOCUSED)
						.action(transferFocusAction);
		KeyEvents.Builder transferBackward = KeyEvents.builder(VK_ENTER)
						.condition(WHEN_FOCUSED)
						.modifiers(SHIFT_DOWN_MASK)
						.action(transferFocusBackwardAction);
		if (transferFocusOnEnter) {
			transferForward.enable(this);
			transferBackward.enable(this);
		}
		else {
			transferForward.disable(this);
			transferBackward.disable(this);
		}
	}

	/**
	 * @return a Control for triggering a search
	 */
	public Control searchControl() {
		if (searchControl == null) {
			searchControl = Control.builder(() -> performSearch(true))
							.smallIcon(FrameworkIcons.instance().search())
							.enabled(model.searchStringModified())
							.build();
		}

		return searchControl;
	}

	/**
	 * @return a Control for inserting a new record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<Control> addControl() {
		return Optional.ofNullable(addControl);
	}

	/**
	 * @return a Control for editing the selected record, if one is available
	 * @see Builder#editPanel(Supplier)
	 */
	public Optional<Control> editControl() {
		return Optional.ofNullable(editControl);
	}

	/**
	 * @return the value controlling the search indicator type
	 * @see #SEARCH_INDICATOR
	 */
	public Value<SearchIndicator> searchIndicator() {
		return searchIndicator;
	}

	/**
	 * Controls the factory for the {@link Selector} responsible for selecting items from the search result.
	 * @return the Value controlling the factory for the {@link Selector} implementation to use when presenting
	 * a selection dialog to the user
	 */
	public Value<Function<EntitySearchModel, Selector>> selectorFactory() {
		return selectorFactory;
	}

	/**
	 * @return the State controlling whether this field should trigger a search when it loses focus
	 */
	public State searchOnFocusLost() {
		return searchOnFocusLost;
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
		 * @param selectAllOnFocusGained true if the contents should be selected when the field gains focus
		 * @return this builder instance
		 */
		Builder selectAllOnFocusGained(boolean selectAllOnFocusGained);

		/**
		 * @param searchIndicator the search indicator
		 * @return this builder instance
		 */
		Builder searchIndicator(SearchIndicator searchIndicator);

		/**
		 * @param selectorFactory the selector factory to use
		 * @return this builder instance
		 */
		Builder selectorFactory(Function<EntitySearchModel, Selector> selectorFactory);

		/**
		 * A edit panel is required for the add and edit controls.
		 * @param editPanel the edit panel supplier
		 * @return this builder instance
		 */
		Builder editPanel(Supplier<EntityEditPanel> editPanel);

		/**
		 * @param keyboardShortcut the keyboard shortcut key
		 * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
		 * @return this builder instance
		 */
		Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);

		/**
		 * @param limit the search result limit
		 * @return this builder instance
		 */
		Builder limit(int limit);
	}

	private void bindEvents() {
		new SearchStringValue(this).link(model.searchString());
		model.searchString().addListener(this::updateColors);
		model.entities().addListener(() -> setCaretPosition(0));
		searchIndicator.addListener(this::updateSearchIndicator);
		addFocusListener(new SearchFocusListener());
		addKeyListener(new EnterEscapeListener());
		linkToEnabledState(model.searchStringModified().not(), transferFocusAction, transferFocusBackwardAction);
	}

	private void updateSearchIndicator() {
		if (searchIndicatorConsumer != null) {
			searching.removeConsumer(searchIndicatorConsumer);
		}
		searchIndicatorConsumer = createSearchIndicatorConsumer();
		searching.addConsumer(searchIndicatorConsumer);
	}

	private Consumer<Boolean> createSearchIndicatorConsumer() {
		switch (searchIndicator.get()) {
			case WAIT_CURSOR:
				return new WaitCursorWhileSearching();
			case PROGRESS_BAR:
				return new ProgressBarWhileSearching();
			default:
				throw new IllegalArgumentException("Unknown search indicator: " + searchIndicator);
		}
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
		if (nullOrEmpty(model.searchString().get())) {
			model.entities().clear();
		}
		else if (model.searchStringModified().get()) {
			cancelCurrentSearch();
			searching.set(true);
			searchWorker = ProgressWorker.builder(model::search)
							.onResult(searchResult -> handleResult(searchResult, promptUser))
							.onException(this::handleException)
							.onCancelled(this::handleCancel)
							.onInterrupted(this::handleInterrupted)
							.execute();
		}
	}

	private void cancelCurrentSearch() {
		ProgressWorker<?, ?> currentWorker = searchWorker;
		if (currentWorker != null) {
			currentWorker.cancel(true);
		}
	}

	private void handleResult(List<Entity> searchResult, boolean promptUser) {
		endSearch();
		if (searchResult.size() == 1) {
			model.entities().set(searchResult);
		}
		else if (promptUser) {
			promptUser(searchResult);
		}
		selectAll();
		updateColors();
	}

	private void promptUser(List<Entity> searchResult) {
		if (searchResult.isEmpty()) {
			JOptionPane.showMessageDialog(this, FrameworkMessages.noSearchResults(),
							SwingMessages.get("OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			selectorFactory.get().apply(model).select(this, searchResult);
		}
	}

	private void handleException(Exception exception) {
		endSearch();
		updateColors();
		Dialogs.displayExceptionDialog(exception, Utilities.parentWindow(this));
	}

	private void handleCancel() {
		endSearch();
	}

	private void handleInterrupted() {
		endSearch();
		Thread.currentThread().interrupt();
	}

	private void endSearch() {
		searchWorker = null;
		searching.set(false);
	}

	private JPopupMenu createPopupMenu() {
		return menu(Controls.controls(Control.builder(() -> Dialogs.componentDialog(settingsPanel())
										.owner(EntitySearchField.this)
										.title(FrameworkMessages.settings())
										.icon(FrameworkIcons.instance().settings())
										.show())
						.name(FrameworkMessages.settings())
						.smallIcon(FrameworkIcons.instance().settings())
						.build()))
						.createPopupMenu();
	}

	private SettingsPanel settingsPanel() {
		if (settingsPanel == null) {
			settingsPanel = new SettingsPanel(model);
		}

		return settingsPanel;
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
			setLayout(borderLayout());
			setBorder(emptyBorder());
			add(createSearchColumnPanel(searchModel), BorderLayout.CENTER);
			add(createSouthPanel(searchModel), BorderLayout.SOUTH);
		}

		private static JPanel createSearchColumnPanel(EntitySearchModel searchModel) {
			CardLayout cardLayout = new CardLayout(5, 5);
			PanelBuilder columnBasePanelBuilder = panel(cardLayout);
			FilteredComboBoxModel<Item<Column<String>>> columnComboBoxModel = new FilteredComboBoxModel<>();
			EntityDefinition definition = searchModel.connectionProvider().entities().definition(searchModel.entityType());
			for (Map.Entry<Column<String>, EntitySearchModel.Settings> entry : searchModel.settings().entrySet()) {
				columnComboBoxModel.add(Item.item(entry.getKey(), definition.columns().definition(entry.getKey()).caption()));
				columnBasePanelBuilder.add(createSettingsPanel(entry.getValue()), entry.getKey().name());
			}
			JPanel columnBasePanel = columnBasePanelBuilder.build();
			if (columnComboBoxModel.getSize() > 0) {
				columnComboBoxModel.selectionEvent().addConsumer(selected -> cardLayout.show(columnBasePanel, selected.get().name()));
				columnComboBoxModel.setSelectedItem(columnComboBoxModel.getElementAt(0));
			}

			return borderLayoutPanel()
							.border(BorderFactory.createTitledBorder(MESSAGES.getString("search_columns")))
							.northComponent(comboBox(columnComboBoxModel).build())
							.centerComponent(columnBasePanel)
							.build();
		}

		private static JPanel createSouthPanel(EntitySearchModel searchModel) {
			BorderLayoutPanelBuilder panelBuilder = borderLayoutPanel();
			if (!searchModel.singleSelection()) {
				panelBuilder.centerComponent(createSeparatorPanel(searchModel));
			}
			panelBuilder.eastComponent(createLimitPanel(searchModel));

			return panelBuilder.build();
		}

		private static JPanel createSeparatorPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.westComponent(new JLabel(MESSAGES.getString("multiple_item_separator")))
							.centerComponent(stringField(searchModel.separator())
											.columns(1)
											.maximumLength(1)
											.build())
							.build();
		}

		private static JPanel createLimitPanel(EntitySearchModel searchModel) {
			return borderLayoutPanel()
							.westComponent(new JLabel(MESSAGES.getString("result_limit")))
							.centerComponent(integerField(searchModel.limit())
											.columns(4)
											.build())
							.build();
		}

		private static JPanel createSettingsPanel(EntitySearchModel.Settings settings) {
			return gridLayoutPanel(4, 1)
							.add(checkBox(settings.caseSensitive())
											.text(MESSAGES.getString("case_sensitive"))
											.build())
							.add(checkBox(settings.wildcardPrefix())
											.text(MESSAGES.getString("prefix_wildcard"))
											.build())
							.add(checkBox(settings.wildcardPostfix())
											.text(MESSAGES.getString("postfix_wildcard"))
											.build())
							.add(checkBox(settings.spaceAsWildcard())
											.text(MESSAGES.getString("space_as_wildcard"))
											.build())
							.build();
		}
	}

	/**
	 * Provides a way for the user to select one or more of a given set of entities
	 * @see #listSelector(EntitySearchModel)
	 * @see #tableSelector(EntitySearchModel)
	 */
	public interface Selector {

		/**
		 * Displays a dialog for selecting from the given entities.
		 * @param dialogOwner the dialog owner
		 * @param entities the entities to select from
		 */
		void select(JComponent dialogOwner, List<Entity> entities);

		/**
		 * Sets the preferred size of the selection component.
		 * @param preferredSize the preferred selection component size
		 */
		void preferredSize(Dimension preferredSize);
	}

	/**
	 * A {@link Selector} based on a {@link JList}.
	 */
	public interface ListSelector extends Selector {

		/**
		 * @return the list used for selecting entities
		 */
		JList<Entity> list();
	}

	/**
	 * A {@link Selector} based on a {@link FilteredTable}.
	 */
	public interface TableSelector extends Selector {

		/**
		 * @return the table used for selecting entities
		 */
		FilteredTable<Entity, Attribute<?>> table();
	}

	/**
	 * @param searchModel the search model
	 * @return a {@link Selector} based on a {@link JList}.
	 */
	public static ListSelector listSelector(EntitySearchModel searchModel) {
		return new DefaultListSelector(searchModel);
	}

	/**
	 * @param searchModel the search model
	 * @return a {@link Selector} based on a {@link FilteredTable}.
	 */
	public static TableSelector tableSelector(EntitySearchModel searchModel) {
		return new DefaultTableSelector(searchModel);
	}

	private static final class DefaultListSelector implements ListSelector {

		private final EntitySearchModel searchModel;
		private final JList<Entity> list;
		private final JPanel selectorPanel;
		private final JLabel resultLimitLabel = label()
						.horizontalAlignment(SwingConstants.RIGHT)
						.build();
		private final Control selectControl = control(new SelectCommand());

		private DefaultListSelector(EntitySearchModel searchModel) {
			this.searchModel = requireNonNull(searchModel);
			this.list = createList(searchModel);
			this.selectorPanel = borderLayoutPanel()
							.centerComponent(scrollPane(list).build())
							.southComponent(resultLimitLabel)
							.border(createEmptyBorder(Layouts.GAP.get(), Layouts.GAP.get(), 0, Layouts.GAP.get()))
							.build();
		}

		@Override
		public JList<Entity> list() {
			return list;
		}

		@Override
		public void select(JComponent dialogOwner, List<Entity> entities) {
			DefaultListModel<Entity> listModel = (DefaultListModel<Entity>) list.getModel();
			requireNonNull(entities).forEach(listModel::addElement);
			list.scrollRectToVisible(list.getCellBounds(0, 0));
			initializeResultLimitMessage(resultLimitLabel, searchModel.limit().optional().orElse(-1), entities.size());

			okCancelDialog(selectorPanel)
							.owner(dialogOwner)
							.title(MESSAGES.getString("select_entity"))
							.okAction(selectControl)
							.show();

			listModel.removeAllElements();
		}

		@Override
		public void preferredSize(Dimension preferredSize) {
			selectorPanel.setPreferredSize(preferredSize);
		}

		private JList<Entity> createList(EntitySearchModel searchModel) {
			DefaultListModel<Entity> listModel = new DefaultListModel<>();
			ListBuilder<Entity, ?, ?> listBuilder = searchModel.singleSelection() ?
							Components.list(listModel).selectedItem() : Components.list(listModel).selectedItems();

			return listBuilder.mouseListener(new DoubleClickListener())
							.onBuild(new RemoveDefaultEnterAction())
							.build();
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() {
				searchModel.entities().set(list.getSelectedValuesList());
				disposeParentWindow(list);
			}
		}

		private final class DoubleClickListener extends MouseAdapter {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectControl.actionPerformed(null);
				}
			}
		}

		private static final class RemoveDefaultEnterAction implements Consumer<JList<Entity>> {
			@Override
			public void accept(JList list) {
				list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
								.put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
			}
		}
	}

	private static final class DefaultTableSelector implements TableSelector {

		private final EntitySearchModel searchModel;
		private final FilteredTable<Entity, Attribute<?>> table;
		private final JPanel selectorPanel;
		private final JLabel resultLimitLabel = label()
						.horizontalAlignment(SwingConstants.RIGHT)
						.build();
		private final Control selectControl = control(new SelectCommand());

		private DefaultTableSelector(EntitySearchModel searchModel) {
			this.searchModel = requireNonNull(searchModel);
			table = createTable();
			selectorPanel = borderLayoutPanel()
							.centerComponent(scrollPane(table).build())
							.southComponent(borderLayoutPanel()
											.westComponent(table.searchField())
											.centerComponent(resultLimitLabel)
											.build())
							.border(createEmptyBorder(Layouts.GAP.get(), Layouts.GAP.get(), 0, Layouts.GAP.get()))
							.build();
		}

		/**
		 * @return the underlying FilteredTable
		 */
		public FilteredTable<Entity, Attribute<?>> table() {
			return table;
		}

		@Override
		public void select(JComponent dialogOwner, List<Entity> entities) {
			table.getModel().addItemsAtSorted(0, requireNonNull(entities));
			table.scrollRectToVisible(table.getCellRect(0, 0, true));
			initializeResultLimitMessage(resultLimitLabel, searchModel.limit().optional().orElse(-1), entities.size());

			okCancelDialog(selectorPanel)
							.owner(dialogOwner)
							.title(MESSAGES.getString("select_entity"))
							.okAction(selectControl)
							.show();

			table.getModel().clear();
			table.searchField().setText("");
		}

		@Override
		public void preferredSize(Dimension preferredSize) {
			selectorPanel.setPreferredSize(preferredSize);
		}

		private FilteredTable<Entity, Attribute<?>> createTable() {
			return FilteredTable.builder(createTableModel())
							.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
							.selectionMode(searchModel.singleSelection() ?
											ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
							.doubleClickAction(selectControl)
							.keyEvent(KeyEvents.builder(VK_ENTER)
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(selectControl))
							.keyEvent(KeyEvents.builder(VK_ENTER)
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(selectControl))
							.keyEvent(KeyEvents.builder(VK_F)
											.modifiers(CTRL_DOWN_MASK)
											.action(control(this::requestSearchFieldFocus)))
							.onBuild(t -> KeyEvents.builder(VK_ENTER)
											.action(selectControl)
											.enable(t.searchField()))
							.build();
		}

		private SwingEntityTableModel createTableModel() {
			SwingEntityTableModel tableModel = new DefaultTableModel();
			tableModel.columnModel().columns().forEach(column ->
							column.setCellRenderer(EntityTableCellRenderer.builder(tableModel, column.getIdentifier()).build()));
			Collection<Column<String>> columns = searchModel.columns();
			tableModel.columnModel().setVisibleColumns(columns.toArray(new Attribute[0]));
			tableModel.sortModel().setSortOrder(columns.iterator().next(), SortOrder.ASCENDING);

			return tableModel;
		}

		private void requestSearchFieldFocus() {
			table.searchField().requestFocusInWindow();
		}

		private final class SelectCommand implements Control.Command {
			@Override
			public void execute() throws Exception {
				searchModel.entities().set(table.getModel().selectionModel().getSelectedItems());
				disposeParentWindow(table);
			}
		}

		private final class DefaultTableModel extends SwingEntityTableModel {

			private DefaultTableModel() {
				super(searchModel.entityType(), searchModel.connectionProvider());
			}

			@Override
			protected Collection<Entity> refreshItems() {
				return emptyList();
			}
		}
	}

	private static void initializeResultLimitMessage(JLabel label, int limit, int resultSize) {
		boolean resultLimitReached = limit == resultSize;
		if (resultLimitReached) {
			label.setText(format(MESSAGES.getString("result_limited"), limit));
			label.setVisible(true);
		}
		label.setVisible(resultLimitReached);
	}

	private static final class SingleSelectionValue extends AbstractComponentValue<Entity, EntitySearchField> {

		private SingleSelectionValue(EntitySearchField searchField) {
			super(searchField);
			searchField.model().entity().addListener(this::notifyListeners);
		}

		@Override
		protected Entity getComponentValue() {
			return component().model().entity().get();
		}

		@Override
		protected void setComponentValue(Entity value) {
			component().model().entity().set(value);
		}
	}

	private static final class MultiSelectionValue extends AbstractComponentValue<Collection<Entity>, EntitySearchField> {

		private MultiSelectionValue(EntitySearchField searchField) {
			super(searchField);
			searchField.model().entities().addListener(this::notifyListeners);
		}

		@Override
		protected Collection<Entity> getComponentValue() {
			return component().model().entities().get();
		}

		@Override
		protected void setComponentValue(Collection<Entity> value) {
			component().model().entities().set(value);
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
					model().entities().clear();
				}
				else if (shouldPerformSearch()) {
					performSearch(false);
				}
			}
			updateColors();
		}

		private boolean shouldPerformSearch() {
			return searchOnFocusLost.get() && !searching.get() && model.searchStringModified().get();
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
					model.reset();
					selectAll();
				}
			}
		}
	}

	private final class WaitCursorWhileSearching implements Consumer<Boolean> {

		private final Cursor defaultCursor = getCursor();

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				setCursor(Cursors.WAIT);
			}
			else {
				setCursor(defaultCursor);
			}
		}
	}

	private final class ProgressBarWhileSearching implements Consumer<Boolean> {

		private final JProgressBar progressBar = progressBar()
						.indeterminate(true)
						.string(MESSAGES.getString("searching") + "...")
						.stringPainted(true)
						.build();

		@Override
		public void accept(Boolean isSearching) {
			if (isSearching) {
				setLayout(new BorderLayout());
				add(progressBar, BorderLayout.CENTER);
			}
			else {
				remove(progressBar);
				setLayout(null);
			}
			revalidate();
			repaint();
		}
	}

	private static final class DefaultEntitySearchFieldBuilder extends AbstractComponentBuilder<Entity, EntitySearchField, Builder> implements Builder {

		private final EntitySearchModel searchModel;
		private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

		private int columns = -1;
		private boolean upperCase;
		private boolean lowerCase;
		private boolean searchHintEnabled = true;
		private boolean searchOnFocusLost = true;
		private boolean selectAllOnFocusGained = true;
		private SearchIndicator searchIndicator = SEARCH_INDICATOR.get();
		private Function<EntitySearchModel, Selector> selectorFactory = new ListSelectorFactory();
		private Supplier<EntityEditPanel> editPanel;

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
		public Builder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
			this.selectAllOnFocusGained = selectAllOnFocusGained;
			return this;
		}

		@Override
		public Builder searchIndicator(SearchIndicator searchIndicator) {
			this.searchIndicator = requireNonNull(searchIndicator);
			return this;
		}

		@Override
		public Builder selectorFactory(Function<EntitySearchModel, Selector> selectorFactory) {
			this.selectorFactory = requireNonNull(selectorFactory);
			return this;
		}

		@Override
		public Builder editPanel(Supplier<EntityEditPanel> editPanel) {
			this.editPanel = requireNonNull(editPanel);
			return this;
		}

		@Override
		public Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
			keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
			return this;
		}

		@Override
		public Builder limit(int limit) {
			this.searchModel.limit().set(limit);
			return this;
		}

		@Override
		protected EntitySearchField createComponent() {
			return new EntitySearchField(this);
		}

		@Override
		protected ComponentValue<Entity, EntitySearchField> createComponentValue(EntitySearchField component) {
			return component.singleSelectionValue();
		}

		@Override
		protected void setInitialValue(EntitySearchField component, Entity initialValue) {
			component.model().entity().set(initialValue);
		}

		@Override
		protected void enableTransferFocusOnEnter(EntitySearchField component) {
			component.transferFocusOnEnter(true);
		}

		private static final class ListSelectorFactory implements Function<EntitySearchModel, Selector> {

			@Override
			public Selector apply(EntitySearchModel searchModel) {
				return new DefaultListSelector(searchModel);
			}
		}
	}
}
