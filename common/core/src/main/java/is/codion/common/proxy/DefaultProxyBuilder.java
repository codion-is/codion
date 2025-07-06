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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.proxy;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultProxyBuilder<T> implements ProxyBuilder<T> {

	private final Map<MethodKey, ProxyMethod<T>> methodMap = new HashMap<>();
	private final Class<T> interfaceToProxy;

	private @Nullable T delegate;

	DefaultProxyBuilder(Class<T> interfaceToProxy) {
		if (!requireNonNull(interfaceToProxy).isInterface()) {
			throw new IllegalArgumentException(interfaceToProxy + " is not an interface");
		}
		this.interfaceToProxy = interfaceToProxy;
	}

	@Override
	public ProxyBuilder<T> delegate(T delegate) {
		requireNonNull(delegate);
		synchronized (methodMap) {
			this.delegate = delegate;
		}
		return this;
	}

	@Override
	public ProxyBuilder<T> method(String name, ProxyMethod<T> method) {
		return method(name, emptyList(), method);
	}

	@Override
	public ProxyBuilder<T> method(String name, Class<?> parameterType, ProxyMethod<T> method) {
		return method(name, singletonList(requireNonNull(parameterType)), method);
	}

	@Override
	public ProxyBuilder<T> method(String name, List<Class<?>> parameterTypes, ProxyMethod<T> method) {
		requireNonNull(name);
		requireNonNull(parameterTypes);
		requireNonNull(method);
		try {
			synchronized (methodMap) {
				methodMap.put(findMethod(singletonList(interfaceToProxy), name, parameterTypes), method);
			}

			return this;
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T build() {
		synchronized (methodMap) {
			return (T) Proxy.newProxyInstance(interfaceToProxy.getClassLoader(),
							new Class[] {interfaceToProxy}, new DefaultHandler<>(new HashMap<>(methodMap), delegate));
		}
	}

	private static MethodKey findMethod(List<Class<?>> interfaces, String methodName, List<Class<?>> parameterTypes) throws NoSuchMethodException {
		Method method = null;
		for (Class<?> anInterface : interfaces) {
			try {
				method = anInterface.getMethod(methodName, parameterTypes.toArray(new Class[0]));
			}
			catch (NoSuchMethodException e) {/**/}
		}
		if (method != null) {
			return new MethodKey(method);
		}

		List<Class<?>> superInterfaces = interfaces.stream()
						.flatMap(new GetSuperInterfaces())
						.collect(Collectors.toList());
		if (superInterfaces.isEmpty()) {
			return new MethodKey(Object.class.getMethod(methodName, parameterTypes.toArray(new Class[0])));
		}

		return findMethod(superInterfaces, methodName, parameterTypes);
	}

	private static final class GetSuperInterfaces implements Function<Class<?>, Stream<Class<?>>> {

		@Override
		public Stream<Class<?>> apply(Class<?> anInterface) {
			return Arrays.stream(anInterface.getInterfaces());
		}
	}

	private static final class DefaultHandler<T> implements InvocationHandler {

		private static final String EQUALS = "equals";

		private final Map<MethodKey, ProxyMethod<T>> methodMap;
		private final @Nullable T delegate;

		private DefaultHandler(Map<MethodKey, ProxyMethod<T>> methodMap, @Nullable T delegate) {
			this.methodMap = methodMap;
			this.delegate = delegate;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
			ProxyMethod<T> proxyMethod = methodMap.get(new MethodKey(method));
			if (proxyMethod != null) {
				return proxyMethod.invoke(new DefaultProxyMethodParameters<>((T) proxy, delegate, args));
			}
			if (isEqualsMethod(method)) {
				return proxy == args[0];
			}
			if (delegate != null) {
				try {
					return method.invoke(delegate, args);
				}
				catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}

			throw new UnsupportedOperationException(method.toString());
		}

		private static boolean isEqualsMethod(Method method) {
			return EQUALS.equals(method.getName()) &&
							method.getParameterCount() == 1 &&
							method.getParameterTypes()[0].equals(Object.class);
		}
	}

	private static final class DefaultProxyMethodParameters<T> implements ProxyMethod.Parameters<T> {

		private final T proxy;
		private final @Nullable T delegate;
		private final List<?> arguments;

		private DefaultProxyMethodParameters(T proxy, @Nullable T delegate, @Nullable Object[] arguments) {
			this.proxy = requireNonNull(proxy);
			this.delegate = delegate;
			this.arguments = arguments == null ? emptyList() : unmodifiableList(Arrays.asList(arguments));
		}

		@Override
		public T proxy() {
			return proxy;
		}

		@Override
		public T delegate() {
			if (delegate == null) {
				throw new IllegalStateException("No delegate specified for proxy");
			}

			return delegate;
		}

		@Override
		public List<?> arguments() {
			return arguments;
		}
	}

	private static final class MethodKey {

		private final String methodName;
		private final Class<?>[] parameterTypes;
		private final int hashCode;

		private MethodKey(Method method) {
			this.methodName = method.getName();
			this.parameterTypes = method.getParameterTypes();
			this.hashCode = createHashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			MethodKey methodKey = (MethodKey) object;

			return methodName.equals(methodKey.methodName) &&
							Arrays.equals(parameterTypes, methodKey.parameterTypes);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		private int createHashCode() {
			return 31 * methodName.hashCode() + Arrays.hashCode(parameterTypes);
		}
	}
}
