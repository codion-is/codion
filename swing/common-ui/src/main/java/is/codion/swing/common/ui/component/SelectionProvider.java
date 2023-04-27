/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import java.util.Optional;

/**
 * Provides the user with the ability to select a value.
 * @param <T> the value type
 */
public interface SelectionProvider<T> {

  /**
   * @param component the component in which value should be selected
   * @return the selected value, an empty Optional if nothing was selected
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  Optional<T> select(JComponent component);
}
