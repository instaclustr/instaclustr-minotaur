package com.instaclustr.minotaur.impl;

import java.util.List;
import java.util.Objects;

public class TokenRange {

    private String startToken;
    private String endToken;
    private List<EndpointDetails> endpointDetails;

    public TokenRange(String startToken, String endToken, List<EndpointDetails> endpointDetails) {
        this.startToken = startToken;
        this.endToken = endToken;
        this.endpointDetails = endpointDetails;
    }

    public String getStartToken() {
        return startToken;
    }

    public String getEndToken() {
        return endToken;
    }

    public List<EndpointDetails> getEndpointDetails() {
        return endpointDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TokenRange that = (TokenRange) o;
        return startToken.equals(that.startToken) &&
            endToken.equals(that.endToken) &&
            endpointDetails.equals(that.endpointDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startToken, endToken, endpointDetails);
    }

    @Override
    public String toString() {
        return "TokenRange(" +
            "start_token:" + startToken +
            ", end_token:" + endToken +
            ", endpoint_details:" + endpointDetails +
            ')';
    }
}
