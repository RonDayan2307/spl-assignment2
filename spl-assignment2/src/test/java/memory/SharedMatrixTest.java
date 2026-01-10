package memory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedMatrixTest {

    private SharedMatrix matrix;

    @BeforeEach
    public void setUp() {
        matrix = new SharedMatrix();
    }

    // ==========================================
    // 1. CONSTRUCTOR (Data) TESTS
    // ==========================================

    @Test
    public void testConstructor_Small_Pass() {
        // 1. Small Pass: 2x2
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(2, m.length());
        assertEquals(1.0, m.get(0).get(0));
    }

    @Test
    public void testConstructor_Small_Fail() {
        // 2. Small Fail: Null input
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    public void testConstructor_Mid_Pass() {
        // 3. Mid Pass: 50x50
        double[][] data = new double[50][50];
        data[0][0] = 99.0;
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(50, m.length());
        assertEquals(99.0, m.get(0).get(0));
    }

    @Test
    public void testConstructor_Mid_Fail() {
        // 4. Mid Fail: Jagged Array (50 rows, but one is shorter)
        double[][] data = new double[50][50];
        data[25] = new double[10]; // Short row
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(data));
    }

    @Test
    public void testConstructor_Large_Pass() {
        // 5. Large Pass: 500x500
        double[][] data = new double[500][500];
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(500, m.length());
    }

    // ==========================================
    // 2. LOAD ROW MAJOR TESTS
    // ==========================================

    @Test
    public void testLoadRowMajor_Small_Pass() {
        // 1. Small Pass
        double[][] data = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(data);
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testLoadRowMajor_Small_Fail() {
        // 2. Small Fail: Null
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null));
    }

    @Test
    public void testLoadRowMajor_Mid_Pass() {
        // 3. Mid Pass: 100x10
        double[][] data = new double[100][10];
        for(int i=0; i<100; i++) Arrays.fill(data[i], 1.0);
        
        matrix.loadRowMajor(data);
        assertEquals(100, matrix.length());
        assertEquals(1.0, matrix.get(99).get(9));
    }

    @Test
    public void testLoadRowMajor_Mid_Fail() {
        // 4. Mid Fail: Inconsistent Row Lengths
        double[][] data = new double[10][10];
        data[5] = new double[11]; // One row too long
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(data));
    }

    @Test
    public void testLoadRowMajor_Large_Pass() {
        // 5. Large Pass: 1000x100
        double[][] data = new double[1000][100];
        matrix.loadRowMajor(data);
        assertEquals(1000, matrix.length());
    }

    // ==========================================
    // 3. LOAD COLUMN MAJOR TESTS
    // ==========================================

    @Test
    public void testLoadColumnMajor_Small_Pass() {
        // 1. Small Pass: 2x3 input -> 3 vectors of length 2
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        matrix.loadColumnMajor(data);
        assertEquals(3, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        // Check transpose logic: Col 0 should be [1, 4]
        assertEquals(1.0, matrix.get(0).get(0));
        assertEquals(4.0, matrix.get(0).get(1));
    }

    @Test
    public void testLoadColumnMajor_Small_Fail() {
        // 2. Small Fail: Null
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(null));
    }

    @Test
    public void testLoadColumnMajor_Mid_Pass() {
        // 3. Mid Pass: 10x100 input -> 100 vectors of length 10
        double[][] data = new double[10][100];
        matrix.loadColumnMajor(data);
        assertEquals(100, matrix.length()); // 100 columns
    }

    @Test
    public void testLoadColumnMajor_Mid_Fail() {
        // 4. Mid Fail: Jagged array check
        double[][] data = new double[20][20];
        data[10] = new double[5]; // Jagged
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(data));
    }

    @Test
    public void testLoadColumnMajor_Large_Pass() {
        // 5. Large Pass: 500x500
        double[][] data = new double[500][500];
        matrix.loadColumnMajor(data);
        assertEquals(500, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    // ==========================================
    // 4. READ ROW MAJOR TESTS
    // ==========================================

    @Test
    public void testReadRowMajor_Small_Pass() {
        // 1. Small Pass: Simple read back
        double[][] data = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(data);
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(data[0], result[0]);
    }

    @Test
    public void testReadRowMajor_Small_Fail() {
        // 2. Small Fail (Logic Check): Ensure reading from Empty matrix returns empty 0x0, not null
        SharedMatrix empty = new SharedMatrix();
        double[][] res = empty.readRowMajor();
        assertNotNull(res);
        assertEquals(0, res.length);
    }

    @Test
    public void testReadRowMajor_Mid_Pass() {
        // 3. Mid Pass: Read back Column-Major data (Transposition Logic Check)
        // Input: 2x3. Loaded as Columns. Read back should be 2x3.
        double[][] data = new double[2][3];
        data[0][2] = 5.0; // Mark a spot
        matrix.loadColumnMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        assertEquals(5.0, result[0][2]);
    }

    @Test
    public void testReadRowMajor_Mid_Fail() {
        // 4. Mid Fail (Logic): Ensure we don't get Column-Major data back
        // If we load 10x2 as Columns, internal storage is 2 vectors of len 10.
        // Reading it back must return 10x2, NOT 2x10.
        double[][] data = new double[10][2];
        matrix.loadColumnMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(10, result.length); // Rows
        assertEquals(2, result[0].length); // Cols
        assertNotEquals(2, result.length); // Should NOT be transposed
    }

    @Test
    public void testReadRowMajor_Large_Pass() {
        // 5. Large Pass: 200x200 integrity
        double[][] data = new double[200][200];
        data[199][199] = 7.0;
        matrix.loadRowMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(7.0, result[199][199]);
    }

    // ==========================================
    // 5. GET() TESTS
    // ==========================================

    @Test
    public void testGet_Small_Pass() {
        // 1. Small Pass
        matrix.loadRowMajor(new double[][]{{1}});
        assertNotNull(matrix.get(0));
    }

    @Test
    public void testGet_Small_Fail() {
        // 2. Small Fail: IndexOutOfBounds
        matrix.loadRowMajor(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(1));
    }

    @Test
    public void testGet_Mid_Pass() {
        // 3. Mid Pass: Index 50
        matrix.loadRowMajor(new double[100][1]);
        assertNotNull(matrix.get(50));
    }

    @Test
    public void testGet_Mid_Fail() {
        // 4. Mid Fail: Negative Index
        matrix.loadRowMajor(new double[100][1]);
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(-1));
    }

    @Test
    public void testGet_Large_Pass() {
        // 5. Large Pass: Index 999
        matrix.loadRowMajor(new double[1000][1]);
        assertNotNull(matrix.get(999));
    }

    // ==========================================
    // 6. LENGTH() TESTS
    // ==========================================

    @Test
    public void testLength_Small_Pass() {
        // 1. Small Pass
        matrix.loadRowMajor(new double[][]{{1}, {2}});
        assertEquals(2, matrix.length());
    }

    @Test
    public void testLength_Small_Fail() {
        // 2. Small Fail (Logic): Ensure empty length is 0
        assertEquals(0, matrix.length());
    }

    @Test
    public void testLength_Mid_Pass() {
        // 3. Mid Pass
        matrix.loadRowMajor(new double[50][10]);
        assertEquals(50, matrix.length());
    }

    @Test
    public void testLength_Mid_Fail() {
        // 4. Mid Fail (Logic): Load Col-Major and ensure length reflects COLUMNS not ROWS
        // 10 rows, 5 cols.
        matrix.loadColumnMajor(new double[10][5]); 
        assertEquals(5, matrix.length()); // Internal vectors = columns
        assertNotEquals(10, matrix.length());
    }

    @Test
    public void testLength_Large_Pass() {
        // 5. Large Pass
        matrix.loadRowMajor(new double[1000][1]);
        assertEquals(1000, matrix.length());
    }

    // ==========================================
    // 7. GET ORIENTATION TESTS
    // ==========================================

    @Test
    public void testGetOrientation_Small_Pass() {
        // 1. Small Pass: Default
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Small_Fail() {
        // 2. Small Fail (Logic): Load Column, ensure NOT Row
        matrix.loadColumnMajor(new double[][]{{1}});
        assertNotEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Mid_Pass() {
        // 3. Mid Pass: Load Row Major explicitly
        matrix.loadRowMajor(new double[10][10]);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Mid_Fail() {
        // 4. Mid Fail (Logic): Transpose internal vector and ensure matrix orientation reflects it?
        // Note: SharedMatrix.getOrientation() usually delegates to vectors[0].
        // If we hack one vector, matrix might report that.
        matrix.loadRowMajor(new double[10][10]);
        matrix.get(0).transpose(); // Hack the first vector
        // Now it should report COLUMN_MAJOR because it reads vector[0]
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Large_Pass() {
        // 5. Large Pass
        matrix.loadColumnMajor(new double[100][100]);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }
}