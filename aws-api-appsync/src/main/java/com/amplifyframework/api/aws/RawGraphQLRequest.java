package com.amplifyframework.api.aws;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * A low level implementation of GraphQLRequest, which takes a document String, variables Map and authorization mode.
 * @param <T> Type of T, the data contained in the GraphQLResponse expected from this request.
 */
public class RawGraphQLRequest<T> extends GraphQLRequest<T> implements AuthorizedRequest{
    private final String document;
    private final Map<String, Object> variables;
    private final AuthorizationType authorizationType;

    /**
     * Constructor for RawGraphQLRequest.
     * @param document document String for request
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public RawGraphQLRequest(
            String document,
            Type responseType,
            VariablesSerializer variablesSerializer,
            AuthorizationType authorizationType
    ) {
        this(document, Collections.emptyMap(), responseType, variablesSerializer, authorizationType);
    }

    /**
     * Constructor for RawGraphQLRequest.
     * @param document query document to process
     * @param variables variables to be added
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public RawGraphQLRequest(
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

        RawGraphQLRequest<?> that = (RawGraphQLRequest<?>) object;
        return ObjectsCompat.equals(document, that.document) &&
                ObjectsCompat.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), document, variables);
    }
}
