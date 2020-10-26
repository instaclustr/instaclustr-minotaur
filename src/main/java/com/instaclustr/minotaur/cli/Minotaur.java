package com.instaclustr.minotaur.cli;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.instaclustr.minotaur.impl.RebuildPlan;
import com.instaclustr.minotaur.impl.RebuildPlanner;
import com.instaclustr.minotaur.impl.TokenRange;
import com.instaclustr.minotaur.impl.TokenRangeParser;
import com.instaclustr.operations.FunctionWithEx;
import com.instaclustr.picocli.CLIApplication;
import com.instaclustr.picocli.CassandraJMXSpec;
import jmx.org.apache.cassandra.CassandraJMXConnectionInfo;
import jmx.org.apache.cassandra.CassandraObjectNames.V3;
import jmx.org.apache.cassandra.service.CassandraJMXService;
import jmx.org.apache.cassandra.service.CassandraJMXServiceImpl;
import jmx.org.apache.cassandra.service.cassandra3.EndpointSnitchInfoMBean;
import jmx.org.apache.cassandra.service.cassandra3.StorageServiceMBean;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    versionProvider = Minotaur.class,
    name = "minotaur",
    usageHelpWidth = 128,
    description = "Application for consistent rebuilding of a Cassandra cluster",
    mixinStandardHelpOptions = true
)
public class Minotaur extends CLIApplication implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Minotaur.class);

    @Mixin
    public CassandraJMXSpec jmxSpec;

    private CassandraJMXService jmxService;

    @Option(names = {"--source-dc", "-s"}, required = true)
    public String sourceDC;

    @Option(names = {"--keyspaces", "-k"}, description = "if no keyspace is specified, all keyspaces on non-local strategy will be rebuilt.")
    public String keyspaces;

    public static void main(String[] args) {
        main(args, true);
    }

    public static void mainWithoutExit(String[] args) {
        main(args, false);
    }

    public static void main(String[] args, boolean exit) {
        int exitCode = execute(new CommandLine(new Minotaur()), args);

        if (exit) {
            System.exit(exitCode);
        }
    }

    @Override
    public String getImplementationTitle() {
        return "minotaur";
    }

    @Override
    public void run() {
        jmxService = new CassandraJMXServiceImpl(new CassandraJMXConnectionInfo(jmxSpec));

        logger.info("Starting rebuild ...");

        final List<String> keyspaces = new ArrayList<>();

        try {
            keyspaces.addAll(getKeyspaces());
        } catch (final Exception ex) {
            System.exit(1);
        }

        for (final String keyspace : keyspaces) {
            if (rebuild(getEndpoint(), getDatacenter(), describeRing(keyspace), keyspace) != 0) {
                System.exit(1);
            }
        }

        System.exit(0);
    }

    private List<String> getKeyspaces() {
        if (this.keyspaces != null) {
            return Stream.of(this.keyspaces.split(",")).collect(toList());
        } else {
            try {
                return jmxService.doWithCassandra3StorageServiceMBean(new FunctionWithEx<StorageServiceMBean, List<String>>() {
                    @Override
                    public List<String> apply(final StorageServiceMBean object) {
                        return object.getNonLocalStrategyKeyspaces();
                    }
                });
            } catch (final Exception ex) {
                logger.error("Unable to get a list of non-local strategy keyspaces!", ex);
                throw new IllegalStateException();
            }
        }
    }

    private int rebuild(final String localHost, final String datacenter, final List<TokenRange> ring, final String keyspace) {
        try {
            if (datacenter.equals(this.sourceDC)) {
                logger.error(" This host was specified as a source for rebuilding. Sources for a rebuild can only be other nodes in the cluster.");
                return 1;
            }

            logger.info("Rebuilding keyspace {}", keyspace);
            logger.debug(String.format("Host: %s, datacenter: %s, Ring: %s", localHost, datacenter, ring));

            jmxService.doWithCassandra3StorageServiceMBean(new FunctionWithEx<StorageServiceMBean, Void>() {
                @Override
                public Void apply(final StorageServiceMBean probe) {

                    final RebuildPlanner planner = new RebuildPlanner();
                    final List<RebuildPlan> plan = planner.createPlan(localHost, datacenter, sourceDC, ring);

                    for (final RebuildPlan rebuildPlan : plan) {
                        final String tokens = rebuildPlan.getTokens().stream().map(RebuildPlan.Range::toString).collect(Collectors.joining(","));

                        logger.debug(String.format("Rebuilding keyspace %s from Host: %s, Tokens: %s", keyspace, rebuildPlan.getSource(), tokens));

                        probe.rebuild(sourceDC, keyspace, tokens, rebuildPlan.getSource());
                    }
                    logger.info("Rebuild of keyspace {} finished", keyspace);

                    return null;
                }
            });

            return 0;
        } catch (final Exception ex) {
            logger.error("Unable to rebuild keyspace {}!", keyspace, ex);
            return 1;
        }
    }

    private String getDatacenter() {
        try {
            return jmxService.doWithMBean(new FunctionWithEx<EndpointSnitchInfoMBean, String>() {
                @Override
                public String apply(final EndpointSnitchInfoMBean object) {
                    return object.getDatacenter();
                }
            }, EndpointSnitchInfoMBean.class, V3.ENDPOINT_SNITCH_INFO_MBEAN_NAME);
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to get datacenter!", ex);
        }
    }

    private String getEndpoint() {
        try {
            return jmxService.doWithCassandra3StorageServiceMBean(new FunctionWithEx<StorageServiceMBean, String>() {
                @Override
                public String apply(final StorageServiceMBean ssMBean) {
                    waitUntilInitialised(ssMBean);
                    return ssMBean.getHostIdToEndpoint().get(ssMBean.getLocalHostId());
                }
            });
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to get endpoint!", ex);
        }
    }

    private List<String> describeRingJMX(final String keyspace) {
        try {
            return jmxService.doWithCassandra3StorageServiceMBean(new FunctionWithEx<StorageServiceMBean, List<String>>() {
                @Override
                public List<String> apply(final StorageServiceMBean ssMBean) throws Exception {
                    waitUntilInitialised(ssMBean);
                    return ssMBean.describeRingJMX(keyspace);
                }
            });
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to describe Cassandra ring!", ex);
        }
    }

    private List<TokenRange> describeRing(String keyspace) {
        try {

            final List<String> strRing = describeRingJMX(keyspace);
            final List<TokenRange> ring = new ArrayList<>(strRing.size());

            final TokenRangeParser parser = new TokenRangeParser();

            for (final String strTokenRange : strRing) {
                ring.add(parser.parse(strTokenRange));
            }

            return ring;
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to describe ring!", ex);
        }
    }

    private void waitUntilInitialised(final StorageServiceMBean serviceMBean) {
        Awaitility.await().pollInterval(5, SECONDS).timeout(5, MINUTES).until(() -> {
            final boolean isInitialised = serviceMBean.isInitialized();

            if (!isInitialised) {
                logger.info("Waiting until StorageServiceMBean is initialised ... sleeping for 5 secs and trying again.");
            }

            return isInitialised;
        });
    }
}
