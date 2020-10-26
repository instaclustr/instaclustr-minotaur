package com.instaclustr.minotaur.impl;

import java.util.Objects;

public class EndpointDetails {

    public final String host;
    public final String datacenter;
    public final String rack;

    public EndpointDetails(final String host, final String datacenter, final String rack) {
        this.host = host;
        this.datacenter = datacenter;
        this.rack = rack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointDetails that = (EndpointDetails) o;
        return host.equals(that.host) &&
            datacenter.equals(that.datacenter) &&
            rack.equals(that.rack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, datacenter, rack);
    }

    @Override
    public String toString() {
        return "EndpointDetails(" +
            "host:" + host +
            ", datacenter:" + datacenter +
            ", rack:" + rack +
            ')';
    }
}
