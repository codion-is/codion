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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.common.logging.MethodTrace.methodTrace;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

final class DefaultMethodTracer implements MethodTracer {

	private final Deque<MethodTrace> callStack = new LinkedList<>();
	private final LinkedList<MethodTrace> entries = new LinkedList<>();
	private final ArgumentFormatter formatter = new ArgumentFormatter();
	private final int maxSize;

	private @Nullable Consumer<MethodTrace> onTrace;

	DefaultMethodTracer(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public synchronized void enter(String method) {
		callStack.push(methodTrace(method, null));
	}

	@Override
	public synchronized void enter(String method, @Nullable Object argument) {
		callStack.push(methodTrace(method, formatter.format(method, argument)));
	}

	@Override
	public synchronized void enter(String method, @Nullable Object... arguments) {
		callStack.push(methodTrace(method, formatter.format(method, arguments)));
	}

	@Override
	public MethodTrace exit(String method) {
		return exit(method, null);
	}

	@Override
	public MethodTrace exit(String method, @Nullable Exception exception) {
		return exit(method, exception, null);
	}

	@Override
	public synchronized MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage) {
		if (callStack.isEmpty()) {
			throw new IllegalStateException("Call stack is empty when trying to log method exit: " + method);
		}
		MethodTrace entry = callStack.pop();
		if (!entry.method().equals(method)) {
			throw new IllegalStateException("Expecting method " + entry.method() + " but got " + method + " when trying to log method exit");
		}
		entry.complete(exception, exitMessage);
		if (callStack.isEmpty()) {
			if (entries.size() == maxSize) {
				entries.removeFirst();
			}
			entries.addLast(entry);
			if (onTrace != null) {
				onTrace.accept(entry);
			}
		}
		else {
			callStack.peek().addChild(entry);
		}

		return entry;
	}

	@Override
	public synchronized void onTrace(Consumer<MethodTrace> consumer) {
		this.onTrace = consumer;
	}

	@Override
	public synchronized List<MethodTrace> entries() {
		return unmodifiableList(new ArrayList<>(entries));
	}

	private static final class ArgumentFormatter {

		private static final String BRACKET_OPEN = "[";
		private static final String BRACKET_CLOSE = "]";

		private String format(String methodName, @Nullable Object argument) {
			if ("prepareStatement".equals(methodName)) {
				return (String) argument;
			}

			return format(argument);
		}

		private String format(@Nullable Object argument) {
			if (argument == null) {
				return "null";
			}
			if (argument instanceof String) {
				return "'" + argument + "'";
			}
			if (argument instanceof Entity) {
				return entityToString((Entity) argument);
			}
			else if (argument instanceof Entity.Key) {
				return entityKeyToString((Entity.Key) argument);
			}
			if (argument instanceof List) {
				return format((List<?>) argument);
			}
			if (argument instanceof Collection) {
				return format((Collection<?>) argument);
			}
			if (argument instanceof byte[]) {
				return "byte[" + ((byte[]) argument).length + "]";
			}
			if (argument.getClass().isArray()) {
				return format((Object[]) argument);
			}

			return argument.toString();
		}

		private String format(List<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}
			if (arguments.size() == 1) {
				return format(arguments.get(0));
			}

			return arguments.stream()
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String format(Collection<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}

			return arguments.stream()
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String format(Object[] arguments) {
			if (arguments.length == 0) {
				return "";
			}
			if (arguments.length == 1) {
				return format(arguments[0]);
			}

			return stream(arguments)
							.map(this::format)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private static String entityToString(Entity entity) {
			StringBuilder builder = new StringBuilder(entity.type().name()).append(" {");
			for (ColumnDefinition<?> columnDefinition : entity.definition().columns().definitions()) {
				boolean modified = entity.modified(columnDefinition.attribute());
				if (columnDefinition.primaryKey() || modified) {
					StringBuilder valueString = new StringBuilder();
					if (modified) {
						valueString.append(entity.original(columnDefinition.attribute())).append("->");
					}
					valueString.append(entity.string(columnDefinition.attribute()));
					builder.append(columnDefinition.attribute()).append(":").append(valueString).append(",");
				}
			}
			builder.deleteCharAt(builder.length() - 1);

			return builder.append("}").toString();
		}

		private static String entityKeyToString(Entity.Key key) {
			return key.type() + " {" + key + "}";
		}
	}
}
