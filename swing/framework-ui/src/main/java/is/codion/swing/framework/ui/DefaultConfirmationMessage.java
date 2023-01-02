/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import static java.util.Objects.requireNonNull;

final class DefaultConfirmationMessage implements ConfirmationMessage {

  private final String message;
  private final String title;

  DefaultConfirmationMessage(String message, String title) {
    this.message = requireNonNull(message);
    this.title = requireNonNull(title);
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public String title() {
    return title;
  }
}
