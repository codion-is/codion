/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JList;
import javax.swing.ListModel;
import java.util.HashSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

final class ListValue<T> extends AbstractComponentValue<Set<T>, JList<T>> {

  ListValue(JList<T> list) {
    super(list);
    list.addListSelectionListener(e -> notifyListeners());
  }

  @Override
  protected Set<T> getComponentValue() {
    return new HashSet<>(component().getSelectedValuesList());
  }

  @Override
  protected void setComponentValue(Set<T> value) {
    selectValues(component(), value);
  }

  static <T> void selectValues(JList<T> list, Set<T> valueSet) {
    list.setSelectedIndices(valueSet.stream()
            .map(value -> indexOf(list, value))
            .filter(OptionalInt::isPresent)
            .mapToInt(OptionalInt::getAsInt)
            .toArray());
  }

  private static <T> OptionalInt indexOf(JList<T> list, T element) {
    ListModel<T> model = list.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      if (Objects.equals(model.getElementAt(i), element)) {
        return OptionalInt.of(i);
      }
    }

    return OptionalInt.empty();
  }
}
