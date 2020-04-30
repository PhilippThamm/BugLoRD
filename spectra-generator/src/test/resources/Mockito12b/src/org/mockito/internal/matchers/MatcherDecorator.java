/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.matchers;

import org.hamcrest.Matcher;

import java.io.Serializable;

@SuppressWarnings("unchecked")
public interface MatcherDecorator extends Serializable {
    Matcher getActualMatcher();
}
