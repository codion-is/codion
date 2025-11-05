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
package is.codion.common.utilities.proxy;

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

/**
 * Builds a dynamic proxy for a single interface.
 * <p>
 * This builder is reusable and thread-safe. Each call to {@link #build()} creates a new proxy instance
 * based on the current builder state. Subsequent changes to the builder do not affect previously built proxies.
 * <p>
 * Methods can be replaced by calling {@link #method(String, ProxyBuilder.ProxyMethod)} again with the same method signature,
 * but once added, methods cannot be removed from the builder.
 * <p>
 * Note that if the {@link Object#equals(Object)} method is not proxied the resulting proxy is equal only to itself.
 * {@snippet :
 * List<String> list = new ArrayList<>();
 *
 * ProxyBuilder<List> builder = ProxyBuilder.of(List.class)
 *     .delegate(list)
 *     .method("add", Object.class, parameters -> {
 *       Object item = parameters.arguments().get(0);
 *       System.out.println("Adding: " + item);
 *
 *       return parameters.delegate().add(item);
 *     })
 *     .method("size", parameters -> {
 *       System.out.println("Size");
 *
 *       return parameters.delegate().size();
 *     });
 *
 * List<String> proxy1 = builder.build();
 *
 * // Builder can be reused and modified
 * builder.method("remove", Object.class, parameters -> {
 *   Object item = parameters.arguments().get(0);
 *   System.out.println("Removing: " + item);
 *
 *   return parameters.delegate().remove(item);
 * });
 *
 * List<String> proxy2 = builder.build(); // Has all three methods
 * // proxy1 still has only add() and size() methods proxied
 *}
 * @param <T> the proxy type
 * @see #of(Class)
 */
public final class ProxyBuilder<T> {

	private final Map<MethodKey, ProxyMethod<T>> methodMap = new HashMap<>();
	private final Class<T> interfaceToProxy;

	private @Nullable T delegate;

	private ProxyBuilder(Class<T> interfaceToProxy) {
		if (!requireNonNull(interfaceToProxy).isInterface()) {
			throw new IllegalArgumentException(interfaceToProxy + " is not an interface");
		}
		this.interfaceToProxy = interfaceToProxy;
	}

	/**
	 * Sets the delegate instance to forward non-proxied method calls to.
	 * If not specified, all non-proxied methods throw {@link UnsupportedOperationException}.
	 * <p>
	 * The delegate is captured at build time. Changing the delegate after building a proxy
	 * does not affect previously built proxies.
	 * @param delegate the delegate instance to receive all non-proxied method calls
	 * @return this proxy builder
	 */
	public ProxyBuilder<T> delegate(T delegate) {
		requireNonNull(delegate);
		synchronized (methodMap) {
			this.delegate = delegate;
		}
		return this;
	}


	/**
	 * Proxy the given no-argument method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param method the proxy replacement method
	 * @return this proxy builder
	 */
	public ProxyBuilder<T> method(String name, ProxyMethod<T> method) {
		return method(name, emptyList(), method);
	}

	/**
	 * Proxy the given method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param parameterType the method parameter type
	 * @param method the proxy method
	 * @return this proxy builder
	 */
	public ProxyBuilder<T> method(String name, Class<?> parameterType, ProxyMethod<T> method) {
		return method(name, singletonList(requireNonNull(parameterType)), method);
	}

	/**
	 * Proxy the given method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param parameterTypes the method parameter types
	 * @param method the proxy method
	 * @return this proxy builder
	 */
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

	/**
	 * Builds the Proxy instance.
	 * <p>
	 * Each call to this method returns a new proxy instance with the current builder configuration.
	 * The returned proxy is independent of the builder and will not be affected by subsequent
	 * changes to the builder.
	 * @return a new proxy instance
	 */
	public T build() {
		synchronized (methodMap) {
			return (T) Proxy.newProxyInstance(interfaceToProxy.getClassLoader(),
							new Class[] {interfaceToProxy}, new DefaultHandler<>(new HashMap<>(methodMap), delegate));
		}
	}

	/**
	 * A proxy method.
	 * @param <T> the proxy type
	 */
	public interface ProxyMethod<T> {

		/**
		 * Invokes this proxy method.
		 * @param parameters the parameters
		 * @return the result, may be null
		 * @throws Throwable in case of an exception
		 */
		@Nullable Object invoke(Parameters<T> parameters) throws Throwable;

		/**
		 * Parameters available to the invocation handler when calling a proxy method.
		 * @param <T> the proxy type
		 */
		sealed interface Parameters<T> {

			/**
			 * @return the proxy instance
			 */
			T proxy();

			/**
			 * @return the delegate instance
			 * @throws IllegalStateException in case no delagate is available
			 */
			T delegate();

			/**
			 * @return the method arguments or an empty list in case of no arguments
			 */
			List<?> arguments();
		}
	}

	/**
	 * Returns a new {@link ProxyBuilder} instance.
	 * <p>Note: Unlike other builders in the framework, ProxyBuilder serves as both the
	 * builder interface and the factory, as it builds dynamic proxy instances rather
	 * than framework objects with separate builder interfaces.
	 * @param interfaceToProxy the interface to proxy
	 * @param <T> the proxy type
	 * @return a new {@link ProxyBuilder} instance.
	 * @throws IllegalArgumentException in case {@code interfaceToProxy} is not an interface
	 */
	public static <T> ProxyBuilder<T> of(Class<T> interfaceToProxy) {
		return new ProxyBuilder<>(interfaceToProxy);
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
