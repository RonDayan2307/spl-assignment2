package spl.lae;

import java.util.ArrayList;
import java.util.List;

import memory.SharedMatrix;
import parser.ComputationNode;
import parser.ComputationNodeType;
import scheduling.TiredExecutor;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        this.executor = new TiredExecutor(numThreads);
    }

    public void setLeft(SharedMatrix l){
        leftMatrix=l;
    }

    public void setRight(SharedMatrix r){
        rightMatrix = r;
    }

    public SharedMatrix getLeft(){
        return leftMatrix;
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        ComputationNode activeNode = computationRoot.findResolvable();
        while (activeNode != null && computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
            loadAndCompute(activeNode);
            activeNode = computationRoot.findResolvable();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        List<Runnable> tasks;
        List<ComputationNode> children = node.getChildren();
        double[][] firstRaw = children.get(0).getMatrix();
        leftMatrix.loadRowMajor(firstRaw);
        if (children.size() > 1) {
            double[][] secondRaw = children.get(1).getMatrix();
            rightMatrix.loadRowMajor(secondRaw);
        }

        ComputationNodeType type = node.getNodeType();
        if (type == ComputationNodeType.ADD) {
            tasks = createAddTasks();
        } 
        else if (type == ComputationNodeType.MULTIPLY) {
            tasks = createMultiplyTasks();
        } 
        else if (type == ComputationNodeType.NEGATE) {
            tasks = createNegateTasks();
        } 
        else if (type == ComputationNodeType.TRANSPOSE) {
            tasks = createTransposeTasks();
        } 
        else {
            throw new IllegalArgumentException("Unsupported operation: " + type);
        }

        executor.submitAll(tasks);
        double[][] resultData = leftMatrix.readRowMajor();
        node.resolve(resultData);
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        int rows = leftMatrix.length();
        if (rows != rightMatrix.length() || leftMatrix.get(0).length() != rightMatrix.get(0).length()) {
            throw new IllegalArgumentException("Matrix dimensions mismatch for Addition.");
        }
        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            tasks[i] = () -> {
                leftMatrix.get(rowIndex).add(rightMatrix.get(rowIndex));
            };
        }
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < tasks.length; i++) {
            taskList.add(tasks[i]);
        }
        return taskList;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        int rows = leftMatrix.length();
        Runnable[] taskArray = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            taskArray[i] = () -> {
                leftMatrix.get(rowIndex).vecMatMul(rightMatrix);
            };
        }
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < taskArray.length; i++) {
            taskList.add(taskArray[i]);
        }
        return taskList;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        int rows = leftMatrix.length();
        Runnable[] taskArray = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            taskArray[i] = () -> {
                leftMatrix.get(rowIndex).negate();
            };
        }
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < taskArray.length; i++) {
            taskList.add(taskArray[i]);
        }
        return taskList;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        int rows = leftMatrix.length();
        Runnable[] taskArray = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            taskArray[i] = () -> {
                leftMatrix.get(rowIndex).transpose();
            };
        }
        List<Runnable> taskList = new ArrayList<>();
        for (int i = 0; i < taskArray.length; i++) {
            taskList.add(taskArray[i]);
        }
        return taskList;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }


// shutdown executor - to be accessed from main
    public void shutdown() {
        try {
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}