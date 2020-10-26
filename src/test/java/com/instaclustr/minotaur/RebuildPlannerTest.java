package com.instaclustr.minotaur;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.instaclustr.minotaur.impl.EndpointDetails;
import com.instaclustr.minotaur.impl.RebuildPlan;
import com.instaclustr.minotaur.impl.RebuildPlanner;
import com.instaclustr.minotaur.impl.TokenRange;
import org.junit.Test;

public class RebuildPlannerTest {

    @Test
    public void testPlanner() {

        final EndpointDetails e1 = new EndpointDetails("127.0.0.1", "dc1", "r1");
        final EndpointDetails e2 = new EndpointDetails("127.0.0.2", "dc1", "r1");
        final EndpointDetails e3 = new EndpointDetails("127.0.0.3", "dc1", "r1");
        final EndpointDetails e4 = new EndpointDetails("127.0.0.4", "dc2", "r1");
        final EndpointDetails e5 = new EndpointDetails("127.0.0.5", "dc2", "r1");
        final EndpointDetails e6 = new EndpointDetails("127.0.0.6", "dc2", "r1");

        final List<TokenRange> ring = Arrays.asList(
            new TokenRange("-9223372036854775808", "-9223372036854775708", Arrays.asList(e4, e2, e5, e3, e6, e1)),
            new TokenRange("-9223372036854775708", "-3074457345618258603", Arrays.asList(e2, e5, e3, e6, e1, e4)),
            new TokenRange("-307445734561825860", "-3074457345618258503", Arrays.asList(e5, e3, e6, e1, e4, e2)),
            new TokenRange("-3074457345618258503", "3074457345618258602", Arrays.asList(e3, e6, e1, e4, e2, e5)),
            new TokenRange("3074457345618258602", "3074457345618258702", Arrays.asList(e6, e1, e4, e2, e5, e3)),
            new TokenRange("3074457345618258702", "-9223372036854775808", Arrays.asList(e1, e4, e2, e5, e3, e6))
        );

        final RebuildPlan.Range r1 = new RebuildPlan.Range("-9223372036854775808", "-9223372036854775708");
        final RebuildPlan.Range r2 = new RebuildPlan.Range("-9223372036854775708", "-3074457345618258603");
        final RebuildPlan.Range r3 = new RebuildPlan.Range("-307445734561825860", "-3074457345618258503");
        final RebuildPlan.Range r4 = new RebuildPlan.Range("-3074457345618258503", "3074457345618258602");
        final RebuildPlan.Range r5 = new RebuildPlan.Range("3074457345618258602", "3074457345618258702");
        final RebuildPlan.Range r6 = new RebuildPlan.Range("3074457345618258702", "-9223372036854775808");

        final RebuildPlanner planner = new RebuildPlanner();

        final List<RebuildPlan> plan = planner.createPlan("127.0.0.4", "dc2", "dc1", ring);
        final List<RebuildPlan> expected = Arrays.asList(
            new RebuildPlan(Arrays.asList(r1, r3, r5), "127.0.0.2"),
            new RebuildPlan(Arrays.asList(r2, r4, r6), "127.0.0.1")
        );
        assertEquals(expected, plan);

        final List<RebuildPlan> plan2 = planner.createPlan("127.0.0.5", "dc2", "dc1", ring);
        final List<RebuildPlan> expected2 = Arrays.asList(
            new RebuildPlan(Arrays.asList(r1, r3, r5), "127.0.0.3"),
            new RebuildPlan(Arrays.asList(r2, r4, r6), "127.0.0.2")
        );
        assertEquals(expected2, plan2);

        final List<RebuildPlan> plan3 = planner.createPlan("127.0.0.6", "dc2", "dc1", ring);
        final List<RebuildPlan> expected3 = Arrays.asList(
            new RebuildPlan(Arrays.asList(r1, r3, r5), "127.0.0.1"),
            new RebuildPlan(Arrays.asList(r2, r4, r6), "127.0.0.3")
        );

        assertEquals(expected3, plan3);
    }
}
