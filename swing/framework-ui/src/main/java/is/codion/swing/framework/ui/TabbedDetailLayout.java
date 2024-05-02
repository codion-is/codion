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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel.DetailController;
import is.codion.swing.framework.ui.EntityPanel.DetailLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.EntityPanel.WindowType;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.TabbedDetailLayout.TabbedDetailLayoutControl.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

/**
 * A {@link DetailLayout} implementation based on a JTabbedPane.<br>
 * <pre>
 * The default layout is as follows:
 * __________________________________
 * |  edit    |control|             |
 * |  panel   | panel |             |
 * |__________|_______|   detail    |
 * |                  |   panels    |
 * |   table panel    |             |
 * |(EntityTablePanel)|             |
 * |                  |             |
 * |__________________|_____________|
 * </pre>
 */
public final class TabbedDetailLayout implements DetailLayout {

	/**
	 * Specifies whether actions to hide detail panels or show them in a dialog should be available to the user,
	 * for example in a popup menu or on a toolbar.<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	public static final PropertyValue<Boolean> INCLUDE_CONTROLS =
					Configuration.booleanValue("is.codion.swing.framework.ui.TabbedPanelLayout.includeControls", true);

	private static final MessageBundle MESSAGES =
					messageBundle(TabbedDetailLayout.class, getBundle(TabbedDetailLayout.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final KeyboardShortcuts<TabbedDetailLayoutControl> KEYBOARD_SHORTCUTS =
					keyboardShortcuts(TabbedDetailLayoutControl.class);

	/**
	 * The controls.
	 */
	public enum TabbedDetailLayoutControl implements KeyboardShortcuts.Shortcut {
		/**
		 * Resizes the detail panel to the right.<br>
		 * Default key stroke: SHIFT-ALT-RIGHT ARROW
		 */
		RESIZE_RIGHT(keyStroke(VK_RIGHT, ALT_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Resizes the detail panel to the left.<br>
		 * Default key stroke: SHIFT-ALT-LEFT ARROW
		 */
		RESIZE_LEFT(keyStroke(VK_LEFT, ALT_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Collapses the detail panel all the way to the right, hiding it.<br>
		 * Default key stroke: SHIFT-CTRL-ALT RIGHT ARROW
		 */
		COLLAPSE(keyStroke(VK_RIGHT, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Expands the detail panel all the way to the left, hiding the parent.<br>
		 * Default key stroke: SHIFT-CTRL-ALT LEFT ARROW
		 */
		EXPAND(keyStroke(VK_LEFT, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		TabbedDetailLayoutControl(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
	}

	private static final int RESIZE_AMOUNT = 30;
	private static final String DETAIL_TABLES = "detail_tables";
	private static final double DEFAULT_SPLIT_PANE_RESIZE_WEIGHT = 0.5;
	private static final int DETAIL_WINDOW_OFFSET = 38;//titlebar height
	private static final double DETAIL_WINDOW_SIZE_RATIO = 0.66;

	private final EntityPanel entityPanel;
	private final TabbedDetailController detailController;
	private final boolean includeControls;
	private final double splitPaneResizeWeight;
	private final KeyboardShortcuts<TabbedDetailLayoutControl> keyboardShortcuts;
	private final WindowType windowType;

	private JTabbedPane tabbedPane;
	private JSplitPane splitPane;
	private Window panelWindow;
	private PanelState panelState = EMBEDDED;

	private TabbedDetailLayout(DefaultBuilder builder) {
		this.entityPanel = builder.entityPanel;
		this.windowType = builder.windowType;
		this.panelState = builder.panelState;
		this.includeControls = builder.includeControls;
		this.splitPaneResizeWeight = builder.splitPaneResizeWeight;
		this.detailController = new TabbedDetailController();
		this.keyboardShortcuts = builder.keyboardShortcuts;
	}

	@Override
	public void updateUI() {
		Utilities.updateUI(tabbedPane, splitPane);
	}

	@Override
	public Optional<JComponent> layout() {
		if (entityPanel.detailPanels().isEmpty()) {
			throw new IllegalStateException("EntityPanel " + entityPanel + " has no detail panels");
		}
		if (splitPane != null) {
			throw new IllegalStateException("EntityPanel " + entityPanel + " has already been laid out");
		}
		entityPanel.activateEvent().addListener(new ShowIfHidden());
		entityPanel.detailPanels().forEach(this::bindEvents);
		splitPane = createSplitPane(entityPanel.mainPanel());
		tabbedPane = createTabbedPane(entityPanel.detailPanels());
		setupControls(entityPanel);
		initializePanelState();

		return Optional.of(splitPane);
	}

	@Override
	public Optional<DetailController> controller() {
		return Optional.of(detailController);
	}

	/**
	 * @param entityPanel the entity panel
	 * @return a new {@link TabbedDetailLayout.Builder} instance
	 */
	public static Builder builder(EntityPanel entityPanel) {
		return new DefaultBuilder(entityPanel);
	}

	/**
	 * Builds a {@link TabbedDetailLayout}.
	 */
	public interface Builder {

		/**
		 * @param panelState the initial detail panel state
		 * @return this builder instance
		 */
		Builder panelState(PanelState panelState);

		/**
		 * @param windowType the window type to use
		 * @return this builder instance
		 */
		Builder windowType(WindowType windowType);

		/**
		 * @param splitPaneResizeWeight the detail panel split pane size weight
		 * @return this builder instance
		 */
		Builder splitPaneResizeWeight(double splitPaneResizeWeight);

		/**
		 * @param includeControls true if detail panel controls should be available
		 * @return this builder instance
		 */
		Builder includeControls(boolean includeControls);

		/**
		 * @param control the control
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(TabbedDetailLayoutControl control, KeyStroke keyStroke);

		/**
		 * @return a new {@link TabbedDetailLayout} instance based on this builder
		 */
		TabbedDetailLayout build();
	}

	private void bindEvents(EntityPanel detailPanel) {
		detailPanel.activateEvent().addConsumer(detailController::activated);
		keyboardShortcuts.keyStroke(RESIZE_RIGHT).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, true, false))));
		keyboardShortcuts.keyStroke(RESIZE_LEFT).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, false, false))));
		keyboardShortcuts.keyStroke(COLLAPSE).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, true, true))));
		keyboardShortcuts.keyStroke(EXPAND).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, false, true))));
	}

	private void setupControls(EntityPanel entityPanel) {
		if (entityPanel.containsTablePanel() && includeControls) {
			EntityTablePanel tablePanel = entityPanel.tablePanel();
			tablePanel.addToolBarControls(Controls.builder()
							.control(Control.builder(detailController::toggleDetailState)
											.smallIcon(ICONS.detail())
											.description(MESSAGES.getString("toggle_detail")))
							.build());
			tablePanel.addPopupMenuControls(Controls.builder()
							.name(MESSAGES.getString(DETAIL_TABLES))
							.smallIcon(ICONS.detail())
							.controls(entityPanel.detailPanels().stream()
											.map(detailPanel -> Control.builder(new ActivateDetailPanel(detailPanel))
															.name(detailPanel.caption())
															.build())
											.toArray(Control[]::new))
							.build());
		}
	}

	private void initializePanelState() {
		Value<PanelState> detailPanelStateValue = detailController.panelState(selectedDetailPanel());
		if (detailPanelStateValue.isNotEqualTo(panelState)) {
			detailPanelStateValue.set(panelState);
		}
		else {
			detailController.updateDetailState();
		}
	}

	private EntityPanel selectedDetailPanel() {
		if (tabbedPane == null) {
			throw new IllegalStateException("Detail panel has not been laid out");
		}

		return (EntityPanel) tabbedPane.getSelectedComponent();
	}

	private JSplitPane createSplitPane(JPanel mainPanel) {
		return splitPane()
						.orientation(JSplitPane.HORIZONTAL_SPLIT)
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.dividerSize(GAP.get() * 2)
						.resizeWeight(splitPaneResizeWeight)
						.leftComponent(mainPanel)
						.propertyChangeListener(new ActivateWhenDetailHidden())
						.build();
	}

	private JTabbedPane createTabbedPane(Collection<EntityPanel> detailPanels) {
		TabbedPaneBuilder builder = tabbedPane()
						.focusable(false)
						.changeListener(e -> selectedDetailPanel().activate())
						.minimumSize(new Dimension(0, 0))
						.focusCycleRoot(true);
		detailPanels.forEach(detailPanel -> builder.tabBuilder(detailPanel.caption(), detailPanel)
						.toolTipText(detailPanel.description().orElse(null))
						.icon(detailPanel.icon().orElse(null))
						.add());
		if (includeControls) {
			builder.mouseListener(new TabbedPaneMouseReleasedListener());
		}

		return builder.build();
	}

	private final class ShowIfHidden implements Runnable {

		@Override
		public void run() {
			if (splitPane.getDividerLocation() == splitPane.getMinimumDividerLocation()) {
				splitPane.resetToPreferredSizes();
			}
		}
	}

	private final class ActivateDetailPanel implements Control.Command {

		private final EntityPanel detailPanel;

		private ActivateDetailPanel(EntityPanel detailPanel) {
			this.detailPanel = detailPanel;
		}

		@Override
		public void execute() {
			if (detailController.panelState.isEqualTo(HIDDEN)) {
				detailController.panelState.set(EMBEDDED);
			}
			detailPanel.activate();
		}
	}

	private final class ActivateWhenDetailHidden implements PropertyChangeListener {

		private static final String DIVIDER_LOCATION = "dividerLocation";

		@Override
		public void propertyChange(PropertyChangeEvent changeEvent) {
			if (DIVIDER_LOCATION.equals(changeEvent.getPropertyName())) {
				JSplitPane pane = (JSplitPane) changeEvent.getSource();
				if (Objects.equals(changeEvent.getNewValue(), pane.getMaximumDividerLocation())) {
					entityPanel.activate();
				}
			}
		}
	}

	private static final class ResizeAction extends AbstractAction {

		private final EntityPanel panel;
		private final boolean right;
		private final boolean expand;

		private ResizeAction(EntityPanel panel, boolean right, boolean expand) {
			super("Resize " + (right ? "right" : "left"));
			this.panel = panel;
			this.right = right;
			this.expand = expand;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			panel.parentPanel().ifPresent(parentPanel ->
							resizePanel(parentPanel, right, expand));
		}

		private static void resizePanel(EntityPanel parentPanel, boolean right, boolean expand) {
			TabbedDetailLayout detailLayout = parentPanel.detailLayout();
			JSplitPane splitPane = detailLayout.splitPane;
			if (expand) {
				expand(splitPane, right);
			}
			else {
				resize(splitPane, right);
			}
		}

		private static void expand(JSplitPane splitPane, boolean right) {
			boolean expandedOrCollapsed =
							splitPane.getDividerLocation() == splitPane.getMinimumDividerLocation() ||
											splitPane.getDividerLocation() == splitPane.getMaximumDividerLocation();
			if (expandedOrCollapsed) {
				splitPane.resetToPreferredSizes();
			}
			else {
				splitPane.setDividerLocation(right ? splitPane.getMaximumDividerLocation() : splitPane.getMinimumDividerLocation());
			}
		}

		private static void resize(JSplitPane splitPane, boolean right) {
			if (right) {
				splitPane.setDividerLocation(Math.min(splitPane.getDividerLocation() + RESIZE_AMOUNT,
								splitPane.getMaximumDividerLocation()));
			}
			else {
				splitPane.setDividerLocation(Math.max(splitPane.getDividerLocation() - RESIZE_AMOUNT, 0));
			}
		}
	}

	private final class TabbedPaneMouseReleasedListener extends MouseAdapter {

		@Override
		public void mouseReleased(MouseEvent e) {
			EntityPanel selectedDetailPanel = selectedDetailPanel();
			if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
				detailController.panelState(selectedDetailPanel).map(state -> state == WINDOW ? EMBEDDED : WINDOW);
			}
			else if (e.getButton() == MouseEvent.BUTTON2) {
				detailController.panelState(selectedDetailPanel).map(state -> state == EMBEDDED ? HIDDEN : EMBEDDED);
			}
		}
	}

	private final class TabbedDetailController implements DetailController {

		/**
		 * Holds the current state of the detail panels (HIDDEN, EMBEDDED or WINDOW)
		 */
		private final Value<PanelState> panelState = Value.nonNull(EMBEDDED)
						.listener(this::updateDetailState)
						.build();

		@Override
		public void activated(EntityPanel detailPanel) {
			requireNonNull(detailPanel);
			if (tabbedPane == null) {
				throw new IllegalStateException("Detail panel has not been laid out");
			}
			tabbedPane.setFocusable(true);
			tabbedPane.setSelectedComponent(detailPanel);
			tabbedPane.setFocusable(false);
			if (splitPane.getDividerLocation() == splitPane.getMaximumDividerLocation()) {
				splitPane.resetToPreferredSizes();
			}
			activateDetailModelLink(detailPanel.model());
		}

		@Override
		public Value<PanelState> panelState(EntityPanel detailPanel) {
			requireNonNull(detailPanel);

			return panelState;
		}

		private void updateDetailState() {
			EntityPanel selectedDetailPanel = selectedDetailPanel();
			if (panelState.isNotEqualTo(HIDDEN)) {
				selectedDetailPanel.initialize();
			}
			SwingEntityModel selectedDetailModel = selectedDetailPanel.model();
			if (entityPanel.model().containsDetailModel(selectedDetailModel)) {
				entityPanel.model().detailModelLink(selectedDetailModel).active().set(panelState.isNotEqualTo(HIDDEN));
			}
			if (previousPanelState() == WINDOW) {
				disposeDetailWindow();
			}
			if (panelState.isEqualTo(EMBEDDED)) {
				splitPane.setRightComponent(tabbedPane);
			}
			else if (panelState.isEqualTo(HIDDEN)) {
				splitPane.setRightComponent(null);
			}
			else {
				displayDetailWindow();
			}

			entityPanel.revalidate();
		}

		private PanelState previousPanelState() {
			if (panelWindow != null) {
				return WINDOW;
			}
			else if (tabbedPane.isShowing()) {
				return EMBEDDED;
			}

			return HIDDEN;
		}

		private void activateDetailModelLink(SwingEntityModel detailModel) {
			SwingEntityModel model = entityPanel.model();
			if (model.containsDetailModel(detailModel)) {
				model.activeDetailModels().get().stream()
								.filter(activeDetailModel -> activeDetailModel != detailModel)
								.forEach(activeDetailModel -> model.detailModelLink(activeDetailModel).active().set(false));
				model.detailModelLink(detailModel).active().set(true);
			}
		}

		private void toggleDetailState() {
			panelState.map(EntityPanel.PANEL_STATE_MAPPER);
		}

		private void displayDetailWindow() {
			Window parent = parentWindow(entityPanel);
			if (parent != null) {
				Dimension parentSize = parent.getSize();
				Dimension size = detailWindowSize(parentSize);
				Point parentLocation = parent.getLocation();
				int detailWindowX = parentLocation.x + (parentSize.width - size.width);
				int detailWindowY = parentLocation.y + (parentSize.height - size.height) - DETAIL_WINDOW_OFFSET;
				panelWindow = createDetailWindow();
				panelWindow.setSize(size);
				panelWindow.setLocation(new Point(detailWindowX, detailWindowY));
				panelWindow.setVisible(true);
			}
		}

		private void disposeDetailWindow() {
			if (panelWindow != null) {
				panelWindow.setVisible(false);
				panelWindow.dispose();
				panelWindow = null;
			}
		}

		private Dimension detailWindowSize(Dimension parentSize) {
			int detailWindowWidth = (int) (parentSize.width * DETAIL_WINDOW_SIZE_RATIO);
			int detailWindowHeight = entityPanel.containsEditPanel() ? (int) (parentSize.height * DETAIL_WINDOW_SIZE_RATIO) : parentSize.height;

			return new Dimension(detailWindowWidth, detailWindowHeight);
		}

		private Window createDetailWindow() {
			if (windowType == WindowType.FRAME) {
				return Windows.frame(createEmptyBorderBasePanel(tabbedPane))
								.title(entityPanel.caption() + " - " + MESSAGES.getString(DETAIL_TABLES))
								.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
								.onClosed(windowEvent -> {
									//the frame can be closed when embedding the panel, don't hide if that's the case
									if (panelState.isNotEqualTo(EMBEDDED)) {
										panelState.set(HIDDEN);
									}
								})
								.build();
			}

			return Dialogs.componentDialog(createEmptyBorderBasePanel(tabbedPane))
							.owner(entityPanel)
							.title(entityPanel.caption() + " - " + MESSAGES.getString(DETAIL_TABLES))
							.modal(false)
							.onClosed(e -> {
								//the dialog can be closed when embedding the panel, don't hide if that's the case
								if (panelState.isNotEqualTo(EMBEDDED)) {
									panelState.set(HIDDEN);
								}
							})
							.build();
		}

		private JPanel createEmptyBorderBasePanel(JComponent component) {
			int gap = Layouts.GAP.get();
			return Components.borderLayoutPanel()
							.centerComponent(component)
							.border(createEmptyBorder(gap, gap, 0, gap))
							.build();
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final KeyboardShortcuts<TabbedDetailLayoutControl> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

		private final EntityPanel entityPanel;

		private PanelState panelState = EMBEDDED;
		private WindowType windowType;
		private double splitPaneResizeWeight = DEFAULT_SPLIT_PANE_RESIZE_WEIGHT;
		private boolean includeControls = INCLUDE_CONTROLS.get();

		private DefaultBuilder(EntityPanel entityPanel) {
			this.entityPanel = requireNonNull(entityPanel);
			this.windowType = entityPanel.windowType();
		}

		@Override
		public Builder panelState(PanelState panelState) {
			this.panelState = requireNonNull(panelState);
			return this;
		}

		@Override
		public Builder windowType(WindowType windowType) {
			this.windowType = requireNonNull(windowType);
			return this;
		}

		@Override
		public Builder splitPaneResizeWeight(double splitPaneResizeWeight) {
			this.splitPaneResizeWeight = splitPaneResizeWeight;
			return this;
		}

		@Override
		public Builder includeControls(boolean includeControls) {
			this.includeControls = includeControls;
			return this;
		}

		@Override
		public Builder keyStroke(TabbedDetailLayoutControl control, KeyStroke keyStroke) {
			keyboardShortcuts.keyStroke(control).set(keyStroke);
			return this;
		}

		@Override
		public TabbedDetailLayout build() {
			return new TabbedDetailLayout(this);
		}
	}
}
