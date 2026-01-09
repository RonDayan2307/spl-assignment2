package spl.lae;

import java.util.Arrays;
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

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        ComputationNode active = computationRoot.findResolvable();
        while (active != null) {
            loadAndCompute(active);
            active = computationRoot.findResolvable();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        List<Runnable> tasks;
        List<ComputationNode> children = node.getChildren();
        double[][] first = children.get(0).getMatrix();
        leftMatrix.loadRowMajor(first);
        if (children.size() > 1) {
            double[][] second = children.get(1).getMatrix();
            rightMatrix.loadRowMajor(second);
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
            throw new IllegalArgumentException("unknown operation: " + type);
        }

        executor.submitAll(tasks);
        double[][] result = leftMatrix.readRowMajor();
        node.resolve(result);
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        int rows = leftMatrix.length();
        if (rows == 0) { 
            return Arrays.asList();
        }
        if (rows != rightMatrix.length() || leftMatrix.get(0).length() != rightMatrix.get(0).length()) {
            throw new IllegalArgumentException("dimentions dont match - on add task.");
        }
        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int index = i;
            tasks[i] = () -> {
                leftMatrix.get(index).add(rightMatrix.get(index));
            };
        }
        return Arrays.asList(tasks);
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        int rows = leftMatrix.length();
        if (rows == 0) { 
            return Arrays.asList();
        }
        if (leftMatrix.get(0).length() != rightMatrix.length()) {
            throw new IllegalArgumentException("dimentions dont match - on multiply task.");
        }
        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int index = i;
            tasks[i] = () -> {
                leftMatrix.get(index).vecMatMul(this.rightMatrix);
            };
        }
        return Arrays.asList(tasks);
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        int rows = leftMatrix.length();
        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int index = i;
            tasks[i] = () -> {
                leftMatrix.get(index).negate();
            };
        }
        return Arrays.asList(tasks);
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        int rows = leftMatrix.length();
        Runnable[] tasks = new Runnable[rows];
        for (int i = 0; i < rows; i++) {
            final int index = i;
            tasks[i] = () -> {
                leftMatrix.get(index).transpose();
            };
        }
        return Arrays.asList(tasks);
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