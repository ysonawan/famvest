package com.fam.vest.service;

public interface AdminService {

    void captureSnapshot();

    String encrypt(String text  );

    void restartApplication();

    void notifySchedulerErrors();
}
