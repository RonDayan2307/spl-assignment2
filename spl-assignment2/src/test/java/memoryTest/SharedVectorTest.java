package memoryTest;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import memory.SharedVector;
import memory.VectorOrientation;


public class SharedVectorTest{


    @Test
    public void test_get_index_success(){
        SharedVector v = new SharedVector(new double[]{1.0,2.0}, VectorOrientation.COLUMN_MAJOR);

        double expectedForIndexZero = 1.0;
        double actualForIndexZero = v.get(0);


        Assertions.assertEquals(expectedForIndexZero, actualForIndexZero);
    }


}