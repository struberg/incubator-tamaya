/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tamaya.resolver.internal;

import org.apache.tamaya.resolver.spi.ExpressionEvaluator;
import org.apache.tamaya.spi.ServiceContext;
import org.apache.tamaya.resolver.spi.ExpressionResolver;

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default expression evaluator that manages several instances of {@link org.apache.tamaya.resolver.spi.ExpressionResolver}.
 * Each resolver is identified by a resolver id. Each expression passed has the form resolverId:resolverExpression, which
 * has the advantage that different resolvers can be active in parallel.
 */
@Priority(10000)
public class DefaultExpressionEvaluator implements ExpressionEvaluator {

    private static final Logger LOG = Logger.getLogger(DefaultExpressionEvaluator.class.getName());

    private List<ExpressionResolver> resolvers = new ArrayList<>();

    public DefaultExpressionEvaluator() {
        for(ExpressionResolver resolver: ServiceContext.getInstance().getServices(ExpressionResolver.class)){
            resolvers.add(resolver);
        }
        Collections.sort(resolvers, this::compareExpressionResolver);
    }

    /**
     * Order ExpressionResolver reversely, the most important come first.
     *
     * @param res1 the first ExpressionResolver
     * @param res2 the second ExpressionResolver
     * @return the comparison result.
     */
    private int compareExpressionResolver(ExpressionResolver res1, ExpressionResolver res2) {
        Priority prio1 = res1.getClass().getAnnotation(Priority.class);
        Priority prio2 = res2.getClass().getAnnotation(Priority.class);
        int ord1 = prio1 != null ? prio1.value() : 0;
        int ord2 = prio2 != null ? prio2.value() : 0;
        if (ord1 < ord2) {
            return -1;
        } else if (ord1 > ord2) {
            return 1;
        } else {
            return res1.getClass().getName().compareTo(res2.getClass().getName());
        }
    }

    /**
     * Resolves an expression in the form current <code>${resolverId:expression}</code> or
     * <code>${<prefix>expression}</code>. The expression can be
     * part current any type current literal text. Also multiple expressions with mixed matching resolvers are
     * supported.
     * All control characters (${}\) can be escaped using '\'.<br>
     * So all the following are valid expressions:
     * <ul>
     * <li><code>${expression}</code></li>
     * <li><code>bla bla ${expression}</code></li>
     * <li><code>${expression} bla bla</code></li>
     * <li><code>bla bla ${expression} bla bla</code></li>
     * <li><code>${expression}${resolverId2:expression2}</code></li>
     * <li><code>foo ${expression}${resolverId2:expression2}</code></li>
     * <li><code>foo ${expression} bar ${resolverId2:expression2}</code></li>
     * <li><code>${expression}foo${resolverId2:expression2}bar</code></li>
     * <li><code>foor${expression}bar${resolverId2:expression2}more</code></li>
     * <li><code>\${expression}foo${resolverId2:expression2}bar</code> (first expression is escaped).</li>
     * </ul>
     * Given {@code resolverId:} is a valid prefix targeting a {@link java.beans.Expression} explicitly, also the
     * following expressions are valid:
     * <ul>
     * <li><code>${resolverId:expression}</code></li>
     * <li><code>bla bla ${resolverId:expression}</code></li>
     * <li><code>${resolverId:expression} bla bla</code></li>
     * <li><code>bla bla ${resolverId:expression} bla bla</code></li>
     * <li><code>${resolverId:expression}${resolverId2:expression2}</code></li>
     * <li><code>foo ${resolverId:expression}${resolverId2:expression2}</code></li>
     * <li><code>foo ${resolverId:expression} bar ${resolverId2:expression2}</code></li>
     * <li><code>${resolverId:expression}foo${resolverId2:expression2}bar</code></li>
     * <li><code>foor${resolverId:expression}bar${resolverId2:expression2}more</code></li>
     * <li><code>\${resolverId:expression}foo${resolverId2:expression2}bar</code> (first expression is escaped).</li>
     * </ul>
     *
     * @param key the key to be filtered
     * @param valueToBeFiltered value to be analyzed for expressions
     * @return the resolved value, or the input in case where no expression was detected.
     */
    @Override
    public String filterProperty(String key, String valueToBeFiltered){
        StringTokenizer tokenizer = new StringTokenizer(valueToBeFiltered, "${}\\", true);
        boolean escaped = false;
        StringBuilder resolvedValue = new StringBuilder();
        StringBuilder current = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (escaped) {
                switch (token) {
                    case "n":
                        current.append("\n");
                        break;
                    case "r":
                        current.append("\r");
                        break;
                    case "t":
                        current.append("\t");
                        break;
                    default:
                        current.append(token);
                        break;
                }
                escaped = false;
                continue;
            }
            switch (token) {
                case "\\":
                    escaped = true;
                    break;
                case "$":
                    if (current.length() > 0) {
                        resolvedValue.append(current);
                        current.setLength(0);
                    }
                    if (!"{".equals(tokenizer.nextToken())) {
                        LOG.warning("Invalid expression syntax in: " + valueToBeFiltered);
                        return valueToBeFiltered;
                    }
                    String subExpression = tokenizer.nextToken();
                    if (!"}".equals(tokenizer.nextToken())) {
                        LOG.warning("Invalid expression syntax in: " + valueToBeFiltered);
                        return valueToBeFiltered;
                    }
                    // evaluate sub-expression
                    current.append(evaluateInternal(subExpression));
                    break;
                default:
                    current.append(token);
            }
        }
        if (current.length() > 0) {
            resolvedValue.append(current);
        }
        return resolvedValue.toString();
    }

    private String evaluateInternal(String subExpression) {
        String value = null;
        // 1 check for explicit prefix
        for(ExpressionResolver resolver:resolvers){
            if(subExpression.startsWith(resolver.getResolverPrefix())){
                value = resolver.evaluate(subExpression.substring(resolver.getResolverPrefix().length()));
                break;
            }
        }
        if(value==null){
            for(ExpressionResolver resolver:resolvers){
                try{
                    value = resolver.evaluate(subExpression);
                    if(value!=null){
                        return value;
                    }
                }catch(Exception e){
                    LOG.log(Level.WARNING, "Error during expression resolution from " + resolver, e);
                }
            }
        }
        if(value==null){
            LOG.log(Level.WARNING, "Unresolvable expression encountered " + subExpression);
            value = '[' + subExpression + ']';
        }
        return value;
    }


}
