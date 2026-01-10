package spl.lae;

import java.io.IOException;

import parser.ComputationNode;
import parser.InputParser;
import parser.OutputWriter;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java spl.lae.Main <input_file> <output_file> <num_threads>");
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];
        int numThreads = 0;

        try {
            numThreads = Integer.parseInt(args[2]);
        } catch (Exception e) {
            System.err.println("number of threads must be an integer.");
            return;
        }

        LinearAlgebraEngine engine = null;

        try {
            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(inputPath);
            engine = new LinearAlgebraEngine(numThreads);
            ComputationNode resultNode = engine.run(root);
            double[][] resultMatrix = resultNode.getMatrix();
            OutputWriter.write(resultMatrix, outputPath);
            System.out.println(engine.getWorkerReport());
        } catch (Exception e) {
            try {
                OutputWriter.write(e.getMessage(), outputPath);
            } catch (IOException ioException) {
                System.err.println("failed to write error to output file: " + ioException.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (engine != null && engine.getExecutor() != null) {
                try {
                    engine.getExecutor().shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}