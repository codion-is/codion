/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  DefaultControls(final String name, final char mnemonic, final StateObserver enabledState,
                  final Icon smallIcon, final List<Action> controls) {
    super(name, enabledState, smallIcon);
    setMnemonic(mnemonic);
    for (final Action control : controls) {
      if (control != null) {
        add(control);
      }
      else {
        addSeparator();
      }
    }
  }

  @Override
  public List<Action> getActions() {
    return unmodifiableList(actions);
  }

  @Override
  public Controls add(final Action action) {
    actions.add(requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls addAt(final int index, final Action action) {
    actions.add(index, requireNonNull(action, "action"));
    return this;
  }

  @Override
  public Controls remove(final Action action) {
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
  public Action get(final int index) {
    return actions.get(index);
  }

  @Override
  public Controls add(final Controls controls) {
    actions.add(requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addAt(final int index, final Controls controls) {
    actions.add(index, requireNonNull(controls, CONTROLS_PARAMETER));
    return this;
  }

  @Override
  public Controls addSeparator() {
    actions.add(null);
    return this;
  }

  @Override
  public Controls addSeparatorAt(final int index) {
    actions.add(index, null);
    return this;
  }

  @Override
  public Controls addAll(final Controls controls) {
    actions.addAll(requireNonNull(controls, CONTROLS_PARAMETER).getActions());
    return this;
  }

  @Override
  public JPanel createVerticalButtonPanel() {
    final JPanel panel = addEmptyBorder(new JPanel(Layouts.gridLayout(0, 1)));
    new ButtonControlHandler(panel, this, true);

    return panel;
  }

  @Override
  public JPanel createHorizontalButtonPanel() {
    final JPanel panel = addEmptyBorder(new JPanel(Layouts.gridLayout(1, 0)));
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
    final JMenu menu = new JMenu(this);
    new MenuControlHandler(menu, this);

    return menu;
  }

  @Override
  public JMenuBar createMenuBar() {
    final JMenuBar menuBar = new JMenuBar();
    actions.stream()
            .filter(Controls.class::isInstance)
            .map(Controls.class::cast)
            .forEach(subControls -> menuBar.add(subControls.createMenu()));

    return menuBar;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}

  private JToolBar createToolBar(final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    actions.forEach(new ToolBarControlHandler(toolBar));

    return toolBar;
  }

  private static JPanel addEmptyBorder(final JPanel panel) {
    final Integer gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    if (gap != null) {
      panel.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
    }

    return panel;
  }
}
