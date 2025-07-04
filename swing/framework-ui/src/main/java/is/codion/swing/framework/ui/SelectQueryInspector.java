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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Conjunction;
import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityQueryModel;
import is.codion.swing.common.ui.component.Components;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.combination;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.invokeLater;

final class SelectQueryInspector extends JPanel {

	private final JTextArea textArea = Components.textArea()
					.rowsColumns(30, 42)
					.editable(false)
					.onBuild(SelectQueryInspector::setMonospaceFont)
					.build();
	private final EntityQueries queries;
	private final EntityQueryModel queryModel;

	SelectQueryInspector(EntityQueryModel queryModel) {
		requireNonNull(queryModel);
		this.queries = EntityQueries.instance()
						.orElseThrow(() -> new IllegalStateException("No EntityQueries instance available"))
						.create(Database.instance(), queryModel.connectionProvider().connection().entities());
		this.queryModel = queryModel;
		this.queryModel.condition().changed().addListener(this::refreshQuery);
		this.queryModel.limit().addListener(this::refreshQuery);
		this.queryModel.orderBy().addListener(this::refreshQuery);
		this.queryModel.attributes().included().addListener(this::refreshQuery);
		initializeUI();
		refreshQuery();
	}

	private void refreshQuery() {
		invokeLater(() -> textArea.setText(createSelectQuery()));
	}

	private String createSelectQuery() {
		Condition where = createCondition(queryModel.condition().where(Conjunction.AND), queryModel.where());
		Condition having = createCondition(queryModel.condition().having(Conjunction.AND), queryModel.having());
		EntityConnection.Select select = EntityConnection.Select.where(where)
						.having(having)
						.attributes(queryModel.attributes().get())
						.limit(queryModel.limit().get())
						.orderBy(queryModel.orderBy().get())
						.build();

		return BasicFormatterImpl.format(queries.select(select));
	}

	private void initializeUI() {
		setLayout(new BorderLayout());
		add(scrollPane(textArea).build());
	}

	private static Condition createCondition(Condition entityCondition, EntityQueryModel.AdditionalCondition additional) {
		return additional.optional()
						.map(Supplier::get)
						.map(condition -> combination(additional.conjunction().getOrThrow(), entityCondition, condition))
						.map(Condition.class::cast)
						.orElse(entityCondition);
	}

	private static void setMonospaceFont(JTextArea textArea) {
		Font font = textArea.getFont();
		textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
	}

	/**
	 * <p>Adapted from <a href="https://github.com/hibernate/hibernate-orm/blob/main/hibernate-core/src/main/java/org/hibernate/engine/jdbc/internal/BasicFormatterImpl.java">BasicFormatterImpl.java</a> (Apache 2.0)
	 * with minor changes, mostly formatting.</p>
	 * Performs formatting of basic SQL statements (DML + query).
	 * @author Gavin King
	 * @author Steve Ebersole
	 */
	static final class BasicFormatterImpl {

		private static final Set<String> NON_FUNCTION_NAMES = new HashSet<>(
						asList("select", "from", "on", "set", "and", "or", "where", "having", "by", "using")
		);

		private static final String INDENT_STRING = "    ";
		private static final String WHITESPACE = " \n\r\f\t";

		private BasicFormatterImpl() {}

		public static String format(String source) {
			return new FormatProcess(source).perform();
		}

		private static class FormatProcess {
			boolean beginLine = true;
			boolean afterBeginBeforeEnd;
			boolean afterByOrSetOrFromOrSelect;
			int afterOn;
			boolean afterBetween;
			boolean afterExtract;
			boolean afterInsert;
			int inFunction;
			int parensSinceSelect;
			int valuesParenCount;
			private final LinkedList<Integer> parenCounts = new LinkedList<>();
			private final LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<>();

			// Track INSERT formatting
			boolean inInsertColumns;
			boolean inValues;
			int commaCountInInsert;

			int indent = 0;

			StringBuilder result = new StringBuilder();
			StringTokenizer tokens;
			String lastToken;
			String token;
			String lcToken;

			private static final String ESCAPED_QUOTE_PLACEHOLDER = "\u0001ESQUOTE\u0001";

			public FormatProcess(String sql) {
				assert sql != null : "SQL to format should not be null";

				// Temporarily replace escaped single quotes to avoid tokenization issues
				sql = sql.replace("''", ESCAPED_QUOTE_PLACEHOLDER);

				tokens = new StringTokenizer(
								sql,
								"()+*/-=<>'`\"[]," + WHITESPACE,
								true
				);
			}

