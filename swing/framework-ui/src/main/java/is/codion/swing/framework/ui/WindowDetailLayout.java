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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel.DetailLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.JComponent;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.WindowDetailLayout.WindowType.DIALOG;
import static is.codion.swing.framework.ui.WindowDetailLayout.WindowType.FRAME;
import static java.util.Objects.requireNonNull;

/**
 * A detail layout which displays detail panels in a window, opened via the table popup menu.
 */
public final class WindowDetailLayout implements DetailLayout {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TabbedDetailLayout.class.getName());

  private static final String DETAIL_TABLES = "detail_tables";

  private static final Value.Validator<PanelState> PANEL_STATE_VALIDATOR = new RejectEmbedded();

  /**
   * Specifies the window type.
   */
  public enum WindowType {

    /**
     * Display detail panels in a JFrame
     */
    FRAME,

    /**
     * Display detail panels in a JDialog
     */
    DIALOG
  }

  private final Map<EntityPanel, DetailWindow> panelWindows = new HashMap<>();
  private final WindowType windowType;

  private EntityPanel entityPanel;

  private WindowDetailLayout(DefaultBuilder builder) {
    this.windowType = builder.windowType;
  }

  @Override
  public JComponent layout(EntityPanel entityPanel) {
    this.entityPanel = requireNonNull(entityPanel);
    if (!entityPanel.detailPanels().isEmpty()) {
      entityPanel.detailPanels().forEach(detailPanel ->
              panelWindows.put(detailPanel, new DetailWindow(detailPanel)));
      setupControls(entityPanel);
    }

    return entityPanel.editControlTablePanel();
  }

  @Override
  public Value<PanelState> panelState(EntityPanel detailPanel) {
    return detailWindow(detailPanel).panelState;
  }

  @Override
  public void select(EntityPanel entityPanel) {
    Window panelWindow = detailWindow(entityPanel).window;
    if (panelWindow.isShowing()) {
      panelWindow.toFront();
    }
  }

  /**
   * @return a new {@link WindowDetailLayout} instance based on {@link WindowType#DIALOG}.
   */
  public static WindowDetailLayout windowDetailLayout() {
    return windowDetailLayout(DIALOG);
  }

  /**
   * @param windowType the window type
   * @return a new {@link WindowDetailLayout} instance based on the given window type.
   */
  public static WindowDetailLayout windowDetailLayout(WindowType windowType) {
    return builder().windowType(windowType).build();
  }

  /**
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  private void setupControls(EntityPanel entityPanel) {
    if (entityPanel.containsTablePanel()) {
      Controls.Builder controls = Controls.builder()
              .name(MESSAGES.getString(DETAIL_TABLES))
              .smallIcon(FrameworkIcons.instance().detail());
      entityPanel.detailPanels().forEach(detailPanel ->
              controls.control(Control.builder(() -> panelWindows.get(detailPanel).panelState.set(WINDOW))
                      .name(detailPanel.caption().get())
                      .build()));
      entityPanel.tablePanel().addPopupMenuControls(controls.build());
    }
  }

  private DetailWindow detailWindow(EntityPanel detailPanel) {
    DetailWindow detailWindow = panelWindows.get(requireNonNull(detailPanel));
    if (detailWindow == null) {
      throw new IllegalArgumentException("Detail panel not found: " + detailPanel);
    }

    return detailWindow;
  }

  /**
   * Builds a {@link WindowDetailLayout} instance.
   */
  public interface Builder {

    /**
     * @param windowType specifies whether a JFrame or a JDialog should be used
     * @return this Builder instance
     */
    Builder windowType(WindowType windowType);

    /**
     * @return a new {@link WindowDetailLayout} instance
     */
    WindowDetailLayout build();
  }

  private final class DetailWindow {

    private final Value<PanelState> panelState = Value.value(HIDDEN, HIDDEN, Notify.WHEN_SET);
    private final EntityPanel detailPanel;

    private Window window;
    private boolean packed = false;

    private DetailWindow(EntityPanel detailPanel) {
      this.detailPanel = detailPanel;
      panelState.addValidator(PANEL_STATE_VALIDATOR);
      panelState.addDataListener(this::updateDetailState);
    }

    private void updateDetailState(PanelState panelState) {
      if (window == null) {
        window = createDetailWindow();
      }
      if (panelState == WINDOW) {
        detailPanel.initialize();
        if (!packed) {
          window.pack();
          packed = true;
        }
        window.setVisible(true);
        window.toFront();
      }
      else {
        window.setVisible(false);
      }
      SwingEntityModel model = detailPanel.model();
      if (entityPanel.model().containsDetailModel(model)) {
        entityPanel.model().detailModelLink(model).active().set(panelState == WINDOW);
      }
    }

    private Window createDetailWindow() {
      if (windowType == FRAME) {
        return Windows.frame(detailPanel)
                .locationRelativeTo(entityPanel)
                .title(detailPanel.caption().get())
                .onClosing(windowEvent -> panelWindows.get(detailPanel).panelState.set(HIDDEN))
                .build();
      }

      return Dialogs.componentDialog(detailPanel)
              .owner(entityPanel)
              .locationRelativeTo(entityPanel)
              .title(detailPanel.caption().get())
              .modal(false)
              .onClosed(windowEvent -> panelWindows.get(detailPanel).panelState.set(HIDDEN))
              .build();
    }
  }

  private static final class DefaultBuilder implements Builder {

    private WindowType windowType = EntityPanel.USE_FRAME_PANEL_DISPLAY.get() ? FRAME : DIALOG;

    @Override
    public Builder windowType(WindowType windowType) {
      this.windowType = windowType;
      return this;
    }

    @Override
    public WindowDetailLayout build() {
      return new WindowDetailLayout(this);
    }
  }

  private static final class RejectEmbedded implements Value.Validator<PanelState> {
    @Override
    public void validate(PanelState panelState) {
      if (panelState == EMBEDDED) {
        throw new IllegalArgumentException("WindowedDetailLayout does not support the EMBEDDED PanelState");
      }
    }
  }
}
