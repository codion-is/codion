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

import java.util.List;

/**
 * Builds a dynamic proxy for a single interface.
 * Note that if the {@link Object#equals(Object)} method is not proxied the resulting proxy is equal only to itself.
 * <pre>
 * {@code
 * List<String> list = new ArrayList<>();
 *
 * List<String> listProxy = ProxyBuilder.builder(List.class)
 *     .delegate(list)
 *     .method("add", Object.class, parameters -> {
 *       Object item = parameters.arguments().get(0);
 *       System.out.println("Adding: " + item);
 *
 *       return parameters.delegate().add(item);
 *     })
 *     .method("remove", Object.class, parameters -> {
 *       Object item = parameters.arguments().get(0);
 *       System.out.println("Removing: " + item);
 *
 *       return parameters.delegate().remove(item);
 *     })
 *     .method("equals", Object.class, parameters -> {
 *       Object object = parameters.arguments().get(0);
 *       System.out.println("Equals: " + object);
 *
 *       return parameters.delegate().equals(object);
 *     })
 *     .method("size", parameters -> {
 *       System.out.println("Size");
 *
 *       return parameters.delegate().size();
 *     })
 *     .build();
 * }
 * </pre>
 * @param <T> the proxy type
 * @see #builder(Class)
 */
public interface ProxyBuilder<T> {

	/**
	 * Sets the delegate instance to forward non-proxied method calls to.
	 * If not specified, all non-proxied methods throw {@link UnsupportedOperationException}.
	 * @param delegate the delegate instance to receive all non-proxied method calls
	 * @return this proxy builder
	 */
	ProxyBuilder<T> delegate(T delegate);

	/**
	 * Proxy the given no-argument method with the given proxy method.
	 * @param methodName the method name
	 * @param proxyMethod the proxy replacement method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String methodName, ProxyMethod<T> proxyMethod);

	/**
	 * Proxy the given single-argument method with the given proxy method.
	 * @param methodName the method name
	 * @param parameterType the method parameter type
	 * @param proxyMethod the proxy method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String methodName, Class<?> parameterType, ProxyMethod<T> proxyMethod);

	/**
	 * Proxy the given method with the given proxy method.
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @param proxyMethod the proxy method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String methodName, List<Class<?>> parameterTypes, ProxyMethod<T> proxyMethod);

	/**
	 * Builds the Proxy instance.
	 * @return a new proxy instance
	 */
	T build();

	/**
	 * A proxy method.
	 * @param <T> the proxy type
	 */
	interface ProxyMethod<T> {

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
		interface Parameters<T> {

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
	 * @param interfaceToProxy the interface to proxy
	 * @param <T> the proxy type
	 * @return a new {@link ProxyBuilder} instance.
	 * @throws IllegalArgumentException in case {@code interfaceToProxy} is not an interface
	 */
	static <T> ProxyBuilder<T> builder(Class<T> interfaceToProxy) {
		return new DefaultProxyBuilder<>(interfaceToProxy);
	}
}
