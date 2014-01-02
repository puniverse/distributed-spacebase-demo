package co.paralleluniverse.spacebase.galaxy;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.db.api.DbExecutors;
import co.paralleluniverse.db.store.galaxy.GalaxyStore;
import co.paralleluniverse.galaxy.Grid;
import co.paralleluniverse.spacebase.AABB;
import co.paralleluniverse.spacebase.ElementUpdater;
import co.paralleluniverse.spacebase.SpaceBase;
import co.paralleluniverse.spacebase.SpaceBaseBuilder;
import co.paralleluniverse.spacebase.SpatialModifyingVisitor;
import co.paralleluniverse.spacebase.SpatialQueries;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author eitan
 */
public class DistributedSpaceBaseDemo {
    static final double VELOCITY = 0.01;
    static final int MAX_NODE_ID = 2;
    static final int N = 10;
    private final Grid grid;
    private final SpaceBase<Thing> sb;
    private final int node;

    public DistributedSpaceBaseDemo(final int node) throws InterruptedException {
        // System.setProperty("co.paralleluniverse.io.useJDKSerialization", "true");
        this.node = node;
        System.setProperty("galaxy.nodeId", Integer.toString(node));
        System.setProperty("galaxy.port", Integer.toString(7050 + node));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + node));
        this.grid = co.paralleluniverse.galaxy.Grid.getInstance();
        
        System.setProperty("co.paralleluniverse.flightRecorderDumpFile", "spacebase-" + node + ".log");

        SpaceBaseBuilder builder = new SpaceBaseBuilder();
        builder.setExecutor(DbExecutors.parallel(4));
        builder.setStore(GalaxyStore.class);
        builder.setDimensions(2);
        this.sb = builder.build("glxTest");
    }

    public void run() throws InterruptedException {
        if (node == 1)
            Debug.dumpAfter(40000);
        final AABB myBounds = AABB.create(node - 1, node, 0, 1); // [0, 1] / [1, 2]     
        final AABB worldBounds = AABB.create(0, MAX_NODE_ID, 0, 1);
        final RandSpatial random = new RandSpatial();
        for (int i = 0; i < N; i++) {
            Thing thing = random.randomPoint(myBounds);
            thing.setVelocityDir(random.nextDouble() * 2 * Math.PI, VELOCITY);
            thing.setSpatialToken(sb.insert(thing, thing.getAABB()));
            thing.getSpatialToken();
        }

        for (;;) {
            sb.queryForUpdate(SpatialQueries.intersect(myBounds), new SpatialModifyingVisitor<Thing>() {
                final AtomicInteger count = new AtomicInteger();
                final long start = System.nanoTime();

                @Override
                public void visit(ElementUpdater<Thing> updater) {
                    final Thing elem = updater.elem();
                    elem.move(worldBounds);
                    final boolean offside = !myBounds.contains(elem.getAABB());
                    System.out.println("node" + node + ": " + elem + (offside ? " (out of my zone)" : ""));
                    updater.update(elem.getAABB());
                    count.incrementAndGet();
                }

                @Override
                public void done() {
                    final long nanos = System.nanoTime() - start;
                    System.out.println("node" + node + ": " + count.get() + " - " + (nanos / 1000000.0) + "  ==================================");
                    System.out.println();
                }
            });
            Thread.sleep(50);
        }
    }
}