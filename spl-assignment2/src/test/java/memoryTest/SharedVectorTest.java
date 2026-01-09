package memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        // Note: We copy data to avoid side effects between tests since arrays are mutable
        rowVec = new SharedVector(data1.clone(), VectorOrientation.ROW_MAJOR);
        colVec = new SharedVector(data2.clone(), VectorOrientation.COLUMN_MAJOR);
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals(3, rowVec.length());
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation());
        assertEquals(1.0, rowVec.get(0));
        assertEquals(2.0, rowVec.get(1));
        assertEquals(3.0, rowVec.get(2));
    }

    @Test
    public void testTranspose() {
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation());
        
        rowVec.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, rowVec.getOrientation());
        
        rowVec.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, rowVec.getOrientation());
    }

    @Test
    public void testNegate() {
        rowVec.negate();
        assertEquals(-1.0, rowVec.get(0));
        assertEquals(-2.0, rowVec.get(1));
        assertEquals(-3.0, rowVec.get(2));
        
        // Negate again to get back to original
        rowVec.negate();
        assertEquals(1.0, rowVec.get(0));
    }

    @Test
    public void testAddSuccess() {
        SharedVector other = new SharedVector(new double[]{10.0, 20.0, 30.0}, VectorOrientation.ROW_MAJOR);
        
        rowVec.add(other);
        
        // Expected: {1+10, 2+20, 3+30} -> {11, 22, 33}
        assertEquals(11.0, rowVec.get(0));
        assertEquals(22.0, rowVec.get(1));
        assertEquals(33.0, rowVec.get(2));
    }

    @Test
    public void testAddFailDimensionMismatch() {
        SharedVector other = new SharedVector(new double[]{10.0, 20.0}, VectorOrientation.ROW_MAJOR);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rowVec.add(other);
        });
        
        assertTrue(exception.getMessage().contains("length"));
    }

    @Test
    public void testAddFailOrientationMismatch() {
        // rowVec is ROW, colVec is COLUMN
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rowVec.add(colVec);
        });
        
        assertTrue(exception.getMessage().contains("orientation"));
    }

    @Test
    public void testDotProductSuccess() {
        // Dot product requires: ROW . COL
        // rowVec = {1, 2, 3}
        // colVec = {4, 5, 6}
        // Result = 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        
        double result = rowVec.dot(colVec);
        assertEquals(32.0, result, 0.0001);
    }

    @Test
    public void testDotFailOrientation() {
        SharedVector otherRow = new SharedVector(data2.clone(), VectorOrientation.ROW_MAJOR);
        
        // ROW . ROW should fail
        assertThrows(IllegalArgumentException.class, () -> {
            rowVec.dot(otherRow);
        });
        
        // COL . COL should fail
        SharedVector otherCol = new SharedVector(data1.clone(), VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            colVec.dot(otherCol);
        });
    }

    @Test
    public void testDotFailLength() {
        SharedVector shortCol = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.COLUMN_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> {
            rowVec.dot(shortCol);
        });
    }

    @Test
    public void testVecMatMul() {
        // Vector: [1, 2]
        SharedVector v = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        
        // Matrix:
        // [3, 4]
        // [5, 6]
        // Result: [1*3 + 2*5, 1*4 + 2*6] = [3+10, 4+12] = [13, 16]
        
        double[][] matrixData = {
            {3.0, 4.0},
            {5.0, 6.0}
        };
        SharedMatrix matrix = new SharedMatrix(matrixData);
        // Ensure matrix is loaded as ROW_MAJOR initially (SharedMatrix default for double[][])
        // vecMatMul handles the internal transposition logic.
        
        v.vecMatMul(matrix);
        
        assertEquals(13.0, v.get(0), 0.0001);
        assertEquals(16.0, v.get(1), 0.0001);
    }
    
    @Test
    public void testVecMatMulWithColumnMajorMatrix() {
        // This tests the optimization where the matrix is ALREADY Column Major.
        // Vector: [1, 2]
        SharedVector v = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        
        double[][] matrixData = {
            {3.0, 4.0},
            {5.0, 6.0}
        };
        
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(matrixData); // Explicitly load as Column Major
        
        // If the matrix is already Column Major, vecMatMul should use it directly
        // Result calculation is the same: [13, 16]
        
        v.vecMatMul(matrix);
        
        assertEquals(13.0, v.get(0), 0.0001);
        assertEquals(16.0, v.get(1), 0.0001);
    }
}