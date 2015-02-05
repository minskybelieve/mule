/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.config.ConfigResource;
import org.mule.config.bootstrap.BootstrapException;
import org.mule.config.spring.editors.MulePropertyEditorRegistrar;
import org.mule.config.spring.processors.AnnotatedTransformerObjectPostProcessor;
import org.mule.config.spring.processors.ExpressionEnricherPostProcessor;
import org.mule.config.spring.processors.LifecycleStatePostProcessor;
import org.mule.config.spring.processors.PostRegistrationActionsPostProcessor;
import org.mule.config.spring.util.InitialisingBeanDefintionRegistry;
import org.mule.registry.MuleRegistryHelper;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context
 * that allows resources to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractXmlApplicationContext
{

    private MuleContext muleContext;
    private Resource[] springResources;
    private static final ThreadLocal<MuleContext> currentMuleContext = new ThreadLocal<MuleContext>();

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param configResources
     * @see org.mule.config.spring.SpringRegistry
     */
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] configResources)
            throws BeansException
    {
        this(muleContext, convert(configResources));
    }

    public MuleArtifactContext(MuleContext muleContext, Resource[] springResources) throws BeansException
    {
        this.muleContext = muleContext;
        this.springResources = springResources;
    }

    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)
    {
        super.prepareBeanFactory(beanFactory);

        registerEditors(beanFactory);

        addBeanPostProcessors(beanFactory,
                              new MuleContextPostProcessor(muleContext),
                              new ExpressionEvaluatorPostProcessor(muleContext),
                              new GlobalNamePostProcessor(),
                              new ExpressionEnricherPostProcessor(muleContext),
                              new NotificationListenersPostProcessor(muleContext),
                              new AnnotatedTransformerObjectPostProcessor(muleContext),
                              new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext.getRegistry()),
                              new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState())
        );

        beanFactory.registerSingleton(MuleProperties.OBJECT_MULE_CONTEXT, muleContext);


        SpringRegistryBootstrap bootstrap = new SpringRegistryBootstrap(new InitialisingBeanDefintionRegistry((BeanDefinitionRegistry) beanFactory));
        initialiseBootstrap(bootstrap);

        try
        {
            bootstrap.boot();
        }
        catch (BootstrapException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private void registerEditors(ConfigurableListableBeanFactory beanFactory)
    {
        MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
        registrar.setMuleContext(muleContext);
        beanFactory.addPropertyEditorRegistrar(registrar);
    }

    protected void initialiseBootstrap(SpringRegistryBootstrap bootstrap)
    {
        bootstrap.setMuleContext(muleContext);
    }

    private void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors)
    {
        for (BeanPostProcessor processor : processors)
        {
            beanFactory.addBeanPostProcessor(processor);
        }
    }

    private static Resource[] convert(ConfigResource[] resources)
    {
        Resource[] configResources = new Resource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            ConfigResource resource = resources[i];
            if (resource.getUrl() != null)
            {
                configResources[i] = new UrlResource(resource.getUrl());
            }
            else
            {
                try
                {
                    configResources[i] = new ByteArrayResource(IOUtils.toByteArray(resource.getInputStream()), resource.getResourceName());
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return configResources;
    }

    @Override
    protected Resource[] getConfigResources()
    {
        return springResources;
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        BeanDefinitionReader beanDefinitionReader = createBeanDefinitionReader(beanFactory);
        // Communicate mule context to parsers
        try
        {
            currentMuleContext.set(muleContext);
            beanDefinitionReader.loadBeanDefinitions(springResources);
        }
        finally
        {
            currentMuleContext.remove();
        }
    }

    protected BeanDefinitionReader createBeanDefinitionReader(DefaultListableBeanFactory beanFactory)
    {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        //hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(getBeanDefinitionDocumentReaderClass());
        //add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanDefinitionReader.getRegistry(), null);

        return beanDefinitionReader;
    }

    protected Class<? extends MuleBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass()
    {
        return MuleBeanDefinitionDocumentReader.class;
    }

    @Override
    protected DefaultListableBeanFactory createBeanFactory()
    {
        //Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
        DefaultListableBeanFactory bf = super.createBeanFactory();
        if (getParent() != null)
        {
            //Copy over all processors
            AbstractBeanFactory beanFactory = (AbstractBeanFactory) getParent().getAutowireCapableBeanFactory();
            bf.copyConfigurationFrom(beanFactory);
        }
        return bf;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public static ThreadLocal<MuleContext> getCurrentMuleContext()
    {
        return currentMuleContext;
    }
}
