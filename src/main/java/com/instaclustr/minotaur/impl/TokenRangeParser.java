package com.instaclustr.minotaur.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TokenRangeParser {

    private int pos;
    private String input;

    public TokenRange parse(String input) throws IOException {
        this.input = input;
        this.pos = 0;

        match("TokenRange(start_token:");
        final String startToken = matchUntil(',');

        match(", end_token:");
        final String endToken = matchUntil(',');

        match(", endpoints:[");
        matchUntil(']');
        match("], rpc_endpoints:[");
        matchUntil(']');
        match("], endpoint_details:[");

        final List<EndpointDetails> endpointDetails = new ArrayList<>(6);

        while (input.charAt(pos) != ']') {
            match("EndpointDetails(host:");
            final String host = matchUntil(',');
            match(", datacenter:");
            final String datacenter = matchUntil(',');
            match(", rack:");
            final String rack = matchUntil(')');
            match(")");
            optional(", ");
            endpointDetails.add(new EndpointDetails(host, datacenter, rack));
        }

        match("])");

        return new TokenRange(startToken, endToken, endpointDetails);
    }

    private void match(String str) throws IOException {
        int len = str.length();
        String subStr = input.substring(pos, pos + len);
        if (str.equals(subStr)) {
            pos += len;
        } else {
            throw new IOException("Invalid TokenRange format: " + subStr + " does not match " + str);
        }
    }

    private void optional(String str) {
        int len = str.length();
        String subStr = input.substring(pos, pos + len);
        if (str.equals(subStr)) {
            pos += len;
        }
    }

    private String matchUntil(char delim) {
        int start = pos;
        while (input.charAt(pos) != delim) {
            pos++;
        }
        return input.substring(start, pos);
    }
}
