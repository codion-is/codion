/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

/**
 * Specifies the message and title for a user confirmation.
 * @see #confirmationMessage(String, String)
 */
public interface ConfirmationMessage {

  /**
   * @return the message to present
   */
  String message();

  /**
   * @return the title
   */
  String title();

  /**
   * Creates a new {@link ConfirmationMessage} instance
   * @param message the message
   * @param title the title
   * @return a new {@link ConfirmationMessage} instance
   */
  static ConfirmationMessage confirmationMessage(String message, String title) {
    return new DefaultConfirmationMessage(message, title);
  }
}
