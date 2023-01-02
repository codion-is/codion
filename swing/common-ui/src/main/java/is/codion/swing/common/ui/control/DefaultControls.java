/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultControls extends AbstractControl implements Controls {

  private static final String CONTROLS_PARAMETER = "controls";

  private final List<Action> actions = new ArrayList<>();

  DefaultControls(String name, char mnemonic, StateObserver enabledState,
                  Icon smallIcon, List<Action> controls) {
    super(name, enabledState, smallIcon);
    setMnemonic(mnemonic);
    for (Action control : controls) {
      if (control != null) {
        add(control);
      }
      else {
        addSeparator();
      }
    }
  }

  @Override
  public List<Action> actions() {
    return unmodifiableList(actions);
  }

  @Override
  public Controls add(Action action) {
    actions.add(requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls addAt(int index, Action action) {
    actions.add(index, requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls remove(Action action) {
    if (action != null) {
      actions.remove(action);
    }
    return this;
  }

  @Override
  public Controls removeAll() {
    actions.clear();
    return this;
  }

  @Override
  public int size() {
    return actions.size();
  }

  @Override
  public boolean isEmpty() {
    return actions.isEmpty();
  }

  @Override
  public Action get(int index) {
    return actions.get(index);
  }

  @Override
  public Controls add(Controls controls) {
    actions.add(requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addAt(int index, Controls controls) {
    actions.add(index, requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addSeparator() {
    actions.add(null);
    return this;
  }

  @Override
  public Controls addSeparatorAt(int index) {
    actions.add(index, null);
    return this;
  }

  @Override
  public Controls addAll(Controls controls) {
    actions.addAll(requireNonNull(controls, CONTROLS_PARAMETER).actions());
    return this;
  }

  @Override
  public JPanel createVerticalButtonPanel() {
    JPanel panel = addEmptyBorder(new JPanel(Layouts.gridLayout(0, 1)));
    new ButtonControlHandler(panel, this, true);

    return panel;
  }

  @Override
  public JPanel createHorizontalButtonPanel() {
    JPanel panel = addEmptyBorder(new JPanel(Layouts.gridLayout(1, 0)));
    new ButtonControlHandler(panel, this, false);

    return panel;
  }

  @Override
  public JToolBar createVerticalToolBar() {
    return createToolBar(SwingConstants.VERTICAL);
  }

  @Override
  public JToolBar createHorizontalToolBar() {
    return createToolBar(SwingConstants.HORIZONTAL);
  }

  @Override
  public JPopupMenu createPopupMenu() {
    return createMenu().getPopupMenu();
  }

  @Override
  public JMenu createMenu() {
    JMenu menu = new JMenu(this);
    new MenuControlHandler(menu, this);

    return menu;
  }

  @Override
  public JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    actions.stream()
            .filter(Controls.class::isInstance)
            .map(Controls.class::cast)
            .forEach(subControls -> menuBar.add(subControls.createMenu()));

    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent e) {/*Not required*/}

  private JToolBar createToolBar(int orientation) {
    JToolBar toolBar = new JToolBar(orientation);
    actions.forEach(new ToolBarControlHandler(toolBar));

    return toolBar;
  }

  private static JPanel addEmptyBorder(JPanel panel) {
    Integer gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    if (gap != null) {
      panel.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
    }

    return panel;
  }
}
