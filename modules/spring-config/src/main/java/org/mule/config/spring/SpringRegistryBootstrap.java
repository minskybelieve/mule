/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.RegistryProvider;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.bootstrap.BootstrapObjectFactory;
import org.mule.config.bootstrap.ClassPathRegistryBootstrapDiscoverer;
import org.mule.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.config.spring.factories.BootstrapObjectFactoryBean;
import org.mule.config.spring.factories.ConstantFactoryBean;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

/**
 * Specialization of {@link SimpleRegistryBootstrap which instead of registering the objects directly
 * into a {@link Registry}, generates {@link BeanDefinition}s into a {@link BeanDefinitionRegistry} so
 * that the Spring framework can create those objects when initialising an {@link ApplicationContext}}
 *
 * @since 3.7.0
 */
public class SpringRegistryBootstrap extends SimpleRegistryBootstrap implements BeanFactoryAware
{

    private final AtomicBoolean initialised = new AtomicBoolean(false);
    private BeanDefinitionRegistry beanDefinitionRegistry;

    public SpringRegistryBootstrap()
    {
        super(new ClassPathRegistryBootstrapDiscoverer());
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        checkArgument(beanFactory instanceof BeanDefinitionRegistry, "this bootstrap class only accepts BeanFactory instances which implement " + BeanDefinitionRegistry.class);
        beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            return;
        }

        super.initialise();
        try
        {
            monopolizeRegistries();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

        initialised.set(true);
    }

    @Override
    protected void registerTransformers() throws MuleException
    {
        //no-op .. will happen on post processors
    }

    @Override
    protected void doRegisterTransformer(String name, Class<?> returnClass, Class<? extends Transformer> transformerClass, String mime) throws Exception
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(transformerClass);

        DataType returnType = null;

        if (returnClass != null)
        {
            returnType = DataTypeFactory.create(returnClass, mime);
            builder.addPropertyValue("returnDataType", returnType);
        }

        if (name == null)
        {
            //This will generate a default name for the transformer
            //We then prefix the name to ensure there is less chance of conflict if the user registers
            // the transformer with the same name
            name = "_" + TransformerUtils.generateTransformerName(transformerClass, returnType);
        }

        builder.addPropertyValue("name", name);
        doRegisterObject(name, builder);
    }

    private void monopolizeRegistries()
    {
        if (!(muleContext.getRegistry() instanceof RegistryProvider))
        {
            return;
        }

        for (Registry registry : ((RegistryProvider) muleContext.getRegistry()).getRegistries())
        {
            if (registry instanceof SpringRegistry)
            {
                continue;
            }

            for (Entry<String, Object> entry : registry.lookupByType(Object.class).entrySet())
            {
                registerInstance(entry.getKey(), entry.getValue());
            }

            muleContext.removeRegistry(registry);
        }
    }

    @Override
    protected void doRegisterObject(String key, String className) throws Exception
    {
        Class<?> clazz = getClass(className);
        doRegisterObject(key, clazz);
    }

    private void doRegisterObject(String key, Class<?> type) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        BeanDefinitionBuilder builder;

        if (BootstrapObjectFactory.class.isAssignableFrom(type))
        {
            builder = BeanDefinitionBuilder.rootBeanDefinition(BootstrapObjectFactoryBean.class);
            builder.addConstructorArgValue(ClassUtils.instanciateClass(type));
        }
        else
        {
            builder = BeanDefinitionBuilder.rootBeanDefinition(type);
        }

        doRegisterObject(key, builder);
    }

    private void doRegisterObject(String key, BeanDefinitionBuilder builder)
    {
        beanDefinitionRegistry.registerBeanDefinition(key, builder.getBeanDefinition());
    }

    private void registerInstance(String key, Object value)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConstantFactoryBean.class);
        builder.addConstructorArgValue(value);
        doRegisterObject(key, builder);
    }
}
