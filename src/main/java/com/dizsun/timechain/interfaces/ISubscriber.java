package com.dizsun.timechain.interfaces;

public interface ISubscriber {
    void doPerTC();

    void doPerTP();

    void doPerTE();

    void doPerRunning();
}
