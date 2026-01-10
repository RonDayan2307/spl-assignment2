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

    // GET WORKER ID TESTS

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
        assertNotEquals(1, t.getWorkerId());
    }

    @Test
    public void testGetWorkerId_Large_Pass() {
        TiredThread t = new TiredThread(Integer.MAX_VALUE, 1.0);
        assertEquals(Integer.MAX_VALUE, t.getWorkerId());
    }

    // NEW TASK TESTS

    @Test
    public void testNewTask_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        assertDoesNotThrow(() -> t.newTask(() -> {}));
    }

    @Test
    public void testNewTask_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        t.newTask(() -> {});
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }

    @Test
    public void testNewTask_Mid_Pass() throws InterruptedException {
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
        TiredThread t = new TiredThread(1, 1.0);
        assertThrows(NullPointerException.class, () -> t.newTask(null));
    }

    @Test
    public void testNewTask_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        int taskCount = 1000;
        CountDownLatch done = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
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

    // RUN TESTS

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
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        
        t.newTask(() -> { throw new RuntimeException("Oops"); });
        
        Thread.sleep(50);
        assertTrue(t.isAlive()); 
        
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        l.await();
        t.shutdown();
    }

    @Test
    public void testRun_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        List<Integer> results = new ArrayList<>();
        
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
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.interrupt();
        t.join(100);
        assertFalse(t.isAlive());
    }

    @Test
    public void testRun_Large_Pass() throws InterruptedException {
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

    // 4. SHUTDOWN TESTS

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
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.shutdown();
        assertDoesNotThrow(() -> t.join(1000));
        assertFalse(t.isAlive());
    }

    @Test
    public void testShutdown_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        new Thread(t::shutdown).start();
        
        l.await();
        t.join(1000);
        assertFalse(t.isAlive());
    }

    @Test
    public void testShutdown_Mid_Fail() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        t.shutdown();
        t.join();
        
        try { t.newTask(() -> {}); } catch(Exception e) {}
        
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }

    @Test
    public void testShutdown_Large_Pass() throws InterruptedException {
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

    // 5. GET FATIGUE TESTS

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
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 20); 
        t.shutdown();
        t.join();
        
        assertTrue(t.getFatigue() > 0);
    }

    @Test
    public void testGetFatigue_Mid_Fail() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1000.0);
        t.start();
        runTask(t, 5);
        t.shutdown();
        t.join();
        
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
        assertTrue(f >= 5_000_000);
    }

    // 6. IS BUSY TESTS

    @Test
    public void testIsBusy_Small_Pass() {
        TiredThread t = new TiredThread(1, 1.0);
        assertFalse(t.isBusy());
    }

    @Test
    public void testIsBusy_Small_Fail() {
        TiredThread t = new TiredThread(1, 1.0);
        assertNotEquals(true, t.isBusy());
    }

    @Test
    public void testIsBusy_Mid_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        CountDownLatch latch = new CountDownLatch(1);
        
        t.newTask(() -> {
            try {
                latch.countDown(); 
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        });
        
        latch.await();
        assertTrue(t.isBusy());
        
        t.shutdown();
    }

    @Test
    public void testIsBusy_Mid_Fail() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 1);
        Thread.sleep(20);
        assertFalse(t.isBusy());
        t.shutdown();
    }

    @Test
    public void testIsBusy_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        for(int i=0; i<10; i++) {
            assertFalse(t.isBusy());
            runTask(t, 1);
            Thread.sleep(10); 
        }
        t.shutdown();
    }

    // GET TIME USED TESTS

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
        runTask(t, 20); 
        long end = System.nanoTime();
        t.shutdown();
        t.join();
        
        assertTrue(t.getTimeUsed() > 0);
        assertTrue(t.getTimeUsed() <= (end - start));
    }

    @Test
    public void testGetTimeUsed_Mid_Fail() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        runTask(t, 10);
        t.shutdown();
        t.join();
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
        assertTrue(t.getTimeUsed() >= 20_000_000 - 5_000_000); // Allow some jitter
    }

    // GET TIME IDLE TESTS

    @Test
    public void testGetTimeIdle_Small_Pass() {
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
        Thread.sleep(50);
        
        CountDownLatch l = new CountDownLatch(1);
        t.newTask(l::countDown);
        l.await();
        
        assertTrue(t.getTimeIdle() > 0);
        t.shutdown();
    }

    @Test
    public void testGetTimeIdle_Mid_Fail() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        Thread.sleep(20);
        runTask(t, 0); 
        long idle1 = t.getTimeIdle();
        
        Thread.sleep(20);
        runTask(t, 0); 
        long idle2 = t.getTimeIdle();
        
        assertTrue(idle2 > idle1);
        t.shutdown();
    }

    @Test
    public void testGetTimeIdle_Large_Pass() throws InterruptedException {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();
        Thread.sleep(100);
        t.shutdown();
        t.join();
        
        assertTrue(t.getTimeIdle() >= 90_000_000); 
    }

    // COMPARE TO TESTS

    @Test
    public void testCompareTo_Small_Pass() {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 1.0); 
        assertEquals(0, t1.compareTo(t2));
    }

    @Test
    public void testCompareTo_Small_Fail() {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 1.0);
        assertFalse(t1.compareTo(t2) != 0);
    }

    @Test
    public void testCompareTo_Mid_Pass() throws InterruptedException {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 10.0); 
        
        t1.start();
        t2.start();
        
        runTask(t1, 10);
        runTask(t2, 10);
        
        t1.shutdown(); t2.shutdown();
        t1.join(); t2.join();
        
        assertTrue(t1.compareTo(t2) < 0);
    }

    @Test
    public void testCompareTo_Mid_Fail() throws InterruptedException {
        TiredThread t1 = new TiredThread(1, 1.0);
        TiredThread t2 = new TiredThread(2, 0.0); 
        
        t1.start();
        runTask(t1, 10);
        t1.shutdown(); t1.join();
        
        assertFalse(t1.compareTo(t2) < 0);
    }

    @Test
    public void testCompareTo_Large_Pass() throws InterruptedException {
        List<TiredThread> list = new ArrayList<>();
        TiredThread t1 = new TiredThread(1, 10.0);
        TiredThread t2 = new TiredThread(2, 1.0);
        
        t1.start(); t2.start();
        runTask(t1, 10);
        runTask(t2, 10);
        t1.shutdown(); t2.shutdown();
        t1.join(); t2.join();
        
        list.add(t1);
        list.add(t2);
        Collections.sort(list);
        
        assertEquals(t2, list.get(0));
        assertEquals(t1, list.get(1));
    }
}