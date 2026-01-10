package memory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedMatrixTest {

    private SharedMatrix matrix;
    // Standard 2x3 matrix for testing
    // [1, 2, 3]
    // [4, 5, 6]
    private double[][] dataRows = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    // ==========================================
    // BASIC FUNCTIONALITY
    // ==========================================

    @Test
    public void testEmptyConstructor() {
        assertEquals(0, matrix.length());
        // Default orientation for empty matrix is often implementation specific,
        // but your code returns ROW_MAJOR
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testConstructorWithData() {
        SharedMatrix m = new SharedMatrix(dataRows);
        assertEquals(2, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        
        // Verify content of first row
        assertEquals(1.0, m.get(0).get(0));
        assertEquals(3.0, m.get(0).get(2));
    }

    @Test
    public void testLoadRowMajor() {
        matrix.loadRowMajor(dataRows);
        assertEquals(2, matrix.length()); // 2 rows
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        
        // Check Row 1
        SharedVector v1 = matrix.get(0);
        assertEquals(1.0, v1.get(0));
        assertEquals(2.0, v1.get(1));
        
        // Check Row 2
        SharedVector v2 = matrix.get(1);
        assertEquals(4.0, v2.get(0));
    }

    @Test
    public void testLoadColumnMajor() {
        // dataRows is 2x3.
        // Loaded as Column Major, it should become 3 vectors of length 2.
        // Col 1: [1, 4]
        // Col 2: [2, 5]
        // Col 3: [3, 6]
        
        matrix.loadColumnMajor(dataRows);
        
        assertEquals(3, matrix.length()); // 3 columns
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        
        // Check Column 1 ([1, 4])
        SharedVector v0 = matrix.get(0);
        assertEquals(1.0, v0.get(0));
        assertEquals(4.0, v0.get(1));
        
        // Check Column 3 ([3, 6])
        SharedVector v2 = matrix.get(2);
        assertEquals(3.0, v2.get(0));
        assertEquals(6.0, v2.get(1));
    }

    @Test
    public void testReadRowMajorFromRowMajor() {
        matrix.loadRowMajor(dataRows);
        double[][] result = matrix.readRowMajor();
        
        // Should match input exactly
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        assertArrayEquals(dataRows[0], result[0]);
        assertArrayEquals(dataRows[1], result[1]);
    }

    @Test
    public void testReadRowMajorFromColumnMajor() {
        // Load as columns:
        // Col 0: [1, 4]
        // Col 1: [2, 5]
        // Col 2: [3, 6]
        matrix.loadColumnMajor(dataRows);
        
        // Read back as rows. Should reconstruct original 2x3 matrix:
        // [1, 2, 3]
        // [4, 5, 6]
        double[][] result = matrix.readRowMajor();
        
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        assertArrayEquals(dataRows[0], result[0], 0.0001);
        assertArrayEquals(dataRows[1], result[1], 0.0001);
    }

    // ==========================================
    // ERROR HANDLING
    // ==========================================

    @Test
    public void testLoadNull() {
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null));
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(null));
    }

    @Test
    public void testLoadJaggedArray() {
        double[][] jagged = {
            {1.0, 2.0},
            {3.0} // Short row
        };
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(jagged));
    }

    // ==========================================
    // CONCURRENCY & LOCKING
    // ==========================================

    @Test
    public void testConcurrentReload() throws InterruptedException {
        // One thread repeatedly reloads the matrix with different dimensions
        // Another thread repeatedly reads it.
        // Goal: Ensure no IndexOutOfBounds or partial reads occur.
        
        matrix.loadRowMajor(dataRows);
        
        int iterations = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        
        // Writer Thread
        pool.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations; i++) {
                    if (i % 2 == 0) {
                        // 2x3
                        matrix.loadRowMajor(dataRows);
                    } else {
                        // 1x1 small matrix
                        matrix.loadRowMajor(new double[][]{{99.9}});
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Reader Thread
        pool.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations; i++) {
                    double[][] res = matrix.readRowMajor();
                    // Just verify integrity (not null, consistent dimensions)
                    if (res.length > 0) {
                        int cols = res[0].length;
                        for (double[] row : res) {
                            assertEquals(cols, row.length, "Rows had inconsistent lengths during read");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        startLatch.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(finished, "Threads should finish in time");
    }
}