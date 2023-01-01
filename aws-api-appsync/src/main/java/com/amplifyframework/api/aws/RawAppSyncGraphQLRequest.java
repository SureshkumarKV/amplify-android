/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * A low level implementation of GraphQLRequest, which takes a document String, variables Map,
 *  authorization mode and authorization strategy.
 * @param <T> Type of T, the data contained in the GraphQLResponse expected from this request.
 */
public final class RawAppSyncGraphQLRequest<T> extends GraphQLRequest<T> implements AuthorizedRequest {
    private final String document;
    private final Map<String, Object> variables;
    private final AuthorizationType authorizationType;

    /**
     * Constructor for RawAppSyncGraphQLRequest.
     * @param document document String for request
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     * @param authorizationType The {@link AuthorizationType} for this request.
     */
    public RawAppSyncGraphQLRequest(
            String document,
            Type responseType,
            VariablesSerializer variablesSerializer,
            AuthorizationType authorizationType
    ) {
        this(document, Collections.emptyMap(), responseType, variablesSerializer, authorizationType);
    }

    /**
     * Constructor for RawAppSyncGraphQLRequest.
     * @param document query document to process
     * @param variables variables to be added
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     * @param authorizationType The {@link AuthorizationType} for this request.
     */
    public RawAppSyncGraphQLRequest(
            String document,
            Map<String, Object> variables,
            Type responseType,
            VariablesSerializer variablesSerializer,
            AuthorizationType authorizationType
    ) {
        super(responseType, variablesSerializer);
        this.variables = variables;
        this.document = document;
        this.authorizationType = authorizationType;
    }

    @Override
    public String getQuery() {
        return this.document;
    }

    @Override
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    /**
     * Returns the {@link AuthorizationType} for this request.
     * @return the {@link AuthorizationType} for this request.
     */
    @Override
    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        if (!super.equals(object)) {
            return false;
        }

        RawAppSyncGraphQLRequest<?> that = (RawAppSyncGraphQLRequest<?>) object;
        return ObjectsCompat.equals(document, that.document) &&
                ObjectsCompat.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), document, variables);
    }
}
