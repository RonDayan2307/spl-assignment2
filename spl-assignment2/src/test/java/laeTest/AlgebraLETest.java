package laeTest;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import memory.SharedMatrix;
import spl.lae.LinearAlgebraEngine;

public class AlgebraLETest{



    @Test
    public void testCreateAddTasks(){
        LinearAlgebraEngine e = new LinearAlgebraEngine(4);
        SharedMatrix rightSharedMatrix = new SharedMatrix(new double[][] {{1.0,2.0,3.0},{1.0,2.0,3.0},{1.0,2.0,3.0}}); //matrix
        SharedMatrix leftSharedMatrix = new SharedMatrix(new double[][] {{1.0,2.0,3.0},{1.0,2.0,3.0},{1.0,2.0,3.0}}); // left matrix 
        e.setLeft(leftSharedMatrix);
        e.setRight(leftSharedMatrix);
        List<Runnable> addTasks = e.createAddTasks();
        SharedMatrix expectedSharedMatrix = new SharedMatrix(new double[][] {{2.0,4.0,6.0},{2.0,4.0,6.0},{2.0,4.0,6.0}});// define what is the expected matrix , what is the expected runnables 
        for (Runnable elem : addTasks) {
            Thread t = new Thread(elem);
            t.start();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        SharedMatrix actualRes = e.getLeft();
        for(int i = 0;i<actualRes.length();i++){
            System.err.println();
            for(int j = 0 ; j<actualRes.get(i).length();j++){
                System.err.print(actualRes.get(i).get(j));
                Assertions.assertEquals(expectedSharedMatrix.get(i).get(j), actualRes.get(i).get(j));
            }
        }
    }

    @Test
    public void createMultiplyTasks(){
        LinearAlgebraEngine e = new LinearAlgebraEngine(4);
        SharedMatrix rightSharedMatrix = new SharedMatrix(new double[][] {{1.0,2.0,3.0},{1.0,2.0,3.0},{1.0,2.0,3.0}}); //matrix
        SharedMatrix leftSharedMatrix = new SharedMatrix(new double[][] {{1.0,2.0,3.0},{1.0,2.0,3.0},{1.0,2.0,3.0}}); // left matrix 
        e.setLeft(leftSharedMatrix);
        e.setRight(leftSharedMatrix);
        List<Runnable> addTasks = e.createMultiplyTasks();
        SharedMatrix expectedSharedMatrix = new SharedMatrix(new double[][] {{6.0,12.0,18.0},{6.0,12.0,18.0},{6.0,12.0,18.0}});// define what is the expected matrix , what is the expected runnables 
        for (Runnable elem : addTasks) {
            //Thread t = new Thread(elem);
            //t.start();
            elem.run();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        SharedMatrix actualRes = e.getLeft();
        for(int i = 0;i<actualRes.length();i++){
            System.err.println();
            for(int j = 0 ; j<actualRes.get(i).length();j++){
                System.err.print(actualRes.get(i).get(j));
                Assertions.assertEquals(expectedSharedMatrix.get(i).get(j), actualRes.get(i).get(j));
            }
        }
        

    }
}