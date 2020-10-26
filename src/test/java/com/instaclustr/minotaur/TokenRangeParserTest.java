package com.instaclustr.minotaur;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.instaclustr.minotaur.impl.EndpointDetails;
import com.instaclustr.minotaur.impl.TokenRange;
import com.instaclustr.minotaur.impl.TokenRangeParser;
import org.junit.Test;

public class TokenRangeParserTest {

    @Test
    public void testParser() throws IOException {
        final String example = "TokenRange(start_token:3074457345618258602, "
            + "end_token:-9223372036854775808, "
            + "endpoints:[127.0.0.1, 127.0.0.2, 127.0.0.3], "
            + "rpc_endpoints:[127.0.0.1, 127.0.0.2, 127.0.0.3], "
            + "endpoint_details:[EndpointDetails(host:127.0.0.1, datacenter:datacenter1, rack:rack1), "
            + "EndpointDetails(host:127.0.0.2, datacenter:datacenter1, rack:rack1), "
            + "EndpointDetails(host:127.0.0.3, datacenter:datacenter1, rack:rack1)])";

        final TokenRangeParser parser = new TokenRangeParser();

        final TokenRange tr = parser.parse(example);
        assertEquals("3074457345618258602", tr.getStartToken());
        assertEquals("-9223372036854775808", tr.getEndToken());

        final List<EndpointDetails> details = tr.getEndpointDetails();

        final List<EndpointDetails> expectedDetails = Arrays.asList(
            new EndpointDetails("127.0.0.1", "datacenter1", "rack1"),
            new EndpointDetails("127.0.0.2", "datacenter1", "rack1"),
            new EndpointDetails("127.0.0.3", "datacenter1", "rack1"));

        assertEquals(expectedDetails, details);
    }
}
