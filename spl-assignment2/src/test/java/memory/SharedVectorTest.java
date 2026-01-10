package memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SharedVectorTest {

    // GET() TESTS

    @Test
    public void testGet_Small_Pass() {
        SharedVector v = new SharedVector(new double[]{10.0, 20.0, 30.0}, VectorOrientation.ROW_MAJOR);
        assertEquals(20.0, v.get(1));
    }

    @Test
    public void testGet_Small_Fail() {
        SharedVector v = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(5));
    }

    @Test
    public void testGet_Mid_Pass() {
        double[] data = new double[100];
        for(int i=0; i<100; i++) data[i] = i;
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        
        assertEquals(50.0, v.get(50));
        assertEquals(99.0, v.get(99));
    }

    @Test
    public void testGet_Mid_Fail() {
        double[] data = new double[100];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1));
    }

    @Test
    public void testGet_Large_Pass() {
        int size = 10000;
        double[] data = new double[size];
        data[size-1] = 999.9;
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        
        assertEquals(999.9, v.get(size-1));
    }

    // LENGTH() TESTS

    @Test
    public void testLength_Small_Pass() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(3, v.length());
    }

    @Test
    public void testLength_Small_Fail() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertNotEquals(0, v.length());
    }

    @Test
    public void testLength_Mid_Pass() {
        double[] data = new double[500];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(500, v.length());
    }

    @Test
    public void testLength_Mid_Fail() {
        double[] data = new double[500];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertNotEquals(499, v.length());
    }

    @Test
    public void testLength_Large_Pass() {
        double[] data = new double[100000];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(100000, v.length());
    }

    // TRANSPOSE() TESTS

    @Test
    public void testTranspose_Small_Pass() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    @Test
    public void testTranspose_Small_Fail() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertNotEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    public void testTranspose_Mid_Pass() {
        double[] data = new double[100];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.transpose();
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    public void testTranspose_Mid_Fail() {
        SharedVector v = new SharedVector(new double[100], VectorOrientation.COLUMN_MAJOR);
        v.transpose();
        assertNotEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    @Test
    public void testTranspose_Large_Pass() {
        int size = 10000;
        double[] data = new double[size];
        data[0] = 123.0;
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(123.0, v.get(0)); 
    }

    // ADD() TESTS

    @Test
    public void testAdd_Small_Pass() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        assertEquals(4.0, v1.get(0));
        assertEquals(6.0, v1.get(1));
    }

    @Test
    public void testAdd_Small_Fail() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
        assertTrue(e.getMessage().contains("orientation"));
    }

    @Test
    public void testAdd_Mid_Pass() {
        double[] d1 = new double[100];
        double[] d2 = new double[100];
        for(int i=0; i<100; i++) { d1[i]=1; d2[i]=2; }
        
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        
        assertEquals(3.0, v1.get(50));
    }

    @Test
    public void testAdd_Mid_Fail() {
        SharedVector v1 = new SharedVector(new double[100], VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[99], VectorOrientation.ROW_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
        assertTrue(e.getMessage().contains("length"));
    }

    @Test
    public void testAdd_Large_Pass() {
        int size = 20000;
        double[] d1 = new double[size];
        double[] d2 = new double[size];
        for(int i=0; i<size; i++) { d1[i]=i; d2[i]=1; }
        
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        
        assertEquals(size, v1.get(size-1)); 
    }

    // NEGATE() TESTS

    @Test
    public void testNegate_Small_Pass() {
        SharedVector v = new SharedVector(new double[]{1, -1}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1.0, v.get(0));
        assertEquals(1.0, v.get(1));
    }

    @Test
    public void testNegate_Small_Fail() {
        SharedVector v = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertNotEquals(5.0, v.get(0));
    }

    @Test
    public void testNegate_Mid_Pass() {
        double[] data = new double[100]; 
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(0.0, v.get(50), 0.0001);
    }

    @Test
    public void testNegate_Mid_Fail() {
        double[] data = new double[100];
        for(int i=0; i<100; i++) data[i] = 10;
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertFalse(v.get(50) > 0);
    }

    @Test
    public void testNegate_Large_Pass() {
        int size = 50000;
        double[] data = new double[size];
        for(int i=0; i<size; i++) data[i] = i;
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-49999.0, v.get(size-1));
    }

    // DOT() TESTS

    @Test
    public void testDot_Small_Pass() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(11.0, v1.dot(v2));
    }

    @Test
    public void testDot_Small_Fail() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    @Test
    public void testDot_Mid_Pass() {
        double[] d = new double[100];
        for(int i=0; i<100; i++) d[i] = 1.0;
        
        SharedVector v1 = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d.clone(), VectorOrientation.COLUMN_MAJOR);
        assertEquals(100.0, v1.dot(v2));
    }

    @Test
    public void testDot_Mid_Fail() {
        SharedVector v1 = new SharedVector(new double[100], VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[50], VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    @Test
    public void testDot_Large_Pass() {
        int size = 10000;
        double[] d = new double[size];
        for(int i=0; i<size; i++) d[i] = 0.5; 
        
        SharedVector v1 = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d.clone(), VectorOrientation.COLUMN_MAJOR);
        
        assertEquals(2500.0, v1.dot(v2));
    }

    // VECMATMUL() TESTS

    @Test
    public void testVecMatMul_Small_Pass() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{
            {1, 0},
            {0, 1}
        });
        v.vecMatMul(m);
        assertEquals(1.0, v.get(0));
        assertEquals(2.0, v.get(1));
    }

    @Test
    public void testVecMatMul_Small_Fail() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{{1,0},{0,1}});
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    public void testVecMatMul_Mid_Pass() {
        int size = 10;
        double[] vData = new double[size];
        double[][] mData = new double[size][size];
        
        for(int i=0; i<size; i++) {
            vData[i] = 1.0;
            for(int j=0; j<size; j++) mData[i][j] = 1.0;
        }

        SharedVector v = new SharedVector(vData, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(mData);
        
        v.vecMatMul(m);
        
        assertEquals(10.0, v.get(0));
        assertEquals(10.0, v.get(9));
    }

    @Test
    public void testVecMatMul_Mid_Fail() {
        SharedVector v = new SharedVector(new double[10], VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[5][10]);
        
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    public void testVecMatMul_Large_Pass() {
        int size = 200;
        double[] vData = new double[size];
        double[][] mData = new double[size][size];
        
        vData[0] = 55.0; 
        for(int i=0; i<size; i++) mData[i][i] = 1.0; 

        SharedVector v = new SharedVector(vData, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(mData);
        
        v.vecMatMul(m);
        
        assertEquals(55.0, v.get(0));
        assertEquals(0.0, v.get(1));
        assertEquals(0.0, v.get(size-1));
    }
}