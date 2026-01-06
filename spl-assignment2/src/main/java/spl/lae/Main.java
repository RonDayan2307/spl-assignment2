package spl.lae;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: 
      if (args.length != 3) {
            System.err.println("Usage: java spl.lae.Main <input_file> <output_file> <num_threads>");
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];
        int numThreads = 0;

        try {
            numThreads = Integer.parseInt(args[2]);
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
            // If anything goes wrong (Parsing, Math, I/O), write the error to the file
            try {
                OutputWriter.write(e.getMessage(), outputPath);
            } catch (IOException ioException) {
                System.err.println("Failed to write error to output file: " + ioException.getMessage());
            }
            e.printStackTrace();
        } finally {
            // 6. Graceful Shutdown
            // We must shut down the executor, otherwise the worker threads will 
            // keep the JVM alive and the program will never exit.
            if (engine != null) {
              try {
                engine.executor.shutdown();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
        }
    }
}