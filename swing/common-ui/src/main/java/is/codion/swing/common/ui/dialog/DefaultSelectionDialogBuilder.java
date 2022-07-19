/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Windows;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultSelectionDialogBuilder<T> extends AbstractDialogBuilder<SelectionDialogBuilder<T>>
        implements SelectionDialogBuilder<T> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultSelectionDialogBuilder.class.getName());

  private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;

  private final Collection<T> values;
  private boolean singleSelection;
  private Collection<T> defaultSelection = Collections.emptyList();

  DefaultSelectionDialogBuilder(Collection<T> values) {
    if (requireNonNull(values).isEmpty()) {
      throw new IllegalArgumentException("No values to select from");
    }
    this.values = values;
  }

  @Override
  public SelectionDialogBuilder<T> singleSelection(boolean singleSelection) {
    this.singleSelection = singleSelection;
    return this;
  }

  @Override
  public SelectionDialogBuilder<T> defaultSelection(T defaultSelection) {
    return defaultSelection(Collections.singletonList(defaultSelection));
  }

  @Override
  public SelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection) {
    this.defaultSelection = requireNonNull(defaultSelection);
    return this;
  }

  @Override
  public Optional<T> selectSingle() {
    return selectValue(owner, values, titleProvider == null ? MESSAGES.getString("select_value") : titleProvider.get(),
            defaultSelection.isEmpty() ? null : defaultSelection.iterator().next());
  }

  @Override
  public Collection<T> select() {
    return selectValues(owner, values, titleProvider == null ? MESSAGES.getString("select_values") : titleProvider.get(), singleSelection, defaultSelection);
  }

  static <T> Optional<T> selectValue(Window dialogOwner, Collection<T> values, String dialogTitle,
                                     T defaultSelection) {
    List<T> selected = selectValues(dialogOwner, values, dialogTitle, true,
            defaultSelection == null ? emptyList() : singletonList(defaultSelection));
    if (selected.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(selected.get(0));
  }

  static <T> List<T> selectValues(Window dialogOwner, Collection<T> values,
                                  String dialogTitle, boolean singleSelection,
                                  Collection<T> defaultSelection) {
    DefaultListModel<T> listModel = new DefaultListModel<>();
    values.forEach(listModel::addElement);
    JList<T> list = new JList<>(listModel);
    DisposeDialogAction okAction = new DisposeDialogAction(() -> Windows.getParentDialog(list).orElse(null), null);
    if (singleSelection) {
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          okAction.actionPerformed(null);
        }
      }
    });
    JDialog dialog = new DefaultOkCancelDialogBuilder(new JScrollPane(list))
            .owner(dialogOwner)
            .title(dialogTitle)
            .okAction(okAction)
            .onCancel(list::clearSelection)
            .build();
    if (dialog.getSize().width > MAX_SELECT_VALUE_DIALOG_WIDTH) {
      dialog.setSize(new Dimension(MAX_SELECT_VALUE_DIALOG_WIDTH, dialog.getSize().height));
    }
    if (defaultSelection != null) {
      defaultSelection.forEach(item -> {
        int index = listModel.indexOf(item);
        list.getSelectionModel().addSelectionInterval(index, index);
        list.ensureIndexIsVisible(index);
      });
    }
    dialog.setVisible(true);

    return list.getSelectedValuesList();
  }
}
