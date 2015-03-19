package de.dkfz.roddy.core;

/**
*/
public class AsyncDataFetcherResult<T> {
    public final boolean isFinalResult;
    public final boolean isIntermediateResult;

    public final T resultsObject;

    public AsyncDataFetcherResult(boolean finalResult, T resultsObject) {
        isFinalResult = finalResult;
        isIntermediateResult = !finalResult;
        this.resultsObject = resultsObject;
    }
}
