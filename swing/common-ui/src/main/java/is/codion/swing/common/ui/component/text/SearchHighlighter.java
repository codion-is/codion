/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Util;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Objects.requireNonNull;

/**
 * Highlights search results in a JTextComponent.<br>
 * <br>
 * Instantiate via the {@link SearchHighlighter#searchHighlighter(JTextComponent)} factory method.
 */
public final class SearchHighlighter {

  private final Document document;
  private final Value<String> searchStringValue = Value.value();
  private final State caseSensitiveState = State.state();
  private final Highlighter highlighter = new DefaultHighlighter();
  private final List<MatchPosition> searchTextPositions = new ArrayList<>();
  private final Value<Integer> currentSearchTextPositionIndex = Value.value();
  private final Value<Integer> selectedSearchTextPosition = Value.value();

  private Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
  private Highlighter.HighlightPainter selectedHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);

  private SearchHighlighter(JTextComponent textComponent) {
    this.document = requireNonNull(textComponent).getDocument();
    textComponent.setHighlighter(highlighter);
    bindEvents(textComponent);
  }

  /**
   * @return the search string value
   */
  public Value<String> getSearchStringValue() {
    return searchStringValue;
  }

  /**
   * @return the state controlling whether the search is case-sensitive.
   */
  public State getCaseSensitiveState() {
    return caseSensitiveState;
  }

  /**
   * @param color the color to use when highlighting search results.
   */
  public void setHighlightColor(Color color) {
    highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(requireNonNull(color));
    searchAndHighlightResults();
  }

  /**
   * @param color the color to use when highlighting the selected search result.
   */
  public void setSelectedHighlightColor(Color color) {
    selectedHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(requireNonNull(color));
    searchAndHighlightResults();
  }

  /**
   * @return a text field for entering the search text.
   */
  public JTextField createSearchField() {
    JTextField searchField = new JTextField(12);
    new SearchFieldValue(searchField).link(searchStringValue);
    //todo make this work somehow
    //TextFieldHint.create(searchField, "Search");
    searchField.setComponentPopupMenu(Controls.builder()
            .control(ToggleControl.builder(caseSensitiveState)
                    .caption("Case-sensitive"))
            .build().createPopupMenu());
    KeyEvents.builder(KeyEvent.VK_DOWN)
            .action(control(this::nextSearchPosition))
            .enable(searchField);
    KeyEvents.builder(KeyEvent.VK_UP)
            .action(control(this::previousSearchPosition))
            .enable(searchField);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .action(control(() -> searchStringValue.set(null)))
            .enable(searchField);

    return searchField;
  }

  /**
   * Instantiates a new search highlighter for the given text component.
   * @param textComponent the text component to search
   * @return a new {@link SearchHighlighter} for the given component
   */
  public static SearchHighlighter searchHighlighter(JTextComponent textComponent) {
    return new SearchHighlighter(textComponent);
  }

  /**
   * Moves to the next search position, if available, with wrap-around.
   */
  void nextSearchPosition() {
    if (!searchTextPositions.isEmpty()) {
      deselectCurrentSearchPosition();
      if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.equalTo(searchTextPositions.size() - 1)) {
        currentSearchTextPositionIndex.set(0);
      }
      else {
        currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() + 1);
      }
      selectCurrentSearchPosition();
    }
  }

  /**
   * Moves to the previous search position, if available, with wrap-around.
   */
  void previousSearchPosition() {
    if (!searchTextPositions.isEmpty()) {
      deselectCurrentSearchPosition();
      if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.equalTo(0)) {
        currentSearchTextPositionIndex.set(searchTextPositions.size() - 1);
      }
      else {
        currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() - 1);
      }
      selectCurrentSearchPosition();
    }
  }

  /**
   * @return the index of the selected search position within the document, null if none is selected.
   */
  Integer getSelectedHighlightPosition() {
    return selectedSearchTextPosition.get();
  }

  private void searchAndHighlightResults() {
    currentSearchTextPositionIndex.set(null);
    selectedSearchTextPosition.set(null);
    highlighter.removeAllHighlights();
    searchTextPositions.clear();
    if (!Util.nullOrEmpty(searchStringValue.get())) {
      Pattern pattern = Pattern.compile(searchStringValue.get(), caseSensitiveState.get() ? 0 : Pattern.CASE_INSENSITIVE);
      try {
        Matcher matcher = pattern.matcher(document.getText(0, document.getLength()));
        int searchFrom = 0;
        while (matcher.find(searchFrom)) {
          Object highlightTag = highlighter.addHighlight(matcher.start(), matcher.end(), highlightPainter);
          searchTextPositions.add(new MatchPosition(matcher.start(), matcher.end(), highlightTag));
          searchFrom = matcher.end();
        }
        nextSearchPosition();
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void selectCurrentSearchPosition() {
    MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
    selectedSearchTextPosition.set(matchPosition.start);
    try {
      highlighter.removeHighlight(matchPosition.highlightTag);
      matchPosition.highlightTag = highlighter.addHighlight(matchPosition.start, matchPosition.end, selectedHighlightPainter);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private void deselectCurrentSearchPosition() {
    if (currentSearchTextPositionIndex.isNotNull()) {
      MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
      try {
        highlighter.removeHighlight(matchPosition.highlightTag);
        matchPosition.highlightTag = highlighter.addHighlight(matchPosition.start, matchPosition.end, highlightPainter);
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void bindEvents(JTextComponent textComponent) {
    searchStringValue.addListener(this::searchAndHighlightResults);
    caseSensitiveState.addListener(this::searchAndHighlightResults);
    document.addDocumentListener((DocumentAdapter) e -> searchAndHighlightResults());
    selectedSearchTextPosition.addDataListener(selectedSearchPosition -> {
      if (selectedSearchPosition != null) {
        try {
          textComponent.scrollRectToVisible(textComponent.modelToView(selectedSearchPosition));
        }
        catch (BadLocationException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private static final class SearchFieldValue extends AbstractValue<String> {

    private final JTextField searchField;

    private SearchFieldValue(JTextField searchField) {
      this.searchField = searchField;
      this.searchField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
    }

    @Override
    public String get() {
      return searchField.getText();
    }

    @Override
    protected void setValue(String value) {
      searchField.setText(value);
    }
  }

  private static final class MatchPosition {

    private final int start;
    private final int end;

    private Object highlightTag;

    private MatchPosition(int start, int end, Object highlightTag) {
      this.start = start;
      this.end = end;
      this.highlightTag = highlightTag;
    }
  }
}
