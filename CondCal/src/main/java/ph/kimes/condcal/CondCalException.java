package ph.kimes.condcal;

public class CondCalException extends Exception {

    public CondCalException(String errorMessage) { super(errorMessage); }
    public CondCalException(String errorMessage, Throwable err) { super(errorMessage, err); }
}
