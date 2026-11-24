package com.example.myapplication.repository;

import java.util.concurrent.Callable;

public interface TransactionRunner {
    <T> T run(Callable<T> work);
}
