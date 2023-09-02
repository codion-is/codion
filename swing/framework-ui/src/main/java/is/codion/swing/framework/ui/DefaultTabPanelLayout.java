/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel.DetailController;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityPanel.Direction.LEFT;
import static is.codion.swing.framework.ui.EntityPanel.Direction.RIGHT;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

final class DefaultTabPanelLayout implements TabbedPanelLayout {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultTabPanelLayout.class.getName());

  private static final int RESIZE_AMOUNT = 30;
  private static final String DETAIL_TABLES = "detail_tables";
  private static final double DEFAULT_SPLIT_PANE_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_WINDOW_OFFSET = 38;//titlebar height
  private static final double DETAIL_WINDOW_SIZE_RATIO = 0.66;

  private final DetailController detailController;

  private EntityPanel entityPanel;
  private JTabbedPane detailPanelTabbedPane;
  private JSplitPane tableDetailSplitPane;
  private Window detailPanelWindow;
  private PanelState detailPanelState = EMBEDDED;
  private final boolean includeDetailTabPane;
  private final boolean includeDetailPanelControls;
  private final double splitPaneResizeWeight;

  private DefaultTabPanelLayout(DefaultBuilder builder) {
    this.detailPanelState = builder.detailPanelState;
    this.includeDetailTabPane = builder.includeDetailTabPane;
    this.includeDetailPanelControls = builder.includeDetailPanelControls;
    this.splitPaneResizeWeight = builder.splitPaneResizeWeight;
    this.detailController = new TabbedDetailController();
  }

  @Override
  public void updateUI() {
    Utilities.updateUI(detailPanelTabbedPane, tableDetailSplitPane);
  }

  @Override
  public void layoutPanel(EntityPanel entityPanel) {
    this.entityPanel = entityPanel;
    TabbedPanelLayout.super.layoutPanel(entityPanel);
    tableDetailSplitPane = createTableDetailSplitPane();
    detailPanelTabbedPane = createDetailTabbedPane();
    entityPanel.setLayout(borderLayout());
    entityPanel.add(tableDetailSplitPane == null ?
            entityPanel.editControlTablePanel() :
            Components.borderLayoutPanel()
                    .centerComponent(tableDetailSplitPane)
                    .build(), BorderLayout.CENTER);
    setupResizing();
    detailController.setDetailPanelState(detailPanelState);
  }

  @Override
  public <T extends DetailController> Optional<T> detailController() {
    return Optional.of((T) detailController);
  }

  private void setupResizing() {
    ResizeHorizontallyAction resizeRightAction = new ResizeHorizontallyAction(entityPanel, RIGHT);
    ResizeHorizontallyAction resizeLeftAction = new ResizeHorizontallyAction(entityPanel, LEFT);

    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeRightAction)
            .enable(entityPanel);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeLeftAction)
            .enable(entityPanel);
    if (entityPanel.containsEditPanel()) {
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeRightAction)
              .enable(entityPanel.editControlPanel());
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeLeftAction)
              .enable(entityPanel.editControlPanel());
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
            .border(BorderFactory.createEmptyBorder())//minor facelift when using metal laf
            .resizeWeight(splitPaneResizeWeight)
            .leftComponent(entityPanel.editControlTablePanel())
            .rightComponent(detailPanelTabbedPane)
            .build();
  }

  /**
   * Creates the JTabbedPane containing the detail panels, used in case of multiple detail panels
   *
   * @return the JTabbedPane for holding detail panels
   */
  private JTabbedPane createDetailTabbedPane() {
    if (!includeDetailTabPane || entityPanel.detailPanels().isEmpty()) {
      return null;
    }

    TabbedPaneBuilder builder = tabbedPane()
            .focusable(false)
            .changeListener(e -> selectedDetailPanel().activatePanel());
    entityPanel.detailPanels().forEach(detailPanel -> builder.tabBuilder(detailPanel.getCaption(), detailPanel)
            .toolTipText(detailPanel.getDescription())
            .add());
    if (includeDetailPanelControls) {
      builder.mouseListener(new TabbedPaneMouseReleasesListener());
    }

    return builder.build();
  }

  private static final class ResizeHorizontallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final EntityPanel.Direction direction;

    private ResizeHorizontallyAction(EntityPanel panel, EntityPanel.Direction direction) {
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
      DefaultTabPanelLayout detailPanelLayout = panel.panelLayout();
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

  private final class TabbedPaneMouseReleasesListener extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent e) {
      PanelState panelState = detailController.getDetailPanelState();
      if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
        detailController.setDetailPanelState(panelState == WINDOW ? EMBEDDED : WINDOW);
      }
      else if (e.getButton() == MouseEvent.BUTTON2) {
        detailController.setDetailPanelState(panelState == EMBEDDED ? HIDDEN : EMBEDDED);
      }
    }
  }

  private final class TabbedDetailController implements DetailController {

    /**
     * Holds the current state of the detail panels (HIDDEN, EMBEDDED or WINDOW)
     */
    private PanelState detailPanelState = EMBEDDED;

    @Override
    public void selectDetailPanel(EntityPanel detailPanel) {
      if (detailPanelTabbedPane != null) {
        detailPanelTabbedPane.setSelectedComponent(detailPanel);
      }
      activateDetailModelLink(detailPanel.model());
    }

    @Override
    public PanelState getDetailPanelState() {
      return detailPanelState;
    }

    @Override
    public void setDetailPanelState(PanelState detailPanelState) {
      requireNonNull(detailPanelState);
      PanelState previousPanelState = this.detailPanelState;
      this.detailPanelState = detailPanelState;
      if (detailPanelTabbedPane == null) {
        this.detailPanelState = detailPanelState;
        return;
      }

      if (detailPanelState != HIDDEN) {
        selectedDetailPanel().initialize();
      }

      if (previousPanelState == WINDOW) {//if we are leaving the WINDOW state, hide all child detail windows
        for (EntityPanel detailPanel : entityPanel.detailPanels()) {
          detailPanel.panelLayout().detailController()
                  .filter(TabbedDetailController.class::isInstance)
                  .map(TabbedDetailController.class::cast)
                  .ifPresent(detailPanelController -> {
                    TabbedDetailController tabDetailPanelLayout = detailPanelController;
                    if (tabDetailPanelLayout.detailPanelState == WINDOW) {
                      tabDetailPanelLayout.setDetailPanelState(HIDDEN);
                    }
                  });
        }
        disposeDetailWindow();
      }

      SwingEntityModel detailModel = selectedDetailPanel().model();
      if (entityPanel.model().containsDetailModel(detailModel)) {
        entityPanel.model().detailModelLink(detailModel).setActive(detailPanelState != HIDDEN);
      }

      if (detailPanelState == EMBEDDED) {
        if (tableDetailSplitPane.getRightComponent() != detailPanelTabbedPane) {
          tableDetailSplitPane.setRightComponent(detailPanelTabbedPane);
        }
      }
      else if (detailPanelState == HIDDEN) {
        tableDetailSplitPane.setRightComponent(null);
      }
      else {
        showDetailWindow();
      }

      entityPanel.revalidate();
    }

    /**
     * Toggles the detail panel state between WINDOW, HIDDEN and EMBEDDED
     */
    @Override
    public void toggleDetailPanelState() {
      if (detailPanelState == WINDOW) {
        setDetailPanelState(HIDDEN);
      }
      else if (detailPanelState == EMBEDDED) {
        setDetailPanelState(WINDOW);
      }
      else {
        setDetailPanelState(EMBEDDED);
      }
    }

    @Override
    public void setupTablePanelControls(EntityTablePanel tablePanel) {
      Controls controls = Controls.controls();
      toggleDetailPanelControl().ifPresent(controls::add);
      if (controls.isNotEmpty()) {
        tablePanel.addToolBarControls(controls);
      }
      detailPanelControls().ifPresent(tablePanel::addPopupMenuControls);
    }

    private void activateDetailModelLink(SwingEntityModel detailModel) {
      SwingEntityModel model = entityPanel.model();
      if (model.containsDetailModel(detailModel)) {
        model.activeDetailModels().forEach(linkedDetailModel ->
                model.detailModelLink(linkedDetailModel).setActive(false));
        model.detailModelLink(detailModel).setActive(true);
      }
    }

    private Optional<Control> toggleDetailPanelControl() {
      if (includeDetailPanelControls && !entityPanel.detailPanels().isEmpty()) {
        return Optional.of(createToggleDetailPanelControl());
      }

      return Optional.empty();
    }


    private Optional<Controls> detailPanelControls() {
      if (includeDetailPanelControls && !entityPanel.detailPanels().isEmpty()) {
        return Optional.of(createDetailPanelControls());
      }

      return Optional.empty();
    }

    /**
     * @return a control for toggling the detail panel
     */
    private Control createToggleDetailPanelControl() {
      return Control.builder(this::toggleDetailPanelState)
              .smallIcon(FrameworkIcons.instance().detail())
              .description(MESSAGES.getString("toggle_detail"))
              .build();
    }

    private Controls createDetailPanelControls() {
      Controls.Builder controls = Controls.builder()
              .name(MESSAGES.getString(DETAIL_TABLES))
              .smallIcon(FrameworkIcons.instance().detail());
      entityPanel.detailPanels().forEach(detailPanel ->
              controls.control(Control.builder(new SelectDetailPanelCommand(detailPanel))
                      .name(detailPanel.getCaption())));

      return controls.build();
    }

    /**
     * Shows the detail panels in a window
     */
    private void showDetailWindow() {
      Window parent = parentWindow(entityPanel);
      if (parent != null) {
        Dimension parentSize = parent.getSize();
        Dimension size = detailWindowSize(parentSize);
        Point parentLocation = parent.getLocation();
        int detailWindowX = parentLocation.x + (parentSize.width - size.width);
        int detailWindowY = parentLocation.y + (parentSize.height - size.height) - DETAIL_WINDOW_OFFSET;
        detailPanelWindow = createDetailPanelWindow();
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

    /**
     * @param parentSize the size of the parent window
     * @return the size to use when showing the detail window
     */
    private Dimension detailWindowSize(Dimension parentSize) {
      int detailWindowWidth = (int) (parentSize.width * DETAIL_WINDOW_SIZE_RATIO);
      int detailWindowHeight = entityPanel.containsEditPanel() ? (int) (parentSize.height * DETAIL_WINDOW_SIZE_RATIO) : parentSize.height;

      return new Dimension(detailWindowWidth, detailWindowHeight);
    }

    private Window createDetailPanelWindow() {
      if (EntityPanel.USE_FRAME_PANEL_DISPLAY.get()) {
        return Windows.frame(detailPanelTabbedPane)
                .title(entityPanel.getCaption() + " - " + MESSAGES.getString(DETAIL_TABLES))
                .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                .onClosed(windowEvent -> {
                  //the frame can be closed when embedding the panel, don't hide if that's the case
                  if (getDetailPanelState() != EMBEDDED) {
                    setDetailPanelState(HIDDEN);
                  }
                })
                .build();
      }

      return Dialogs.componentDialog(detailPanelTabbedPane)
              .owner(entityPanel)
              .title(entityPanel.getCaption() + " - " + MESSAGES.getString(DETAIL_TABLES))
              .modal(false)
              .onClosed(e -> {
                //the dialog can be closed when embedding the panel, don't hide if that's the case
                if (getDetailPanelState() != EMBEDDED) {
                  setDetailPanelState(HIDDEN);
                }
              })
              .build();
    }

    private final class SelectDetailPanelCommand implements Control.Command {

      private final EntityPanel detailPanel;

      private SelectDetailPanelCommand(EntityPanel detailPanel) {
        this.detailPanel = detailPanel;
      }

      @Override
      public void perform() {
        setDetailPanelState(EMBEDDED);
        detailPanel.activatePanel();
      }
    }
  }

  static final class DefaultBuilder implements Builder {

    private PanelState detailPanelState = EMBEDDED;
    private double splitPaneResizeWeight = DEFAULT_SPLIT_PANE_RESIZE_WEIGHT;
    private boolean includeDetailTabPane = true;
    private boolean includeDetailPanelControls = INCLUDE_DETAIL_PANEL_CONTROLS.get();

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
    public Builder includeDetailPanelControls(boolean includeDetailPanelControls) {
      this.includeDetailPanelControls = includeDetailPanelControls;
      return this;
    }

    @Override
    public TabbedPanelLayout build() {
      return new DefaultTabPanelLayout(this);
    }
  }
}
