/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;

public class FooExpressionEvaluator implements ExpressionEvaluator
{

    public Object evaluate(String expression, MuleMessage message)
    {
        return message.getPayload() + expression;
    }

    public String getName()
    {
        return "foo";
    }
}
