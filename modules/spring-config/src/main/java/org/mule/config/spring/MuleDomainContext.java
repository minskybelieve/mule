/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.config.ConfigResource;

import org.springframework.beans.BeansException;

/**
 * @deprecated since Mule 3.7.0. Please use {@link org.mule.registry.spring.MuleDomainContext} instead. This class
 * wil be removed in Mule 4.0
 */
@Deprecated
final class MuleDomainContext extends org.mule.registry.spring.MuleDomainContext
{

    public MuleDomainContext(MuleContext muleContext, ConfigResource[] configResources) throws BeansException
    {
        super(muleContext, configResources);
    }
}
