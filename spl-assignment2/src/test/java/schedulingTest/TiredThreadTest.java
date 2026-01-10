package scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TiredThreadTest {

    // Helper to run task and wait a bit to ensure updates propagate
    private void runTask(TiredThread t, long sleepMillis) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        t.newTask(() -> {
            try {
                if(sleepMillis > 0) Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    // ==========================================
    // 1. GET WORKER ID TESTS
    // ==========================================

    @Test
    public void testGetWorkerId_Small_Pass() {
        TiredThread t = new TiredThread(5, 1.0);
        assertEquals(5, t.getWorkerId());
    }

    @Test
    public void testGetWorkerId_Small_Fail() {
        TiredThread t = new TiredThread(5, 1.0);
        assertNotEquals(0, t.getWorkerId());
    }

    @Test
    public void testGetWorkerId_Mid_Pass() {
        TiredThread t = new TiredThread(1000, 1.0);
        assertEquals(1000, t.getWorkerId());
    }

    @Test
    public void testGetWorkerId_Mid_Fail() {
        TiredThread t = new TiredThread(-1, 1.0);
        // Ensure it returns exactly what was passed, even if negative
        assertNotEquals(1, t.getWorkerId());
    }

    @Test
    public void testGetWorkerId_Large_Pass() {
        TiredThread t = new TiredThread(Integer.MAX_VALUE, 1.0);
        assertEquals(Integer.MAX_VALUE, t.getWorkerId());
    }

    // ==========================================
    // 2. NEW TASK TESTS
    // ==========================================

    @Test
    public void testNewTask_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        // Not starting thread, just filling the queue slot (capacity 1)
        assertDoesNotThrow(() -> t.newTask(() -> {}));
    }

    @Test
    public void testNewTask_Small_Fail() {
        // Queue capacity is 1. If we add 2 tasks without starting the thread, it should fail.
        TiredThread t = new TiredThread(1, 1.0);
        t.newTask(() -> {});
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }

    @Test
    public void testNewTask_Mid_Pass() throws InterruptedException {
        // Start thread, submit task, wait for finish, submit another
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        
        CountDownLatch l1 = new CountDownLatch(1);
        t.newTask(l1::countDown);
        l1.await();

        CountDownLatch l2 = new CountDownLatch(1);
        assertDoesNotThrow(() -> t.newTask(l2::countDown));
        l2.await();
        t.shutdown();
    }

    @Test
    public void testNewTask_Mid_Fail() {
        // ArrayBlockingQueue throws NPE on null offer
        TiredThread t = new TiredThread(1, 1.0);
        assertThrows(NullPointerException.class, () -> t.newTask(null));
    }

    @Test
    public void testNewTask_Large_Pass() throws InterruptedException {
        // Feed 1000 tasks sequentially
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        int taskCount = 1000;
        CountDownLatch done = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            // Busy wait until we can submit (simple simulation of executor logic)
            boolean submitted = false;
            while(!submitted) {
                try {
                    t.newTask(done::countDown);
                    submitted = true;
                } catch(IllegalStateException e) {
                    Thread.sleep(1);
                }
            }
        }
        
        done.await(5, TimeUnit.SECONDS);
        assertEquals(0, done.getCount());
        t.shutdown();
    }

    // ==========================================
    // 3. RUN TESTS (Execution Logic)
    // ==========================================

    @Test
    public void testRun_Small_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        assertTrue(l.await(1, TimeUnit.SECONDS));
        t.shutdown();
    }

    @Test
    public void testRun_Small_Fail() throws InterruptedException {
        // Ensure thread does NOT die on RuntimeException in task
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        
        // Task that crashes
        t.newTask(() -> { throw new RuntimeException("Oops"); });
        
        // Give it time to crash and recover
        Thread.sleep(50);
        assertTrue(t.isAlive()); // Should still be alive
        
        // Should accept new task
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        l.await();
        t.shutdown();
    }

    @Test
    public void testRun_Mid_Pass() throws InterruptedException {
        // Verify multiple tasks execute in order
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        List<Integer> results = new ArrayList<>();
        
        // Since queue is size 1, we must coordinate
        CountDownLatch l1 = new CountDownLatch(1);
        t.newTask(() -> { results.add(1); l1.countDown(); });
        l1.await();
        
        CountDownLatch l2 = new CountDownLatch(1);
        t.newTask(() -> { results.add(2); l2.countDown(); });
        l2.await();
        
        assertEquals(2, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        t.shutdown();
    }

    @Test
    public void testRun_Mid_Fail() throws InterruptedException {
        // Logic Check: Uncaught exception shouldn't stop processing, 
        // but explicit Interruption should (via Poison Pill or Interrupt)
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.interrupt(); // Interrupt the worker directly
        t.join(100);
        assertFalse(t.isAlive());
    }

    @Test
    public void testRun_Large_Pass() throws InterruptedException {
        // Loop execution stability
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        for(int i=0; i<500; i++) {
            CountDownLatch l = new CountDownLatch(1);
            t.newTask(l::countDown);
            l.await();
        }
        t.shutdown();
        t.join();
        assertFalse(t.isAlive());
    }

    // ==========================================
    // 4. SHUTDOWN TESTS
    // ==========================================

    @Test
    public void testShutdown_Small_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.shutdown();
        t.join(1000);
        assertFalse(t.isAlive());
    }

    @Test
    public void testShutdown_Small_Fail() {
        // Anti-Check: Thread shouldn't be alive after join
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.shutdown();
        assertDoesNotThrow(() -> t.join(1000));
        assertFalse(t.isAlive());
    }

    @Test
    public void testShutdown_Mid_Pass() throws InterruptedException {
        // Shutdown with pending work (Logic: Poison pill is queued)
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        
        CountDownLatch l = new CountDownLatch(1);
        // Send a task
        t.newTask(l::countDown);
        // Then send shutdown (will block until task 1 is picked up)
        // Note: shutdown() puts to queue. If queue has task 1, shutdown waits until task 1 is taken.
        new Thread(t::shutdown).start();
        
        l.await(); // Task 1 finishes
        t.join(1000); // Then thread should die
        assertFalse(t.isAlive());
    }

    @Test
    public void testShutdown_Mid_Fail() throws InterruptedException {
        // Ensure sending tasks after shutdown throws exception (because thread died and queue is likely full or consumer gone)
        // Note: queue capacity 1. If thread dead, nobody takes. Queue becomes full. Next offer throws.
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.shutdown();
        t.join();
        
        // Fill queue once (might succeed if pill was taken)
        try { t.newTask(() -> {}); } catch(Exception e) {}
        
        // Second fill must fail as nobody is taking
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }

    @Test
    public void testShutdown_Large_Pass() throws InterruptedException {
        // Shutting down multiple threads
        int count = 50;
        List<TiredThread> threads = new ArrayList<>();
        for(int i=0; i<count; i++) {
            TiredThread t = new TiredThread(i, 1.0);
            t.start();
            threads.add(t);
        }
        
        for(TiredThread t : threads) t.shutdown();
        for(TiredThread t : threads) t.join(1000);
        
        for(TiredThread t : threads) assertFalse(t.isAlive());
    }

    // ==========================================
    // 5. GET FATIGUE TESTS
    // ==========================================

    @Test
    public void testGetFatigue_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        assertEquals(0.0, t.getFatigue());
    }

    @Test
    public void testGetFatigue_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        assertNotEquals(10.0, t.getFatigue());
    }

    @Test
    public void testGetFatigue_Mid_Pass() throws InterruptedException {
        // Run a task that sleeps 10ms. Fatigue = 1.0 * time
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 20); // 20ms
        t.shutdown();
        t.join();
        
        assertTrue(t.getFatigue() > 0);
    }

    @Test
    public void testGetFatigue_Mid_Fail() throws InterruptedException {
        // High Factor
        TiredThread t = new TiredThread(1, 1000.0);
        t.start();
        runTask(t, 5);
        t.shutdown();
        t.join();
        
        // Fatigue should be huge
        assertTrue(t.getFatigue() > 1000);
    }

    @Test
    public void testGetFatigue_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 0.5);
        t.start();
        runTask(t, 10);
        t.shutdown();
        t.join();
        
        double f = t.getFatigue();
        // Time used >= 10ms (10,000,000 ns). Fatigue = 0.5 * time
        assertTrue(f >= 5_000_000);
    }

    // ==========================================
    // 6. IS BUSY TESTS
    // ==========================================

    @Test
    public void testIsBusy_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        assertFalse(t.isBusy());
    }

    @Test
    public void testIsBusy_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        // Shouldn't be true without task
        assertNotEquals(true, t.isBusy());
    }

    @Test
    public void testIsBusy_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        CountDownLatch latch = new CountDownLatch(1);
        
        t.newTask(() -> {
            try {
                // While inside task, busy should be true (best effort check)
                // We can't easily check "t.isBusy()" from here reliably due to race,
                // but we can check from outside while this sleeps.
                latch.countDown(); 
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        });
        
        latch.await(); // Wait for task to start
        // Short window where it is definitely sleeping
        assertTrue(t.isBusy());
        
        t.shutdown();
    }

    @Test
    public void testIsBusy_Mid_Fail() throws InterruptedException {
        // Anti-check: After task finishes, it must go back to false
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 1);
        // Wait a tiny bit for cleanup
        Thread.sleep(20);
        assertFalse(t.isBusy());
        t.shutdown();
    }

    @Test
    public void testIsBusy_Large_Pass() throws InterruptedException {
        // Toggle check
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        for(int i=0; i<10; i++) {
            assertFalse(t.isBusy());
            runTask(t, 1);
            Thread.sleep(10); // Ensure it clears
        }
        t.shutdown();
    }

    // ==========================================
    // 7. GET TIME USED TESTS
    // ==========================================

    @Test
    public void testGetTimeUsed_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        assertEquals(0, t.getTimeUsed());
    }

    @Test
    public void testGetTimeUsed_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        assertNotEquals(100, t.getTimeUsed());
    }

    @Test
    public void testGetTimeUsed_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        long start = System.nanoTime();
        runTask(t, 20); // sleep 20ms
        long end = System.nanoTime();
        t.shutdown();
        t.join();
        
        assertTrue(t.getTimeUsed() > 0);
        // Can't be more than wall clock time
        assertTrue(t.getTimeUsed() <= (end - start));
    }

    @Test
    public void testGetTimeUsed_Mid_Fail() throws InterruptedException {
        // Anti-check logic
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 10);
        t.shutdown();
        t.join();
        // Time used must be at least the sleep time (approx 10ms = 10,000,000ns)
        assertFalse(t.getTimeUsed() < 5_000_000);
    }

    @Test
    public void testGetTimeUsed_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 10);
        runTask(t, 10);
        t.shutdown();
        t.join();
        // 20ms total roughly
        assertTrue(t.getTimeUsed() >= 20_000_000 - 5_000_000); // Allow some jitter
    }

    // ==========================================
    // 8. GET TIME IDLE TESTS
    // ==========================================

    @Test
    public void testGetTimeIdle_Small_Pass() {
        // Initially 0 until accessed or run? 
        // Code: timeIdle initialized to 0. Updates in run loop.
        TiredThread t = new TiredThread(1, 1.0);
        assertEquals(0, t.getTimeIdle());
    }

    @Test
    public void testGetTimeIdle_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        assertNotEquals(-1, t.getTimeIdle());
    }

    @Test
    public void testGetTimeIdle_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        // It sits idle for 50ms
        Thread.sleep(50);
        
        // Push a task to trigger the idle-time update (logic is: update idle time just after taking from queue)
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        l.await();
        
        assertTrue(t.getTimeIdle() > 0);
        t.shutdown();
    }

    @Test
    public void testGetTimeIdle_Mid_Fail() throws InterruptedException {
        // Verify idle time is accumulating
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        Thread.sleep(20);
        runTask(t, 0); // trigger update
        long idle1 = t.getTimeIdle();
        
        Thread.sleep(20);
        runTask(t, 0); // trigger update
        long idle2 = t.getTimeIdle();
        
        assertTrue(idle2 > idle1);
        t.shutdown();
    }

    @Test
    public void testGetTimeIdle_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        // Wait 100ms
        Thread.sleep(100);
        t.shutdown(); // Shutdown sends pill -> triggers take() -> updates idle
        t.join();
        
        assertTrue(t.getTimeIdle() >= 90_000_000); // Approx 100ms
    }

    // ==========================================
    // 9. COMPARE TO TESTS
    // ==========================================

    @Test
    public void testCompareTo_Small_Pass() {
        TiredThread t1 = new TiredThread(1, 1.0); // Fatigue 0
        TiredThread t2 = new TiredThread(2, 1.0); // Fatigue 0
        assertEquals(0, t1.compareTo(t2));
    }

    @Test
    public void testCompareTo_Small_Fail() {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 1.0);
        // Should not be unequal
        assertFalse(t1.compareTo(t2) != 0);
    }

    @Test
    public void testCompareTo_Mid_Pass() throws InterruptedException {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 10.0); // High factor
        
        t1.start();
        t2.start();
        
        // Run tasks on both. t2 should gain more fatigue due to factor.
        runTask(t1, 10);
        runTask(t2, 10);
        
        t1.shutdown(); t2.shutdown();
        t1.join(); t2.join();
        
        // t2 fatigue > t1 fatigue. So t1 < t2. result should be negative.
        // wait... compareTo: Double.compare(this, other). 
        // if this < other -> -1.
        assertTrue(t1.compareTo(t2) < 0);
    }

    @Test
    public void testCompareTo_Mid_Fail() throws InterruptedException {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 0.0); // Factor 0 -> Fatigue always 0
        
        t1.start();
        runTask(t1, 10); // t1 gains fatigue
        t1.shutdown(); t1.join();
        
        // t1 > t2
        assertFalse(t1.compareTo(t2) < 0);
    }

    @Test
    public void testCompareTo_Large_Pass() throws InterruptedException {
        // Sorting test
        List<TiredThread> list = new ArrayList<>();
        TiredThread t1 = new TiredThread(1, 10.0); // will be high
        TiredThread t2 = new TiredThread(2, 1.0);  // will be low
        
        t1.start(); t2.start();
        runTask(t1, 10);
        runTask(t2, 10);
        t1.shutdown(); t2.shutdown();
        t1.join(); t2.join();
        
        list.add(t1);
        list.add(t2);
        Collections.sort(list);
        
        assertEquals(t2, list.get(0)); // Low fatigue first
        assertEquals(t1, list.get(1)); // High fatigue second
    }
}