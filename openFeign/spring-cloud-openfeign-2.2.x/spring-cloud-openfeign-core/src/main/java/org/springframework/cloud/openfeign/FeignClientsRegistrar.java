/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Venil Noronha
 * @author Gang Li
 * @author Michal Domagala
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 */
class FeignClientsRegistrar
		implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	// patterned after Spring Integration IntegrationComponentScanRegistrar
	// and RibbonClientsConfigurationRegistgrar

	private ResourceLoader resourceLoader;

	private Environment environment;

	FeignClientsRegistrar() {
	}

	static void validateFallback(final Class clazz) {
		Assert.isTrue(!clazz.isInterface(),
				"Fallback class must implement the interface annotated by @FeignClient");
	}

	static void validateFallbackFactory(final Class clazz) {
		Assert.isTrue(!clazz.isInterface(), "Fallback factory must produce instances "
				+ "of fallback classes that implement the interface annotated by @FeignClient");
	}

	static String getName(String name) {
		if (!StringUtils.hasText(name)) {
			return "";
		}

		String host = null;
		try {
			String url;
			if (!name.startsWith("http://") && !name.startsWith("https://")) {
				url = "http://" + name;
			}
			else {
				url = name;
			}
			host = new URI(url).getHost();

		}
		catch (URISyntaxException e) {
		}
		Assert.state(host != null, "Service id not legal hostname (" + name + ")");
		return name;
	}

	static String getUrl(String url) {
		if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
			if (!url.contains("://")) {
				url = "http://" + url;
			}
			try {
				new URL(url);
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException(url + " is malformed", e);
			}
		}
		return url;
	}

	static String getPath(String path) {
		if (StringUtils.hasText(path)) {
			path = path.trim();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 *
	 * @param metadata 包含@EnableFeignClients与@SpringBootApplication两个注解的元数据
	 * @param registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		// day02：将@EnableFeignClients注解中的defaultConfiguration属性注册到一个缓存map
		registerDefaultConfiguration(metadata, registry);
		// day02：1) 扫描所有@FeignClient注解的接口，即扫描到所有Feign接口
		// day02：2) 将每个@FeignClient注解的configuration属性注册到一个缓存map
		// day02：3) 根据@FeignClient注解元数据生成 FeignClientFactoryBean 的BeanDefinition，
		// day02：并将这个BeanDefinition注册到一个缓存map
		registerFeignClients(metadata, registry);
	}

	private void registerDefaultConfiguration(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		// day02：获取到@EnableFeignClients注解的属性值
		Map<String, Object> defaultAttrs = metadata
			    // day02：第二个参数true，表示将Class类型的属性变为了String类型
				.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
			String name;
			// day02：判断当前注解所标注的类是否为闭合类
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			// day02：注册这个defaultConfiguration属性
			registerClientConfiguration(registry, name,
					defaultAttrs.get("defaultConfiguration"));
		}
	}

	public void registerFeignClients(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {

		LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
		// day02：获取@EnableFeignClients注解的属性元数据
		Map<String, Object> attrs = metadata
				.getAnnotationAttributes(EnableFeignClients.class.getName());
		// day02：获取clients属性值
		final Class<?>[] clients = attrs == null ? null
				: (Class<?>[]) attrs.get("clients");
		// day02：若clients属性为空，则启用类路径扫描
		if (clients == null || clients.length == 0) {
			// day02：定义一个扫描器
			ClassPathScanningCandidateComponentProvider scanner = getScanner();
			// day02：初始化扫描器
			scanner.setResourceLoader(this.resourceLoader);
			// day02：为扫描器添加一个扫描@FeignClient注解的Filter
			scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
			// day02：获取@EnableFeignClients注解中所有指定的基本包
			Set<String> basePackages = getBasePackages(metadata);
			// day02：遍历这些基本包，将其中的@FeignClient接口找到并写入到集合
			for (String basePackage : basePackages) {
				candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
			}
		}
		else {  // day02：指定了clients属性，将所有指定的Feign接口类添加到集合
			for (Class<?> clazz : clients) {
				candidateComponents.add(new AnnotatedGenericBeanDefinition(clazz));
			}
		}

		// day02：遍历 候选组件 集合(所有Feign接口都在这个集合中)
		for (BeanDefinition candidateComponent : candidateComponents) {
			// day02：只处理Feign接口组件
			if (candidateComponent instanceof AnnotatedBeanDefinition) {
				// verify annotated class is an interface
				AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
				// day02：从BeanDefinition中获取注解元数据
				AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
				// day02：断言：若不是接口，直接抛出异常
				Assert.isTrue(annotationMetadata.isInterface(),
						"@FeignClient can only be specified on an interface");

				// day02：获取@FeignClient注解属性
				Map<String, Object> attributes = annotationMetadata
						.getAnnotationAttributes(FeignClient.class.getCanonicalName());

				// day02：获取Feign名称
				String name = getClientName(attributes);

				// day02：将当前遍历的Feign接口的configuration注册到缓存map
				registerClientConfiguration(registry, name,
						attributes.get("configuration"));

				// day02：将FeignClientFactoryBean的BeanDefinition注册到缓存map
				registerFeignClient(registry, annotationMetadata, attributes);
			}  // day02：end-if
		} // day02：end-for
	}

	private void registerFeignClient(BeanDefinitionRegistry registry,
			AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
		String className = annotationMetadata.getClassName();
		Class clazz = ClassUtils.resolveClassName(className, null);

		// day02：生成一个beanFactory，其会为FeignClientFactoryBean生成一些必要的组件
		ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
				? (ConfigurableBeanFactory) registry : null;
		String contextId = getContextId(beanFactory, attributes);
		String name = getName(attributes);

		FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
		factoryBean.setBeanFactory(beanFactory);
		factoryBean.setName(name);
		factoryBean.setContextId(contextId);
		factoryBean.setType(clazz);

		BeanDefinitionBuilder definition = BeanDefinitionBuilder
				.genericBeanDefinition(clazz, () -> {
					factoryBean.setUrl(getUrl(beanFactory, attributes));
					factoryBean.setPath(getPath(beanFactory, attributes));
					factoryBean.setDecode404(Boolean
							.parseBoolean(String.valueOf(attributes.get("decode404"))));
					Object fallback = attributes.get("fallback");
					if (fallback != null) {
						factoryBean.setFallback(fallback instanceof Class
								? (Class<?>) fallback
								: ClassUtils.resolveClassName(fallback.toString(), null));
					}
					Object fallbackFactory = attributes.get("fallbackFactory");
					if (fallbackFactory != null) {
						factoryBean.setFallbackFactory(fallbackFactory instanceof Class
								? (Class<?>) fallbackFactory
								: ClassUtils.resolveClassName(fallbackFactory.toString(),
										null));
					}
					return factoryBean.getObject();
				});
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		definition.setLazyInit(true);
		validate(attributes);

		// day02：获取到FeignClientFactoryBean的beanDefinition
		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setAttribute("feignClientsRegistrarFactoryBean", factoryBean);

		// has a default, won't be null
		boolean primary = (Boolean) attributes.get("primary");

		beanDefinition.setPrimary(primary);

		String[] qualifiers = getQualifiers(attributes);
		if (ObjectUtils.isEmpty(qualifiers)) {
			qualifiers = new String[] { contextId + "FeignClient" };
		}

		// day02：生成beanDefinition的holder，通过holder可以获取到这个beanDefinition
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
				qualifiers);
		// day02：注册
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private void validate(Map<String, Object> attributes) {
		AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
		// This blows up if an aliased property is overspecified
		// FIXME annotation.getAliasedString("name", FeignClient.class, null);
		validateFallback(annotation.getClass("fallback"));
		validateFallbackFactory(annotation.getClass("fallbackFactory"));
	}

	/* for testing */ String getName(Map<String, Object> attributes) {
		return getName(null, attributes);
	}

	String getName(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
		String name = (String) attributes.get("serviceId");
		if (!StringUtils.hasText(name)) {
			name = (String) attributes.get("name");
		}
		if (!StringUtils.hasText(name)) {
			name = (String) attributes.get("value");
		}
		name = resolve(beanFactory, name);
		return getName(name);
	}

	private String getContextId(ConfigurableBeanFactory beanFactory,
			Map<String, Object> attributes) {
		String contextId = (String) attributes.get("contextId");
		if (!StringUtils.hasText(contextId)) {
			return getName(attributes);
		}

		contextId = resolve(beanFactory, contextId);
		return getName(contextId);
	}

	private String resolve(ConfigurableBeanFactory beanFactory, String value) {
		if (StringUtils.hasText(value)) {
			if (beanFactory == null) {
				return this.environment.resolvePlaceholders(value);
			}
			BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
			String resolved = beanFactory.resolveEmbeddedValue(value);
			if (resolver == null) {
				return resolved;
			}
			return String.valueOf(resolver.evaluate(resolved,
					new BeanExpressionContext(beanFactory, null)));
		}
		return value;
	}

	private String getUrl(ConfigurableBeanFactory beanFactory,
			Map<String, Object> attributes) {
		String url = resolve(beanFactory, (String) attributes.get("url"));
		return getUrl(url);
	}

	private String getPath(ConfigurableBeanFactory beanFactory,
			Map<String, Object> attributes) {
		String path = resolve(beanFactory, (String) attributes.get("path"));
		return getPath(path);
	}

	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
			@Override
			protected boolean isCandidateComponent(
					AnnotatedBeanDefinition beanDefinition) {
				boolean isCandidate = false;
				if (beanDefinition.getMetadata().isIndependent()) {
					if (!beanDefinition.getMetadata().isAnnotation()) {
						isCandidate = true;
					}
				}
				return isCandidate;
			}
		};
	}

	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		// day02：获取@EnableFeignClients注解的属性元数据
		Map<String, Object> attributes = importingClassMetadata
				.getAnnotationAttributes(EnableFeignClients.class.getCanonicalName());

		Set<String> basePackages = new HashSet<>();
		// day02：遍历value属性
		for (String pkg : (String[]) attributes.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		// day02：遍历basePackages属性
		for (String pkg : (String[]) attributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		// day02：遍历basePackageClasses属性
		for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		// day02：若没有指定以上三个属性，则将当前注解所标注的类所在的包添加到basePackages中
		if (basePackages.isEmpty()) {
			basePackages.add(
					ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}
		return basePackages;
	}

	private String getQualifier(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String qualifier = (String) client.get("qualifier");
		if (StringUtils.hasText(qualifier)) {
			return qualifier;
		}
		return null;
	}

	private String[] getQualifiers(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		List<String> qualifierList = new ArrayList<>(
				Arrays.asList((String[]) client.get("qualifiers")));
		qualifierList.removeIf(qualifier -> !StringUtils.hasText(qualifier));
		if (qualifierList.isEmpty() && getQualifier(client) != null) {
			qualifierList = Collections.singletonList(getQualifier(client));
		}
		return !qualifierList.isEmpty() ? qualifierList.toArray(new String[0]) : null;
	}

	private String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		// day02：获取contextId属性
		String value = (String) client.get("contextId");
		// day02：若value为空，则获取value属性值
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("value");
		}
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("serviceId");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}

		throw new IllegalStateException("Either 'name' or 'value' must be provided in @"
				+ FeignClient.class.getSimpleName());
	}

	private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
			Object configuration) {
		// day02：生成一个Builder
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(FeignClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(
				name + "." + FeignClientSpecification.class.getSimpleName(),
				builder.getBeanDefinition());  // day02：获取到构建的BeanDefinition
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
