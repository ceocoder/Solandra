/**
 * Copyright T Jake Luciani
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lucandra.cluster;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import lucandra.CassandraUtils;
import lucandra.dht.RandomPartitioner;

import org.junit.BeforeClass;
import org.junit.Test;


public class IndexManagerTests
{
    static String indexName = String.valueOf(System.nanoTime());
    static
    {
       // Logger.getLogger(CassandraIndexManager.class).setLevel(Level.TRACE);
    }

    private class TestCassandraIndexManager extends CassandraIndexManager
    {

        public TestCassandraIndexManager(int shardsAtOnce)
        {
            super(shardsAtOnce);
            // TODO Auto-generated constructor stub
        }

        public String getToken()
        {
            return Thread.currentThread().getName();
        }

    }

    @BeforeClass
    public static void setUpBeforeClass()
    { 
        // start cassandra
        try
        {
            CassandraUtils.startupServer();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    //@Test
    public void testCassandraIncrement3()
    {

        indexName = String.valueOf(System.nanoTime());

        ExecutorService svc = Executors.newFixedThreadPool(16);

        final TestCassandraIndexManager idx = new TestCassandraIndexManager(1);

        List<Callable<Set<Long>>> callables = new ArrayList<Callable<Set<Long>>>();
        for (int i = 0; i < 1000; i++)
        {

            final int iname = i;

            Callable<Set<Long>> r = new Callable<Set<Long>>() {

                public Set<Long> call()
                {

                    long startTime = System.currentTimeMillis();

                    Set<Long> all = new HashSet<Long>(1000);

                    for (int j = 0; j < 1000; j++)
                    {
                        Long id = null;
                        try
                        {
                            id = idx.getNextId("i" + iname, "i" + j);

                            assertTrue(id + " already exists " + all.size(), all.add(id));
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }

                        if (j % 500 == 0)
                        {
                            long endTime = System.currentTimeMillis();
                            System.err.println(Thread.currentThread().getName() + " id:" + id + ", 500 iterations in "
                                    + (endTime - startTime) / 1000 + " sec");
                            startTime = endTime;
                        }
                    }

                    return all;
                }

            };

            callables.add(r);
        }

        try
        {
            List<Future<Set<Long>>> results = svc.invokeAll(callables);

            for (Future<Set<Long>> result : results)
            {
                Set<Long> thread = result.get();

            }
        }
        catch (InterruptedException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (ExecutionException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        svc.shutdown();

        try
        {
            svc.awaitTermination(10, TimeUnit.MINUTES);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Test
    public void testCassandraIncrement() throws IOException
    {

        CassandraIndexManager idx = new CassandraIndexManager(4);

        Set<Long> all = new HashSet<Long>(CassandraIndexManager.maxDocsPerShard);

        long startTime = System.currentTimeMillis();

        Map<Integer, AtomicInteger> shardStats = new HashMap<Integer, AtomicInteger>();

        // Add
        for (int i = 0; i < CassandraIndexManager.maxDocsPerShard * 2; i++)
        {
            Long id = idx.getNextId(indexName, "i" + i);

            assertNotNull(id);

            // System.err.println(CassandraIndexManager.getShardFromDocId(id));
            AtomicInteger counter = shardStats.get(CassandraIndexManager.getShardFromDocId(id));
            if (counter == null)
            {
                counter = new AtomicInteger(0);
                shardStats.put(CassandraIndexManager.getShardFromDocId(id), counter);
            }
            counter.incrementAndGet();

            assertTrue(id + " already exists " + all.size(), all.add(id));

            if (i % 10000 == 0)
            {
                long endTime = System.currentTimeMillis();
                System.err.println("added:" + id + ", 10k iterations in " + (endTime - startTime) / 1000 + " sec "
                        + shardStats);
                startTime = endTime;
            }
        }

        assertEquals(3, CassandraIndexManager.getShardFromDocId(idx.getMaxId(indexName)));

        // Update
        for (int i = 0; i < CassandraIndexManager.maxDocsPerShard * 2; i++)
        {
            Long id = idx.getId(indexName, "i" + i);

            assertNotNull("i" + i, id);

            if (i % 10000 == 0)
            {
                long endTime = System.currentTimeMillis();
                System.err.println("updated:" + id + ", 10k iterations in " + (endTime - startTime) / 1000 + " sec");
                startTime = endTime;
            }

        }
    }

    @Test
    public void testCassandraIncrement2() throws Exception
    {

        indexName = String.valueOf(System.nanoTime());

        ExecutorService svc = Executors.newFixedThreadPool(16);


        List<Callable<Set<Long>>> callables = new ArrayList<Callable<Set<Long>>>();
        for (int i = 0; i < 3; i++)
        {
            final int iidx = i;
            
            Callable<Set<Long>> r = new Callable<Set<Long>>() {

                public Set<Long> call()
                {
                    TestCassandraIndexManager idx = new TestCassandraIndexManager(4);

                    
                    long startTime = System.currentTimeMillis();

                    Set<Long> all = new HashSet<Long>(CassandraIndexManager.maxDocsPerShard);

                    for (int i = 0; i < CassandraIndexManager.maxDocsPerShard / 10; i++)
                    {
                        Long id = null;
                        try
                        {
                            id = idx.getNextId(indexName, "i" + i+"_"+iidx);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }

                        assertTrue(id + " already exists " + all.size() +" shard="+CassandraIndexManager.getShardFromDocId(id)+" id="+CassandraIndexManager.getShardedDocId(id), all.add(id));

                        if (i > 0 && i % 10000 == 0)
                        {
                            long endTime = System.currentTimeMillis();
                            System.err.println(Thread.currentThread().getName() + " id:" + id + ", 10k iterations in "
                                    + (endTime - startTime) / 1000 + " sec");
                            startTime = endTime;
                            
                           if (i < 20000)
                                try
                                {
                                    Thread.sleep(120 * 1000);
                                }
                                catch (InterruptedException e)
                                {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }                         
                        }
                    }

                    return all;
                }

            };

            callables.add(r);
        }

        List<Future<Set<Long>>> results = svc.invokeAll(callables);

        Set<Long> all = new HashSet<Long>(CassandraIndexManager.maxDocsPerShard);
        boolean hasError = false;
        for (Future<Set<Long>> result : results)
        {
            Set<Long> thread = result.get();

            for (Long id : thread)
            {
                if (!all.add(id))
                {
                    System.err.println(id + " already exists " + all.size()+" shard="+CassandraIndexManager.getShardFromDocId(id)+" id="+CassandraIndexManager.getShardedDocId(id));
                    hasError = true;
                    
                }
            }
        }
        
        if(hasError)
            fail("Found duplicate entries");

        svc.shutdown();

        svc.awaitTermination(10, TimeUnit.MINUTES);

    }

   // @Test
    public void testCustomRandomPartitioner()
    {
        String[] keys = new String[] { "0", "83316744970572273156255124564039073023",
                "22040284005381836676397683785200205813", "43045609512509978730039130609641356928",
                "35329030817634227734261170198958572329", "127605887595351923798765477786913079295" };

        RandomPartitioner rp = new RandomPartitioner();

        for (String key : keys)
        {
            byte[] keyBytes = key.getBytes();

            ByteBuffer hashBuf = ByteBuffer.allocate(keyBytes.length + CassandraUtils.delimeterBytes.length);
            hashBuf.put(keyBytes);
            hashBuf.put(CassandraUtils.delimeterBytes);
            hashBuf.flip();

            assertEquals(rp.getToken(hashBuf).token.abs().toString(), key);
        }
    }

}
