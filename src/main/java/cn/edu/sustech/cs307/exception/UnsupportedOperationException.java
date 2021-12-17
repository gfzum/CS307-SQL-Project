package cn.edu.sustech.cs307.exception;

public class UnsupportedOperationException extends RuntimeException{
    public UnsupportedOperationException(){
    }
    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }
}
