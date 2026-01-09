package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        readLock();
        try {
            return this.vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        // TODO: return vector length
        readLock();
        try {
            return this.vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        readLock();
        try {
            return this.orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        this.lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        this.lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        writeLock();
        try {
            if (this.orientation == VectorOrientation.COLUMN_MAJOR) {
                this.orientation = VectorOrientation.ROW_MAJOR;
            } else {
                this.orientation = VectorOrientation.COLUMN_MAJOR;
            }
        } finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        writeLock();
        try {
            other.readLock();
            try {
                if (this.vector.length != other.length()) {
                    throw new IllegalArgumentException("vector lengths do not match - on add vectors");
                }
                if (this.orientation != other.getOrientation()) {
                    throw new IllegalArgumentException("vector orientation do not match - on add vectors");
                }
                for (int i = 0; i < this.vector.length; i++) {
                    this.vector[i] = this.vector[i] + other.get(i);
                }
            } finally {
                other.readUnlock();
            }
        } finally {
            writeUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        writeLock();
        try {
            for (int i = 0; i < this.vector.length; i++) {
                this.vector[i] *= -1;
            }
        } finally {
            writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        readLock();
        try {
            other.readLock();
            try {
                if (this.orientation != VectorOrientation.ROW_MAJOR || other.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
                    throw new IllegalArgumentException("not ROW dot COLUMN - on dot vectors");
                }
                if (this.vector.length != other.length()) {
                    throw new IllegalArgumentException("ROW length != COLUMN length - on dot vectors");
                }

                double ans = 0;
                for (int i = 0; i < this.vector.length; i++) {
                    ans += this.vector[i] * other.get(i);
                }
                return ans;
            } finally {
                other.readUnlock();
            }
        } finally {
            readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        writeLock();
        try {
            if (this.orientation != VectorOrientation.ROW_MAJOR) {
                throw new IllegalArgumentException("not ROW dot COLUMN - on vecMatMul");
            }

            // if matrix is ROW_MAJOR - transpose it, else - use it
            SharedMatrix matrix_copy = matrix;
            if (matrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
                double[][] rawData = matrix.readRowMajor(); 
                matrix_copy = new SharedMatrix();
                matrix_copy.loadColumnMajor(rawData);
            }

            double[] resultVector = new double[matrix_copy.length()];
            for (int i = 0; i < matrix_copy.length(); i++) {
                SharedVector column = matrix_copy.get(i); 
                resultVector[i] = this.dot(column);
            }

            this.vector = resultVector;            
        } finally {
            writeUnlock();
        }
    }
}
