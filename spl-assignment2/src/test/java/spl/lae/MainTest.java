package spl.lae;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testMain_Small_Pass() throws IOException {
        Path inputFile = tempDir.resolve("input_small.json");
        Path outputFile = tempDir.resolve("output_small.json");
        String jsonContent = "[[1.0, 2.0], [3.0, 4.0]]"; 
        Files.writeString(inputFile, jsonContent);

        String[] args = {"2", inputFile.toString(), outputFile.toString()};
        Main.main(args);

        assertTrue(Files.exists(outputFile), "Output file should be created");
        
        String stdout = outContent.toString();
        String stderr = errContent.toString();
        
        assertTrue(stdout.contains("Worker Report"), 
            "Worker Report missing. The program likely crashed.\nSTDERR: " + stderr);
        assertEquals("", stderr, "Standard error should be empty");
    }

    @Test
    public void testMain_Small_Fail() throws IOException {
        String[] args = {"only_one_arg"};
        Main.main(args);
        String stderr = errContent.toString();
        assertTrue(stderr.contains("Usage:"), "Should print Usage message. Actual: " + stderr);
    }

    @Test
    public void testMain_Mid_Pass() throws IOException {
        Path inputFile = tempDir.resolve("input_mid.json");
        Path outputFile = tempDir.resolve("output_mid.json");
        
        String jsonInput = "{" +
                "\"operator\": \"T\"," + 
                "\"operands\": [" +
                "   [[1.0, 2.0]]" + 
                "]" +
                "}";
        Files.writeString(inputFile, jsonInput);

        String[] args = {"4",  inputFile.toString(), outputFile.toString()};
        Main.main(args);

        String stdout = outContent.toString();
        String stderr = errContent.toString();

        assertTrue(stdout.contains("Worker Report"), 
            "Worker Report missing.\nSTDERR (Crash Log): " + stderr);
            
        assertEquals("", stderr, "Expected no errors in System.err");
    }

    @Test
    public void testMain_Mid_Fail() throws IOException {
        Path inputFile = tempDir.resolve("dummy.json");
        Path outputFile = tempDir.resolve("dummy.out");
        String[] args = { inputFile.toString(), outputFile.toString(), "abc" };

        Main.main(args);

        String stderr = errContent.toString();
        assertTrue(stderr.contains("number of threads must be an integer"), 
            "Should catch number format exception. Actual: " + stderr);
    }

    @Test
    public void testMain_Large_Pass_ErrorHandling() throws IOException {
        Path inputFile = tempDir.resolve("input_error.json");
        Path outputFile = tempDir.resolve("output_error.json");

        String jsonInput = "{" +
                "\"operator\": \"+\"," + 
                "\"operands\": [" +
                "   [[1.0]]," + 
                "   [[1.0, 2.0]]" +
                "]" +
                "}";
        Files.writeString(inputFile, jsonInput);

        String[] args = {"4",  inputFile.toString(), outputFile.toString()};
        Main.main(args);

        assertTrue(Files.exists(outputFile), "Output file should exist even if calculation failed");
        
        String outputContent = Files.readString(outputFile);
        assertFalse(outputContent.isEmpty(), "Output file should contain the error message");
    }
}