package spl.lae;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import memory.SharedMatrix;
import parser.ComputationNode;
import parser.ComputationNodeType;

public class LinearAlgebraEngineTest {

    private LinearAlgebraEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new LinearAlgebraEngine(4);
    }

    @AfterEach
    public void tearDown() {
        if (engine != null) {
            engine.shutdown();
        }
    }

    // ==========================================
    // HELPER: REFLECTION FOR PRIVATE STATE
    // ==========================================
    private void injectLeftMatrix(double[][] data) throws Exception {
        Field f = LinearAlgebraEngine.class.getDeclaredField("leftMatrix");
        f.setAccessible(true);
        SharedMatrix m = (SharedMatrix) f.get(engine);
        if (data == null || data.length == 0) return; 
        m.loadRowMajor(data);
    }

    private void injectRightMatrix(double[][] data) throws Exception {
        Field f = LinearAlgebraEngine.class.getDeclaredField("rightMatrix");
        f.setAccessible(true);
        SharedMatrix m = (SharedMatrix) f.get(engine);
        if (data == null || data.length == 0) return;
        m.loadRowMajor(data);
    }

    // ==========================================
    // 1. CONSTRUCTOR TESTS
    // ==========================================

    @Test
    public void testConstructor_Small_Pass() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        assertNotNull(lae.getWorkerReport());
        lae.shutdown();
    }

    @Test
    public void testConstructor_Small_Fail() {
        assertThrows(NegativeArraySizeException.class, () -> new LinearAlgebraEngine(-1));
    }

    @Test
    public void testConstructor_Mid_Pass() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(10);
        assertTrue(lae.getWorkerReport().contains("Worker 9"));
        lae.shutdown();
    }

    @Test
    public void testConstructor_Mid_Fail() {
        assertDoesNotThrow(() -> new LinearAlgebraEngine(0));
    }

    @Test
    public void testConstructor_Large_Pass() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(100);
        lae.shutdown();
    }

    // ==========================================
    // 2. CREATE ADD TASKS TESTS
    // ==========================================

    @Test
    public void testCreateAddTasks_Small_Pass() throws Exception {
        injectLeftMatrix(new double[][]{{1}});
        injectRightMatrix(new double[][]{{2}});
        List<Runnable> tasks = engine.createAddTasks();
        assertEquals(1, tasks.size());
    }

    @Test
    public void testCreateAddTasks_Small_Fail() throws Exception {
        injectLeftMatrix(new double[][]{{1}});
        injectRightMatrix(new double[][]{{1, 2}});
        assertThrows(IllegalArgumentException.class, () -> engine.createAddTasks());
    }

    @Test
    public void testCreateAddTasks_Mid_Pass() throws Exception {
        double[][] data = new double[50][50];
        injectLeftMatrix(data);
        injectRightMatrix(data);
        List<Runnable> tasks = engine.createAddTasks();
        assertEquals(50, tasks.size());
    }

    @Test
    public void testCreateAddTasks_Mid_Fail() throws Exception {
        injectLeftMatrix(new double[10][5]);
        injectRightMatrix(new double[5][5]);
        assertThrows(IllegalArgumentException.class, () -> engine.createAddTasks());
    }

    @Test
    public void testCreateAddTasks_Large_Pass() throws Exception {
        injectLeftMatrix(new double[1000][1]);
        injectRightMatrix(new double[1000][1]);
        List<Runnable> tasks = engine.createAddTasks();
        assertEquals(1000, tasks.size());
    }

    // ==========================================
    // 3. CREATE MULTIPLY TASKS TESTS
    // ==========================================

    @Test
    public void testCreateMultiplyTasks_Small_Pass() throws Exception {
        injectLeftMatrix(new double[][]{{1, 2}});
        injectRightMatrix(new double[][]{{3}, {4}});
        List<Runnable> tasks = engine.createMultiplyTasks();
        assertEquals(1, tasks.size());
    }

    @Test
    public void testCreateMultiplyTasks_Small_Fail() throws Exception {
        injectLeftMatrix(new double[][]{{1, 2}});
        injectRightMatrix(new double[][]{{1}, {2}, {3}});
        assertThrows(IllegalArgumentException.class, () -> engine.createMultiplyTasks());
    }

    @Test
    public void testCreateMultiplyTasks_Mid_Pass() throws Exception {
        injectLeftMatrix(new double[10][10]);
        injectRightMatrix(new double[10][10]);
        List<Runnable> tasks = engine.createMultiplyTasks();
        assertEquals(10, tasks.size());
    }

    @Test
    public void testCreateMultiplyTasks_Mid_Fail() throws Exception {
        injectLeftMatrix(new double[10][5]);
        injectRightMatrix(new double[4][10]);
        assertThrows(IllegalArgumentException.class, () -> engine.createMultiplyTasks());
    }

    @Test
    public void testCreateMultiplyTasks_Large_Pass() throws Exception {
        injectLeftMatrix(new double[500][5]);
        injectRightMatrix(new double[5][500]);
        List<Runnable> tasks = engine.createMultiplyTasks();
        assertEquals(500, tasks.size());
    }

    // ==========================================
    // 4. CREATE NEGATE TASKS TESTS
    // ==========================================

    @Test
    public void testCreateNegateTasks_Small_Pass() throws Exception {
        injectLeftMatrix(new double[][]{{1}});
        List<Runnable> tasks = engine.createNegateTasks();
        assertEquals(1, tasks.size());
    }

    @Test
    public void testCreateNegateTasks_Small_Fail() throws Exception {
        injectLeftMatrix(new double[0][0]);
        List<Runnable> tasks = engine.createNegateTasks();
        assertEquals(0, tasks.size());
    }

    @Test
    public void testCreateNegateTasks_Mid_Pass() throws Exception {
        injectLeftMatrix(new double[50][10]);
        List<Runnable> tasks = engine.createNegateTasks();
        assertEquals(50, tasks.size());
    }

    @Test
    public void testCreateNegateTasks_Mid_Fail() throws Exception {
        injectLeftMatrix(new double[10][10]);
        injectRightMatrix(new double[5][5]); 
        assertDoesNotThrow(() -> engine.createNegateTasks());
    }

    @Test
    public void testCreateNegateTasks_Large_Pass() throws Exception {
        injectLeftMatrix(new double[1000][1]);
        assertEquals(1000, engine.createNegateTasks().size());
    }

    // ==========================================
    // 5. CREATE TRANSPOSE TASKS TESTS
    // ==========================================

    @Test
    public void testCreateTransposeTasks_Small_Pass() throws Exception {
        injectLeftMatrix(new double[][]{{1}});
        assertEquals(1, engine.createTransposeTasks().size());
    }

    @Test
    public void testCreateTransposeTasks_Small_Fail() throws Exception {
        injectLeftMatrix(new double[0][0]);
        assertEquals(0, engine.createTransposeTasks().size());
    }

    @Test
    public void testCreateTransposeTasks_Mid_Pass() throws Exception {
        injectLeftMatrix(new double[50][50]);
        assertEquals(50, engine.createTransposeTasks().size());
    }

    @Test
    public void testCreateTransposeTasks_Mid_Fail() throws Exception {
        injectLeftMatrix(new double[1][100]);
        assertDoesNotThrow(() -> engine.createTransposeTasks());
    }

    @Test
    public void testCreateTransposeTasks_Large_Pass() throws Exception {
        injectLeftMatrix(new double[1000][5]);
        assertEquals(1000, engine.createTransposeTasks().size());
    }

    // ==========================================
    // 6. GET WORKER REPORT TESTS
    // ==========================================

    @Test
    public void testGetWorkerReport_Small_Pass() {
        assertNotNull(engine.getWorkerReport());
    }

    @Test
    public void testGetWorkerReport_Small_Fail() {
        assertNotEquals("", engine.getWorkerReport());
    }

    @Test
    public void testGetWorkerReport_Mid_Pass() {
        assertTrue(engine.getWorkerReport().contains("Worker Report"));
    }

    @Test
    public void testGetWorkerReport_Mid_Fail() {
        assertNotNull(engine.getWorkerReport());
    }

    @Test
    public void testGetWorkerReport_Large_Pass() {
        LinearAlgebraEngine big = new LinearAlgebraEngine(100);
        assertTrue(big.getWorkerReport().length() > 100);
        big.shutdown();
    }

    // ==========================================
    // 7. SHUTDOWN TESTS
    // ==========================================

    @Test
    public void testShutdown_Small_Pass() {
        engine.shutdown();
    }

    @Test
    public void testShutdown_Small_Fail() {
        engine.shutdown();
        assertDoesNotThrow(() -> engine.shutdown());
    }

    @Test
    public void testShutdown_Mid_Pass() {
        LinearAlgebraEngine e = new LinearAlgebraEngine(5);
        e.shutdown();
    }

    @Test
    public void testShutdown_Mid_Fail() {
        engine.shutdown();
    }

    @Test
    public void testShutdown_Large_Pass() {
        LinearAlgebraEngine e = new LinearAlgebraEngine(50);
        e.shutdown();
    }

    // ==========================================
    // 8. RUN & LOAD_AND_COMPUTE TESTS (MOCKING)
    // ==========================================

    /**
     * MOCK NODE IMPLEMENTATION for Testing
     */
    static class MockComputationNode extends ComputationNode {
        private ComputationNode nextResolvable;
        private ComputationNodeType type;
        private List<ComputationNode> children;
        private double[][] result;
        private double[][] matrixData;

        public MockComputationNode(ComputationNodeType type, List<ComputationNode> children, double[][] matrixData) {
            super(type, children); 
            this.type = type;
            this.children = children;
            this.matrixData = matrixData;
        }

        public void setNextResolvable(ComputationNode n) { this.nextResolvable = n; }

        @Override public ComputationNode findResolvable() { 
            ComputationNode temp = nextResolvable;
            nextResolvable = null; 
            return temp; 
        }
        @Override public List<ComputationNode> getChildren() { return children; }
        @Override public ComputationNodeType getNodeType() { return type; }
        @Override public double[][] getMatrix() { return matrixData; }
        @Override public void resolve(double[][] res) { this.result = res; }
        
        public double[][] getResult() { return result; }
    }

    @Test
    public void testRun_Small_Pass() {
        // 1. Small Pass: Simple 1x1 Negate
        double[][] data = {{1.0}};
        // Use MATRIX for leaf node type
        MockComputationNode leaf = new MockComputationNode(ComputationNodeType.MATRIX, null, data);
        MockComputationNode root = new MockComputationNode(ComputationNodeType.NEGATE, Arrays.asList(leaf), null);
        
        root.setNextResolvable(root);
        engine.run(root);
        
        assertNotNull(root.getResult());
        assertEquals(-1.0, root.getResult()[0][0]);
    }

    @Test
    public void testRun_Small_Fail() {
        // 2. Small Fail: Operation mismatch (Unknown Op)
        // Passing null as type to the ROOT node triggers "unknown operation"
        MockComputationNode badNode = new MockComputationNode(null, Arrays.asList(new MockComputationNode(ComputationNodeType.MATRIX, null, new double[][]{{1}})), null);
        badNode.setNextResolvable(badNode);
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(badNode));
    }

    @Test
    public void testRun_Mid_Pass() {
        // 3. Mid Pass: 2x2 Transpose
        double[][] data = {{1, 2}, {3, 4}};
        MockComputationNode leaf = new MockComputationNode(ComputationNodeType.MATRIX, null, data);
        MockComputationNode root = new MockComputationNode(ComputationNodeType.TRANSPOSE, Arrays.asList(leaf), null);
        
        root.setNextResolvable(root);
        engine.run(root);
        
        double[][] res = root.getResult();
        assertEquals(1.0, res[0][0]);
        assertEquals(3.0, res[0][1]);
    }

    @Test
    public void testRun_Mid_Fail() {
        // 4. Mid Fail: Add with mismatched dimensions
        double[][] d1 = {{1}};
        double[][] d2 = {{1, 2}};
        MockComputationNode l1 = new MockComputationNode(ComputationNodeType.MATRIX, null, d1);
        MockComputationNode l2 = new MockComputationNode(ComputationNodeType.MATRIX, null, d2);
        MockComputationNode root = new MockComputationNode(ComputationNodeType.ADD, Arrays.asList(l1, l2), null);
        
        root.setNextResolvable(root);
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    public void testRun_Large_Pass() {
        // 5. Large Pass: Add 100x100
        double[][] data = new double[100][100];
        MockComputationNode l1 = new MockComputationNode(ComputationNodeType.MATRIX, null, data);
        MockComputationNode l2 = new MockComputationNode(ComputationNodeType.MATRIX, null, data);
        MockComputationNode root = new MockComputationNode(ComputationNodeType.ADD, Arrays.asList(l1, l2), null);
        
        root.setNextResolvable(root);
        engine.run(root);
        
        assertNotNull(root.getResult());
        assertEquals(100, root.getResult().length);
    }
}