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
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlId;
import is.codion.swing.common.ui.control.ControlKeyStrokes;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static is.codion.swing.common.ui.control.ControlId.commandControl;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.controlKeyStrokes;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.EntityPanel.panelStateMapper;
import static is.codion.swing.framework.ui.TabbedDetailLayout.ControlIds.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Arrays.asList;
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
					Configuration.booleanValue(TabbedDetailLayout.class.getName() + ".includeControls", true);

	private static final MessageBundle MESSAGES =
					messageBundle(TabbedDetailLayout.class, getBundle(TabbedDetailLayout.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * The default keyboard shortcut keyStrokes.
	 */
	public static final ControlKeyStrokes CONTROL_KEY_STROKES = controlKeyStrokes(ControlIds.class);

	/**
	 * The controls.
	 */
	public static final class ControlIds {

		/**
		 * Resizes the detail panel to the right.<br>
		 * Default key stroke: SHIFT-ALT-RIGHT ARROW
		 */
		public static final ControlId<CommandControl> RESIZE_RIGHT = commandControl(keyStroke(VK_RIGHT, ALT_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Resizes the detail panel to the left.<br>
		 * Default key stroke: SHIFT-ALT-LEFT ARROW
		 */
		public static final ControlId<CommandControl> RESIZE_LEFT = commandControl(keyStroke(VK_LEFT, ALT_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Collapses the detail panel all the way to the right, hiding it.<br>
		 * Default key stroke: SHIFT-CTRL-ALT RIGHT ARROW
		 */
		public static final ControlId<CommandControl> COLLAPSE = commandControl(keyStroke(VK_RIGHT, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Expands the detail panel all the way to the left, hiding the parent.<br>
		 * Default key stroke: SHIFT-CTRL-ALT LEFT ARROW
		 */
		public static final ControlId<CommandControl> EXPAND = commandControl(keyStroke(VK_LEFT, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK));

		private ControlIds() {}
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
	private final ControlKeyStrokes controlKeyStrokes;
	private final WindowType windowType;

	private JTabbedPane tabbedPane;
	private JSplitPane splitPane;
	private Window panelWindow;

	private TabbedDetailLayout(DefaultBuilder builder) {
		this.entityPanel = builder.entityPanel;
		this.windowType = builder.windowType;
		this.includeControls = builder.includeControls;
		this.splitPaneResizeWeight = builder.splitPaneResizeWeight;
		this.detailController = new TabbedDetailController(builder.enabledDetailStates, builder.initialState);
		this.controlKeyStrokes = builder.controlKeyStrokes;
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
		detailController.initialize();

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
		 * Default {@link PanelState#EMBEDDED}
		 * @param initialState the initial detail panel state
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the given state is {@link PanelState#WINDOW}
		 * @throws IllegalArgumentException in case the given state is not enabled
		 * @see #enabledDetailStates(PanelState...)
		 */
		Builder initialDetailState(PanelState initialState);

		/**
		 * Sets the enabled detail panel states, with the first being the initial one.
		 * Note that {@link PanelState#WINDOW} is not supported as the initial state.
		 * @param panelStates the enabled detail panel states
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the given states do not include the initial state
		 * @throws IllegalArgumentException in case no {@code panelStates} are specified
		 */
		Builder enabledDetailStates(PanelState... panelStates);

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
		 * @param controlId the control id
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(ControlId<?> controlId, KeyStroke keyStroke);

		/**
		 * @return a new {@link TabbedDetailLayout} instance based on this builder
		 */
		TabbedDetailLayout build();
	}

	private void bindEvents(EntityPanel detailPanel) {
		detailPanel.activateEvent().addConsumer(detailController::activated);
		controlKeyStrokes.keyStroke(RESIZE_RIGHT).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, true, false))));
		controlKeyStrokes.keyStroke(RESIZE_LEFT).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, false, false))));
		controlKeyStrokes.keyStroke(COLLAPSE).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, true, true))));
		controlKeyStrokes.keyStroke(EXPAND).optional().ifPresent(keyStroke ->
						detailPanel.addKeyEvent(KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(new ResizeAction(detailPanel, false, true))));
	}

	private void setupControls(EntityPanel entityPanel) {
		if (entityPanel.containsTablePanel() && includeControls) {
			EntityTablePanel tablePanel = entityPanel.tablePanel();
			tablePanel.addToolBarControls(Controls.builder()
							.control(Control.builder()
											.command(detailController::toggleDetailState)
											.smallIcon(ICONS.detail())
											.description(MESSAGES.getString("toggle_detail")))
							.build());
			tablePanel.addPopupMenuControls(Controls.builder()
							.name(MESSAGES.getString(DETAIL_TABLES))
							.smallIcon(ICONS.detail())
							.controls(entityPanel.detailPanels().stream()
											.map(detailPanel -> Control.builder()
															.command(new ActivateDetailPanel(detailPanel))
															.name(detailPanel.caption())
															.build())
											.toArray(Control[]::new))
							.build());
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
				detailController.panelState.set(detailController.panelStateMapper.apply(HIDDEN));
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
			if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 &&
							detailController.panelStates.containsAll(asList(WINDOW, EMBEDDED))) {
				detailController.panelState(selectedDetailPanel).map(state -> state == WINDOW ? EMBEDDED : WINDOW);
			}
			else if (e.getButton() == MouseEvent.BUTTON2 &&
							detailController.panelStates.containsAll(asList(HIDDEN, EMBEDDED))) {
				detailController.panelState(selectedDetailPanel).map(state -> state == EMBEDDED ? HIDDEN : EMBEDDED);
			}
		}
	}

	private final class TabbedDetailController implements DetailController {

		private final Set<PanelState> panelStates;
		private final Function<PanelState, PanelState> panelStateMapper;
		private final Value<PanelState> panelState;

		private TabbedDetailController(Set<PanelState> panelStates, PanelState initialState) {
			this.panelStates = panelStates;
			this.panelStateMapper = panelStateMapper(panelStates);
			this.panelState = Value.nonNull(initialState)
							.consumer(this::updateDetailState)
							.build();
		}

		@Override
		public void activated(EntityPanel detailPanel) {
			requireNonNull(detailPanel);
			if (tabbedPane == null) {
				throw new IllegalStateException("Detail panel has not been laid out");
			}
			tabbedPane.setFocusable(true);
			tabbedPane.setSelectedComponent(detailPanel);
			tabbedPane.setFocusable(false);
			showDetailPanel();
			activateDetailModelLink(detailPanel.model());
		}

		@Override
		public Value<PanelState> panelState(EntityPanel detailPanel) {
			requireNonNull(detailPanel);

			return panelState;
		}

		private void showDetailPanel() {
			if (splitPane.getDividerLocation() == splitPane.getMaximumDividerLocation()) {
				splitPane.resetToPreferredSizes();
			}
			if (detailController.panelState.isEqualTo(HIDDEN)) {
				panelState.set(panelStateMapper.apply(HIDDEN));
			}
		}

		private void updateDetailState(PanelState newState) {
			SwingEntityModel model = selectedDetailPanel().model();
			switch (newState) {
				case HIDDEN:
					deactivateDetailModelLink(model);
					disposeDetailWindow();
					splitPane.setRightComponent(null);
					break;
				case EMBEDDED:
					activateDetailModelLink(model);
					selectedDetailPanel().initialize();
					disposeDetailWindow();
					splitPane.setRightComponent(tabbedPane);
					break;
				case WINDOW:
					activateDetailModelLink(model);
					selectedDetailPanel().initialize();
					displayDetailWindow();
					break;
			}
			entityPanel.revalidate();
		}

		private void deactivateDetailModelLink(SwingEntityModel detailModel) {
			SwingEntityModel model = entityPanel.model();
			if (model.containsDetailModel(detailModel)) {
				model.detailModelLink(detailModel).active().set(false);
			}
		}

		private void activateDetailModelLink(SwingEntityModel detailModel) {
			SwingEntityModel model = entityPanel.model();
			if (model.containsDetailModel(detailModel)) {
				model.linkedDetailModels().get().stream()
								.filter(linkedDetailModel -> linkedDetailModel != detailModel)
								.forEach(linkedDetailModel -> model.detailModelLink(linkedDetailModel).active().set(false));
				model.detailModelLink(detailModel).active().set(true);
			}
		}

		private void initialize() {
			updateDetailState(panelState.get());
		}

		private void toggleDetailState() {
			panelState.map(panelStateMapper);
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

		private static JPanel createEmptyBorderBasePanel(JComponent component) {
			int gap = Layouts.GAP.get();
			return Components.borderLayoutPanel()
							.centerComponent(component)
							.border(createEmptyBorder(gap, gap, 0, gap))
							.build();
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final ControlKeyStrokes controlKeyStrokes = CONTROL_KEY_STROKES.copy();

		private final EntityPanel entityPanel;
		private final Set<PanelState> enabledDetailStates =
						new LinkedHashSet<>(asList(PanelState.values()));

		private PanelState initialState = EMBEDDED;
		private WindowType windowType;
		private double splitPaneResizeWeight = DEFAULT_SPLIT_PANE_RESIZE_WEIGHT;
		private boolean includeControls = INCLUDE_CONTROLS.get();

		private DefaultBuilder(EntityPanel entityPanel) {
			this.entityPanel = requireNonNull(entityPanel);
			this.windowType = entityPanel.windowType();
		}

		@Override
		public Builder initialDetailState(PanelState initialState) {
			if (requireNonNull(initialState) == WINDOW) {
				throw new IllegalArgumentException(WINDOW + " is not a supported initial state");
			}
			if (!enabledDetailStates.contains(initialState)) {
				throw new IllegalArgumentException("Detail state: " + initialState + " is not enabled");
			}
			this.initialState = initialState;
			return this;
		}

		@Override
		public Builder enabledDetailStates(PanelState... panelStates) {
			if (requireNonNull(panelStates).length == 0) {
				throw new IllegalArgumentException("No detail panel states specified");
			}
			List<PanelState> states = asList(panelStates);
			if (!states.contains(initialState)) {
				throw new IllegalArgumentException("Detail state has already been set to: " + initialState);
			}
			this.enabledDetailStates.clear();
			this.enabledDetailStates.addAll(states);
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
		public Builder keyStroke(ControlId<?> controlId, KeyStroke keyStroke) {
			controlKeyStrokes.keyStroke(controlId).set(keyStroke);
			return this;
		}

		@Override
		public TabbedDetailLayout build() {
			return new TabbedDetailLayout(this);
		}
	}
}
