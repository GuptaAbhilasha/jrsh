package com.jaspersoft.jasperserver.jrsh.core.operation;

/**
 * @author Alexander Krasnyanskiy
 */
public class OperationResult {

    private String resultMessage;
    private ResultCode resultCode;
    private Operation context;
    private OperationResult previous;

    public OperationResult(String resultMessage, ResultCode resultCode, Operation context, OperationResult previous) {
        this.resultMessage = resultMessage;
        this.resultCode = resultCode;
        this.context = context;
        this.previous = previous;
    }

    public enum ResultCode {
        SUCCESS(0),
        FAILED(1),
        INTERRUPTED(2);

        private int value;

        ResultCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public Operation getContext() {
        return context;
    }

    public void setContext(Operation context) {
        this.context = context;
    }

    public OperationResult getPrevious() {
        return previous;
    }

    public void setPrevious(OperationResult previous) {
        this.previous = previous;
    }
}