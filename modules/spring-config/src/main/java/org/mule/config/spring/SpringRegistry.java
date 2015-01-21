/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @deprecated since 3.7.0. Please use {@link org.mule.registry.spring.SpringRegistry} instead. This class will be
 * removed in Mule 4.0
 */
@Deprecated
public class SpringRegistry extends org.mule.registry.spring.SpringRegistry
{

    public SpringRegistry(ApplicationContext applicationContext, MuleContext muleContext)
    {
        super(applicationContext, muleContext);
    }

    public SpringRegistry(String id, ApplicationContext applicationContext, MuleContext muleContext)
    {
        super(id, applicationContext, muleContext);
    }

    public SpringRegistry(ConfigurableApplicationContext applicationContext, ApplicationContext parentContext, MuleContext muleContext)
    {
        super(applicationContext, parentContext, muleContext);
    }

    public SpringRegistry(String id, ConfigurableApplicationContext applicationContext, ApplicationContext parentContext, MuleContext muleContext)
    {
        super(id, applicationContext, parentContext, muleContext);
    }
}
