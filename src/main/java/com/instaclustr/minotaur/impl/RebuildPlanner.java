package com.instaclustr.minotaur.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RebuildPlanner {

    public List<RebuildPlan> createPlan(String localHost, String localDc, String sourceDc, List<TokenRange> ranges) {

        final Map<String, List<TokenRange>> sourceTokens = new LinkedHashMap<>(); // Pairs of source -> tokens

        for (final TokenRange tr : filter(localHost, ranges)) {
            final List<String> sourceHosts = hostsForDatacenter(tr.getEndpointDetails(), sourceDc);
            final List<String> localHosts = hostsForDatacenter(tr.getEndpointDetails(), localDc);

            int localPos = localHosts.indexOf(localHost);
            int sourcePos = localPos % sourceHosts.size();

            final String source = sourceHosts.get(sourcePos);

            sourceTokens.computeIfAbsent(source, s -> new ArrayList<>());
            sourceTokens.get(source).add(tr);
        }

        final List<RebuildPlan> plan = new ArrayList<>(sourceTokens.keySet().size());

        for (final Map.Entry<String, List<TokenRange>> entry : sourceTokens.entrySet()) {
            final String source = entry.getKey();
            final List<TokenRange> sourceRanges = entry.getValue();
            final List<RebuildPlan.Range> tokens = new ArrayList<>();

            for (final TokenRange tr : sourceRanges) {
                tokens.add(new RebuildPlan.Range(tr.getStartToken(), tr.getEndToken()));
            }

            plan.add(new RebuildPlan(tokens, source));
        }

        return plan;
    }

    private List<TokenRange> filter(String localEndpoint, List<TokenRange> ranges) {
        final List<TokenRange> filtered = new ArrayList<>(ranges.size());

        for (final TokenRange tr : ranges) {
            for (final EndpointDetails endpoint : tr.getEndpointDetails()) {
                if (endpoint.host.equals(localEndpoint)) {
                    filtered.add(tr);
                }
            }
        }

        return filtered;
    }

    private List<String> hostsForDatacenter(List<EndpointDetails> endpoints, String dc) {
        List<String> hosts = new ArrayList<>(endpoints.size());
        for (EndpointDetails endpoint : endpoints) {
            if (endpoint.datacenter.equals(dc)) {
                hosts.add(endpoint.host);
            }
        }
        return hosts;
    }
}
