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
            throw new IllegalArgumentException("matrix is null - on loadRowMajor");
        }
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        int vec_len = matrix[0].length;
        for (double[] vec : matrix) {
            if (vec.length != vec_len){
                throw new IllegalArgumentException("vector length in matrix invalid - on loadRowMajor");
            }
        }
        SharedVector[] new_vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            new_vectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        acquireAllVectorWriteLocks(this.vectors);
        SharedVector[] tmp = this.vectors;
        this.vectors = new_vectors;
        releaseAllVectorWriteLocks(tmp);
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        if (matrix == null){
            throw new IllegalArgumentException("matrix is null - on loadColumnMajor");
        }
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        int vec_len = matrix[0].length;
        for (double[] vec : matrix) {
            if (vec.length != vec_len){
                throw new IllegalArgumentException("vector length in matrix invalid - on loadColumnMajor");
            }
        }
        SharedVector[] new_vectors = new SharedVector[matrix[0].length];
        for (int i = 0; i < matrix[0].length; i++) {
            double[] column_vector = new double[matrix.length];
            for (int j = 0; j < matrix.length; j++) {
                column_vector[j] = matrix[j][i];
            }
            new_vectors[i] = new SharedVector(column_vector, VectorOrientation.COLUMN_MAJOR);
        }
        acquireAllVectorWriteLocks(this.vectors);
        SharedVector[] tmp = this.vectors;
        this.vectors = new_vectors;
        releaseAllVectorWriteLocks(tmp);
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        if (this.vectors == null){
            throw new IllegalArgumentException("matrix is null - on readRowMajor");
        }
        if (this.vectors.length == 0) {
            return new double[0][0];
        }
        acquireAllVectorReadLocks(this.vectors);
        try {
            int vec_len = this.vectors[0].length();
            VectorOrientation vec_ori = vectors[0].getOrientation();
            for (SharedVector vec : this.vectors) {
                if (vec.length() != vec_len){
                    throw new IllegalArgumentException("vector length in matrix invalid - on readRowMajor");
                }
                if (vec.getOrientation() != vec_ori){
                    throw new IllegalArgumentException("vector orientation in matrix invalid - on readRowMajor");
                }
            }
            //check the vector orientation and react accordingly
            if (vec_ori == VectorOrientation.ROW_MAJOR) {
                double[][] ans = new double[this.vectors.length][vec_len];
                for (int i = 0; i < this.vectors.length; i++) {
                    for (int j = 0; j < vec_len; j++) {
                        ans[i][j] = vectors[i].get(j);
                    }
                }
                return ans;
            }
            else if (vec_ori == VectorOrientation.COLUMN_MAJOR) {
                double[][] ans = new double[vec_len][this.vectors.length];
                for (int i = 0; i < this.vectors.length; i++) {
                    for (int j = 0; j < vec_len; j++) {
                        ans[j][i] = vectors[i].get(j);
                    }
                }
                return ans;
            }
        } finally {
            releaseAllVectorReadLocks(this.vectors);
        }
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return this.vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if (this.vectors.length == 0) {
            return VectorOrientation.ROW_MAJOR; 
        }
        return this.vectors[0].getOrientation();
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
            vec.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}
