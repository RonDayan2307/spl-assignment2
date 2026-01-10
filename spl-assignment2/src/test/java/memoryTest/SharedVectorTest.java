package memory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedVectorTest {

    private SharedVector rowVec;
    private SharedVector colVec;
    private double[] data1 = {1.0, 2.0, 3.0};
    private double[] data2 = {4.0, 5.0, 6.0};

    @BeforeEach
    public void setUp() {
        // Reset vectors before each test
        rowVec = new SharedVector(data1.clone(), VectorOrientation.ROW_MAJOR);
        colVec = new SharedVector(data2.clone(), VectorOrientation.COLUMN_MAJOR);
    }

    // ==========================================
    // BASIC TESTS (From previous run)
    // ==========================================

    @Test
    public void testGetAndLength() {
        assertEquals(3, rowVec.length());
        assertEquals(1.0, rowVec.get(0));
        assertEquals(3.0, rowVec.get(2));
        assertDoesNotThrow(() -> rowVec.get(1));
    }

    @Test
    public void testGetOrientation() {
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, colVec.getOrientation());
    }

    @Test
    public void testTranspose() {
        rowVec.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, rowVec.getOrientation(), 
            "Row vector should become Column vector after transpose");

        colVec.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, colVec.getOrientation(), 
            "Column vector should become Row vector after transpose");
    }

    @Test
    public void testAddValid() {
        SharedVector other = new SharedVector(new double[]{10.0, 10.0, 10.0}, VectorOrientation.ROW_MAJOR);
        rowVec.add(other);
        assertEquals(11.0, rowVec.get(0));
        assertEquals(12.0, rowVec.get(1));
        assertEquals(13.0, rowVec.get(2));
    }

    @Test
    public void testAddMismatchLength() {
        SharedVector shortVec = new SharedVector(new double[]{1.0, 1.0}, VectorOrientation.ROW_MAJOR);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> rowVec.add(shortVec));
        assertTrue(exception.getMessage().contains("length"));
    }

    @Test
    public void testAddMismatchOrientation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> rowVec.add(colVec));
        assertTrue(exception.getMessage().contains("orientation"));
    }

    @Test
    public void testNegate() {
        rowVec.negate();
        assertEquals(-1.0, rowVec.get(0));
        assertEquals(-2.0, rowVec.get(1));
        assertEquals(-3.0, rowVec.get(2));
        rowVec.negate();
        assertEquals(1.0, rowVec.get(0));
    }

    @Test
    public void testDotProductValid() {
        // [1, 2, 3] . [4, 5, 6]^T = 32
        double result = rowVec.dot(colVec);
        assertEquals(32.0, result, 0.0001);
    }

    @Test
    public void testDotProductInvalidOrientation() {
        SharedVector row2 = new SharedVector(data2.clone(), VectorOrientation.ROW_MAJOR);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> rowVec.dot(row2));
        assertTrue(exception.getMessage().contains("ROW dot COLUMN"));
    }

    @Test
    public void testLockingBehavior() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            rowVec.writeLock();
            try {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            } finally {
                rowVec.writeUnlock();
            }
            latch.countDown();
        });
        t.start();
        boolean finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "Thread should finish, implying locks were released correctly");
    }

    // ==========================================
    // COMPLEX & STRESS TESTS (New)
    // ==========================================

    @Test
    public void testChainedOperations() {
        // Scenario: Start with Row [1, 2, 3]
        // 1. Negate -> [-1, -2, -3]
        // 2. Transpose -> Column [-1, -2, -3]
        // 3. Negate -> Column [1, 2, 3]
        // 4. Dot Product with Row [1, 1, 1] (Valid: Row dot Column)
        
        SharedVector v = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector ones = new SharedVector(new double[]{1.0, 1.0, 1.0}, VectorOrientation.ROW_MAJOR);

        v.negate(); 
        assertEquals(-1.0, v.get(0));
        
        v.transpose(); 
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        
        v.negate(); 
        assertEquals(1.0, v.get(0));

        // Now we compute ones (Row) dot v (Column)
        double result = ones.dot(v);
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    public void testLargeVectorMath() {
        // Create two large vectors of size 10,000 to ensure no loop errors or overflows
        int size = 10000;
        double[] largeData1 = new double[size];
        double[] largeData2 = new double[size];
        
        for (int i = 0; i < size; i++) {
            largeData1[i] = 1.0;
            largeData2[i] = 2.0;
        }

        SharedVector v1 = new SharedVector(largeData1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(largeData2, VectorOrientation.COLUMN_MAJOR);

        // Dot product should be 1*2 * 10000 = 20000
        assertEquals(20000.0, v1.dot(v2), 0.001);
    }

    @Test
    public void testConcurrentNegateAndRead() throws InterruptedException {
        // Stress Test: 
        // 10 threads trying to read the vector while 1 thread tries to negate it continuously.
        // This ensures the ReadWriteLock is actually working (Readers can overlap, Writer is exclusive).
        
        int numReaders = 10;
        ExecutorService pool = Executors.newFixedThreadPool(numReaders + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // Writer Thread (Negates 100 times)
        pool.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    rowVec.negate();
                }
            } catch (Exception e) {
                error.set(e);
            }
        });

        // Reader Threads
        for (int i = 0; i < numReaders; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        // Just acquire read lock and read a value
                        rowVec.get(0); 
                    }
                } catch (Exception e) {
                    error.set(e);
                }
            });
        }

        startLatch.countDown(); // Start all threads
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        if (error.get() != null) {
            fail("Exception occurred in concurrent test: " + error.get().getMessage());
        }
        
        // Final state check: The vector was negated 100 times (even number), so it should be back to original
        // [1, 2, 3]
        assertEquals(1.0, rowVec.get(0), 0.001, "After 100 negations, value should be original");
    }

    @Test
    public void testConcurrentAdd() throws InterruptedException {
        // Stress Test:
        // 50 threads all adding [1, 1, 1] to the main vector [0, 0, 0].
        // If locking is correct, final result should be [50, 50, 50].
        // If locking is missing/race conditions exist, some updates will be lost (e.g., result < 50).

        SharedVector base = new SharedVector(new double[]{0.0, 0.0, 0.0}, VectorOrientation.ROW_MAJOR);
        SharedVector increment = new SharedVector(new double[]{1.0, 1.0, 1.0}, VectorOrientation.ROW_MAJOR);
        
        int numThreads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < numThreads; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    base.add(increment);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        assertEquals(50.0, base.get(0), 0.001, "Race condition detected! Expected 50.0 but got " + base.get(0));
    }
}