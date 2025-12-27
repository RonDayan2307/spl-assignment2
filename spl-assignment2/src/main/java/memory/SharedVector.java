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
            double ans = this.vector[index];
            readUnlock();
            return ans;
        } catch (Exception exp) {
            readUnlock();
            throw exp;
        }
        // catches any exception and prints its message
    }

    public int length() {
        // TODO: return vector length
        readLock();
        int ans = this.vector.length;
        readUnlock();
        return ans;
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        readLock();
        VectorOrientation ans = this.orientation;
        readUnlock();
        return ans;
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
        if(orientation == VectorOrientation.COLUMN_MAJOR){
            orientation = VectorOrientation.ROW_MAJOR;
        }
        else{
            orientation = VectorOrientation.COLUMN_MAJOR;
        }
        writeUnlock();
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        try {
            if (length() != other.length()){
                throw new IllegalArgumentException("vector length doesn't match - on add vectors");
            }
            writeLock();
            other.readLock();
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = this.vector[i] + other.vector[i];
            }
        } finally {
            writeUnlock();
            other.readUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = this.vector[i] * (-1);
            }
        } finally {
            writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        if (getOrientation() != VectorOrientation.ROW_MAJOR || other.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("not ROW dot COLUMN - on dot vectors");
        }
        if (length() != other.length()){
            throw new IllegalArgumentException("ROW length doesn't match COLUMN length - on dot vectors");
        }
        readLock();
        other.readLock();
        double ans = 0;
        try {
            for (int i = 0; i < length(); i++) {
                ans += this.vector[i] * other.vector[i];
            }
        } finally {
            readUnlock();
            other.readUnlock();
        }
        return ans;
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        writeLock();
        SharedVector column = matrix;
        writeUnlock();

    }
}
