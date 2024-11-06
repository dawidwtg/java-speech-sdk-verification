package org.wtg;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        TranslationTests.translationWithFileAsync();

        System.exit(0);
    }
}