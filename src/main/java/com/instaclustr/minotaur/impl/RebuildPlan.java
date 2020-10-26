package com.instaclustr.minotaur.impl;

import java.util.List;
import java.util.Objects;

public class RebuildPlan {

    private List<Range> tokens;
    private String source;

    public static class Range {

        private String start;
        private String end;

        public Range(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Range range = (Range) o;
            return start.equals(range.start) &&
                end.equals(range.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public String toString() {
            return "(" + start + "," + end + "]";
        }
    }

    public RebuildPlan(List<Range> tokens, String source) {
        this.tokens = tokens;
        this.source = source;
    }

    public List<Range> getTokens() {
        return tokens;
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RebuildPlan that = (RebuildPlan) o;
        return tokens.equals(that.tokens) &&
            source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokens, source);
    }

    @Override
    public String toString() {
        return "RebuildPlan{" +
            ", tokens=" + tokens +
            ", source='" + source + '\'' +
            '}';
    }
}
