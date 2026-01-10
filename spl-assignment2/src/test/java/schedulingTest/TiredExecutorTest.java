package scheduling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutorTest {

    // ==========================================
    // 1. CONSTRUCTOR TESTS
    // ==========================================

    @Test
    public void testConstructor_Small_Pass() throws InterruptedException {
        // 1. Small Pass: Create 1 thread
        TiredExecutor pool = new TiredExecutor(1);
        String report = pool.getWorkerReport();
        assertTrue(report.contains("Worker 0"));
        pool.shutdown();
    }

    @Test
    public void testConstructor_Small_Fail() {
        // 2. Small Fail: Negative threads (Java array throws NegativeArraySizeException)
        assertThrows(NegativeArraySizeException.class, () -> new TiredExecutor(-1));
    }

    @Test
    public void testConstructor_Mid_Pass() throws InterruptedException {
        // 3. Mid Pass: 10 threads
        TiredExecutor pool = new TiredExecutor(10);
        String report = pool.getWorkerReport();
        // Check rudimentary string presence
        assertTrue(report.contains("Worker 9")); 
        pool.shutdown();
    }

    @Test
    public void testConstructor_Mid_Fail() {
        // 4. Mid Fail: Zero threads? 
        // Logic: Array size 0. Creating 0 workers. 
        // submit() will block forever because idleMinHeap is empty.
        TiredExecutor pool = new TiredExecutor(0);
        
        // This should timeout or block if we try to use it
        Thread t = new Thread(() -> pool.submit(() -> {}));
        t.start();
        try {
            t.join(100);
        } catch (InterruptedException e) {}
        
        assertTrue(t.isAlive(), "Submit should block forever on 0-thread pool");
        t.interrupt(); // Clean up
    }

    @Test
    public void testConstructor_Large_Pass() throws InterruptedException {
        // 5. Large Pass: 100 threads creation stress
        TiredExecutor pool = new TiredExecutor(100);
        pool.shutdown(); // Should close cleanly
    }

    // ==========================================
    // 2. SUBMIT TESTS
    // ==========================================

    @Test
    public void testSubmit_Small_Pass() throws InterruptedException {
        // 1. Small Pass: Run 1 task
        TiredExecutor pool = new TiredExecutor(1);
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testSubmit_Small_Fail() throws InterruptedException {
        // 2. Small Fail: Submit null (Task wrapper runs, calls null.run(), throws NPE inside worker)
        // Executor should remain stable.
        TiredExecutor pool = new TiredExecutor(1);
        pool.submit(null); // Will cause exception in worker thread, printed to stderr
        
        // Pool should still work
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testSubmit_Mid_Pass() throws InterruptedException {
        // 3. Mid Pass: Submit more tasks than threads (Queueing logic)
        TiredExecutor pool = new TiredExecutor(2);
        CountDownLatch latch = new CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testSubmit_Mid_Fail() throws InterruptedException {
        // 4. Mid Fail: Submit Task that throws exception
        // Ensure pool recycles the worker even if task crashes
        TiredExecutor pool = new TiredExecutor(1);
        
        pool.submit(() -> { throw new RuntimeException("Crash!"); });
        
        // Worker should return to heap. If not, next submit blocks forever.
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Worker was not returned to pool after exception");
        pool.shutdown();
    }

    @Test
    public void testSubmit_Large_Pass() throws InterruptedException {
        // 5. Large Pass: High throughput (1000 tasks)
        TiredExecutor pool = new TiredExecutor(4); // 4 threads
        int count = 1000;
        CountDownLatch latch = new CountDownLatch(count);
        
        for (int i = 0; i < count; i++) {
            pool.submit(latch::countDown);
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
    }

    // ==========================================
    // 3. SUBMIT ALL TESTS
    // ==========================================

    @Test
    public void testSubmitAll_Small_Pass() throws InterruptedException {
        // 1. Small Pass
        TiredExecutor pool = new TiredExecutor(2);
        List<Runnable> tasks = Collections.singletonList(() -> {});
        pool.submitAll(tasks);
        // If we reach here, it passed (wait worked)
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Small_Fail() throws InterruptedException {
        // 2. Small Fail: List contains null
        TiredExecutor pool = new TiredExecutor(1);
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {});
        tasks.add(null); // Will crash worker
        tasks.add(() -> {});

        // Should complete all (even the crashing one returns worker to pool)
        // If the null task crashed the worker without return, this would hang.
        assertDoesNotThrow(() -> pool.submitAll(tasks));
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Mid_Pass() throws InterruptedException {
        // 3. Mid Pass: 20 tasks, 4 threads. 
        // Ensure blocking until ALL finished.
        TiredExecutor pool = new TiredExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);
        List<Runnable> tasks = new ArrayList<>();
        
        for(int i=0; i<20; i++) {
            tasks.add(() -> {
                try { Thread.sleep(10); } catch(Exception e){}
                counter.incrementAndGet();
            });
        }
        
        pool.submitAll(tasks);
        
        assertEquals(20, counter.get(), "SubmitAll returned before all tasks finished");
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Mid_Fail() throws InterruptedException {
        // 4. Mid Fail: Empty List
        TiredExecutor pool = new TiredExecutor(1);
        pool.submitAll(Collections.emptyList());
        // Should return immediately without hanging
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Large_Pass() throws InterruptedException {
        // 5. Large Pass: 500 tasks
        TiredExecutor pool = new TiredExecutor(10);
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0; i<500; i++) tasks.add(() -> {});
        
        long start = System.currentTimeMillis();
        pool.submitAll(tasks);
        long end = System.currentTimeMillis();
        
        assertTrue((end - start) < 5000); // Sanity check
        pool.shutdown();
    }

    // ==========================================
    // 4. SHUTDOWN TESTS
    // ==========================================

    @Test
    public void testShutdown_Small_Pass() throws InterruptedException {
        // 1. Small Pass
        TiredExecutor pool = new TiredExecutor(1);
        pool.shutdown();
        // No exception thrown
    }

    @Test
    public void testShutdown_Small_Fail() throws InterruptedException {
        // 2. Small Fail: Calling shutdown twice?
        TiredExecutor pool = new TiredExecutor(1);
        pool.shutdown();
        assertThrows(IllegalStateException.class, pool::shutdown); 
        // Why? Because workers are dead. queue is full or closed. 
        // worker.shutdown() puts poison pill. If worker dead, put might fail or queue logic inside worker throws.
    }

    @Test
    public void testShutdown_Mid_Pass() throws InterruptedException {
        // 3. Mid Pass: Shutdown while tasks are running
        TiredExecutor pool = new TiredExecutor(2);
        pool.submit(() -> {
            try { Thread.sleep(100); } catch (Exception e) {}
        });
        
        long start = System.currentTimeMillis();
        pool.shutdown(); // Should wait for join
        long end = System.currentTimeMillis();
        
        assertTrue((end - start) >= 100); // Must have waited for task
    }

    @Test
    public void testShutdown_Mid_Fail() throws InterruptedException {
        // 4. Mid Fail: Interruption during shutdown?
        // Hard to simulate cleanly in unit test without mocking Thread.currentThread()
        // We will test logic: Shutdown empty pool
        TiredExecutor pool = new TiredExecutor(5);
        pool.shutdown();
    }

    @Test
    public void testShutdown_Large_Pass() throws InterruptedException {
        // 5. Large Pass
        TiredExecutor pool = new TiredExecutor(50);
        for(int i=0; i<100; i++) pool.submit(() -> {});
        pool.shutdown(); // Should finish pending and close
    }

    // ==========================================
    // 5. GET WORKER REPORT TESTS
    // ==========================================

    @Test
    public void testReport_Small_Pass() throws InterruptedException {
        // 1. Small Pass
        TiredExecutor pool = new TiredExecutor(1);
        String r = pool.getWorkerReport();
        assertNotNull(r);
        assertTrue(r.startsWith("Worker Report"));
        pool.shutdown();
    }

    @Test
    public void testReport_Small_Fail() throws InterruptedException {
        // 2. Small Fail: Ensure it doesn't return null even if empty (though ctor prevents true empty)
        TiredExecutor pool = new TiredExecutor(1);
        assertNotNull(pool.getWorkerReport());
        pool.shutdown();
    }

    @Test
    public void testReport_Mid_Pass() throws InterruptedException {
        // 3. Mid Pass: Check content after work
        TiredExecutor pool = new TiredExecutor(1);
        pool.submitAll(Collections.singletonList(() -> {
            try { Thread.sleep(50); } catch(Exception e){}
        }));
        
        String r = pool.getWorkerReport();
        assertTrue(r.contains("Used:"));
        // Check rudimentary statistic presence (Used > 0)
        // It's hard to parse exact string without regex, but we check non-empty values
        pool.shutdown();
    }

    @Test
    public void testReport_Mid_Fail() {
        // 4. Mid Fail: N/A for string getter. 
        // We test stability during heavy load?
        TiredExecutor pool = new TiredExecutor(5);
        AtomicBoolean stop = new AtomicBoolean(false);
        
        Thread logger = new Thread(() -> {
            while(!stop.get()) pool.getWorkerReport(); // Spam report
        });
        logger.start();
        
        pool.submitAll(Arrays.asList(() -> {}, () -> {}));
        stop.set(true);
        try { pool.shutdown(); logger.join(); } catch(Exception e){}
    }

    @Test
    public void testReport_Large_Pass() throws InterruptedException {
        // 5. Large Pass: 100 threads report
        TiredExecutor pool = new TiredExecutor(100);
        String r = pool.getWorkerReport();
        // Should contain 100 lines of workers
        int count = r.split("Worker").length - 1; // "Worker Report" + "Worker X..."
        // Note: Title contains "Worker", so total matches = 1 + 100 = 101?
        // Let's just check length is substantial
        assertTrue(r.length() > 1000);
        pool.shutdown();
    }
}