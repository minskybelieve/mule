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
import org.springframework.core.io.Resource;

/**
 * @deprecated since 3.7.0, please use {@link org.mule.registry.spring.MuleArtifactContext} instead. This class will be
 * removed in Mule 4.0
 */
@Deprecated
public class MuleArtifactContext extends org.mule.registry.spring.MuleArtifactContext
{
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] configResources) throws BeansException
    {
        super(muleContext, configResources);
    }

    public MuleArtifactContext(MuleContext muleContext, Resource[] springResources) throws BeansException
    {
        super(muleContext, springResources);
    }
}
