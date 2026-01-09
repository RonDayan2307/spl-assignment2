package spl.lae;
import java.io.IOException;

import parser.ComputationNode;
import parser.InputParser;
import parser.OutputWriter;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: 
      if (args.length != 3) {
            System.err.println("Usage: java spl.lae.Main <input_file> <output_file> <num_threads>");
            args = new String[3];
            args[0] = "4";
            args[1] = "example.json";
            args[2] = "out.json";
        }

        String inputPath = args[1];
        String outputPath = args[2];
        int numThreads =0;

        try {
            numThreads = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Number of threads must be an integer.");
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
                System.err.println("Failed to write error to output file: " + ioException.getMessage());
            }
            e.printStackTrace();
        } finally {
            if (engine != null) {
              try {
                engine.shutdown();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
        }
    }
}