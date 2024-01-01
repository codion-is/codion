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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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
import is.codion.swing.framework.ui.EntityPanel.PanelLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityPanel.Direction.LEFT;
import static is.codion.swing.framework.ui.EntityPanel.Direction.RIGHT;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.TabbedPanelLayout.KeyboardShortcut.RESIZE_LEFT;
import static is.codion.swing.framework.ui.TabbedPanelLayout.KeyboardShortcut.RESIZE_RIGHT;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_R;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

/**
 * A {@link PanelLayout} implementation based on a JTabbedPane.<br>
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
public final class TabbedPanelLayout implements PanelLayout {

  /**
   * Specifies whether actions to hide detail panels or show them in a dialog should be available to the user,
   * for example in a popup menu or on a toolbar.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_DETAIL_CONTROLS =
          Configuration.booleanValue("is.codion.swing.framework.ui.TabbedPanelLayout.includeDetailControls", true);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TabbedPanelLayout.class.getName());

  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS = keyboardShortcuts(KeyboardShortcut.class, new DefaultKeyboardShortcuts());

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcut {
    RESIZE_RIGHT,
    RESIZE_LEFT
  }

  private static final int RESIZE_AMOUNT = 30;
  private static final String DETAIL_TABLES = "detail_tables";
  private static final double DEFAULT_SPLIT_PANE_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_WINDOW_OFFSET = 38;//titlebar height
  private static final double DETAIL_WINDOW_SIZE_RATIO = 0.66;
  private final TabbedDetailController detailController;

  private EntityPanel entityPanel;
  private JTabbedPane detailPanelTabbedPane;
  private JSplitPane tableDetailSplitPane;
  private Window detailPanelWindow;
  private PanelState detailPanelState = EMBEDDED;
  private final boolean includeDetailTabPane;
  private final boolean includeDetailPanelControls;
  private final double splitPaneResizeWeight;
  private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts;

  private TabbedPanelLayout(DefaultBuilder builder) {
    this.detailPanelState = builder.detailPanelState;
    this.includeDetailTabPane = builder.includeDetailTabPane;
    this.includeDetailPanelControls = builder.includeDetailControls;
    this.splitPaneResizeWeight = builder.splitPaneResizeWeight;
    this.detailController = new TabbedDetailController();
    this.keyboardShortcuts = builder.keyboardShortcuts;
  }

  @Override
  public void updateUI() {
    Utilities.updateUI(detailPanelTabbedPane, tableDetailSplitPane);
  }

  @Override
  public void layout(EntityPanel entityPanel) {
    this.entityPanel = requireNonNull(entityPanel);
    tableDetailSplitPane = createTableDetailSplitPane();
    detailPanelTabbedPane = createDetailTabbedPane();
    entityPanel.setLayout(borderLayout());
    entityPanel.add(tableDetailSplitPane == null ?
            entityPanel.editControlTablePanel() :
            tableDetailSplitPane, BorderLayout.CENTER);
    setupResizing();
    setupControls();
    initializeDetailPanelState();
  }

  @Override
  public <T extends EntityPanel.DetailController> Optional<T> detailController() {
    return Optional.of((T) detailController);
  }

  /**
   * @param detailPanelState the detail panel state
   * @return a new {@link TabbedPanelLayout} with the given detail panel state
   */
  public static TabbedPanelLayout detailPanelState(PanelState detailPanelState) {
    return builder().detailPanelState(detailPanelState).build();
  }

  /**
   * @param splitPaneResizeWeight the split pane resize weight
   * @return a new {@link TabbedPanelLayout} with the given split pane resize weight
   */
  public static TabbedPanelLayout splitPaneResizeWeight(double splitPaneResizeWeight) {
    return builder().splitPaneResizeWeight(splitPaneResizeWeight).build();
  }

  /**
   * @return a new {@link TabbedPanelLayout.Builder} instance
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  /**
   * Builds a {@link TabbedPanelLayout}.
   */
  public interface Builder {

    /**
     * @param detailPanelState the initial detail panel state
     * @return this builder instance
     */
    Builder detailPanelState(PanelState detailPanelState);

    /**
     * @param splitPaneResizeWeight the detail panel split pane size weight
     * @return this builder instance
     */
    Builder splitPaneResizeWeight(double splitPaneResizeWeight);

    /**
     * @param includeDetailTabPane true if the detail panel tab pane should be included
     * @return this builder instance
     */
    Builder includeDetailTabPane(boolean includeDetailTabPane);

    /**
     * @param includeDetailControls true if detail panel controls should be available
     * @return this builder instance
     */
    Builder includeDetailControls(boolean includeDetailControls);

    /**
     * @param keyboardShortcut the keyboard shortcut key
     * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
     * @return this builder instance
     */
    Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);

    /**
     * @return a new {@link TabbedPanelLayout} instance based on this builder
     */
    TabbedPanelLayout build();
  }

  private void setupResizing() {
    KeyEvents.Builder resizeRightKeyEvent = KeyEvents.builder(keyboardShortcuts.keyStroke(RESIZE_RIGHT).get())
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeHorizontally(entityPanel, RIGHT));
    KeyEvents.Builder resizeLeftKeyEvent = KeyEvents.builder(keyboardShortcuts.keyStroke(RESIZE_LEFT).get())
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeHorizontally(entityPanel, LEFT));
    resizeRightKeyEvent.enable(entityPanel);
    resizeLeftKeyEvent.enable(entityPanel);
    if (entityPanel.containsEditPanel()) {
      resizeRightKeyEvent.enable(entityPanel.editControlPanel());
      resizeLeftKeyEvent.enable(entityPanel.editControlPanel());
    }
  }

  private void setupControls() {
    if (entityPanel.containsTablePanel()) {
      EntityTablePanel tablePanel = entityPanel.tablePanel();
      Controls controls = Controls.controls();
      detailController.toggleDetailControl().ifPresent(controls::add);
      if (controls.notEmpty()) {
        tablePanel.configure().addToolBarControls(controls);
      }
      detailController.detailControls().ifPresent(tablePanel.configure()::addPopupMenuControls);
    }
  }

  private void initializeDetailPanelState() {
    if (detailPanelTabbedPane != null) {
      Value<PanelState> detailPanelStateValue = detailController.panelState(selectedDetailPanel());
      if (detailPanelStateValue.notEqualTo(detailPanelState)) {
        detailPanelStateValue.set(detailPanelState);
      }
      else {
        detailController.updateDetailState();
      }
    }
  }

  private EntityPanel selectedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new IllegalStateException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  private JSplitPane createTableDetailSplitPane() {
    if (!includeDetailTabPane || entityPanel.detailPanels().isEmpty()) {
      return null;
    }

    return splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .dividerSize(GAP.get() * 2)
            .resizeWeight(splitPaneResizeWeight)
            .leftComponent(entityPanel.editControlTablePanel())
            .rightComponent(detailPanelTabbedPane)
            .build();
  }

  private JTabbedPane createDetailTabbedPane() {
    if (!includeDetailTabPane || entityPanel.detailPanels().isEmpty()) {
      return null;
    }

    TabbedPaneBuilder builder = tabbedPane()
            .focusable(false)
            .changeListener(e -> selectedDetailPanel().activate());
    entityPanel.detailPanels().forEach(detailPanel -> builder.tabBuilder(detailPanel.caption().get(), detailPanel)
            .toolTipText(detailPanel.getDescription())
            .add());
    if (includeDetailPanelControls) {
      builder.mouseListener(new TabbedPaneMouseReleasedListener());
    }

    return builder.build();
  }

  private static final class DefaultKeyboardShortcuts implements Function<KeyboardShortcut, KeyStroke> {

    @Override
    public KeyStroke apply(KeyboardShortcut shortcut) {
      switch (shortcut) {
        case RESIZE_LEFT: return keyStroke(VK_LEFT, ALT_DOWN_MASK | SHIFT_DOWN_MASK);
        case RESIZE_RIGHT: return keyStroke(VK_R, ALT_DOWN_MASK | SHIFT_DOWN_MASK);
        default: throw new IllegalArgumentException();
      }
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
      TabbedPanelLayout detailPanelLayout = panel.panelLayout();
      JSplitPane splitPane = detailPanelLayout.tableDetailSplitPane;
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
      EntityPanel detailPanel = selectedDetailPanel();
      if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
        detailController.panelState(detailPanel).map(panelState -> panelState == WINDOW ? EMBEDDED : WINDOW);
      }
      else if (e.getButton() == MouseEvent.BUTTON2) {
        detailController.panelState(detailPanel).map(panelState -> panelState == EMBEDDED ? HIDDEN : EMBEDDED);
      }
    }
  }

  private final class TabbedDetailController implements EntityPanel.DetailController {

    /**
     * Holds the current state of the detail panels (HIDDEN, EMBEDDED or WINDOW)
     */
    private final Value<PanelState> panelState = Value.value(EMBEDDED, EMBEDDED);
    private final PanelStateMapper stateMapper = new PanelStateMapper();

    private TabbedDetailController() {
      panelState.addListener(this::updateDetailState);
    }

    @Override
    public void select(EntityPanel detailPanel) {
      if (detailPanelTabbedPane != null) {
        detailPanelTabbedPane.setSelectedComponent(detailPanel);
      }
      activateDetailModelLink(detailPanel.model());
    }

    @Override
    public Value<PanelState> panelState(EntityPanel detailPanel) {
      requireNonNull(detailPanel);

      return panelState;
    }

    private void updateDetailState() {
      if (detailPanelTabbedPane == null) {
        return;
      }

      PanelState previousPanelState = previousPanelState();
      if (panelState.notEqualTo(HIDDEN)) {
        selectedDetailPanel().initialize();
      }

      if (previousPanelState == WINDOW) {//if we are leaving the WINDOW state, hide all child detail windows
        for (EntityPanel detailPanel : entityPanel.detailPanels()) {
          detailPanel.panelLayout().detailController()
                  .filter(TabbedDetailController.class::isInstance)
                  .map(TabbedDetailController.class::cast)
                  .ifPresent(controller -> {
                    if (controller.panelState.equalTo(WINDOW)) {
                      controller.panelState.set(HIDDEN);
                    }
                  });
        }
        disposeDetailWindow();
      }

      SwingEntityModel detailModel = selectedDetailPanel().model();
      if (entityPanel.model().containsDetailModel(detailModel)) {
        entityPanel.model().detailModelLink(detailModel)
                .active().set(panelState.notEqualTo(HIDDEN));
      }
      if (panelState.equalTo(EMBEDDED)) {
        if (tableDetailSplitPane.getRightComponent() != detailPanelTabbedPane) {
          tableDetailSplitPane.setRightComponent(detailPanelTabbedPane);
        }
      }
      else if (panelState.equalTo(HIDDEN)) {
        tableDetailSplitPane.setRightComponent(null);
      }
      else {
        showDetailWindow();
      }

      entityPanel.revalidate();
    }

    private PanelState previousPanelState() {
      if (detailPanelWindow != null) {
        return WINDOW;
      }
      else if (detailPanelTabbedPane.isShowing()) {
        return EMBEDDED;
      }

      return HIDDEN;
    }

    private void activateDetailModelLink(SwingEntityModel detailModel) {
      SwingEntityModel model = entityPanel.model();
      if (model.containsDetailModel(detailModel)) {
        model.activeDetailModels().stream()
                .filter(activeDetailModel -> activeDetailModel != detailModel)
                .forEach(activeDetailModel -> model.detailModelLink(activeDetailModel).active().set(false));
        model.detailModelLink(detailModel).active().set(true);
      }
    }

    private Optional<Control> toggleDetailControl() {
      if (includeDetailPanelControls && !entityPanel.detailPanels().isEmpty()) {
        return Optional.of(createToggleDetailControl());
      }

      return Optional.empty();
    }


    private Optional<Controls> detailControls() {
      if (includeDetailPanelControls && !entityPanel.detailPanels().isEmpty()) {
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
      panelState.map(stateMapper);
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

    private void showDetailWindow() {
      Window parent = parentWindow(entityPanel);
      if (parent != null) {
        Dimension parentSize = parent.getSize();
        Dimension size = detailWindowSize(parentSize);
        Point parentLocation = parent.getLocation();
        int detailWindowX = parentLocation.x + (parentSize.width - size.width);
        int detailWindowY = parentLocation.y + (parentSize.height - size.height) - DETAIL_WINDOW_OFFSET;
        detailPanelWindow = createDetailWindow();
        detailPanelWindow.setSize(size);
        detailPanelWindow.setLocation(new Point(detailWindowX, detailWindowY));
        detailPanelWindow.setVisible(true);
      }
    }

    private void disposeDetailWindow() {
      if (detailPanelWindow != null) {
        detailPanelWindow.setVisible(false);
        detailPanelWindow.dispose();
        detailPanelWindow = null;
      }
    }

    private Dimension detailWindowSize(Dimension parentSize) {
      int detailWindowWidth = (int) (parentSize.width * DETAIL_WINDOW_SIZE_RATIO);
      int detailWindowHeight = entityPanel.containsEditPanel() ? (int) (parentSize.height * DETAIL_WINDOW_SIZE_RATIO) : parentSize.height;

      return new Dimension(detailWindowWidth, detailWindowHeight);
    }

    private Window createDetailWindow() {
      if (EntityPanel.USE_FRAME_PANEL_DISPLAY.get()) {
        return Windows.frame(createEmptyBorderBasePanel(detailPanelTabbedPane))
                .title(entityPanel.caption().get() + " - " + MESSAGES.getString(DETAIL_TABLES))
                .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                .onClosed(windowEvent -> {
                  //the frame can be closed when embedding the panel, don't hide if that's the case
                  if (panelState.notEqualTo(EMBEDDED)) {
                    panelState.set(HIDDEN);
                  }
                })
                .build();
      }

      return Dialogs.componentDialog(createEmptyBorderBasePanel(detailPanelTabbedPane))
              .owner(entityPanel)
              .title(entityPanel.caption().get() + " - " + MESSAGES.getString(DETAIL_TABLES))
              .modal(false)
              .onClosed(e -> {
                //the dialog can be closed when embedding the panel, don't hide if that's the case
                if (panelState.notEqualTo(EMBEDDED)) {
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
        if (panelState.equalTo(HIDDEN)) {
          panelState.set(EMBEDDED);
        }
        detailPanel.activate();
      }
    }

    private final class PanelStateMapper implements Function<PanelState, PanelState> {

      @Override
      public PanelState apply(PanelState state) {
        switch (state) {
          case HIDDEN:
            return EMBEDDED;
          case EMBEDDED:
            return WINDOW;
          case WINDOW:
            return HIDDEN;
          default:
            throw new IllegalArgumentException("Unknown panel state: " + state);
        }
      }
    }
  }

  private static final class DefaultBuilder implements Builder {

    private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

    private PanelState detailPanelState = EMBEDDED;
    private double splitPaneResizeWeight = DEFAULT_SPLIT_PANE_RESIZE_WEIGHT;
    private boolean includeDetailTabPane = true;
    private boolean includeDetailControls = INCLUDE_DETAIL_CONTROLS.get();

    @Override
    public Builder detailPanelState(PanelState detailPanelState) {
      this.detailPanelState = requireNonNull(detailPanelState);
      return this;
    }

    @Override
    public Builder splitPaneResizeWeight(double splitPaneResizeWeight) {
      this.splitPaneResizeWeight = splitPaneResizeWeight;
      return this;
    }

    @Override
    public Builder includeDetailTabPane(boolean includeDetailTabPane) {
      this.includeDetailTabPane = includeDetailTabPane;
      return this;
    }

    @Override
    public Builder includeDetailControls(boolean includeDetailControls) {
      this.includeDetailControls = includeDetailControls;
      return this;
    }

    @Override
    public Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
      keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
      return this;
    }

    @Override
    public TabbedPanelLayout build() {
      return new TabbedPanelLayout(this);
    }
  }
}
