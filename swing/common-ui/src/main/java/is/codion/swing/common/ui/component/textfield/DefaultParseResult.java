/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

class DefaultParseResult<T> implements Parser.ParseResult<T> {

  private final String text;
  private final T value;
  private final boolean successful;

  DefaultParseResult(String text, T value, boolean successful) {
    this.text = text;
    this.value = value;
    this.successful = successful;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public boolean successful() {
    return successful;
  }
}