			public String perform() {
				while (tokens.hasMoreTokens()) {
					token = tokens.nextToken();
					if ("-".equals(token) && result.toString().endsWith("-")) {
						do {
							result.append(token);
							token = tokens.nextToken();
						}
						while (!"\n".equals(token) && tokens.hasMoreTokens());
					}
					lcToken = token.toLowerCase(Locale.ROOT);
					switch (lcToken) {
						case "'":
						case "`":
						case "\"":
							String t;
							do {
								t = tokens.nextToken();
								token += t;
							}
							while (!lcToken.equals(t) && tokens.hasMoreTokens());
							lcToken = token;
							misc();
							break;
						// SQL Server uses "[" and "]" to escape reserved words
						// see SQLServerDialect.openQuote and SQLServerDialect.closeQuote
						case "[":
							String tt;
							do {
								tt = tokens.nextToken();
								token += tt;
							}
							while (!"]".equals(tt) && tokens.hasMoreTokens());
							lcToken = token;
							misc();
							break;
						case ",":
							comma();
							break;
						case "(":
							openParen();
							break;
						case ")":
							closeParen();
							break;
						case "select":
							select();
							break;
						case "merge":
						case "insert":
						case "update":
						case "delete":
							updateOrInsertOrDelete();
							break;
						case "values":
							values();
							break;
						case "on":
							on();
							break;
						case "between":
							afterBetween = true;
							misc();
							break;
						case "trim":
						case "extract":
							afterExtract = true;
							misc();
							break;
						//TODO: detect when 'left', 'right' are function names
						case "left":
						case "right":
						case "full":
						case "inner":
						case "outer":
						case "cross":
						case "group":
						case "order":
						case "returning":
						case "using":
							beginNewClause();
							break;
						case "from":
							from();
							break;
						case "where":
						case "set":
						case "having":
						case "by":
						case "join":
						case "into":
						case "union":
						case "intersect":
						case "offset":
						case "limit":
						case "fetch":
							endNewClause();
							break;
						case "case":
							beginCase();
							break;
						case "end":
							endCase();
							break;
						case "when":
						case "else":
							when();
							break;
						case "then":
							then();
							break;
						case "and":
							and();
							break;
						case "or":
							or();
							break;
						default:
							if (isWhitespace(token)) {
								white();
							}
							else {
								misc();
							}
					}
					if (!isWhitespace(token)) {
						lastToken = lcToken;
					}
				}

				return result.toString();
			}

			private void or() {
				logical();
			}

			private void and() {
				if (afterBetween) {
					misc();
					afterBetween = false;
				}
				else {
					logical();
				}
			}

			private void from() {
				if (afterExtract) {
					misc();
					afterExtract = false;
				}
				else {
					endNewClause();
				}
			}

			private void comma() {
				if (afterInsert && (inInsertColumns || inValues)) {
					// Handle INSERT column lists and VALUES - break every 5 items
					out();
					commaCountInInsert++;
					if (commaCountInInsert % 5 == 0) {
						result.append("\n");
						// Add appropriate indentation
						for (int i = 0; i < indent; i++) {
							result.append("\t");
						}
					}
				}
				else if (afterByOrSetOrFromOrSelect && inFunction == 0) {
					commaAfterByOrFromOrSelect();
				}
				//			else if ( afterOn && inFunction==0 ) {
				//				commaAfterOn();
				//			}
				else {
					misc();
				}
			}

			private void then() {
				incrementIndent();
				newline();
				misc();
			}

			private void when() {
				decrementIndent();
				newline();
				out();
				beginLine = false;
				afterBeginBeforeEnd = true;
			}

			//		private void commaAfterOn() {
			//			out();
			//			decrementIndent();
			//			newline();
			//			afterOn = false;
			//			afterByOrSetOrFromOrSelect = true;
			//		}

			private void commaAfterByOrFromOrSelect() {
				out();
				newline();
			}

			private void logical() {
				newline();
				out();
				beginLine = false;
			}

			private void endCase() {
				afterBeginBeforeEnd = false;
				decrementIndent();
				decrementIndent();
				logical();
			}

			private void on() {
				if (afterOn == 0) {
					incrementIndent();
				}
				else if (afterOn == 1) {
					// ad hoc, but gives a nice result
					decrementIndent();
				}
				afterOn++;
				newline();
				out();
				beginLine = false;
			}

			private void beginCase() {
				out();
				beginLine = false;
				incrementIndent();
				incrementIndent();
				afterBeginBeforeEnd = true;
			}

			private void misc() {
				out();
				if (afterInsert && inFunction == 0 && !inInsertColumns && !inValues) {
					// Only add newline after INSERT if we're not in column list or values
					newline();
					// Don't reset afterInsert here - let closeParen handle it
				}
				else {
					beginLine = false;
				}
			}

			private void white() {
				if (!beginLine) {
					result.append(" ");
				}
			}

