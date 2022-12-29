package com.amplifyframework.api.aws;

public interface AuthorizedRequest {
    /**
     * Returns the {@link AuthorizationType} for this request.
     * @return the {@link AuthorizationType} for this request.
     */
    AuthorizationType getAuthorizationType();
}
