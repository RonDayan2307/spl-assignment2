package scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TiredExecutorTest {

    // CONSTRUCTOR TESTS

    @Test
    public void testConstructor_Small_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        String report = pool.getWorkerReport();
        assertTrue(report.contains("Worker 0"));
        pool.shutdown();
    }

    @Test
    public void testConstructor_Small_Fail() {
        assertThrows(NegativeArraySizeException.class, () -> new TiredExecutor(-1));
    }

    @Test
    public void testConstructor_Mid_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(10);
        String report = pool.getWorkerReport();
        assertTrue(report.contains("Worker 9")); 
        pool.shutdown();
    }

    @Test
    public void testConstructor_Mid_Fail() {
        TiredExecutor pool = new TiredExecutor(0);
        
        Thread t = new Thread(() -> pool.submit(() -> {}));
        t.start();
        try {
            t.join(100);
        } catch (InterruptedException e) {}
        
        assertTrue(t.isAlive(), "Submit should block forever on 0-thread pool");
        t.interrupt();
    }

    @Test
    public void testConstructor_Large_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(100);
        pool.shutdown();
    }

    // SUBMIT TESTS

    @Test
    public void testSubmit_Small_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testSubmit_Small_Fail() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        pool.submit(null); 
        
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testSubmit_Mid_Pass() throws InterruptedException {
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
        TiredExecutor pool = new TiredExecutor(1);
        
        pool.submit(() -> { throw new RuntimeException("Crash!"); });
        
        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(latch::countDown);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Worker was not returned to pool after exception");
        pool.shutdown();
    }

    @Test
    public void testSubmit_Large_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(4);
        int count = 1000;
        CountDownLatch latch = new CountDownLatch(count);
        
        for (int i = 0; i < count; i++) {
            pool.submit(latch::countDown);
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
    }

    // SUBMIT ALL TESTS

    @Test
    public void testSubmitAll_Small_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(2);
        List<Runnable> tasks = Collections.singletonList(() -> {});
        pool.submitAll(tasks);
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Small_Fail() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {});
        tasks.add(null); 
        tasks.add(() -> {});

        assertDoesNotThrow(() -> pool.submitAll(tasks));
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Mid_Pass() throws InterruptedException {
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
        assertEquals(20, counter.get());
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Mid_Fail() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        pool.submitAll(Collections.emptyList());
        pool.shutdown();
    }

    @Test
    public void testSubmitAll_Large_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(10);
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0; i<500; i++) tasks.add(() -> {});
        
        long start = System.currentTimeMillis();
        pool.submitAll(tasks);
        long end = System.currentTimeMillis();
        
        assertTrue((end - start) < 5000); 
        pool.shutdown();
    }

    // SHUTDOWN TESTS

    @Test
    public void testShutdown_Small_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        pool.shutdown();
    }

    @Test
    public void testShutdown_Small_Fail() {
        TiredExecutor pool = new TiredExecutor(1);
        
        Thread.currentThread().interrupt(); // Set interrupt flag manually
        
        assertThrows(InterruptedException.class, pool::shutdown);
        
        Thread.interrupted(); 
    }

    @Test
    public void testShutdown_Mid_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(2);
        pool.submit(() -> {
            try { Thread.sleep(100); } catch (Exception e) {}
        });
        
        long start = System.currentTimeMillis();
        pool.shutdown(); 
        long end = System.currentTimeMillis();
        
        assertTrue((end - start) >= 100);
    }

    @Test
    public void testShutdown_Mid_Fail() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(5);
        pool.shutdown();
        assertDoesNotThrow(pool::shutdown);
    }

    @Test
    public void testShutdown_Large_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(50);
        for(int i=0; i<100; i++) pool.submit(() -> {});
        pool.shutdown(); 
    }

    // GET WORKER REPORT TESTS

    @Test
    public void testReport_Small_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        String r = pool.getWorkerReport();
        assertNotNull(r);
        assertTrue(r.startsWith("Worker Report"));
        pool.shutdown();
    }

    @Test
    public void testReport_Small_Fail() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        assertNotNull(pool.getWorkerReport());
        pool.shutdown();
    }

    @Test
    public void testReport_Mid_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(1);
        pool.submitAll(Collections.singletonList(() -> {
            try { Thread.sleep(50); } catch(Exception e){}
        }));
        
        String r = pool.getWorkerReport();
        assertTrue(r.contains("Used:"));
        pool.shutdown();
    }

    @Test
    public void testReport_Mid_Fail() {
        TiredExecutor pool = new TiredExecutor(5);
        AtomicBoolean stop = new AtomicBoolean(false);
        
        Thread logger = new Thread(() -> {
            while(!stop.get()) pool.getWorkerReport();
        });
        logger.start();
        
        pool.submitAll(Arrays.asList(() -> {}, () -> {}));
        stop.set(true);
        try { pool.shutdown(); logger.join(); } catch(Exception e){}
    }

    @Test
    public void testReport_Large_Pass() throws InterruptedException {
        TiredExecutor pool = new TiredExecutor(100);
        String r = pool.getWorkerReport();
        assertTrue(r.length() > 1000);
        pool.shutdown();
    }
}