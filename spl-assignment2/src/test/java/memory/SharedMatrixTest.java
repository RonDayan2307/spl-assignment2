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

    // CONSTRUCTOR TESTS

    @Test
    public void testConstructor_Small_Pass() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(2, m.length());
        assertEquals(1.0, m.get(0).get(0));
    }

    @Test
    public void testConstructor_Small_Fail() {
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    public void testConstructor_Mid_Pass() {
        double[][] data = new double[50][50];
        data[0][0] = 99.0;
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(50, m.length());
        assertEquals(99.0, m.get(0).get(0));
    }

    @Test
    public void testConstructor_Mid_Fail() {
        double[][] data = new double[50][50];
        data[25] = new double[10]; 
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(data));
    }

    @Test
    public void testConstructor_Large_Pass() {
        double[][] data = new double[500][500];
        SharedMatrix m = new SharedMatrix(data);
        assertEquals(500, m.length());
    }

    // LOAD ROW MAJOR TESTS

    @Test
    public void testLoadRowMajor_Small_Pass() {
        double[][] data = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(data);
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testLoadRowMajor_Small_Fail() {
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null));
    }

    @Test
    public void testLoadRowMajor_Mid_Pass() {
        double[][] data = new double[100][10];
        for(int i=0; i<100; i++) Arrays.fill(data[i], 1.0);
        
        matrix.loadRowMajor(data);
        assertEquals(100, matrix.length());
        assertEquals(1.0, matrix.get(99).get(9));
    }

    @Test
    public void testLoadRowMajor_Mid_Fail() {
        double[][] data = new double[10][10];
        data[5] = new double[11]; 
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(data));
    }

    @Test
    public void testLoadRowMajor_Large_Pass() {
        double[][] data = new double[1000][100];
        matrix.loadRowMajor(data);
        assertEquals(1000, matrix.length());
    }

    // LOAD COLUMN MAJOR TESTS

    @Test
    public void testLoadColumnMajor_Small_Pass() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        matrix.loadColumnMajor(data);
        assertEquals(3, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        assertEquals(1.0, matrix.get(0).get(0));
        assertEquals(4.0, matrix.get(0).get(1));
    }

    @Test
    public void testLoadColumnMajor_Small_Fail() {
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(null));
    }

    @Test
    public void testLoadColumnMajor_Mid_Pass() {
        double[][] data = new double[10][100];
        matrix.loadColumnMajor(data);
        assertEquals(100, matrix.length()); 
    }

    @Test
    public void testLoadColumnMajor_Mid_Fail() {
        double[][] data = new double[20][20];
        data[10] = new double[5]; 
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(data));
    }

    @Test
    public void testLoadColumnMajor_Large_Pass() {
        double[][] data = new double[500][500];
        matrix.loadColumnMajor(data);
        assertEquals(500, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    // READ ROW MAJOR TESTS

    @Test
    public void testReadRowMajor_Small_Pass() {
        double[][] data = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(data);
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(data[0], result[0]);
    }

    @Test
    public void testReadRowMajor_Small_Fail() {
        SharedMatrix empty = new SharedMatrix();
        double[][] res = empty.readRowMajor();
        assertNotNull(res);
        assertEquals(0, res.length);
    }

    @Test
    public void testReadRowMajor_Mid_Pass() {
        double[][] data = new double[2][3];
        data[0][2] = 5.0; 
        matrix.loadColumnMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        assertEquals(5.0, result[0][2]);
    }

    @Test
    public void testReadRowMajor_Mid_Fail() {
        double[][] data = new double[10][2];
        matrix.loadColumnMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(10, result.length); 
        assertEquals(2, result[0].length); 
        assertNotEquals(2, result.length); 
    }

    @Test
    public void testReadRowMajor_Large_Pass() {
        double[][] data = new double[200][200];
        data[199][199] = 7.0;
        matrix.loadRowMajor(data);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(7.0, result[199][199]);
    }

    // GET() TESTS

    @Test
    public void testGet_Small_Pass() {
        matrix.loadRowMajor(new double[][]{{1}});
        assertNotNull(matrix.get(0));
    }

    @Test
    public void testGet_Small_Fail() {
        matrix.loadRowMajor(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(1));
    }

    @Test
    public void testGet_Mid_Pass() {
        matrix.loadRowMajor(new double[100][1]);
        assertNotNull(matrix.get(50));
    }

    @Test
    public void testGet_Mid_Fail() {
        matrix.loadRowMajor(new double[100][1]);
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(-1));
    }

    @Test
    public void testGet_Large_Pass() {
        matrix.loadRowMajor(new double[1000][1]);
        assertNotNull(matrix.get(999));
    }

    // LENGTH() TESTS

    @Test
    public void testLength_Small_Pass() {
        matrix.loadRowMajor(new double[][]{{1}, {2}});
        assertEquals(2, matrix.length());
    }

    @Test
    public void testLength_Small_Fail() {
        assertEquals(0, matrix.length());
    }

    @Test
    public void testLength_Mid_Pass() {
        matrix.loadRowMajor(new double[50][10]);
        assertEquals(50, matrix.length());
    }

    @Test
    public void testLength_Mid_Fail() {
        matrix.loadColumnMajor(new double[10][5]); 
        assertEquals(5, matrix.length()); 
        assertNotEquals(10, matrix.length());
    }

    @Test
    public void testLength_Large_Pass() {
        matrix.loadRowMajor(new double[1000][1]);
        assertEquals(1000, matrix.length());
    }

    // GET ORIENTATION TESTS

    @Test
    public void testGetOrientation_Small_Pass() {
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Small_Fail() {
        matrix.loadColumnMajor(new double[][]{{1}});
        assertNotEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Mid_Pass() {
        matrix.loadRowMajor(new double[10][10]);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Mid_Fail() {
        matrix.loadRowMajor(new double[10][10]);
        matrix.get(0).transpose(); 
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    @Test
    public void testGetOrientation_Large_Pass() {
        matrix.loadColumnMajor(new double[100][100]);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }
}