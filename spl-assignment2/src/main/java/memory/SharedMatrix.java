package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        this.loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        if (matrix == null){
            throw new IllegalArgumentException("matrix is null - on sharedmatrix constructor");
        }
        if (matrix.length < 1){
            throw new IllegalArgumentException("matrix length invalid - on sharedmatrix constructor");
        }
        int veclen = matrix[0].length;
        for (double[] vec : matrix) {
            if (vec.length != veclen){
                throw new IllegalArgumentException("vector length in matrix invalid - on sharedmatrix constructor");
            }
        }
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        acquireAllVectorReadLocks(this.vectors);
        try {
            SharedVector ans = this.vectors[index];
            releaseAllVectorReadLocks(this.vectors);
            return ans;
        } catch (Exception exp) {
            releaseAllVectorReadLocks(this.vectors);
            throw exp;
        }
        // catches any exception and prints its message
    }

    public int length() {
        // TODO: return number of stored vectors
        acquireAllVectorReadLocks(this.vectors);
        int ans = this.vectors.length;
        releaseAllVectorReadLocks(this.vectors);
        return ans;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        acquireAllVectorReadLocks(this.vectors);
        try {
            VectorOrientation ans = vectors[0].getOrientation();;
            releaseAllVectorReadLocks(this.vectors);
            return ans;
        } catch (Exception exp) {
            releaseAllVectorReadLocks(this.vectors);
            throw exp;
        }
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (SharedVector vec : vecs) {
            vec.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector vec : vecs) {
            vec.readUnlock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}
