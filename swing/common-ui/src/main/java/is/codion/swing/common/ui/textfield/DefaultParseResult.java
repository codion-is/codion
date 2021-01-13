/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

class DefaultParseResult<T> implements Parser.ParseResult<T> {

  private final String text;
  private final T value;
  private final int charetOffset;
  private final boolean successful;

  DefaultParseResult(final String text, final T value, final int charetOffset, final boolean successful) {
    this.text = text;
    this.value = value;
    this.charetOffset = charetOffset;
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
  public int getCharetOffset() {
    return charetOffset;
  }

  @Override
  public boolean successful() {
    return successful;
  }
}
