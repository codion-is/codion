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
import is.codion.swing.framework.ui.EntityPanel.DetailLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
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
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static is.codion.swing.framework.ui.EntityPanel.Direction.LEFT;
import static is.codion.swing.framework.ui.EntityPanel.Direction.RIGHT;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.TabbedDetailLayout.KeyboardShortcut.RESIZE_LEFT;
import static is.codion.swing.framework.ui.TabbedDetailLayout.KeyboardShortcut.RESIZE_RIGHT;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Objects.requireNonNull;
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
 * |                  |   panel     |
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

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TabbedDetailLayout.class.getName());

  /**
   * The default keyboard shortcut keyStrokes.
   */
  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS =
          keyboardShortcuts(KeyboardShortcut.class, TabbedDetailLayout::defaultKeyStroke);

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcut {
    /**
     * Resizes this panel to the right.
     */
    RESIZE_RIGHT,
    /**
     * Resizes this panel to the left.
     */
    RESIZE_LEFT
  }

  private static final int RESIZE_AMOUNT = 30;
  private static final String DETAIL_TABLES = "detail_tables";
  private static final double DEFAULT_SPLIT_PANE_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_WINDOW_OFFSET = 38;//titlebar height
  private static final double DETAIL_WINDOW_SIZE_RATIO = 0.66;

  private final TabbedDetailController detailController;
  private final boolean includeTabbedPane;
  private final boolean includeControls;
  private final double splitPaneResizeWeight;
  private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts;

  private EntityPanel entityPanel;
  private JTabbedPane tabbedPane;
  private JSplitPane splitPane;
  private Window panelWindow;
  private PanelState panelState = EMBEDDED;

  private TabbedDetailLayout(DefaultBuilder builder) {
    this.panelState = builder.panelState;
    this.includeTabbedPane = builder.includeTabbedPane;
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
  public JComponent layout(EntityPanel entityPanel) {
    requireNonNull(entityPanel);
    if (this.entityPanel != null) {
      throw new IllegalStateException("EntityPanel has already been laid out: " + entityPanel);
    }
    this.entityPanel = entityPanel;
    if (!includeTabbedPane || entityPanel.detailPanels().isEmpty()) {
      return entityPanel.mainPanel();
    }

    return layoutPanel(entityPanel);
  }

  @Override
  public void select(EntityPanel entityPanel) {
    detailController.select(requireNonNull(entityPanel));
  }

  @Override
  public Value<PanelState> panelState(EntityPanel detailPanel) {
    requireNonNull(detailPanel);

    return detailController.panelState();
  }

  /**
   * @return a new {@link TabbedDetailLayout.Builder} instance
   */
  public static Builder builder() {
    return new DefaultBuilder();
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
     * @param splitPaneResizeWeight the detail panel split pane size weight
     * @return this builder instance
     */
    Builder splitPaneResizeWeight(double splitPaneResizeWeight);

    /**
     * @param includeTabbedPane true if the detail panel tab pane should be included
     * @return this builder instance
     */
    Builder includeTabbedPane(boolean includeTabbedPane);

    /**
     * @param includeControls true if detail panel controls should be available
     * @return this builder instance
     */
    Builder includeControls(boolean includeControls);

    /**
     * @param keyboardShortcut the keyboard shortcut key
     * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
     * @return this builder instance
     */
    Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);

    /**
     * @return a new {@link TabbedDetailLayout} instance based on this builder
     */
    TabbedDetailLayout build();
  }

  private void setupResizing(EntityPanel detailPanel) {
    detailPanel.addKeyEvent(KeyEvents.builder(keyboardShortcuts.keyStroke(RESIZE_RIGHT).get())
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeHorizontally(detailPanel, RIGHT)));
    detailPanel.addKeyEvent(KeyEvents.builder(keyboardShortcuts.keyStroke(RESIZE_LEFT).get())
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeHorizontally(detailPanel, LEFT)));
  }

  private void setupControls(EntityPanel entityPanel) {
    if (entityPanel.containsTablePanel()) {
      EntityTablePanel tablePanel = entityPanel.tablePanel();
      Controls controls = Controls.controls();
      detailController.toggleDetailControl().ifPresent(controls::add);
      if (controls.notEmpty()) {
        tablePanel.addToolBarControls(controls);
      }
      detailController.detailControls().ifPresent(tablePanel::addPopupMenuControls);
    }
  }

  private void initializePanelState() {
    selectedDetailPanel().ifPresent(selectedDetailPanel -> {
      Value<PanelState> detailPanelStateValue = detailController.panelState();
      if (detailPanelStateValue.isNotEqualTo(panelState)) {
        detailPanelStateValue.set(panelState);
      }
      else {
        detailController.updateDetailState();
      }
    });
  }

  private Optional<EntityPanel> selectedDetailPanel() {
    return Optional.ofNullable(tabbedPane == null ? null : (EntityPanel) tabbedPane.getSelectedComponent());
  }

  private JComponent layoutPanel(EntityPanel entityPanel) {
    splitPane = createSplitPane(entityPanel.mainPanel());
    tabbedPane = createTabbedPane(entityPanel.detailPanels());
    entityPanel.detailPanels().forEach(this::setupResizing);
    setupControls(entityPanel);
    initializePanelState();

    return splitPane;
  }

  private JSplitPane createSplitPane(JPanel editControlTablePanel) {
    return splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .dividerSize(GAP.get() * 2)
            .resizeWeight(splitPaneResizeWeight)
            .leftComponent(editControlTablePanel)
            .build();
  }

  private JTabbedPane createTabbedPane(Collection<EntityPanel> detailPanels) {
    TabbedPaneBuilder builder = tabbedPane()
            .focusable(false)
            .changeListener(e -> selectedDetailPanel().ifPresent(EntityPanel::activate))
            .onBuild(tabbedPane -> tabbedPane.setFocusCycleRoot(true));
    detailPanels.forEach(detailPanel -> builder.tabBuilder(detailPanel.caption().get(), detailPanel)
            .toolTipText(detailPanel.description().get())
            .add());
    if (includeControls) {
      builder.mouseListener(new TabbedPaneMouseReleasedListener());
    }

    return builder.build();
  }

  private static KeyStroke defaultKeyStroke(KeyboardShortcut shortcut) {
    switch (shortcut) {
      case RESIZE_LEFT: return keyStroke(VK_LEFT, ALT_DOWN_MASK | SHIFT_DOWN_MASK);
      case RESIZE_RIGHT: return keyStroke(VK_RIGHT, ALT_DOWN_MASK | SHIFT_DOWN_MASK);
      default: throw new IllegalArgumentException();
    }
  }

  private static final class ResizeHorizontally extends AbstractAction {

    private final EntityPanel panel;
    private final EntityPanel.Direction direction;

    private ResizeHorizontally(EntityPanel panel, EntityPanel.Direction direction) {
      super("Resize " + direction);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      panel.parentPanel().ifPresent(parentPanel ->
              resizePanel(parentPanel, direction));
    }

    private static void resizePanel(EntityPanel panel, EntityPanel.Direction direction) {
      TabbedDetailLayout detailPanelLayout = panel.detailLayout();
      JSplitPane splitPane = detailPanelLayout.splitPane;
      switch (requireNonNull(direction)) {
        case RIGHT:
          if (splitPane != null) {
            splitPane.setDividerLocation(Math.min(splitPane.getDividerLocation() + RESIZE_AMOUNT,
                    splitPane.getMaximumDividerLocation()));
          }
          break;
        case LEFT:
          if (splitPane != null) {
            splitPane.setDividerLocation(Math.max(splitPane.getDividerLocation() - RESIZE_AMOUNT, 0));
          }
          break;
        default:
          throw new IllegalArgumentException("Undefined resize direction: " + direction);
      }
    }
  }

  private final class TabbedPaneMouseReleasedListener extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent e) {
      selectedDetailPanel().ifPresent(selectedDetailPanel -> {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
          detailController.panelState().map(state -> state == WINDOW ? EMBEDDED : WINDOW);
        }
        else if (e.getButton() == MouseEvent.BUTTON2) {
          detailController.panelState().map(state -> state == EMBEDDED ? HIDDEN : EMBEDDED);
        }
      });
    }
  }

  private final class TabbedDetailController {

    /**
     * Holds the current state of the detail panels (HIDDEN, EMBEDDED or WINDOW)
     */
    private final Value<PanelState> panelState = Value.value(EMBEDDED, EMBEDDED);

    private TabbedDetailController() {
      panelState.addListener(this::updateDetailState);
    }

    private void select(EntityPanel detailPanel) {
      if (tabbedPane != null) {
        tabbedPane.setFocusable(true);
        tabbedPane.setSelectedComponent(detailPanel);
        tabbedPane.setFocusable(false);
      }
      activateDetailModelLink(detailPanel.model());
    }

    private Value<PanelState> panelState() {
      return panelState;
    }

    private void updateDetailState() {
      selectedDetailPanel().ifPresent(selectedDetailPanel -> {
        if (panelState.isNotEqualTo(HIDDEN)) {
          selectedDetailPanel.initialize();
        }
        SwingEntityModel selectedDetailModel = selectedDetailPanel.model();
        if (entityPanel.model().containsDetailModel(selectedDetailModel)) {
          entityPanel.model().detailModelLink(selectedDetailModel).active().set(panelState.isNotEqualTo(HIDDEN));
        }
      });
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
      if (tabbedPane == null) {
        throw new IllegalStateException("No tabbed detail pane available");
      }
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

    private Optional<Control> toggleDetailControl() {
      if (includeControls && !entityPanel.detailPanels().isEmpty()) {
        return Optional.of(createToggleDetailControl());
      }

      return Optional.empty();
    }


    private Optional<Controls> detailControls() {
      if (includeControls && !entityPanel.detailPanels().isEmpty()) {
        return Optional.of(createDetailControls());
      }

      return Optional.empty();
    }

    private Control createToggleDetailControl() {
      return Control.builder(this::toggleDetailState)
              .smallIcon(FrameworkIcons.instance().detail())
              .description(MESSAGES.getString("toggle_detail"))
              .build();
    }

    private void toggleDetailState() {
      panelState.map(EntityPanel.PANEL_STATE_MAPPER);
    }

    private Controls createDetailControls() {
      Controls.Builder controls = Controls.builder()
              .name(MESSAGES.getString(DETAIL_TABLES))
              .smallIcon(FrameworkIcons.instance().detail());
      entityPanel.detailPanels().forEach(detailPanel ->
              controls.control(Control.builder(new ActivateDetailPanel(detailPanel))
                      .name(detailPanel.caption().get())));

      return controls.build();
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
      if (EntityPanel.Config.USE_FRAME_PANEL_DISPLAY.get()) {
        return Windows.frame(createEmptyBorderBasePanel(tabbedPane))
                .title(entityPanel.caption().get() + " - " + MESSAGES.getString(DETAIL_TABLES))
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
              .title(entityPanel.caption().get() + " - " + MESSAGES.getString(DETAIL_TABLES))
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

    private final class ActivateDetailPanel implements Control.Command {

      private final EntityPanel detailPanel;

      private ActivateDetailPanel(EntityPanel detailPanel) {
        this.detailPanel = detailPanel;
      }

      @Override
      public void execute() {
        if (panelState.isEqualTo(HIDDEN)) {
          panelState.set(EMBEDDED);
        }
        detailPanel.activate();
      }
    }
  }

  private static final class DefaultBuilder implements Builder {

    private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

    private PanelState panelState = EMBEDDED;
    private double splitPaneResizeWeight = DEFAULT_SPLIT_PANE_RESIZE_WEIGHT;
    private boolean includeTabbedPane = true;
    private boolean includeControls = INCLUDE_CONTROLS.get();

    @Override
    public Builder panelState(PanelState panelState) {
      this.panelState = requireNonNull(panelState);
      return this;
    }

    @Override
    public Builder splitPaneResizeWeight(double splitPaneResizeWeight) {
      this.splitPaneResizeWeight = splitPaneResizeWeight;
      return this;
    }

    @Override
    public Builder includeTabbedPane(boolean includeTabbedPane) {
      this.includeTabbedPane = includeTabbedPane;
      return this;
    }

    @Override
    public Builder includeControls(boolean includeControls) {
      this.includeControls = includeControls;
      return this;
    }

    @Override
    public Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
      keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
      return this;
    }

    @Override
    public TabbedDetailLayout build() {
      return new TabbedDetailLayout(this);
    }
  }
}
