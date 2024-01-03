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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JList;
import javax.swing.ListModel;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
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
            .filter(Optional::isPresent)
            .map(Optional::get)
            .mapToInt(Integer::intValue)
            .toArray());
  }

  private static <T> Optional<Integer> indexOf(JList<T> list, T element) {
    ListModel<T> model = list.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      if (Objects.equals(model.getElementAt(i), element)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }
}