			private void updateOrInsertOrDelete() {
				if (indent > 1) {
					//probably just the insert SQL function
					out();
				}
				else {
					out();
					incrementIndent();
					beginLine = false;
					if ("update".equals(lcToken)) {
						newline();
					}
					if ("insert".equals(lcToken)) {
						afterInsert = true;
						inInsertColumns = false;
						inValues = false;
						commaCountInInsert = 0;
					}
				}
			}

			private void select() {
				out();
				incrementIndent();
				newline();
				parenCounts.addLast(parensSinceSelect);
				afterByOrFromOrSelects.addLast(afterByOrSetOrFromOrSelect);
				parensSinceSelect = 0;
				afterByOrSetOrFromOrSelect = true;
			}

			private void out() {
				if (!result.isEmpty() && result.charAt(result.length() - 1) == ',') {
					result.append(" ");
				}
				// Restore escaped quotes in the token before appending
				result.append(token.replace(ESCAPED_QUOTE_PLACEHOLDER, "''"));
			}

			private void endNewClause() {
				if (!afterBeginBeforeEnd) {
					decrementIndent();
					if (afterOn == 1) {
						decrementIndent();
					}
					if (afterOn > 0) {
						afterOn = 0;
					}
					newline();
				}
				out();
				if (!"union".equals(lcToken) && !"intersect".equals(lcToken)) {
					incrementIndent();
				}
				newline();
				afterBeginBeforeEnd = false;
				afterByOrSetOrFromOrSelect = "by".equals(lcToken)
								|| "set".equals(lcToken)
								|| "from".equals(lcToken);
			}

			private void beginNewClause() {
				if (!afterBeginBeforeEnd) {
					if (afterOn == 1) {
						decrementIndent();
					}
					if (afterOn > 0) {
						afterOn = 0;
					}
					decrementIndent();
					newline();
				}
				out();
				beginLine = false;
				afterBeginBeforeEnd = true;
			}

			private void values() {
				if (parensSinceSelect == 0) {
					if (!afterBeginBeforeEnd) {
						decrementIndent();
					}
					newline();
					out();
					incrementIndent();
					newline();
					valuesParenCount = parensSinceSelect + 1;
					// Mark that we're entering VALUES section
					if (afterInsert) {
						inValues = true;
						commaCountInInsert = 0;
					}
				}
				else {
					out();
				}
			}

			private void closeParen() {
				// Check if we're closing the INSERT column list or VALUES list
				if (inInsertColumns) {
					inInsertColumns = false;
				}
				else if (inValues) {
					inValues = false;
					// If we're closing the VALUES parentheses, we're done with INSERT
					afterInsert = false;
				}

				if (parensSinceSelect == 0) {
					decrementIndent();
					parensSinceSelect = parenCounts.removeLast();
					afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
				}
				else if (valuesParenCount == parensSinceSelect) {
					valuesParenCount = 0;
					if (afterBeginBeforeEnd) {
						decrementIndent();
					}
				}
				parensSinceSelect--;
				if (inFunction > 0) {
					// this should come first,
					// because we increment
					// inFunction for every
					// opening paren from the
					// first one after the
					// function name
					inFunction--;
					out();
				}
				else if (afterOn > 0) {
					out();
				}
				else {
					if (!afterByOrSetOrFromOrSelect) {
						decrementIndent();
						newline();
					}
					out();
				}
				beginLine = false;
			}

			private void openParen() {
				if (isFunctionName(lastToken) || inFunction > 0) {
					inFunction++;
				}
				beginLine = false;

				// Check if this is the opening paren for INSERT column list or VALUES
				if (afterInsert) {
					if (!inInsertColumns && !inValues) {
						// First parenthesis after INSERT - column list
						inInsertColumns = true;
						commaCountInInsert = 0;
					}
					else if (inValues) {
						// Parenthesis after VALUES
						commaCountInInsert = 0;
					}
				}

				if (afterOn > 0 || inFunction > 0) {
					out();
				}
				else {
					out();
					if (!afterByOrSetOrFromOrSelect) {
						incrementIndent();
						newline();
						beginLine = true;
					}
				}
				parensSinceSelect++;
			}

			private void incrementIndent() {
				indent++;
			}

			private void decrementIndent() {
				if (indent > 0) {
					indent--;
				}
			}

			private static boolean isFunctionName(String tok) {
				if (tok == null || tok.isEmpty()) {
					return false;
				}

				char begin = tok.charAt(0);
				boolean isIdentifier = Character.isJavaIdentifierStart(begin) || '"' == begin;

				return isIdentifier && !NON_FUNCTION_NAMES.contains(tok);
			}

			private static boolean isWhitespace(String token) {
				return WHITESPACE.contains(token);
			}

			private void newline() {
				result.append(lineSeparator())
								.append(INDENT_STRING.repeat(indent));
				beginLine = true;
			}
		}
	}
}
