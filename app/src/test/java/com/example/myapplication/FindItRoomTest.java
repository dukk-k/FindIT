package com.example.myapplication;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public final class FindItRoomTest extends FindItWorkflowCases {}
